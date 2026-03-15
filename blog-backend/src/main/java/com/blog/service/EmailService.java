package com.blog.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final StringRedisTemplate redisTemplate;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${spring.mail.sender-name:Blog}")
    private String senderName;

    private static final String RESET_TOKEN_PREFIX = "reset:";
    private static final String RESET_CODE_PREFIX = "reset_code:";
    private static final String RESET_VERIFIED_PREFIX = "reset_verified:";
    private static final long TOKEN_EXPIRE_MINUTES = 30;

    private static final String REGISTER_CODE_PREFIX = "reg_code:";
    private static final long REGISTER_CODE_EXPIRE_MINUTES = 10;

    private void setFrom(MimeMessageHelper helper) throws MessagingException {
        try {
            helper.setFrom(new InternetAddress(fromEmail, senderName, "UTF-8"));
        } catch (java.io.UnsupportedEncodingException e) {
            helper.setFrom(fromEmail);
        }
    }

    public String generateResetToken(Long userId) {
        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(RESET_TOKEN_PREFIX + token, userId.toString(), TOKEN_EXPIRE_MINUTES, TimeUnit.MINUTES);
        return token;
    }

    public Long validateResetToken(String token) {
        String userId = redisTemplate.opsForValue().get(RESET_TOKEN_PREFIX + token);
        if (userId != null) {
            redisTemplate.delete(RESET_TOKEN_PREFIX + token);
            return Long.parseLong(userId);
        }
        return null;
    }

    public void storeResetCode(String email, String code) {
        redisTemplate.opsForValue().set(RESET_CODE_PREFIX + email, code, TOKEN_EXPIRE_MINUTES, TimeUnit.MINUTES);
    }

    public boolean validateResetCode(String email, String code) {
        String storedCode = redisTemplate.opsForValue().get(RESET_CODE_PREFIX + email);
        if (storedCode != null && storedCode.equals(code)) {
            redisTemplate.delete(RESET_CODE_PREFIX + email);
            return true;
        }
        return false;
    }

    public void markResetVerified(String email) {
        redisTemplate.opsForValue().set(RESET_VERIFIED_PREFIX + email, "1", TOKEN_EXPIRE_MINUTES, TimeUnit.MINUTES);
    }

    public boolean consumeResetVerified(String email) {
        String key = RESET_VERIFIED_PREFIX + email;
        String flag = redisTemplate.opsForValue().get(key);
        if (flag != null) {
            redisTemplate.delete(key);
            return true;
        }
        return false;
    }

    public void sendResetPasswordEmail(String toEmail, String username, String code) throws MessagingException {
        log.info("正在发送重置密码邮件到: {}，用户名: {}", toEmail, username);
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        setFrom(helper);
        helper.setTo(toEmail);
        helper.setSubject("重置密码验证码");
        helper.setText(buildResetEmailContent(username, code), true);
        mailSender.send(message);
        log.info("重置密码邮件已成功发送到: {}", toEmail);
    }

    /**
     * 检查重置密码令牌是否存在
     */
    public boolean hasValidResetToken(String token) {
        String userId = redisTemplate.opsForValue().get(RESET_TOKEN_PREFIX + token);
        return userId != null;
    }


    // ===== 注册邮箱验证码 =====

    public void sendRegisterCode(String toEmail, String code) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        setFrom(helper);
        helper.setTo(toEmail);
        helper.setSubject("注册验证码");
        helper.setText(buildRegisterEmailContent(code), true);
        mailSender.send(message);
        log.info("注册验证码已发送到: {}", toEmail);
    }

    public void storeRegisterCode(String email, String code) {
        redisTemplate.opsForValue().set(
            REGISTER_CODE_PREFIX + email.toLowerCase(),
            code,
            REGISTER_CODE_EXPIRE_MINUTES,
            TimeUnit.MINUTES
        );
    }

    /**
     * 验证注册验证码（验证后立即删除，防止重放）
     */
    public boolean validateRegisterCode(String email, String code) {
        String key = REGISTER_CODE_PREFIX + email.toLowerCase();
        String stored = redisTemplate.opsForValue().get(key);
        if (stored != null && stored.equals(code)) {
            redisTemplate.delete(key);
            return true;
        }
        return false;
    }

    private String buildRegisterEmailContent(String code) {
        return "<div style=\"max-width:600px;margin:0 auto;padding:20px;font-family:Arial,sans-serif;\">" +
            "<h2 style=\"color:#333;\">邮箱验证</h2>" +
            "<p>您正在注册账号，验证码为：</p>" +
            "<div style=\"text-align:center;margin:30px 0;\">" +
            "<span style=\"background:#4F46E5;color:white;padding:12px 30px;font-size:24px;letter-spacing:5px;border-radius:5px;display:inline-block;\">" + code + "</span>" +
            "</div>" +
            "<p style=\"color:#999;font-size:12px;\">此验证码10分钟内有效。如果您没有注册，请忽略此邮件。</p>" +
            "</div>";
    }

    private String buildResetEmailContent(String username, String code) {
        return "<div style=\"max-width:600px;margin:0 auto;padding:20px;font-family:Arial,sans-serif;\">" +
            "<h2 style=\"color:#333;\">重置密码</h2>" +
            "<p>亲爱的 " + username + "，</p>" +
            "<p>您的密码重置验证码是：</p>" +
            "<div style=\"text-align:center;margin:30px 0;\">" +
            "<span style=\"background:#4F46E5;color:white;padding:12px 30px;font-size:24px;letter-spacing:5px;border-radius:5px;display:inline-block;\">" + code + "</span>" +
            "</div>" +
            "<p style=\"color:#999;font-size:12px;\">此验证码30分钟内有效。如果您没有请求重置密码，请忽略此邮件。</p>" +
            "</div>";
    }

}
