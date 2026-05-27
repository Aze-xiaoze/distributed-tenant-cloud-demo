package com.tenant.auth.util;

import com.tenant.auth.config.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT工具类
 * 提供JWT令牌的生成、解析和验证功能，支持在令牌中携带租户ID
 * <p>算法：HS512（HMAC-SHA512），密钥至少需64字节
 * <p>令牌结构：
 * <ul>
 *   <li>sub(Subject) — 用户名</li>
 *   <li>tenantId(Custom Claim) — 租户ID</li>
 *   <li>iat(Issued At) — 签发时间</li>
 *   <li>exp(Expiration) — 过期时间</li>
 * </ul>
 *
 * @author Aze
 */
@Component
public class JwtUtil {

    private final JwtProperties jwtProperties;

    public JwtUtil(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    /**
     * 获取签名密钥
     * 确保密钥长度满足HS512的要求（至少64字节）
     *
     * @return SecretKey 实例
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 从令牌中获取用户名
     *
     * @param token 令牌
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getSubject();
    }

    /**
     * 从令牌中获取租户ID
     *
     * @param token 令牌
     * @return 租户ID
     */
    public String getTenantIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("tenantId", String.class);
    }

    /**
     * 从令牌中获取过期日期
     *
     * @param token 令牌
     * @return 过期日期
     */
    public Date getExpirationDateFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getExpiration();
    }

    /**
     * 从令牌中获取声明信息
     *
     * @param token 令牌
     * @return 声明信息
     */
    private Claims getClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 判断令牌是否过期
     *
     * @param token 令牌
     * @return 是否过期
     */
    private boolean isTokenExpired(String token) {
        Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    /**
     * 生成令牌（包含用户名和租户ID）
     *
     * @param username 用户名
     * @param tenantId 租户ID
     * @return 令牌
     */
    public String generateToken(String username, String tenantId) {
        Claims claims = Jwts.claims().setSubject(username);
        claims.put("tenantId", tenantId);
        Date createdDate = new Date();
        Date expirationDate = new Date(createdDate.getTime() + jwtProperties.getExpiration());

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(createdDate)
                .setExpiration(expirationDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * 验证令牌
     *
     * @param token    令牌
     * @param username 用户名
     * @return 是否有效
     */
    public Boolean validateToken(String token, String username) {
        String tokenUsername = getUsernameFromToken(token);
        return tokenUsername.equals(username) && !isTokenExpired(token);
    }
}