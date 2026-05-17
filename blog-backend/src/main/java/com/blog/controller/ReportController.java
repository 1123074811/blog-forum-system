package com.blog.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.annotation.AuditLog;
import com.blog.context.BaseContext;
import com.blog.pojo.dto.ApiResponse;
import com.blog.pojo.entity.Article;
import com.blog.pojo.entity.Comment;
import com.blog.pojo.entity.Report;
import com.blog.pojo.entity.User;
import com.blog.service.ArticleService;
import com.blog.service.CommentService;
import com.blog.service.ReportService;
import com.blog.service.UserService;
import com.blog.util.DateUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequiredArgsConstructor
public class ReportController extends BaseController {

    private static final Set<String> ALLOWED_TARGET_TYPES = Set.of("article", "comment");
    private static final Set<String> ALLOWED_REASONS = Set.of(
            "spam", "porn", "violence", "illegal", "harassment", "misinformation", "copyright", "other"
    );
    private static final Set<String> ALLOWED_ACTIONS = Set.of("delete_content", "ban_user", "dismiss");

    private final ReportService reportService;
    private final ArticleService articleService;
    private final CommentService commentService;
    private final UserService userService;

    @PostMapping("/api/reports")
    public ApiResponse<Report> createReport(@RequestBody Map<String, Object> body, Authentication auth) {
        Long userId = getCurrentUserId(auth);
        String targetType = cleanString(body.get("targetType"), 20);
        Long targetId = toLong(body.get("targetId"));
        String reason = cleanString(body.get("reason"), 50);
        String description = cleanString(body.get("description"), 1000);

        if (!ALLOWED_TARGET_TYPES.contains(targetType) || targetId == null || targetId <= 0) {
            return ApiResponse.error("举报目标不合法");
        }
        if (!ALLOWED_REASONS.contains(reason)) {
            return ApiResponse.error("举报原因不合法");
        }
        if (!targetExists(targetType, targetId)) {
            return ApiResponse.error("举报目标不存在");
        }

        Report report = new Report();
        report.setReporterId(userId);
        report.setTargetType(targetType);
        report.setTargetId(targetId);
        report.setReason(reason);
        report.setDescription(description);
        report.setStatus("pending");
        report.setCreatedAt(DateUtil.now());

        // Prevent duplicate pending reports for the same target by the same user
        long existing = reportService.count(new LambdaQueryWrapper<Report>()
                .eq(Report::getReporterId, userId)
                .eq(Report::getTargetType, targetType)
                .eq(Report::getTargetId, targetId)
                .eq(Report::getStatus, "pending"));
        if (existing > 0) {
            return ApiResponse.error("您已举报过该内容，请等待处理");
        }

        reportService.save(report);
        return ApiResponse.success(report);
    }

