package com.tenant.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 网关租户识别过滤器
 * 从请求头中提取租户标识（X-Tenant-ID），确保租户信息在微服务间传递
 * <p>优先级：{@link Ordered#HIGHEST_PRECEDENCE}（最高优先级，在认证过滤器之前执行），
 * 确保即使未认证的请求（如登录/注册）也能携带租户信息
 * <p>若请求中无租户ID，自动填充默认值 "default_tenant"，保证下游服务不会因缺少租户上下文而异常
 *
 * @author Aze
 */
@Component
public class TenantGatewayFilter implements GlobalFilter, Ordered {

    private static final String TENANT_HEADER_NAME = "X-Tenant-ID";
    private static final String DEFAULT_TENANT_ID = "default_tenant";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // 从请求头中获取租户ID
        String tenantId = request.getHeaders().getFirst(TENANT_HEADER_NAME);

        // 如果没有租户ID，设置默认租户ID并添加到请求头
        if (tenantId == null || tenantId.trim().isEmpty()) {
            tenantId = DEFAULT_TENANT_ID;
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header(TENANT_HEADER_NAME, tenantId)
                    .build();
            exchange = exchange.mutate().request(mutatedRequest).build();
        }

        // 将租户ID放入交换属性中，供后续过滤器使用
        exchange.getAttributes().put("tenantId", tenantId);

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        // 在认证过滤器之前执行
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
