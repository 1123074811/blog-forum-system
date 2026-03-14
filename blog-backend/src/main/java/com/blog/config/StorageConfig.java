package com.blog.config;

import com.blog.service.storage.StorageService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * 存储服务配置
 * 负责初始化选定的存储服务
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class StorageConfig {
    
    private final StorageService storageService;
    
    @Value("${storage.type:minio}")
    private String storageType;
    
    @Value("${storage.init-fail-fast:true}")
    private boolean initFailFast;

    @PostConstruct
    public void init() {
        if (!initFailFast) {
            try {
                log.info("Storage type: {}", storageType);
                storageService.init();
            } catch (Exception e) {
                log.warn("Storage init failed, continue startup because storage.init-fail-fast=false", e);
            }
            return;
        }
        try {
            log.info("当前使用的存储类型: {}", storageType);
            storageService.init();
        } catch (Exception e) {
            log.error("存储服务初始化失败", e);
            throw new RuntimeException("存储服务初始化失败", e);
        }
    }
}
