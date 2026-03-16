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
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.blog.websocket.ChatWebSocketHandler;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController extends BaseController {

    private final UserService userService;
    private final FollowService followService;
    private final NotificationService notificationService;
    private final UserConverter userConverter;
    private final ChatWebSocketHandler chatWebSocketHandler;

    @GetMapping("/{id}")
    public ApiResponse<UserVO> getUser(@PathVariable Long id, Authentication auth) {
        User user = userService.getById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        UserVO userVO = userConverter.toVO(user);
        userVO.setFollowerCount(followService.getFollowerCount(id));
        userVO.setFollowingCount(followService.getFollowingCount(id));
        userVO.setIsOnline(chatWebSocketHandler.isOnline(id));
        Long currentUserId = getCurrentUserIdOptional(auth);
        if (currentUserId != null && !currentUserId.equals(id)) {
            userVO.setIsFollowing(followService.isFollowing(currentUserId, id));
        } else {
            userVO.setIsFollowing(false);
        }
        // hide email for non-owner
        if (currentUserId == null || !currentUserId.equals(id)) {
            userVO.setEmail(null);
        }

        return ApiResponse.success(userVO);
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

        userService.updateById(user);
        userService.clearUserCache(user.getId(), user.getUsername());
        return ApiResponse.success(userConverter.toVO(user));
    }

    @PostMapping("/{id}/follow")
    @RateLimit(key = "user:follow", count = 30, time = 60, limitType = RateLimit.LimitType.USER, message = "操作过于频繁，请稍后再试")
    public ApiResponse<Void> follow(@PathVariable Long id, Authentication auth) {
        Long currentUserId = getCurrentUserId(auth);
        if (currentUserId.equals(id)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不能关注自己");
        }
        followService.follow(currentUserId, id);
        notificationService.send(id, currentUserId, "follow", currentUserId, "关注了你");
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{id}/follow")
    public ApiResponse<Void> unfollow(@PathVariable Long id, Authentication auth) {
        Long currentUserId = getCurrentUserId(auth);
        followService.unfollow(currentUserId, id);
        return ApiResponse.success(null);
    }
}
