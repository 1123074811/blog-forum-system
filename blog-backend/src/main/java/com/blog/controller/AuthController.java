package com.blog.controller;

import com.blog.dto.ApiResponse;
import com.blog.dto.AuthResponse;
import com.blog.dto.ForgotPasswordRequest;
import com.blog.dto.LoginRequest;
import com.blog.dto.RegisterRequest;
import com.blog.dto.ResetPasswordRequest;
import com.blog.dto.VerifyCodeRequest;
import com.blog.entity.User;
import com.blog.service.CaptchaService;
import com.blog.service.EmailService;
import com.blog.service.RateLimitService;
import com.blog.service.TokenService;
import com.blog.service.UserService;
import com.blog.util.JwtUtil;
import com.blog.util.PasswordUtil;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.mail.MailException;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final CaptchaService captchaService;
    private final EmailService emailService;
    private final RateLimitService rateLimitService;
    private final TokenService tokenService;

    @org.springframework.beans.factory.annotation.Value("${jwt.expiration}")
    private long tokenExpiration;

    @GetMapping("/captcha")
    public ApiResponse<Map<String, String>> getCaptcha() throws IOException {
        CaptchaService.CaptchaResult result = captchaService.generateCaptcha();
        return ApiResponse.success(Map.of("key", result.captchaId(), "image", result.imageBase64()));
    }

    @PostMapping("/forgot-password")
    public ApiResponse<String> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        String username = request.getUsername();
        if (username == null || username.isBlank()) {
            return ApiResponse.error("账号不能为空");
        }
        User user = userService.findByUsername(username);
        if (user == null) {
            return ApiResponse.error("账号不存在");
        }
        
        String email = user.getEmail();
        if (email == null || email.trim().isEmpty()) {
            return ApiResponse.error("用户未绑定邮箱地址，无法重置密码");
        }
        
        // 验证邮箱格式
        if (!PasswordUtil.isValidEmail(email)) {
            log.warn("用户 {} 绑定的邮箱地址格式无效: {}", username, email);
            return ApiResponse.error("用户邮箱地址格式不正确，无法发送邮件");
        }
        
        String code = String.format("%06d", new Random().nextInt(1000000));
        try {
            emailService.sendResetPasswordEmail(email, user.getUsername(), code);
            // 存储验证码到Redis
            emailService.storeResetCode(email, code);
        } catch (MailException e) {
            log.error("发送重置密码邮件失败", e);
            if (e.getMessage() != null && e.getMessage().contains("550")) {
                log.warn("邮件发送失败，邮箱地址可能不存在: {}", email);
                return ApiResponse.error("邮箱地址不存在，请检查");
            }
            return ApiResponse.error("邮件发送失败: " + e.getMessage());
        } catch (MessagingException e) {
            log.error("发送重置密码邮件失败", e);
            if (e.getMessage() != null && e.getMessage().contains("550")) {
                log.warn("邮件发送失败，邮箱地址可能不存在: {}", email);
                return ApiResponse.error("邮箱地址不存在，请检查");
            }
            return ApiResponse.error("邮件发送失败: " + e.getMessage());
        }
        return ApiResponse.success("验证码已发送到邮箱");
    }

    @PostMapping("/verify-code")
    public ApiResponse<String> verifyCode(@RequestBody VerifyCodeRequest request) {
        String username = request.getUsername();
        String code = request.getCode();
        if (username == null || username.isBlank() || code == null || code.isBlank()) {
            return ApiResponse.error("参数不能为空");
        }
        User user = userService.findByUsername(username);
        if (user == null) {
            return ApiResponse.error("账号不存在");
        }
        String email = user.getEmail();
        if (!emailService.validateResetCode(email, code)) {
            return ApiResponse.error("验证码错误或已过期");
        }
        emailService.markResetVerified(email);
        return ApiResponse.success("验证通过");
    }

    @PostMapping("/reset-password")
    public ApiResponse<String> resetPassword(@RequestBody ResetPasswordRequest request) {
        String username = request.getUsername();
        String code = request.getCode();
        String newPassword = request.getPassword();
        
        if (username == null || code == null || newPassword == null) {
            return ApiResponse.error("参数不能为空");
        }

        User user = userService.findByUsername(username);
        if (user == null) {
            return ApiResponse.error("用户不存在");
        }

        String email = user.getEmail();
        if (!emailService.consumeResetVerified(email)) {
            return ApiResponse.error("验证码已过期，请重新获取");
        }
        
        if (!PasswordUtil.isValidPassword(newPassword)) {
            return ApiResponse.error("密码格式不正确，长度至少6位");
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        userService.updateById(user);
        userService.clearUserCache(user.getId(), user.getUsername());
        return ApiResponse.success("密码重置成功");
    }

    @PostMapping("/register")
    public ApiResponse<String> register(@RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        if (!captchaService.validateCaptcha(request.getCaptchaId(), request.getCaptchaCode())) {
            return ApiResponse.error("验证码错误或已过期");
        }
        String ip = getClientIp(httpRequest);
        if (!rateLimitService.isRegisterAllowed(ip)) {
            return ApiResponse.error("注册过于频繁，请1小时后再试");
        }
        if (userService.existsByUsername(request.getUsername())) {
            return ApiResponse.error("用户名已存在");
        }
        if (userService.existsByEmail(request.getEmail())) {
            return ApiResponse.error("邮箱已被注册");
        }
        userService.register(request.getUsername(), request.getPassword(), request.getEmail());
        rateLimitService.recordRegister(ip);
        return ApiResponse.success("注册成功");
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@RequestBody LoginRequest request) {
        if (!captchaService.validateCaptcha(request.getCaptchaId(), request.getCaptchaCode())) {
            return ApiResponse.error("验证码错误或已过期");
        }
        User user = userService.findByUsername(request.getUsername());
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ApiResponse.error("用户名或密码错误");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());
        // 将 token 存储到 Redis
        tokenService.saveToken(token, user.getId(), tokenExpiration);
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
        // 将新 token 存储到 Redis
        tokenService.saveToken(newToken, userId, tokenExpiration);
        return ApiResponse.success(newToken);
    }

    @PostMapping("/logout")
    public ApiResponse<String> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            tokenService.deleteToken(token);
        }
        return ApiResponse.success("登出成功");
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
