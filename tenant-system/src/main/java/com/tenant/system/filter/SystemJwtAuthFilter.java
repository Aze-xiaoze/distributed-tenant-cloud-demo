package com.tenant.system.filter;

import com.tenant.core.tenant.TenantContextHolder;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

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

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String TENANT_HEADER_NAME = "X-Tenant-ID";

    @Value("${jwt.secret:defaultSecretKeyForTenantCloudPlatformThatIsAtLeast64BytesLongForHS512}")
    private String secret;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = extractToken(request);

            if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(key)
                        .build()
                        .parseClaimsJws(token)
                        .getBody();

                String username = claims.getSubject();
                String tenantId = claims.get("tenantId", String.class);

                if (username != null) {
                    // 设置租户上下文
                    if (tenantId != null) {
                        TenantContextHolder.setCurrentTenantId(tenantId);
                    }

                    // 设置Security上下文
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    username, null,
                                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                            );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } else {
                // 从请求头获取租户ID（网关转发过来的）
                String headerTenantId = request.getHeader(TENANT_HEADER_NAME);
                if (headerTenantId != null && !headerTenantId.trim().isEmpty()) {
                    TenantContextHolder.setCurrentTenantId(headerTenantId);
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
