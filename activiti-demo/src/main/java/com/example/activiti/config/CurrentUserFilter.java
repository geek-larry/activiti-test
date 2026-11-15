package com.example.activiti.config;

import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 读取请求头 X-Current-User 并调用 SecurityUtil.logInAs() 设置当前操作人身份。
 * 在 SecurityConfig 中通过 addFilterBefore 注册到 SpringSecurity 过滤链,
 * 使每个请求自动设置身份,前端只需在请求头带 X-Current-User。
 */
public class CurrentUserFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Current-User";
    public static final String DEFAULT_USER = "zhangsan";

    private final SecurityUtil securityUtil;

    public CurrentUserFilter(SecurityUtil securityUtil) {
        this.securityUtil = securityUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String username = request.getHeader(HEADER);
        if (username == null || username.trim().isEmpty()) {
            username = DEFAULT_USER;
        }
        securityUtil.logInAs(username);
        filterChain.doFilter(request, response);
    }
}
