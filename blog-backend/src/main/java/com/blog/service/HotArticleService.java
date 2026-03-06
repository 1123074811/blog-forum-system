package com.blog.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.pojo.entity.Article;

/**
 * 热门文章服务接口
 */
public interface HotArticleService {

    /**
     * 更新热门文章排行榜（定时任务调用）
     */
    void updateHotArticles();

    /**
     * 从缓存获取热门文章列表
     */
    Page<Article> getHotArticlesFromCache(int page, int limit, Long categoryId, String search, Long currentUserId);

    /**
     * 更新单篇文章的热度分数
     */
    void updateArticleHotScore(Long articleId);
}
