package com.blog.service;

/**
 * Redis 数据预热服务
 * 在应用启动时将数据库中的数据加载到Redis
 */
public interface RedisWarmupService {
    
    /**
     * 预热文章浏览量数据
     */
    void warmupViewCounts();
    
    /**
     * 预热所有缓存数据
     */
    void warmupAllCache();
}
