package com.tenant.auth.filter;

import com.tenant.auth.util.JwtUtil;
import com.tenant.core.tenant.TenantContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private static final String AUTHORIZATION_HEADER = com.tenant.common.constant.TenantConstants.AUTHORIZATION_HEADER;
    private static final String BEARER_PREFIX = com.tenant.common.constant.TenantConstants.BEARER_PREFIX;
    private static final String TENANT_HEADER_NAME = com.tenant.common.constant.TenantConstants.X_TENANT_ID_HEADER;

    /**
     * 租户ID来源验证标记头（由网关AuthGatewayFilter设置）
     */
    private static final String TENANT_VERIFIED_HEADER = com.tenant.common.constant.TenantConstants.X_TENANT_VERIFIED_HEADER;

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
                        // 如果令牌中没有租户ID，则从请求头中获取（仅当租户ID已由网关验证时）
                        String headerTenantId = request.getHeader(TENANT_HEADER_NAME);
                        String verified = request.getHeader(TENANT_VERIFIED_HEADER);
                        if (headerTenantId != null && !headerTenantId.trim().isEmpty()) {
                            if (!"true".equals(verified)) {
                                log.warn("安全警告：JWT中无租户ID，且请求头租户ID未经验证。tenantId={}, path={}",
                                        headerTenantId, request.getRequestURI());
                            }
                            TenantContextHolder.setCurrentTenantId(headerTenantId);
                        }
                    }

                    // 4. 从JWT中提取角色和权限，设置Spring Security认证上下文
                    List<String> roles = jwtUtil.getRolesFromToken(token);
                    List<String> permissions = jwtUtil.getPermissionsFromToken(token);
                    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                    // 添加角色（ROLE_前缀，供@PreAuthorize("hasRole('XXX')")使用）
                    if (roles != null) {
                        authorities.addAll(roles.stream()
                                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                                .collect(Collectors.toList()));
                    }
                    // 添加权限标识（供@RequiresPermission和@PreAuthorize("hasAuthority('XXX')")使用）
                    if (permissions != null) {
                        authorities.addAll(permissions.stream()
                                .map(SimpleGrantedAuthority::new)
                                .collect(Collectors.toList()));
                    }
                    // 如果JWT中无角色和权限，默认赋予ROLE_USER
                    if (authorities.isEmpty()) {
                        authorities.add(new SimpleGrantedAuthority(com.tenant.common.constant.TenantConstants.ROLE_USER));
                    }

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    username,
                                    null,
                                    authorities
                            );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } else {
                // 没有有效令牌时，从请求头获取租户ID
                String headerTenantId = request.getHeader(TENANT_HEADER_NAME);
                if (headerTenantId != null && !headerTenantId.trim().isEmpty()) {
                    // 检查租户ID来源是否由网关验证
                    String verified = request.getHeader(TENANT_VERIFIED_HEADER);
                    if (!"true".equals(verified)) {
                        log.warn("安全警告：未认证请求的租户ID未经JWT验证，可能绕过了网关。tenantId={}, path={}",
                                headerTenantId, request.getRequestURI());
                    }
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
