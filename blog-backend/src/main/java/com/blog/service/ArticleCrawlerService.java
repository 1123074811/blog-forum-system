package com.blog.service;

import com.blog.pojo.dto.CrawlResponse;
import com.blog.service.crawler.ArticleParser;
import com.blog.util.UrlSecurityUtil;
import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleCrawlerService {

    private final List<ArticleParser> parsers;
    private final MinioService minioService;

    @CircuitBreaker(name = "crawler", fallbackMethod = "crawlArticleFallback")
    @Retry(name = "crawler")
    public CrawlResponse crawlArticle(String url) {
        try {
            if (url == null || url.trim().isEmpty()) {
                throw new IllegalArgumentException("请提供文章链接");
            }

            url = url.trim();
            UrlSecurityUtil.validatePublicHttpUrl(url);

            ArticleParser parser = findParser(url);
            if (parser == null) {
                throw new IllegalArgumentException("暂不支持该网站，目前支持：CSDN、掘金、博客园、知乎");
            }

            log.info("爬取文章 [{}]: {}", parser.getPlatformName(), url);

            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .timeout(15000)
                    .ignoreHttpErrors(true)
                    .followRedirects(true)
                    .get();

            String title = parser.parseTitle(doc);
            String htmlContent = parser.parseContent(doc);
            if (htmlContent == null || htmlContent.trim().isEmpty()) {
                throw new IllegalStateException("无法解析文章内容，请检查链接是否正确");
            }

            htmlContent = processImages(htmlContent);
            String markdownContent = convertHtmlToMarkdown(htmlContent);
            markdownContent = addSourceFooter(markdownContent, url);

            return CrawlResponse.builder()
                    .title(title)
                    .content(markdownContent)
                    .sourceUrl(url)
                    .build();
        } catch (IllegalArgumentException e) {
            log.error("爬取文章参数错误: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("爬取文章失败: {}", e.getMessage(), e);
            throw new RuntimeException("无法解析该链接，请检查链接是否正确。错误: " + e.getMessage(), e);
        }
    }

    private CrawlResponse crawlArticleFallback(String url, Exception e) {
        log.warn("文章爬取服务降级。URL: {}, 原因: {}", url, e.getMessage());
        return CrawlResponse.builder()
                .title("爬取失败")
                .content("抱歉，文章爬取服务暂时不可用，请稍后再试。\n\n原始链接：" + url)
                .sourceUrl(url)
                .build();
    }

    private ArticleParser findParser(String url) {
        return parsers.stream()
                .filter(parser -> parser.supports(url))
                .findFirst()
                .orElse(null);
    }

    private String processImages(String htmlContent) {
        try {
            Document doc = Jsoup.parse(htmlContent);
            Elements images = doc.select("img");

            for (Element img : images) {
                String originalSrc = img.attr("src");
                if (originalSrc == null || originalSrc.isEmpty()) {
                    continue;
                }

                if (originalSrc.startsWith("//")) {
                    originalSrc = "https:" + originalSrc;
                } else if (originalSrc.startsWith("/")) {
                    continue;
                }

                if (originalSrc.startsWith("data:")) {
                    continue;
                }

                try {
                    String extension = getImageExtension(originalSrc);
                    String filename = "crawled/" + UUID.randomUUID() + extension;
                    String newUrl = minioService.uploadFromUrl(originalSrc, filename);
                    img.attr("src", newUrl);
                } catch (Exception e) {
                    log.warn("图片上传失败，保留原链接: {}", originalSrc);
                }
            }

            return doc.body().html();
        } catch (Exception e) {
            log.error("处理图片失败", e);
            return htmlContent;
        }
    }

    private String getImageExtension(String url) {
        Pattern pattern = Pattern.compile("\\.(jpg|jpeg|png|gif|webp|bmp)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            return "." + matcher.group(1).toLowerCase();
        }
        return ".jpg";
    }

    private String convertHtmlToMarkdown(String html) {
        FlexmarkHtmlConverter converter = FlexmarkHtmlConverter.builder().build();
        String markdown = converter.convert(html);

        markdown = markdown.replaceAll(
                "(?m)^(#{1,6})\\s+(.+?)\\s*\\{#([A-Za-z0-9_-]+)\\}\\s*$",
                "$1 $2 <span id=\"$3\">\u200b</span>"
        );
        markdown = markdown.replaceAll("\\{#[^}]+\\}", "");
        markdown = markdown.replaceAll("(#{1,6})\\s*\\*+\\s*\\*+\\s*\\*+\\s*\\*+\\s*\\*+", "$1");
        markdown = markdown.replaceAll("\\*\\*\\* \\*\\* \\* \\*\\* \\*\\*\\*", "---");
        markdown = markdown.replaceAll("(?m)^(#{1,6})([^\\s#])", "$1 $2");
        markdown = markdown.replaceAll("(?m)^(#{1,6})\\s+([^\\n]{1,12}[：:])\\s*$", "**$2**");
        markdown = markdown.replaceAll("\\n{3,}", "\n\n");
        markdown = markdown.replaceAll("(?m)^(#{1,6})\\s+([^\\n]+?)\\s*$", "$1 $2");

        return markdown;
    }

    private String addSourceFooter(String content, String sourceUrl) {
        String linkText = "原文链接";
        try {
            URL parsed = new URL(sourceUrl);
            linkText = parsed.getHost();
        } catch (Exception ignored) {
        }
        return content + "\n\n---\n\n> 本文转载自：[" + linkText + "](" + sourceUrl + ")";
    }
}
