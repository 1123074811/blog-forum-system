package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.entity.Question;
import com.blog.entity.QuizBank;
import com.blog.mapper.QuestionMapper;
import com.blog.mapper.QuizBankMapper;
import com.blog.service.QuizService;
import com.blog.util.DateUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuizServiceImpl implements QuizService {

    private final QuizBankMapper quizBankMapper;
    private final QuestionMapper questionMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public QuizBank importQuiz(String jsonContent, Long userId) {
        try {
            JsonNode root = objectMapper.readTree(jsonContent);

            QuizBank quizBank = new QuizBank();
            quizBank.setUserId(userId);
            quizBank.setTitle(root.has("title") ? root.get("title").asText() : "未命名题库");
            quizBank.setCreatedAt(DateUtil.now());

            JsonNode questions = root.get("questions");
            quizBank.setQuestionCount(questions != null ? questions.size() : 0);
            quizBankMapper.insert(quizBank);

            if (questions != null && questions.isArray()) {
                List<Question> questionList = new java.util.ArrayList<>();
                int order = 0;
                for (JsonNode q : questions) {
                    Question question = new Question();
                    question.setQuizBankId(quizBank.getId());
                    question.setType(q.get("type").asText());
                    question.setQuestion(q.get("question").asText());
                    question.setOptions(q.has("options") ? objectMapper.writeValueAsString(q.get("options")) : null);
                    question.setAnswer(objectMapper.writeValueAsString(q.get("answer")));
                    question.setExplanation(q.has("explanation") ? q.get("explanation").asText() : null);
                    question.setSortOrder(order++);
                    questionList.add(question);
                }
                // 批量插入，每500条一批
                int batchSize = 500;
                for (int i = 0; i < questionList.size(); i += batchSize) {
                    int end = Math.min(i + batchSize, questionList.size());
                    questionMapper.insertBatch(questionList.subList(i, end));
                }
            }
            return quizBank;
        } catch (Exception e) {
            throw new RuntimeException("解析JSON失败: " + e.getMessage());
        }
    }

    @Override
    public List<QuizBank> getUserQuizBanks(Long userId) {
        return quizBankMapper.selectList(
            new LambdaQueryWrapper<QuizBank>()
                .eq(QuizBank::getUserId, userId)
                .orderByDesc(QuizBank::getCreatedAt)
        );
    }

    @Override
    public List<QuizBank> getPublicQuizBanks() {
        return quizBankMapper.selectList(
            new LambdaQueryWrapper<QuizBank>()
                .eq(QuizBank::getIsPublic, true)
                .orderByDesc(QuizBank::getCreatedAt)
        );
    }

    @Override
    public QuizBank getQuizBankWithQuestions(Long id) {
        QuizBank quizBank = quizBankMapper.selectById(id);
        if (quizBank != null) {
            List<Question> questions = questionMapper.selectList(
                new LambdaQueryWrapper<Question>()
                    .eq(Question::getQuizBankId, id)
                    .orderByAsc(Question::getSortOrder)
            );
            quizBank.setQuestions(questions);
        }
        return quizBank;
    }

    @Override
    public Page<Question> getQuestionsByPage(Long quizBankId, int page, int size) {
        return questionMapper.selectPage(
            new Page<>(page, size),
            new LambdaQueryWrapper<Question>()
                .eq(Question::getQuizBankId, quizBankId)
                .orderByAsc(Question::getSortOrder)
        );
    }

    @Override
    @Transactional
    public void deleteQuizBank(Long id, Long userId) {
        QuizBank quizBank = quizBankMapper.selectById(id);
        if (quizBank != null && quizBank.getUserId().equals(userId)) {
            questionMapper.delete(new LambdaQueryWrapper<Question>().eq(Question::getQuizBankId, id));
            quizBankMapper.deleteById(id);
        }
    }

    @Override
    public List<QuizBank> getAllQuizBanks() {
        return quizBankMapper.selectList(new LambdaQueryWrapper<QuizBank>().orderByDesc(QuizBank::getCreatedAt));
    }

    @Override
    @Transactional
    public void adminDelete(Long id) {
        questionMapper.delete(new LambdaQueryWrapper<Question>().eq(Question::getQuizBankId, id));
        quizBankMapper.deleteById(id);
    }

    @Override
    public QuizBank adminUpdate(Long id, QuizBank quizBank) {
        QuizBank existing = quizBankMapper.selectById(id);
        if (existing == null) throw new RuntimeException("QuizBank not found");
        existing.setTitle(quizBank.getTitle());
        existing.setDescription(quizBank.getDescription());
        quizBankMapper.updateById(existing);
        return existing;
    }

    @Override
    public void togglePublic(Long id, Long userId, Boolean isPublic) {
        QuizBank quizBank = quizBankMapper.selectById(id);
        if (quizBank != null && quizBank.getUserId().equals(userId)) {
            quizBank.setIsPublic(isPublic);
            quizBankMapper.updateById(quizBank);
        }
    }

    @Override
    public void save(QuizBank quizBank) {
        quizBankMapper.insert(quizBank);
    }

    @Override
    public QuizBank getById(Long id) {
        return quizBankMapper.selectById(id);
    }

    @Override
    @Transactional
    public void removeByIds(List<Long> ids) {
        for (Long id : ids) {
            questionMapper.delete(new LambdaQueryWrapper<Question>().eq(Question::getQuizBankId, id));
        }
        quizBankMapper.deleteBatchIds(ids);
    }

    @Override
    @Transactional
    public Question addQuestion(Question question) {
        // 获取当前题库的最大排序号
        List<Question> questions = questionMapper.selectList(
            new LambdaQueryWrapper<Question>()
                .eq(Question::getQuizBankId, question.getQuizBankId())
                .orderByDesc(Question::getSortOrder)
                .last("LIMIT 1")
        );
        int maxOrder = questions.isEmpty() ? 0 : questions.get(0).getSortOrder() + 1;
        question.setSortOrder(maxOrder);

        questionMapper.insert(question);

        // 更新题库的题目数量
        updateQuestionCount(question.getQuizBankId());
        return question;
    }

    @Override
    @Transactional
    public Question updateQuestion(Question question) {
        questionMapper.updateById(question);
        return question;
    }

    @Override
    @Transactional
    public void deleteQuestion(Long id) {
        Question question = questionMapper.selectById(id);
        if (question != null) {
            questionMapper.deleteById(id);
            // 更新题库的题目数量
            updateQuestionCount(question.getQuizBankId());
        }
    }

    private void updateQuestionCount(Long quizBankId) {
        Long count = questionMapper.selectCount(
            new LambdaQueryWrapper<Question>().eq(Question::getQuizBankId, quizBankId)
        );
        QuizBank quizBank = quizBankMapper.selectById(quizBankId);
        if (quizBank != null) {
            quizBank.setQuestionCount(count.intValue());
            quizBankMapper.updateById(quizBank);
        }
    }
}
