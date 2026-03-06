package com.blog.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.pojo.entity.Question;
import com.blog.pojo.entity.QuizBank;
import java.util.List;

public interface QuizService {
    QuizBank importQuiz(String jsonContent, Long userId);
    List<QuizBank> getUserQuizBanks(Long userId);
    List<QuizBank> getPublicQuizBanks();
    QuizBank getQuizBankWithQuestions(Long id);
    Page<Question> getQuestionsByPage(Long quizBankId, int page, int size);
    void deleteQuizBank(Long id, Long userId);
    List<QuizBank> getAllQuizBanks();
    void adminDelete(Long id);
    QuizBank adminUpdate(Long id, QuizBank quizBank);
    void togglePublic(Long id, Long userId, Boolean isPublic);
    void save(QuizBank quizBank);
    QuizBank getById(Long id);
    void removeByIds(List<Long> ids);

    // 题目管理方法
    Question addQuestion(Question question);
    Question updateQuestion(Question question);
    void deleteQuestion(Long id);
}
