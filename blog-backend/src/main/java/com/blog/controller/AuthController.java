package com.blog.controller;

import com.blog.dto.ApiResponse;
import com.blog.dto.AuthResponse;
import com.blog.dto.LoginRequest;
import com.blog.dto.RegisterRequest;
import com.blog.entity.User;
import com.blog.service.UserService;
import com.blog.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@RequestBody RegisterRequest request) {
        if (userService.existsByUsername(request.getUsername())) {
            return ApiResponse.error("Username already exists");
        }
        if (userService.existsByEmail(request.getEmail())) {
            return ApiResponse.error("Email already exists");
        }

        User user = userService.register(request.getUsername(), request.getPassword(), request.getEmail());
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());

        user.setPassword(null);
        return ApiResponse.success(new AuthResponse(token, refreshToken, user));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@RequestBody LoginRequest request) {
        User user = userService.findByUsername(request.getUsername());
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ApiResponse.error("Invalid username or password");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());

        user.setPassword(null);
        return ApiResponse.success(new AuthResponse(token, refreshToken, user));
    }

    @PostMapping("/refresh")
    public ApiResponse<String> refresh(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ApiResponse.error("Invalid token");
        }

        String refreshToken = authHeader.substring(7);
        if (!jwtUtil.validateToken(refreshToken)) {
            return ApiResponse.error("Invalid or expired refresh token");
        }

        Long userId = jwtUtil.getUserIdFromToken(refreshToken);
        String username = jwtUtil.getUsernameFromToken(refreshToken);
        String newToken = jwtUtil.generateToken(userId, username);

        return ApiResponse.success(newToken);
    }
}
