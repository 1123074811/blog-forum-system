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
     * 清理 XSS 脚本 —— 转义所有 HTML 特殊字符，彻底防止注入
     */
    private String cleanXSS(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        value = value.replace("&", "&amp;");
        value = value.replace("<", "&lt;");
        value = value.replace(">", "&gt;");
        value = value.replace("\"", "&quot;");
        value = value.replace("'", "&#x27;");
        value = value.replace("/", "&#x2F;");
        return value;
    }
}
