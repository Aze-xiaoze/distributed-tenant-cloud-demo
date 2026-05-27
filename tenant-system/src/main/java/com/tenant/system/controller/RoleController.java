package com.tenant.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tenant.common.vo.Result;
import com.tenant.system.entity.Role;
import com.tenant.system.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 角色管理控制器
 * 提供角色分页查询、详情查询、新增、更新、删除等CRUD接口
 * <p>接口前缀：{@code /role}（经网关StripPrefix=1后映射为 {@code /system/role/**}）
 * <p>租户隔离：角色的增删改查由MyBatis-Plus多租户插件自动过滤
 * <p><b>注意</b>：sys_role表在 {@link com.tenant.core.config.MybatisPlusConfig} 中被标记为忽略租户过滤，
 * 因为角色属于租户共享数据，具体取决于业务设计
 *
 * @author Aze
 */
@RestController
@RequestMapping("/role")
public class RoleController {

    @Autowired
    private RoleService roleService;

    /**
     * 分页查询角色列表
     *
     * @param current 当前页
     * @param size    每页大小
     * @return 分页结果
     */
    @GetMapping("/page")
    public Result<Page<Role>> page(@RequestParam(defaultValue = "1") Long current,
                                   @RequestParam(defaultValue = "10") Long size) {
        Page<Role> page = new Page<>(current, size);
        Page<Role> result = roleService.page(page);
        return Result.success(result);
    }

    /**
     * 根据ID获取角色信息
     *
     * @param id 角色ID
     * @return 角色信息
     */
    @GetMapping("/{id}")
    public Result<Role> getById(@PathVariable Long id) {
        Role role = roleService.getById(id);
        if (role != null) {
            return Result.success(role);
        }
        return Result.error("角色不存在");
    }

    /**
     * 新增角色
     *
     * @param role 角色信息
     * @return 操作结果
     */
    @PostMapping
    public Result<String> add(@RequestBody Role role) {
        boolean success = roleService.save(role);
        return success ? Result.success("新增成功") : Result.error("新增失败");
    }

    /**
     * 更新角色
     *
     * @param role 角色信息
     * @return 操作结果
     */
    @PutMapping
    public Result<String> update(@RequestBody Role role) {
        boolean success = roleService.updateById(role);
        return success ? Result.success("更新成功") : Result.error("更新失败");
    }

    /**
     * 删除角色
     *
     * @param id 角色ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        boolean success = roleService.removeById(id);
        return success ? Result.success("删除成功") : Result.error("删除失败");
    }
}
