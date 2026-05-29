package com.tenant.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tenant.system.entity.TenantEntity;
import com.tenant.system.mapper.TenantMapper;
import com.tenant.system.service.TenantService;
import org.springframework.stereotype.Service;

/**
 * 租户服务实现类
 *
 * @author Aze
 */
@Service
public class TenantServiceImpl extends ServiceImpl<TenantMapper, TenantEntity> implements TenantService {

    @Override
    public TenantEntity getByTenantCode(String tenantCode) {
        LambdaQueryWrapper<TenantEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TenantEntity::getTenantCode, tenantCode);
        return this.getOne(wrapper);
    }
}
