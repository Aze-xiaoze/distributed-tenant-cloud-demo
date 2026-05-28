package com.tenant.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 菜单实体类
 * 映射数据库 sys_menu 表，用于RBAC权限模型中的菜单和权限管理
 * <p>菜单类型：1-目录，2-菜单，3-按钮
 * <p>共享表，在 {@link com.tenant.core.config.MybatisPlusConfig} 中被标记为忽略租户过滤
 * <p>逻辑删除：{@link TableLogic} 标记 deleted 字段
 *
 * @author Aze
 */
@Data
@TableName("sys_menu")
public class Menu implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 父菜单ID（0为顶级）
     */
    private Long parentId;

    /**
     * 菜单名称
     */
    @NotBlank(message = "菜单名称不能为空")
    private String menuName;

    /**
     * 菜单类型（1-目录，2-菜单，3-按钮）
     */
    @NotNull(message = "菜单类型不能为空")
    private Integer menuType;

    /**
     * 路由路径
     */
    private String path;

    /**
     * 权限标识
     */
    private String permission;

    /**
     * 图标
     */
    private String icon;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 是否可见（1-可见，0-隐藏）
     */
    private Integer visible;

    /**
     * 状态（1-启用，0-禁用）
     */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
