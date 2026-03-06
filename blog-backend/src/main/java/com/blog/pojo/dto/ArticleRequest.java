package com.blog.pojo.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

@Data
public class ArticleRequest {
    @NotBlank(message = "文章标题不能为空")
    @Size(min = 1, max = 200, message = "标题长度必须在1-200之间")
    private String title;

    @NotBlank(message = "文章内容不能为空")
    private String content;

    private Long categoryId;
    private List<Long> tags;
    private String status;
}
