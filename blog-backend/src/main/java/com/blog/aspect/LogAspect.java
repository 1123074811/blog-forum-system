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

    /**
     * 环绕通知：记录请求日志和执行时间
     */
    @Around("controllerLog()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 获取方法信息
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        log.info("==> {}.{} 开始执行，参数: {}", className, methodName, args);

        Object result;
        try {
            result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;
            log.info("<== {}.{} 执行成功，耗时: {}ms", className, methodName, executionTime);
        } catch (Throwable e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("<== {}.{} 执行失败，耗时: {}ms，异常: {}",
                className, methodName, executionTime, e.getMessage());
            throw e;
        }

        return result;
    }
}
