package com.blog.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * XSS 请求包装器
 */
public class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {

    private byte[] body;

    public XssHttpServletRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);
        // 读取请求体
        body = StreamUtils.copyToByteArray(request.getInputStream());
    }

    @Override
    public String getParameter(String name) {
        String value = super.getParameter(name);
        return cleanXSS(value);
    }

    @Override
    public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(name);
        if (values == null) {
            return null;
        }
        String[] cleanValues = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            cleanValues[i] = cleanXSS(values[i]);
        }
        return cleanValues;
    }

    @Override
    public String getHeader(String name) {
        String value = super.getHeader(name);
        return cleanXSS(value);
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        if (body == null || body.length == 0) {
            return super.getInputStream();
        }

        // 清理 JSON 请求体中的 XSS
        String bodyStr = new String(body, StandardCharsets.UTF_8);
        String contentType = super.getContentType();

        if (contentType != null && contentType.contains("application/json")) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> jsonMap = mapper.readValue(bodyStr, Map.class);
                Map<String, Object> cleanedMap = cleanJsonMap(jsonMap);
                bodyStr = mapper.writeValueAsString(cleanedMap);
                body = bodyStr.getBytes(StandardCharsets.UTF_8);
            } catch (Exception e) {
                // 如果不是有效的 JSON，直接清理字符串
                bodyStr = cleanXSS(bodyStr);
                body = bodyStr.getBytes(StandardCharsets.UTF_8);
            }
        } else {
            bodyStr = cleanXSS(bodyStr);
            body = bodyStr.getBytes(StandardCharsets.UTF_8);
        }

        final ByteArrayInputStream bais = new ByteArrayInputStream(body);
        return new ServletInputStream() {
            @Override
            public int read() throws IOException {
                return bais.read();
            }

            @Override
            public boolean isFinished() {
                return bais.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener listener) {
            }
        };
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }

    /**
     * 清理 JSON Map 中的 XSS
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> cleanJsonMap(Map<String, Object> map) {
        Map<String, Object> cleanedMap = new HashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String) {
                cleanedMap.put(entry.getKey(), cleanXSS((String) value));
            } else if (value instanceof Map) {
                cleanedMap.put(entry.getKey(), cleanJsonMap((Map<String, Object>) value));
            } else {
                cleanedMap.put(entry.getKey(), value);
            }
        }
        return cleanedMap;
    }

    /**
     * 清理 XSS 脚本
     */
    private String cleanXSS(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }

        // 移除脚本标签
        value = value.replaceAll("<script[^>]*?>.*?</script>", "");
        value = value.replaceAll("<iframe[^>]*?>.*?</iframe>", "");
        value = value.replaceAll("<object[^>]*?>.*?</object>", "");
        value = value.replaceAll("<embed[^>]*?>.*?</embed>", "");

        // 移除事件处理器
        value = value.replaceAll("(?i)on\\w+\\s*=", "");

        // 移除 javascript: 协议
        value = value.replaceAll("(?i)javascript:", "");
        value = value.replaceAll("(?i)vbscript:", "");

        // 移除 eval 和 expression
        value = value.replaceAll("(?i)eval\\(", "");
        value = value.replaceAll("(?i)expression\\(", "");

        return value;
    }
}
