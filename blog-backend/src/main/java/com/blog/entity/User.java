package com.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("users")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String nickname;
    private String password;
    private String email;
    private String avatar;
    private String bio;
    private String role;
    private String githubId;
    private String giteeId;
    private Boolean banned;
    private String createdAt;
    private String updatedAt;
}
