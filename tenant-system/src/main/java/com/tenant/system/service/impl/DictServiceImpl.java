package com.tenant.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tenant.system.entity.DictEntity;
import com.tenant.system.mapper.DictMapper;
import com.tenant.system.service.DictService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 字典服务实现类
 *
 * @author Aze
 */
@Service
public class DictServiceImpl extends ServiceImpl<DictMapper, DictEntity> implements DictService {

    @Override
    public List<DictEntity> listByDictType(String dictType) {
        LambdaQueryWrapper<DictEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DictEntity::getDictType, dictType)
                .eq(DictEntity::getStatus, 1)
                .orderByAsc(DictEntity::getSortOrder);
        return this.list(wrapper);
    }
}
