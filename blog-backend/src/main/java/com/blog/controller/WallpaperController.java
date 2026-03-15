package com.blog.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.pojo.dto.ApiResponse;
import com.blog.pojo.entity.Media;
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
    @org.springframework.beans.factory.annotation.Value("${amap.key:}")
    private String amapKey;
    @org.springframework.beans.factory.annotation.Value("${amap.weather.extensions:base}")
    private String amapWeatherExtensions;

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
    public ApiResponse<Map<String, Object>> getWeather(HttpServletRequest request,
                                                       @org.springframework.web.bind.annotation.RequestParam(required = false) Boolean debug) {
        try {
            String ip = getClientIp(request);
            boolean isLocalIp = ip.startsWith("127.") || ip.startsWith("0:");
            if (isLocalIp) {
                String publicIp = getPublicIp();
                if (publicIp != null && !publicIp.isBlank()) {
                    ip = publicIp;
                    isLocalIp = false;
                }
            }

            // 本地IP使用默认城市
            String cacheKey = "weather:v2:" + (isLocalIp ? "default" : ip);

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
            String adcode = null;

            // 获取IP定位（高德）
            if (!isLocalIp) {
                GeoInfo geo = getGeoByIpFromAmap(client, ip);
                if (geo != null) {
                    if (geo.city != null && !geo.city.isBlank()) {
                        city = geo.city.replace("市", "");
                    }
                    if (geo.region != null) {
                        region = geo.region;
                    }
                    adcode = geo.adcode;
                }
            }

            // 获取天气（高德）
            boolean usedAdcode = adcode != null && !adcode.isBlank();
            String cityParam = usedAdcode ? adcode : city;
            HttpRequest weatherReq = HttpRequest.newBuilder()
                    .uri(URI.create("https://restapi.amap.com/v3/weather/weatherInfo?key="
                            + java.net.URLEncoder.encode(amapKey, "UTF-8")
                            + "&city=" + java.net.URLEncoder.encode(cityParam, "UTF-8")
                            + "&extensions=" + java.net.URLEncoder.encode(amapWeatherExtensions, "UTF-8")
                            + "&output=JSON"))
                    .timeout(java.time.Duration.ofSeconds(10))
                    .GET().build();
            HttpResponse<String> weatherRes = client.send(weatherReq, HttpResponse.BodyHandlers.ofString());
            JsonNode weatherData = objectMapper.readTree(weatherRes.body());
            if (!"1".equals(weatherData.path("status").asText())) {
                return ApiResponse.error("获取天气失败: " + weatherData.path("info").asText("未知错误"));
            }

            Map<String, Object> result = new java.util.HashMap<>();
            if ("all".equalsIgnoreCase(amapWeatherExtensions)) {
                JsonNode forecasts = weatherData.path("forecasts");
                JsonNode first = forecasts.isArray() && forecasts.size() > 0 ? forecasts.get(0) : null;
                JsonNode cast = first != null && first.path("casts").isArray() && first.path("casts").size() > 0
                        ? first.path("casts").get(0) : null;
                if (first != null) {
                    city = first.path("city").asText(city);
                    region = first.path("province").asText(region);
                }
                if (cast != null) {
                    result.put("temp", cast.path("daytemp").asText());
                    result.put("feelsLike", cast.path("daytemp").asText());
                    result.put("humidity", "");
                    result.put("desc", cast.path("dayweather").asText());
                    String dateStr = cast.path("date").asText();
                    result.put("date", formatDateFromAmap(dateStr));
                }
            } else {
                JsonNode lives = weatherData.path("lives");
                JsonNode live = lives.isArray() && lives.size() > 0 ? lives.get(0) : null;
                if (live != null) {
                    city = live.path("city").asText(city);
                    region = live.path("province").asText(region);
                    result.put("temp", live.path("temperature").asText());
                    result.put("feelsLike", live.path("temperature").asText());
                    result.put("humidity", live.path("humidity").asText());
                    result.put("desc", live.path("weather").asText());
                    String reportTime = live.path("reporttime").asText();
                    result.put("date", formatDateFromAmap(reportTime));
                }
            }
            result.put("city", city);
            result.put("region", region);
            if (Boolean.TRUE.equals(debug)) {
                result.put("_debug_ip", ip);
                result.put("_debug_adcode", adcode);
                result.put("_debug_usedAdcode", usedAdcode);
                result.put("_debug_provider", "amap");
            }

            // 缓存30分钟
            stringRedisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(result), 30, TimeUnit.MINUTES);

            return ApiResponse.success(result);
        } catch (Exception e) {
            return ApiResponse.error("获取天气失败: " + e.getMessage());
        }
    }

    private static class GeoInfo {
        String city;
        String region;
        String adcode;
    }

    private GeoInfo getGeoByIpFromAmap(HttpClient client, String ip) {
        if (amapKey == null || amapKey.isBlank()) return null;
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://restapi.amap.com/v3/ip?key="
                            + java.net.URLEncoder.encode(amapKey, "UTF-8")
                            + "&ip=" + java.net.URLEncoder.encode(ip, "UTF-8")
                            + "&output=JSON"))
                    .timeout(java.time.Duration.ofSeconds(3))
                    .GET().build();
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            String body = res.body() == null ? "" : res.body().trim();
            if (body.isEmpty()) return null;
            JsonNode node = objectMapper.readTree(body);
            if (!"1".equals(node.path("status").asText())) return null;
            GeoInfo geo = new GeoInfo();
            geo.city = node.path("city").asText(null);
            geo.region = node.path("province").asText(null);
            geo.adcode = node.path("adcode").asText(null);
            return geo;
        } catch (Exception ignored) {}
        return null;
    }

    private String getClientIp(HttpServletRequest request) {
        // 只信任 X-Real-IP（由 Nginx 注入的真实 IP），不信任客户端可伪造的 X-Forwarded-For
        String ip = request.getHeader("X-Real-IP");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("CF-Connecting-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip == null ? "" : ip.trim();
    }

    private String getPublicIp() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(3))
                    .build();
            String[] urls = {
                    "https://api.ipify.org?format=json",
                    "https://api64.ipify.org?format=json",
                    "https://ipv4.icanhazip.com",
                    "https://ifconfig.me/ip",
                    "https://ipinfo.io/ip"
            };
            for (String url : urls) {
                try {
                    HttpRequest req = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .timeout(java.time.Duration.ofSeconds(3))
                            .GET().build();
                    HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
                    String body = res.body() == null ? "" : res.body().trim();
                    String ip = extractIp(body);
                    if (ip != null && !ip.isBlank()) {
                        return ip;
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String extractIp(String body) {
        if (body == null || body.isBlank()) return null;
        try {
            JsonNode node = objectMapper.readTree(body);
            if (node.has("ip")) {
                return node.get("ip").asText();
            }
        } catch (Exception ignored) {}
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("([0-9]{1,3}\\.){3}[0-9]{1,3}|([0-9a-fA-F]{0,4}:){2,7}[0-9a-fA-F]{0,4}")
                .matcher(body);
        return matcher.find() ? matcher.group() : null;
    }

    private String formatDateFromAmap(String raw) {
        try {
            if (raw == null || raw.isBlank()) {
                return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日 E", java.util.Locale.CHINESE));
            }
            String datePart = raw.length() >= 10 ? raw.substring(0, 10) : raw;
            LocalDate date = LocalDate.parse(datePart);
            return date.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日 E", java.util.Locale.CHINESE));
        } catch (Exception ignored) {}
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日 E", java.util.Locale.CHINESE));
    }
}
