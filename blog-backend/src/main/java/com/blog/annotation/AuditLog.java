package com.blog.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 审计日志注解
 * 用于记录关键操作
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {
    /**
     * 操作模块
     */
    String module() default "";

    /**
     * 操作类型
     */
    String operation() default "";

    /**
     * 操作描述
     */
    String description() default "";
}
