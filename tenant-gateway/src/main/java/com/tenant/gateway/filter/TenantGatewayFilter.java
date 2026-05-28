package com.tenant.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * <p><b>安全设计</b>：本过滤器在{@link AuthGatewayFilter}之后执行（优先级更低），
 * 仅处理未经JWT验证的请求（白名单路径，如登录/注册）。对于已认证请求，
 * {@link AuthGatewayFilter}已从JWT Claims中设置X-Tenant-ID并标记X-Tenant-Verified=true，
 * 本过滤器检测到该标记后跳过处理，避免覆盖JWT验证过的租户ID
 * <p>若请求中无租户ID且未经过JWT验证，自动填充默认值 "default_tenant"，
 * 保证下游服务不会因缺少租户上下文而异常
 *
 * @author Aze
 */
@Component
public class TenantGatewayFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(TenantGatewayFilter.class);

    private static final String TENANT_HEADER_NAME = com.tenant.common.constant.TenantConstants.X_TENANT_ID_HEADER;

    /**
     * 租户ID来源验证标记头（与{@link AuthGatewayFilter}中定义一致）
     */
    private static final String TENANT_VERIFIED_HEADER = com.tenant.common.constant.TenantConstants.X_TENANT_VERIFIED_HEADER;
    private static final String DEFAULT_TENANT_ID = com.tenant.common.constant.TenantConstants.DEFAULT_TENANT_ID;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // 安全检查：如果租户ID已由AuthGatewayFilter从JWT中验证并设置，则跳过处理
        // 防止本过滤器用客户端请求头覆盖JWT验证过的租户ID
        String verified = request.getHeaders().getFirst(TENANT_VERIFIED_HEADER);
        if ("true".equals(verified)) {
            String tenantId = request.getHeaders().getFirst(TENANT_HEADER_NAME);
            exchange.getAttributes().put("tenantId", tenantId);
            log.debug("租户ID已由JWT验证：tenantId={}", tenantId);
            return chain.filter(exchange);
        }

        // 未认证请求（白名单路径）：从客户端请求头获取租户ID
        String tenantId = request.getHeaders().getFirst(TENANT_HEADER_NAME);

        // 如果没有租户ID，设置默认租户ID
        if (tenantId == null || tenantId.trim().isEmpty()) {
            tenantId = DEFAULT_TENANT_ID;
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header(TENANT_HEADER_NAME, tenantId)
                    .build();
            exchange = exchange.mutate().request(mutatedRequest).build();
        }

        // 将租户ID放入交换属性中，供后续过滤器使用
        exchange.getAttributes().put("tenantId", tenantId);
        log.debug("租户识别完成（未验证）：tenantId={}", tenantId);

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        // 在认证过滤器之后执行（AuthGatewayFilter=HIGHEST_PRECEDENCE，本过滤器=HIGHEST_PRECEDENCE+1）
        // 确保已认证请求的租户ID不被客户端请求头覆盖
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
