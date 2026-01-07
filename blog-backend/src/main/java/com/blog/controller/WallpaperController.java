package com.blog.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.dto.ApiResponse;
import com.blog.entity.Media;
import com.blog.mapper.MediaMapper;
import com.blog.service.MinioService;
import com.blog.util.DateUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
@RequestMapping("/api/wallpaper")
@RequiredArgsConstructor
public class WallpaperController {

    private final MediaMapper mediaMapper;
    private final MinioService minioService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final AtomicBoolean bingFetching = new AtomicBoolean(false);

    @org.springframework.beans.factory.annotation.Value("${weather.default-city:北京}")
    private String defaultCity;

    @GetMapping("/bing")
    public ApiResponse<List<Map<String, String>>> getBingWallpapers() {
        // 检查今天的壁纸是否已存在
        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String todayKey = "bing_" + today;
        Media todayWallpaper = mediaMapper.selectOne(new LambdaQueryWrapper<Media>().like(Media::getUrl, todayKey).last("LIMIT 1"));

        // 如果今天的壁纸已存在，直接返回缓存
        if (todayWallpaper != null) {
            List<Media> cached = mediaMapper.selectList(new LambdaQueryWrapper<Media>().eq(Media::getSource, "bing").orderByDesc(Media::getId).last("LIMIT 8"));
            return ApiResponse.success(cached.stream().map(m -> Map.of("url", m.getUrl(), "title", m.getTitle())).toList());
        }

        // 防止并发爬取
        if (!bingFetching.compareAndSet(false, true)) {
            List<Media> cached = mediaMapper.selectList(new LambdaQueryWrapper<Media>().eq(Media::getSource, "bing").orderByDesc(Media::getId).last("LIMIT 8"));
            if (!cached.isEmpty()) {
                return ApiResponse.success(cached.stream().map(m -> Map.of("url", m.getUrl(), "title", m.getTitle())).toList());
            }
            return ApiResponse.error("正在加载中，请稍后刷新");
        }
        try {
            return doFetchBingWallpapers();
        } finally {
            bingFetching.set(false);
        }
    }

    private ApiResponse<List<Map<String, String>>> doFetchBingWallpapers() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://www.bing.com/HPImageArchive.aspx?format=js&idx=0&n=8&mkt=zh-CN"))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode images = root.get("images");
            if (images == null || !images.isArray()) {
                return ApiResponse.success(List.of());
            }

            // 并行处理所有图片
            List<CompletableFuture<Map<String, String>>> futures = new ArrayList<>();
            for (JsonNode img : images) {
                String bingUrl = "https://www.bing.com" + img.get("url").asText();
                String title = img.has("copyright") ? img.get("copyright").asText() : "Bing Wallpaper";
                String startdate = img.has("startdate") ? img.get("startdate").asText() : "";
                String todayKey = "bing_" + startdate;

                futures.add(CompletableFuture.supplyAsync(() -> {
                    try {
                        Media existing = mediaMapper.selectOne(new LambdaQueryWrapper<Media>().like(Media::getUrl, todayKey).last("LIMIT 1"));
                        String minioUrl;
                        if (existing != null) {
                            minioUrl = existing.getUrl();
                        } else {
                            minioUrl = minioService.uploadFromUrl(bingUrl, todayKey + ".jpg");
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
                        }
                        return Map.of("url", minioUrl, "title", title);
                    } catch (Exception e) {
                        return Map.of("url", bingUrl, "title", title);
                    }
                }));
            }

            List<Map<String, String>> wallpapers = futures.stream()
                    .map(CompletableFuture::join)
                    .toList();
            return ApiResponse.success(wallpapers);
        } catch (Exception e) {
            return ApiResponse.error("获取壁纸失败: " + e.getMessage());
        }
    }

    @GetMapping("/douyin-hot")
    public ApiResponse<List<Map<String, String>>> getDouyinHot() {
        List<Map<String, String>> hotList = new ArrayList<>();
        try {
            Document doc = Jsoup.connect("https://www.iesdouyin.com/web/api/v2/hotsearch/billboard/word/")
                    .ignoreContentType(true)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(10000)
                    .get();
            String json = doc.body().text();
            JsonNode root = objectMapper.readTree(json);
            JsonNode list = root.get("word_list");
            if (list != null && list.isArray()) {
                int count = 0;
                for (JsonNode item : list) {
                    if (count >= 10) break;
                    String word = item.get("word").asText();
                    String hotValue = item.has("hot_value") ? item.get("hot_value").asText() : "";
                    hotList.add(Map.of("title", word, "hot", hotValue));
                    count++;
                }
            }
        } catch (Exception e) {
            return ApiResponse.error("获取热榜失败: " + e.getMessage());
        }
        return ApiResponse.success(hotList);
    }

    @GetMapping("/weather")
    public ApiResponse<Map<String, Object>> getWeather(HttpServletRequest request) {
        try {
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty()) ip = request.getRemoteAddr();
            if (ip.contains(",")) ip = ip.split(",")[0].trim();

            // 本地IP使用默认城市
            String cacheKey = "weather:" + (ip.startsWith("127.") || ip.startsWith("0:") ? "default" : ip);

            // 尝试从缓存获取
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return ApiResponse.success(objectMapper.readValue(cached, Map.class));
            }

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(5))
                    .build();
            String city = defaultCity;
            String region = "";

            // 获取IP定位
            if (!ip.startsWith("127.") && !ip.startsWith("0:")) {
                try {
                    HttpRequest locReq = HttpRequest.newBuilder()
                            .uri(URI.create("http://whois.pconline.com.cn/ipJson.jsp?ip=" + ip + "&json=true"))
                            .timeout(java.time.Duration.ofSeconds(3))
                            .GET().build();
                    HttpResponse<String> locRes = client.send(locReq, HttpResponse.BodyHandlers.ofString());
                    JsonNode loc = objectMapper.readTree(locRes.body());
                    if (loc.has("city") && !loc.get("city").asText().isEmpty()) {
                        city = loc.get("city").asText().replace("市", "");
                    }
                    if (loc.has("pro")) region = loc.get("pro").asText();
                } catch (Exception ignored) {}
            }

            // 获取天气
            HttpRequest weatherReq = HttpRequest.newBuilder()
                    .uri(URI.create("http://wttr.in/" + java.net.URLEncoder.encode(city, "UTF-8") + "?format=j1"))
                    .header("Accept-Language", "zh-CN")
                    .header("User-Agent", "curl/7.64.1")
                    .timeout(java.time.Duration.ofSeconds(10))
                    .GET().build();
            HttpResponse<String> weatherRes = client.send(weatherReq, HttpResponse.BodyHandlers.ofString());
            JsonNode weatherData = objectMapper.readTree(weatherRes.body());

            JsonNode current = weatherData.get("current_condition").get(0);
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("city", city);
            result.put("region", region);
            result.put("temp", current.get("temp_C").asText());
            result.put("feelsLike", current.get("FeelsLikeC").asText());
            result.put("humidity", current.get("humidity").asText());
            result.put("desc", current.has("lang_zh") && current.get("lang_zh").size() > 0
                    ? current.get("lang_zh").get(0).get("value").asText()
                    : current.get("weatherDesc").get(0).get("value").asText());
            result.put("date", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日 E", java.util.Locale.CHINESE)));

            // 缓存30分钟
            stringRedisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(result), 30, TimeUnit.MINUTES);

            return ApiResponse.success(result);
        } catch (Exception e) {
            return ApiResponse.error("获取天气失败: " + e.getMessage());
        }
    }
}
