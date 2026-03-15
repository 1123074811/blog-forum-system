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

    // 同一邮箱域名每小时最多注册次数（防止 @xxx.com 批量注册）
    private static final String DOMAIN_LIMIT_PREFIX = "rate:reg_domain:";
    private static final int MAX_REGISTER_PER_DOMAIN_PER_HOUR = 5;

    // 全局注册速率兜底（防代理绕过 IP 限制）
    private static final String GLOBAL_REGISTER_KEY = "rate:reg_global";
    private static final int MAX_GLOBAL_REGISTER_PER_MINUTE = 20;

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

    /**
     * 检查邮箱域名注册频率（防 @xxx.com 批量注册）
     */
    public boolean isDomainAllowed(String email) {
        String domain = extractDomain(email);
        if (domain == null) return false;
        String key = DOMAIN_LIMIT_PREFIX + domain;
        String countStr = redisTemplate.opsForValue().get(key);
        int count = countStr == null ? 0 : Integer.parseInt(countStr);
        return count < MAX_REGISTER_PER_DOMAIN_PER_HOUR;
    }

    public void recordDomainRegister(String email) {
        String domain = extractDomain(email);
        if (domain == null) return;
        String key = DOMAIN_LIMIT_PREFIX + domain;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, EXPIRE_HOURS, TimeUnit.HOURS);
        }
    }

    /**
     * 全局注册速率兜底（每分钟最多 20 次，防代理池绕过 IP 限制）
     */
    public boolean isGlobalRateAllowed() {
        String countStr = redisTemplate.opsForValue().get(GLOBAL_REGISTER_KEY);
        int count = countStr == null ? 0 : Integer.parseInt(countStr);
        return count < MAX_GLOBAL_REGISTER_PER_MINUTE;
    }

    public void recordGlobalRegister() {
        Long count = redisTemplate.opsForValue().increment(GLOBAL_REGISTER_KEY);
        if (count != null && count == 1) {
            redisTemplate.expire(GLOBAL_REGISTER_KEY, 1, TimeUnit.MINUTES);
        }
    }

    private String extractDomain(String email) {
        if (email == null || !email.contains("@")) return null;
        return email.substring(email.indexOf('@') + 1).toLowerCase();
    }
}
