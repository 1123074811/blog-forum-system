package com.blog.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
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

    /**
     * 生成文章摘要
     * 使用熔断器和重试机制
     */
    @CircuitBreaker(name = "zhipuAi", fallbackMethod = "generateSummaryFallback")
    @Retry(name = "zhipuAi")
    public String generateSummary(String content) {
        try {
            log.info("调用智谱AI生成摘要");
            String text = content.length() > 2000 ? content.substring(0, 2000) : content;
            Map<String, Object> body = Map.of(
                "model", "glm-4-flash",
                "messages", List.of(Map.of("role", "user", "content", "请用一句话总结以下文章的核心内容，不超过100字：\n\n" + text))
            );
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://open.bigmodel.cn/api/paas/v4/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .timeout(Duration.ofSeconds(30))
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                log.error("智谱AI API返回错误状态码: {}", response.statusCode());
                throw new RuntimeException("智谱AI服务返回错误: " + response.statusCode());
            }
            
            JsonNode root = objectMapper.readTree(response.body());
            String summary = root.path("choices").get(0).path("message").path("content").asText();
            log.info("摘要生成成功");
            return summary;
        } catch (Exception e) {
            log.error("智谱AI调用失败", e);
            throw new RuntimeException("智谱AI服务调用失败", e);
        }
    }

    /**
     * 生成摘要的降级方法
     */
    private String generateSummaryFallback(String content, Exception e) {
        log.warn("智谱AI服务降级，使用默认摘要。原因: {}", e.getMessage());
        // 返回文章前100个字符作为摘要
        String fallbackSummary = content.length() > 100 
            ? content.substring(0, 100) + "..." 
            : content;
        return fallbackSummary;
    }

    /**
     * 翻译数据库字段名或表名为中文
     * 使用熔断器和重试机制
     * @param text 要翻译的文本
     * @param context 上下文类型：database_field 或 table
     * @return 翻译后的中文名称
     */
    @CircuitBreaker(name = "zhipuAi", fallbackMethod = "translateDatabaseNameFallback")
    @Retry(name = "zhipuAi")
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
                throw new RuntimeException("智谱AI服务返回错误: " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            String result = root.path("choices").get(0).path("message").path("content").asText().trim();
            
            // 清理结果，移除可能的引号和多余字符
            result = result.replaceAll("^[\"'`]|[\"'`]$", "").trim();
            
            log.info("翻译成功：{} -> {}", text, result);
            return result;
            
        } catch (Exception e) {
            log.error("智谱AI翻译调用失败，原文：{}", text, e);
            throw new RuntimeException("智谱AI翻译服务调用失败", e);
        }
    }

    /**
     * 翻译的降级方法
     */
    private String translateDatabaseNameFallback(String text, String context, Exception e) {
        log.warn("智谱AI翻译服务降级，返回原文。原因: {}", e.getMessage());
        // 简单的驼峰转中文处理
        return text.replaceAll("([A-Z])", " $1").trim();
    }

    /**
     * 生成人生模拟器事件
     * 使用熔断器和重试机制
     */
    @CircuitBreaker(name = "zhipuAi", fallbackMethod = "generateLifeEventFallback")
    @Retry(name = "zhipuAi")
    public Map<String, Object> generateLifeEvent(int age, Map<String, Object> attributes, long wealth, List<String> recentEvents) {
        try {
            String recentEventsStr = recentEvents != null && !recentEvents.isEmpty()
                ? String.join("；", recentEvents)
                : "无";

            String prompt = String.format(
                "你是人生模拟器的事件生成器。根据角色当前状态生成一个人生事件。\n\n" +
                "角色状态：\n" +
                "- 年龄：%d岁\n" +
                "- 颜值：%s\n" +
                "- 智力：%s\n" +
                "- 体质：%s\n" +
                "- 心理：%s\n" +
                "- 家境：%s\n" +
                "- 财富：%d元\n" +
                "- 最近事件：%s\n\n" +
                "请生成一个符合年龄和属性的事件，用JSON格式返回：\n\n" +
                "【普通事件】{\"text\":\"事件描述\",\"effects\":{\"属性名\":变化值}}\n\n" +
                "【选择事件】(20%%概率)：{\"choice\":{\"question\":\"问题\",\"options\":[{\"text\":\"选项1\",\"effects\":{...}},{\"text\":\"选项2\",\"effects\":{...}}]}}\n\n" +
                "【职业事件】可添加career字段：\n" +
                "- 学历变化：\"career\":{\"education\":\"小学/初中/高中/大学/硕士/博士\"}\n" +
                "- 职业变化：\"career\":{\"job\":\"职业名称\",\"salary\":月薪}\n\n" +
                "年龄与学历对应：\n" +
                "- 6岁：上小学\n" +
                "- 12岁：上初中\n" +
                "- 15岁：上高中（智力>6）或职高/辍学\n" +
                "- 18岁：上大学（智力>7）或工作\n" +
                "- 22岁：大学毕业找工作\n" +
                "- 工作后每年有工资收入\n\n" +
                "职业参考（根据学历和智力）：\n" +
                "- 无学历：工人、服务员、快递员（3000-5000/月）\n" +
                "- 高中：销售、文员、技工（4000-8000/月）\n" +
                "- 大学：程序员、教师、会计（8000-20000/月）\n" +
                "- 硕博：研究员、医生、律师（15000-50000/月）\n\n" +
                "属性名可选：looks/intelligence/physique/mental/wealth\n" +
                "属性变化范围-10到+10\n\n" +
                "财富变化参考：学费5000-30000/年，买房50-500万，结婚5-50万\n\n" +
                "只返回JSON，不要其他内容。",
                age,
                attributes.get("looks"),
                attributes.get("intelligence"),
                attributes.get("physique"),
                attributes.get("mental"),
                attributes.get("family"),
                wealth,
                recentEventsStr
            );

            Map<String, Object> body = Map.of(
                "model", "glm-4-flash",
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "temperature", 0.9
            );

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://open.bigmodel.cn/api/paas/v4/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .timeout(Duration.ofSeconds(30))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("人生事件生成失败，状态码：{}", response.statusCode());
                throw new RuntimeException("智谱AI服务返回错误: " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            String content = root.path("choices").get(0).path("message").path("content").asText().trim();

            // 清理可能的markdown代码块标记和前缀文字
            content = content.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();

            // 提取JSON部分（从第一个{到最后一个}）
            int start = content.indexOf('{');
            int end = content.lastIndexOf('}');
            if (start >= 0 && end > start) {
                content = content.substring(start, end + 1);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> result = objectMapper.readValue(content, Map.class);
            return result;
        } catch (Exception e) {
            log.error("人生事件生成失败", e);
            throw new RuntimeException("人生事件生成失败", e);
        }
    }

    /**
     * 人生事件生成的降级方法
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> generateLifeEventFallback(int age, Map<String, Object> attributes, long wealth, List<String> recentEvents, Exception e) {
        log.warn("人生事件生成服务降级，使用默认事件。原因: {}", e.getMessage());
        // 返回一个默认的简单事件
        return Map.of(
            "text", "平凡的一天，没有什么特别的事情发生。",
            "effects", Map.of()
        );
    }
}
