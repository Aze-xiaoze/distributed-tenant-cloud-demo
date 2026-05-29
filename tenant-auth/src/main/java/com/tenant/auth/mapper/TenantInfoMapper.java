package com.tenant.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tenant.auth.entity.TenantInfoEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 租户信息Mapper（auth服务只读）
 * <p>auth服务仅查询租户的基本信息（状态、过期时间、最大用户数），用于登录校验
 * <p>租户的增删改操作由tenant-system服务负责
 *
 * @author Aze
 */
@Mapper
public interface TenantInfoMapper extends BaseMapper<TenantInfoEntity> {

    /**
     * 根据租户编码查询租户状态和过期信息
     *
     * @param tenantCode 租户编码
     * @return 租户信息
     */
    @Select("SELECT tenant_code, status, expire_time, max_users FROM tenants WHERE tenant_code = #{tenantCode} AND deleted = 0")
    TenantInfoEntity selectByTenantCode(String tenantCode);
}