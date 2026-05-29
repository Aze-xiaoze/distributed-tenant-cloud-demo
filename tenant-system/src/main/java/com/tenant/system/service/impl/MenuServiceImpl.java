package com.tenant.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tenant.system.entity.MenuEntity;
import com.tenant.system.entity.RoleMenuEntity;
import com.tenant.system.mapper.MenuMapper;
import com.tenant.system.mapper.RoleMenuMapper;
import com.tenant.system.service.MenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 菜单服务实现类
 *
 * @author Aze
 */
@Service
public class MenuServiceImpl extends ServiceImpl<MenuMapper, MenuEntity> implements MenuService {

    @Autowired
    private RoleMenuMapper roleMenuMapper;

    @Override
    public List<MenuEntity> listByRoleId(Long roleId) {
        // 先查角色-菜单关联，获取菜单ID列表
        LambdaQueryWrapper<RoleMenuEntity> rmWrapper = new LambdaQueryWrapper<>();
        rmWrapper.eq(RoleMenuEntity::getRoleId, roleId);
        List<Long> menuIds = roleMenuMapper.selectList(rmWrapper).stream()
                .map(RoleMenuEntity::getMenuId)
                .collect(Collectors.toList());

        if (menuIds.isEmpty()) {
            return List.of();
        }

        // 再查菜单详情
        LambdaQueryWrapper<MenuEntity> menuWrapper = new LambdaQueryWrapper<>();
        menuWrapper.in(MenuEntity::getId, menuIds).orderByAsc(MenuEntity::getSortOrder);
        return this.list(menuWrapper);
    }
}
