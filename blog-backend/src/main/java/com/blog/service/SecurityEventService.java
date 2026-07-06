package com.blog.service;

import com.blog.mapper.IpBlacklistMapper;
import com.blog.pojo.entity.IpBlacklist;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityEventService {

    private final RedisTemplate<String, Object> redis;
    private final IpBlacklistMapper ipBlacklistMapper;
    private final IpLocationService ipLocationService;

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

    public long incrementWithTtl(String key, long ttlSeconds) {
        return increment(key, ttlSeconds);
    }

    public int getThreatLevel(String ip) {
        Object value = redis.opsForValue().get("risk:ip:threat:" + ip);
        if (value == null) {
            return 0;
        }
        return Integer.parseInt(value.toString());
    }

    public void setThreatLevel(String ip, int level, long ttlSeconds) {
        redis.opsForValue().set("risk:ip:threat:" + ip, level, ttlSeconds, TimeUnit.SECONDS);
    }

    public void banIp(String ip, String reason, Duration ttl) {
        if (ip == null || ip.isBlank()) {
            return;
        }
        String normalizedIp = ip.trim();
        setBanCache(normalizedIp, reason, ttl);
        syncBanToDb(normalizedIp, reason, ttl);
        log.warn("[SECURITY] Banned ip={} reason={} ttl={}", normalizedIp, reason, ttl);
    }

    public boolean isIpBanned(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }
        String normalizedIp = ip.trim();
        if (Boolean.TRUE.equals(redis.hasKey("banned_ip:" + normalizedIp))) {
            return true;
        }

        IpBlacklist record = ipBlacklistMapper.findEffectiveByIp(normalizedIp);
        if (record == null) {
            IpBlacklist activeRecord = ipBlacklistMapper.findActiveByIp(normalizedIp);
            if (activeRecord != null && activeRecord.getExpireTime() != null
                    && !activeRecord.getExpireTime().isAfter(LocalDateTime.now())) {
                activeRecord.setStatus(0);
                activeRecord.setUpdatedAt(LocalDateTime.now());
                ipBlacklistMapper.updateById(activeRecord);
            }
            return false;
        }

        setBanCache(record.getIp(), record.getReason(), remainingTtl(record));
        return true;
    }

    public void unbanIp(String ip) {
        redis.delete("banned_ip:" + ip);
        IpBlacklist record = ipBlacklistMapper.findActiveByIp(ip);
        if (record != null) {
            record.setStatus(0);
            record.setUpdatedAt(LocalDateTime.now());
            ipBlacklistMapper.updateById(record);
        }
        log.info("[SECURITY] Unbanned ip={}", ip);
    }

    public List<IpBlacklist> listEffectiveBans() {
        markExpiredBansInactive();
        return ipBlacklistMapper.findEffectiveBans();
    }

    public int restoreEffectiveBansToRedis() {
        markExpiredBansInactive();
        List<IpBlacklist> records = ipBlacklistMapper.findEffectiveBans();
        int count = 0;
        for (IpBlacklist record : records) {
            setBanCache(record.getIp(), record.getReason(), remainingTtl(record));
            count++;
        }
        log.info("[SECURITY] Restored {} active IP bans to Redis", count);
        return count;
    }

    public int markExpiredBansInactive() {
        int count = ipBlacklistMapper.markExpiredInactive();
        if (count > 0) {
            log.info("[SECURITY] Marked {} expired IP bans inactive", count);
        }
        return count;
    }

    private void syncBanToDb(String ip, String reason, Duration ttl) {
        try {
            String location = ipLocationService.resolve(ip);
            int banType = (ttl == null || ttl.isZero()) ? 1 : 2;
            LocalDateTime expireTime = (ttl != null && !ttl.isZero())
                    ? LocalDateTime.now().plus(ttl) : null;
            ipBlacklistMapper.upsertBan(ip, reason, location, banType, expireTime);
        } catch (Exception e) {
            log.error("[SECURITY] Failed to sync ban to DB for ip={}", ip, e);
        }
    }

    private void setBanCache(String ip, String reason, Duration ttl) {
        String key = "banned_ip:" + ip;
        String value = (reason == null || reason.isBlank()) ? "security_policy" : reason;
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            redis.opsForValue().set(key, value);
            return;
        }
        redis.opsForValue().set(key, value, ttl);
    }

    private Duration remainingTtl(IpBlacklist record) {
        if (record.getExpireTime() == null) {
            return null;
        }
        return Duration.between(LocalDateTime.now(), record.getExpireTime());
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
