package com.blog.dto;

import lombok.Data;
import java.util.List;

@Data
public class ArticleRequest {
    private String title;
    private String content;
    private Long categoryId;
    private List<Long> tags;
    private String status;
}
