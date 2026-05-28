package com.tenant.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tenant.common.vo.Result;
import com.tenant.system.entity.Tenant;
import com.tenant.system.service.TenantService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 租户管理控制器
 * 提供租户分页查询、详情查询、新增、更新等管理接口
 * <p>接口前缀：{@code /tenant}（经网关StripPrefix=1后映射为 {@code /system/tenant/**}）
 * <p>租户表为共享表，不进行租户过滤
 *
 * @author Aze
 */
@RestController
@RequestMapping("/tenant")
public class TenantController {

    @Autowired
    private TenantService tenantService;

    /**
     * 分页查询租户列表
     */
    @GetMapping("/page")
    public Result<Page<Tenant>> page(@RequestParam(defaultValue = "1") Long current,
                                     @RequestParam(defaultValue = "10") Long size) {
        Page<Tenant> page = new Page<>(current, size);
        Page<Tenant> result = tenantService.page(page);
        return Result.success(result);
    }

    /**
     * 根据ID获取租户信息
     */
    @GetMapping("/{id}")
    public Result<Tenant> getById(@PathVariable Long id) {
        Tenant tenant = tenantService.getById(id);
        if (tenant != null) {
            return Result.success(tenant);
        }
        return Result.error("租户不存在");
    }

    /**
     * 根据租户编码获取租户信息
     */
    @GetMapping("/code/{tenantCode}")
    public Result<Tenant> getByTenantCode(@PathVariable String tenantCode) {
        Tenant tenant = tenantService.getByTenantCode(tenantCode);
        if (tenant != null) {
            return Result.success(tenant);
        }
        return Result.error("租户不存在");
    }

    /**
     * 新增租户
     */
    @PostMapping
    public Result<String> add(@Valid @RequestBody Tenant tenant) {
        boolean success = tenantService.save(tenant);
        return success ? Result.success("新增成功") : Result.error("新增失败");
    }

    /**
     * 更新租户
     */
    @PutMapping
    public Result<String> update(@Valid @RequestBody Tenant tenant) {
        boolean success = tenantService.updateById(tenant);
        return success ? Result.success("更新成功") : Result.error("更新失败");
    }

    /**
     * 更新租户状态
     */
    @PutMapping("/{id}/status")
    public Result<String> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        Tenant tenant = new Tenant();
        tenant.setId(id);
        tenant.setStatus(status);
        boolean success = tenantService.updateById(tenant);
        return success ? Result.success("操作成功") : Result.error("操作失败");
    }
}
