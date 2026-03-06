package com.blog.enumeration;

import lombok.Getter;

/**
 * 启用状态枚举
 */
@Getter
public enum EnableStatus {
    DISABLED(0, "禁用"),
    ENABLED(1, "启用");

    private final Integer code;
    private final String description;

    EnableStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据code获取枚举
     */
    public static EnableStatus fromCode(Integer code) {
        for (EnableStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的启用状态: " + code);
    }

    /**
     * 验证状态码是否有效
     */
    public static boolean isValid(Integer code) {
        if (code == null) {
            return false;
        }
        for (EnableStatus status : values()) {
            if (status.code.equals(code)) {
                return true;
            }
        }
        return false;
    }
}
