package com.blog.controller;

import com.blog.pojo.dto.ApiResponse;
import com.blog.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 存储服务测试控制器
 * 用于测试和验证存储功能
 * 
 * 注意：生产环境建议删除或禁用此控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/storage/test")
@RequiredArgsConstructor
public class StorageTestController {
    
    private final StorageService storageService;
    
    @Value("${storage.type:minio}")
    private String storageType;

    /**
     * 获取当前存储配置信息
     */
    @GetMapping("/info")
    public ApiResponse<Map<String, String>> getStorageInfo() {
        Map<String, String> info = new HashMap<>();
        info.put("storageType", storageType);
        info.put("implementation", storageService.getClass().getSimpleName());
        
        log.info("当前存储类型: {}, 实现类: {}", storageType, storageService.getClass().getSimpleName());
        return ApiResponse.success(info);
    }

    /**
     * 测试文件上传
     */
    @PostMapping("/upload")
    public ApiResponse<Map<String, String>> testUpload(@RequestParam("file") MultipartFile file) {
        try {
            log.info("测试上传文件: {}, 大小: {} bytes", file.getOriginalFilename(), file.getSize());
            
            String url = storageService.upload(file, "test");
            
            Map<String, String> result = new HashMap<>();
            result.put("url", url);
            result.put("filename", file.getOriginalFilename());
            result.put("size", String.valueOf(file.getSize()));
            result.put("storageType", storageType);
            
            log.info("文件上传成功: {}", url);
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("文件上传失败", e);
            return ApiResponse.error("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 测试从URL上传
     */
    @PostMapping("/upload-from-url")
    public ApiResponse<Map<String, String>> testUploadFromUrl(
            @RequestParam("url") String imageUrl,
            @RequestParam("filename") String filename) {
        try {
            log.info("测试从URL上传: {}", imageUrl);
            
            String url = storageService.uploadFromUrl(imageUrl, "test/" + filename);
            
            Map<String, String> result = new HashMap<>();
            result.put("url", url);
            result.put("sourceUrl", imageUrl);
            result.put("filename", filename);
            result.put("storageType", storageType);
            
            log.info("从URL上传成功: {}", url);
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("从URL上传失败", e);
            return ApiResponse.error("从URL上传失败: " + e.getMessage());
        }
    }

    /**
     * 测试文件删除
     */
    @DeleteMapping("/delete")
    public ApiResponse<String> testDelete(@RequestParam("filename") String filename) {
        try {
            log.info("测试删除文件: {}", filename);
            
            storageService.delete(filename);
            
            log.info("文件删除成功: {}", filename);
            return ApiResponse.success("文件删除成功");
        } catch (Exception e) {
            log.error("文件删除失败", e);
            return ApiResponse.error("文件删除失败: " + e.getMessage());
        }
    }
}
