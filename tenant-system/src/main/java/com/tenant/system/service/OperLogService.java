package com.tenant.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tenant.core.log.OperLogEntity;

/**
 * 操作日志服务接口
 *
 * @author Aze
 */
public interface OperLogService extends IService<OperLogEntity> {

    /**
     * 分页查询操作日志
     *
     * @param page      分页参数
     * @param operator  操作人（可选）
     * @param tenantId  租户ID（可选）
     * @param title     操作模块（可选）
     * @return 分页结果
     */
    IPage<OperLogEntity> queryPage(IPage<OperLogEntity> page, String operator, String tenantId, String title);
}
