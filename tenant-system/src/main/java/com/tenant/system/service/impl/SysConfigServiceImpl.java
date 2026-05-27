package com.tenant.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tenant.system.entity.SysConfig;
import com.tenant.system.mapper.SysConfigMapper;
import com.tenant.system.service.SysConfigService;
import org.springframework.stereotype.Service;

/**
 * 系统配置服务实现类
 *
 * @author Aze
 */
@Service
public class SysConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfig> implements SysConfigService {

    @Override
    public String getConfigValue(String configKey) {
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysConfig::getConfigKey, configKey);
        SysConfig config = this.getOne(wrapper);
        return config != null ? config.getConfigValue() : null;
    }
}
