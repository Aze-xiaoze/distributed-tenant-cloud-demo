package com.tenant.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tenant.common.vo.ResultVO;
import com.tenant.system.entity.MenuEntity;
import com.tenant.system.service.MenuService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜单管理控制器
 * 提供菜单分页查询、详情查询、新增、更新、删除、按角色查询菜单树等接口
 * <p>接口前缀：{@code /menu}（经网关StripPrefix=1后映射为 {@code /system/menu/**}）
 * <p>菜单表为共享表，不进行租户过滤
 *
 * @author Aze
 */
@RestController
@RequestMapping("/menu")
public class MenuController {

    @Autowired
    private MenuService menuService;

    /**
     * 分页查询菜单列表
     */
    @GetMapping("/page")
    public ResultVO<Page<MenuEntity>> page(@RequestParam(defaultValue = "1") Long current,
                                           @RequestParam(defaultValue = "10") Long size) {
        Page<MenuEntity> page = new Page<>(current, size);
        Page<MenuEntity> result = menuService.page(page);
        return ResultVO.success(result);
    }

    /**
     * 根据ID获取菜单信息
     */
    @GetMapping("/{id}")
    public ResultVO<MenuEntity> getById(@PathVariable Long id) {
        MenuEntity menuEntity = menuService.getById(id);
        if (menuEntity != null) {
            return ResultVO.success(menuEntity);
        }
        return ResultVO.error("菜单不存在");
    }

    /**
     * 根据角色ID获取菜单列表
     */
    @GetMapping("/role/{roleId}")
    public ResultVO<List<MenuEntity>> listByRoleId(@PathVariable Long roleId) {
        List<MenuEntity> menuEntities = menuService.listByRoleId(roleId);
        return ResultVO.success(menuEntities);
    }

    /**
     * 新增菜单
     */
    @PostMapping
    public ResultVO<String> add(@Valid @RequestBody MenuEntity menuEntity) {
        boolean success = menuService.save(menuEntity);
        return success ? ResultVO.success("新增成功") : ResultVO.error("新增失败");
    }

    /**
     * 更新菜单
     */
    @PutMapping
    public ResultVO<String> update(@Valid @RequestBody MenuEntity menuEntity) {
        boolean success = menuService.updateById(menuEntity);
        return success ? ResultVO.success("更新成功") : ResultVO.error("更新失败");
    }

    /**
     * 删除菜单
     */
    @DeleteMapping("/{id}")
    public ResultVO<String> delete(@PathVariable Long id) {
        boolean success = menuService.removeById(id);
        return success ? ResultVO.success("删除成功") : ResultVO.error("删除失败");
    }
}
