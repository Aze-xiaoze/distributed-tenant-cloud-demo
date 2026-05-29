package com.tenant.gateway.filter;

import com.tenant.common.security.JwtTokenClaims;
import com.tenant.common.security.JwtTokenParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 网关JWT认证过滤器
 * 验证请求中的JWT令牌，提取用户名和租户ID并传递给下游服务
 * <p><b>安全核心</b>：从JWT Claims中提取租户ID，强制覆盖客户端可能伪造的X-Tenant-ID请求头，
 * 确保已认证请求的租户身份来源于JWT签名验证，而非客户端可篡改的HTTP头
 * <p>处理流程：
 * <ol>
 *   <li>白名单路径（登录/注册/健康检查）直接放行，不修改租户头</li>
 *   <li>提取Authorization头中的Bearer Token</li>
 *   <li>解析JWT令牌，提取用户名(subject)和租户ID(tenantId Claim)</li>
 *   <li><b>强制覆盖</b>X-Tenant-ID为JWT中的tenantId，设置X-Tenant-Verified=true</li>
 *   <li>将用户名添加到请求头（X-User-Name），传递给下游服务</li>
 * </ol>
 * <p>优先级：{@link Ordered#HIGHEST_PRECEDENCE}（最高优先级，在租户过滤器之前执行），
 * 确保已认证请求的租户ID由JWT签名保障，而非客户端请求头
 * <p>JWT解析失败返回401 JSON响应
 *
 * @author Aze
 */
@Component
public class AuthGatewayFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(AuthGatewayFilter.class);

    private static final String BEARER_PREFIX = com.tenant.common.constant.TenantConstants.BEARER_PREFIX;
    private static final String USER_HEADER = com.tenant.common.constant.TenantConstants.X_USER_NAME_HEADER;
    private static final String TENANT_HEADER = com.tenant.common.constant.TenantConstants.X_TENANT_ID_HEADER;

    /**
     * Token黑名单Key前缀（与TokenBlacklistService中定义一致）
     */
    private static final String TOKEN_BLACKLIST_PREFIX = com.tenant.common.constant.TenantConstants.TOKEN_BLACKLIST_PREFIX;

    /**
     * 租户ID来源验证标记头，下游服务据此判断租户ID是否经过JWT签名验证
     * <p>值为"true"表示租户ID来源于JWT Claims（可信），下游服务可安全使用
     * <p>值不存在表示租户ID来源于客户端请求头（不可信，仅限白名单路径使用）
     */
    private static final String TENANT_VERIFIED_HEADER = com.tenant.common.constant.TenantConstants.X_TENANT_VERIFIED_HEADER;

    /**
     * 不需要认证的路径列表
     * <p>白名单路径的租户ID由{@link TenantGatewayFilter}从请求头提取（如登录时需指定租户）
     */
    private static final List<String> WHITE_LIST = List.of(
            "/auth/login",
            "/auth/register",
            "/auth/validate-token",
            "/auth/refresh-token",
            "/auth/logout",
            "/actuator",
            "/doc.html",
            "/webjars/",
            "/v3/api-docs",
            "/swagger-resources",
            "/favicon.ico"
    );

    private final StringRedisTemplate redisTemplate;
    private final String jwtSecret;

    public AuthGatewayFilter(StringRedisTemplate redisTemplate,
                             @Value("${jwt.secret:defaultSecretKeyForTenantCloudPlatformThatIsAtLeast64BytesLongForHS512}") String jwtSecret) {
        this.redisTemplate = redisTemplate;
        this.jwtSecret = jwtSecret;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // 白名单路径直接放行，不修改租户头（由TenantGatewayFilter处理）
        if (isWhiteListed(path)) {
            return chain.filter(exchange);
        }

        // 提取Authorization头
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return unauthorizedResponse(exchange);
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        JwtTokenParser tokenParser = new JwtTokenParser(jwtSecret);
        JwtTokenClaims claims = tokenParser.parseToken(token);
        if (claims == null) {
            log.warn("JWT解析失败，path={}", path);
            return unauthorizedResponse(exchange);
        }

        String username = claims.getUsername();
        String tenantId = claims.getTenantId();
        String jti = claims.getJti();
        String verifiedTenantId = tenantId != null ? tenantId : "default_tenant";

        // 安全检查：禁止RefreshToken被当作AccessToken使用
        // RefreshToken仅能用于/auth/refresh-token接口，不能用于其他API鉴权
        if (claims.isRefreshToken()) {
            log.warn("RefreshToken不能用于API鉴权：jti={}, username={}", jti, username);
            return unauthorizedResponse(exchange);
        }

        // 从JWT中提取权限列表，透传给下游服务（供@RequiresPermission切面使用）
        List<String> permissions = claims.getPermissions();
        String permissionsStr = (permissions != null && !permissions.isEmpty())
                ? String.join(",", permissions) : "";

        // 检查Token是否已被吊销（黑名单检查）
        if (jti != null && Boolean.TRUE.equals(redisTemplate.hasKey(TOKEN_BLACKLIST_PREFIX + jti))) {
            log.warn("Token已被吊销：jti={}, username={}", jti, username);
            return unauthorizedResponse(exchange);
        }

        log.debug("JWT认证通过：username={}, tenantId={}", username, verifiedTenantId);

        // 关键安全修复：从JWT Claims中提取租户ID，强制覆盖客户端可能伪造的X-Tenant-ID
        // 使用headers.set()确保替换而非追加，防止客户端伪造的租户ID残留
        ServerHttpRequest mutatedRequest = request.mutate()
                .headers(headers -> {
                    headers.set(USER_HEADER, username);
                    headers.set(TENANT_HEADER, verifiedTenantId);
                    headers.set(TENANT_VERIFIED_HEADER, "true");
                    // 透传权限标识列表（逗号分隔），供下游@RequiresPermission切面使用
                    if (!permissionsStr.isEmpty()) {
                        headers.set("X-User-Permissions", permissionsStr);
                    }
                })
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    /**
     * 判断路径是否在白名单中
     */
    private boolean isWhiteListed(String path) {
        return WHITE_LIST.stream().anyMatch(path::startsWith);
    }

    /**
     * 返回401未认证响应
     */
    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        String body = "{\"code\":401,\"message\":\"未认证，请先登录\",\"timestamp\":"
                + System.currentTimeMillis() + "}";
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }

    @Override
    public int getOrder() {
        // 次高优先级，确保在 TraceId 过滤器之后执行（TraceId=HIGHEST_PRECEDENCE）
        // 这样认证日志中也能关联到 TraceId
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
