package com.blog.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Slf4j
public final class FileValidationUtil {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
        "jpg", "jpeg", "png", "gif", "webp", "bmp", "ico",
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "md", "csv", "json", "xml",
        "mp4", "webm", "mp3", "wav", "ogg",
        "zip"
    );

    private static final Map<String, byte[]> MAGIC_BYTES = Map.of(
        "jpg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
        "jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
        "png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47},
        "gif", new byte[]{0x47, 0x49, 0x46, 0x38},
        "webp", new byte[]{0x52, 0x49, 0x46, 0x46},
        "bmp", new byte[]{0x42, 0x4D},
        "pdf", new byte[]{0x25, 0x50, 0x44, 0x46},
        "zip", new byte[]{0x50, 0x4B, 0x03, 0x04}
    );

    private static final Set<String> IMAGE_EXTENSIONS = Set.of(
        "jpg", "jpeg", "png", "gif", "webp", "bmp", "ico"
    );

    private static final Map<String, String> EXTENSION_TO_MIME = Map.ofEntries(
        Map.entry("jpg", "image/jpeg"),
        Map.entry("jpeg", "image/jpeg"),
        Map.entry("png", "image/png"),
        Map.entry("gif", "image/gif"),
        Map.entry("webp", "image/webp"),
        Map.entry("bmp", "image/bmp"),
        Map.entry("ico", "image/x-icon"),
        Map.entry("pdf", "application/pdf"),
        Map.entry("zip", "application/zip"),
        Map.entry("json", "application/json"),
        Map.entry("xml", "application/xml"),
        Map.entry("mp4", "video/mp4"),
        Map.entry("webm", "video/webm"),
        Map.entry("mp3", "audio/mpeg"),
        Map.entry("wav", "audio/wav"),
        Map.entry("ogg", "audio/ogg")
    );

    private FileValidationUtil() {}

    public static void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        String ext = extractExtension(originalFilename);

        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            log.warn("Upload rejected - disallowed extension: {}", ext);
            throw new IllegalArgumentException("不支持的文件类型");
        }

        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank()) {
            String expectedMime = EXTENSION_TO_MIME.get(ext);
            if (expectedMime != null) {
                String normalizedContentType = contentType.toLowerCase(Locale.ROOT).split(";")[0].trim();
                if (!expectedMime.equals(normalizedContentType) && !isCompatibleMime(ext, normalizedContentType)) {
                    log.warn("Upload rejected - MIME mismatch: ext={}, expected={}, actual={}", ext, expectedMime, normalizedContentType);
                    throw new IllegalArgumentException("文件类型与扩展名不匹配");
                }
            }
        }

        if (MAGIC_BYTES.containsKey(ext)) {
            byte[] expectedMagic = MAGIC_BYTES.get(ext);
            try (InputStream in = file.getInputStream()) {
                byte[] header = new byte[expectedMagic.length];
                int read = in.read(header);
                if (read < expectedMagic.length) {
                    throw new IllegalArgumentException("文件格式校验失败");
                }
                for (int i = 0; i < expectedMagic.length; i++) {
                    if (header[i] != expectedMagic[i]) {
                        log.warn("Upload rejected - magic number mismatch for extension: {}", ext);
                        throw new IllegalArgumentException("文件格式校验失败");
                    }
                }
            } catch (IOException e) {
                log.error("Failed to read file for magic number validation", e);
                throw new IllegalArgumentException("文件读取失败");
            }
        }
    }

    public static void validateImage(MultipartFile file) {
        validate(file);
        String ext = extractExtension(file.getOriginalFilename());
        if (!IMAGE_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("仅支持图片文件上传");
        }
    }

    private static String extractExtension(String filename) {
        if (filename == null || filename.isBlank()) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static boolean isCompatibleMime(String ext, String contentType) {
        // Office documents have various MIME types
        Set<String> officeExts = Set.of("doc", "docx", "xls", "xlsx", "ppt", "pptx");
        if (officeExts.contains(ext) && (contentType.contains("officedocument") || contentType.contains("ms-") || contentType.contains("opendocument"))) {
            return true;
        }
        // Text-based types
        Set<String> textExts = Set.of("txt", "md", "csv", "json", "xml");
        if (textExts.contains(ext) && contentType.startsWith("text/")) {
            return true;
        }
        // JSON
        if ("json".equals(ext) && contentType.contains("json")) {
            return true;
        }
        // XML
        if ("xml".equals(ext) && contentType.contains("xml")) {
            return true;
        }
        return false;
    }
}
