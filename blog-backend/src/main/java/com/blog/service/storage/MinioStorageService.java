package com.blog.service.storage;

import com.blog.config.MinioConfig;
import com.blog.util.UrlSecurityUtil;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.SetBucketPolicyArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "storage.type", havingValue = "minio", matchIfMissing = true)
public class MinioStorageService implements StorageService {

    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 10000;
    private static final int MAX_DOWNLOAD_BYTES = 10 * 1024 * 1024;

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
        String contentType = file.getContentType() == null ? "application/octet-stream" : file.getContentType();

        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(config.getBucket())
                        .object(objectName)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(contentType)
                        .build()
        );

        String url = config.getEndpoint() + "/" + config.getBucket() + "/" + objectName;
        log.debug("文件上传成功: {}", url);
        return url;
    }

    @Override
    public String uploadFromUrl(String imageUrl, String filename) throws Exception {
        UrlSecurityUtil.validatePublicHttpUrl(imageUrl);

        URLConnection connection = new URL(imageUrl).openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setRequestProperty("User-Agent", "BlogForumBot/1.0");

        String contentType = normalizeContentType(connection.getContentType(), filename);
        try (InputStream inputStream = connection.getInputStream()) {
            byte[] data = readAllBytesLimited(inputStream);
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(config.getBucket())
                            .object(filename)
                            .stream(new ByteArrayInputStream(data), data.length, -1)
                            .contentType(contentType)
                            .build()
            );

            String fileUrl = config.getEndpoint() + "/" + config.getBucket() + "/" + filename;
            log.debug("URL上传文件成功: {}", fileUrl);
            return fileUrl;
        }
    }

    @Override
    public void delete(String filename) throws Exception {
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

    private String normalizeContentType(String contentType, String filename) {
        if (contentType != null && !contentType.isBlank()) {
            String normalized = contentType.toLowerCase(Locale.ROOT);
            if (!normalized.startsWith("image/")) {
                throw new IllegalArgumentException("Only image content is allowed");
            }
            return normalized;
        }

        String lower = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".gif")) {
            return "image/gif";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        return "image/jpeg";
    }

    private byte[] readAllBytesLimited(InputStream inputStream) throws Exception {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int total = 0;
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                total += read;
                if (total > MAX_DOWNLOAD_BYTES) {
                    throw new IllegalArgumentException("Remote file is too large");
                }
                outputStream.write(buffer, 0, read);
            }
            return outputStream.toByteArray();
        }
    }
}
