package com.tenant.system.config;

import com.tenant.system.filter.SystemJwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * tenant-system 安全配置
 * 接受网关转发的已认证请求，通过 {@link SystemJwtAuthFilter} 验证JWT令牌
 * <p>配置策略：
 * <ul>
 *   <li>禁用CSRF — 微服务架构使用JWT</li>
 *   <li>无状态会话 — 不使用HttpSession</li>
 *   <li>放行路径 — 仅健康检查（/actuator/**），其他全部需认证</li>
 * </ul>
 *
 * @author Aze
 */
@Configuration
@EnableWebSecurity
public class SystemSecurityConfig {

    private final SystemJwtAuthFilter systemJwtAuthFilter;

    public SystemSecurityConfig(SystemJwtAuthFilter systemJwtAuthFilter) {
        this.systemJwtAuthFilter = systemJwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(systemJwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
