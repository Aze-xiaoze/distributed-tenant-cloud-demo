package com.tenant.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tenant.common.vo.ResultVO;
import com.tenant.system.entity.SysConfigEntity;
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
    public ResultVO<Page<SysConfigEntity>> page(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @Parameter(description = "配置键模糊查询") @RequestParam(required = false) String configKey) {
        Page<SysConfigEntity> page = new Page<>(current, size);
        LambdaQueryWrapper<SysConfigEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(configKey != null && !configKey.isEmpty(), SysConfigEntity::getConfigKey, configKey)
                .orderByDesc(SysConfigEntity::getCreateTime);
        return ResultVO.success(sysConfigService.page(page, wrapper));
    }

    /**
     * 根据配置键获取配置值
     */
    @GetMapping("/key/{configKey}")
    @Operation(summary = "根据键获取配置值", description = "根据配置键查询对应的配置值")
    public ResultVO<String> getByKey(@PathVariable String configKey) {
        String value = sysConfigService.getConfigValue(configKey);
        if (value != null) {
            return ResultVO.success(value);
        }
        return ResultVO.error("配置项不存在");
    }

    /**
     * 根据ID获取配置信息
     */
    @GetMapping("/{id}")
    @Operation(summary = "配置详情", description = "根据ID获取配置详情")
    public ResultVO<SysConfigEntity> getById(@PathVariable Long id) {
        SysConfigEntity config = sysConfigService.getById(id);
        if (config != null) {
            return ResultVO.success(config);
        }
        return ResultVO.error("配置不存在");
    }

    /**
     * 新增配置
     */
    @PostMapping
    @Operation(summary = "新增配置", description = "新增系统配置项")
    public ResultVO<String> add(@Valid @RequestBody SysConfigEntity config) {
        boolean success = sysConfigService.save(config);
        return success ? ResultVO.success("新增成功") : ResultVO.error("新增失败");
    }

    /**
     * 更新配置
     */
    @PutMapping
    @Operation(summary = "修改配置", description = "修改系统配置项")
    public ResultVO<String> update(@Valid @RequestBody SysConfigEntity config) {
        boolean success = sysConfigService.updateById(config);
        return success ? ResultVO.success("更新成功") : ResultVO.error("更新失败");
    }

    /**
     * 删除配置
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除配置", description = "根据ID删除配置项")
    public ResultVO<String> delete(@PathVariable Long id) {
        boolean success = sysConfigService.removeById(id);
        return success ? ResultVO.success("删除成功") : ResultVO.error("删除失败");
    }
}
