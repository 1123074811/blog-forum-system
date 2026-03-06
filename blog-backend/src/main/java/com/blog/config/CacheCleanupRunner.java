package com.blog.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 应用启动时清理缓存
 * 注意：生产环境请谨慎使用，建议只在开发环境启用
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CacheCleanupRunner implements CommandLineRunner {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void run(String... args) {
        try {
            // 清理所有缓存（开发环境）
            // 生产环境建议只清理特定前缀的缓存
            log.warn("清理 Redis 缓存...");

            // 方式1：清理所有缓存
            // redisTemplate.getConnectionFactory().getConnection().flushDb();

            // 方式2：只清理特定前缀的缓存（推荐）
            redisTemplate.delete(redisTemplate.keys("*"));

            log.info("Redis 缓存清理完成");
        } catch (Exception e) {
            log.error("清理 Redis 缓存失败", e);
        }
    }
}
