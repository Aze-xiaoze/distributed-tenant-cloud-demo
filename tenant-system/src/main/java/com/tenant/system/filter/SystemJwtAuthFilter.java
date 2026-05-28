package com.tenant.system.filter;

import com.tenant.common.security.JwtTokenClaims;
import com.tenant.common.security.JwtTokenParser;
import com.tenant.core.tenant.TenantContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
 * tenant-system 服务的JWT认证过滤器
 * 解析请求中的JWT令牌，设置Spring Security上下文和租户上下文
 * <p>与tenant-auth的JwtAuthenticationFilter类似，但本过滤器直接使用JJWT API解析令牌（不依赖JwtUtil），
 * 因为tenant-system不依赖tenant-auth模块，仅共享JWT密钥配置
 * <p><b>重要</b>：finally块中清理 {@link TenantContextHolder}，防止ThreadLocal内存泄漏
 *
 * @author Aze
 */
@Component
public class SystemJwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(SystemJwtAuthFilter.class);

    private static final String AUTHORIZATION_HEADER = com.tenant.common.constant.TenantConstants.AUTHORIZATION_HEADER;
    private static final String BEARER_PREFIX = com.tenant.common.constant.TenantConstants.BEARER_PREFIX;
    private static final String TENANT_HEADER_NAME = com.tenant.common.constant.TenantConstants.X_TENANT_ID_HEADER;

    /**
     * 租户ID来源验证标记头（由网关AuthGatewayFilter设置）
     */
    private static final String TENANT_VERIFIED_HEADER = com.tenant.common.constant.TenantConstants.X_TENANT_VERIFIED_HEADER;

    @Value("${jwt.secret:defaultSecretKeyForTenantCloudPlatformThatIsAtLeast64BytesLongForHS512}")
    private String secret;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = extractToken(request);
            JwtTokenParser tokenParser = new JwtTokenParser(secret);

            if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                JwtTokenClaims claims = tokenParser.parseToken(token);

                if (claims != null && claims.getUsername() != null) {
                    String username = claims.getUsername();
                    String tenantId = claims.getTenantId();

                    // 设置租户上下文（JWT中的租户ID为可信来源）
                    if (tenantId != null) {
                        TenantContextHolder.setCurrentTenantId(tenantId);
                    } else {
                        // JWT中无租户ID，从请求头获取并检查来源
                        String headerTenantId = request.getHeader(TENANT_HEADER_NAME);
                        if (headerTenantId != null && !headerTenantId.trim().isEmpty()) {
                            String verified = request.getHeader(TENANT_VERIFIED_HEADER);
                            if (!"true".equals(verified)) {
                                log.warn("安全警告：JWT中无租户ID，且请求头租户ID未经验证。tenantId={}, path={}",
                                        headerTenantId, request.getRequestURI());
                            }
                            TenantContextHolder.setCurrentTenantId(headerTenantId);
                        }
                    }

                    // 设置Security上下文（从JWT提取角色和权限）
                    List<String> roles = claims.getRoles();
                    List<String> permissions = claims.getPermissions();
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
                                    username, null,
                                    authorities
                            );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } else {
                // 从请求头获取租户ID（网关转发过来的）
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
        } catch (ServletException | IOException e) {
            // 异常由全局异常处理器处理，此处直接抛出
            throw e;
        }
        // 注意：TenantContextHolder.clear() 已由 TenantIdentificationFilter 统一清理，此处不再重复清理
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
