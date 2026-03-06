package com.blog.enumeration;

import lombok.Getter;

/**
 * 文章状态枚举
 */
@Getter
public enum ArticleStatus {
    DRAFT("draft", "草稿"),
    PUBLISHED("published", "已发布"),
    ARCHIVED("archived", "已归档");

    private final String code;
    private final String description;

    ArticleStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据code获取枚举
     */
    public static ArticleStatus fromCode(String code) {
        for (ArticleStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的文章状态: " + code);
    }

    /**
     * 验证状态码是否有效
     */
    public static boolean isValid(String code) {
        if (code == null) {
            return false;
        }
        for (ArticleStatus status : values()) {
            if (status.code.equals(code)) {
                return true;
            }
        }
        return false;
    }
}
