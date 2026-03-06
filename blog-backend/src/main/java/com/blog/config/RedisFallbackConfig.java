package com.blog.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.RedisConnectionFailureException;

/**
 * Redis 降级配置
 * 当 Redis 不可用时，不影响核心业务功能
 */
@Slf4j
@Configuration
public class RedisFallbackConfig implements CachingConfigurer {

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                handleRedisError(exception, "获取缓存", cache.getName(), key);
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                handleRedisError(exception, "写入缓存", cache.getName(), key);
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                handleRedisError(exception, "删除缓存", cache.getName(), key);
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                handleRedisError(exception, "清空缓存", cache.getName(), null);
            }

            private void handleRedisError(RuntimeException exception, String operation, String cacheName, Object key) {
                if (exception instanceof RedisConnectionFailureException) {
                    log.warn("Redis 连接失败，{}操作降级。缓存名: {}, Key: {}", operation, cacheName, key);
                } else {
                    log.error("Redis {}操作失败。缓存名: {}, Key: {}", operation, cacheName, key, exception);
                }
                // 不抛出异常，让业务继续执行
            }
        };
    }
}
