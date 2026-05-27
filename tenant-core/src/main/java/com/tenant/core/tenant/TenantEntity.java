package com.tenant.core.tenant;

/**
 * 租户标识接口
 * 所有需要支持多租户数据隔离的实体类都应实现此接口
 * <p>实现此接口的实体类必须包含 {@code tenantId} 字段，
 * MyBatis-Plus多租户插件会自动在该实体对应的表中拼接 {@code WHERE tenant_id = ?} 条件
 * <p>此接口同时为实体类提供统一的租户ID访问契约
 *
 * @author Aze
 */
public interface TenantEntity {
    
    /**
     * 获取租户ID
     *
     * @return 租户ID
     */
    String getTenantId();

    /**
     * 设置租户ID
     *
     * @param tenantId 租户ID
     */
    void setTenantId(String tenantId);
}