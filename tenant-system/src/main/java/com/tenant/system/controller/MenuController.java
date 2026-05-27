package com.tenant.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tenant.common.vo.Result;
import com.tenant.system.entity.Menu;
import com.tenant.system.service.MenuService;
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
    public Result<Page<Menu>> page(@RequestParam(defaultValue = "1") Long current,
                                   @RequestParam(defaultValue = "10") Long size) {
        Page<Menu> page = new Page<>(current, size);
        Page<Menu> result = menuService.page(page);
        return Result.success(result);
    }

    /**
     * 根据ID获取菜单信息
     */
    @GetMapping("/{id}")
    public Result<Menu> getById(@PathVariable Long id) {
        Menu menu = menuService.getById(id);
        if (menu != null) {
            return Result.success(menu);
        }
        return Result.error("菜单不存在");
    }

    /**
     * 根据角色ID获取菜单列表
     */
    @GetMapping("/role/{roleId}")
    public Result<List<Menu>> listByRoleId(@PathVariable Long roleId) {
        List<Menu> menus = menuService.listByRoleId(roleId);
        return Result.success(menus);
    }

    /**
     * 新增菜单
     */
    @PostMapping
    public Result<String> add(@RequestBody Menu menu) {
        boolean success = menuService.save(menu);
        return success ? Result.success("新增成功") : Result.error("新增失败");
    }

    /**
     * 更新菜单
     */
    @PutMapping
    public Result<String> update(@RequestBody Menu menu) {
        boolean success = menuService.updateById(menu);
        return success ? Result.success("更新成功") : Result.error("更新失败");
    }

    /**
     * 删除菜单
     */
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        boolean success = menuService.removeById(id);
        return success ? Result.success("删除成功") : Result.error("删除失败");
    }
}
