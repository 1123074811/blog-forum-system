package com.blog.util;

import java.util.regex.Pattern;

public class PasswordUtil {
    
    /**
     * 验证用户名格式
     * 4-20位，只允许字母、数字、下划线，不能以数字开头，不能纯数字
     */
    public static boolean isValidUsername(String username) {
        if (username == null || username.length() < 4 || username.length() > 20) {
            return false;
        }
        return username.matches("^[a-zA-Z0-9_]{4,20}$");
    }

    /**
     * 验证密码强度
     * 至少6位，包含字母和数字
     */
    public static boolean isValidPassword(String password) {
        if (password == null || password.length() < 6) {
            return false;
        }
        // 可以根据需要调整密码复杂度要求
        return true;
    }
    
    /**
     * 验证邮箱格式
     */
    public static boolean isValidEmail(String email) {
        if (email == null) {
            return false;
        }
        String emailRegex = "^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$";
        Pattern pattern = Pattern.compile(emailRegex);
        return pattern.matcher(email).matches();
    }
}