package com.tenant.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tenant.system.entity.Tenant;
import org.apache.ibatis.annotations.Mapper;

/**
 * 租户Mapper接口
 * 基于MyBatis-Plus {@link BaseMapper} 自动获得单表CRUD方法
 * <p>租户表为共享表，不进行租户过滤
 *
 * @author Aze
 */
@Mapper
public interface TenantMapper extends BaseMapper<Tenant> {
}
