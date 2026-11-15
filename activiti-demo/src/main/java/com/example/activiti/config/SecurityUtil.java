package com.example.activiti.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * 安全工具:模拟登录用户写入 SecurityContext。
 * Activiti7 的 TaskRuntime/ProcessRuntime 会从 SecurityContext 取当前用户,
 * 且要求该用户拥有 ROLE_ACTIVITI_USER 权限,否则操作任务会抛错。
 */
@Component
public class SecurityUtil {

    private final UserDetailsService userDetailsService;

    public SecurityUtil(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    /**
     * 模拟指定用户登录,写入 SecurityContext。
     * 在每个调用 Activiti Runtime API 的方法前调用。
     */
    public void logInAs(String username) {
        UserDetails user = userDetailsService.loadUserByUsername(username);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    /**
     * 获取当前登录用户名
     */
    public String getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return null;
        }
        return auth.getName();
    }
}
