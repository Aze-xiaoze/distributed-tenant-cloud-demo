package com.tenant.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tenant.core.tenant.TenantEntity;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体类
 * 映射数据库 sys_user 表，实现 {@link TenantEntity} 接口以支持多租户数据隔离
 * <p>租户过滤由MyBatis-Plus {@link com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor} 自动处理，
 * 无需在查询条件中手动添加 tenant_id
 *
 * @author Aze
 */
@Data
@TableName("sys_user")
public class User implements TenantEntity, Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 状态（1-启用，0-禁用）
     */
    private Integer status;

    /**
     * 租户ID
     */
    private String tenantId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}