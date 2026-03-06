package com.blog.service.impl;

import com.blog.pojo.entity.FileEntity;
import com.blog.mapper.FileMapper;
import com.blog.service.FileService;
import com.blog.service.MinioService;
import com.blog.util.DateUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final FileMapper fileMapper;
    private final MinioService minioService;

    @Override
    public FileEntity uploadFile(MultipartFile file, Long userId) {
        try {
            String url = minioService.upload(file);

            FileEntity fileEntity = new FileEntity();
            fileEntity.setUserId(userId);
            fileEntity.setFileName(file.getOriginalFilename());
            fileEntity.setFilePath(url);
            fileEntity.setFileSize(file.getSize());
            fileEntity.setFileType(file.getContentType());
            fileEntity.setCreatedAt(DateUtil.now());
            fileMapper.insert(fileEntity);

            return fileEntity;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file", e);
        }
    }
}
