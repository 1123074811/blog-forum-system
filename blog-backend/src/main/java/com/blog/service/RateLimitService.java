package com.blog.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;

    private static final String REGISTER_LIMIT_PREFIX = "rate:register:";
    private static final int MAX_REGISTER_PER_HOUR = 3;
    private static final long EXPIRE_HOURS = 1;

    public boolean isRegisterAllowed(String ip) {
        String key = REGISTER_LIMIT_PREFIX + ip;
        String countStr = redisTemplate.opsForValue().get(key);
        int count = countStr == null ? 0 : Integer.parseInt(countStr);
        return count < MAX_REGISTER_PER_HOUR;
    }

    public void recordRegister(String ip) {
        String key = REGISTER_LIMIT_PREFIX + ip;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, EXPIRE_HOURS, TimeUnit.HOURS);
        }
    }

    public int getRemainingAttempts(String ip) {
        String key = REGISTER_LIMIT_PREFIX + ip;
        String countStr = redisTemplate.opsForValue().get(key);
        int count = countStr == null ? 0 : Integer.parseInt(countStr);
        return Math.max(0, MAX_REGISTER_PER_HOUR - count);
    }
}
