package com.blog.service.crawler.impl;

import com.blog.service.crawler.ArticleParser;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

/**
 * 掘金文章解析器
 */
@Component
public class JuejinParser implements ArticleParser {

    @Override
    public boolean supports(String url) {
        return url.contains("juejin.cn") || url.contains("juejin.im");
    }

    @Override
    public String parseTitle(Document doc) {
        // 掘金标题选择器
        Element titleElement = doc.selectFirst("h1.article-title");
        if (titleElement == null) {
            titleElement = doc.selectFirst("h1");
        }
        return titleElement != null ? titleElement.text() : "未知标题";
    }

    @Override
    public String parseContent(Document doc) {
        // 掘金文章内容选择器
        Element contentElement = doc.selectFirst("div.markdown-body");
        if (contentElement == null) {
            contentElement = doc.selectFirst("article");
        }

        if (contentElement != null) {
            // 移除不需要的元素
            contentElement.select(".copy-code-btn").remove();
            return contentElement.html();
        }

        return "";
    }

    @Override
    public String getPlatformName() {
        return "掘金";
    }
}
