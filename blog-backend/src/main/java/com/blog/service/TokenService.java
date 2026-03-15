package com.blog.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenService {

    private static final String TOKEN_PREFIX = "token:";
    private static final String USER_TOKEN_SET_PREFIX = "user:tokens:";

    private final RedisTemplate<String, Object> redisTemplate;

    public void saveToken(String token, Long userId, long expirationMs) {
        String tokenKey = TOKEN_PREFIX + token;
        redisTemplate.opsForValue().set(tokenKey, userId, expirationMs, TimeUnit.MILLISECONDS);
        redisTemplate.opsForSet().add(USER_TOKEN_SET_PREFIX + userId, token);
        extendSetTtlIfNeeded(USER_TOKEN_SET_PREFIX + userId, expirationMs);
    }

    public boolean validateToken(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(TOKEN_PREFIX + token));
    }

    public void refreshToken(String token, long expirationMs) {
        String key = TOKEN_PREFIX + token;
        redisTemplate.expire(key, expirationMs, TimeUnit.MILLISECONDS);
        Long userId = getUserId(token);
        if (userId != null) {
            extendSetTtlIfNeeded(USER_TOKEN_SET_PREFIX + userId, expirationMs);
        }
    }

    public void deleteToken(String token) {
        Long userId = getUserId(token);
        redisTemplate.delete(TOKEN_PREFIX + token);
        if (userId != null) {
            redisTemplate.opsForSet().remove(USER_TOKEN_SET_PREFIX + userId, token);
        }
    }

    public Long getUserId(String token) {
        Object value = redisTemplate.opsForValue().get(TOKEN_PREFIX + token);
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    public long revokeUserSessions(Long userId) {
        String setKey = USER_TOKEN_SET_PREFIX + userId;
        Set<Object> tokens = redisTemplate.opsForSet().members(setKey);
        long revoked = 0L;
        if (tokens != null) {
            for (Object token : tokens) {
                if (token == null) {
                    continue;
                }
                redisTemplate.delete(TOKEN_PREFIX + token);
                revoked++;
            }
        }
        redisTemplate.delete(setKey);
        return revoked;
    }

    public long revokeAllSessions() {
        Set<String> tokenKeys = redisTemplate.keys(TOKEN_PREFIX + "*");
        Set<String> userTokenSetKeys = redisTemplate.keys(USER_TOKEN_SET_PREFIX + "*");
        long revoked = 0L;
        if (tokenKeys != null && !tokenKeys.isEmpty()) {
            revoked += tokenKeys.size();
            redisTemplate.delete(tokenKeys);
        }
        if (userTokenSetKeys != null && !userTokenSetKeys.isEmpty()) {
            redisTemplate.delete(userTokenSetKeys);
        }
        log.warn("All sessions revoked, count={}", revoked);
        return revoked;
    }

    private void extendSetTtlIfNeeded(String setKey, long expirationMs) {
        Long currentTtl = redisTemplate.getExpire(setKey, TimeUnit.MILLISECONDS);
        if (currentTtl == null || currentTtl < 0 || currentTtl < expirationMs) {
            redisTemplate.expire(setKey, expirationMs, TimeUnit.MILLISECONDS);
        }
    }
}
