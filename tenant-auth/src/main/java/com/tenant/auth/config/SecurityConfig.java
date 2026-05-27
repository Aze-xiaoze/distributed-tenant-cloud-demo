package com.tenant.auth.config;

import com.tenant.auth.filter.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 安全配置类
 * 基于Spring Security 6的 {@link SecurityFilterChain} 函数式配置方式（替代废弃的WebSecurityConfigurerAdapter）
 * <p>配置策略：
 * <ul>
 *   <li>禁用CSRF — 微服务架构使用JWT，无需CSRF防护</li>
 *   <li>无状态会话 — 不使用HttpSession，完全依赖JWT令牌</li>
 *   <li>放行路径 — 登录、注册、令牌验证、健康检查</li>
 *   <li>认证入口 — 未认证返回401 JSON，无权限返回403 JSON</li>
 * </ul>
 *
 * @author Aze
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /**
     * 密码编码器
     * 使用BCrypt算法对密码进行加密
     *
     * @return 密码编码器实例
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 配置安全过滤链
     * 定义认证授权策略，集成JWT过滤器
     *
     * @param http HTTP安全配置对象
     * @return SecurityFilterChain 实例
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 禁用CSRF保护（微服务架构使用JWT，无需CSRF）
            .csrf(csrf -> csrf.disable())
            // 无状态会话管理（JWT方式，不使用Session）
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 配置请求授权规则
            .authorizeHttpRequests(auth -> auth
                // 允许访问登录和注册接口
                .requestMatchers("/auth/login", "/auth/register", "/auth/validate-token").permitAll()
                // 允许访问健康检查和监控接口
                .requestMatchers("/actuator/**").permitAll()
                // 其他所有请求都需要认证
                .anyRequest().authenticated()
            )
            // 配置异常处理
            .exceptionHandling(exceptions -> exceptions
                // 未认证处理
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setContentType("application/json;charset=UTF-8");
                    response.setStatus(401);
                    response.getWriter().write("{\"code\":401,\"message\":\"未认证，请先登录\",\"timestamp\":" + System.currentTimeMillis() + "}");
                })
                // 无权限处理
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setContentType("application/json;charset=UTF-8");
                    response.setStatus(403);
                    response.getWriter().write("{\"code\":403,\"message\":\"无权限访问\",\"timestamp\":" + System.currentTimeMillis() + "}");
                })
            )
            // 在UsernamePasswordAuthenticationFilter之前插入JWT过滤器
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}