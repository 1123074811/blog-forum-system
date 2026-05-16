package com.blog.util;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ClientIpResolver {

    @Value("${app.security.trusted-proxy-ips:127.0.0.1,0:0:0:0:0:0:0:1,::1}")
    private String trustedProxyIps;

    private Set<String> trustedProxies;

    @PostConstruct
    void init() {
        trustedProxies = Arrays.stream(trustedProxyIps.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());
    }

    public String resolve(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        String remoteAddr = normalize(request.getRemoteAddr());
        if (!isTrustedProxy(remoteAddr)) {
            return remoteAddr;
        }

        String cfIp = firstValidIp(request.getHeader("CF-Connecting-IP"));
        if (cfIp != null) {
            return cfIp;
        }

        String realIp = firstValidIp(request.getHeader("X-Real-IP"));
        if (realIp != null) {
            return realIp;
        }

        String forwardedFor = firstValidIp(request.getHeader("X-Forwarded-For"));
        if (forwardedFor != null) {
            return forwardedFor;
        }

        return remoteAddr;
    }

    private boolean isTrustedProxy(String remoteAddr) {
        if (remoteAddr == null || remoteAddr.isBlank()) {
            return false;
        }
        return trustedProxies.contains(remoteAddr) || isLoopback(remoteAddr);
    }

    private boolean isLoopback(String ip) {
        try {
            return InetAddress.getByName(ip).isLoopbackAddress();
        } catch (Exception ignored) {
            return false;
        }
    }

    private String firstValidIp(String header) {
        if (header == null || header.isBlank()) {
            return null;
        }
        for (String part : header.split(",")) {
            String ip = normalize(part);
            if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
                return ip;
            }
        }
        return null;
    }

    private String normalize(String value) {
        if (value == null) {
            return "unknown";
        }
        String ip = value.trim();
        if (ip.startsWith("[") && ip.contains("]")) {
            ip = ip.substring(1, ip.indexOf(']'));
        }
        int colon = ip.indexOf(':');
        if (colon > -1 && ip.indexOf(':', colon + 1) == -1 && ip.matches(".*:\\d+$")) {
            ip = ip.substring(0, colon);
        }
        return ip.isBlank() ? "unknown" : ip;
    }
}
