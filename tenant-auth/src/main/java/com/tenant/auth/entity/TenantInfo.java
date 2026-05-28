package com.tenant.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 租户信息实体（auth服务专用，只读）
 * <p>仅包含登录校验所需的基本字段：租户编码、状态、过期时间、最大用户数
 * <p>完整的租户实体在tenant-system模块中
 *
 * @author Aze
 */
@Data
@TableName("tenants")
public class TenantInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 租户编码 */
    private String tenantCode;

    /** 状态（1-启用，0-禁用） */
    private Integer status;

    /** 过期时间 */
    private LocalDateTime expireTime;

    /** 最大用户数 */
    private Integer maxUsers;
}