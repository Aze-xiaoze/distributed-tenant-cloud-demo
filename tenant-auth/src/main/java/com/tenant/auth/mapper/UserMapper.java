package com.tenant.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tenant.auth.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户映射器接口
 * 基于MyBatis-Plus {@link BaseMapper} 自动获得单表CRUD方法，无需编写XML
 * <p>多租户过滤由 {@link com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor} 自动处理，
 * 所有查询方法会自动追加 {@code WHERE tenant_id = ?} 条件
 *
 * @author Aze
 */
@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {
    // 继承BaseMapper，自动获得常用的增删改查方法
    // 多租户过滤由MyBatis-Plus插件自动处理
}