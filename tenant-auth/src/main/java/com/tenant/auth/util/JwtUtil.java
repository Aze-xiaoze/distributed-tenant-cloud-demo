package com.tenant.auth.util;

import com.tenant.auth.config.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * JWT工具类
 * 提供JWT令牌的生成、解析和验证功能，支持在令牌中携带租户ID和角色列表
 * <p>算法：HS512（HMAC-SHA512），密钥至少需64字节
 * <p>令牌结构：
 * <ul>
 *   <li>sub(Subject) — 用户名</li>
 *   <li>jti(JWT ID) — 唯一标识（UUID），用于Token吊销和防重放攻击</li>
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
     * 从令牌中获取JWT唯一标识（jti）
     * <p>jti用于Token吊销和防重放攻击
     *
     * @param token 令牌
     * @return JWT唯一标识
     */
    public String getJtiFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getId();
    }

    /**
     * 从令牌中获取签发时间（毫秒时间戳）
     * <p>用于用户全量Token吊销时判断Token是否在吊销时间点之前签发
     *
     * @param token 令牌
     * @return 签发时间的毫秒时间戳
     */
    public long getIssuedAtMillisFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        Date issuedAt = claims.getIssuedAt();
        return issuedAt != null ? issuedAt.getTime() : 0;
    }

    /**
     * 从令牌中获取角色列表
     *
     * @param token 令牌
     * @return 角色编码列表
     */
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("roles", List.class);
    }

    /**
     * 从令牌中获取权限标识列表
     *
     * @param token 令牌
     * @return 权限标识列表
     */
    @SuppressWarnings("unchecked")
    public List<String> getPermissionsFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("permissions", List.class);
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
     * 生成令牌（包含用户名、租户ID、角色和权限标识）
     * <p>jti（JWT ID）使用UUID生成，用于Token吊销和防重放攻击
     *
     * @param username    用户名
     * @param tenantId    租户ID
     * @param roles       角色编码列表
     * @param permissions 权限标识列表
     * @return 令牌
     */
    public String generateToken(String username, String tenantId, List<String> roles, List<String> permissions) {
        Date createdDate = new Date();
        Date expirationDate = new Date(createdDate.getTime() + jwtProperties.getExpiration());

        return Jwts.builder()
                .setSubject(username)
                .setId(UUID.randomUUID().toString()) // jti：唯一标识，用于Token吊销
                .claim("tenantId", tenantId)
                .claim("roles", roles != null ? roles : List.of())
                .claim("permissions", permissions != null ? permissions : List.of())
                .setIssuedAt(createdDate)
                .setExpiration(expirationDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 生成令牌（兼容旧接口，不含角色权限）
     *
     * @param username 用户名
     * @param tenantId 租户ID
     * @return 令牌
     */
    public String generateToken(String username, String tenantId) {
        return generateToken(username, tenantId, List.of(), List.of());
    }

    /**
     * 生成RefreshToken（刷新令牌）
     * <p>RefreshToken仅携带用户名和租户ID，不携带角色权限，有效期较长
     * <p>通过tokenType声明区分AccessToken和RefreshToken，防止RefreshToken被当作AccessToken使用
     *
     * @param username 用户名
     * @param tenantId 租户ID
     * @return 刷新令牌
     */
    public String generateRefreshToken(String username, String tenantId) {
        Date createdDate = new Date();
        Date expirationDate = new Date(createdDate.getTime() + jwtProperties.getRefreshExpiration());

        return Jwts.builder()
                .setSubject(username)
                .setId(UUID.randomUUID().toString())
                .claim("tenantId", tenantId)
                .claim("tokenType", "refresh") // 标记为RefreshToken，防止被当作AccessToken使用
                .setIssuedAt(createdDate)
                .setExpiration(expirationDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 判断令牌是否为RefreshToken
     *
     * @param token 令牌
     * @return true=RefreshToken，false=AccessToken
     */
    public boolean isRefreshToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return "refresh".equals(claims.get("tokenType", String.class));
        } catch (Exception e) {
            return false;
        }
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