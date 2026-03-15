package com.blog.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityEventService {

    private final RedisTemplate<String, Object> redis;

    public void recordLoginFail(String ip, String username) {
        increment("risk:ip:loginfail:" + ip, 300);
        increment("risk:user:loginfail:" + username, 300);
        long ipCount = getCount("risk:ip:loginfail:" + ip);
        if (ipCount >= 10) {
            banIp(ip, "login_bruteforce", Duration.ofMinutes(30));
        }
    }

    public void recordUnauthorized(String ip) {
        long count = increment("risk:ip:unauth:" + ip, 300);
        if (count >= 5) {
            banIp(ip, "unauthorized_access", Duration.ofHours(1));
        }
    }

    public void recordRateLimit(String ip) {
        long count = increment("ddos:429count:" + ip, 60);
        if (count >= 20) {
            banIp(ip, "rate_limit_exceeded", Duration.ofHours(1));
        }
    }

    public void banIp(String ip, String reason, Duration ttl) {
        if (ip == null || ip.isBlank()) {
            return;
        }
        redis.opsForValue().set("banned_ip:" + ip, reason, ttl);
        log.warn("[SECURITY] Banned ip={} reason={} ttl={}", ip, reason, ttl);
    }

    public boolean isIpBanned(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }
        return Boolean.TRUE.equals(redis.hasKey("banned_ip:" + ip));
    }

    public void unbanIp(String ip) {
        redis.delete("banned_ip:" + ip);
        log.info("[SECURITY] Unbanned ip={}", ip);
    }

    private long increment(String key, long ttlSeconds) {
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redis.expire(key, ttlSeconds, TimeUnit.SECONDS);
        }
        return count == null ? 1L : count;
    }

    private long getCount(String key) {
        Object value = redis.opsForValue().get(key);
        if (value == null) {
            return 0L;
        }
        return Long.parseLong(value.toString());
    }
}
