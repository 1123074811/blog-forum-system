package com.blog.service.crawler.impl;

import com.blog.service.crawler.ArticleParser;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

/**
 * CSDN文章解析器
 */
@Slf4j
@Component
public class CsdnParser implements ArticleParser {

    @Override
    public boolean supports(String url) {
        return url.contains("blog.csdn.net") || url.contains("csdn.net");
    }

    @Override
    public String parseTitle(Document doc) {
        // CSDN标题选择器 - 尝试多种可能的选择器
        Element titleElement = doc.selectFirst("h1.title-article");
        if (titleElement == null) {
            titleElement = doc.selectFirst("h1#articleContentId");
        }
        if (titleElement == null) {
            titleElement = doc.selectFirst(".article-title-box h1");
        }
        if (titleElement == null) {
            titleElement = doc.selectFirst("h1");
        }

        String title = titleElement != null ? titleElement.text().trim() : "未知标题";
        log.info("CSDN解析标题: {}", title);
        return title;
    }

    @Override
    public String parseContent(Document doc) {
        // CSDN文章内容选择器 - 尝试多种可能的选择器
        Element contentElement = doc.selectFirst("div#content_views");
        if (contentElement == null) {
            contentElement = doc.selectFirst("div.article_content");
        }
        if (contentElement == null) {
            contentElement = doc.selectFirst("article");
        }
        if (contentElement == null) {
            contentElement = doc.selectFirst("div.markdown_views");
        }

        if (contentElement != null) {
            // 移除不需要的元素
            contentElement.select(".hljs-button").remove();
            contentElement.select(".hide-article-box").remove();
            contentElement.select("script").remove();
            contentElement.select("style").remove();
            contentElement.select(".comment-box").remove();

            String html = contentElement.html();
            log.info("CSDN解析内容成功，长度: {}", html.length());
            return html;
        }

        log.error("CSDN内容解析失败，未找到内容元素");
        return "";
    }

    @Override
    public String getPlatformName() {
        return "CSDN";
    }
}
