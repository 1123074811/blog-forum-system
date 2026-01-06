package com.blog.constant;

/**
 * 应用常量
 */
public final class AppConstants {
    private AppConstants() {}

    // ==================== 分页 ====================
    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int PAGE_SIZE_20 = 20;
    public static final int PAGE_SIZE_50 = 50;
    public static final int TREE_HOLE_LIMIT = 100;
    public static final int HOT_ARTICLE_LIMIT = 5;
    public static final int NOTIFICATION_LIMIT = 50;
    public static final int QUIZ_BATCH_SIZE = 500;

    // ==================== 缓存 ====================
    public static final String CACHE_NULL_VALUE = "NULL";
    public static final String CACHE_LOCK_PREFIX = "lock:";
    public static final String CACHE_ARTICLE_PREFIX = "article:";
    public static final String CACHE_ARTICLE_VIEW_PREFIX = "article:view:";
    public static final String CACHE_USER_PREFIX = "user:";
    public static final String CACHE_CATEGORY_LIST = "category:list";
    public static final String CACHE_TAG_LIST = "tag:list";
    public static final String CACHE_BING_PREFIX = "bing_";

    public static final long CACHE_NULL_TTL_MINUTES = 2;
    public static final long CACHE_ARTICLE_TTL_MINUTES = 30;
    public static final long CACHE_USER_TTL_MINUTES = 30;
    public static final long CACHE_CATEGORY_TTL_MINUTES = 60;
    public static final double CACHE_TTL_RANDOM_FACTOR = 0.2;

    public static final int LOCK_WAIT_SECONDS = 3;
    public static final int LOCK_LEASE_SECONDS = 10;

    // ==================== 文章 ====================
    public static final String ARTICLE_STATUS_PUBLISHED = "published";
    public static final String ARTICLE_STATUS_DRAFT = "draft";
    public static final String SORT_LATEST = "latest";
    public static final String SORT_POPULAR = "popular";
    public static final int VIEW_COUNT_UPDATE_THRESHOLD = 10;

    // ==================== 用户 ====================
    public static final String ROLE_USER = "user";
    public static final String ROLE_ADMIN = "admin";
    public static final String SPRING_ROLE_USER = "ROLE_USER";
    public static final String DEFAULT_NICKNAME_PREFIX = "用户";
    public static final int NICKNAME_RANDOM_BOUND = 100000;

    // ==================== JWT ====================
    public static final String JWT_BEARER_PREFIX = "Bearer ";
    public static final int JWT_BEARER_PREFIX_LENGTH = 7;
    public static final String JWT_CLAIM_USER_ID = "userId";
    public static final String HTTP_HEADER_AUTHORIZATION = "Authorization";

    // ==================== 媒体 ====================
    public static final String MEDIA_TYPE_IMAGE = "image";
    public static final String MEDIA_TYPE_VIDEO = "video";
    public static final String MEDIA_SOURCE_BING = "bing";

    // ==================== 题库 ====================
    public static final String QUIZ_TYPE_FILE = "file";
    public static final String QUIZ_DEFAULT_TITLE = "未命名题库";
    public static final String FILE_EXT_JSON = "json";
    public static final String CHARSET_UTF8 = "UTF-8";

    // ==================== 通知 ====================
    public static final String NOTIFY_TYPE_LIKE = "like";
    public static final String NOTIFY_TYPE_FAVORITE = "favorite";
    public static final String NOTIFY_TYPE_FOLLOW = "follow";
    public static final String NOTIFY_MSG_LIKE = "赞了你的文章";
    public static final String NOTIFY_MSG_FAVORITE = "收藏了你的文章";
    public static final String NOTIFY_MSG_FOLLOW = "关注了你";

    // ==================== OAuth ====================
    public static final String OAUTH_GITHUB_PREFIX = "github_";
    public static final String OAUTH_GITEE_PREFIX = "gitee_";

    // ==================== 日期格式 ====================
    public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String DATE_FORMAT_CN = "yyyy年MM月dd日 E";

    // ==================== 错误消息 ====================
    public static final String ERR_USER_NOT_FOUND = "User not found";
    public static final String ERR_UNAUTHORIZED = "Unauthorized";
    public static final String ERR_CANNOT_FOLLOW_SELF = "Cannot follow yourself";
    public static final String ERR_ARTICLE_NOT_FOUND = "Article not found";
    public static final String ERR_COMMENT_NOT_FOUND = "Comment not found";
    public static final String ERR_QUIZ_NOT_FOUND = "题库不存在";
    public static final String ERR_INVALID_CREDENTIALS = "Invalid username or password";
    public static final String ERR_USERNAME_EXISTS = "Username already exists";
    public static final String ERR_EMAIL_EXISTS = "Email already exists";
    public static final String ERR_INVALID_TOKEN = "Invalid token";
    public static final String ERR_INVALID_REFRESH_TOKEN = "Invalid or expired refresh token";
    public static final String ERR_LOADING = "正在加载中，请稍后刷新";

    // ==================== HTTP ====================
    public static final int HTTP_TIMEOUT_SHORT = 3;
    public static final int HTTP_TIMEOUT_MEDIUM = 5;
    public static final int HTTP_TIMEOUT_LONG = 10;
    public static final String LOCAL_IP_V4 = "127.0.0.1";
    public static final String LOCAL_IP_V6 = "0:0:0:0:0:0:0:1";

    // ==================== 外部API ====================
    public static final String BING_API_URL = "https://www.bing.com/HPImageArchive.aspx?format=js&idx=0&n=8&mkt=zh-CN";
    public static final String BING_BASE_URL = "https://www.bing.com";
    public static final String BING_DEFAULT_TITLE = "Bing Wallpaper";
    public static final int BING_WALLPAPER_COUNT = 8;
    public static final String DOUYIN_HOT_API = "https://www.iesdouyin.com/web/api/v2/hotsearch/billboard/word/";
    public static final int DOUYIN_HOT_LIMIT = 10;
    public static final String IP_LOCATION_API = "http://whois.pconline.com.cn/ipJson.jsp";
    public static final String WEATHER_API_BASE = "http://wttr.in/";

    // ==================== Redis ====================
    public static final int REDIS_MIN_IDLE = 4;
    public static final int REDIS_MAX_CONNECTIONS = 8;

    // ==================== 系统 ====================
    public static final long SYSTEM_USER_ID = 1L;
}
