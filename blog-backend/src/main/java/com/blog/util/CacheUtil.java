package com.blog.util;

import com.blog.constant.AppConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheUtil {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedissonClient redissonClient;

    @SuppressWarnings("unchecked")
    public <T> T getWithLock(String key, Class<T> type, long ttl, TimeUnit unit, Supplier<T> dbFallback) {
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                return AppConstants.CACHE_NULL_VALUE.equals(cached) ? null : (T) cached;
            }
        } catch (Exception e) {
            log.error("Redis 读取失败, key: {}, 降级查询数据库", key, e);
            return dbFallback.get();
        }

        RLock lock = redissonClient.getLock(AppConstants.CACHE_LOCK_PREFIX + key);
        try {
            if (lock.tryLock(AppConstants.LOCK_WAIT_SECONDS, AppConstants.LOCK_LEASE_SECONDS, TimeUnit.SECONDS)) {
                try {
                    Object cached = redisTemplate.opsForValue().get(key);
                    if (cached != null) {
                        return AppConstants.CACHE_NULL_VALUE.equals(cached) ? null : (T) cached;
                    }

                    T data = dbFallback.get();
                    if (data == null) {
                        redisTemplate.opsForValue().set(key, AppConstants.CACHE_NULL_VALUE, AppConstants.CACHE_NULL_TTL_MINUTES, TimeUnit.MINUTES);
                        return null;
                    }

                    long randomTtl = ttl + (long) (Math.random() * ttl * AppConstants.CACHE_TTL_RANDOM_FACTOR);
                    redisTemplate.opsForValue().set(key, data, randomTtl, unit);
                    return data;
                } finally {
                    lock.unlock();
                }
            }
            return dbFallback.get();
        } catch (Exception e) {
            log.error("Redis 锁操作失败, key: {}, 降级查询数据库", key, e);
            return dbFallback.get();
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T getWithPassThrough(String key, Class<T> type, long ttl, TimeUnit unit, Supplier<T> dbFallback) {
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return AppConstants.CACHE_NULL_VALUE.equals(cached) ? null : (T) cached;
        }

        T data = dbFallback.get();
        if (data == null) {
            redisTemplate.opsForValue().set(key, AppConstants.CACHE_NULL_VALUE, AppConstants.CACHE_NULL_TTL_MINUTES, TimeUnit.MINUTES);
            return null;
        }

        long randomTtl = ttl + (long) (Math.random() * ttl * AppConstants.CACHE_TTL_RANDOM_FACTOR);
        redisTemplate.opsForValue().set(key, data, randomTtl, unit);
        return data;
    }

    public void set(String key, Object value, long ttl, TimeUnit unit) {
        long randomTtl = ttl + (long) (Math.random() * ttl * AppConstants.CACHE_TTL_RANDOM_FACTOR);
        redisTemplate.opsForValue().set(key, value, randomTtl, unit);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        return AppConstants.CACHE_NULL_VALUE.equals(value) ? null : (T) value;
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }

    public void deleteByPattern(String pattern) {
        var keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    public Long increment(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    public Long increment(String key, long ttl, TimeUnit unit) {
        try {
            Long val = redisTemplate.opsForValue().increment(key);
            if (val != null && val == 1) {
                redisTemplate.expire(key, ttl, unit);
            }
            return val;
        } catch (Exception e) {
            log.error("Redis increment 失败, key: {}", key, e);
            return null;
        }
    }
}
