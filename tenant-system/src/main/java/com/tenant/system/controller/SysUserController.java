package com.tenant.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tenant.common.vo.Result;
import com.tenant.system.entity.SysUser;
import com.tenant.system.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 系统用户管理控制器
 * 提供用户分页查询、详情查询、状态更新等管理接口
 * <p>接口前缀：{@code /user}（经网关StripPrefix=1后映射为 {@code /system/user/**}）
 * <p>租户隔离：所有查询由MyBatis-Plus多租户插件自动过滤，仅返回当前租户的数据
 *
 * @author Aze
 */
@RestController
@RequestMapping("/user")
public class SysUserController {

    @Autowired
    private SysUserService sysUserService;

    /**
     * 分页查询用户列表
     * 租户过滤由MyBatis-Plus插件自动处理
     *
     * @param current 当前页
     * @param size    每页大小
     * @return 分页结果
     */
    @GetMapping("/page")
    public Result<Page<SysUser>> page(@RequestParam(defaultValue = "1") Long current,
                                      @RequestParam(defaultValue = "10") Long size) {
        Page<SysUser> page = new Page<>(current, size);
        Page<SysUser> result = sysUserService.page(page);
        return Result.success(result);
    }

    /**
     * 根据ID获取用户信息
     *
     * @param id 用户ID
     * @return 用户信息
     */
    @GetMapping("/{id}")
    public Result<SysUser> getById(@PathVariable Long id) {
        SysUser user = sysUserService.getById(id);
        if (user != null) {
            return Result.success(user);
        }
        return Result.error("用户不存在");
    }

    /**
     * 根据用户名获取用户信息
     *
     * @param username 用户名
     * @return 用户信息
     */
    @GetMapping("/username/{username}")
    public Result<Object> getByUsername(@PathVariable String username) {
        SysUser user = sysUserService.getUserByUsername(username);
        if (user != null) {
            return Result.success(user);
        }
        return Result.error("用户不存在");
    }

    /**
     * 更新用户状态
     *
     * @param id     用户ID
     * @param status 状态值
     * @return 操作结果
     */
    @PutMapping("/{id}/status")
    public Result<String> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setStatus(status);
        boolean success = sysUserService.updateById(user);
        return success ? Result.success("操作成功") : Result.error("操作失败");
    }
}
