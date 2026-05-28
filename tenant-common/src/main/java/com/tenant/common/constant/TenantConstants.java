package com.tenant.common.constant;

/**
 * 多租户平台全局常量
 * <p>集中管理跨模块共享的常量，避免魔法数字和硬编码字符串分散在各处
 *
 * @author Aze
 */
public final class TenantConstants {

    private TenantConstants() {
        // 禁止实例化
    }

    // ======================== 租户相关 ========================

    /**
     * 默认租户ID
     */
    public static final String DEFAULT_TENANT_ID = "default_tenant";

    // ======================== HTTP请求头 ========================

    /**
     * 租户ID请求头名称
     */
    public static final String X_TENANT_ID_HEADER = "X-Tenant-ID";

    /**
     * 租户ID来源验证标记头
     */
    public static final String X_TENANT_VERIFIED_HEADER = "X-Tenant-Verified";

    /**
     * 用户名请求头（网关透传）
     */
    public static final String X_USER_NAME_HEADER = "X-User-Name";

    /**
     * 用户权限列表请求头（网关透传，逗号分隔）
     */
    public static final String X_USER_PERMISSIONS_HEADER = "X-User-Permissions";

    /**
     * 认证请求头
     */
    public static final String AUTHORIZATION_HEADER = "Authorization";

    /**
     * Bearer Token前缀
     */
    public static final String BEARER_PREFIX = "Bearer ";

    // ======================== 安全与认证 ========================

    /**
     * 超级管理员角色标识
     */
    public static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";

    /**
     * 默认角色（当JWT中无角色时赋予）
     */
    public static final String ROLE_USER = "ROLE_USER";

    // ======================== Redis Key前缀 ========================

    /**
     * Token黑名单Key前缀
     */
    public static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:jti:";

    /**
     * 用户全量Token吊销ZSet Key前缀
     */
    public static final String USER_BLACKLIST_PREFIX = "token:blacklist:user:";

    /**
     * 租户过期缓存Key前缀
     */
    public static final String TENANT_EXPIRED_PREFIX = "tenant:expired:";

    /**
     * 租户用户数缓存Key前缀
     */
    public static final String TENANT_USER_COUNT_PREFIX = "tenant:user:count:";

    // ======================== 日志与参数 ========================

    /**
     * 操作日志请求参数最大长度
     */
    public static final int MAX_LOG_PARAM_LENGTH = 4096;

    /**
     * 操作日志响应结果最大长度
     */
    public static final int MAX_LOG_RESULT_LENGTH = 4096;

    // ======================== 登录安全 ========================

    /**
     * 登录失败锁定时间（分钟）
     */
    public static final int LOGIN_LOCK_MINUTES = 15;

    /**
     * 触发账户锁定的连续失败次数
     */
    public static final int LOGIN_LOCK_THRESHOLD = 5;

    /**
     * IP封禁时间（分钟）
     */
    public static final int IP_BAN_MINUTES = 30;

    /**
     * 触发IP封禁的连续失败次数
     */
    public static final int IP_BAN_THRESHOLD = 10;

    // ======================== 租户校验缓存 ========================

    /**
     * 租户过期缓存TTL（小时）
     */
    public static final long TENANT_EXPIRED_CACHE_TTL_HOURS = 1;

    /**
     * 租户用户数缓存TTL（分钟）
     */
    public static final long TENANT_USER_COUNT_CACHE_TTL_MINUTES = 5;
}
