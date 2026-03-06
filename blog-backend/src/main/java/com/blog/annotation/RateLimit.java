package com.blog.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 限流注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * 限流key前缀
     */
    String key() default "rate_limit";

    /**
     * 限流时间窗口（秒）
     */
    int time() default 60;

    /**
     * 时间窗口内最大请求次数
     */
    int count() default 100;

    /**
     * 限流类型：IP、USER、GLOBAL
     */
    LimitType limitType() default LimitType.IP;

    /**
     * 提示信息
     */
    String message() default "操作过于频繁，请稍后再试";

    enum LimitType {
        /**
         * 根据 IP 限流
         */
        IP,

        /**
         * 根据用户 ID 限流
         */
        USER,

        /**
         * 全局限流
         */
        GLOBAL
    }
}