    @GetMapping("/api/admin/reports")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, Object>> getReports(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "pending") String status) {

        Page<Report> pageParam = new Page<>(page, limit);
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<Report>()
                .eq(Report::getStatus, status)
                .orderByDesc(Report::getCreatedAt);
        Page<Report> result = reportService.page(pageParam, wrapper);

        if (!result.getRecords().isEmpty()) {
            List<Long> reporterIds = result.getRecords().stream().map(Report::getReporterId).distinct().toList();
            Map<Long, String> userMap = new HashMap<>();
            if (!reporterIds.isEmpty()) {
                userService.listByIds(reporterIds).forEach(u -> userMap.put(u.getId(), u.getUsername()));
            }

            for (Report r : result.getRecords()) {
                r.setReporterName(userMap.getOrDefault(r.getReporterId(), "未知用户"));
                if (r.getHandledBy() != null) {
                    User handler = userService.getById(r.getHandledBy());
                    r.setHandlerName(handler != null ? handler.getUsername() : null);
                }
                // Attach target info
                if ("article".equals(r.getTargetType())) {
                    Article article = articleService.getById(r.getTargetId());
                    if (article != null) {
                        r.setTargetTitle(article.getTitle());
                        String content = article.getContent();
                        r.setTargetContent(content != null && content.length() > 200
                                ? content.substring(0, 200) + "..." : content);
                    } else {
                        r.setTargetTitle("(文章已删除)");
                    }
                } else if ("comment".equals(r.getTargetType())) {
                    Comment comment = commentService.getById(r.getTargetId());
                    if (comment != null) {
                        String c = comment.getContent();
                        r.setTargetContent(c != null && c.length() > 200
                                ? c.substring(0, 200) + "..." : c);
                        r.setTargetTitle("评论 #" + r.getTargetId());
                    } else {
                        r.setTargetTitle("(评论已删除)");
                    }
                }
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("data", result.getRecords());
        response.put("total", result.getTotal());

        // Count by status
        long pendingCount = reportService.count(new LambdaQueryWrapper<Report>().eq(Report::getStatus, "pending"));
        long resolvedCount = reportService.count(new LambdaQueryWrapper<Report>().eq(Report::getStatus, "resolved"));
        long dismissedCount = reportService.count(new LambdaQueryWrapper<Report>().eq(Report::getStatus, "dismissed"));
        response.put("pendingCount", pendingCount);
        response.put("resolvedCount", resolvedCount);
        response.put("dismissedCount", dismissedCount);

        return ApiResponse.success(response);
    }

    @PutMapping("/api/admin/reports/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(module = "Content Moderation", operation = "Handle Report")
    public ApiResponse<Report> handleReport(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Report report = reportService.getById(id);
        if (report == null) {
            return ApiResponse.error("举报不存在");
        }
        if (!"pending".equals(report.getStatus())) {
            return ApiResponse.error("该举报已处理");
        }

        Long adminId = BaseContext.getCurrentId();
        String action = cleanString(body.getOrDefault("action", "dismiss"), 50);
        String note = cleanString(body.get("note"), 1000);
        if (!ALLOWED_ACTIONS.contains(action)) {
            return ApiResponse.error("处理动作不合法");
        }

        // If action is "delete_content", also delete the reported content
        if ("delete_content".equals(action)) {
            if ("article".equals(report.getTargetType())) {
                articleService.removeById(report.getTargetId());
            } else if ("comment".equals(report.getTargetType())) {
                commentService.removeById(report.getTargetId());
            }
            report.setStatus("resolved");
        } else if ("ban_user".equals(action)) {
            // Ban the content author
            Long authorId = resolveTargetAuthorId(report.getTargetType(), report.getTargetId());
            if (authorId != null) {
                User user = userService.getById(authorId);
                if (user != null && !"admin".equalsIgnoreCase(user.getRole())) {
                    user.setBanned(true);
                    userService.updateById(user);
                    userService.clearUserCache(user.getId(), user.getUsername());
                }
            }
            report.setStatus("resolved");
        } else if ("dismiss".equals(action)) {
            report.setStatus("dismissed");
        }

        report.setHandledBy(adminId);
        report.setHandledAt(DateUtil.now());
        report.setHandleNote(note);
        reportService.updateById(report);

        return ApiResponse.success(report);
    }

    private boolean targetExists(String targetType, Long targetId) {
        if ("article".equals(targetType)) {
            return articleService.getById(targetId) != null;
        }
        if ("comment".equals(targetType)) {
            return commentService.getById(targetId) != null;
        }
        return false;
    }

    private Long resolveTargetAuthorId(String targetType, Long targetId) {
        if ("article".equals(targetType)) {
            Article article = articleService.getById(targetId);
            return article == null ? null : article.getUserId();
        }
        if ("comment".equals(targetType)) {
            Comment comment = commentService.getById(targetId);
            return comment == null ? null : comment.getUserId();
        }
        return null;
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return value == null ? null : Long.parseLong(String.valueOf(value));
        } catch (Exception ignored) {
            return null;
        }
    }

    private String cleanString(Object value, int maxLength) {
        if (!(value instanceof String text)) {
            return "";
        }
        String trimmed = text.trim();
        return trimmed.length() > maxLength ? trimmed.substring(0, maxLength) : trimmed;
    }
}
