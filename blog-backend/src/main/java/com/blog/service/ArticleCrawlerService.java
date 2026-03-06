package com.blog.service;

import com.blog.pojo.dto.CrawlResponse;
import com.blog.service.crawler.ArticleParser;
import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文章爬虫服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleCrawlerService {

    private final List<ArticleParser> parsers;
    private final MinioService minioService;

    /**
     * 爬取文章
     */
    public CrawlResponse crawlArticle(String url) {
        try {
            log.info("开始爬取文章: {}", url);

            // 验证URL格式
            if (url == null || url.trim().isEmpty()) {
                throw new RuntimeException("请提供文章链接");
            }

            // 查找支持的解析器
            ArticleParser parser = findParser(url);
            if (parser == null) {
                log.warn("不支持的网站: {}", url);
                throw new RuntimeException("暂不支持该网站，目前支持：CSDN、掘金、博客园、知乎");
            }

            log.info("使用解析器: {}", parser.getPlatformName());

            // 获取网页内容
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .timeout(15000)
                    .ignoreHttpErrors(true)
                    .followRedirects(true)
                    .get();

            log.info("网页获取成功，开始解析");

            // 解析标题
            String title = parser.parseTitle(doc);
            log.info("解析到标题: {}", title);

            // 解析内容
            String htmlContent = parser.parseContent(doc);
            if (htmlContent == null || htmlContent.trim().isEmpty()) {
                log.error("无法解析文章内容，HTML为空");
                throw new RuntimeException("无法解析文章内容，请检查链接是否正确");
            }

            log.info("内容解析成功，长度: {}", htmlContent.length());

            // 处理图片：下载并上传到MinIO
            htmlContent = processImages(htmlContent);

            // 转换为Markdown
            String markdownContent = convertHtmlToMarkdown(htmlContent);
            log.info("Markdown转换成功，长度: {}", markdownContent.length());

            // 添加来源声明
            markdownContent = addSourceFooter(markdownContent, url);

            log.info("文章爬取成功");

            return CrawlResponse.builder()
                    .title(title)
                    .content(markdownContent)
                    .sourceUrl(url)
                    .build();

        } catch (Exception e) {
            log.error("爬取文章失败: {}", e.getMessage(), e);
            if (e.getMessage() != null && (e.getMessage().contains("支持") || e.getMessage().contains("链接"))) {
                throw new RuntimeException(e.getMessage());
            }
            throw new RuntimeException("无法解析该链接，请检查链接是否正确。错误: " + e.getMessage());
        }
    }

    /**
     * 查找支持的解析器
     */
    private ArticleParser findParser(String url) {
        return parsers.stream()
                .filter(parser -> parser.supports(url))
                .findFirst()
                .orElse(null);
    }

    /**
     * 处理图片：下载并上传到MinIO
     */
    private String processImages(String htmlContent) {
        try {
            Document doc = Jsoup.parse(htmlContent);
            Elements images = doc.select("img");

            for (Element img : images) {
                String originalSrc = img.attr("src");
                if (originalSrc == null || originalSrc.isEmpty()) {
                    continue;
                }

                // 处理相对路径
                if (originalSrc.startsWith("//")) {
                    originalSrc = "https:" + originalSrc;
                } else if (originalSrc.startsWith("/")) {
                    continue; // 跳过相对路径
                }

                // 跳过base64图片
                if (originalSrc.startsWith("data:")) {
                    continue;
                }

                try {
                    // 生成文件名
                    String extension = getImageExtension(originalSrc);
                    String filename = "crawled/" + UUID.randomUUID() + extension;

                    // 下载并上传到MinIO
                    String newUrl = minioService.uploadFromUrl(originalSrc, filename);
                    img.attr("src", newUrl);

                    log.info("图片上传成功: {} -> {}", originalSrc, newUrl);
                } catch (Exception e) {
                    log.warn("图片上传失败，保留原链接: {}", originalSrc, e);
                    // 保留原链接
                }
            }

            return doc.body().html();
        } catch (Exception e) {
            log.error("处理图片失败", e);
            return htmlContent;
        }
    }

    /**
     * 获取图片扩展名
     */
    private String getImageExtension(String url) {
        Pattern pattern = Pattern.compile("\\.(jpg|jpeg|png|gif|webp|bmp)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            return "." + matcher.group(1).toLowerCase();
        }
        return ".jpg"; // 默认扩展名
    }

    /**
     * 将HTML转换为Markdown
     */
    private String convertHtmlToMarkdown(String html) {
        FlexmarkHtmlConverter converter = FlexmarkHtmlConverter.builder().build();
        String markdown = converter.convert(html);

        // 清理Markdown中的锚点ID语法 {#xxx}
        markdown = markdown.replaceAll("\\s*\\{#[^}]+\\}", "");

        // 清理标题中的多余星号和分隔符
        markdown = markdown.replaceAll("(#{1,6})\\s*\\*+\\s*\\*+\\s*\\*+\\s*\\*+\\s*\\*+", "$1");
        markdown = markdown.replaceAll("\\*\\*\\* \\*\\* \\* \\*\\* \\*\\*\\*", "---");

        // 修复标题格式：确保#后有空格
        markdown = markdown.replaceAll("(#{1,6})([^\\s#])", "$1 $2");

        // 清理多余的空行（超过2个连续空行）
        markdown = markdown.replaceAll("\\n{3,}", "\n\n");

        // 清理标题前后多余的空格
        markdown = markdown.replaceAll("(#{1,6})\\s+([^\\n]+?)\\s*\\n", "$1 $2\n");

        return markdown;
    }

    /**
     * 添加来源声明
     */
    private String addSourceFooter(String content, String sourceUrl) {
        // 提取域名作为链接文本
        String linkText = "原文链接";
        try {
            java.net.URL url = new java.net.URL(sourceUrl);
            linkText = url.getHost();
        } catch (Exception e) {
            // 如果解析失败，使用默认文本
        }
        return content + "\n\n---\n\n> 本文转载自：[" + linkText + "](" + sourceUrl + ")";
    }
}
