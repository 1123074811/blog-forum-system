package com.blog.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.pojo.entity.User;
import com.blog.service.TokenService;
import com.blog.service.UserService;
import com.blog.util.DateUtil;
import com.blog.util.JwtUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Controller
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class OAuthController {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String OAUTH_STATE_PREFIX = "oauth:state:";
    private static final String OAUTH_TICKET_PREFIX = "oauth:ticket:";
    private static final long STATE_TTL_MINUTES = 5;
    private static final long TICKET_TTL_SECONDS = 60;

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final TokenService tokenService;
    private final RestTemplate restTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${jwt.expiration}")
    private long tokenExpiration;
    @Value("${jwt.refresh-expiration}")
    private long refreshTokenExpiration;

    @Value("${oauth.frontend-redirect-base:http://localhost:5173}")
    private String frontendRedirectBase;

    @Value("${oauth.github.client-id}")
    private String githubClientId;
    @Value("${oauth.github.client-secret}")
    private String githubClientSecret;
    @Value("${oauth.github.redirect-uri}")
    private String githubRedirectUri;

    @Value("${oauth.gitee.client-id}")
    private String giteeClientId;
    @Value("${oauth.gitee.client-secret}")
    private String giteeClientSecret;
    @Value("${oauth.gitee.redirect-uri}")
    private String giteeRedirectUri;

    @GetMapping("/github")
    public String githubLogin() {
        String state = generateState("github");
        return "redirect:https://github.com/login/oauth/authorize?client_id=" + encodeUrlParam(githubClientId)
                + "&redirect_uri=" + encodeUrlParam(githubRedirectUri)
                + "&scope=user"
                + "&state=" + encodeUrlParam(state);
    }

    @GetMapping("/github/callback")
    public String githubCallback(@RequestParam String code, @RequestParam(required = false) String state) {
        if (!validateState("github", state)) {
            log.warn("GitHub OAuth state validation failed");
            return buildOAuthErrorRedirect("invalid_state");
        }
        try {
            String accessToken = fetchGithubToken(code);
            JsonNode userInfo = fetchGithubUser(accessToken);
            User user = upsertGithubUser(userInfo);
            return buildOAuthTicketRedirect(user);
        } catch (Exception e) {
            log.error("GitHub oauth failed", e);
            return buildOAuthErrorRedirect("github_failed");
        }
    }

    @GetMapping("/gitee")
    public String giteeLogin() {
        String state = generateState("gitee");
        String authUrl = "https://gitee.com/oauth/authorize?client_id=" + encodeUrlParam(giteeClientId)
                + "&redirect_uri=" + encodeUrlParam(giteeRedirectUri)
                + "&response_type=code"
                + "&state=" + encodeUrlParam(state);
        return "redirect:" + authUrl;
    }

    @GetMapping("/gitee/callback")
    public String giteeCallback(@RequestParam String code, @RequestParam(required = false) String state) {
        if (!validateState("gitee", state)) {
            log.warn("Gitee OAuth state validation failed");
            return buildOAuthErrorRedirect("invalid_state");
        }
        try {
            String accessToken = fetchGiteeToken(code);
            JsonNode userInfo = fetchGiteeUser(accessToken);
            User user = upsertGiteeUser(userInfo);
            return buildOAuthTicketRedirect(user);
        } catch (Exception e) {
            log.error("Gitee oauth failed", e);
            return buildOAuthErrorRedirect("gitee_failed");
        }
    }

    @PostMapping("/exchange-ticket")
    @com.blog.annotation.RateLimit(key = "oauth_exchange_ticket", count = 10, time = 60, limitType = com.blog.annotation.RateLimit.LimitType.IP, message = "请求过于频繁，请稍后再试")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> exchangeTicket(@RequestBody Map<String, String> body) {
        if (body == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Invalid ticket"));
        }
        String ticket = body.get("ticket");
        if (ticket == null || !ticket.matches("[a-f0-9]{64}")) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Invalid ticket"));
        }

        String key = OAUTH_TICKET_PREFIX + ticket;
        Object userIdObj = redisTemplate.opsForValue().get(key);
        if (userIdObj == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Ticket expired or invalid"));
        }
        redisTemplate.delete(key);

        Long userId = Long.valueOf(userIdObj.toString());
        User user = userService.getById(userId);
        if (user == null || Boolean.TRUE.equals(user.getBanned())) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "User not found"));
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername(), user.getRole());
        tokenService.saveToken(token, user.getId(), tokenExpiration);
        tokenService.saveToken(refreshToken, user.getId(), refreshTokenExpiration);
        userService.cacheUserRole(user.getId(), user.getRole());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "token", token,
                "refreshToken", refreshToken,
                "userId", user.getId()
        ));
    }

    private String fetchGithubToken(String code) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Accept", "application/json");

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", githubClientId);
        params.add("client_secret", githubClientSecret);
        params.add("code", code);
        params.add("redirect_uri", githubRedirectUri);

        ResponseEntity<String> tokenRes = restTemplate.postForEntity("https://github.com/login/oauth/access_token", new HttpEntity<>(params, headers), String.class);
        JsonNode tokenJson = objectMapper.readTree(tokenRes.getBody());
        return tokenJson.get("access_token").asText();
    }

    private JsonNode fetchGithubUser(String accessToken) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        ResponseEntity<String> userRes = restTemplate.exchange("https://api.github.com/user", HttpMethod.GET, new HttpEntity<>(headers), String.class);
        return objectMapper.readTree(userRes.getBody());
    }

    private User upsertGithubUser(JsonNode userInfo) {
        String githubId = userInfo.get("id").asText();
        String username = userInfo.get("login").asText();
        String avatar = userInfo.has("avatar_url") ? userInfo.get("avatar_url").asText() : null;
        String nickname = userInfo.has("name") && !userInfo.get("name").isNull() ? userInfo.get("name").asText() : username;

        User user = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getGithubId, githubId));
        if (user == null) {
            user = new User();
            user.setUsername("github_" + githubId);
            user.setPassword("");
            user.setEmail("");
            user.setGithubId(githubId);
            user.setNickname(nickname);
            user.setAvatar(avatar);
            user.setRole("user");
            user.setCreatedAt(DateUtil.now());
            userService.save(user);
            user = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getGithubId, githubId));
        }
        return user;
    }

    private String fetchGiteeToken(String code) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", giteeClientId);
        params.add("client_secret", giteeClientSecret);
        params.add("code", code);
        params.add("redirect_uri", giteeRedirectUri);

        ResponseEntity<String> tokenRes = restTemplate.postForEntity("https://gitee.com/oauth/token", new HttpEntity<>(params, headers), String.class);
        JsonNode tokenJson = objectMapper.readTree(tokenRes.getBody());
        return tokenJson.get("access_token").asText();
    }

    private JsonNode fetchGiteeUser(String accessToken) throws Exception {
        ResponseEntity<String> userRes = restTemplate.getForEntity("https://gitee.com/api/v5/user?access_token=" + accessToken, String.class);
        return objectMapper.readTree(userRes.getBody());
    }

    private User upsertGiteeUser(JsonNode userInfo) {
        String giteeId = userInfo.get("id").asText();
        String username = userInfo.get("login").asText();
        String avatar = userInfo.has("avatar_url") && !userInfo.get("avatar_url").isNull() ? userInfo.get("avatar_url").asText() : null;
        String nickname = userInfo.has("name") && !userInfo.get("name").isNull() ? userInfo.get("name").asText() : username;

        User user = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getGiteeId, giteeId));
        if (user == null) {
            user = new User();
            user.setUsername("gitee_" + giteeId);
            user.setPassword("");
            user.setEmail("");
            user.setGiteeId(giteeId);
            user.setNickname(nickname);
            user.setAvatar(avatar);
            user.setRole("user");
            user.setCreatedAt(DateUtil.now());
            userService.save(user);
            user = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getGiteeId, giteeId));
        } else {
            boolean needUpdate = false;
            if (nickname != null && !nickname.equals(user.getNickname())) {
                user.setNickname(nickname);
                needUpdate = true;
            }
            if (avatar != null && !avatar.equals(user.getAvatar())) {
                user.setAvatar(avatar);
                needUpdate = true;
            }
            if (needUpdate) {
                userService.updateById(user);
            }
        }
        return user;
    }

    private String generateState(String provider) {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        String state = sb.toString();
        redisTemplate.opsForValue().set(OAUTH_STATE_PREFIX + provider + ":" + state, "1", STATE_TTL_MINUTES, TimeUnit.MINUTES);
        return state;
    }

    private boolean validateState(String provider, String state) {
        if (state == null || state.isBlank()) {
            return false;
        }
        String key = OAUTH_STATE_PREFIX + provider + ":" + state;
        Boolean hasKey = redisTemplate.hasKey(key);
        if (Boolean.TRUE.equals(hasKey)) {
            redisTemplate.delete(key);
            return true;
        }
        return false;
    }

    private String buildOAuthTicketRedirect(User user) {
        String ticket = generateSecureToken();
        redisTemplate.opsForValue().set(OAUTH_TICKET_PREFIX + ticket, user.getId(), TICKET_TTL_SECONDS, TimeUnit.SECONDS);
        return "redirect:" + frontendRedirectBase + "/oauth-callback?ticket=" + encodeUrlParam(ticket);
    }

    private String buildOAuthErrorRedirect(String error) {
        return "redirect:" + frontendRedirectBase + "/oauth-callback?error=" + encodeUrlParam(error);
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private String encodeUrlParam(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
