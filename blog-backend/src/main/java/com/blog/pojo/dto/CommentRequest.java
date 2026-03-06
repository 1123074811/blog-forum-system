package com.blog.pojo.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Data
public class CommentRequest {
    @NotNull(message = "文章ID不能为空")
    private Long articleId;
    
    private Long parentId;
    
    @NotBlank(message = "评论内容不能为空")
    @Size(min = 1, max = 1000, message = "评论内容长度必须在1-1000之间")
    private String content;
}
