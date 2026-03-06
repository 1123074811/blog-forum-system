package com.blog.pojo.dto;

import lombok.Data;

/**
 * 文章爬取请求DTO
 */
@Data
public class CrawlRequest {
    /**
     * 文章URL
     */
    private String url;
}
