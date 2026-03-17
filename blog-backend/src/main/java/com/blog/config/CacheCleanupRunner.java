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
            redisTemplate.delete(redisTemplate.keys("*"));
            log.info("Redis 缓存已清理");
        } catch (Exception e) {
            log.error("清理 Redis 缓存失败", e);
        }
    }
}
