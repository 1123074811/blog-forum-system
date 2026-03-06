package com.blog.context;

/**
 * ThreadLocal 工具类，用于保存和获取当前登录用户ID
 */
public class BaseContext {

    private static final ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    /**
     * 设置当前线程的用户ID
     */
    public static void setCurrentId(Long id) {
        threadLocal.set(id);
    }

    /**
     * 获取当前线程的用户ID
     */
    public static Long getCurrentId() {
        return threadLocal.get();
    }

    /**
     * 移除当前线程的用户ID
     */
    public static void removeCurrentId() {
        threadLocal.remove();
    }
}
