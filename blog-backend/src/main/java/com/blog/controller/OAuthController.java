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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Controller
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class OAuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final TokenService tokenService;
    private final RestTemplate restTemplate;
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
        return "redirect:https://github.com/login/oauth/authorize?client_id=" + githubClientId + "&redirect_uri=" + githubRedirectUri + "&scope=user";
    }

    @GetMapping("/github/callback")
    public String githubCallback(@RequestParam String code) {
        try {
            String accessToken = fetchGithubToken(code);
            JsonNode userInfo = fetchGithubUser(accessToken);
            User user = upsertGithubUser(userInfo);
            return buildOAuthSuccessRedirect(user);
        } catch (Exception e) {
            log.error("GitHub oauth failed", e);
            return buildOAuthErrorRedirect("github_failed", e.getMessage());
        }
    }

    @GetMapping("/gitee")
    public String giteeLogin() {
        String authUrl = "https://gitee.com/oauth/authorize?client_id=" + giteeClientId
                + "&redirect_uri=" + giteeRedirectUri
                + "&response_type=code";
        return "redirect:" + authUrl;
    }

    @GetMapping("/gitee/callback")
    public String giteeCallback(@RequestParam String code) {
        try {
            String accessToken = fetchGiteeToken(code);
            JsonNode userInfo = fetchGiteeUser(accessToken);
            User user = upsertGiteeUser(userInfo);
            return buildOAuthSuccessRedirect(user);
        } catch (Exception e) {
            log.error("Gitee oauth failed", e);
            return buildOAuthErrorRedirect("gitee_failed", e.getMessage());
        }
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

    private String buildOAuthSuccessRedirect(User user) {
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername(), user.getRole());
        tokenService.saveToken(token, user.getId(), tokenExpiration);
        tokenService.saveToken(refreshToken, user.getId(), refreshTokenExpiration);
        return "redirect:" + frontendRedirectBase + "/oauth-callback?token=" + token + "&refreshToken=" + refreshToken + "&userId=" + user.getId();
    }

    private String buildOAuthErrorRedirect(String error, String message) {
        return "redirect:" + frontendRedirectBase + "/login?error=" + error + "&message=" + (message == null ? "" : message);
    }
}
