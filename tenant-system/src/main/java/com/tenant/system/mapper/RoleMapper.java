package com.tenant.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tenant.system.entity.RoleEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色Mapper接口
 * 基于MyBatis-Plus {@link BaseMapper} 自动获得单表CRUD方法
 *
 * @author Aze
 */
@Mapper
public interface RoleMapper extends BaseMapper<RoleEntity> {
}
