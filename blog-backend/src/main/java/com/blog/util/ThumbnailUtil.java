package com.blog.util;

import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 图片缩略图工具类
 */
public class ThumbnailUtil {

    /** 缩略图最大边长（px） */
    private static final int THUMB_SIZE = 400;
    /** 缩略图 JPEG 质量 0~1 */
    private static final double THUMB_QUALITY = 0.8;

    private ThumbnailUtil() {}

    /**
     * 生成缩略图字节数组（JPEG 格式）
     * 仅对图片类型处理，视频直接返回 null
     *
     * @param file 上传的文件
     * @return 缩略图字节数组，非图片返回 null
     */
    public static byte[] generate(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return null;
        }
        // GIF 不处理（避免丢失动画）
        if (contentType.contains("gif")) {
            return null;
        }
        try (InputStream in = file.getInputStream();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Thumbnails.of(in)
                    .size(THUMB_SIZE, THUMB_SIZE)
                    .crop(Positions.CENTER)
                    .outputFormat("jpg")
                    .outputQuality(THUMB_QUALITY)
                    .toOutputStream(out);
            return out.toByteArray();
        } catch (IOException e) {
            // 缩略图生成失败不影响主流程
            return null;
        }
    }

    /**
     * 根据原始 objectName 生成缩略图的 objectName
     * 例如: images/abc.jpg → thumbnails/abc_thumb.jpg
     */
    public static String thumbObjectName(String originalObjectName) {
        int slash = originalObjectName.lastIndexOf('/');
        String dir = slash >= 0 ? originalObjectName.substring(0, slash + 1) : "";
        String name = slash >= 0 ? originalObjectName.substring(slash + 1) : originalObjectName;
        int dot = name.lastIndexOf('.');
        String base = dot >= 0 ? name.substring(0, dot) : name;
        return "thumbnails/" + dir + base + "_thumb.jpg";
    }
}
