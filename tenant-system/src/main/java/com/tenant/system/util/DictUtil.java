package com.tenant.system.util;

import com.tenant.system.entity.DictEntity;
import com.tenant.system.service.DictService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 数据字典缓存工具类
 * <p>提供带缓存的功能查询字典数据，避免每次请求都走数据库
 * <p>缓存策略：
 * <ul>
 *   <li>缓存名称：{@code dict}</li>
 *   <li>缓存键：字典类型（{@code dict:{dictType}}）</li>
 *   <li>过期时间：跟随 CacheConfig 默认 30 分钟</li>
 *   <li>空值不缓存</li>
 * </ul>
 * <p>使用示例：
 * <pre>
 *     List&lt;DictEntity&gt; items = dictUtil.getDictItems("user_status");
 *     String label = dictUtil.getDictLabel("user_status", "1");
 * </pre>
 *
 * @author Aze
 */
@Slf4j
@Component
public class DictUtil {

    private static final String CACHE_NAME = "dict";

    private final DictService dictService;
    private final DictUtil self;

    public DictUtil(DictService dictService, DictUtil self) {
        this.dictService = dictService;
        this.self = self;
    }

    /**
     * 根据字典类型获取所有启用的字典项（带缓存）
     *
     * @param dictType 字典类型
     * @return 字典项列表
     */
    @Cacheable(value = CACHE_NAME, key = "#dictType", unless = "#result == null || #result.isEmpty()")
    public List<DictEntity> getDictItems(String dictType) {
        log.debug("缓存未命中，从数据库查询字典: type={}", dictType);
        return dictService.listByDictType(dictType);
    }

    /**
     * 根据字典类型和字典值获取字典标签
     * <p>先查询字典列表（利用缓存），再从列表中匹配值
     *
     * @param dictType  字典类型
     * @param dictValue 字典值
     * @return 字典标签，不存在返回 null
     */
    public String getDictLabel(String dictType, String dictValue) {
        // 通过代理对象调用，确保 @Cacheable 生效
        List<DictEntity> items = self.getDictItems(dictType);
        for (DictEntity dictEntity : items) {
            if (dictEntity.getDictValue().equals(dictValue)) {
                return dictEntity.getDictLabel();
            }
        }
        return null;
    }

    /**
     * 刷新指定字典类型的缓存
     *
     * @param dictType 字典类型
     */
    @CacheEvict(value = CACHE_NAME, key = "#dictType")
    public void refreshDict(String dictType) {
        log.info("刷新字典缓存: type={}", dictType);
    }

    /**
     * 清除所有字典缓存
     */
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void refreshAllDicts() {
        log.info("刷新所有字典缓存");
    }
}