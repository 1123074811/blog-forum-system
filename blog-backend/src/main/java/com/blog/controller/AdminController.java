package com.blog.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.annotation.AuditLog;
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
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
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
    private final SiteVisitService siteVisitService;
    private final com.blog.mapper.ArticleFavoriteMapper articleFavoriteMapper;
    private final com.blog.mapper.ArticleMapper articleMapper;
    private final com.blog.mapper.CommentMapper commentMapper;
    private final com.blog.mapper.MediaMapper mediaMapper;
    private final com.blog.mapper.FileMapper fileMapper;
    private final com.blog.mapper.UserMapper userMapper;
    private final com.blog.mapper.SiteVisitMapper siteVisitMapper;
    private final com.blog.mapper.IpBlacklistMapper ipBlacklistMapper;
    private final ChatWebSocketHandler chatWebSocketHandler;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @GetMapping("/ping")
    public ApiResponse<String> ping() {
        return ApiResponse.success("ok");
    }

    @GetMapping("/online-status")
    public ApiResponse<Map<String, Object>> getOnlineStatus() {
        String todayStr = DateUtil.now().substring(0, 10);
        SiteVisit todayVisit = siteVisitService.getOne(new LambdaQueryWrapper<SiteVisit>().eq(SiteVisit::getVisitDate, todayStr));
        long activeIpBans = ipBlacklistMapper.selectCount(
                new LambdaQueryWrapper<IpBlacklist>().eq(IpBlacklist::getStatus, 1));

        Map<String, Object> result = new HashMap<>();
        result.put("totalOnlineUsers", chatWebSocketHandler.getOnlineUserCount());
        result.put("todayNewUsers", todayVisit != null && todayVisit.getNewUsers() != null ? todayVisit.getNewUsers() : 0);
        result.put("todayNewArticles", todayVisit != null && todayVisit.getNewArticles() != null ? todayVisit.getNewArticles() : 0);
        result.put("activeIpBans", (int) activeIpBans);
        return ApiResponse.success(result);
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
        stats.put("totalMedia", Math.toIntExact(mediaMapper.selectCount(null)));
        stats.put("totalFiles", Math.toIntExact(fileMapper.selectCount(null)));
        stats.put("totalOnlineUsers", chatWebSocketHandler.getOnlineUserCount());

        // Operational metrics
        long activeIpBans = ipBlacklistMapper.selectCount(
                new LambdaQueryWrapper<IpBlacklist>().eq(IpBlacklist::getStatus, 1));
        stats.put("activeIpBans", (int) activeIpBans);

        // Visitor Statistics — use SUM query instead of loading all rows
        String todayStr = DateUtil.now().substring(0, 10);
        SiteVisit todayVisit = siteVisitService.getOne(new LambdaQueryWrapper<SiteVisit>().eq(SiteVisit::getVisitDate, todayStr));
        int totalVisitors = siteVisitMapper.sumTotalUv();
        stats.put("totalVisitors", totalVisitors);
        stats.put("todayVisitors", todayVisit != null ? todayVisit.getUv() : 0);
        stats.put("todayNewUsers", todayVisit != null && todayVisit.getNewUsers() != null ? todayVisit.getNewUsers() : 0);
        stats.put("todayNewArticles", todayVisit != null && todayVisit.getNewArticles() != null ? todayVisit.getNewArticles() : 0);

        List<Article> topArticles = articleService.list(new LambdaQueryWrapper<Article>()
                .orderByDesc(Article::getViewCount).last("LIMIT 10"));
        stats.put("viewRanking", topArticles);

        int days = parseRangeDays(range);
        String normalizedGranularity = normalizeGranularity(granularity);
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1L);

        String startStr = startDate.toString();
        String endStr = endDate.plusDays(1).toString();

        // Use SQL COUNT GROUP BY instead of loading all rows into memory
        stats.put("trend", buildTrendFromDb(startStr, endStr, normalizedGranularity));
        stats.put("registrationTrend", buildRegistrationTrendFromDb(startStr, endStr, normalizedGranularity));
        stats.put("articleStatusDistribution", buildArticleStatusFromDb(startStr, endStr));
        stats.put("activeAuthors", buildActiveAuthorsFromDb(startStr, endStr, 5));
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

    private Map<String, Object> buildTrendFromDb(String startStr, String endStr, String granularity) {
        List<Map<String, Object>> articleRows;
        List<Map<String, Object>> commentRows;
        LinkedHashMap<String, Integer> articleCounts = new LinkedHashMap<>();
        LinkedHashMap<String, Integer> commentCounts = new LinkedHashMap<>();

        if ("month".equals(granularity)) {
            articleRows = articleMapper.countByMonthRange(startStr, endStr);
            commentRows = commentMapper.countByMonthRange(startStr, endStr);
            LocalDate start = LocalDate.parse(startStr);
            LocalDate end = LocalDate.parse(endStr).minusDays(1);
            YearMonth current = YearMonth.from(start);
            YearMonth endMonth = YearMonth.from(end);
            while (!current.isAfter(endMonth)) {
                String key = current.toString();
                articleCounts.put(key, 0);
                commentCounts.put(key, 0);
                current = current.plusMonths(1);
            }
            for (Map<String, Object> row : articleRows) {
                articleCounts.put(String.valueOf(row.get("dateKey")), ((Number) row.get("cnt")).intValue());
            }
            for (Map<String, Object> row : commentRows) {
                commentCounts.put(String.valueOf(row.get("dateKey")), ((Number) row.get("cnt")).intValue());
            }
        } else if ("year".equals(granularity)) {
            articleRows = articleMapper.countByYearRange(startStr, endStr);
            commentRows = commentMapper.countByYearRange(startStr, endStr);
            int startYear = LocalDate.parse(startStr).getYear();
            int endYear = LocalDate.parse(endStr).minusDays(1).getYear();
            for (int y = startYear; y <= endYear; y++) {
                String key = String.valueOf(y);
                articleCounts.put(key, 0);
                commentCounts.put(key, 0);
            }
            for (Map<String, Object> row : articleRows) {
                articleCounts.put(String.valueOf(row.get("dateKey")), ((Number) row.get("cnt")).intValue());
            }
            for (Map<String, Object> row : commentRows) {
                commentCounts.put(String.valueOf(row.get("dateKey")), ((Number) row.get("cnt")).intValue());
            }
        } else {
            articleRows = articleMapper.countByDateRange(startStr, endStr);
            commentRows = commentMapper.countByDateRange(startStr, endStr);
            LocalDate start = LocalDate.parse(startStr);
            LocalDate end = LocalDate.parse(endStr).minusDays(1);
            LocalDate current = start;
            while (!current.isAfter(end)) {
                String key = current.toString();
                articleCounts.put(key, 0);
                commentCounts.put(key, 0);
                current = current.plusDays(1);
            }
            for (Map<String, Object> row : articleRows) {
                articleCounts.put(String.valueOf(row.get("dateKey")), ((Number) row.get("cnt")).intValue());
            }
            for (Map<String, Object> row : commentRows) {
                commentCounts.put(String.valueOf(row.get("dateKey")), ((Number) row.get("cnt")).intValue());
            }
        }

        Map<String, Object> trend = new HashMap<>();
        trend.put("labels", new ArrayList<>(articleCounts.keySet()));
        trend.put("articleCounts", new ArrayList<>(articleCounts.values()));
        trend.put("commentCounts", new ArrayList<>(commentCounts.values()));
        return trend;
    }

    private List<Map<String, Object>> buildArticleStatusFromDb(String startStr, String endStr) {
        List<Map<String, Object>> rows = articleMapper.countByStatusInRange(startStr, endStr);
        int published = 0;
        int draft = 0;
        for (Map<String, Object> row : rows) {
            String status = String.valueOf(row.get("status"));
            int cnt = ((Number) row.get("cnt")).intValue();
            if ("published".equalsIgnoreCase(status)) {
                published = cnt;
            } else if ("draft".equalsIgnoreCase(status)) {
                draft = cnt;
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        result.add(Map.of("name", "已发布", "value", published));
        result.add(Map.of("name", "草稿", "value", draft));
        return result;
    }

    private Map<String, Object> buildRegistrationTrendFromDb(String startStr, String endStr, String granularity) {
        List<Map<String, Object>> rows;
        LinkedHashMap<String, Integer> counts = new LinkedHashMap<>();

        if ("month".equals(granularity)) {
            rows = userMapper.countByMonthRange(startStr, endStr);
            LocalDate start = LocalDate.parse(startStr);
            LocalDate end = LocalDate.parse(endStr).minusDays(1);
            YearMonth current = YearMonth.from(start);
            YearMonth endMonth = YearMonth.from(end);
            while (!current.isAfter(endMonth)) {
                counts.put(current.toString(), 0);
                current = current.plusMonths(1);
            }
        } else if ("year".equals(granularity)) {
            rows = userMapper.countByYearRange(startStr, endStr);
            int startYear = LocalDate.parse(startStr).getYear();
            int endYear = LocalDate.parse(endStr).minusDays(1).getYear();
            for (int y = startYear; y <= endYear; y++) {
                counts.put(String.valueOf(y), 0);
            }
        } else {
            rows = userMapper.countByDateRange(startStr, endStr);
            LocalDate start = LocalDate.parse(startStr);
            LocalDate end = LocalDate.parse(endStr).minusDays(1);
            LocalDate current = start;
            while (!current.isAfter(end)) {
                counts.put(current.toString(), 0);
                current = current.plusDays(1);
            }
        }
        for (Map<String, Object> row : rows) {
            counts.put(String.valueOf(row.get("dateKey")), ((Number) row.get("cnt")).intValue());
        }
        Map<String, Object> result = new HashMap<>();
        result.put("labels", new ArrayList<>(counts.keySet()));
        result.put("values", new ArrayList<>(counts.values()));
        return result;
    }

    private List<Map<String, Object>> buildActiveAuthorsFromDb(String startStr, String endStr, int topN) {
        List<Map<String, Object>> rows = articleMapper.countActiveAuthors(startStr, endStr, topN);
        return rows.stream()
                .map(row -> {
                    Long userId = ((Number) row.get("userId")).longValue();
                    User user = userService.getById(userId);
                    String name = user != null
                            ? (user.getNickname() != null && !user.getNickname().isBlank() ? user.getNickname() : user.getUsername())
                            : "用户#" + userId;
                    Map<String, Object> item = new HashMap<>();
                    item.put("userId", userId);
                    item.put("name", name);
                    item.put("count", ((Number) row.get("cnt")).longValue());
                    return item;
                })
                .collect(Collectors.toList());
    }

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

