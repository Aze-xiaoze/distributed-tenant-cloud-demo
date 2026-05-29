package com.tenant.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tenant.common.vo.ResultVO;
import com.tenant.core.log.OperLogEntity;
import com.tenant.system.service.OperLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 操作日志管理控制器
 * 提供操作日志的分页查询、详情查看和删除接口
 *
 * @author Aze
 */
@RestController
@RequestMapping("/oper-log")
@Tag(name = "操作日志", description = "系统操作日志管理")
public class OperLogController {

    @Autowired
    private OperLogService operLogService;

    /**
     * 分页查询操作日志
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询操作日志", description = "支持按操作人、租户ID、操作模块筛选")
    public ResultVO<IPage<OperLogEntity>> page(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @Parameter(description = "操作人") @RequestParam(required = false) String operator,
            @Parameter(description = "租户ID") @RequestParam(required = false) String tenantId,
            @Parameter(description = "操作模块") @RequestParam(required = false) String title) {
        Page<OperLogEntity> page = new Page<>(current, size);
        return ResultVO.success(operLogService.queryPage(page, operator, tenantId, title));
    }

    /**
     * 根据ID获取操作日志详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "操作日志详情", description = "根据ID获取操作日志详情")
    public ResultVO<OperLogEntity> getById(@PathVariable Long id) {
        OperLogEntity log = operLogService.getById(id);
        return log != null ? ResultVO.success(log) : ResultVO.error("日志不存在");
    }

    /**
     * 删除操作日志
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除操作日志", description = "根据ID删除操作日志")
    public ResultVO<String> delete(@PathVariable Long id) {
        boolean success = operLogService.removeById(id);
        return success ? ResultVO.success("删除成功") : ResultVO.error("删除失败");
    }

    /**
     * 批量删除操作日志
     */
    @DeleteMapping("/batch")
    @Operation(summary = "批量删除操作日志", description = "根据ID列表批量删除操作日志")
    public ResultVO<String> deleteBatch(@RequestParam java.util.List<Long> ids) {
        boolean success = operLogService.removeByIds(ids);
        return success ? ResultVO.success("批量删除成功") : ResultVO.error("批量删除失败");
    }
}
