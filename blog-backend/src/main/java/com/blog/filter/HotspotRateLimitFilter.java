package com.blog.filter;

import com.blog.pojo.dto.ApiResponse;
import com.blog.service.SecurityEventService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class HotspotRateLimitFilter extends OncePerRequestFilter {

    private final SecurityEventService securityEventService;
    private final ObjectMapper objectMapper;

    @Value("${app.security.hotspot-whitelist-enabled:false}")
    private boolean hotspotWhitelistEnabled;

    @Value("${app.security.hotspot-whitelist-ips:}")
    private String hotspotWhitelistIps;

    private Set<String> whitelistIps = Collections.emptySet();

    private static final List<PathRule> SENSITIVE_RULES = List.of(
            new PathRule("/api/admin", 10, 30),
            new PathRule("/api/auth", 10, 40),
            new PathRule("/api/users", 10, 60),
            new PathRule("/api/comments", 10, 60),
            new PathRule("/api/articles", 10, 80),
            new PathRule("/api", 30, 300)
    );

    private static final long THREAT_LEVEL_TTL = 600;

    @PostConstruct
    void initWhitelist() {
        if (hotspotWhitelistIps == null || hotspotWhitelistIps.isBlank()) {
            whitelistIps = Collections.emptySet();
            return;
        }
        whitelistIps = new HashSet<>(Arrays.stream(hotspotWhitelistIps.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList());
        log.info("Hotspot whitelist enabled={}, ips={}", hotspotWhitelistEnabled, whitelistIps);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = extractIp(request);
        if (hotspotWhitelistEnabled && whitelistIps.contains(ip)) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        PathRule matchedRule = matchRule(path);
        if (matchedRule == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String countKey = "hotspot:count:" + matchedRule.prefix.replace("/", "_") + ":" + ip;
        long reqCount = securityEventService.incrementWithTtl(countKey, matchedRule.windowSeconds);

        if (reqCount <= matchedRule.maxRequests) {
            filterChain.doFilter(request, response);
            return;
        }

        int level = securityEventService.getThreatLevel(ip);

        if (level == 0) {
            securityEventService.setThreatLevel(ip, 1, THREAT_LEVEL_TTL);
            log.warn("[HOTSPOT] level=1 ip={} path={} count={}", ip, path, reqCount);
            writeJson(response, HttpStatus.TOO_MANY_REQUESTS,
                    ApiResponse.error(429, "请求过于频繁，请稍后再试"));
        } else if (level == 1) {
            securityEventService.setThreatLevel(ip, 2, THREAT_LEVEL_TTL);
            log.warn("[HOTSPOT] level=2 ip={} path={} count={}", ip, path, reqCount);
            writeJson(response, HttpStatus.TOO_MANY_REQUESTS,
                    ApiResponse.error(429, "检测到异常高频访问，您的IP已被标记，请立即停止，否则将被封禁"));
        } else {
            securityEventService.banIp(ip, "hotspot_ddos", java.time.Duration.ofHours(24));
            securityEventService.setThreatLevel(ip, 3, THREAT_LEVEL_TTL);
            log.warn("[HOTSPOT] level=3 BANNED ip={} path={} count={}", ip, path, reqCount);
            writeJson(response, HttpStatus.FORBIDDEN,
                    ApiResponse.error(403, "IP已因超高频访问被封禁，如有疑问请联系管理员"));
        }
    }

    private PathRule matchRule(String path) {
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

    private record PathRule(String prefix, long windowSeconds, long maxRequests) {
    }
}
