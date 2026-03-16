package com.blog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.annotation.RateLimit;
import com.blog.converter.ArticleConverter;
import com.blog.pojo.dto.ApiResponse;
import com.blog.pojo.dto.ArticleRequest;
import com.blog.pojo.dto.CrawlRequest;
import com.blog.pojo.dto.CrawlResponse;
import com.blog.pojo.dto.PageResponse;
import com.blog.pojo.entity.Article;
import com.blog.exception.BusinessException;
import com.blog.exception.ErrorCode;
import com.blog.service.ArticleService;
import com.blog.service.ArticleCrawlerService;
import com.blog.service.ZhipuAiService;
import com.blog.pojo.vo.ArticleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
@Validated
public class ArticleController extends BaseController {

    private final ArticleService articleService;
    private final ArticleConverter articleConverter;
    private final ZhipuAiService zhipuAiService;
    private final ArticleCrawlerService articleCrawlerService;

    @GetMapping
    public ApiResponse<PageResponse<ArticleVO>> getArticles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) Long category,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "latest") String sort,
            Authentication auth) {
        Long currentUserId = getCurrentUserIdOptional(auth);
        Page<Article> result = articleService.getArticles(page, limit, category, userId, search, sort, currentUserId);
        PageResponse<ArticleVO> response = new PageResponse<>(
                articleConverter.toVOList(result.getRecords()),
                result.getTotal(),
                page,
                limit
        );
        return ApiResponse.success(response);
    }

    @GetMapping("/following")
    public ApiResponse<PageResponse<ArticleVO>> getFollowingArticles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            Authentication auth) {
        Long userId = getCurrentUserId(auth);
        Page<Article> result = articleService.getFollowingArticles(page, limit, userId);
        PageResponse<ArticleVO> response = new PageResponse<>(
                articleConverter.toVOList(result.getRecords()),
                result.getTotal(),
                page,
                limit
        );
        return ApiResponse.success(response);
    }

    @GetMapping("/{id}")
    public ApiResponse<ArticleVO> getArticle(@PathVariable Long id) {
        Article article = articleService.getArticleWithAuthor(id);
        if (article == null) {
            throw new BusinessException(ErrorCode.ARTICLE_NOT_FOUND);
        }
        articleService.incrementViewCount(id);
        return ApiResponse.success(articleConverter.toVO(article));
    }

    @PostMapping
    @RateLimit(key = "article:create", count = 5, time = 3600, limitType = RateLimit.LimitType.USER, message = "发文过于频繁，每小时最多5篇")
    public ApiResponse<ArticleVO> createArticle(@Valid @RequestBody ArticleRequest request, Authentication auth) {
        Long userId = getCurrentUserId(auth);
        Article article = articleService.createArticle(request, userId);
        return ApiResponse.success(articleConverter.toVO(article));
    }

    @PutMapping("/{id}")
    public ApiResponse<ArticleVO> updateArticle(@PathVariable Long id, @Valid @RequestBody ArticleRequest request, Authentication auth) {
        Long userId = getCurrentUserId(auth);
        Article article = articleService.updateArticle(id, request, userId);
        return ApiResponse.success(articleConverter.toVO(article));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteArticle(@PathVariable Long id, Authentication auth) {
        Long userId = getCurrentUserId(auth);
        articleService.deleteArticle(id, userId);
        return ApiResponse.success(null);
    }

    @GetMapping("/{id}/summary")
    public ApiResponse<String> getArticleSummary(@PathVariable Long id) {
        Article article = articleService.getById(id);
        if (article == null) {
            throw new BusinessException(ErrorCode.ARTICLE_NOT_FOUND);
        }
        String summary = zhipuAiService.generateSummary(article.getContent());
        if (summary == null) {
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);
        }
        return ApiResponse.success(summary);
    }

    @PostMapping("/crawl")
    @RateLimit(key = "crawl", count = 20, time = 86400, limitType = RateLimit.LimitType.USER, message = "每日爬取限额已用完")
    public ApiResponse<CrawlResponse> crawlArticle(@Valid @RequestBody CrawlRequest request) {
        if (request.getUrl() == null || request.getUrl().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请提供文章链接");
        }
        try {
            CrawlResponse response = articleCrawlerService.crawlArticle(request.getUrl());
            return ApiResponse.success(response);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.CRAWL_SERVICE_ERROR, e.getMessage());
        }
    }
}
