package com.tenant.common.security;

import lombok.Data;

import java.util.List;

/**
 * JWT 令牌声明封装对象
 * 统一封装从JWT中解析出的常用字段，避免各过滤器直接操作JJWT的Claims对象
 * <p>所有字段均可为空，调用方需自行判空处理
 *
 * @author Aze
 */
@Data
public class JwtTokenClaims {

    private String username;
    private String tenantId;
    private String jti;
    private String tokenType;
    private List<String> roles;
    private List<String> permissions;
    private Long issuedAtMillis;
    private Long expirationMillis;

    /**
     * 判断当前令牌是否为RefreshToken
     */
    public boolean isRefreshToken() {
        return "refresh".equals(tokenType);
    }

    /**
     * 获取令牌剩余有效时间（毫秒）
     *
     * @return 剩余毫秒数，已过期返回0
     */
    public long getRemainingMillis() {
        if (expirationMillis == null) {
            return 0;
        }
        long remaining = expirationMillis - System.currentTimeMillis();
        return Math.max(remaining, 0);
    }
}
