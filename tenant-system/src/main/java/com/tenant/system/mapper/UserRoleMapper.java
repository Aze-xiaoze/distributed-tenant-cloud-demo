package com.tenant.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tenant.system.entity.UserRoleEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户角色关联Mapper接口
 * <p>多租户过滤由MyBatis-Plus插件自动处理
 *
 * @author Aze
 */
@Mapper
public interface UserRoleMapper extends BaseMapper<UserRoleEntity> {
}
