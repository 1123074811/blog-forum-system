package com.blog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.dto.ApiResponse;
import com.blog.dto.QuizImportRequest;
import com.blog.entity.Question;
import com.blog.entity.QuizBank;
import com.blog.service.MinioService;
import com.blog.service.QuizService;
import com.blog.util.DateUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;
    private final MinioService minioService;

    @PostMapping("/import")
    public ApiResponse<QuizBank> importQuiz(@RequestBody QuizImportRequest request, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        QuizBank quizBank = quizService.importQuiz(request.getContent(), userId);
        return ApiResponse.success(quizBank);
    }

    @PostMapping("/upload")
    public ApiResponse<QuizBank> uploadFile(@RequestParam("file") MultipartFile file,
                                            @RequestParam("title") String title,
                                            Authentication auth) throws Exception {
        Long userId = (Long) auth.getPrincipal();
        String originalName = file.getOriginalFilename();
        String ext = originalName.substring(originalName.lastIndexOf(".") + 1).toLowerCase();

        // 检查是否为JSON文件且符合题目格式
        if ("json".equals(ext)) {
            String content = new String(file.getBytes(), "UTF-8");
            if (content.contains("\"questions\"") && content.contains("\"answer\"")) {
                QuizBank quizBank = quizService.importQuiz(content, userId);
                return ApiResponse.success(quizBank);
            }
        }

        // 上传到 MinIO
        String fileUrl = minioService.upload(file, "quiz");

        QuizBank quizBank = new QuizBank();
        quizBank.setUserId(userId);
        quizBank.setTitle(title);
        quizBank.setType("file");
        quizBank.setFilePath(fileUrl);
        quizBank.setFileType(ext);
        quizBank.setQuestionCount(0);
        quizBank.setIsPublic(false);
        quizBank.setCreatedAt(DateUtil.now());
        quizService.save(quizBank);

        return ApiResponse.success(quizBank);
    }

    @GetMapping
    public ApiResponse<List<QuizBank>> getQuizBanks(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.success(quizService.getUserQuizBanks(userId));
    }

    @GetMapping("/{id}")
    public ApiResponse<QuizBank> getQuizBank(@PathVariable Long id) {
        QuizBank quizBank = quizService.getQuizBankWithQuestions(id);
        if (quizBank == null) {
            return ApiResponse.error("题库不存在");
        }
        return ApiResponse.success(quizBank);
    }

    @GetMapping("/{id}/questions")
    public ApiResponse<Page<Question>> getQuestionsByPage(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.success(quizService.getQuestionsByPage(id, page, size));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> deleteQuizBank(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        quizService.deleteQuizBank(id, userId);
        return ApiResponse.success(true);
    }

    @PutMapping("/{id}/public")
    public ApiResponse<Boolean> togglePublic(@PathVariable Long id, @RequestParam Boolean isPublic, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        quizService.togglePublic(id, userId, isPublic);
        return ApiResponse.success(true);
    }

    @GetMapping("/public")
    public ApiResponse<List<QuizBank>> getPublicQuizBanks() {
        return ApiResponse.success(quizService.getPublicQuizBanks());
    }

    @GetMapping("/admin/all")
    public ApiResponse<List<QuizBank>> getAllQuizBanks() {
        return ApiResponse.success(quizService.getAllQuizBanks());
    }

    @DeleteMapping("/admin/{id}")
    public ApiResponse<Boolean> adminDelete(@PathVariable Long id) {
        quizService.adminDelete(id);
        return ApiResponse.success(true);
    }

    @PutMapping("/admin/{id}")
    public ApiResponse<QuizBank> adminUpdate(@PathVariable Long id, @RequestBody QuizBank quizBank) {
        return ApiResponse.success(quizService.adminUpdate(id, quizBank));
    }

    @PostMapping("/admin/batch-delete")
    public ApiResponse<Boolean> batchDelete(@RequestBody Map<String, List<Long>> body) {
        List<Long> ids = body.get("ids");
        if (ids != null && !ids.isEmpty()) {
            quizService.removeByIds(ids);
        }
        return ApiResponse.success(true);
    }
}
