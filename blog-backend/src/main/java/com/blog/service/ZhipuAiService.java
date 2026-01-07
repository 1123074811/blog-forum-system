package com.blog.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ZhipuAiService {

    @Value("${zhipu.api-key}")
    private String apiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();

    public String generateSummary(String content) {
        try {
            String text = content.length() > 2000 ? content.substring(0, 2000) : content;
            Map<String, Object> body = Map.of(
                "model", "glm-4-flash",
                "messages", List.of(Map.of("role", "user", "content", "请用一句话总结以下文章的核心内容，不超过50字：\n\n" + text))
            );
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://open.bigmodel.cn/api/paas/v4/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .timeout(Duration.ofSeconds(30))
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());
            return root.path("choices").get(0).path("message").path("content").asText();
        } catch (Exception e) {
            log.error("智谱AI调用失败", e);
            return null;
        }
    }
}
