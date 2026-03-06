package com.blog.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("comment_likes")
public class CommentLike {
    private Long commentId;
    private Long userId;
    private String createdAt;
}
