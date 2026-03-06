package com.blog.constant;

import com.blog.enumeration.ArticleStatus;
import com.blog.enumeration.EnableStatus;
import com.blog.enumeration.UserRole;

/**
 * 状态常量
 * @deprecated 建议使用枚举类替代：{@link EnableStatus}, {@link ArticleStatus}, {@link UserRole}
 */
@Deprecated
public class StatusConstant {

    /**
     * 启用
     * @deprecated 使用 {@link EnableStatus#ENABLED}
     */
    @Deprecated
    public static final Integer ENABLE = EnableStatus.ENABLED.getCode();

    /**
     * 禁用
     * @deprecated 使用 {@link EnableStatus#DISABLED}
     */
    @Deprecated
    public static final Integer DISABLE = EnableStatus.DISABLED.getCode();

    /**
     * 文章状态：草稿
     * @deprecated 使用 {@link ArticleStatus#DRAFT}
     */
    @Deprecated
    public static final String ARTICLE_DRAFT = ArticleStatus.DRAFT.getCode();

    /**
     * 文章状态：已发布
     * @deprecated 使用 {@link ArticleStatus#PUBLISHED}
     */
    @Deprecated
    public static final String ARTICLE_PUBLISHED = ArticleStatus.PUBLISHED.getCode();

    /**
     * 用户角色：普通用户
     * @deprecated 使用 {@link UserRole#USER}
     */
    @Deprecated
    public static final String ROLE_USER = UserRole.USER.getCode();

    /**
     * 用户角色：管理员
     * @deprecated 使用 {@link UserRole#ADMIN}
     */
    @Deprecated
    public static final String ROLE_ADMIN = UserRole.ADMIN.getCode();
}
