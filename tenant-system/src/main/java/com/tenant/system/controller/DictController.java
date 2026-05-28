package com.tenant.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tenant.common.vo.Result;
import com.tenant.system.entity.Dict;
import com.tenant.system.service.DictService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据字典管理控制器
 * <p>提供字典类型和字典项的增删改查接口
 *
 * @author Aze
 */
@RestController
@RequestMapping("/dict")
@Tag(name = "数据字典", description = "字典类型与字典项管理")
public class DictController {

    @Autowired
    private DictService dictService;

    @GetMapping("/page")
    @Operation(summary = "分页查询字典", description = "支持按字典类型筛选")
    public Result<Page<Dict>> page(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @Parameter(description = "字典类型") @RequestParam(required = false) String dictType) {
        Page<Dict> page = new Page<>(current, size);
        LambdaQueryWrapper<Dict> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(dictType != null && !dictType.isEmpty(), Dict::getDictType, dictType)
                .orderByAsc(Dict::getSortOrder);
        return Result.success(dictService.page(page, wrapper));
    }

    @GetMapping("/type/{dictType}")
    @Operation(summary = "按类型查询字典项", description = "获取指定字典类型下的所有字典项")
    public Result<List<Dict>> listByType(@PathVariable String dictType) {
        LambdaQueryWrapper<Dict> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Dict::getDictType, dictType)
                .eq(Dict::getStatus, 1)
                .orderByAsc(Dict::getSortOrder);
        return Result.success(dictService.list(wrapper));
    }

    @GetMapping("/{id}")
    @Operation(summary = "字典详情", description = "根据ID获取字典项详情")
    public Result<Dict> getById(@PathVariable Long id) {
        Dict dict = dictService.getById(id);
        return dict != null ? Result.success(dict) : Result.error("字典项不存在");
    }

    @PostMapping
    @Operation(summary = "新增字典", description = "新增字典项")
    public Result<String> save(@Valid @RequestBody Dict dict) {
        boolean success = dictService.save(dict);
        return success ? Result.success("新增成功") : Result.error("新增失败");
    }

    @PutMapping
    @Operation(summary = "修改字典", description = "修改字典项")
    public Result<String> update(@Valid @RequestBody Dict dict) {
        boolean success = dictService.updateById(dict);
        return success ? Result.success("修改成功") : Result.error("修改失败");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除字典", description = "根据ID删除字典项")
    public Result<String> delete(@PathVariable Long id) {
        boolean success = dictService.removeById(id);
        return success ? Result.success("删除成功") : Result.error("删除失败");
    }
}
