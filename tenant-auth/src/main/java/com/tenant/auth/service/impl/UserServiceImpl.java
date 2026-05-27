package main.java.com.tenant.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tenant.auth.entity.User;
import com.tenant.auth.mapper.UserMapper;
import com.tenant.auth.service.UserService;
import com.tenant.core.tenant.TenantContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现类
 * 基于MyBatis-Plus {@link ServiceImpl} 实现用户查询、认证和注册功能
 * <p>租户隔离说明：租户过滤由MyBatis-Plus {@link TenantLineInnerInterceptor} 自动处理，
 * 无需在查询条件中手动添加tenant_id，插件会自动拼接 {@code WHERE tenant_id = ?}
 * <p>注册逻辑中租户ID的来源：{@link TenantContextHolder#getCurrentTenantId()}，
 * 若上下文中无租户ID则回退到 "default_tenant"
 *
 * @author Aze
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 根据用户名获取用户信息
     * 租户过滤由MyBatis-Plus插件自动添加 WHERE tenant_id = ?
     *
     * @param username 用户名
     * @return 用户信息
     */
    @Override
    public User getUserByUsername(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        return this.getOne(wrapper);
    }

    /**
     * 根据用户ID获取用户信息
     * 租户过滤由MyBatis-Plus插件自动添加 WHERE tenant_id = ?
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    @Override
    public User getUserById(Long userId) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getId, userId);
        return this.getOne(wrapper);
    }

    /**
     * 验证用户凭据
     *
     * @param username 用户名
     * @param password 密码
     * @return 用户信息，如果验证失败则返回null
     */
    @Override
    public User authenticate(String username, String password) {
        User user = this.getUserByUsername(username);
        if (user != null && passwordEncoder.matches(password, user.getPassword())) {
            return user;
        }
        return null;
    }

    /**
     * 注册新用户
     * 自动从上下文中获取租户ID并设置到用户实体
     *
     * @param user 用户信息
     * @return 注册是否成功
     */
    @Override
    public boolean registerUser(User user) {
        // 从上下文中获取租户ID
        String tenantId = TenantContextHolder.getCurrentTenantId();
        if (tenantId == null || tenantId.trim().isEmpty()) {
            tenantId = "default_tenant";
        }
        user.setTenantId(tenantId);
        // 加密密码
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return this.save(user);
    }
}