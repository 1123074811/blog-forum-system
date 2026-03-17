package com.blog.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * 操作日志切面
 */
@Aspect
@Component
@Slf4j
public class LogAspect {

    /**
     * 切入点：所有 Controller 的公共方法
     */
    @Pointcut("execution(public * com.blog.controller..*.*(..))")
    public void controllerLog() {
    }

    // 慢请求阈值（毫秒）
    private static final long SLOW_THRESHOLD_MS = 1000;

    /**
     * 环绕通知：仅记录慢请求和异常，减少日志噪音
     */
    @Around("controllerLog()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long cost = System.currentTimeMillis() - startTime;
            if (cost >= SLOW_THRESHOLD_MS) {
                log.warn("[SLOW] {}.{} {}ms", joinPoint.getTarget().getClass().getSimpleName(),
                        joinPoint.getSignature().getName(), cost);
            }
            return result;
        } catch (Throwable e) {
            long cost = System.currentTimeMillis() - startTime;
            log.error("[ERR] {}.{} {}ms - {}", joinPoint.getTarget().getClass().getSimpleName(),
                    joinPoint.getSignature().getName(), cost, e.getMessage());
            throw e;
        }
    }
}
