package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.pojo.entity.Album;
import com.blog.pojo.entity.Media;
import com.blog.pojo.entity.User;
import com.blog.mapper.AlbumMapper;
import com.blog.mapper.MediaMapper;
import com.blog.mapper.UserMapper;
import com.blog.service.MediaService;
import com.blog.service.MinioService;
import com.blog.util.DateUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MediaServiceImpl implements MediaService {

    private final MediaMapper mediaMapper;
    private final AlbumMapper albumMapper;
    private final UserMapper userMapper;
    private final MinioService minioService;

    private void fillUserInfo(List<Media> mediaList) {
        if (mediaList.isEmpty()) return;
        List<Long> userIds = mediaList.stream().map(Media::getUserId).distinct().toList();
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        mediaList.forEach(m -> {
            // 如果是匿名发布，不填充用户信息
            if (Boolean.TRUE.equals(m.getIsAnonymous())) {
                return;
            }
            User u = userMap.get(m.getUserId());
            if (u != null) {
                m.setNickname(u.getNickname());
                m.setAvatar(u.getAvatar());
            }
        });
    }

    @Override
    public Media upload(MultipartFile file, Long albumId, String title, String description, Long userId) {
        try {
            String url = minioService.upload(file);
            String contentType = file.getContentType();
            String type = contentType != null && contentType.startsWith("video") ? "video" : "image";

            Media media = new Media();
            media.setUserId(userId);
            media.setAlbumId(albumId);
            media.setTitle(title);
            media.setDescription(description);
            media.setUrl(url);
            media.setType(type);
            media.setFileSize(file.getSize());
            media.setIsPublic(false);
            media.setCreatedAt(DateUtil.now());
            media.setUpdatedAt(DateUtil.now());
            mediaMapper.insert(media);

            if (albumId != null) {
                Album album = albumMapper.selectById(albumId);
                if (album != null) {
                    album.setMediaCount(album.getMediaCount() + 1);
                    if (album.getCoverUrl() == null) {
                        album.setCoverUrl(url);
                    }
                    albumMapper.updateById(album);
                }
            }
            return media;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload media", e);
        }
    }

    @Override
    public Media update(Long id, String title, String description, Long userId) {
        Media media = mediaMapper.selectById(id);
        if (media == null || !media.getUserId().equals(userId)) {
            throw new RuntimeException("Media not found or access denied");
        }
        media.setTitle(title);
        media.setDescription(description);
        media.setUpdatedAt(DateUtil.now());
        mediaMapper.updateById(media);
        return media;
    }

    @Override
    public void delete(Long id, Long userId) {
        Media media = mediaMapper.selectById(id);
        if (media == null || !media.getUserId().equals(userId)) {
            throw new RuntimeException("Media not found or access denied");
        }
        mediaMapper.deleteById(id);
        if (media.getAlbumId() != null) {
            Album album = albumMapper.selectById(media.getAlbumId());
            if (album != null) {
                album.setMediaCount(Math.max(0, album.getMediaCount() - 1));
                albumMapper.updateById(album);
            }
        }
    }

    @Override
    public Media getById(Long id, Long userId) {
        Media media = mediaMapper.selectById(id);
        if (media == null) return null;
        if (media.getIsPublic() || media.getUserId().equals(userId)) {
            return media;
        }
        return null;
    }

    @Override
    public List<Media> getByAlbumId(Long albumId, Long userId) {
        Album album = albumMapper.selectById(albumId);
        if (album == null) return List.of();
        if (!album.getIsPublic() && !album.getUserId().equals(userId)) {
            return List.of();
        }
        List<Media> list = mediaMapper.selectList(new LambdaQueryWrapper<Media>()
                .eq(Media::getAlbumId, albumId)
                .orderByDesc(Media::getCreatedAt));
        fillUserInfo(list);
        return list;
    }

    @Override
    public List<Media> getMyMedia(Long userId, String type, int page, int size) {
        LambdaQueryWrapper<Media> wrapper = new LambdaQueryWrapper<Media>()
                .eq(Media::getUserId, userId)
                .orderByDesc(Media::getCreatedAt)
                .last("LIMIT " + size + " OFFSET " + (page - 1) * size);
        if (type != null) {
            wrapper.eq(Media::getType, type);
        }
        List<Media> list = mediaMapper.selectList(wrapper);
        fillUserInfo(list);
        return list;
    }

    @Override
    public List<Media> getPublicMedia(String type, int page, int size) {
        LambdaQueryWrapper<Media> wrapper = new LambdaQueryWrapper<Media>()
                .eq(Media::getIsPublic, true)
                .orderByDesc(Media::getCreatedAt)
                .last("LIMIT " + size + " OFFSET " + (page - 1) * size);
        if (type != null) {
            wrapper.eq(Media::getType, type);
        }
        List<Media> list = mediaMapper.selectList(wrapper);
        fillUserInfo(list);
        return list;
    }

    @Override
    public void togglePublic(Long id, Long userId, Boolean isAnonymous) {
        Media media = mediaMapper.selectById(id);
        if (media == null || !media.getUserId().equals(userId)) {
            throw new RuntimeException("Media not found or access denied");
        }
        media.setIsPublic(!media.getIsPublic());
        // 如果设为公开，设置匿名状态；如果设为私密，清除匿名状态
        if (media.getIsPublic()) {
            media.setIsAnonymous(isAnonymous != null && isAnonymous);
        } else {
            media.setIsAnonymous(false);
        }
        media.setUpdatedAt(DateUtil.now());
        mediaMapper.updateById(media);
    }

    @Override
    public List<Media> getAllMedia() {
        List<Media> list = mediaMapper.selectList(new LambdaQueryWrapper<Media>().orderByDesc(Media::getCreatedAt));
        fillUserInfo(list);
        // 根据所属相册的公开状态设置媒体的公开状态
        List<Long> albumIds = list.stream().map(Media::getAlbumId).filter(id -> id != null).distinct().toList();
        if (!albumIds.isEmpty()) {
            Map<Long, Boolean> albumPublicMap = albumMapper.selectBatchIds(albumIds).stream()
                    .collect(Collectors.toMap(Album::getId, a -> Boolean.TRUE.equals(a.getIsPublic())));
            list.forEach(m -> {
                if (m.getAlbumId() != null && Boolean.TRUE.equals(albumPublicMap.get(m.getAlbumId()))) {
                    m.setIsPublic(true);
                }
            });
        }
        return list;
    }

    @Override
    public void adminDelete(Long id) {
        Media media = mediaMapper.selectById(id);
        if (media != null && media.getAlbumId() != null) {
            Album album = albumMapper.selectById(media.getAlbumId());
            if (album != null) {
                album.setMediaCount(Math.max(0, album.getMediaCount() - 1));
                albumMapper.updateById(album);
            }
        }
        mediaMapper.deleteById(id);
    }

    @Override
    public Media adminUpdate(Long id, String title, String description, Boolean isPublic) {
        Media media = mediaMapper.selectById(id);
        if (media == null) throw new RuntimeException("Media not found");
        media.setTitle(title);
        media.setDescription(description);
        media.setIsPublic(isPublic);
        media.setUpdatedAt(DateUtil.now());
        mediaMapper.updateById(media);
        return media;
    }

    @Override
    public void removeByIds(List<Long> ids) {
        mediaMapper.deleteBatchIds(ids);
    }
}
