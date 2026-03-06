package com.blog.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 自定义健康检查配置
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class HealthCheckConfig {

    private final JdbcTemplate jdbcTemplate;
    private final RedisConnectionFactory redisConnectionFactory;

    /**
     * 数据库健康检查
     */
    @Bean
    public HealthIndicator dbHealthIndicator() {
        return () -> {
            try {
                jdbcTemplate.queryForObject("SELECT 1", Integer.class);
                return Health.up()
                        .withDetail("database", "MySQL")
                        .withDetail("status", "可用")
                        .build();
            } catch (Exception e) {
                log.error("数据库健康检查失败", e);
                return Health.down()
                        .withDetail("database", "MySQL")
                        .withDetail("status", "不可用")
                        .withDetail("error", e.getMessage())
                        .build();
            }
        };
    }

    /**
     * Redis 健康检查
     */
    @Bean
    public HealthIndicator redisHealthIndicator() {
        return () -> {
            try {
                redisConnectionFactory.getConnection().ping();
                return Health.up()
                        .withDetail("cache", "Redis")
                        .withDetail("status", "可用")
                        .build();
            } catch (Exception e) {
                log.warn("Redis 健康检查失败，服务降级", e);
                // Redis 不可用时返回 UP 状态，因为有降级机制
                return Health.up()
                        .withDetail("cache", "Redis")
                        .withDetail("status", "降级运行")
                        .withDetail("message", "Redis 不可用，已启用降级机制")
                        .build();
            }
        };
    }

    /**
     * 磁盘空间健康检查
     */
    @Bean
    public HealthIndicator diskSpaceHealthIndicator() {
        return () -> {
            try {
                java.io.File file = new java.io.File(".");
                long freeSpace = file.getFreeSpace();
                long totalSpace = file.getTotalSpace();
                long usedSpace = totalSpace - freeSpace;
                double usagePercent = (double) usedSpace / totalSpace * 100;

                if (usagePercent > 90) {
                    return Health.down()
                            .withDetail("diskSpace", "磁盘空间不足")
                            .withDetail("total", formatBytes(totalSpace))
                            .withDetail("free", formatBytes(freeSpace))
                            .withDetail("used", formatBytes(usedSpace))
                            .withDetail("usagePercent", String.format("%.2f%%", usagePercent))
                            .build();
                } else if (usagePercent > 80) {
                    return Health.up()
                            .withDetail("diskSpace", "磁盘空间充足")
                            .withDetail("total", formatBytes(totalSpace))
                            .withDetail("free", formatBytes(freeSpace))
                            .withDetail("warning", "磁盘使用率超过80%")
                            .build();
                } else {
                    return Health.up()
                            .withDetail("diskSpace", "磁盘空间充足")
                            .withDetail("total", formatBytes(totalSpace))
                            .withDetail("free", formatBytes(freeSpace))
                            .withDetail("usagePercent", String.format("%.2f%%", usagePercent))
                            .build();
                }
            } catch (Exception e) {
                log.error("磁盘空间检查失败", e);
                return Health.unknown()
                        .withDetail("error", e.getMessage())
                        .build();
            }
        };
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
    }
}
