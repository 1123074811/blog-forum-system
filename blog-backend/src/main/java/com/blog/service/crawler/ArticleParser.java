package com.blog.service.crawler;

import org.jsoup.nodes.Document;

/**
 * 文章解析器接口
 */
public interface ArticleParser {
    /**
     * 判断是否支持该URL
     */
    boolean supports(String url);

    /**
     * 解析文章标题
     */
    String parseTitle(Document doc);

    /**
     * 解析文章内容（HTML）
     */
    String parseContent(Document doc);

    /**
     * 获取平台名称
     */
    String getPlatformName();
}
