package com.blog.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.lionsoul.ip2region.xdb.Searcher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
@Slf4j
public class IpLocationService {

    @Value("${app.ip2region.db-path:./data/ip2region.xdb}")
    private String dbPath;

    @Value("${app.ip2region.db-url:https://raw.githubusercontent.com/lionsoul2014/ip2region/master/data/ip2region.xdb}")
    private String dbUrl;

    private Searcher searcher;

    @PostConstruct
    public void init() {
        try {
            Path path = Paths.get(dbPath);
            if (!Files.exists(path)) {
                Files.createDirectories(path.getParent());
                log.info("[ip2region] Downloading xdb from {} ...", dbUrl);
                try (InputStream in = new URL(dbUrl).openStream()) {
                    Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
                }
                log.info("[ip2region] Downloaded to {}", path.toAbsolutePath());
            }
            searcher = Searcher.newWithFileOnly(path.toString());
            log.info("[ip2region] Initialized with {}", path.toAbsolutePath());
        } catch (Exception e) {
            log.error("[ip2region] Init failed, IP location will be unavailable", e);
        }
    }

    public String resolve(String ip) {
        if (ip == null || ip.isBlank() || "unknown".equals(ip)) {
            return "未知";
        }
        if (isInternalIp(ip)) {
            return "内网";
        }
        if (searcher == null) {
            return "未知";
        }
        try {
            String region = searcher.search(ip);
            return formatRegion(region);
        } catch (Exception e) {
            log.debug("[ip2region] Lookup failed for ip={}", ip, e);
            return "未知";
        }
    }

    public boolean isInternalIp(String ip) {
        try {
            InetAddress addr = InetAddress.getByName(ip);
            if (addr.isLoopbackAddress() || addr.isLinkLocalAddress() || addr.isSiteLocalAddress()) {
                return true;
            }
        } catch (Exception ignored) {
        }

        if (ip.startsWith("10.") || ip.startsWith("192.168.") || ip.startsWith("172.")) {
            return true;
        }
        if (ip.startsWith("127.")) {
            return true;
        }
        if (ip.equals("0:0:0:0:0:0:0:1") || ip.equals("::1")) {
            return true;
        }
        return false;
    }

    private String formatRegion(String raw) {
        if (raw == null || raw.isBlank()) {
            return "未知";
        }
        String[] parts = raw.split("\\|");
        if (parts.length < 4) {
            return raw;
        }
        String country = clean(parts[0]);
        String province = clean(parts[2]);
        String city = clean(parts[3]);
        StringBuilder sb = new StringBuilder();
        if (!country.isEmpty() && !"0".equals(country)) {
            sb.append(country);
        }
        if (!province.isEmpty() && !"0".equals(province) && !province.equals(country)) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(province);
        }
        if (!city.isEmpty() && !"0".equals(city) && !city.equals(province)) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(city);
        }
        return sb.length() > 0 ? sb.toString() : "未知";
    }

    private String clean(String s) {
        return s == null ? "" : s.trim();
    }
}
