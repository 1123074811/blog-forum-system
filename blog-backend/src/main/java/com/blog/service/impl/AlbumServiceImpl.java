package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.pojo.entity.Album;
import com.blog.pojo.entity.Media;
import com.blog.pojo.entity.User;
import com.blog.mapper.AlbumMapper;
import com.blog.mapper.MediaMapper;
import com.blog.mapper.UserMapper;
import com.blog.service.AlbumService;
import com.blog.util.DateUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AlbumServiceImpl implements AlbumService {

    private final AlbumMapper albumMapper;
    private final MediaMapper mediaMapper;
    private final UserMapper userMapper;

    private void fillCoverUrls(List<Album> albums) {
        for (Album album : albums) {
            List<Media> mediaList = mediaMapper.selectList(new LambdaQueryWrapper<Media>()
                    .eq(Media::getAlbumId, album.getId())
                    .orderByDesc(Media::getCreatedAt)
                    .last("LIMIT 3"));
            // 封面列表优先使用缩略图，降级到原图
            album.setCoverUrls(mediaList.stream()
                    .map(m -> m.getThumbnailUrl() != null ? m.getThumbnailUrl() : m.getUrl())
                    .collect(Collectors.toList()));
            if (album.getCoverUrl() == null && !mediaList.isEmpty()) {
                Media first = mediaList.get(0);
                album.setCoverUrl(first.getThumbnailUrl() != null ? first.getThumbnailUrl() : first.getUrl());
            }
        }
    }
    
    private void fillUserInfo(List<Album> albums) {
        List<Long> userIds = albums.stream().map(Album::getUserId).distinct().toList();
        if (userIds.isEmpty()) return;
        
        List<User> users = userMapper.selectBatchIds(userIds);
        Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u));
        
        for (Album album : albums) {
            // 如果是匿名发布，不填充用户信息
            if (Boolean.TRUE.equals(album.getIsAnonymous())) {
                continue;
            }
            User user = userMap.get(album.getUserId());
            if (user != null) {
                album.setNickname(user.getNickname());
                album.setAvatar(user.getAvatar());
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
        fillUserInfo(albums);
        return albums;
    }

    @Override
    public void togglePublic(Long id, Long userId, Boolean isAnonymous) {
        Album album = albumMapper.selectById(id);
        if (album == null || !album.getUserId().equals(userId)) {
            throw new RuntimeException("Album not found or access denied");
        }
        album.setIsPublic(!album.getIsPublic());
        // 如果设为公开，设置匿名状态；如果设为私密，清除匿名状态
        if (album.getIsPublic()) {
            album.setIsAnonymous(isAnonymous != null && isAnonymous);
        } else {
            album.setIsAnonymous(false);
        }
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

    @Override
    public void removeByIds(List<Long> ids) {
        albumMapper.deleteBatchIds(ids);
    }
}
