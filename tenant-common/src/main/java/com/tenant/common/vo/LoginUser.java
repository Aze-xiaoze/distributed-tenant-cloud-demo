package com.tenant.common.vo;

import lombok.Data;

import java.util.Set;

/**
 * 当前登录用户信息 DTO
 * <p>由 {@link com.tenant.core.resolver.CurrentUserArgumentResolver} 自动填充，
 * Controller 方法参数标注 {@link com.tenant.common.annotation.CurrentUser} 即可注入
 * <p>数据来源：Spring Security Authentication + TenantContextHolder
 *
 * @author Aze
 */
@Data
public class LoginUser {

    /**
     * 用户名（来自 JWT subject / Authentication principal）
     */
    private String username;

    /**
     * 所属租户ID（来自 TenantContextHolder）
     */
    private String tenantId;

    /**
     * 用户角色标识集合（来自 Authentication authorities，已去除 ROLE_ 前缀）
     */
    private Set<String> roles;

    /**
     * 用户权限标识集合（来自 Authentication authorities，非 ROLE_ 开头的）
     */
    private Set<String> permissions;

    /**
     * 是否已认证
     */
    private boolean authenticated;
}