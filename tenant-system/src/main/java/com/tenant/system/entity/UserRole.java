package com.tenant.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.tenant.core.tenant.TenantEntity;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户角色关联实体类
 * 映射数据库 sys_user_role 表，用于RBAC权限模型中用户与角色的多对多关联
 * <p>实现 {@link TenantEntity} 接口，租户过滤由MyBatis-Plus插件自动处理
 *
 * @author Aze
 */
@Data
@TableName("sys_user_role")
public class UserRole implements TenantEntity, Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 角色ID
     */
    private Long roleId;

    /**
     * 租户ID
     */
    private String tenantId;

    private LocalDateTime createTime;
}
