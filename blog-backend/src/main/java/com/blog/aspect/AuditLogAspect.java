package com.blog.aspect;

import com.blog.annotation.AuditLog;
import com.blog.context.BaseContext;
import com.blog.mapper.SecurityEventMapper;
import com.blog.pojo.entity.SecurityEvent;
import com.blog.service.AlertService;
import com.blog.util.DateUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.Map;

/**
 * 审计日志切面
 * 记录关键操作的详细信息
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final ObjectMapper objectMapper;
    private final SecurityEventMapper securityEventMapper;
    private final AlertService alertService;

    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 获取请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;
        // 获取当前用户ID
        Long userId = BaseContext.getCurrentId();

        // 获取方法信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        // 构建审计日志
        Map<String, Object> auditInfo = new HashMap<>();
        auditInfo.put("module", auditLog.module());
        auditInfo.put("operation", auditLog.operation());
        auditInfo.put("className", signature.getDeclaringTypeName());
        auditInfo.put("methodName", signature.getName());

        if (request != null) {
            auditInfo.put("requestMethod", request.getMethod());
            auditInfo.put("requestUrl", request.getRequestURI());
            auditInfo.put("ip", getClientIp(request));
            auditInfo.put("userAgent", request.getHeader("User-Agent"));
        }

        // 记录请求参数（敏感信息需要脱敏）
        try {
            auditInfo.put("params", desensitize(objectMapper.writeValueAsString(joinPoint.getArgs())));
        } catch (Exception ignored) {
            auditInfo.put("params", "serialize_failed");
        }

        String status = "SUCCESS";
        String errorMessage = null;

        try {
            return joinPoint.proceed();
        } catch (Exception e) {
            status = "FAILURE";
            errorMessage = e.getMessage();
            throw e;
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;
            auditInfo.put("status", status);
            auditInfo.put("executionTime", executionTime + "ms");
            if (errorMessage != null) {
                auditInfo.put("errorMessage", errorMessage);
            }

            // 记录审计日志
            SecurityEvent event = SecurityEvent.builder()
                    .traceId(MDC.get("traceId"))
                    .userId(userId)
                    .ip((String) auditInfo.get("ip"))
                    .userAgent((String) auditInfo.get("userAgent"))
                    .method((String) auditInfo.get("requestMethod"))
                    .path((String) auditInfo.get("requestUrl"))
                    .operation(auditLog.operation())
                    .result(status)
                    .reasonCode(errorMessage == null ? "OK" : "EXCEPTION")
                    .detail((String) auditInfo.get("params"))
                    .createdAt(DateUtil.now())
                    .build();

            try {
                securityEventMapper.insert(event);
            } catch (Exception insertEx) {
                log.warn("Failed to persist security event", insertEx);
            }

            if ("FAILURE".equals(status) || isHighRiskOperation(auditLog.operation())) {
                alertService.sendAlert(event);
            }

            if ("FAILURE".equals(status)) {
                log.error("[AUDIT] {}", objectToJson(auditInfo));
            } else {
                log.info("[AUDIT] {}", objectToJson(auditInfo));
            }
        }
    }

    private boolean isHighRiskOperation(String operation) {
        if (operation == null) {
            return false;
        }
        String op = operation.toLowerCase();
        return op.contains("delete")
                || op.contains("ban")
                || op.contains("revoke")
                || op.contains("封")
                || op.contains("删");
    }

    private String objectToJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            return String.valueOf(object);
        }
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 处理多个IP的情况，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 敏感信息脱敏
     */
    private String desensitize(String content) {
        if (content == null) {
            return null;
        }
        String value = content;
        // 脱敏密码字段
        value = value.replaceAll("\\\"password\\\"\\s*:\\s*\\\"[^\\\"]*\\\"", "\\\"password\\\":\\\"******\\\"");
        value = value.replaceAll("\\\"oldPassword\\\"\\s*:\\s*\\\"[^\\\"]*\\\"", "\\\"oldPassword\\\":\\\"******\\\"");
        value = value.replaceAll("\\\"newPassword\\\"\\s*:\\s*\\\"[^\\\"]*\\\"", "\\\"newPassword\\\":\\\"******\\\"");
        // 脱敏手机号
        value = value.replaceAll("\\\"phone\\\"\\s*:\\s*\\\"(\\d{3})\\d{4}(\\d{4})\\\"", "\\\"phone\\\":\\\"$1****$2\\\"");
        return value;
    }
}
