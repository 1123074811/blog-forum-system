package com.blog.service.storage;

import com.blog.config.MinioConfig;
import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * MinIO存储服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "storage.type", havingValue = "minio", matchIfMissing = true)
public class MinioStorageService implements StorageService {
    
    private final MinioClient minioClient;
    private final MinioConfig config;

    @Override
    public void init() throws Exception {
        log.info("初始化MinIO存储服务，Bucket: {}", config.getBucket());
        boolean exists = minioClient.bucketExists(
            BucketExistsArgs.builder()
                .bucket(config.getBucket())
                .build()
        );
        
        if (!exists) {
            minioClient.makeBucket(
                MakeBucketArgs.builder()
                    .bucket(config.getBucket())
                    .build()
            );
            log.info("创建MinIO Bucket: {}", config.getBucket());
        }
        
        // 设置bucket为公开读取
        String policy = """
            {
                "Version": "2012-10-17",
                "Statement": [{
                    "Effect": "Allow",
                    "Principal": {"AWS": ["*"]},
                    "Action": ["s3:GetObject"],
                    "Resource": ["arn:aws:s3:::%s/*"]
                }]
            }
            """.formatted(config.getBucket());
            
        minioClient.setBucketPolicy(
            SetBucketPolicyArgs.builder()
                .bucket(config.getBucket())
                .config(policy)
                .build()
        );
        log.info("MinIO存储服务初始化完成");
    }

    @Override
    public String upload(MultipartFile file) throws Exception {
        return upload(file, "");
    }

    @Override
    public String upload(MultipartFile file, String folder) throws Exception {
        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        String objectName = folder.isEmpty() ? filename : folder + "/" + filename;
        
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(config.getBucket())
                .object(objectName)
                .stream(file.getInputStream(), file.getSize(), -1)
                .contentType(file.getContentType())
                .build()
        );
        
        String url = config.getEndpoint() + "/" + config.getBucket() + "/" + objectName;
        log.debug("文件上传成功: {}", url);
        return url;
    }

    @Override
    public String uploadFromUrl(String imageUrl, String filename) throws Exception {
        java.net.URL url = new java.net.URL(imageUrl);
        try (java.io.InputStream is = url.openStream()) {
            byte[] data = is.readAllBytes();
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(config.getBucket())
                    .object(filename)
                    .stream(new java.io.ByteArrayInputStream(data), data.length, -1)
                    .contentType("image/jpeg")
                    .build()
            );
            
            String fileUrl = config.getEndpoint() + "/" + config.getBucket() + "/" + filename;
            log.debug("从URL上传文件成功: {}", fileUrl);
            return fileUrl;
        }
    }

    @Override
    public void delete(String filename) throws Exception {
        // 如果传入的是完整URL，提取文件名
        if (filename.startsWith("http")) {
            filename = filename.substring(filename.lastIndexOf("/") + 1);
        }
        
        minioClient.removeObject(
            RemoveObjectArgs.builder()
                .bucket(config.getBucket())
                .object(filename)
                .build()
        );
        log.debug("文件删除成功: {}", filename);
    }
}
