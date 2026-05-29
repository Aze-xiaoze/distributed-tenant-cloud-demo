package com.tenant.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tenant.common.vo.ResultVO;
import com.tenant.system.entity.DictEntity;
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
    public ResultVO<Page<DictEntity>> page(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @Parameter(description = "字典类型") @RequestParam(required = false) String dictType) {
        Page<DictEntity> page = new Page<>(current, size);
        LambdaQueryWrapper<DictEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(dictType != null && !dictType.isEmpty(), DictEntity::getDictType, dictType)
                .orderByAsc(DictEntity::getSortOrder);
        return ResultVO.success(dictService.page(page, wrapper));
    }

    @GetMapping("/type/{dictType}")
    @Operation(summary = "按类型查询字典项", description = "获取指定字典类型下的所有字典项")
    public ResultVO<List<DictEntity>> listByType(@PathVariable String dictType) {
        LambdaQueryWrapper<DictEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DictEntity::getDictType, dictType)
                .eq(DictEntity::getStatus, 1)
                .orderByAsc(DictEntity::getSortOrder);
        return ResultVO.success(dictService.list(wrapper));
    }

    @GetMapping("/{id}")
    @Operation(summary = "字典详情", description = "根据ID获取字典项详情")
    public ResultVO<DictEntity> getById(@PathVariable Long id) {
        DictEntity dictEntity = dictService.getById(id);
        return dictEntity != null ? ResultVO.success(dictEntity) : ResultVO.error("字典项不存在");
    }

    @PostMapping
    @Operation(summary = "新增字典", description = "新增字典项")
    public ResultVO<String> save(@Valid @RequestBody DictEntity dictEntity) {
        boolean success = dictService.save(dictEntity);
        return success ? ResultVO.success("新增成功") : ResultVO.error("新增失败");
    }

    @PutMapping
    @Operation(summary = "修改字典", description = "修改字典项")
    public ResultVO<String> update(@Valid @RequestBody DictEntity dictEntity) {
        boolean success = dictService.updateById(dictEntity);
        return success ? ResultVO.success("修改成功") : ResultVO.error("修改失败");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除字典", description = "根据ID删除字典项")
    public ResultVO<String> delete(@PathVariable Long id) {
        boolean success = dictService.removeById(id);
        return success ? ResultVO.success("删除成功") : ResultVO.error("删除失败");
    }
}
