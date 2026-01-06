package com.blog.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.blog.entity.Article;

public interface ArticleService extends IService<Article> {
    Page<Article> getArticles(int page, int limit, Long categoryId, Long userId, String search, String sort);
    Page<Article> getArticles(int page, int limit, Long categoryId, Long userId, String search, String sort, Long currentUserId);
    void incrementViewCount(Long articleId);
    Article getArticleWithAuthor(Long id);
}
