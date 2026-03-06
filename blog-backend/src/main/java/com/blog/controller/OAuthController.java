package com.blog.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.pojo.entity.User;
import com.blog.service.UserService;
import com.blog.util.DateUtil;
import com.blog.util.JwtUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

@Controller
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class OAuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

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
            // 获取access_token
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Accept", "application/json");
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("client_id", githubClientId);
            params.add("client_secret", githubClientSecret);
            params.add("code", code);
            params.add("redirect_uri", githubRedirectUri);

            ResponseEntity<String> tokenRes = restTemplate.postForEntity("https://github.com/login/oauth/access_token", new HttpEntity<>(params, headers), String.class);
            String accessToken = objectMapper.readTree(tokenRes.getBody()).get("access_token").asText();

            // 获取用户信息
            headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            ResponseEntity<String> userRes = restTemplate.exchange("https://api.github.com/user", HttpMethod.GET, new HttpEntity<>(headers), String.class);
            JsonNode userInfo = objectMapper.readTree(userRes.getBody());

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

            String token = jwtUtil.generateToken(user.getId(), user.getUsername());
            String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());
            return "redirect:http://localhost:5173/oauth-callback?token=" + token + "&refreshToken=" + refreshToken + "&userId=" + user.getId();
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:http://localhost:5173/login?error=github_failed";
        }
    }

    @GetMapping("/gitee")
    public String giteeLogin() {
        return "redirect:https://gitee.com/oauth/authorize?client_id=" + giteeClientId + "&redirect_uri=" + giteeRedirectUri + "&response_type=code";
    }

    @GetMapping("/gitee/callback")
    public String giteeCallback(@RequestParam String code) {
        try {
            // 获取access_token
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "authorization_code");
            params.add("client_id", giteeClientId);
            params.add("client_secret", giteeClientSecret);
            params.add("code", code);
            params.add("redirect_uri", giteeRedirectUri);

            ResponseEntity<String> tokenRes = restTemplate.postForEntity("https://gitee.com/oauth/token", new HttpEntity<>(params, headers), String.class);
            String accessToken = objectMapper.readTree(tokenRes.getBody()).get("access_token").asText();

            // 获取用户信息
            ResponseEntity<String> userRes = restTemplate.getForEntity("https://gitee.com/api/v5/user?access_token=" + accessToken, String.class);
            JsonNode userInfo = objectMapper.readTree(userRes.getBody());

            String giteeId = userInfo.get("id").asText();
            String username = userInfo.get("login").asText();
            String avatar = userInfo.has("avatar_url") ? userInfo.get("avatar_url").asText() : null;
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
            }

            String token = jwtUtil.generateToken(user.getId(), user.getUsername());
            String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());
            return "redirect:http://localhost:5173/oauth-callback?token=" + token + "&refreshToken=" + refreshToken + "&userId=" + user.getId();
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:http://localhost:5173/login?error=gitee_failed";
        }
    }
}
