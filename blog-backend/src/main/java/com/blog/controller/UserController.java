package com.blog.controller;

import com.blog.annotation.RateLimit;
import com.blog.converter.UserConverter;
import com.blog.pojo.dto.ApiResponse;
import com.blog.pojo.dto.UserUpdateRequest;
import com.blog.pojo.entity.User;
import com.blog.exception.BusinessException;
import com.blog.exception.ErrorCode;
import com.blog.service.FollowService;
import com.blog.service.NotificationService;
import com.blog.service.UserService;
import com.blog.pojo.vo.UserVO;
import com.blog.util.CacheUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.blog.websocket.ChatWebSocketHandler;

import jakarta.validation.Valid;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController extends BaseController {

    private final UserService userService;
    private final FollowService followService;
    private final NotificationService notificationService;
    private final UserConverter userConverter;
    private final ChatWebSocketHandler chatWebSocketHandler;
    private final CacheUtil cacheUtil;

    private static final String CACHE_USER_PROFILE = "user:profile:";
    private static final long PROFILE_CACHE_TTL = 5L;

    @GetMapping("/{id}")
    public ApiResponse<UserVO> getUser(@PathVariable Long id, Authentication auth) {
        Long currentUserId = getCurrentUserIdOptional(auth);

        // 对于非登录用户，缓存公开的用户资料
        if (currentUserId == null) {
            String cacheKey = CACHE_USER_PROFILE + id;
            UserVO cached = cacheUtil.get(cacheKey);
            if (cached != null) {
                return ApiResponse.success(cached);
            }
            UserVO userVO = buildUserVO(id, null);
            cacheUtil.set(cacheKey, userVO, PROFILE_CACHE_TTL, TimeUnit.MINUTES);
            return ApiResponse.success(userVO);
        }

        // 登录用户：缓存不含isFollowing的基础资料，isFollowing单独查
        // 本人资料优先直读数据库，避免更新后短时间读取到旧缓存
        if (currentUserId.equals(id)) {
            UserVO userVO = buildUserVO(id, currentUserId);
            userVO.setIsFollowing(false);
            userVO.setEmail(userVO.getEmail());
            userVO.setIsOnline(chatWebSocketHandler.isOnline(id));
            return ApiResponse.success(userVO);
        }

        String cacheKey = CACHE_USER_PROFILE + id;
        UserVO userVO;
        UserVO cached = cacheUtil.get(cacheKey);
        if (cached != null) {
            userVO = cached;
        } else {
            userVO = buildUserVO(id, null);
            cacheUtil.set(cacheKey, userVO, PROFILE_CACHE_TTL, TimeUnit.MINUTES);
        }

        // isFollowing 是用户相关的动态数据，不缓存到公共key
        if (!currentUserId.equals(id)) {
            userVO.setIsFollowing(followService.isFollowing(currentUserId, id));
        } else {
            userVO.setIsFollowing(false);
            userVO.setEmail(userVO.getEmail()); // 本人可见email
        }
        // 非本人隐藏email
        if (!currentUserId.equals(id)) {
            userVO.setEmail(null);
        }
        // 在线状态实时查（WebSocket内存，无DB开销）
        userVO.setIsOnline(chatWebSocketHandler.isOnline(id));

        return ApiResponse.success(userVO);
    }

    private UserVO buildUserVO(Long id, Long currentUserId) {
        User user = userService.getById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        UserVO userVO = userConverter.toVO(user);
        // getFollowerCount/getFollowingCount 内部已有Redis缓存
        userVO.setFollowerCount(followService.getFollowerCount(id));
        userVO.setFollowingCount(followService.getFollowingCount(id));
        userVO.setIsOnline(chatWebSocketHandler.isOnline(id));
        userVO.setIsFollowing(false);
        return userVO;
    }

    @PutMapping("/{id}")
    public ApiResponse<UserVO> updateUser(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest updateData, Authentication auth) {
        Long currentUserId = getCurrentUserId(auth);
        checkPermission(id, currentUserId);

        User user = userService.getById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        if (updateData.getAvatar() != null) user.setAvatar(updateData.getAvatar());
        if (updateData.getNickname() != null) user.setNickname(updateData.getNickname());
        if (updateData.getBio() != null) user.setBio(updateData.getBio());

        boolean updated = userService.updateById(user);
        if (!updated) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新资料失败，请稍后重试");
        }
        userService.clearUserCache(user.getId(), user.getUsername());
        // 清除用户资料缓存
        cacheUtil.delete(CACHE_USER_PROFILE + id);
        User latest = userService.getById(id);
        if (latest == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return ApiResponse.success(userConverter.toVO(latest));
    }

    @PostMapping("/{id}/follow")
    @RateLimit(key = "user:follow", count = 30, time = 60, limitType = RateLimit.LimitType.USER, message = "操作过于频繁，请稍后再试")
    public ApiResponse<Void> follow(@PathVariable Long id, Authentication auth) {
        Long currentUserId = getCurrentUserId(auth);
        if (currentUserId.equals(id)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不能关注自己");
        }
        followService.follow(currentUserId, id);
        // 清除被关注者的资料缓存（粉丝数变了）
        cacheUtil.delete(CACHE_USER_PROFILE + id);
        notificationService.send(id, currentUserId, "follow", currentUserId, "关注了你");
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{id}/follow")
    public ApiResponse<Void> unfollow(@PathVariable Long id, Authentication auth) {
        Long currentUserId = getCurrentUserId(auth);
        followService.unfollow(currentUserId, id);
        cacheUtil.delete(CACHE_USER_PROFILE + id);
        return ApiResponse.success(null);
    }
}
