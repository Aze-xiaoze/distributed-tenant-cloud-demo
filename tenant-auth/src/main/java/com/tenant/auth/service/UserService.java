package com.tenant.auth.service;

import com.tenant.auth.entity.User;

/**
 * 用户服务接口
 * 定义用户相关的业务操作契约，包括查询、认证和注册
 * <p>实现类 {@link main.java.com.tenant.auth.service.impl.UserServiceImpl} 基于 MyBatis-Plus ServiceImpl
 *
 * @author Aze
 */
public interface UserService {

    /**
     * 根据用户名获取用户信息
     *
     * @param username 用户名
     * @return 用户信息
     */
    User getUserByUsername(String username);

    /**
     * 根据用户ID获取用户信息
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    User getUserById(Long userId);

    /**
     * 验证用户凭据
     *
     * @param username 用户名
     * @param password 密码
     * @return 用户信息，如果验证失败则返回null
     */
    User authenticate(String username, String password);

    /**
     * 注册新用户
     *
     * @param user 用户信息
     * @return 注册是否成功
     */
    boolean registerUser(User user);
}