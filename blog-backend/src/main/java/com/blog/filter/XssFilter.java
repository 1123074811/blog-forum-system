package com.blog.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * XSS 过滤器
 */
@Slf4j
public class XssFilter implements Filter {

    private List<String> excludes;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        String excludesParam = filterConfig.getInitParameter("excludes");
        if (StringUtils.hasText(excludesParam)) {
            excludes = Arrays.asList(excludesParam.split(","));
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String uri = httpRequest.getRequestURI();

        // 检查是否在排除列表中
        if (excludes != null && excludes.stream().anyMatch(uri::contains)) {
            chain.doFilter(request, response);
            return;
        }

        // 使用 XSS 包装器
        XssHttpServletRequestWrapper xssRequest = new XssHttpServletRequestWrapper(httpRequest);
        chain.doFilter(xssRequest, response);
    }

    @Override
    public void destroy() {
    }
}
