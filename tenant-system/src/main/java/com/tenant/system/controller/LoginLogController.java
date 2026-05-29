package com.tenant.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tenant.common.vo.ResultVO;
import com.tenant.core.log.LoginLogEntity;
import com.tenant.system.service.LoginLogQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 登录日志管理控制器
 * 提供登录日志的分页查询、详情查看和删除接口
 *
 * @author Aze
 */
@RestController
@RequestMapping("/login-log")
@Tag(name = "登录日志", description = "系统登录日志管理")
public class LoginLogController {

    @Autowired
    private LoginLogQueryService loginLogQueryService;

    /**
     * 分页查询登录日志
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询登录日志", description = "支持按用户名、租户ID、登录状态筛选")
    public ResultVO<IPage<LoginLogEntity>> page(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @Parameter(description = "用户名") @RequestParam(required = false) String username,
            @Parameter(description = "租户ID") @RequestParam(required = false) String tenantId,
            @Parameter(description = "登录状态：1-成功，0-失败") @RequestParam(required = false) Integer status) {
        Page<LoginLogEntity> page = new Page<>(current, size);
        return ResultVO.success(loginLogQueryService.queryPage(page, username, tenantId, status));
    }

    /**
     * 根据ID获取登录日志详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "登录日志详情", description = "根据ID获取登录日志详情")
    public ResultVO<LoginLogEntity> getById(@PathVariable Long id) {
        LoginLogEntity log = loginLogQueryService.getById(id);
        return log != null ? ResultVO.success(log) : ResultVO.error("日志不存在");
    }

    /**
     * 删除登录日志
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除登录日志", description = "根据ID删除登录日志")
    public ResultVO<String> delete(@PathVariable Long id) {
        boolean success = loginLogQueryService.removeById(id);
        return success ? ResultVO.success("删除成功") : ResultVO.error("删除失败");
    }

    /**
     * 批量删除登录日志
     */
    @DeleteMapping("/batch")
    @Operation(summary = "批量删除登录日志", description = "根据ID列表批量删除登录日志")
    public ResultVO<String> deleteBatch(@RequestParam java.util.List<Long> ids) {
        boolean success = loginLogQueryService.removeByIds(ids);
        return success ? ResultVO.success("批量删除成功") : ResultVO.error("批量删除失败");
    }
}
