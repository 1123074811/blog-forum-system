package com.blog.controller;

import com.blog.dto.ApiResponse;
import com.blog.entity.Album;
import com.blog.service.AlbumService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/albums")
@RequiredArgsConstructor
public class AlbumController {

    private final AlbumService albumService;

    @PostMapping
    public ApiResponse<Album> create(@RequestBody Album album, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.success(albumService.create(album, userId));
    }

    @PutMapping("/{id}")
    public ApiResponse<Album> update(@PathVariable Long id, @RequestBody Album album, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.success(albumService.update(id, album, userId));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        albumService.delete(id, userId);
        return ApiResponse.success(null);
    }

    @GetMapping("/{id}")
    public ApiResponse<Album> getById(@PathVariable Long id, Authentication auth) {
        Long userId = auth != null ? (Long) auth.getPrincipal() : null;
        return ApiResponse.success(albumService.getById(id, userId));
    }

    @GetMapping("/my")
    public ApiResponse<List<Album>> getMyAlbums(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.success(albumService.getMyAlbums(userId));
    }

    @GetMapping("/public")
    public ApiResponse<List<Album>> getPublicAlbums(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(albumService.getPublicAlbums(page, size));
    }

    @PostMapping("/{id}/toggle-public")
    public ApiResponse<Void> togglePublic(
            @PathVariable Long id,
            @RequestParam(required = false) Boolean isAnonymous,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        albumService.togglePublic(id, userId, isAnonymous);
        return ApiResponse.success(null);
    }

    @GetMapping("/admin/all")
    public ApiResponse<List<Album>> getAllAlbums() {
        return ApiResponse.success(albumService.getAllAlbums());
    }

    @DeleteMapping("/admin/{id}")
    public ApiResponse<Void> adminDelete(@PathVariable Long id) {
        albumService.adminDelete(id);
        return ApiResponse.success(null);
    }

    @PutMapping("/admin/{id}")
    public ApiResponse<Album> adminUpdate(@PathVariable Long id, @RequestBody Album album) {
        return ApiResponse.success(albumService.adminUpdate(id, album));
    }

    @PostMapping("/admin/batch-delete")
    public ApiResponse<Boolean> batchDelete(@RequestBody Map<String, List<Long>> body) {
        List<Long> ids = body.get("ids");
        if (ids != null && !ids.isEmpty()) {
            albumService.removeByIds(ids);
        }
        return ApiResponse.success(true);
    }
}
