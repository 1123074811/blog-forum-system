package com.blog.service.crawler.impl;

import com.blog.service.crawler.ArticleParser;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

/**
 * 知乎文章解析器
 */
@Component
public class ZhihuParser implements ArticleParser {

    @Override
    public boolean supports(String url) {
        return url.contains("zhuanlan.zhihu.com");
    }

    @Override
    public String parseTitle(Document doc) {
        // 知乎标题选择器
        Element titleElement = doc.selectFirst("h1.Post-Title");
        if (titleElement == null) {
            titleElement = doc.selectFirst("h1");
        }
        return titleElement != null ? titleElement.text() : "未知标题";
    }

    @Override
    public String parseContent(Document doc) {
        // 知乎文章内容选择器
        Element contentElement = doc.selectFirst("div.Post-RichTextContainer");
        if (contentElement == null) {
            contentElement = doc.selectFirst("div.RichText");
        }
        if (contentElement == null) {
            contentElement = doc.selectFirst("article");
        }

        if (contentElement != null) {
            return contentElement.html();
        }

        return "";
    }

    @Override
    public String getPlatformName() {
        return "知乎";
    }
}
