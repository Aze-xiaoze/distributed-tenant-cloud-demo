package com.tenant.system.controller;

import com.tenant.common.vo.Result;
import com.tenant.system.entity.SysConfig;
import com.tenant.system.service.SysConfigService;
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
public class SysConfigController {

    @Autowired
    private SysConfigService sysConfigService;

    /**
     * 根据配置键获取配置值
     */
    @GetMapping("/key/{configKey}")
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
    public Result<String> add(@RequestBody SysConfig config) {
        boolean success = sysConfigService.save(config);
        return success ? Result.success("新增成功") : Result.error("新增失败");
    }

    /**
     * 更新配置
     */
    @PutMapping
    public Result<String> update(@RequestBody SysConfig config) {
        boolean success = sysConfigService.updateById(config);
        return success ? Result.success("更新成功") : Result.error("更新失败");
    }

    /**
     * 删除配置
     */
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        boolean success = sysConfigService.removeById(id);
        return success ? Result.success("删除成功") : Result.error("删除失败");
    }
}
