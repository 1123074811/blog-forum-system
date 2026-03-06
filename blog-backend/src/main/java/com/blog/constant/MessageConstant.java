package com.blog.constant;

/**
 * 消息提示常量类
 */
public class MessageConstant {

    // 通用消息
    public static final String UNKNOWN_ERROR = "未知错误";
    public static final String OPERATION_SUCCESS = "操作成功";
    public static final String PARAM_ERROR = "参数错误";

    // 用户相关
    public static final String USER_NOT_FOUND = "用户不存在";
    public static final String USER_ALREADY_EXISTS = "用户已存在";
    public static final String USERNAME_OR_PASSWORD_ERROR = "用户名或密码错误";
    public static final String PASSWORD_NOT_MATCH = "密码不匹配";
    public static final String USER_DISABLED = "用户已被禁用";
    public static final String USER_NOT_LOGIN = "用户未登录";
    public static final String UNAUTHORIZED = "未授权";
    public static final String FORBIDDEN = "无权限";

    // 文章相关
    public static final String ARTICLE_NOT_FOUND = "文章不存在";
    public static final String ARTICLE_PERMISSION_DENIED = "无权操作该文章";
    public static final String ARTICLE_STATUS_ERROR = "文章状态错误";
    public static final String ARTICLE_TITLE_REQUIRED = "文章标题不能为空";
    public static final String ARTICLE_CONTENT_REQUIRED = "文章内容不能为空";

    // 评论相关
    public static final String COMMENT_NOT_FOUND = "评论不存在";
    public static final String COMMENT_PERMISSION_DENIED = "无权操作该评论";
    public static final String COMMENT_CONTENT_REQUIRED = "评论内容不能为空";

    // 分类相关
    public static final String CATEGORY_NOT_FOUND = "分类不存在";
    public static final String CATEGORY_BE_RELATED_BY_ARTICLE = "当前分类关联了文章，不能删除";

    // 标签相关
    public static final String TAG_NOT_FOUND = "标签不存在";
    public static final String TAG_BE_RELATED_BY_ARTICLE = "当前标签关联了文章，不能删除";

    // 文件相关
    public static final String FILE_UPLOAD_ERROR = "文件上传失败";
    public static final String FILE_TYPE_ERROR = "文件类型不支持";
    public static final String FILE_SIZE_ERROR = "文件大小超出限制";

    // 限流相关
    public static final String RATE_LIMIT_EXCEEDED = "操作过于频繁，请稍后再试";

    // 第三方服务
    public static final String AI_SERVICE_ERROR = "AI服务异常";
    public static final String CRAWL_SERVICE_ERROR = "爬虫服务异常";
    public static final String EMAIL_SERVICE_ERROR = "邮件服务异常";

    // 关注相关
    public static final String CANNOT_FOLLOW_YOURSELF = "不能关注自己";
    public static final String ALREADY_FOLLOWED = "已经关注过了";
    public static final String NOT_FOLLOWED = "未关注该用户";
}
