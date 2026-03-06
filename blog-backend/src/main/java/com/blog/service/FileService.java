package com.blog.service;

import com.blog.pojo.entity.FileEntity;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {
    FileEntity uploadFile(MultipartFile file, Long userId);
}
