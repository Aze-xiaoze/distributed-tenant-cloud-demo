package com.tenant.gateway.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * CORS 安全配置校验器
 * <p>在应用启动时校验跨域配置的安全性，防止危险的配置组合：
 * <ul>
 *   <li>{@code allowed-origins: *} 与 {@code allow-credentials: true} 组合会导致CSRF-like风险</li>
 *   <li>生产环境应使用明确的域名白名单</li>
 * </ul>
 * <p>发现问题时记录严重警告日志，提醒运维人员修正
 *
 * @author Aze
 */
@Component
public class CorsSecurityChecker {

    private static final Logger log = LoggerFactory.getLogger(CorsSecurityChecker.class);

    @Value("${spring.cloud.gateway.globalcors.cors-configurations.'[/**]'.allow-credentials:true}")
    private boolean allowCredentials;

    @Value("${CORS_ALLOWED_ORIGINS:http://localhost:3000,http://localhost:8080}")
    private String allowedOrigins;

    @PostConstruct
    public void check() {
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        boolean hasWildcard = origins.stream().anyMatch(o -> "*".equals(o.trim()));

        if (allowCredentials && hasWildcard) {
            log.error("""
                    
                    ============================================================
                    CORS 安全配置错误：allow-credentials=true 与 allowed-origins=* 不能同时使用！
                    此组合会导致跨域凭证泄露风险（CSRF-like攻击）。
                    请修改配置：
                      1. 生产环境：设置明确的 allowed-origins 域名白名单
                      2. 或关闭 allow-credentials（设为 false）
                    ============================================================""");
        } else if (hasWildcard) {
            log.warn("CORS 安全警告：allowed-origins 包含通配符 '*'，建议生产环境使用明确的域名白名单");
        }

        if (!hasWildcard) {
            for (String origin : origins) {
                String trimmed = origin.trim();
                if (!trimmed.startsWith("https://") && !trimmed.startsWith("http://")) {
                    log.warn("CORS 配置警告：allowed-origins 中的 '{}' 缺少协议前缀（http:// 或 https://）", trimmed);
                }
            }
        }
    }
}
