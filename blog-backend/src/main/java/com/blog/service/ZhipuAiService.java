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

    /**
     * 翻译数据库字段名或表名为中文
     * @param text 要翻译的文本
     * @param context 上下文类型：database_field 或 table
     * @return 翻译后的中文名称
     */
    public String translateDatabaseName(String text, String context) {
        log.info("开始翻译: text={}, context={}", text, context);
        
        try {
            String prompt;
            if ("table".equals(context)) {
                prompt = String.format(
                    "请将以下数据库表名翻译成简洁的中文名称，只返回翻译结果，不要解释：\n表名：%s\n\n" +
                    "翻译要求：\n" +
                    "1. 返回2-4个汉字的简洁名称\n" +
                    "2. 体现表的业务含义\n" +
                    "3. 不要包含\"表\"字\n" +
                    "4. 只返回翻译结果，不要其他内容",
                    text
                );
            } else {
                prompt = String.format(
                    "请将以下数据库字段名翻译成简洁的中文名称，只返回翻译结果，不要解释：\n字段名：%s\n\n" +
                    "翻译要求：\n" +
                    "1. 返回2-6个汉字的简洁名称\n" +
                    "2. 体现字段的业务含义\n" +
                    "3. 符合中文表达习惯\n" +
                    "4. 只返回翻译结果，不要其他内容",
                    text
                );
            }

            log.debug("翻译提示词: {}", prompt);

            Map<String, Object> body = Map.of(
                "model", "glm-4-flash",
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "temperature", 0.1  // 降低随机性，提高翻译一致性
            );

            log.debug("请求体: {}", objectMapper.writeValueAsString(body));

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://open.bigmodel.cn/api/paas/v4/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .timeout(Duration.ofSeconds(30))
                .build();

            log.info("发送智谱AI请求...");
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            log.info("智谱AI响应状态码: {}", response.statusCode());
            log.debug("智谱AI响应内容: {}", response.body());
            
            if (response.statusCode() != 200) {
                log.error("智谱AI翻译API调用失败，状态码：{}, 响应：{}", response.statusCode(), response.body());
                return null;
            }

            JsonNode root = objectMapper.readTree(response.body());
            String result = root.path("choices").get(0).path("message").path("content").asText().trim();
            
            // 清理结果，移除可能的引号和多余字符
            result = result.replaceAll("^[\"'`]|[\"'`]$", "").trim();
            
            log.info("翻译成功：{} -> {}", text, result);
            return result;
            
        } catch (Exception e) {
            log.error("智谱AI翻译调用失败，原文：{}", text, e);
            return null;
        }
    }
}
