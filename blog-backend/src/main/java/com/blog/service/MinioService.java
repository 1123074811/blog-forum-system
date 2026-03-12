package com.blog.service;

import com.blog.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储服务（兼容层）
 * 保持原有API不变，内部委托给StorageService实现
 * 
 * @deprecated 建议直接使用 StorageService 接口
 */
@Service
@RequiredArgsConstructor
public class MinioService {
    
    private final StorageService storageService;

    public String upload(MultipartFile file) throws Exception {
        return storageService.upload(file);
    }

    public String upload(MultipartFile file, String folder) throws Exception {
        return storageService.upload(file, folder);
    }

    public String uploadFromUrl(String imageUrl, String filename) throws Exception {
        return storageService.uploadFromUrl(imageUrl, filename);
    }

    public void delete(String filename) throws Exception {
        storageService.delete(filename);
    }
}
