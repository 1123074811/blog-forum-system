package com.blog.service.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * 统一的文件存储服务接口
 * 支持多种存储实现（MinIO、阿里云OSS等）
 */
public interface StorageService {
    
    /**
     * 上传文件
     * @param file 文件
     * @return 文件访问URL
     */
    String upload(MultipartFile file) throws Exception;
    
    /**
     * 上传文件到指定文件夹
     * @param file 文件
     * @param folder 文件夹路径
     * @return 文件访问URL
     */
    String upload(MultipartFile file, String folder) throws Exception;
    
    /**
     * 从URL上传文件
     * @param imageUrl 图片URL
     * @param filename 文件名
     * @return 文件访问URL
     */
    String uploadFromUrl(String imageUrl, String filename) throws Exception;
    
    /**
     * 上传字节数组（用于缩略图等内存中生成的文件）
     * @param data        文件字节数组
     * @param objectName  存储路径/文件名
     * @param contentType MIME 类型
     * @return 文件访问 URL
     */
    String uploadBytes(byte[] data, String objectName, String contentType) throws Exception;

    /**
     * 删除文件
     * @param filename 文件名或完整路径
     */
    void delete(String filename) throws Exception;
    
    /**
     * 初始化存储桶/容器
     */
    void init() throws Exception;
}
