package com.tenant.core.tenant;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 租户缓存工具类
 * 基于Redis提供租户信息的缓存操作，减少对租户表的频繁查询
 * <p>缓存结构：
 * <ul>
 *   <li>租户名称缓存：{@code tenant:info:{tenantId}} → 租户名称，TTL=24小时</li>
 *   <li>租户状态缓存：{@code tenant:status:{tenantId}} → 状态值（1-启用/0-禁用），TTL=24小时</li>
 * </ul>
 *
 * @author Aze
 */
@Component
public class TenantCacheUtil {

    private static final String TENANT_KEY_PREFIX = "tenant:info:";
    private static final String TENANT_STATUS_PREFIX = "tenant:status:";
    private static final long TENANT_CACHE_TTL_HOURS = 24;

    private final StringRedisTemplate redisTemplate;

    public TenantCacheUtil(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 缓存租户ID与名称的映射
     *
     * @param tenantId   租户ID
     * @param tenantName 租户名称
     */
    public void cacheTenantName(String tenantId, String tenantName) {
        String key = TENANT_KEY_PREFIX + tenantId;
        redisTemplate.opsForValue().set(key, tenantName, TENANT_CACHE_TTL_HOURS, TimeUnit.HOURS);
    }

    /**
     * 获取缓存的租户名称
     *
     * @param tenantId 租户ID
     * @return 租户名称，不存在则返回null
     */
    public String getTenantName(String tenantId) {
        String key = TENANT_KEY_PREFIX + tenantId;
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 检查租户是否有效（存在且启用）
     *
     * @param tenantId 租户ID
     * @return 是否有效
     */
    public boolean isTenantValid(String tenantId) {
        String statusKey = TENANT_STATUS_PREFIX + tenantId;
        String status = redisTemplate.opsForValue().get(statusKey);
        return "1".equals(status);
    }

    /**
     * 设置租户状态缓存
     *
     * @param tenantId 租户ID
     * @param status   状态（1-启用，0-禁用）
     */
    public void setTenantStatus(String tenantId, String status) {
        String key = TENANT_STATUS_PREFIX + tenantId;
        redisTemplate.opsForValue().set(key, status, TENANT_CACHE_TTL_HOURS, TimeUnit.HOURS);
    }

    /**
     * 清除租户缓存
     *
     * @param tenantId 租户ID
     */
    public void evictTenantCache(String tenantId) {
        redisTemplate.delete(TENANT_KEY_PREFIX + tenantId);
        redisTemplate.delete(TENANT_STATUS_PREFIX + tenantId);
    }
}
