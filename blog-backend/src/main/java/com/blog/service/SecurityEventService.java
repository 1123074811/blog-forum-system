package com.blog.service;

import com.blog.mapper.IpBlacklistMapper;
import com.blog.pojo.entity.IpBlacklist;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityEventService {

    private final RedisTemplate<String, Object> redis;
    private final IpBlacklistMapper ipBlacklistMapper;

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
        // 阈值设为 20，避免普通用户因权限检测（如 /admin/ping）被误封
        if (count >= 20) {
            banIp(ip, "unauthorized_access", Duration.ofHours(1));
        }
    }

    public void recordRateLimit(String ip) {
        // 每次触发限流都累计，窗口10分钟
        long count = increment("ddos:429count:" + ip, 600);
        // 阶梯封禁：5次触发 -> 封10分钟，15次 -> 封1小时，30次 -> 封24小时
        if (count >= 30) {
            banIp(ip, "rate_limit_exceeded", Duration.ofHours(24));
        } else if (count >= 15) {
            banIp(ip, "rate_limit_exceeded", Duration.ofHours(1));
        } else if (count >= 5) {
            banIp(ip, "rate_limit_exceeded", Duration.ofMinutes(10));
        }
    }

    public void banIp(String ip, String reason, Duration ttl) {
        if (ip == null || ip.isBlank()) {
            return;
        }
        LocalDateTime expireTime = ttl == null ? null : LocalDateTime.now().plus(ttl);
        Integer banType = expireTime == null ? 1 : 2;
        String banReason = (reason == null || reason.isBlank()) ? "security_policy" : reason;
        ipBlacklistMapper.upsertBan(ip, banReason, banType, expireTime);
        // 鍏煎鏃у彲鑳界暀瀛樼殑 Redis 灏佺 key锛岄伩鍏嶅幓闄ょ數璺笉涓€鑷?
        redis.delete("banned_ip:" + ip);
        log.warn("[SECURITY] Banned ip={} reason={} expireTime={}", ip, banReason, expireTime);
    }

    public boolean isIpBanned(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }
        IpBlacklist record = ipBlacklistMapper.findActiveByIp(ip);
        if (record == null) {
            return false;
        }
        if (record.getExpireTime() != null && !record.getExpireTime().isAfter(LocalDateTime.now())) {
            ipBlacklistMapper.deleteByIp(ip);
            redis.delete("banned_ip:" + ip);
            return false;
        }
        return true;
    }

    public void unbanIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return;
        }
        ipBlacklistMapper.deleteByIp(ip);
        redis.delete("banned_ip:" + ip);
        redis.delete(THREAT_LEVEL_PREFIX + ip);
        log.info("[SECURITY] Unbanned ip={}", ip);
    }

    // ── 超高频威胁标志数（供 HotspotRateLimitFilter 使用）──────────

    private static final String THREAT_LEVEL_PREFIX = "hotspot:threat:";

    /**
     * 获取 IP 当前威胁标志数，0 表示未标记
     */
    public int getThreatLevel(String ip) {
        Object val = redis.opsForValue().get(THREAT_LEVEL_PREFIX + ip);
        if (val == null) return 0;
        try {
            return Integer.parseInt(val.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 设置 IP 威胁标志数，ttlSeconds 为生命周期
     */
    public void setThreatLevel(String ip, int level, long ttlSeconds) {
        redis.opsForValue().set(THREAT_LEVEL_PREFIX + ip, String.valueOf(level), ttlSeconds, TimeUnit.SECONDS);
    }

    /**
     * 滑动窗口计数：每次调用 +1，首次设置 TTL
     */
    public long incrementWithTtl(String key, long ttlSeconds) {
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redis.expire(key, ttlSeconds, TimeUnit.SECONDS);
        }
        return count == null ? 1L : count;
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
