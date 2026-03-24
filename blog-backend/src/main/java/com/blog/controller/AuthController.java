package com.blog.controller;

import com.blog.annotation.RateLimit;
import com.blog.context.BaseContext;
import com.blog.pojo.dto.ApiResponse;
import com.blog.pojo.dto.AuthResponse;
import com.blog.pojo.dto.ForgotPasswordRequest;
import com.blog.pojo.dto.LoginRequest;
import com.blog.pojo.dto.RegisterRequest;
import com.blog.pojo.dto.ResetPasswordRequest;
import com.blog.pojo.dto.StepUpRequest;
import com.blog.pojo.dto.VerifyCodeRequest;
import com.blog.pojo.entity.User;
import com.blog.service.CaptchaService;
import com.blog.service.EmailService;
import com.blog.service.RateLimitService;
import com.blog.service.SecurityEventService;
import com.blog.service.TokenService;
import com.blog.service.UserService;
import com.blog.util.JwtUtil;
import com.blog.util.PasswordUtil;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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
    private final SecurityEventService securityEventService;
    private final org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;
    private final HttpServletRequest httpServletRequest;

    @org.springframework.beans.factory.annotation.Value("${jwt.expiration}")
    private long tokenExpiration;

    @org.springframework.beans.factory.annotation.Value("${jwt.refresh-expiration}")
    private long refreshTokenExpiration;

    @GetMapping("/captcha")
    @RateLimit(key = "captcha", count = 10, time = 60, limitType = RateLimit.LimitType.IP)
    public ApiResponse<Map<String, Object>> getCaptcha() throws IOException {
        CaptchaService.CaptchaResult result = captchaService.generateCaptcha();
        return ApiResponse.success(Map.of(
                "key", result.captchaId(),
                "image", result.imageBase64(),
                "expiresIn", result.expiresInSeconds()
        ));
    }

    @PostMapping("/forgot-password")
    @RateLimit(key = "forgot_password", count = 3, time = 300, limitType = RateLimit.LimitType.IP, message = "请求过于频繁，请5分钟后再试")
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
            log.warn("Invalid email format for user {}: {}", username, email);
            return ApiResponse.error("用户邮箱地址格式不正确，无法发送邮件");
        }

        String code = String.format("%06d", new Random().nextInt(1000000));
        try {
            emailService.sendResetPasswordEmail(email, user.getUsername(), code);
            // 存储验证码到Redis
            emailService.storeResetCode(email, code);
        } catch (MailException e) {
            log.error("Failed to send reset password mail", e);
            return ApiResponse.error("邮件发送失败: " + e.getMessage());
        } catch (MessagingException e) {
            log.error("Failed to send reset password mail", e);
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

    // 一次性邮箱域名黑名单
    private static final java.util.Set<String> DISPOSABLE_EMAIL_DOMAINS = java.util.Set.of(
        "mailinator.com", "tempmail.com", "guerrillamail.com", "10minutemail.com",
        "throwam.com", "yopmail.com", "sharklasers.com", "guerrillamailblock.com",
        "trashmail.com", "dispostable.com", "fakeinbox.com", "maildrop.cc"
    );

    @PostMapping("/send-register-code")
    @RateLimit(key = "send_reg_code", count = 2, time = 60, limitType = RateLimit.LimitType.IP, message = "发送过于频繁，请稍后再试")
    public ApiResponse<String> sendRegisterCode(@RequestBody Map<String, String> body, HttpServletRequest requestContext) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ApiResponse.error("邮箱不能为空");
        }
        if (!PasswordUtil.isValidEmail(email)) {
            return ApiResponse.error("邮箱格式不正确");
        }
        // 一次性邮箱拦截
        String domain = email.substring(email.indexOf('@') + 1).toLowerCase();
        if (DISPOSABLE_EMAIL_DOMAINS.contains(domain)) {
            return ApiResponse.error("不支持使用临时邮箱注册");
        }
        if (userService.existsByEmail(email)) {
            return ApiResponse.error("邮箱已被注册");
        }
        // 域名频率检查
        if (!rateLimitService.isDomainAllowed(email)) {
            log.warn("Domain register rate limit exceeded: {}", domain);
            return ApiResponse.error("该邮箱域名注册过于频繁，请稍后再试");
        }
        // 全局速率兜底
        if (!rateLimitService.isGlobalRateAllowed()) {
            log.warn("Global register rate limit exceeded");
            return ApiResponse.error("系统繁忙，请稍后再试");
        }
        String code = String.format("%06d", new Random().nextInt(1000000));
        try {
            emailService.storeRegisterCode(email, code);
            emailService.sendRegisterCode(email, code);
        } catch (Exception e) {
            log.error("发送注册验证码失败: {}", email, e);
            return ApiResponse.error("邮件发送失败，请检查邮箱地址");
        }
        return ApiResponse.success("验证码已发送，请查收邮件");
    }

    @PostMapping("/register")
    @RateLimit(key = "register", count = 3, time = 3600, limitType = RateLimit.LimitType.IP, message = "注册过于频繁，请1小时后再试")
    public ApiResponse<String> register(@RequestBody RegisterRequest request, HttpServletRequest requestContext) {
        if (!captchaService.validateCaptcha(request.getCaptchaId(), request.getCaptchaCode())) {
            return ApiResponse.error("验证码错误或已过期");
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            return ApiResponse.error("邮箱不能为空");
        }
        if (!PasswordUtil.isValidEmail(request.getEmail())) {
            return ApiResponse.error("邮箱格式不正确");
        }
        // 校验邮箱验证码
        if (request.getEmailCode() == null || request.getEmailCode().isBlank()) {
            return ApiResponse.error("请先获取邮箱验证码");
        }
        if (!emailService.validateRegisterCode(request.getEmail(), request.getEmailCode())) {
            return ApiResponse.error("邮箱验证码错误或已过期");
        }
        String ip = getClientIp(requestContext);
        if (!rateLimitService.isRegisterAllowed(ip)) {
            return ApiResponse.error("注册过于频繁，请1小时后再试");
        }
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            return ApiResponse.error("账号不能为空");
        }
        if (!PasswordUtil.isValidUsername(request.getUsername())) {
            return ApiResponse.error("账号格式不正确：4-20位，只允许字母、数字、下划线");
        }
        if (userService.existsByUsername(request.getUsername())) {
            return ApiResponse.error("用户名已存在");
        }
        if (userService.existsByEmail(request.getEmail())) {
            return ApiResponse.error("邮箱已被注册");
        }
        userService.register(request.getUsername(), request.getPassword(), request.getEmail());
        rateLimitService.recordRegister(ip);
        rateLimitService.recordDomainRegister(request.getEmail());
        rateLimitService.recordGlobalRegister();
        return ApiResponse.success("注册成功");
    }

    @PostMapping("/login")
    @RateLimit(key = "login", count = 5, time = 60, limitType = RateLimit.LimitType.IP, message = "登录失败次数过多，请1分钟后再试")
    public ApiResponse<AuthResponse> login(@RequestBody LoginRequest request) {
        if (!captchaService.validateCaptcha(request.getCaptchaId(), request.getCaptchaCode())) {
            return ApiResponse.error("验证码错误或已过期");
        }
        // 先尝试通过用户名查找用户
        User user = userService.findByUsername(request.getUsername());
        // 如果找不到，尝试通过邮箱查找用户
        if (user == null) {
            user = userService.findByEmail(request.getUsername());
        }
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            securityEventService.recordLoginFail(getClientIp(httpServletRequest), request.getUsername());
            return ApiResponse.error("账号或密码错误");
        }
        if (Boolean.TRUE.equals(user.getBanned())) {
            return ApiResponse.error("该账号已被封禁，无法登录");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername(), user.getRole());
        // 将 token 存储到 Redis
        tokenService.saveToken(token, user.getId(), tokenExpiration);
        tokenService.saveToken(refreshToken, user.getId(), refreshTokenExpiration);
        userService.cacheUserRole(user.getId(), user.getRole());
        user.setPassword(null);
        return ApiResponse.success(new AuthResponse(token, refreshToken, user));
    }

    @PostMapping("/refresh")
    public ApiResponse<String> refresh(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ApiResponse.error("Invalid token");
        }
        String refreshToken = authHeader.substring(7);
        if (!jwtUtil.validateToken(refreshToken) || !jwtUtil.isRefreshToken(refreshToken) || !tokenService.validateToken(refreshToken)) {
            return ApiResponse.error("Invalid or expired refresh token");
        }

        Long userId = jwtUtil.getUserIdFromToken(refreshToken);
        User user = userService.getById(userId);
        if (user == null || Boolean.TRUE.equals(user.getBanned())) {
            return ApiResponse.error("User not found or banned");
        }

        String role = user.getRole();
        userService.cacheUserRole(userId, role);
        String newToken = jwtUtil.generateToken(userId, user.getUsername(), role);
        tokenService.saveToken(newToken, userId, tokenExpiration);
        return ApiResponse.success(newToken);
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return ApiResponse.success(Map.of("authenticated", false));
        }
        User user = userService.getById(userId);
        if (user == null) {
            return ApiResponse.success(Map.of("authenticated", false));
        }
        return ApiResponse.success(Map.of(
                "authenticated", true,
                "role", user.getRole() != null ? user.getRole() : "USER",
                "id", user.getId()
        ));
    }

    @PostMapping("/logout")
    public ApiResponse<String> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            tokenService.deleteToken(token);
        }
        return ApiResponse.success("登出成功");
    }

    @PostMapping("/step-up/verify")
    public ApiResponse<String> stepUpVerify(@RequestBody StepUpRequest request) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return ApiResponse.error("Unauthorized");
        }
        if (request == null || request.getPassword() == null || request.getPassword().isBlank()) {
            return ApiResponse.error("密码不能为空");
        }

        User user = userService.getById(userId);
        if (user == null) {
            return ApiResponse.error("用户不存在");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            securityEventService.recordLoginFail(getClientIp(httpServletRequest), user.getUsername());
            return ApiResponse.error("密码错误");
        }

        String operation = sanitizeOperation(request.getOperation());
        String opToken = UUID.randomUUID().toString();
        String key = "stepup:token:" + userId + ":" + operation;
        redisTemplate.opsForValue().set(key, opToken, 5, TimeUnit.MINUTES);
        return ApiResponse.success(opToken);
    }

    private String sanitizeOperation(String operation) {
        if (operation == null || operation.isBlank()) {
            return "generic";
        }
        String normalized = operation.trim().toLowerCase();
        if (!normalized.matches("[a-z0-9_:-]{1,64}")) {
            return "generic";
        }
        return normalized;
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
