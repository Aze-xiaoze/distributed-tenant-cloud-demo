package com.tenant.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 网关JWT认证过滤器
 * 验证请求中的JWT令牌，提取用户名和租户ID并传递给下游服务
 * <p>处理流程：
 * <ol>
 *   <li>白名单路径（登录/注册/健康检查）直接放行</li>
 *   <li>提取Authorization头中的Bearer Token</li>
 *   <li>解析JWT令牌，提取用户名(subject)和租户ID(tenantId Claim)</li>
 *   <li>将用户名和租户ID添加到请求头（X-User-Name、X-Tenant-ID），传递给下游服务</li>
 * </ol>
 * <p>优先级：{@link Ordered#HIGHEST_PRECEDENCE + 1}（在租户过滤器之后执行）
 * <p>JWT解析失败返回401 JSON响应
 *
 * @author Aze
 */
@Component
public class AuthGatewayFilter implements GlobalFilter, Ordered {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String USER_HEADER = "X-User-Name";
    private static final String TENANT_HEADER = "X-Tenant-ID";

    /**
     * 不需要认证的路径列表
     */
    private static final List<String> WHITE_LIST = List.of(
            "/auth/login",
            "/auth/register",
            "/auth/validate-token",
            "/actuator"
    );

    @Value("${jwt.secret:defaultSecretKeyForTenantCloudPlatformThatIsAtLeast64BytesLongForHS512}")
    private String secret;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // 白名单路径直接放行
        if (isWhiteListed(path)) {
            return chain.filter(exchange);
        }

        // 提取Authorization头
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return unauthorizedResponse(exchange);
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        try {
            // 解析JWT令牌
            SecretKey key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                    secret.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String username = claims.getSubject();
            String tenantId = claims.get("tenantId", String.class);

            // 将用户名和租户ID添加到请求头，传递给下游服务
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header(USER_HEADER, username)
                    .header(TENANT_HEADER, tenantId != null ? tenantId : "default_tenant")
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (Exception e) {
            return unauthorizedResponse(exchange);
        }
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
        // 在租户过滤器之后执行
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
