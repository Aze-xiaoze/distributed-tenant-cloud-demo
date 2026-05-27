package com.tenant.core.tenant;

/**
 * 多租户上下文持有者
 * 基于ThreadLocal在线程维度存储当前租户ID，实现租户信息的线程级隔离
 * <p>生命周期：请求进入时由Filter设置 → 业务代码中读取 → 请求结束时由Filter清理
 * <p>使用场景：
 * <ul>
 *   <li>MyBatis-Plus {@link com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler} 读取当前租户ID自动拼接SQL</li>
 *   <li>业务代码中需要获取当前租户ID时调用 {@link #getCurrentTenantId()}</li>
 * </ul>
 * <p><b>重要：必须在请求结束时调用 {@link #clear()} 防止ThreadLocal内存泄漏</b>
 *
 * @author Aze
 */
public class TenantContextHolder {

    /**
     * 使用ThreadLocal存储当前线程的租户ID
     */
    private static final ThreadLocal<String> TENANT_CONTEXT = new ThreadLocal<>();

    /**
     * 获取当前租户ID
     *
     * @return 租户ID
     */
    public static String getCurrentTenantId() {
        return TENANT_CONTEXT.get();
    }

    /**
     * 设置当前租户ID
     *
     * @param tenantId 租户ID
     */
    public static void setCurrentTenantId(String tenantId) {
        TENANT_CONTEXT.set(tenantId);
    }

    /**
     * 清除当前租户上下文
     * 防止内存泄漏
     */
    public static void clear() {
        TENANT_CONTEXT.remove();
    }
}