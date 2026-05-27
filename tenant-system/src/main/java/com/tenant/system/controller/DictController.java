package com.tenant.system.controller;

import com.tenant.common.vo.Result;
import com.tenant.system.entity.Dict;
import com.tenant.system.service.DictService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 字典管理控制器
 * 提供字典分页查询、按类型查询、新增、更新、删除等接口
 * <p>接口前缀：{@code /dict}（经网关StripPrefix=1后映射为 {@code /system/dict/**}）
 * <p>字典表为共享表，不进行租户过滤
 *
 * @author Aze
 */
@RestController
@RequestMapping("/dict")
public class DictController {

    @Autowired
    private DictService dictService;

    /**
     * 根据字典类型获取字典列表
     */
    @GetMapping("/type/{dictType}")
    public Result<List<Dict>> listByType(@PathVariable String dictType) {
        List<Dict> dicts = dictService.listByDictType(dictType);
        return Result.success(dicts);
    }

    /**
     * 根据ID获取字典信息
     */
    @GetMapping("/{id}")
    public Result<Dict> getById(@PathVariable Long id) {
        Dict dict = dictService.getById(id);
        if (dict != null) {
            return Result.success(dict);
        }
        return Result.error("字典不存在");
    }

    /**
     * 新增字典
     */
    @PostMapping
    public Result<String> add(@RequestBody Dict dict) {
        boolean success = dictService.save(dict);
        return success ? Result.success("新增成功") : Result.error("新增失败");
    }

    /**
     * 更新字典
     */
    @PutMapping
    public Result<String> update(@RequestBody Dict dict) {
        boolean success = dictService.updateById(dict);
        return success ? Result.success("更新成功") : Result.error("更新失败");
    }

    /**
     * 删除字典
     */
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        boolean success = dictService.removeById(id);
        return success ? Result.success("删除成功") : Result.error("删除失败");
    }
}
