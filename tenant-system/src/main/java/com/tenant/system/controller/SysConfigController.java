package com.tenant.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tenant.common.vo.Result;
import com.tenant.system.entity.SysConfig;
import com.tenant.system.service.SysConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 系统配置管理控制器
 * 提供系统配置查询、新增、更新、删除等接口
 * <p>接口前缀：{@code /config}（经网关StripPrefix=1后映射为 {@code /system/config/**}）
 * <p>配置表为共享表，不进行租户过滤
 *
 * @author Aze
 */
@RestController
@RequestMapping("/config")
@Tag(name = "系统配置", description = "系统参数配置管理")
public class SysConfigController {

    @Autowired
    private SysConfigService sysConfigService;

    /**
     * 分页查询系统配置
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询配置", description = "支持按配置键模糊查询")
    public Result<Page<SysConfig>> page(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @Parameter(description = "配置键模糊查询") @RequestParam(required = false) String configKey) {
        Page<SysConfig> page = new Page<>(current, size);
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(configKey != null && !configKey.isEmpty(), SysConfig::getConfigKey, configKey)
                .orderByDesc(SysConfig::getCreateTime);
        return Result.success(sysConfigService.page(page, wrapper));
    }

    /**
     * 根据配置键获取配置值
     */
    @GetMapping("/key/{configKey}")
    @Operation(summary = "根据键获取配置值", description = "根据配置键查询对应的配置值")
    public Result<String> getByKey(@PathVariable String configKey) {
        String value = sysConfigService.getConfigValue(configKey);
        if (value != null) {
            return Result.success(value);
        }
        return Result.error("配置项不存在");
    }

    /**
     * 根据ID获取配置信息
     */
    @GetMapping("/{id}")
    @Operation(summary = "配置详情", description = "根据ID获取配置详情")
    public Result<SysConfig> getById(@PathVariable Long id) {
        SysConfig config = sysConfigService.getById(id);
        if (config != null) {
            return Result.success(config);
        }
        return Result.error("配置不存在");
    }

    /**
     * 新增配置
     */
    @PostMapping
    @Operation(summary = "新增配置", description = "新增系统配置项")
    public Result<String> add(@Valid @RequestBody SysConfig config) {
        boolean success = sysConfigService.save(config);
        return success ? Result.success("新增成功") : Result.error("新增失败");
    }

    /**
     * 更新配置
     */
    @PutMapping
    @Operation(summary = "修改配置", description = "修改系统配置项")
    public Result<String> update(@Valid @RequestBody SysConfig config) {
        boolean success = sysConfigService.updateById(config);
        return success ? Result.success("更新成功") : Result.error("更新失败");
    }

    /**
     * 删除配置
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除配置", description = "根据ID删除配置项")
    public Result<String> delete(@PathVariable Long id) {
        boolean success = sysConfigService.removeById(id);
        return success ? Result.success("删除成功") : Result.error("删除失败");
    }
}
