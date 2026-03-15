package com.blog.controller;

import com.blog.pojo.dto.ApiResponse;
import com.blog.pojo.entity.Media;
import com.blog.service.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    @PostMapping("/upload")
    public ApiResponse<Media> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Long albumId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.success(mediaService.upload(file, albumId, title, description, userId));
    }

    @PutMapping("/{id}")
    public ApiResponse<Media> update(
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.success(mediaService.update(id, title, description, userId));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        mediaService.delete(id, userId);
        return ApiResponse.success(null);
    }

    @GetMapping("/{id}")
    public ApiResponse<Media> getById(@PathVariable Long id, Authentication auth) {
        Long userId = auth != null ? (Long) auth.getPrincipal() : null;
        return ApiResponse.success(mediaService.getById(id, userId));
    }

    @GetMapping("/album/{albumId}")
    public ApiResponse<List<Media>> getByAlbumId(@PathVariable Long albumId, Authentication auth) {
        Long userId = auth != null ? (Long) auth.getPrincipal() : null;
        return ApiResponse.success(mediaService.getByAlbumId(albumId, userId));
    }

    @GetMapping("/my")
    public ApiResponse<List<Media>> getMyMedia(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.success(mediaService.getMyMedia(userId, type, page, size));
    }

    @GetMapping("/public")
    public ApiResponse<List<Media>> getPublicMedia(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(mediaService.getPublicMedia(type, page, size));
    }

    @PostMapping("/{id}/toggle-public")
    public ApiResponse<Void> togglePublic(
            @PathVariable Long id,
            @RequestParam(required = false) Boolean isAnonymous,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        mediaService.togglePublic(id, userId, isAnonymous);
        return ApiResponse.success(null);
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<Media>> getAllMedia() {
        return ApiResponse.success(mediaService.getAllMedia());
    }

    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> adminDelete(@PathVariable Long id) {
        mediaService.adminDelete(id);
        return ApiResponse.success(null);
    }

    @PutMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Media> adminUpdate(@PathVariable Long id, @RequestBody Media media) {
        return ApiResponse.success(mediaService.adminUpdate(id, media.getTitle(), media.getDescription(), media.getIsPublic()));
    }

    @PostMapping("/admin/batch-delete")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Boolean> batchDelete(@RequestBody Map<String, List<Long>> body) {
        List<Long> ids = body.get("ids");
        if (ids != null && !ids.isEmpty()) {
            mediaService.removeByIds(ids);
        }
        return ApiResponse.success(true);
    }
}
