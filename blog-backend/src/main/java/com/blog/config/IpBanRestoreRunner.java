package com.blog.config;

import com.blog.service.SecurityEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class IpBanRestoreRunner implements ApplicationRunner {

    private final SecurityEventService securityEventService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            securityEventService.restoreEffectiveBansToRedis();
        } catch (Exception e) {
            log.error("[SECURITY] Failed to restore IP bans to Redis", e);
        }
    }
}
