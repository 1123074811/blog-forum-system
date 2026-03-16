package com.blog.filter;

import com.blog.pojo.dto.ApiResponse;
import com.blog.service.SecurityEventService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 超高频访问分级限制 Filter
 *
 * 针对敏感路径（管理后台、认证、注册等）做滑动窗口计数，
 * 维护每个 IP 的"威胁标志数"（threat level）：
 *   level 1 → 触发限流，返回 429，标志数置为 1，生命周期 10 分钟
 *   level 2 → 生命周期内再次超高频，返回 429 + 更严厉提示，标志数升为 2
 *   level 3 → 再次超高频，直接封禁 IP 拉入黑名单（24 小时）
 *
 * 标志数生命周期结束且未升级则自动重置（Redis TTL 自然过期）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HotspotRateLimitFilter extends OncePerRequestFilter {

    private final SecurityEventService securityEventService;
    private final ObjectMapper objectMapper;

    // ── 敏感路径规则 ──────────────────────────────────────────────
    // 每条规则：路径前缀 + 滑动窗口秒数 + 窗口内最大请求数
    private static final List<PathRule> SENSITIVE_RULES = List.of(
        new PathRule("/api/admin",        10,  30),   // 管理后台：10秒内超过30次
        new PathRule("/api/auth",         10,  40),   // 认证接口：10秒内超过40次
        new PathRule("/api/users",        10,  60),   // 用户接口：10秒内超过60次
        new PathRule("/api/comments",     10,  60),   // 评论接口：10秒内超过60次
        new PathRule("/api/articles",     10,  80),   // 文章接口：10秒内超过80次
        new PathRule("/api",              30, 300)    // 全局兜底：30秒内超过300次
    );

    // 标志数生命周期（秒）
    private static final long THREAT_LEVEL_TTL = 600; // 10 分钟

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // OPTIONS 预检直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = extractIp(request);
        String path = request.getRequestURI();

        PathRule matchedRule = matchRule(path);
        if (matchedRule == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // 滑动窗口计数
        String countKey = "hotspot:count:" + matchedRule.prefix.replace("/", "_") + ":" + ip;
        long reqCount = securityEventService.incrementWithTtl(countKey, matchedRule.windowSeconds);

        if (reqCount <= matchedRule.maxRequests) {
            // 未超限，正常放行
            filterChain.doFilter(request, response);
            return;
        }

        // ── 超高频，读取当前威胁标志数 ──────────────────────────
        int level = securityEventService.getThreatLevel(ip);

        if (level == 0) {
            // 第一次触发：标志数置为 1，限流返回 429
            securityEventService.setThreatLevel(ip, 1, THREAT_LEVEL_TTL);
            log.warn("[HOTSPOT] level=1 ip={} path={} count={}", ip, path, reqCount);
            writeJson(response, HttpStatus.TOO_MANY_REQUESTS,
                    ApiResponse.error(429, "请求过于频繁，请稍后再试"));

        } else if (level == 1) {
            // 第二次触发：标志数升为 2，返回更严厉提示
            securityEventService.setThreatLevel(ip, 2, THREAT_LEVEL_TTL);
            log.warn("[HOTSPOT] level=2 ip={} path={} count={}", ip, path, reqCount);
            writeJson(response, HttpStatus.TOO_MANY_REQUESTS,
                    ApiResponse.error(429, "检测到异常高频访问，您的IP已被标记，请立即停止，否则将被封禁"));

        } else {
            // 第三次及以上：直接封禁 24 小时
            securityEventService.banIp(ip, "hotspot_ddos", java.time.Duration.ofHours(24));
            securityEventService.setThreatLevel(ip, 3, THREAT_LEVEL_TTL);
            log.warn("[HOTSPOT] level=3 BANNED ip={} path={} count={}", ip, path, reqCount);
            writeJson(response, HttpStatus.FORBIDDEN,
                    ApiResponse.error(403, "IP已因超高频访问被封禁，如有疑问请联系管理员"));
        }
    }

    private PathRule matchRule(String path) {
        // 按规则顺序匹配，越具体的规则排在前面
        for (PathRule rule : SENSITIVE_RULES) {
            if (path.startsWith(rule.prefix)) {
                return rule;
            }
        }
        return null;
    }

    private void writeJson(HttpServletResponse response, HttpStatus status, Object body) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    private String extractIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private record PathRule(String prefix, long windowSeconds, long maxRequests) {}
}
