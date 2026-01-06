package com.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("comments")
public class Comment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long articleId;
    private Long userId;
    private Long parentId;
    private String content;
    private Integer likeCount;
    private String createdAt;
    private String updatedAt;

    @TableField(exist = false)
    private String username;
    @TableField(exist = false)
    private String avatar;
}
