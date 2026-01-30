package com.blog.controller;

import com.blog.dto.ApiResponse;
import com.blog.entity.User;
import com.blog.service.FollowService;
import com.blog.service.NotificationService;
import com.blog.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final FollowService followService;
    private final NotificationService notificationService;

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> getUser(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user == null) {
            return ApiResponse.error("User not found");
        }
        user.setPassword(null);

        Map<String, Object> result = new HashMap<>();
        result.put("user", user);
        result.put("followerCount", followService.getFollowerCount(id));
        result.put("followingCount", followService.getFollowingCount(id));

        return ApiResponse.success(result);
    }

    @PutMapping("/{id}")
    public ApiResponse<User> updateUser(@PathVariable Long id, @RequestBody User updateData, Authentication auth) {
        Long currentUserId = (Long) auth.getPrincipal();
        if (!currentUserId.equals(id)) {
            return ApiResponse.error("Unauthorized");
        }

        User user = userService.getById(id);
        if (user == null) {
            return ApiResponse.error("User not found");
        }

        if (updateData.getAvatar() != null) user.setAvatar(updateData.getAvatar());
        if (updateData.getNickname() != null) user.setNickname(updateData.getNickname());
        if (updateData.getBio() != null) user.setBio(updateData.getBio());

        userService.updateById(user);
        userService.clearUserCache(user.getId(), user.getUsername());
        user.setPassword(null);
        return ApiResponse.success(user);
    }

    @PostMapping("/{id}/follow")
    public ApiResponse<Boolean> follow(@PathVariable Long id, Authentication auth) {
        Long currentUserId = (Long) auth.getPrincipal();
        if (currentUserId.equals(id)) {
            return ApiResponse.error("Cannot follow yourself");
        }
        followService.follow(currentUserId, id);
        notificationService.send(id, currentUserId, "follow", currentUserId, "关注了你");
        return ApiResponse.success(true);
    }

    @DeleteMapping("/{id}/follow")
    public ApiResponse<Boolean> unfollow(@PathVariable Long id, Authentication auth) {
        Long currentUserId = (Long) auth.getPrincipal();
        followService.unfollow(currentUserId, id);
        return ApiResponse.success(true);
    }
}
