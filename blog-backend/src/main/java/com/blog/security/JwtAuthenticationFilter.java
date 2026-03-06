package com.blog.security;

import com.blog.context.BaseContext;
import com.blog.service.TokenService;
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
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final TokenService tokenService;

    @Value("${jwt.expiration}")
    private long expiration;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            // 先验证 JWT 签名，再验证 Redis 中是否存在
            if (jwtUtil.validateToken(token) && tokenService.validateToken(token)) {
                Long userId = jwtUtil.getUserIdFromToken(token);
                String username = jwtUtil.getUsernameFromToken(token);

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userId, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // 同时设置到 ThreadLocal，方便 AOP 和 Service 使用
                BaseContext.setCurrentId(userId);

                // 滑动过期：每次请求都刷新 Redis 中 token 的过期时间
                tokenService.refreshToken(token, expiration);
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            // 请求结束后清理 ThreadLocal，防止内存泄漏
            BaseContext.removeCurrentId();
        }
    }
}
