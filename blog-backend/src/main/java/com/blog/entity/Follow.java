package com.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("follows")
public class Follow {
    private Long followerId;
    private Long followingId;
    private String createdAt;
}
