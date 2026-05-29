package com.tenant.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tenant.system.entity.SysConfigEntity;

/**
 * 系统配置服务接口
 * <p>实现类：{@link com.tenant.system.service.impl.SysConfigServiceImpl}
 *
 * @author Aze
 */
public interface SysConfigService extends IService<SysConfigEntity> {

    /**
     * 根据配置键获取配置值
     *
     * @param configKey 配置键
     * @return 配置值
     */
    String getConfigValue(String configKey);
}
