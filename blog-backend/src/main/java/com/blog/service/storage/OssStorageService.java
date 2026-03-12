package com.blog.service.storage;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.CannedAccessControlList;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.blog.config.OssConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.UUID;

/**
 * 阿里云OSS存储服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "storage.type", havingValue = "oss")
public class OssStorageService implements StorageService {
    
    private final OSS ossClient;
    private final OssConfig config;

    @Override
    public void init() throws Exception {
        log.info("初始化阿里云OSS存储服务，Bucket: {}", config.getBucket());
        
        // 检查Bucket是否存在
        if (!ossClient.doesBucketExist(config.getBucket())) {
            // 创建Bucket
            ossClient.createBucket(config.getBucket());
            log.info("创建OSS Bucket: {}", config.getBucket());
            
            // 设置Bucket为公共读
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
        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        String objectName = folder.isEmpty() ? filename : folder + "/" + filename;
        
        // 设置文件元数据
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(file.getContentType());
        metadata.setContentLength(file.getSize());
        
        // 上传文件
        PutObjectRequest putObjectRequest = new PutObjectRequest(
            config.getBucket(),
            objectName,
            file.getInputStream(),
            metadata
        );
        
        ossClient.putObject(putObjectRequest);
        
        // 返回文件URL
        String url = getFileUrl(objectName);
        log.debug("文件上传成功: {}", url);
        return url;
    }

    @Override
    public String uploadFromUrl(String imageUrl, String filename) throws Exception {
        URL url = new URL(imageUrl);
        try (InputStream is = url.openStream()) {
            byte[] data = is.readAllBytes();
            
            // 设置文件元数据
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType("image/jpeg");
            metadata.setContentLength(data.length);
            
            // 上传文件
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                config.getBucket(),
                filename,
                new ByteArrayInputStream(data),
                metadata
            );
            
            ossClient.putObject(putObjectRequest);
            
            String fileUrl = getFileUrl(filename);
            log.debug("从URL上传文件成功: {}", fileUrl);
            return fileUrl;
        }
    }

    @Override
    public void delete(String filename) throws Exception {
        // 如果传入的是完整URL，提取文件名
        if (filename.startsWith("http")) {
            // 从URL中提取对象名称
            String bucketDomain = config.getCustomDomain() != null ? 
                config.getCustomDomain() : 
                config.getBucket() + "." + config.getEndpoint();
            
            if (filename.contains(bucketDomain)) {
                filename = filename.substring(filename.indexOf(bucketDomain) + bucketDomain.length() + 1);
            }
        }
        
        ossClient.deleteObject(config.getBucket(), filename);
        log.debug("文件删除成功: {}", filename);
    }
    
    /**
     * 获取文件访问URL
     */
    private String getFileUrl(String objectName) {
        // 如果配置了自定义域名，使用自定义域名
        if (config.getCustomDomain() != null && !config.getCustomDomain().isEmpty()) {
            return "https://" + config.getCustomDomain() + "/" + objectName;
        }
        // 否则使用默认的OSS域名
        return "https://" + config.getBucket() + "." + config.getEndpoint() + "/" + objectName;
    }
}
