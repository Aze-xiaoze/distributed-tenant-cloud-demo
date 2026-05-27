package com.tenant.auth.filter;

import com.tenant.auth.util.JwtUtil;
import com.tenant.core.tenant.TenantContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT认证过滤器
 * 从请求头中提取JWT令牌，验证并设置Spring Security上下文和租户上下文
 * <p>处理流程：
 * <ol>
 *   <li>从Authorization头提取Bearer Token</li>
 *   <li>解析JWT获取用户名和租户ID</li>
 *   <li>将租户ID设置到 {@link TenantContextHolder}（JWT中的租户ID优先，其次取X-Tenant-ID请求头）</li>
 *   <li>将用户名设置到Spring Security上下文，标记当前请求已认证</li>
 * </ol>
 * <p>无有效JWT时，仅从请求头获取租户ID设置上下文（适用于公开接口的租户隔离）
 * <p><b>重要</b>：finally块中清理 {@link TenantContextHolder}，防止ThreadLocal内存泄漏
 *
 * @author Aze
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String TENANT_HEADER_NAME = "X-Tenant-ID";

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // 1. 从请求头中提取JWT令牌
            String token = extractToken(request);

            if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // 2. 从令牌中提取用户名并验证
                String username = jwtUtil.getUsernameFromToken(token);

                if (username != null && jwtUtil.validateToken(token, username)) {
                    // 3. 从令牌中提取租户ID并设置到租户上下文
                    String tenantId = jwtUtil.getTenantIdFromToken(token);
                    if (tenantId != null) {
                        TenantContextHolder.setCurrentTenantId(tenantId);
                    } else {
                        // 如果令牌中没有租户ID，则从请求头中获取
                        String headerTenantId = request.getHeader(TENANT_HEADER_NAME);
                        if (headerTenantId != null && !headerTenantId.trim().isEmpty()) {
                            TenantContextHolder.setCurrentTenantId(headerTenantId);
                        }
                    }

                    // 4. 设置Spring Security认证上下文
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    username,
                                    null,
                                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                            );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } else {
                // 没有有效令牌时，从请求头获取租户ID
                String headerTenantId = request.getHeader(TENANT_HEADER_NAME);
                if (headerTenantId != null && !headerTenantId.trim().isEmpty()) {
                    TenantContextHolder.setCurrentTenantId(headerTenantId);
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            // 请求结束后清理租户上下文，防止内存泄漏
            TenantContextHolder.clear();
        }
    }

    /**
     * 从请求头中提取JWT令牌
     * 格式：Authorization: Bearer <token>
     *
     * @param request HTTP请求
     * @return JWT令牌，如果没有则返回null
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
