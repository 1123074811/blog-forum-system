package com.blog.service;

import com.blog.config.MinioConfig;
import io.minio.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MinioService {
    private final MinioClient minioClient;
    private final MinioConfig config;

    @PostConstruct
    public void init() throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(config.getBucket()).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(config.getBucket()).build());
        }
        // 设置 bucket 为公开读取
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
        minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
            .bucket(config.getBucket())
            .config(policy)
            .build());
    }

    public String upload(MultipartFile file) throws Exception {
        return upload(file, "");
    }

    public String upload(MultipartFile file, String folder) throws Exception {
        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        String objectName = folder.isEmpty() ? filename : folder + "/" + filename;
        minioClient.putObject(PutObjectArgs.builder()
            .bucket(config.getBucket())
            .object(objectName)
            .stream(file.getInputStream(), file.getSize(), -1)
            .contentType(file.getContentType())
            .build());
        return config.getEndpoint() + "/" + config.getBucket() + "/" + objectName;
    }

    public String uploadFromUrl(String imageUrl, String filename) throws Exception {
        java.net.URL url = new java.net.URL(imageUrl);
        try (java.io.InputStream is = url.openStream()) {
            byte[] data = is.readAllBytes();
            minioClient.putObject(PutObjectArgs.builder()
                .bucket(config.getBucket())
                .object(filename)
                .stream(new java.io.ByteArrayInputStream(data), data.length, -1)
                .contentType("image/jpeg")
                .build());
            return config.getEndpoint() + "/" + config.getBucket() + "/" + filename;
        }
    }

    public void delete(String filename) throws Exception {
        minioClient.removeObject(RemoveObjectArgs.builder()
            .bucket(config.getBucket())
            .object(filename)
            .build());
    }
}
