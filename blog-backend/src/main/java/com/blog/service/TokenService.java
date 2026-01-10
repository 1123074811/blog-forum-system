package com.blog.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String TOKEN_PREFIX = "token:";

    // 存储 token 到 Redis
    public void saveToken(String token, Long userId, long expirationMs) {
        String key = TOKEN_PREFIX + token;
        redisTemplate.opsForValue().set(key, userId, expirationMs, TimeUnit.MILLISECONDS);
    }

    // 验证 token 是否存在
    public boolean validateToken(String token) {
        String key = TOKEN_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    // 刷新 token 过期时间（滑动过期）
    public void refreshToken(String token, long expirationMs) {
        String key = TOKEN_PREFIX + token;
        redisTemplate.expire(key, expirationMs, TimeUnit.MILLISECONDS);
    }

    // 删除 token（登出）
    public void deleteToken(String token) {
        String key = TOKEN_PREFIX + token;
        redisTemplate.delete(key);
    }

    // 获取 token 对应的用户ID
    public Long getUserId(String token) {
        String key = TOKEN_PREFIX + token;
        Object value = redisTemplate.opsForValue().get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }
}
