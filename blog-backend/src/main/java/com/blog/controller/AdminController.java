package com.blog.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.annotation.AuditLog;
import com.blog.constant.AppConstants;
import com.blog.pojo.dto.ApiResponse;
import com.blog.pojo.entity.*;
import com.blog.service.*;
import com.blog.util.DateUtil;
import com.blog.websocket.ChatWebSocketHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;
    private final ArticleService articleService;
    private final CommentService commentService;
    private final CategoryService categoryService;
    private final TagService tagService;
    private final com.blog.mapper.ArticleFavoriteMapper articleFavoriteMapper;
    private final ChatWebSocketHandler chatWebSocketHandler;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @GetMapping("/ping")
    public ApiResponse<String> ping() {
        return ApiResponse.success("ok");
    }

    // Statistics
    @GetMapping("/statistics")
    public ApiResponse<Map<String, Object>> getStatistics(
            @RequestParam(defaultValue = "30d") String range,
            @RequestParam(defaultValue = "day") String granularity) {
        String cacheKey = "cache:statistics:" + range + ":" + granularity;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof String cachedJson) {
            try {
                Map<String, Object> parsed = objectMapper.readValue(cachedJson, new TypeReference<Map<String, Object>>() {});
                return ApiResponse.success(parsed);
            } catch (Exception ignored) {
                redisTemplate.delete(cacheKey);
            }
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalArticles", articleService.count());
        stats.put("totalCategories", categoryService.count());
        stats.put("totalUsers", userService.count());
        stats.put("totalComments", commentService.count());
        stats.put("totalOnlineUsers", chatWebSocketHandler.getOnlineUserCount());

        List<Article> topArticles = articleService.list(new LambdaQueryWrapper<Article>()
                .orderByDesc(Article::getViewCount).last("LIMIT 10"));
        stats.put("viewRanking", topArticles);

        int days = parseRangeDays(range);
        String normalizedGranularity = normalizeGranularity(granularity);
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1L);

        List<Article> allArticles = articleService.list();
        List<Comment> allComments = commentService.list();

        stats.put("trend", buildTrend(allArticles, allComments, startDate, endDate, normalizedGranularity));
        stats.put("articleStatusDistribution", buildArticleStatusDistribution(allArticles, startDate, endDate));
        stats.put("activeAuthors", buildActiveAuthors(allArticles, startDate, endDate, 5));
        try {
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(stats), 60, TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }

        return ApiResponse.success(stats);
    }

    // User Management
    @GetMapping("/users")
    public ApiResponse<List<User>> getUsers() {
        List<User> users = userService.list();
        users.forEach(u -> u.setPassword(null));
        return ApiResponse.success(users);
    }

    @DeleteMapping("/users/{id}")
    public ApiResponse<Boolean> deleteUser(@PathVariable Long id) {
        userService.removeById(id);
        return ApiResponse.success(true);
    }

    @PutMapping("/users/{id}/ban")
    public ApiResponse<Boolean> banUser(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        Boolean banned = body.get("banned");
        User user = userService.getById(id);
        if (user == null) {
            return ApiResponse.error("用户不存在");
        }
        if ("admin".equals(user.getRole())) {
            return ApiResponse.error("不能封禁管理员");
        }
        user.setBanned(banned);
        userService.updateById(user);
        userService.clearUserCache(user.getId(), user.getUsername());
        return ApiResponse.success(true);
    }

    // Article Management
    @GetMapping("/articles")
    public ApiResponse<List<Article>> getArticles() {
        return ApiResponse.success(articleService.list());
    }

    @DeleteMapping("/articles/{id}")
    public ApiResponse<Boolean> deleteArticle(@PathVariable Long id) {
        articleService.removeById(id);
        return ApiResponse.success(true);
    }

    // Comment Management
    @GetMapping("/comments")
    public ApiResponse<List<Comment>> getComments() {
        return ApiResponse.success(commentService.list());
    }

    @DeleteMapping("/comments/{id}")
    public ApiResponse<Boolean> deleteComment(@PathVariable Long id) {
        commentService.removeById(id);
        return ApiResponse.success(true);
    }

    // Category Management
    @GetMapping("/categories")
    public ApiResponse<List<Category>> getCategories() {
        return ApiResponse.success(categoryService.list());
    }

    @PostMapping("/categories")
    public ApiResponse<Category> createCategory(@RequestBody Category category) {
        category.setCreatedAt(DateUtil.now());
        categoryService.save(category);
        return ApiResponse.success(category);
    }

    @PutMapping("/categories/{id}")
    public ApiResponse<Category> updateCategory(@PathVariable Long id, @RequestBody Category category) {
        category.setId(id);
        categoryService.updateById(category);
        return ApiResponse.success(category);
    }

    @DeleteMapping("/categories/{id}")
    public ApiResponse<Boolean> deleteCategory(@PathVariable Long id) {
        categoryService.removeById(id);
        return ApiResponse.success(true);
    }

    // Tag Management
    @GetMapping("/tags")
    public ApiResponse<List<Tag>> getTags() {
        return ApiResponse.success(tagService.list());
    }

    @PostMapping("/tags")
    public ApiResponse<Tag> createTag(@RequestBody Tag tag) {
        tag.setCreatedAt(DateUtil.now());
        tagService.save(tag);
        return ApiResponse.success(tag);
    }

    @PutMapping("/tags/{id}")
    public ApiResponse<Tag> updateTag(@PathVariable Long id, @RequestBody Tag tag) {
        tag.setId(id);
        tagService.updateById(tag);
        return ApiResponse.success(tag);
    }

    @DeleteMapping("/tags/{id}")
    public ApiResponse<Boolean> deleteTag(@PathVariable Long id) {
        tagService.removeById(id);
        return ApiResponse.success(true);
    }

    // Favorite Management
    @GetMapping("/favorites")
    public ApiResponse<List<ArticleFavorite>> getFavorites(@RequestParam(defaultValue = "1") int page,
                                                           @RequestParam(defaultValue = "20") int limit) {
        Page<ArticleFavorite> pageParam = new Page<>(page, limit);
        Page<ArticleFavorite> result = articleFavoriteMapper.selectPage(pageParam, null);
        
        if (!result.getRecords().isEmpty()) {
            List<Long> userIds = result.getRecords().stream().map(ArticleFavorite::getUserId).distinct().toList();
            List<Long> articleIds = result.getRecords().stream().map(ArticleFavorite::getArticleId).distinct().toList();

            Map<Long, User> userMap = new HashMap<>();
            if (!userIds.isEmpty()) {
                userService.listByIds(userIds).forEach(u -> userMap.put(u.getId(), u));
            }

            Map<Long, Article> articleMap = new HashMap<>();
            if (!articleIds.isEmpty()) {
                articleService.listByIds(articleIds).forEach(a -> articleMap.put(a.getId(), a));
            }

            for (ArticleFavorite fav : result.getRecords()) {
                User user = userMap.get(fav.getUserId());
                if (user != null) {
                    fav.setUsername(user.getUsername());
                }
                Article article = articleMap.get(fav.getArticleId());
                if (article != null) {
                    fav.setArticleTitle(article.getTitle());
                }
            }
        }
        return ApiResponse.success(result.getRecords());
    }

    @DeleteMapping("/favorites/{id}")
    public ApiResponse<Boolean> deleteFavorite(@PathVariable Long id) {
        articleFavoriteMapper.deleteById(id);
        return ApiResponse.success(true);
    }

    // Batch Delete APIs
    @PostMapping("/users/batch-delete")
    @AuditLog(module = "User Management", operation = "Batch Delete Users")
    public ApiResponse<Boolean> batchDeleteUsers(@RequestBody Map<String, Object> body) {
        if (!validateStepUpToken(body, "batch_delete_users")) {
            return ApiResponse.error("二次验证失败或已过期");
        }
        List<Long> ids = toIdList(body.get("ids"));
        if (ids != null && !ids.isEmpty()) {
            userService.removeByIds(ids);
        }
        return ApiResponse.success(true);
    }

    @PostMapping("/articles/batch-delete")
    @AuditLog(module = "Article Management", operation = "Batch Delete Articles")
    public ApiResponse<Boolean> batchDeleteArticles(@RequestBody Map<String, Object> body) {
        if (!validateStepUpToken(body, "batch_delete_articles")) {
            return ApiResponse.error("二次验证失败或已过期");
        }
        List<Long> ids = toIdList(body.get("ids"));
        if (ids != null && !ids.isEmpty()) {
            articleService.removeByIds(ids);
        }
        return ApiResponse.success(true);
    }

    @PostMapping("/comments/batch-delete")
    @AuditLog(module = "Comment Management", operation = "Batch Delete Comments")
    public ApiResponse<Boolean> batchDeleteComments(@RequestBody Map<String, Object> body) {
        if (!validateStepUpToken(body, "batch_delete_comments")) {
            return ApiResponse.error("二次验证失败或已过期");
        }
        List<Long> ids = toIdList(body.get("ids"));
        if (ids != null && !ids.isEmpty()) {
            commentService.removeByIds(ids);
        }
        return ApiResponse.success(true);
    }

    @PostMapping("/categories/batch-delete")
    @AuditLog(module = "Category Management", operation = "Batch Delete Categories")
    public ApiResponse<Boolean> batchDeleteCategories(@RequestBody Map<String, Object> body) {
        if (!validateStepUpToken(body, "batch_delete_categories")) {
            return ApiResponse.error("二次验证失败或已过期");
        }
        List<Long> ids = toIdList(body.get("ids"));
        if (ids != null && !ids.isEmpty()) {
            categoryService.removeByIds(ids);
        }
        return ApiResponse.success(true);
    }

    @PostMapping("/tags/batch-delete")
    @AuditLog(module = "Tag Management", operation = "Batch Delete Tags")
    public ApiResponse<Boolean> batchDeleteTags(@RequestBody Map<String, Object> body) {
        if (!validateStepUpToken(body, "batch_delete_tags")) {
            return ApiResponse.error("二次验证失败或已过期");
        }
        List<Long> ids = toIdList(body.get("ids"));
        if (ids != null && !ids.isEmpty()) {
            tagService.removeByIds(ids);
        }
        return ApiResponse.success(true);
    }

    @PostMapping("/favorites/batch-delete")
    @AuditLog(module = "Favorite Management", operation = "Batch Delete Favorites")
    public ApiResponse<Boolean> batchDeleteFavorites(@RequestBody Map<String, Object> body) {
        if (!validateStepUpToken(body, "batch_delete_favorites")) {
            return ApiResponse.error("二次验证失败或已过期");
        }
        List<Long> ids = toIdList(body.get("ids"));
        if (ids != null && !ids.isEmpty()) {
            articleFavoriteMapper.deleteBatchIds(ids);
        }
        return ApiResponse.success(true);
    }

    private int parseRangeDays(String range) {
        return switch (range == null ? "" : range.toLowerCase()) {
            case "7d" -> 7;
            case "90d" -> 90;
            default -> 30;
        };
    }

    private String normalizeGranularity(String granularity) {
        String g = granularity == null ? "day" : granularity.toLowerCase();
        if ("month".equals(g) || "year".equals(g)) {
            return g;
        }
        return "day";
    }

    private LocalDate parseDateSafe(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).toLocalDate();
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDate.parse(value.substring(0, 10), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception ignored) {
        }
        return null;
    }

    private Map<String, Object> buildTrend(List<Article> allArticles, List<Comment> allComments,
                                           LocalDate startDate, LocalDate endDate, String granularity) {
        LinkedHashMap<String, Integer> articleCounts = new LinkedHashMap<>();
        LinkedHashMap<String, Integer> commentCounts = new LinkedHashMap<>();

        if ("month".equals(granularity)) {
            YearMonth start = YearMonth.from(startDate);
            YearMonth end = YearMonth.from(endDate);
            YearMonth current = start;
            while (!current.isAfter(end)) {
                String key = current.toString();
                articleCounts.put(key, 0);
                commentCounts.put(key, 0);
                current = current.plusMonths(1);
            }
            for (Article a : allArticles) {
                LocalDate d = parseDateSafe(a.getCreatedAt());
                if (d != null && !d.isBefore(startDate) && !d.isAfter(endDate)) {
                    String key = YearMonth.from(d).toString();
                    articleCounts.computeIfPresent(key, (k, v) -> v + 1);
                }
            }
            for (Comment c : allComments) {
                LocalDate d = parseDateSafe(c.getCreatedAt());
                if (d != null && !d.isBefore(startDate) && !d.isAfter(endDate)) {
                    String key = YearMonth.from(d).toString();
                    commentCounts.computeIfPresent(key, (k, v) -> v + 1);
                }
            }
        } else if ("year".equals(granularity)) {
            int startYear = startDate.getYear();
            int endYear = endDate.getYear();
            for (int y = startYear; y <= endYear; y++) {
                String key = String.valueOf(y);
                articleCounts.put(key, 0);
                commentCounts.put(key, 0);
            }
            for (Article a : allArticles) {
                LocalDate d = parseDateSafe(a.getCreatedAt());
                if (d != null && !d.isBefore(startDate) && !d.isAfter(endDate)) {
                    String key = String.valueOf(d.getYear());
                    articleCounts.computeIfPresent(key, (k, v) -> v + 1);
                }
            }
            for (Comment c : allComments) {
                LocalDate d = parseDateSafe(c.getCreatedAt());
                if (d != null && !d.isBefore(startDate) && !d.isAfter(endDate)) {
                    String key = String.valueOf(d.getYear());
                    commentCounts.computeIfPresent(key, (k, v) -> v + 1);
                }
            }
        } else {
            LocalDate current = startDate;
            while (!current.isAfter(endDate)) {
                String key = current.toString();
                articleCounts.put(key, 0);
                commentCounts.put(key, 0);
                current = current.plusDays(1);
            }
            for (Article a : allArticles) {
                LocalDate d = parseDateSafe(a.getCreatedAt());
                if (d != null && !d.isBefore(startDate) && !d.isAfter(endDate)) {
                    String key = d.toString();
                    articleCounts.computeIfPresent(key, (k, v) -> v + 1);
                }
            }
            for (Comment c : allComments) {
                LocalDate d = parseDateSafe(c.getCreatedAt());
                if (d != null && !d.isBefore(startDate) && !d.isAfter(endDate)) {
                    String key = d.toString();
                    commentCounts.computeIfPresent(key, (k, v) -> v + 1);
                }
            }
        }

        Map<String, Object> trend = new HashMap<>();
        trend.put("labels", new ArrayList<>(articleCounts.keySet()));
        trend.put("articleCounts", new ArrayList<>(articleCounts.values()));
        trend.put("commentCounts", new ArrayList<>(commentCounts.values()));
        return trend;
    }

    private List<Map<String, Object>> buildArticleStatusDistribution(List<Article> allArticles,
                                                                     LocalDate startDate, LocalDate endDate) {
        int published = 0;
        int draft = 0;
        for (Article article : allArticles) {
            LocalDate date = parseDateSafe(article.getCreatedAt());
            if (date == null || date.isBefore(startDate) || date.isAfter(endDate)) {
                continue;
            }
            if ("published".equalsIgnoreCase(article.getStatus())) {
                published++;
            } else if ("draft".equalsIgnoreCase(article.getStatus())) {
                draft++;
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        result.add(Map.of("name", "已发布", "value", published));
        result.add(Map.of("name", "草稿", "value", draft));
        return result;
    }

    private List<Map<String, Object>> buildActiveAuthors(List<Article> allArticles, LocalDate startDate,
                                                         LocalDate endDate, int topN) {
        Map<Long, Long> userArticleCount = allArticles.stream()
                .filter(a -> {
                    LocalDate d = parseDateSafe(a.getCreatedAt());
                    return d != null && !d.isBefore(startDate) && !d.isAfter(endDate);
                })
                .collect(Collectors.groupingBy(Article::getUserId, Collectors.counting()));

        return userArticleCount.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(topN)
                .map(entry -> {
                    User user = userService.getById(entry.getKey());
                    String name = user != null
                            ? (user.getNickname() != null && !user.getNickname().isBlank() ? user.getNickname() : user.getUsername())
                            : "用户#" + entry.getKey();
                    Map<String, Object> item = new HashMap<>();
                    item.put("userId", entry.getKey());
                    item.put("name", name);
                    item.put("count", entry.getValue());
                    return item;
                })
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private List<Long> toIdList(Object rawIds) {
        if (!(rawIds instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .map(v -> {
                    if (v instanceof Number n) {
                        return n.longValue();
                    }
                    try {
                        return Long.parseLong(String.valueOf(v));
                    } catch (Exception ignored) {
                        return null;
                    }
                })
                .filter(v -> v != null)
                .collect(Collectors.toList());
    }

    private boolean validateStepUpToken(Map<String, Object> body, String operation) {
        Object token = body.get("stepUpToken");
        if (!(token instanceof String tokenText) || tokenText.isBlank()) {
            return false;
        }

        Long adminId = com.blog.context.BaseContext.getCurrentId();
        if (adminId == null) {
            return false;
        }

        String key = "stepup:token:" + adminId + ":" + operation;
        Object stored = redisTemplate.opsForValue().get(key);
        if (!(stored instanceof String storedText) || !tokenText.equals(storedText)) {
            return false;
        }
        redisTemplate.delete(key);
        return true;
    }
}

