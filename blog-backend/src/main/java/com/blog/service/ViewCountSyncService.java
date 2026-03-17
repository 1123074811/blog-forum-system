package com.blog.service;

/**
 * 浏览量同步服务
 * 定时将 Redis 中的浏览量同步到数据库
 */
public interface ViewCountSyncService {

    /**
     * 同步所有文章的浏览量到数据库
     */
    void syncAllViewCounts();

    /**
     * 同步单篇文章的浏览量到数据库
     */
    void syncViewCount(Long articleId);

    /**
     * 批量递增文章浏览量（Redis pipeline）
     */
    void batchIncrementViewCount(java.util.List<Long> articleIds);
}
