package com.tenant.auth.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色权限查询Mapper
 * 在tenant-auth模块中查询用户的角色编码和权限标识，用于JWT令牌生成
 * <p>注意：sys_role、sys_user_role、sys_role_menu、sys_menu均为共享表（不进行租户过滤），
 * 本Mapper的SQL直接关联查询，无需考虑租户隔离
 *
 * @author Aze
 */
@Mapper
public interface RolePermissionMapper {

    /**
     * 根据用户ID查询角色编码列表
     *
     * @param userId 用户ID
     * @return 角色编码列表（如：SUPER_ADMIN, TENANT_ADMIN）
     */
    @Select("SELECT r.role_code FROM sys_role r " +
            "INNER JOIN sys_user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND r.status = 1 AND r.deleted = 0")
    List<String> selectRoleCodesByUserId(Long userId);

    /**
     * 根据用户ID查询权限标识列表
     * <p>通过 用户→角色→角色菜单→菜单 关联查询，只查类型为按钮(menu_type=3)的权限标识
     *
     * @param userId 用户ID
     * @return 权限标识列表（如：system:user:add, system:user:edit）
     */
    @Select("SELECT DISTINCT m.permission FROM sys_menu m " +
            "INNER JOIN sys_role_menu rm ON m.id = rm.menu_id " +
            "INNER JOIN sys_user_role ur ON rm.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND m.menu_type = 3 " +
            "AND m.status = 1 AND m.permission IS NOT NULL AND m.permission != ''")
    List<String> selectPermissionsByUserId(Long userId);
}