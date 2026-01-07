package com.blog.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.dto.ApiResponse;
import com.blog.dto.ArticleRequest;
import com.blog.dto.PageResponse;
import com.blog.entity.Article;
import com.blog.entity.ArticleTag;
import com.blog.entity.Follow;
import com.blog.entity.User;
import com.blog.mapper.ArticleTagMapper;
import com.blog.mapper.FollowMapper;
import com.blog.mapper.UserMapper;
import com.blog.service.ArticleService;
import com.blog.service.ZhipuAiService;
import com.blog.util.DateUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import java.util.List;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;
    private final ArticleTagMapper articleTagMapper;
    private final FollowMapper followMapper;
    private final UserMapper userMapper;
    private final ZhipuAiService zhipuAiService;

    @GetMapping
    public ApiResponse<PageResponse<Article>> getArticles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) Long category,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "latest") String sort,
            Authentication auth) {
        Long currentUserId = auth != null && auth.getPrincipal() instanceof Long ? (Long) auth.getPrincipal() : null;
        Page<Article> result = articleService.getArticles(page, limit, category, userId, search, sort, currentUserId);
        return ApiResponse.success(new PageResponse<>(result.getRecords(), result.getTotal(), page, limit));
    }

    @GetMapping("/following")
    public ApiResponse<PageResponse<Article>> getFollowingArticles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            Authentication auth) {
        if (auth == null) {
            return ApiResponse.error("请先登录");
        }
        Long userId = (Long) auth.getPrincipal();
        // 获取关注的用户ID列表
        List<Long> followingIds = followMapper.selectList(
                new LambdaQueryWrapper<Follow>().eq(Follow::getFollowerId, userId)
        ).stream().map(Follow::getFollowingId).toList();

        if (followingIds.isEmpty()) {
            return ApiResponse.success(new PageResponse<>(List.of(), 0L, page, limit));
        }

        // 查询关注用户的文章，按时间倒序
        Page<Article> pageObj = new Page<>(page, limit);
        Page<Article> result = articleService.page(pageObj,
                new LambdaQueryWrapper<Article>()
                        .in(Article::getUserId, followingIds)
                        .eq(Article::getStatus, "published")
                        .orderByDesc(Article::getCreatedAt));
        // 填充用户信息
        List<Article> articles = result.getRecords();
        if (!articles.isEmpty()) {
            Map<Long, User> userMap = userMapper.selectBatchIds(
                    articles.stream().map(Article::getUserId).distinct().toList()
            ).stream().collect(java.util.stream.Collectors.toMap(User::getId, u -> u));
            for (Article article : articles) {
                User user = userMap.get(article.getUserId());
                if (user != null) {
                    article.setAuthorName(user.getUsername());
                    article.setAuthorAvatar(user.getAvatar());
                }
            }
        }
        return ApiResponse.success(new PageResponse<>(result.getRecords(), result.getTotal(), page, limit));
    }

    @GetMapping("/{id}")
    public ApiResponse<Article> getArticle(@PathVariable Long id) {
        Article article = articleService.getArticleWithAuthor(id);
        if (article == null) {
            return ApiResponse.error("Article not found");
        }
        articleService.incrementViewCount(id);
        return ApiResponse.success(article);
    }

    @PostMapping
    public ApiResponse<Article> createArticle(@RequestBody ArticleRequest request, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();

        Article article = new Article();
        article.setUserId(userId);
        article.setTitle(request.getTitle());
        article.setContent(request.getContent());
        article.setCategoryId(request.getCategoryId());
        article.setStatus(request.getStatus() != null ? request.getStatus() : "draft");
        article.setViewCount(0);
        article.setCreatedAt(DateUtil.now());
        article.setUpdatedAt(DateUtil.now());

        articleService.save(article);

        if (request.getTags() != null) {
            for (Long tagId : request.getTags()) {
                ArticleTag at = new ArticleTag();
                at.setArticleId(article.getId());
                at.setTagId(tagId);
                articleTagMapper.insert(at);
            }
        }

        return ApiResponse.success(article);
    }

    @PutMapping("/{id}")
    public ApiResponse<Article> updateArticle(@PathVariable Long id, @RequestBody ArticleRequest request, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        Article article = articleService.getById(id);

        if (article == null) {
            return ApiResponse.error("Article not found");
        }
        if (!article.getUserId().equals(userId)) {
            return ApiResponse.error("Unauthorized");
        }

        if (request.getTitle() != null) article.setTitle(request.getTitle());
        if (request.getContent() != null) article.setContent(request.getContent());
        if (request.getCategoryId() != null) article.setCategoryId(request.getCategoryId());
        if (request.getStatus() != null) article.setStatus(request.getStatus());
        article.setUpdatedAt(DateUtil.now());

        articleService.updateById(article);
        return ApiResponse.success(article);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> deleteArticle(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        Article article = articleService.getById(id);

        if (article == null) {
            return ApiResponse.error("Article not found");
        }
        if (!article.getUserId().equals(userId)) {
            return ApiResponse.error("Unauthorized");
        }

        articleService.removeById(id);
        return ApiResponse.success(true);
    }

    @GetMapping("/{id}/summary")
    public ApiResponse<String> getArticleSummary(@PathVariable Long id) {
        Article article = articleService.getById(id);
        if (article == null) {
            return ApiResponse.error("Article not found");
        }
        String summary = zhipuAiService.generateSummary(article.getContent());
        if (summary == null) {
            return ApiResponse.error("AI 总结生成失败");
        }
        return ApiResponse.success(summary);
    }
}
