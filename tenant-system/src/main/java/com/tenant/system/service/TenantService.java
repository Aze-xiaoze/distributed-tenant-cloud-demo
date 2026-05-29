package com.tenant.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tenant.system.entity.TenantEntity;

/**
 * 租户服务接口
 * <p>实现类：{@link com.tenant.system.service.impl.TenantServiceImpl}
 *
 * @author Aze
 */
public interface TenantService extends IService<TenantEntity> {

    /**
     * 根据租户编码获取租户信息
     *
     * @param tenantCode 租户编码
     * @return 租户信息
     */
    TenantEntity getByTenantCode(String tenantCode);
}
