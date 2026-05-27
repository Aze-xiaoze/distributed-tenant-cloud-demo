package com.tenant.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tenant.system.entity.Menu;

import java.util.List;

/**
 * 菜单服务接口
 * <p>实现类：{@link com.tenant.system.service.impl.MenuServiceImpl}
 *
 * @author Aze
 */
public interface MenuService extends IService<Menu> {

    /**
     * 根据角色ID获取菜单列表
     *
     * @param roleId 角色ID
     * @return 菜单列表
     */
    List<Menu> listByRoleId(Long roleId);
}
