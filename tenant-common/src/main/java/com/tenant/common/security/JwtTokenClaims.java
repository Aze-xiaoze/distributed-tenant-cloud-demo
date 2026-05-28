package com.tenant.common.security;

import java.util.List;

/**
 * JWT 令牌声明封装对象
 * 统一封装从JWT中解析出的常用字段，避免各过滤器直接操作JJWT的Claims对象
 * <p>所有字段均可为空，调用方需自行判空处理
 *
 * @author Aze
 */
public class JwtTokenClaims {

    private String username;
    private String tenantId;
    private String jti;
    private String tokenType;
    private List<String> roles;
    private List<String> permissions;
    private Long issuedAtMillis;
    private Long expirationMillis;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getJti() {
        return jti;
    }

    public void setJti(String jti) {
        this.jti = jti;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }

    public Long getIssuedAtMillis() {
        return issuedAtMillis;
    }

    public void setIssuedAtMillis(Long issuedAtMillis) {
        this.issuedAtMillis = issuedAtMillis;
    }

    public Long getExpirationMillis() {
        return expirationMillis;
    }

    public void setExpirationMillis(Long expirationMillis) {
        this.expirationMillis = expirationMillis;
    }

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
