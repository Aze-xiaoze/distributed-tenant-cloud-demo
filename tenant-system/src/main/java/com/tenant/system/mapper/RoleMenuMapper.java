package com.tenant.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tenant.system.entity.RoleMenu;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色菜单关联Mapper接口
 * <p>共享表，不进行租户过滤
 *
 * @author Aze
 */
@Mapper
public interface RoleMenuMapper extends BaseMapper<RoleMenu> {
}
