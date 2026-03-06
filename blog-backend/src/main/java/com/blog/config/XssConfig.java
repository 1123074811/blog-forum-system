package com.blog.config;

import com.blog.filter.XssFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * XSS 防护配置
 */
@Configuration
public class XssConfig {

    @Bean
    public FilterRegistrationBean<XssFilter> xssFilterRegistration() {
        FilterRegistrationBean<XssFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new XssFilter());
        registration.addUrlPatterns("/*");
        registration.setName("xssFilter");
        registration.setOrder(1);

        Map<String, String> initParameters = new HashMap<>();
        // 排除不需要过滤的接口（如文件上传）
        initParameters.put("excludes", "/api/files/upload,/api/media/upload");
        registration.setInitParameters(initParameters);

        return registration;
    }
}
