package com.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("articles")
public class Article {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    private String content;
    private Long categoryId;
    private String status;
    private Integer viewCount;
    private String createdAt;
    private String updatedAt;

    @TableField(exist = false)
    private String authorName;
    @TableField(exist = false)
    private String authorAvatar;
    @TableField(exist = false)
    private Long likeCount;
    @TableField(exist = false)
    private Boolean liked;
    @TableField(exist = false)
    private Long hotScore;
}
