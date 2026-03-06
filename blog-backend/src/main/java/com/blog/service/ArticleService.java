package com.blog.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.blog.pojo.dto.ArticleRequest;
import com.blog.pojo.entity.Article;

import java.util.List;

public interface ArticleService extends IService<Article> {
    Page<Article> getArticles(int page, int limit, Long categoryId, Long userId, String search, String sort);
    Page<Article> getArticles(int page, int limit, Long categoryId, Long userId, String search, String sort, Long currentUserId);
    void incrementViewCount(Long articleId);
    Article getArticleWithAuthor(Long id);

    /**
     * 获取关注用户的文章列表
     */
    Page<Article> getFollowingArticles(int page, int limit, Long currentUserId);

    /**
     * 创建文章（包含标签关联）
     */
    Article createArticle(ArticleRequest request, Long userId);

    /**
     * 更新文章
     */
    Article updateArticle(Long id, ArticleRequest request, Long userId);

    /**
     * 删除文章
     */
    void deleteArticle(Long id, Long userId);
}
