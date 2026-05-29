package com.tenant.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tenant.common.vo.ResultVO;
import com.tenant.system.entity.TenantEntity;
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
    public ResultVO<Page<TenantEntity>> page(@RequestParam(defaultValue = "1") Long current,
                                             @RequestParam(defaultValue = "10") Long size) {
        Page<TenantEntity> page = new Page<>(current, size);
        Page<TenantEntity> result = tenantService.page(page);
        return ResultVO.success(result);
    }

    /**
     * 根据ID获取租户信息
     */
    @GetMapping("/{id}")
    public ResultVO<TenantEntity> getById(@PathVariable Long id) {
        TenantEntity tenantEntity = tenantService.getById(id);
        if (tenantEntity != null) {
            return ResultVO.success(tenantEntity);
        }
        return ResultVO.error("租户不存在");
    }

    /**
     * 根据租户编码获取租户信息
     */
    @GetMapping("/code/{tenantCode}")
    public ResultVO<TenantEntity> getByTenantCode(@PathVariable String tenantCode) {
        TenantEntity tenantEntity = tenantService.getByTenantCode(tenantCode);
        if (tenantEntity != null) {
            return ResultVO.success(tenantEntity);
        }
        return ResultVO.error("租户不存在");
    }

    /**
     * 新增租户
     */
    @PostMapping
    public ResultVO<String> add(@Valid @RequestBody TenantEntity tenantEntity) {
        boolean success = tenantService.save(tenantEntity);
        return success ? ResultVO.success("新增成功") : ResultVO.error("新增失败");
    }

    /**
     * 更新租户
     */
    @PutMapping
    public ResultVO<String> update(@Valid @RequestBody TenantEntity tenantEntity) {
        boolean success = tenantService.updateById(tenantEntity);
        return success ? ResultVO.success("更新成功") : ResultVO.error("更新失败");
    }

    /**
     * 更新租户状态
     */
    @PutMapping("/{id}/status")
    public ResultVO<String> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        TenantEntity tenantEntity = new TenantEntity();
        tenantEntity.setId(id);
        tenantEntity.setStatus(status);
        boolean success = tenantService.updateById(tenantEntity);
        return success ? ResultVO.success("操作成功") : ResultVO.error("操作失败");
    }
}
