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

import java.util.UUID;

/**
 * 网关 TraceId 生成与透传过滤器
 * <p>为每个请求生成/维护唯一的链路追踪 ID（X-Request-Id），实现跨服务日志关联：
 * <ul>
 *   <li>若请求已携带 X-Request-Id，则透传该值（保证上下游链路一致）</li>
 *   <li>若请求未携带 X-Request-Id，则生成 UUID 作为链路 ID</li>
 *   <li>将 X-Request-Id 通过 HTTP Header 透传到下游微服务</li>
 * </ul>
 * <p>下游 Servlet 服务通过 {@link com.tenant.core.filter.MdcFilter} 将 X-Request-Id 写入 SLF4J MDC，
 * 日志中通过 {@code %X{requestId}} 输出，实现全链路日志追踪
 * <p>优先级：最高（在所有其他过滤器之前执行，确保后续过滤器日志都能关联到 TraceId）
 *
 * @author Aze
 */
@Component
public class TraceIdGatewayFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(TraceIdGatewayFilter.class);

    /**
     * 链路追踪 ID 请求头名称
     */
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // 优先使用客户端传入的 TraceId，保证链路连续性
        String requestId = request.getHeaders().getFirst(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isEmpty()) {
            requestId = generateRequestId();
            log.debug("生成链路追踪 ID: {}", requestId);
        }

        // 将 TraceId 放入 exchange 属性，供本服务内其他组件使用
        exchange.getAttributes().put(REQUEST_ID_HEADER, requestId);

        // 将 TraceId 通过 Header 透传到下游服务
        ServerHttpRequest mutatedRequest = request.mutate()
                .header(REQUEST_ID_HEADER, requestId)
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    @Override
    public int getOrder() {
        // 最高优先级，确保在其他所有过滤器之前执行
        return Ordered.HIGHEST_PRECEDENCE;
    }

    /**
     * 生成请求唯一标识
     *
     * @return 去掉横线的 UUID 字符串（更紧凑）
     */
    private String generateRequestId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
