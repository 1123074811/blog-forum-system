package com.blog.security;

import com.blog.context.BaseContext;
import com.blog.pojo.entity.User;
import com.blog.service.TokenService;
import com.blog.service.UserService;
import com.blog.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final TokenService tokenService;
    private final UserService userService;

    @Value("${jwt.expiration}")
    private long expiration;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                if (jwtUtil.validateToken(token) && jwtUtil.isAccessToken(token) && tokenService.validateToken(token)) {
                    Long userId = jwtUtil.getUserIdFromToken(token);
                    String role = userService.getUserRoleFromCache(userId);
                    if (role == null) {
                        User user = userService.getById(userId);
                        if (user != null && !Boolean.TRUE.equals(user.getBanned())) {
                            role = user.getRole();
                            userService.cacheUserRole(userId, role);
                        }
                    }

                    if (role != null) {
                        String authority = "admin".equalsIgnoreCase(role) ? "ROLE_ADMIN" : "ROLE_USER";
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                userId,
                                null,
                                List.of(new SimpleGrantedAuthority(authority))
                        );
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        BaseContext.setCurrentId(userId);
                        BaseContext.setCurrentRole(role);
                        tokenService.refreshToken(token, expiration);
                    }
                }
            } catch (RuntimeException ex) {
                // 线上常见：Redis 不可用/权限不对/网络抖动导致 token 校验抛异常。
                // 降级策略：视为未登录，交给后续鉴权返回 401，而不是直接 500。
                SecurityContextHolder.clearContext();
                BaseContext.removeAll();
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            BaseContext.removeAll();
        }
    }
}
