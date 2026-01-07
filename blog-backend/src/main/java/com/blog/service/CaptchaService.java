package com.blog.service;

import com.google.code.kaptcha.Producer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class CaptchaService {

    private final Producer captchaProducer;
    private final StringRedisTemplate redisTemplate;

    private static final String CAPTCHA_PREFIX = "captcha:";
    private static final long CAPTCHA_EXPIRE_MINUTES = 5;

    public CaptchaResult generateCaptcha() throws IOException {
        String captchaId = UUID.randomUUID().toString();
        String captchaText = captchaProducer.createText();
        BufferedImage image = captchaProducer.createImage(captchaText);

        redisTemplate.opsForValue().set(
            CAPTCHA_PREFIX + captchaId,
            captchaText.toLowerCase(),
            CAPTCHA_EXPIRE_MINUTES,
            TimeUnit.MINUTES
        );

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());

        return new CaptchaResult(captchaId, "data:image/png;base64," + base64);
    }

    public boolean validateCaptcha(String captchaId, String captchaCode) {
        if (captchaId == null || captchaCode == null) {
            return false;
        }
        String storedCode = redisTemplate.opsForValue().get(CAPTCHA_PREFIX + captchaId);
        if (storedCode != null && storedCode.equalsIgnoreCase(captchaCode.trim())) {
            redisTemplate.delete(CAPTCHA_PREFIX + captchaId);
            return true;
        }
        return false;
    }

    public record CaptchaResult(String captchaId, String imageBase64) {}
}
