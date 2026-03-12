package com.blog.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.pojo.entity.User;
import com.blog.service.UserService;
import com.blog.util.DateUtil;
import com.blog.util.JwtUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Slf4j
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

            String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
            String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername(), user.getRole());
            return "redirect:http://localhost:5173/oauth-callback?token=" + token + "&refreshToken=" + refreshToken + "&userId=" + user.getId();
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:http://localhost:5173/login?error=github_failed";
        }
    }

    @GetMapping("/gitee")
    public String giteeLogin() {
        log.info("用户请求 Gitee 登录");
        String authUrl = "https://gitee.com/oauth/authorize?client_id=" + giteeClientId 
                       + "&redirect_uri=" + giteeRedirectUri 
                       + "&response_type=code";
        log.info("跳转到 Gitee 授权页面: {}", authUrl);
        return "redirect:" + authUrl;
    }

    @GetMapping("/gitee/callback")
    public String giteeCallback(@RequestParam String code) {
        log.info("收到 Gitee 回调，code: {}", code);
        
        try {
            // 获取access_token
            log.info("开始获取 access_token");
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "authorization_code");
            params.add("client_id", giteeClientId);
            params.add("client_secret", giteeClientSecret);
            params.add("code", code);
            params.add("redirect_uri", giteeRedirectUri);

            log.debug("Token 请求参数: client_id={}, redirect_uri={}", giteeClientId, giteeRedirectUri);
            
            ResponseEntity<String> tokenRes = restTemplate.postForEntity(
                "https://gitee.com/oauth/token", 
                new HttpEntity<>(params, headers), 
                String.class
            );
            
            log.info("Token 响应状态码: {}", tokenRes.getStatusCode());
            log.debug("Token 响应内容: {}", tokenRes.getBody());
            
            if (tokenRes.getStatusCode() != HttpStatus.OK) {
                log.error("获取 token 失败，状态码: {}", tokenRes.getStatusCode());
                return "redirect:http://localhost:5173/login?error=gitee_token_failed";
            }
            
            JsonNode tokenJson = objectMapper.readTree(tokenRes.getBody());
            if (!tokenJson.has("access_token")) {
                log.error("响应中没有 access_token: {}", tokenRes.getBody());
                return "redirect:http://localhost:5173/login?error=gitee_no_token";
            }
            
            String accessToken = tokenJson.get("access_token").asText();
            log.info("成功获取 access_token");

            // 获取用户信息
            log.info("开始获取用户信息");
            ResponseEntity<String> userRes = restTemplate.getForEntity(
                "https://gitee.com/api/v5/user?access_token=" + accessToken, 
                String.class
            );
            
            log.info("用户信息响应状态码: {}", userRes.getStatusCode());
            
            if (userRes.getStatusCode() != HttpStatus.OK) {
                log.error("获取用户信息失败，状态码: {}", userRes.getStatusCode());
                return "redirect:http://localhost:5173/login?error=gitee_user_failed";
            }
            
            // 输出完整的用户信息JSON，用于调试
            String userResponseBody = userRes.getBody();
            log.info("Gitee 用户信息完整响应: {}", userResponseBody);
            
            JsonNode userInfo = objectMapper.readTree(userResponseBody);
            
            // 检查必需字段是否存在
            if (!userInfo.has("id")) {
                log.error("Gitee 响应中缺少 id 字段");
                return "redirect:http://localhost:5173/login?error=gitee_missing_id";
            }
            if (!userInfo.has("login")) {
                log.error("Gitee 响应中缺少 login 字段");
                return "redirect:http://localhost:5173/login?error=gitee_missing_login";
            }

            // 安全地提取字段
            String giteeId = userInfo.get("id").asText();
            String username = userInfo.get("login").asText();
            String avatar = userInfo.has("avatar_url") && !userInfo.get("avatar_url").isNull() 
                          ? userInfo.get("avatar_url").asText() 
                          : null;
            String nickname = userInfo.has("name") && !userInfo.get("name").isNull() 
                            ? userInfo.get("name").asText() 
                            : username;

            log.info("解析后的 Gitee 用户信息: id={}, username={}, nickname={}, avatar={}", 
                     giteeId, username, nickname, avatar != null ? "有头像" : "无头像");

            User user = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getGiteeId, giteeId));
            if (user == null) {
                log.info("首次登录，创建新用户");
                user = new User();
                user.setUsername("gitee_" + giteeId);
                user.setPassword("");
                user.setEmail("");
                user.setGiteeId(giteeId);
                user.setNickname(nickname);
                user.setAvatar(avatar);
                user.setRole("user");
                user.setCreatedAt(DateUtil.now());
                
                log.info("准备保存用户: username={}, nickname={}, avatar={}, giteeId={}", 
                         user.getUsername(), user.getNickname(), user.getAvatar(), user.getGiteeId());
                
                boolean saved = userService.save(user);
                if (!saved) {
                    log.error("用户保存失败");
                    return "redirect:http://localhost:5173/login?error=save_user_failed";
                }
                
                user = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getGiteeId, giteeId));
                if (user == null) {
                    log.error("保存后查询用户失败");
                    return "redirect:http://localhost:5173/login?error=query_user_failed";
                }
                
                log.info("新用户创建成功，userId: {}, nickname: {}, avatar: {}", 
                         user.getId(), user.getNickname(), user.getAvatar());
            } else {
                log.info("用户已存在，userId: {}, nickname: {}, avatar: {}", 
                         user.getId(), user.getNickname(), user.getAvatar());
                
                // 更新用户信息（如果Gitee信息有变化）
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
                    log.info("更新用户信息成功");
                }
            }

            String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
            String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername(), user.getRole());
            
            log.info("Gitee 登录成功，userId: {}", user.getId());
            
            return "redirect:http://localhost:5173/oauth-callback?token=" + token 
                 + "&refreshToken=" + refreshToken 
                 + "&userId=" + user.getId();
                 
        } catch (HttpClientErrorException e) {
            log.error("Gitee API 请求失败，状态码: {}, 响应: {}", 
                     e.getStatusCode(), e.getResponseBodyAsString());
            return "redirect:http://localhost:5173/login?error=gitee_api_error";
        } catch (JsonProcessingException e) {
            log.error("解析 JSON 失败", e);
            return "redirect:http://localhost:5173/login?error=json_parse_error";
        } catch (Exception e) {
            log.error("Gitee 登录失败", e);
            return "redirect:http://localhost:5173/login?error=gitee_failed&message=" + e.getMessage();
        }
    }
}
