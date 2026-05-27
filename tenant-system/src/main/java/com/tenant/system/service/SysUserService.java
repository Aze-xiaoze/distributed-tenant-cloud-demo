package com.tenant.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tenant.system.entity.SysUser;

/**
 * 系统用户服务接口
 * 继承MyBatis-Plus {@link IService}，并扩展按用户名查询方法
 * <p>实现类：{@link com.tenant.system.service.impl.SysUserServiceImpl}
 *
 * @author Aze
 */
public interface SysUserService extends IService<SysUser> {

    /**
     * 根据用户名获取用户信息
     *
     * @param username 用户名
     * @return 用户信息
     */
    SysUser getUserByUsername(String username);
}
