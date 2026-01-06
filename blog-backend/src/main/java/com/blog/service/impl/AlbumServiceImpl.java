package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.entity.Album;
import com.blog.entity.Media;
import com.blog.mapper.AlbumMapper;
import com.blog.mapper.MediaMapper;
import com.blog.service.AlbumService;
import com.blog.util.DateUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AlbumServiceImpl implements AlbumService {

    private final AlbumMapper albumMapper;
    private final MediaMapper mediaMapper;

    private void fillCoverUrls(List<Album> albums) {
        for (Album album : albums) {
            List<Media> mediaList = mediaMapper.selectList(new LambdaQueryWrapper<Media>()
                    .eq(Media::getAlbumId, album.getId())
                    .orderByDesc(Media::getCreatedAt)
                    .last("LIMIT 3"));
            album.setCoverUrls(mediaList.stream().map(Media::getUrl).collect(Collectors.toList()));
            if (album.getCoverUrl() == null && !mediaList.isEmpty()) {
                album.setCoverUrl(mediaList.get(0).getUrl());
            }
        }
    }

    @Override
    public Album create(Album album, Long userId) {
        album.setUserId(userId);
        album.setIsPublic(false);
        album.setMediaCount(0);
        album.setCreatedAt(DateUtil.now());
        album.setUpdatedAt(DateUtil.now());
        albumMapper.insert(album);
        return album;
    }

    @Override
    public Album update(Long id, Album album, Long userId) {
        Album existing = getById(id, userId);
        if (existing == null || !existing.getUserId().equals(userId)) {
            throw new RuntimeException("Album not found or access denied");
        }
        existing.setTitle(album.getTitle());
        existing.setDescription(album.getDescription());
        existing.setCoverUrl(album.getCoverUrl());
        existing.setUpdatedAt(DateUtil.now());
        albumMapper.updateById(existing);
        return existing;
    }

    @Override
    public void delete(Long id, Long userId) {
        Album album = getById(id, userId);
        if (album == null || !album.getUserId().equals(userId)) {
            throw new RuntimeException("Album not found or access denied");
        }
        albumMapper.deleteById(id);
    }

    @Override
    public Album getById(Long id, Long userId) {
        Album album = albumMapper.selectById(id);
        if (album == null) return null;
        if (album.getIsPublic() || album.getUserId().equals(userId)) {
            return album;
        }
        return null;
    }

    @Override
    public List<Album> getMyAlbums(Long userId) {
        List<Album> albums = albumMapper.selectList(new LambdaQueryWrapper<Album>()
                .eq(Album::getUserId, userId)
                .orderByDesc(Album::getCreatedAt));
        fillCoverUrls(albums);
        return albums;
    }

    @Override
    public List<Album> getPublicAlbums(int page, int size) {
        List<Album> albums = albumMapper.selectList(new LambdaQueryWrapper<Album>()
                .eq(Album::getIsPublic, true)
                .orderByDesc(Album::getCreatedAt)
                .last("LIMIT " + size + " OFFSET " + (page - 1) * size));
        fillCoverUrls(albums);
        return albums;
    }

    @Override
    public void togglePublic(Long id, Long userId) {
        Album album = albumMapper.selectById(id);
        if (album == null || !album.getUserId().equals(userId)) {
            throw new RuntimeException("Album not found or access denied");
        }
        album.setIsPublic(!album.getIsPublic());
        album.setUpdatedAt(DateUtil.now());
        albumMapper.updateById(album);
    }

    @Override
    public List<Album> getAllAlbums() {
        List<Album> albums = albumMapper.selectList(new LambdaQueryWrapper<Album>().orderByDesc(Album::getCreatedAt));
        fillCoverUrls(albums);
        return albums;
    }

    @Override
    public void adminDelete(Long id) {
        albumMapper.deleteById(id);
    }

    @Override
    public Album adminUpdate(Long id, Album album) {
        Album existing = albumMapper.selectById(id);
        if (existing == null) throw new RuntimeException("Album not found");
        existing.setTitle(album.getTitle());
        existing.setDescription(album.getDescription());
        existing.setIsPublic(album.getIsPublic());
        existing.setUpdatedAt(DateUtil.now());
        albumMapper.updateById(existing);
        return existing;
    }
}
