package com.blog.service;

import com.blog.entity.Album;
import java.util.List;

public interface AlbumService {
    Album create(Album album, Long userId);
    Album update(Long id, Album album, Long userId);
    void delete(Long id, Long userId);
    Album getById(Long id, Long userId);
    List<Album> getMyAlbums(Long userId);
    List<Album> getPublicAlbums(int page, int size);
    void togglePublic(Long id, Long userId);
    List<Album> getAllAlbums();
    void adminDelete(Long id);
    Album adminUpdate(Long id, Album album);
}
