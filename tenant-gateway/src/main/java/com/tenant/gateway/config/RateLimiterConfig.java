package com.tenant.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 网关限流配置
 * 基于 Redis + 令牌桶算法实现请求限流
 * <p>限流维度：租户ID + 客户端IP（不同租户的不同IP独立计算限流）
 * <p>限流规则：
 * <ul>
 *   <li>认证服务（/auth/**）：每租户每IP 10次/秒，防止暴力破解</li>
 *   <li>系统管理服务（/system/**）：每租户每IP 20次/秒</li>
 * </ul>
 * <p>配置参数说明（application.yml中配置）：
 * <ul>
 *   <li>redis-rate-limiter.replenishRate — 令牌桶每秒填充速率</li>
 *   <li>redis-rate-limiter.burstCapacity — 令牌桶最大容量（允许瞬间突发）</li>
 *   <li>redis-rate-limiter.requestedTokens — 每次请求消耗的令牌数（默认1）</li>
 * </ul>
 *
 * @author Aze
 */
@Configuration
public class RateLimiterConfig {

    /**
     * 按租户ID + 客户端IP组合限流
     * <p>优先使用X-Tenant-ID请求头中的租户ID（网关已认证请求中该值由JWT保障），
     * 其次使用远程IP地址
     * <p>Key格式：{tenantId}:{ip}，例如 "tenant_001:192.168.1.100"
     */
    @Bean
    public KeyResolver tenantIpKeyResolver() {
        return exchange -> {
            String tenantId = exchange.getRequest().getHeaders().getFirst("X-Tenant-ID");
            String ip = getClientIp(exchange);
            String key = (tenantId != null ? tenantId : "anonymous") + ":" + (ip != null ? ip : "unknown");
            return Mono.just(key);
        };
    }

    /**
     * 按租户ID限流（适用于租户级总流量控制）
     * <p>Key格式：tenant:{tenantId}
     */
    @Bean
    public KeyResolver tenantKeyResolver() {
        return exchange -> {
            String tenantId = exchange.getRequest().getHeaders().getFirst("X-Tenant-ID");
            return Mono.just("tenant:" + (tenantId != null ? tenantId : "anonymous"));
        };
    }

    /**
     * 获取客户端真实IP
     */
    private String getClientIp(ServerWebExchange exchange) {
        String ip = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = exchange.getRequest().getHeaders().getFirst("X-Real-IP");
        }
        if (ip == null || ip.isEmpty()) {
            ip = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : null;
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}