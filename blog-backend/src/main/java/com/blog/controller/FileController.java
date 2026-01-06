package com.blog.controller;

import com.blog.dto.ApiResponse;
import com.blog.entity.FileEntity;
import com.blog.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload")
    public ApiResponse<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        FileEntity fileEntity = fileService.uploadFile(file, userId);

        Map<String, Object> result = new HashMap<>();
        result.put("id", fileEntity.getId());
        result.put("url", fileEntity.getFilePath());
        result.put("fileName", fileEntity.getFileName());

        return ApiResponse.success(result);
    }
}
