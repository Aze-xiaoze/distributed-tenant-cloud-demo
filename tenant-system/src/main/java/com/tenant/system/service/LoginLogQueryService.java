package com.tenant.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tenant.core.log.LoginLogEntity;

/**
 * 登录日志查询服务接口
 * 提供登录日志的查询与管理能力（管理后台使用）
 *
 * @author Aze
 */
public interface LoginLogQueryService extends IService<LoginLogEntity> {

    /**
     * 分页查询登录日志
     *
     * @param page     分页参数
     * @param username 用户名（可选）
     * @param tenantId 租户ID（可选）
     * @param status   登录状态：1-成功，0-失败（可选）
     * @return 分页结果
     */
    IPage<LoginLogEntity> queryPage(IPage<LoginLogEntity> page, String username, String tenantId, Integer status);
}
