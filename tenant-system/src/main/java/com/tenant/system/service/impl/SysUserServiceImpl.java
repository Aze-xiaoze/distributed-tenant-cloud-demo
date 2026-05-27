package com.tenant.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tenant.system.entity.SysUser;
import com.tenant.system.mapper.SysUserMapper;
import com.tenant.system.service.SysUserService;
import org.springframework.stereotype.Service;

/**
 * 系统用户服务实现类
 * 租户过滤由MyBatis-Plus {@link com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor} 自动处理，
 * 无需在查询条件中手动添加tenant_id
 *
 * @author Aze
 */
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    @Override
    public SysUser getUserByUsername(String username) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username);
        return this.getOne(wrapper);
    }
}
