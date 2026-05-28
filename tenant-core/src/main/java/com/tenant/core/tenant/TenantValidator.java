package com.tenant.core.tenant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 租户校验服务
 * 提供租户过期校验和配额校验功能
 * <p>校验维度：
 * <ul>
 *   <li>租户过期校验：检查租户是否已过期，过期后禁止登录和操作</li>
 *   <li>租户配额校验：检查租户用户数是否已达上限，超出后禁止注册新用户</li>
 *   <li>租户状态校验：检查租户是否被禁用</li>
 * </ul>
 * <p>Redis缓存结构：
 * <ul>
 *   <li>租户过期缓存：{@code tenant:expired:{tenantId}} → "1"，TTL=1小时（减少DB查询）</li>
 *   <li>租户用户数缓存：{@code tenant:user:count:{tenantId}} → 数量，TTL=5分钟</li>
 * </ul>
 *
 * @author Aze
 */
@Component
public class TenantValidator {

    private static final Logger log = LoggerFactory.getLogger(TenantValidator.class);

    private static final String TENANT_EXPIRED_PREFIX = com.tenant.common.constant.TenantConstants.TENANT_EXPIRED_PREFIX;
    private static final String TENANT_USER_COUNT_PREFIX = com.tenant.common.constant.TenantConstants.TENANT_USER_COUNT_PREFIX;
    private static final long EXPIRED_CACHE_TTL_HOURS = com.tenant.common.constant.TenantConstants.TENANT_EXPIRED_CACHE_TTL_HOURS;
    private static final long USER_COUNT_CACHE_TTL_MINUTES = com.tenant.common.constant.TenantConstants.TENANT_USER_COUNT_CACHE_TTL_MINUTES;

    private final StringRedisTemplate redisTemplate;

    private final TenantCacheUtil tenantCacheUtil;

    public TenantValidator(StringRedisTemplate redisTemplate, TenantCacheUtil tenantCacheUtil) {
        this.redisTemplate = redisTemplate;
        this.tenantCacheUtil = tenantCacheUtil;
    }

    /**
     * 校验租户是否可用（未过期、未禁用）
     *
     * @param tenantId   租户ID（tenantCode）
     * @param expireTime 租户过期时间（从数据库查询）
     * @param status     租户状态
     * @return 校验结果
     */
    public TenantValidationResult validateTenant(String tenantId, LocalDateTime expireTime, Integer status) {
        TenantValidationResult result = new TenantValidationResult();

        // 1. 检查租户状态
        if (status == null || status != 1) {
            result.setValid(false);
            result.setErrorMessage("租户已被禁用，请联系管理员");
            return result;
        }

        // 2. 检查租户是否过期
        if (isTenantExpired(tenantId, expireTime)) {
            result.setValid(false);
            result.setErrorMessage("租户已过期，请联系管理员续费");
            return result;
        }

        result.setValid(true);
        return result;
    }

    /**
     * 校验租户用户配额
     *
     * @param tenantId      租户ID
     * @param currentUserCount 当前用户数
     * @param maxUsers      最大用户数
     * @return 校验结果
     */
    public TenantValidationResult validateUserQuota(String tenantId, int currentUserCount, int maxUsers) {
        TenantValidationResult result = new TenantValidationResult();

        if (currentUserCount >= maxUsers) {
            result.setValid(false);
            result.setErrorMessage("租户用户数已达上限（" + maxUsers + "人），无法添加新用户");
            log.warn("租户用户配额已满：tenantId={}, current={}, max={}", tenantId, currentUserCount, maxUsers);
            return result;
        }

        result.setValid(true);
        return result;
    }

    /**
     * 检查租户是否过期
     * <p>优先从Redis缓存获取过期标记，缓存未命中则实时计算并更新缓存
     *
     * @param tenantId   租户ID
     * @param expireTime 过期时间
     * @return true=已过期
     */
    private boolean isTenantExpired(String tenantId, LocalDateTime expireTime) {
        // 无过期时间表示永不过期
        if (expireTime == null) {
            return false;
        }

        // 检查缓存
        String cachedExpired = redisTemplate.opsForValue().get(TENANT_EXPIRED_PREFIX + tenantId);
        if ("1".equals(cachedExpired)) {
            return true;
        }
        if ("0".equals(cachedExpired)) {
            return false;
        }

        // 实时计算
        boolean expired = expireTime.isBefore(LocalDateTime.now());

        // 更新缓存
        String value = expired ? "1" : "0";
        redisTemplate.opsForValue().set(TENANT_EXPIRED_PREFIX + tenantId, value,
                EXPIRED_CACHE_TTL_HOURS, TimeUnit.HOURS);

        return expired;
    }

    /**
     * 缓存租户用户数
     *
     * @param tenantId 租户ID
     * @param count    当前用户数
     */
    public void cacheUserCount(String tenantId, int count) {
        redisTemplate.opsForValue().set(TENANT_USER_COUNT_PREFIX + tenantId,
                String.valueOf(count), USER_COUNT_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
    }

    /**
     * 获取缓存的租户用户数
     *
     * @param tenantId 租户ID
     * @return 用户数，缓存不存在返回-1
     */
    public int getCachedUserCount(String tenantId) {
        String count = redisTemplate.opsForValue().get(TENANT_USER_COUNT_PREFIX + tenantId);
        return count != null ? Integer.parseInt(count) : -1;
    }

    /**
     * 清除租户过期缓存（租户续费后调用）
     *
     * @param tenantId 租户ID
     */
    public void clearExpiredCache(String tenantId) {
        if (tenantId == null || tenantId.isEmpty()) {
            log.warn("清除租户过期缓存失败：tenantId为空");
            return;
        }
        redisTemplate.delete(TENANT_EXPIRED_PREFIX + tenantId);
        log.info("已清除租户过期缓存: tenantId={}", tenantId);
    }

    /**
     * 租户校验结果
     */
    public static class TenantValidationResult {

        private boolean valid = true;

        private String errorMessage;

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }
}