package com.blog.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.pojo.dto.ApiResponse;
import com.blog.pojo.dto.BanIpRequest;
import com.blog.pojo.dto.RevokeSessionsRequest;
import com.blog.pojo.dto.UnblockIpRequest;
import com.blog.pojo.entity.IpBlacklist;
import com.blog.pojo.entity.SecurityEvent;
import com.blog.pojo.entity.User;
import com.blog.service.SecurityEventService;
import com.blog.service.TokenService;
import com.blog.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/admin/security")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSecurityController {

    private final TokenService tokenService;
    private final UserService userService;
    private final SecurityEventService securityEventService;
    private final com.blog.mapper.SecurityEventMapper securityEventMapper;

    @PostMapping("/sessions/revoke")
    public ApiResponse<Long> revokeSessions(@RequestBody RevokeSessionsRequest request) {
        long revoked;
        if (request.isRevokeAll()) {
            revoked = tokenService.revokeAllSessions();
            return ApiResponse.success(revoked);
        }

        if (request.getUserId() != null) {
            revoked = tokenService.revokeUserSessions(request.getUserId());
            return ApiResponse.success(revoked);
        }

        if (request.getRole() != null && !request.getRole().isBlank()) {
            List<User> users = userService.list(new LambdaQueryWrapper<User>().eq(User::getRole, request.getRole()));
            long total = 0L;
            for (User user : users) {
                total += tokenService.revokeUserSessions(user.getId());
            }
            return ApiResponse.success(total);
        }

        return ApiResponse.error("Missing revoke target");
    }

    @PostMapping("/risk/unblock-ip")
    public ApiResponse<Boolean> unblockIp(@RequestBody UnblockIpRequest request) {
        if (request.getIp() == null || request.getIp().isBlank()) {
            return ApiResponse.error("IP is required");
        }
        securityEventService.unbanIp(request.getIp());
        return ApiResponse.success(true);
    }

    @PostMapping("/risk/ban-ip")
    public ApiResponse<Boolean> banIp(@RequestBody BanIpRequest request) {
        if (request.getIp() == null || request.getIp().isBlank()) {
            return ApiResponse.error("IP is required");
        }
        String reason = request.getReason();
        if (reason == null || reason.isBlank()) {
            reason = "security_policy";
        }
        Duration ttl = request.getDurationMinutes() != null && request.getDurationMinutes() > 0
                ? Duration.ofMinutes(request.getDurationMinutes())
                : null;
        securityEventService.banIp(request.getIp(), reason, ttl);
        return ApiResponse.success(true);
    }

    @GetMapping("/banned-ips")
    public ApiResponse<List<IpBlacklist>> getBannedIps() {
        return ApiResponse.success(securityEventService.listEffectiveBans());
    }

    @GetMapping("/events")
    public ApiResponse<Page<SecurityEvent>> getEvents(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        Page<SecurityEvent> pageParam = new Page<>(page, size);
        Page<SecurityEvent> result = securityEventMapper.selectPage(pageParam,
                new LambdaQueryWrapper<SecurityEvent>().orderByDesc(SecurityEvent::getId));
        return ApiResponse.success(result);
    }
}
