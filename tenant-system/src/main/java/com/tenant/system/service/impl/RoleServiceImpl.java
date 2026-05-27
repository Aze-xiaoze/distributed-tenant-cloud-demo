package com.tenant.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tenant.system.entity.Role;
import com.tenant.system.mapper.RoleMapper;
import com.tenant.system.service.RoleService;
import org.springframework.stereotype.Service;

/**
 * 角色服务实现类
 * 继承MyBatis-Plus {@link ServiceImpl}，自动拥有CRUD服务方法
 *
 * @author Aze
 */
@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {
}
