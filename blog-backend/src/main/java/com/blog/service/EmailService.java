package com.blog.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final StringRedisTemplate redisTemplate;

    @Value("${spring.mail.username}")
    private String fromEmail;

    private static final String RESET_TOKEN_PREFIX = "reset:";
    private static final String RESET_CODE_PREFIX = "reset_code:";
    private static final long TOKEN_EXPIRE_MINUTES = 30;

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

    public void sendResetPasswordEmail(String toEmail, String username, String code) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(toEmail);
        helper.setSubject("重置密码验证码");
        helper.setText(buildResetEmailContent(username, code), true);
        mailSender.send(message);
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
