package com.blog.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.constant.AppConstants;
import com.blog.dto.ApiResponse;
import com.blog.entity.*;
import com.blog.service.*;
import com.blog.util.DateUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final ArticleService articleService;
    private final CommentService commentService;
    private final CategoryService categoryService;
    private final TagService tagService;

    // Statistics
    @GetMapping("/statistics")
    public ApiResponse<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalArticles", articleService.count());
        stats.put("totalCategories", categoryService.count());
        stats.put("totalUsers", userService.count());
        stats.put("totalComments", commentService.count());

        List<Article> topArticles = articleService.list(new LambdaQueryWrapper<Article>()
                .orderByDesc(Article::getViewCount).last("LIMIT " + AppConstants.HOT_ARTICLE_LIMIT));
        stats.put("viewRanking", topArticles);

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
}
