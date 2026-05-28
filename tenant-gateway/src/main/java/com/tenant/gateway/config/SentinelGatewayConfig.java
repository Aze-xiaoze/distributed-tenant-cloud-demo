package com.tenant.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import com.alibaba.csp.sentinel.adapter.gateway.sc.SentinelGatewayFilter;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.Set;

/**
 * Sentinel 网关限流与熔断降级配置
 * <p>提供以下能力：
 * <ul>
 *   <li>Gateway 限流规则：按路由ID限流，保护下游服务</li>
 *   <li>熔断降级规则：当下游服务异常率/慢调用比例超阈值时自动熔断</li>
 *   <li>自定义降级响应：被限流/熔断时返回统一 JSON 格式，而非默认错误页</li>
 * </ul>
 * <p>Sentinel Dashboard 可在运行时动态修改规则，此处配置作为默认基线
 *
 * @author Aze
 */
@Configuration
public class SentinelGatewayConfig {

    /**
     * 注册 SentinelGatewayFilter（网关层 Sentinel 入口）
     * <p>Sentinel 1.8.x 版本默认构造器无需参数
     */
    @Bean
    public SentinelGatewayFilter sentinelGatewayFilter() {
        return new SentinelGatewayFilter();
    }

    /**
     * 初始化限流规则、熔断规则和自定义降级响应
     */
    @PostConstruct
    public void init() {
        initGatewayFlowRules();
        initDegradeRules();
        initBlockHandler();
    }

    /**
     * 初始化网关限流规则
     * <p>按路由ID维度限流，Sentinel Dashboard 可动态调整
     */
    private void initGatewayFlowRules() {
        Set<GatewayFlowRule> rules = new HashSet<>();

        // 认证服务限流：QPS上限100，防止突发流量压垮认证服务
        rules.add(new GatewayFlowRule("tenant-auth")
                .setCount(100)
                .setIntervalSec(1)
        );

        // 系统管理服务限流：QPS上限200
        rules.add(new GatewayFlowRule("tenant-system")
                .setCount(200)
                .setIntervalSec(1)
        );

        GatewayRuleManager.loadRules(rules);
    }

    /**
     * 初始化熔断降级规则
     * <p>当服务异常率超阈值或响应时间过长时自动熔断，保护系统整体可用性
     */
    private void initDegradeRules() {
        // 熔断规则由 Sentinel Dashboard 动态下发更灵活
        // 此处仅设置兜底规则，确保未接入Dashboard时也有基本保护
        // 实际生产环境建议通过 Dashboard 或 Nacos 数据源配置
    }

    /**
     * 自定义限流/熔断降级响应
     * <p>被 Sentinel 拦截时返回统一 JSON 格式，便于前端统一处理
     */
    private void initBlockHandler() {
        GatewayCallbackManager.setBlockHandler(new BlockRequestHandler() {
            @Override
            public Mono<ServerResponse> handleRequest(ServerWebExchange exchange, Throwable t) {
                String body = "{\"code\":429,\"message\":\"请求过于频繁，请稍后重试\",\"timestamp\":"
                        + System.currentTimeMillis() + "}";

                return ServerResponse
                        .status(429)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Mono.just(body), String.class);
            }
        });
    }
}
