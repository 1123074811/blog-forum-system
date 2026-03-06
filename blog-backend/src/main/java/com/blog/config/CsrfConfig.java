package com.blog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

/**
 * CSRF 防护配置
 *
 * 虽然是前后端分离项目，但对于某些敏感操作（如修改密码、删除账号等）
 * 仍然建议启用 CSRF 防护
 */
@Configuration
public class CsrfConfig {

    /**
     * CSRF Token 仓库
     * 使用 Cookie 存储，前端需要从 Cookie 中读取 XSRF-TOKEN 并在请求头中携带
     */
    @Bean
    public CookieCsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieName("XSRF-TOKEN");
        repository.setHeaderName("X-XSRF-TOKEN");
        return repository;
    }

    /**
     * CSRF Token 请求处理器
     */
    @Bean
    public CsrfTokenRequestAttributeHandler csrfTokenRequestHandler() {
        CsrfTokenRequestAttributeHandler handler = new CsrfTokenRequestAttributeHandler();
        // 设置 CSRF token 的属性名
        handler.setCsrfRequestAttributeName("_csrf");
        return handler;
    }
}
