package com.blog.enumeration;

import lombok.Getter;

/**
 * 用户角色枚举
 */
@Getter
public enum UserRole {
    USER("user", "普通用户"),
    ADMIN("admin", "管理员"),
    MODERATOR("moderator", "版主");

    private final String code;
    private final String description;

    UserRole(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据code获取枚举
     */
    public static UserRole fromCode(String code) {
        for (UserRole role : values()) {
            if (role.code.equals(code)) {
                return role;
            }
        }
        throw new IllegalArgumentException("未知的用户角色: " + code);
    }

    /**
     * 验证角色码是否有效
     */
    public static boolean isValid(String code) {
        if (code == null) {
            return false;
        }
        for (UserRole role : values()) {
            if (role.code.equals(code)) {
                return true;
            }
        }
        return false;
    }
}
