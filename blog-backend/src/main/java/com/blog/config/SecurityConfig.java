package com.blog.config;

import com.blog.filter.IpBanFilter;
import com.blog.security.JwtAuthenticationFilter;
import com.blog.security.SecurityAccessDeniedHandler;
import com.blog.security.SecurityAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final IpBanFilter ipBanFilter;
    private final SecurityAccessDeniedHandler accessDeniedHandler;
    private final SecurityAuthenticationEntryPoint authenticationEntryPoint;

    @Value("${app.security.cors-allowed-origins:http://localhost:5173}")
    private String corsAllowedOrigins;

    /** 允许的 CORS 主机（可选），与 cors-allowed-origins 二选一或同时使用；请求 Origin 的 host 在此列表中即放行，避免协议/端口差异导致 403 */
    @Value("${app.security.cors-allowed-hosts:}")
    private String corsAllowedHosts;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                    .accessDeniedHandler(accessDeniedHandler)
                    .authenticationEntryPoint(authenticationEntryPoint)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/auth/step-up/verify").authenticated()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/translate").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/articles/crawl").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/articles/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/articles").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/articles/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/articles/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/tags/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/users/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/quiz/public").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/quiz/*").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/quiz/*/questions").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/site-info").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/announcements").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/home").permitAll()
                .requestMatchers("/api/tree-hole/**").permitAll()
                .requestMatchers("/ws/**").permitAll()
                .requestMatchers("/uploads/**").permitAll()
                .requestMatchers("/api/wallpaper/**").permitAll()
                .requestMatchers("/api/life-simulator/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/albums/public").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/albums/*").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/media/public").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/media/*").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/media/album/*").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // Spring 6 PathPatternParser 不允许 "/**/xxx/**"（** 之后不能再有更多片段），否则会抛 PatternParseException
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(ipBanFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> baseOrigins = Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty() && !"*".equals(s))
                .collect(Collectors.toList());
        if (baseOrigins.isEmpty()) {
            baseOrigins = List.of("http://localhost:5173");
        }
        List<String> allowedHosts = corsAllowedHosts == null || corsAllowedHosts.isBlank()
                ? Collections.emptyList()
                : Arrays.stream(corsAllowedHosts.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());

        List<String> origins = baseOrigins;
        List<String> hosts = allowedHosts;

        return request -> {
            CorsConfiguration config = new CorsConfiguration();
            config.setAllowedOrigins(origins);
            config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
            config.setAllowedHeaders(Arrays.asList("*"));
            config.setExposedHeaders(Arrays.asList("X-New-Token"));
            config.setAllowCredentials(true);

            String origin = request.getHeader("Origin");
            if (origin != null && !origin.isBlank() && !origins.contains(origin)) {
                if (!hosts.isEmpty()) {
                    try {
                        String host = URI.create(origin).getHost();
                        if (host != null && hosts.stream().anyMatch(h -> h.equalsIgnoreCase(host))) {
                            config.setAllowedOrigins(List.of(origin));
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
            return config;
        };
    }
}
