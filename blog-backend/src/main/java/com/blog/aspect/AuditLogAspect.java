package com.blog.aspect;

import com.blog.annotation.AuditLog;
import com.blog.context.BaseContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        // 获取请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;
        
        // 获取当前用户ID
        Long userId = null;
        try {
            userId = BaseContext.getCurrentId();
        } catch (Exception e) {
            // 未登录用户
        }

        // 获取方法信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringTypeName();
        String methodName = signature.getName();

        // 构建审计日志
        Map<String, Object> auditInfo = new HashMap<>();
        auditInfo.put("timestamp", LocalDateTime.now().format(formatter));
        auditInfo.put("userId", userId);
        auditInfo.put("module", auditLog.module());
        auditInfo.put("operation", auditLog.operation());
        auditInfo.put("description", auditLog.description());
        auditInfo.put("className", className);
        auditInfo.put("methodName", methodName);
        
        if (request != null) {
            auditInfo.put("requestMethod", request.getMethod());
            auditInfo.put("requestUrl", request.getRequestURI());
            auditInfo.put("ip", getClientIp(request));
            auditInfo.put("userAgent", request.getHeader("User-Agent"));
        }

        // 记录请求参数（敏感信息需要脱敏）
        Object[] args = joinPoint.getArgs();
        if (args != null && args.length > 0) {
            try {
                String params = objectMapper.writeValueAsString(args);
                // 脱敏处理
                params = desensitize(params);
                auditInfo.put("params", params);
            } catch (Exception e) {
                auditInfo.put("params", "参数序列化失败");
            }
        }

        Object result = null;
        String status = "SUCCESS";
        String errorMessage = null;

        try {
            // 执行目标方法
            result = joinPoint.proceed();
            return result;
        } catch (Exception e) {
            status = "FAILURE";
            errorMessage = e.getMessage();
            throw e;
        } finally {
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;

            auditInfo.put("status", status);
            auditInfo.put("executionTime", executionTime + "ms");
            
            if (errorMessage != null) {
                auditInfo.put("errorMessage", errorMessage);
            }

            // 记录审计日志
            if ("FAILURE".equals(status)) {
                log.error("【审计日志】操作失败: {}", objectMapper.writeValueAsString(auditInfo));
            } else {
                log.info("【审计日志】{}", objectMapper.writeValueAsString(auditInfo));
            }
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
        // 脱敏密码字段
        content = content.replaceAll("\"password\"\\s*:\\s*\"[^\"]*\"", "\"password\":\"******\"");
        content = content.replaceAll("\"oldPassword\"\\s*:\\s*\"[^\"]*\"", "\"oldPassword\":\"******\"");
        content = content.replaceAll("\"newPassword\"\\s*:\\s*\"[^\"]*\"", "\"newPassword\":\"******\"");
        // 脱敏手机号
        content = content.replaceAll("\"phone\"\\s*:\\s*\"(\\d{3})\\d{4}(\\d{4})\"", "\"phone\":\"$1****$2\"");
        // 脱敏身份证号
        content = content.replaceAll("\"idCard\"\\s*:\\s*\"(\\d{6})\\d{8}(\\d{4})\"", "\"idCard\":\"$1********$2\"");
        return content;
    }
}
