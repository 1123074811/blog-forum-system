package com.blog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文章爬取响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrawlResponse {
    /**
     * 文章标题
     */
    private String title;

    /**
     * 文章内容（Markdown格式）
     */
    private String content;

    /**
     * 原文链接
     */
    private String sourceUrl;
}
