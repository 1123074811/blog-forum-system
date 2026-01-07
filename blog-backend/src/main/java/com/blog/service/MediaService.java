package com.blog.service;

import com.blog.entity.Media;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface MediaService {
    Media upload(MultipartFile file, Long albumId, String title, String description, Long userId);
    Media update(Long id, String title, String description, Long userId);
    void delete(Long id, Long userId);
    Media getById(Long id, Long userId);
    List<Media> getByAlbumId(Long albumId, Long userId);
    List<Media> getMyMedia(Long userId, String type, int page, int size);
    List<Media> getPublicMedia(String type, int page, int size);
    void togglePublic(Long id, Long userId, Boolean isAnonymous);
    List<Media> getAllMedia();
    void adminDelete(Long id);
    Media adminUpdate(Long id, String title, String description, Boolean isPublic);
    void removeByIds(List<Long> ids);
}
