package com.tenant.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

/**
 * Swagger/Knife4j 文档访问控制过滤器
 * <p>生产环境安全策略：
 * <ul>
 *   <li>通过 {@code swagger.enabled} 配置开关（默认开启，生产环境建议关闭）</li>
 *   <li>通过 {@code swagger.allowed-ips} 配置白名单 IP（逗号分隔，空则不限制）</li>
 * </ul>
 * <p>适用路径：{@code /doc.html}、{@code /webjars/**}、{@code /v3/api-docs/**}、{@code /swagger-resources/**}
 *
 * @author Aze
 */
@Component
public class SwaggerAccessFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(SwaggerAccessFilter.class);

    /**
     * Swagger 文档访问路径前缀
     */
    private static final List<String> SWAGGER_PATHS = List.of(
            "/doc.html",
            "/webjars/",
            "/v3/api-docs",
            "/swagger-resources"
    );

    @Value("${swagger.enabled:true}")
    private boolean swaggerEnabled;

    @Value("${swagger.allowed-ips:}")
    private String allowedIps;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Swagger 未开启或非文档路径，直接放行
        if (!swaggerEnabled) {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getPath().value();
            if (isSwaggerPath(path)) {
                log.warn("Swagger文档已禁用，拒绝访问：path={}, ip={}", path, getClientIp(exchange));
                return forbiddenResponse(exchange);
            }
            return chain.filter(exchange);
        }

        // 白名单 IP 校验
        if (allowedIps != null && !allowedIps.isEmpty()) {
            String clientIp = getClientIp(exchange);
            List<String> ipList = Arrays.asList(allowedIps.split(","));
            if (!ipList.contains(clientIp)) {
                ServerHttpRequest request = exchange.getRequest();
                String path = request.getPath().value();
                if (isSwaggerPath(path)) {
                    log.warn("Swagger文档IP白名单拦截：path={}, ip={}", path, clientIp);
                    return forbiddenResponse(exchange);
                }
            }
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        // 在 Auth 过滤器之后执行（Auth=HIGHEST_PRECEDENCE+1）
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }

    /**
     * 判断是否为 Swagger 文档路径
     */
    private boolean isSwaggerPath(String path) {
        return SWAGGER_PATHS.stream().anyMatch(path::startsWith);
    }

    /**
     * 获取客户端真实 IP
     */
    private String getClientIp(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        String ip = request.getHeaders().getFirst("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeaders().getFirst("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddress() != null ? request.getRemoteAddress().getAddress().getHostAddress() : "unknown";
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 返回 403 禁止访问响应
     */
    private Mono<Void> forbiddenResponse(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        String body = "{\"code\":403,\"message\":\"Swagger文档访问受限\",\"timestamp\":"
                + System.currentTimeMillis() + "}";
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }
}
