package com.blog.service;

import com.blog.pojo.entity.SecurityEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
@Slf4j
public class AlertService {

    @Value("${alert.webhook:}")
    private String webhookUrl;

    public void sendAlert(SecurityEvent event) {
        if (event == null) {
            return;
        }
        String title = "Security Event: " + event.getOperation();
        String content = String.format("ip=%s userId=%s result=%s path=%s reason=%s", event.getIp(), event.getUserId(), event.getResult(), event.getPath(), event.getReasonCode());
        sendAlert(title, content);
    }

    public void sendAlert(String title, String content) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return;
        }

        String safeTitle = title == null ? "Security Alert" : title.replace("\"", "'");
        String safeContent = content == null ? "" : content.replace("\"", "'");
        String body = String.format("{\"msgtype\":\"markdown\",\"markdown\":{\"content\":\"## %s\\n%s\"}}", safeTitle, safeContent);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            log.warn("Failed to send security alert", e);
        }
    }
}
