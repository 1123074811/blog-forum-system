package com.blog.pojo.vo;

import lombok.Data;

/**
 * 用户视图对象（不包含敏感信息）
 */
@Data
public class UserVO {
    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String avatar;
    private String bio;
    private String role;
    private String createdAt;

    // 扩展信息
    private Long followerCount;
    private Long followingCount;
    private Boolean isFollowing;
    private Boolean isOnline;
}
