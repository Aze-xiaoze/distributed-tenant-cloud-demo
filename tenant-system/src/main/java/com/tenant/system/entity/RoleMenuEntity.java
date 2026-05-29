package com.tenant.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 角色菜单关联实体类
 * 映射数据库 sys_role_menu 表，用于角色与菜单权限的多对多关联
 * <p>共享表，在 {@link com.tenant.core.config.MybatisPlusConfig} 中被标记为忽略租户过滤
 *
 * @author Aze
 */
@Data
@TableName("sys_role_menu")
public class RoleMenuEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 角色ID
     */
    private Long roleId;

    /**
     * 菜单ID
     */
    private Long menuId;

    private LocalDateTime createTime;
}
