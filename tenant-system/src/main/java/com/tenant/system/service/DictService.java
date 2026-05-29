package com.tenant.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tenant.system.entity.DictEntity;

import java.util.List;

/**
 * 字典服务接口
 * <p>实现类：{@link com.tenant.system.service.impl.DictServiceImpl}
 *
 * @author Aze
 */
public interface DictService extends IService<DictEntity> {

    /**
     * 根据字典类型获取字典列表
     *
     * @param dictType 字典类型
     * @return 字典列表
     */
    List<DictEntity> listByDictType(String dictType);
}
