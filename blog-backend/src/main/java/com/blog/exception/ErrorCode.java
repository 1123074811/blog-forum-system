package com.blog.exception;

import com.blog.constant.MessageConstant;
import lombok.Getter;

@Getter
public enum ErrorCode {
    // 通用错误 1xxx
    SUCCESS(1000, MessageConstant.OPERATION_SUCCESS),
    SYSTEM_ERROR(1001, MessageConstant.UNKNOWN_ERROR),
    PARAM_ERROR(1002, MessageConstant.PARAM_ERROR),
    UNAUTHORIZED(1003, MessageConstant.UNAUTHORIZED),
    FORBIDDEN(1004, MessageConstant.FORBIDDEN),
    NOT_FOUND(1005, "资源不存在"),

    // 用户相关 2xxx
    USER_NOT_FOUND(2001, MessageConstant.USER_NOT_FOUND),
    USER_ALREADY_EXISTS(2002, MessageConstant.USER_ALREADY_EXISTS),
    USERNAME_OR_PASSWORD_ERROR(2003, MessageConstant.USERNAME_OR_PASSWORD_ERROR),
    PASSWORD_NOT_MATCH(2004, MessageConstant.PASSWORD_NOT_MATCH),
    USER_DISABLED(2005, MessageConstant.USER_DISABLED),
    USER_NOT_LOGIN(2006, MessageConstant.USER_NOT_LOGIN),

    // 文章相关 3xxx
    ARTICLE_NOT_FOUND(3001, MessageConstant.ARTICLE_NOT_FOUND),
    ARTICLE_PERMISSION_DENIED(3002, MessageConstant.ARTICLE_PERMISSION_DENIED),
    ARTICLE_STATUS_ERROR(3003, MessageConstant.ARTICLE_STATUS_ERROR),

    // 评论相关 4xxx
    COMMENT_NOT_FOUND(4001, MessageConstant.COMMENT_NOT_FOUND),
    COMMENT_PERMISSION_DENIED(4002, MessageConstant.COMMENT_PERMISSION_DENIED),

    // 分类相关 5xxx
    CATEGORY_NOT_FOUND(5001, MessageConstant.CATEGORY_NOT_FOUND),
    CATEGORY_BE_RELATED_BY_ARTICLE(5002, MessageConstant.CATEGORY_BE_RELATED_BY_ARTICLE),

    // 标签相关 6xxx
    TAG_NOT_FOUND(6001, MessageConstant.TAG_NOT_FOUND),
    TAG_BE_RELATED_BY_ARTICLE(6002, MessageConstant.TAG_BE_RELATED_BY_ARTICLE),

    // 文件相关 7xxx
    FILE_UPLOAD_ERROR(7001, MessageConstant.FILE_UPLOAD_ERROR),
    FILE_TYPE_ERROR(7002, MessageConstant.FILE_TYPE_ERROR),
    FILE_SIZE_ERROR(7003, MessageConstant.FILE_SIZE_ERROR),

    // 限流相关 8xxx
    RATE_LIMIT_EXCEEDED(8001, MessageConstant.RATE_LIMIT_EXCEEDED),

    // 第三方服务 9xxx
    AI_SERVICE_ERROR(9001, MessageConstant.AI_SERVICE_ERROR),
    CRAWL_SERVICE_ERROR(9002, MessageConstant.CRAWL_SERVICE_ERROR),
    EMAIL_SERVICE_ERROR(9003, MessageConstant.EMAIL_SERVICE_ERROR);

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
