package com.example.activiti.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * SpringSecurity 配置。
 * Activiti7 强依赖 SpringSecurity,TaskRuntime 需要 SecurityContext 中的用户
 * 拥有 ROLE_ACTIVITI_USER 权限。
 *
 * demo 方案:
 * 1. 放行所有 API(anyRequest().permitAll())
 * 2. 注册 InMemory 用户(zhangsan/lisi/wangwu),赋 ROLE_ACTIVITIT_USER
 * 3. 注册 CurrentUserFilter,从请求头 X-Current-User 设置操作人身份
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private final SecurityUtil securityUtil;

    public SecurityConfig(SecurityUtil securityUtil) {
        this.securityUtil = securityUtil;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .authorizeRequests()
                .anyRequest().permitAll()
            .and()
            // 在 SpringSecurity 认证过滤器之前注册 CurrentUserFilter,
            // 使后续 Activiti 能从 SecurityContext 取到当前用户
            .addFilterBefore(new CurrentUserFilter(securityUtil), UsernamePasswordAuthenticationFilter.class);
    }

    /**
     * InMemory 用户:demo 审批人,均拥有 ROLE_ACTIVITI_USER 权限。
     * 密码统一为 123456(BCrypt 编码)。
     *
     * 注意:必须声明为 static Bean。
     * SecurityConfig -> SecurityUtil -> UserDetailsService -> SecurityConfig,
     * 静态 @Bean 无需先实例化配置类即可创建,从而打破该循环依赖。
     */
    @Bean
    public static UserDetailsService userDetailsService(PasswordEncoder encoder) {
        InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager(
            User.builder()
                .username("zhangsan")
                .password(encoder.encode("123456"))
                .roles("ACTIVITI_USER")
                .build(),
            User.builder()
                .username("lisi")
                .password(encoder.encode("123456"))
                .roles("ACTIVITI_USER")
                .build(),
            User.builder()
                .username("wangwu")
                .password(encoder.encode("123456"))
                .roles("ACTIVITI_USER")
                .build()
        );
        return manager;
    }

    @Bean
    public static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
