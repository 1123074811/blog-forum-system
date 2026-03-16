package com.blog.aspect;

import com.blog.annotation.RateLimit;
import com.blog.context.BaseContext;
import com.blog.exception.BusinessException;
import com.blog.exception.ErrorCode;
import com.blog.service.SecurityEventService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;

/**
 * 限流切面
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitAspect {

    private final RedisTemplate<String, Object> redisTemplate;
    private final SecurityEventService securityEventService;

    @Before("@annotation(rateLimit)")
    public void doBefore(JoinPoint joinPoint, RateLimit rateLimit) {
        String key = generateKey(rateLimit);
        // 获取当前请求次数
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == null) {
            count = 1L;
        }

        // 第一次请求时设置过期时间
        if (count == 1L) {
            redisTemplate.expire(key, rateLimit.time(), TimeUnit.SECONDS);
        }

        // 超过限流次数
        if (count > rateLimit.count()) {
            String ip = getIpAddress();
            log.warn("Rate limit exceeded: key={}, count={}, limit={}, ip={}", key, count, rateLimit.count(), ip);
            // 只在刚超限时（count == limit+1）记录，避免每次请求都累加封禁计数
            if (count == rateLimit.count() + 1) {
                securityEventService.recordRateLimit(ip);
            }
            throw new BusinessException(ErrorCode.RATE_LIMIT_EXCEEDED, rateLimit.message());
        }
    }

    /**
     * 生成限流 key
     */
    private String generateKey(RateLimit rateLimit) {
        StringBuilder key = new StringBuilder(rateLimit.key());

        switch (rateLimit.limitType()) {
            case IP -> key.append(":").append(getIpAddress());
            case USER -> {
                Long userId = BaseContext.getCurrentId();
                if (userId != null) {
                    key.append(":").append(userId);
                } else {
                    // 未登录用户使用 IP
                    key.append(":").append(getIpAddress());
                }
            }
            case GLOBAL -> {
                // 全局限流，不添加后缀
            }
        }
        return key.toString();
    }

    /**
     * 获取客户端 IP 地址
     */
    private String getIpAddress() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "unknown";
        }

        HttpServletRequest request = attributes.getRequest();
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 处理多级代理的情况
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
