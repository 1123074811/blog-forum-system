package com.blog.aspect;

import com.blog.validator.SqlInjectionValidator;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * SQL 注入防护切面
 */
@Aspect
@Component
@Slf4j
public class SqlInjectionAspect {

    /**
     * 切入点：所有 Controller 的查询方法
     */
    @Pointcut("execution(* com.blog.controller..*.*(..)) && " +
              "(execution(* *..get*(..)) || execution(* *..search*(..)) || execution(* *..query*(..)))")
    public void queryMethods() {
    }

    /**
     * 前置通知：检查参数是否包含 SQL 注入
     */
    @Before("queryMethods()")
    public void checkSqlInjection(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            return;
        }

        for (Object arg : args) {
            if (arg instanceof String) {
                String value = (String) arg;
                if (SqlInjectionValidator.containsSqlInjection(value)) {
                    log.warn("检测到 SQL 注入风险: method={}, value={}",
                            joinPoint.getSignature().getName(), value);
                    throw new IllegalArgumentException("参数包含非法字符");
                }
            }
        }
    }
}
