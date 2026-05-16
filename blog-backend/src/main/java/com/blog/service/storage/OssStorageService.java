package com.blog.service.storage;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.CannedAccessControlList;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.blog.config.OssConfig;
import com.blog.util.FileValidationUtil;
import com.blog.util.UrlSecurityUtil;
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
@ConditionalOnProperty(name = "storage.type", havingValue = "oss")
public class OssStorageService implements StorageService {

    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 10000;
    private static final int MAX_DOWNLOAD_BYTES = 10 * 1024 * 1024;

    private final OSS ossClient;
    private final OssConfig config;

    @Override
    public void init() {
        log.info("初始化阿里云OSS存储服务，Bucket: {}", config.getBucket());

        if (!ossClient.doesBucketExist(config.getBucket())) {
            ossClient.createBucket(config.getBucket());
            log.info("创建OSS Bucket: {}", config.getBucket());
            ossClient.setBucketAcl(config.getBucket(), CannedAccessControlList.PublicRead);
            log.info("设置Bucket为公共读");
        }

        log.info("阿里云OSS存储服务初始化完成");
    }

    @Override
    public String upload(MultipartFile file) throws Exception {
        return upload(file, "");
    }

    @Override
    public String upload(MultipartFile file, String folder) throws Exception {
        FileValidationUtil.validate(file);
        String filename = UUID.randomUUID() + safeExtension(file.getOriginalFilename());
        String objectName = folder.isEmpty() ? filename : folder + "/" + filename;

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(file.getContentType());
        metadata.setContentLength(file.getSize());

        PutObjectRequest putObjectRequest = new PutObjectRequest(
                config.getBucket(),
                objectName,
                file.getInputStream(),
                metadata
        );

        ossClient.putObject(putObjectRequest);

        String url = getFileUrl(objectName);
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
        byte[] data;
        try (InputStream inputStream = connection.getInputStream()) {
            data = readAllBytesLimited(inputStream);
        }

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(contentType);
        metadata.setContentLength(data.length);

        PutObjectRequest putObjectRequest = new PutObjectRequest(
                config.getBucket(),
                filename,
                new ByteArrayInputStream(data),
                metadata
        );

        ossClient.putObject(putObjectRequest);

        String fileUrl = getFileUrl(filename);
        log.debug("URL上传文件成功: {}", fileUrl);
        return fileUrl;
    }

    @Override
    public String uploadBytes(byte[] data, String objectName, String contentType) throws Exception {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(contentType);
        metadata.setContentLength(data.length);

        PutObjectRequest putObjectRequest = new PutObjectRequest(
                config.getBucket(),
                objectName,
                new ByteArrayInputStream(data),
                metadata
        );

        ossClient.putObject(putObjectRequest);

        String fileUrl = getFileUrl(objectName);
        log.debug("字节数组上传成功: {}", fileUrl);
        return fileUrl;
    }

    @Override
    public void delete(String filename) {
        if (filename.startsWith("http")) {
            String bucketDomain = config.getCustomDomain() != null
                    ? config.getCustomDomain()
                    : config.getBucket() + "." + config.getEndpoint();

            if (filename.contains(bucketDomain)) {
                filename = filename.substring(filename.indexOf(bucketDomain) + bucketDomain.length() + 1);
            }
        }

        ossClient.deleteObject(config.getBucket(), filename);
        log.debug("文件删除成功: {}", filename);
    }

    private String getFileUrl(String objectName) {
        if (config.getCustomDomain() != null && !config.getCustomDomain().isEmpty()) {
            return "https://" + config.getCustomDomain() + "/" + objectName;
        }
        return "https://" + config.getBucket() + "." + config.getEndpoint() + "/" + objectName;
    }

    private String safeExtension(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return ".bin";
        }
        int dot = originalFilename.lastIndexOf('.');
        if (dot < 0 || dot == originalFilename.length() - 1) {
            return ".bin";
        }
        String ext = originalFilename.substring(dot).toLowerCase(Locale.ROOT);
        String sanitized = ext.replaceAll("[^a-z0-9.]", "");
        if (sanitized.length() < 2 || sanitized.length() > 10 || !sanitized.startsWith(".")) {
            return ".bin";
        }
        return sanitized;
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
