package com.tenant.core.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * RefreshToken服务
 * 基于Redis管理RefreshToken的生命周期，支持令牌刷新和吊销
 * <p>存储结构：
 * <ul>
 *   <li>用户→RefreshToken映射：{@code refresh:user:{username}} → refreshToken值，TTL=RefreshToken有效期</li>
 *   <li>RefreshToken→用户映射：{@code refresh:token:{refreshToken}} → username，TTL=RefreshToken有效期</li>
 * </ul>
 * <p>双映射设计：既能通过用户名查找其RefreshToken（用于强制吊销），也能通过RefreshToken反查用户（用于刷新时验证）
 * <p>同一用户同一时刻只允许一个有效RefreshToken，新登录会自动替换旧的
 *
 * @author Aze
 */
@Component
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    /**
     * 用户→RefreshToken映射Key前缀
     */
    private static final String USER_REFRESH_KEY = "refresh:user:";

    /**
     * RefreshToken→用户映射Key前缀
     */
    private static final String TOKEN_REFRESH_KEY = "refresh:token:";

    private final StringRedisTemplate redisTemplate;

    public RefreshTokenService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 存储RefreshToken
     * <p>同一用户只保留最新的RefreshToken，旧的自动覆盖失效
     *
     * @param username        用户名
     * @param refreshToken    刷新令牌值（JWT字符串）
     * @param refreshTtlMs    刷新令牌有效期（毫秒）
     */
    public void storeRefreshToken(String username, String refreshToken, long refreshTtlMs) {
        long ttlSeconds = Math.max(refreshTtlMs / 1000, 1);

        // 存储用户→RefreshToken映射
        redisTemplate.opsForValue().set(USER_REFRESH_KEY + username, refreshToken, ttlSeconds, TimeUnit.SECONDS);
        // 存储RefreshToken→用户映射
        redisTemplate.opsForValue().set(TOKEN_REFRESH_KEY + refreshToken, username, ttlSeconds, TimeUnit.SECONDS);

        log.info("RefreshToken已存储：username={}, ttl={}s", username, ttlSeconds);
    }

    /**
     * 验证RefreshToken是否有效
     * <p>验证逻辑：RefreshToken→用户映射存在且对应的用户→RefreshToken映射值一致
     *
     * @param refreshToken 刷新令牌
     * @return 用户名（验证通过），null表示无效
     */
    public String validateRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            return null;
        }

        // 从RefreshToken→用户映射中获取用户名
        String username = redisTemplate.opsForValue().get(TOKEN_REFRESH_KEY + refreshToken);
        if (username == null) {
            log.debug("RefreshToken无效或已过期");
            return null;
        }

        // 验证用户→RefreshToken映射是否一致（防止旧Token仍有效）
        String storedToken = redisTemplate.opsForValue().get(USER_REFRESH_KEY + username);
        if (!refreshToken.equals(storedToken)) {
            log.debug("RefreshToken已被替换，旧Token无效：username={}", username);
            return null;
        }

        return username;
    }

    /**
     * 吊销指定用户的RefreshToken（用户注销时调用）
     *
     * @param username 用户名
     */
    public void revokeRefreshToken(String username) {
        // 先获取当前RefreshToken，再删除双映射
        String refreshToken = redisTemplate.opsForValue().get(USER_REFRESH_KEY + username);
        if (refreshToken != null) {
            redisTemplate.delete(TOKEN_REFRESH_KEY + refreshToken);
        }
        redisTemplate.delete(USER_REFRESH_KEY + username);
        log.info("RefreshToken已吊销：username={}", username);
    }

    /**
     * 吊销指定的RefreshToken
     *
     * @param refreshToken 刷新令牌
     */
    public void revokeByRefreshToken(String refreshToken) {
        String username = redisTemplate.opsForValue().get(TOKEN_REFRESH_KEY + refreshToken);
        if (username != null) {
            redisTemplate.delete(USER_REFRESH_KEY + username);
        }
        redisTemplate.delete(TOKEN_REFRESH_KEY + refreshToken);
        log.info("RefreshToken已吊销：username={}", username);
    }
}