package com.blog.dto;

import lombok.Data;

@Data
public class CommentRequest {
    private Long articleId;
    private Long parentId;
    private String content;
}
