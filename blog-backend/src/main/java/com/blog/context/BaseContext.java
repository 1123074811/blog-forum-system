package com.blog.context;

/**
 * ThreadLocal 工具类，用于保存和获取当前登录用户ID
 */
public class BaseContext {

    private static final ThreadLocal<Long> userIdLocal = new ThreadLocal<>();
    private static final ThreadLocal<String> roleLocal = new ThreadLocal<>();

    /**
     * 设置当前线程的用户ID
     */
    public static void setCurrentId(Long id) {
        userIdLocal.set(id);
    }

    /**
     * 获取当前线程的用户ID
     */
    public static Long getCurrentId() {
        return userIdLocal.get();
    }

    public static void setCurrentRole(String role) {
        roleLocal.set(role);
    }

    public static String getCurrentRole() {
        return roleLocal.get();
    }

    public static boolean isAdmin() {
        return "admin".equalsIgnoreCase(roleLocal.get());
    }

    public static void removeAll() {
        userIdLocal.remove();
        roleLocal.remove();
    }

    // Keep backward compatibility for existing calls.
    /**
     * 移除当前线程的用户ID
     */
    public static void removeCurrentId() {
        removeAll();
    }
}
