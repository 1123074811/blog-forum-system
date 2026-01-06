package com.blog.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.entity.Media;
import com.blog.mapper.MediaMapper;
import com.blog.service.MinioService;
import com.blog.util.DateUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Slf4j
@Component
@RequiredArgsConstructor
public class BingWallpaperTask {

    private final MediaMapper mediaMapper;
    private final MinioService minioService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Scheduled(cron = "0 5 0 * * ?")
    public void fetchDailyBingWallpaper() {
        log.info("开始爬取必���每日壁纸...");
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://www.bing.com/HPImageArchive.aspx?format=js&idx=0&n=1&mkt=zh-CN"))
                    .GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode images = objectMapper.readTree(response.body()).get("images");
            if (images != null && images.isArray() && images.size() > 0) {
                JsonNode img = images.get(0);
                String bingUrl = "https://www.bing.com" + img.get("url").asText();
                String title = img.has("copyright") ? img.get("copyright").asText() : "Bing Wallpaper";
                String startdate = img.has("startdate") ? img.get("startdate").asText() : "";
                String todayKey = "bing_" + startdate;

                Media existing = mediaMapper.selectOne(new LambdaQueryWrapper<Media>().like(Media::getUrl, todayKey).last("LIMIT 1"));
                if (existing == null) {
                    String minioUrl = minioService.uploadFromUrl(bingUrl, todayKey + ".jpg");
                    Media media = new Media();
                    media.setUserId(1L);
                    media.setTitle(title);
                    media.setUrl(minioUrl);
                    media.setType("image");
                    media.setIsPublic(true);
                    media.setSource("bing");
                    media.setCreatedAt(DateUtil.now());
                    media.setUpdatedAt(DateUtil.now());
                    mediaMapper.insert(media);
                    log.info("必应壁纸保存成功: {}", title);
                } else {
                    log.info("今日壁纸已存在，跳过");
                }
            }
        } catch (Exception e) {
            log.error("爬取必应壁纸失败: {}", e.getMessage());
        }
    }
}
