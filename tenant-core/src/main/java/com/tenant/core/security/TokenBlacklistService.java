package com.tenant.core.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Token黑名单服务
 * 基于Redis实现JWT令牌的吊销机制，支持用户注销和强制下线功能
 * <p>存储结构：
 * <ul>
 *   <li>单Token吊销：{@code token:blacklist:{jti}} → "1"，TTL=Token剩余有效期</li>
 *   <li>用户全量吊销：{@code token:blacklist:user:{username}:{issuedAt} } → "1"，TTL=最长Token有效期</li>
 * </ul>
 * <p>判断Token是否被吊销的逻辑：
 * <ol>
 *   <li>检查Token的jti是否在黑名单中</li>
 *   <li>检查用户在Token签发时间之后是否有全量吊销记录</li>
 * </ol>
 * <p><b>前置条件</b>：JWT生成时必须包含jti（唯一标识）和iat（签发时间）声明
 *
 * @author Aze
 */
@Component
public class TokenBlacklistService {

    private static final Logger log = LoggerFactory.getLogger(TokenBlacklistService.class);

    /**
     * 单Token黑名单Key前缀：token:blacklist:jti:{jti}
     */
    private static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:jti:";

    /**
     * 用户全量吊销Key前缀：token:blacklist:user:{username}:{issuedAt}
     * issuedAt用于区分不同批次的全量吊销
     */
    private static final String USER_BLACKLIST_PREFIX = "token:blacklist:user:";

    /**
     * 默认Token最大有效期（毫秒），用于设置用户全量吊销的TTL
     * 应与JWT过期时间保持一致
     */
    private static final long DEFAULT_TOKEN_MAX_TTL_MS = 3600000L;

    private final StringRedisTemplate redisTemplate;

    public TokenBlacklistService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 吊销单个Token
     * 将Token的jti加入黑名单，TTL设置为Token的剩余有效期
     *
     * @param jti           Token唯一标识
     * @param remainingMs   Token剩余有效期（毫秒）
     */
    public void revokeToken(String jti, long remainingMs) {
        if (jti == null || jti.isEmpty()) {
            log.warn("吊销Token失败：jti为空");
            return;
        }
        String key = TOKEN_BLACKLIST_PREFIX + jti;
        long ttl = Math.max(remainingMs / 1000, 1); // 转为秒，至少1秒
        redisTemplate.opsForValue().set(key, "1", ttl, TimeUnit.SECONDS);
        log.info("Token已吊销：jti={}, ttl={}s", jti, ttl);
    }

    /**
     * 吊销指定用户的所有Token（强制下线）
     * 通过记录用户的吊销时间点，使得在该时间点之前签发的所有Token失效
     *
     * @param username      用户名
     * @param issuedBefore  在此时间之前签发的Token全部失效（毫秒时间戳）
     * @param tokenMaxTtlMs Token最大有效期（毫秒），用于设置TTL
     */
    public void revokeAllUserTokens(String username, long issuedBefore, long tokenMaxTtlMs) {
        String key = USER_BLACKLIST_PREFIX + username + ":" + issuedBefore;
        long ttl = Math.max(tokenMaxTtlMs / 1000, 1);
        redisTemplate.opsForValue().set(key, "1", ttl, TimeUnit.SECONDS);
        log.info("用户全量Token已吊销：username={}, issuedBefore={}, ttl={}s", username, issuedBefore, ttl);
    }

    /**
     * 检查Token是否已被吊销
     * <p>检查逻辑：
     * <ol>
     *   <li>检查Token的jti是否在单Token黑名单中</li>
     *   <li>检查用户在Token签发时间之前是否有全量吊销记录</li>
     * </ol>
     *
     * @param jti      Token唯一标识
     * @param username 用户名
     * @param issuedAt Token签发时间（毫秒时间戳）
     * @return true=已被吊销，false=未吊销
     */
    public boolean isTokenRevoked(String jti, String username, long issuedAt) {
        // 1. 检查单Token黑名单
        if (jti != null && !jti.isEmpty()) {
            String tokenKey = TOKEN_BLACKLIST_PREFIX + jti;
            if (Boolean.TRUE.equals(redisTemplate.hasKey(tokenKey))) {
                log.debug("Token已被单Token黑名单吊销：jti={}", jti);
                return true;
            }
        }

        // 2. 检查用户全量吊销（需要扫描用户维度的黑名单Key）
        // 使用模式匹配查找是否存在 issuedAt 之前的全量吊销记录
        String pattern = USER_BLACKLIST_PREFIX + username + ":*";
        var keys = redisTemplate.keys(pattern);
        if (keys != null) {
            for (String key : keys) {
                // Key格式：token:blacklist:user:{username}:{issuedBeforeTimestamp}
                String[] parts = key.split(":");
                if (parts.length >= 5) {
                    try {
                        long issuedBefore = Long.parseLong(parts[4]);
                        // 如果Token签发时间早于全量吊销时间点，则该Token已被吊销
                        if (issuedAt < issuedBefore) {
                            log.debug("Token已被用户全量吊销：username={}, issuedAt={}, issuedBefore={}",
                                    username, issuedAt, issuedBefore);
                            return true;
                        }
                    } catch (NumberFormatException e) {
                        log.warn("解析用户黑名单Key失败：key={}", key);
                    }
                }
            }
        }

        return false;
    }

    /**
     * 检查Token是否已被吊销（简化版，仅检查单Token黑名单）
     *
     * @param jti Token唯一标识
     * @return true=已被吊销，false=未吊销
     */
    public boolean isTokenRevoked(String jti) {
        if (jti == null || jti.isEmpty()) {
            return false;
        }
        String key = TOKEN_BLACKLIST_PREFIX + jti;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
