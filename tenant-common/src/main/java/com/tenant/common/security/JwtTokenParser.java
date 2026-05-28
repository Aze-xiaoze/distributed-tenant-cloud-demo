package com.tenant.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * JWT 令牌统一解析器
 * <p>供网关（Gateway）、认证服务（Auth）、业务服务（System）共享使用，
 * 消除各模块重复编写JJWT解析代码的问题。
 * <p>本类<b>仅负责解析和验证</b>，不处理Token生成（生成逻辑保留在Auth服务的JwtUtil中）
 * <p>无Spring依赖，可在任意模块中使用
 *
 * @author Aze
 */
public class JwtTokenParser {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenParser.class);

    private final SecretKey signingKey;

    /**
     * 构造解析器
     *
     * @param secret JWT签名密钥（至少64字节以满足HS512要求）
     */
    public JwtTokenParser(String secret) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 解析令牌，提取所有声明字段
     *
     * @param token JWT令牌
     * @return JwtTokenClaims 封装对象，解析失败返回null
     */
    public JwtTokenClaims parseToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            JwtTokenClaims result = new JwtTokenClaims();
            result.setUsername(claims.getSubject());
            result.setTenantId(claims.get("tenantId", String.class));
            result.setJti(claims.getId());
            result.setTokenType(claims.get("tokenType", String.class));
            result.setRoles(claims.get("roles", List.class));
            result.setPermissions(claims.get("permissions", List.class));

            Date issuedAt = claims.getIssuedAt();
            if (issuedAt != null) {
                result.setIssuedAtMillis(issuedAt.getTime());
            }

            Date expiration = claims.getExpiration();
            if (expiration != null) {
                result.setExpirationMillis(expiration.getTime());
            }

            return result;
        } catch (Exception e) {
            log.debug("JWT解析失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 验证令牌有效性（签名正确 + 未过期 + 用户名匹配）
     *
     * @param token    JWT令牌
     * @param username 期望的用户名
     * @return true=有效
     */
    public boolean validateToken(String token, String username) {
        JwtTokenClaims claims = parseToken(token);
        if (claims == null) {
            return false;
        }
        if (claims.getUsername() == null || !claims.getUsername().equals(username)) {
            return false;
        }
        return !isTokenExpired(token);
    }

    /**
     * 验证令牌是否已过期
     *
     * @param token JWT令牌
     * @return true=已过期或解析失败
     */
    public boolean isTokenExpired(String token) {
        JwtTokenClaims claims = parseToken(token);
        if (claims == null || claims.getExpirationMillis() == null) {
            return true;
        }
        return claims.getExpirationMillis() < System.currentTimeMillis();
    }

    /**
     * 判断令牌是否为RefreshToken
     *
     * @param token JWT令牌
     * @return true=RefreshToken
     */
    public boolean isRefreshToken(String token) {
        JwtTokenClaims claims = parseToken(token);
        if (claims == null) {
            return false;
        }
        return claims.isRefreshToken();
    }

    /**
     * 从令牌中提取用户名
     *
     * @param token JWT令牌
     * @return 用户名，解析失败返回null
     */
    public String getUsername(String token) {
        JwtTokenClaims claims = parseToken(token);
        return claims != null ? claims.getUsername() : null;
    }

    /**
     * 从令牌中提取租户ID
     *
     * @param token JWT令牌
     * @return 租户ID，解析失败返回null
     */
    public String getTenantId(String token) {
        JwtTokenClaims claims = parseToken(token);
        return claims != null ? claims.getTenantId() : null;
    }

    /**
     * 从令牌中提取JTI（唯一标识）
     *
     * @param token JWT令牌
     * @return JTI，解析失败返回null
     */
    public String getJti(String token) {
        JwtTokenClaims claims = parseToken(token);
        return claims != null ? claims.getJti() : null;
    }

    /**
     * 从令牌中提取签发时间（毫秒时间戳）
     *
     * @param token JWT令牌
     * @return 签发时间毫秒数，解析失败返回0
     */
    public long getIssuedAtMillis(String token) {
        JwtTokenClaims claims = parseToken(token);
        return claims != null && claims.getIssuedAtMillis() != null ? claims.getIssuedAtMillis() : 0;
    }

    /**
     * 从令牌中提取过期时间（毫秒时间戳）
     *
     * @param token JWT令牌
     * @return 过期时间毫秒数，解析失败返回0
     */
    public long getExpirationMillis(String token) {
        JwtTokenClaims claims = parseToken(token);
        return claims != null && claims.getExpirationMillis() != null ? claims.getExpirationMillis() : 0;
    }

    /**
     * 从令牌中提取角色列表
     *
     * @param token JWT令牌
     * @return 角色列表，解析失败返回null
     */
    @SuppressWarnings("unchecked")
    public List<String> getRoles(String token) {
        JwtTokenClaims claims = parseToken(token);
        return claims != null ? claims.getRoles() : null;
    }

    /**
     * 从令牌中提取权限标识列表
     *
     * @param token JWT令牌
     * @return 权限列表，解析失败返回null
     */
    @SuppressWarnings("unchecked")
    public List<String> getPermissions(String token) {
        JwtTokenClaims claims = parseToken(token);
        return claims != null ? claims.getPermissions() : null;
    }
}
