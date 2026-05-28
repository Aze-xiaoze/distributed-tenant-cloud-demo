package com.tenant.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tenant.core.log.LoginLogEntity;
import com.tenant.core.log.LoginLogMapper;
import com.tenant.system.service.LoginLogQueryService;
import org.springframework.stereotype.Service;

/**
 * 登录日志查询服务实现类
 *
 * @author Aze
 */
@Service
public class LoginLogQueryServiceImpl extends ServiceImpl<LoginLogMapper, LoginLogEntity> implements LoginLogQueryService {

    @Override
    public IPage<LoginLogEntity> queryPage(IPage<LoginLogEntity> page, String username, String tenantId, Integer status) {
        LambdaQueryWrapper<LoginLogEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(username != null && !username.isEmpty(), LoginLogEntity::getUsername, username)
                .eq(tenantId != null && !tenantId.isEmpty(), LoginLogEntity::getTenantId, tenantId)
                .eq(status != null, LoginLogEntity::getLoginStatus, status)
                .orderByDesc(LoginLogEntity::getLoginTime);
        return this.page(page, wrapper);
    }
}
