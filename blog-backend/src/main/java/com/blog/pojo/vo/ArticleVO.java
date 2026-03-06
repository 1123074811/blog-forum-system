package com.blog.pojo.vo;

import lombok.Data;

/**
 * 文章视图对象
 */
@Data
public class ArticleVO {
    private Long id;
    private Long userId;
    private String title;
    private String content;
    private Long categoryId;
    private String status;
    private Integer viewCount;
    private Long likeCount;
    private Boolean liked;
    private Long hotScore;
    private String createdAt;
    private String updatedAt;

    // 作者信息
    private String authorName;
    private String authorAvatar;
}
