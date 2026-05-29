package com.tenant.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tenant.system.entity.SysUserEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统用户Mapper接口
 * 基于MyBatis-Plus {@link BaseMapper} 自动获得单表CRUD方法
 * <p>多租户过滤由 {@link com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor} 自动处理
 *
 * @author Aze
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUserEntity> {
}
