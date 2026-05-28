package com.tenant.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.tenant.core.tenant.TenantEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统用户实体类（tenant-system 模块的视图）
 * 与 tenant-auth 的 User 实体映射同一张 sys_user 表，但密码字段标记为不可查询
 * <p>实现 {@link TenantEntity} 接口以支持多租户数据隔离
 * <p>deleted字段使用MyBatis-Plus {@link TableLogic} 逻辑删除，查询时自动追加 {@code WHERE deleted = 0}
 *
 * @author Aze
 */
@Data
@TableName("sys_user")
public class SysUser implements TenantEntity, Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 密码不在此视图中暴露
     */
    @TableField(select = false)
    private String password;

    private String nickname;

    private String email;

    private String phone;

    private String avatar;

    private Integer status;

    private String tenantId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
