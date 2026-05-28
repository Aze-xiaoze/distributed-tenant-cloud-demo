package com.tenant.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tenant.core.log.OperLogEntity;
import com.tenant.core.log.OperLogMapper;
import com.tenant.system.service.OperLogService;
import org.springframework.stereotype.Service;

/**
 * 操作日志服务实现类
 *
 * @author Aze
 */
@Service
public class OperLogServiceImpl extends ServiceImpl<OperLogMapper, OperLogEntity> implements OperLogService {

    @Override
    public IPage<OperLogEntity> queryPage(IPage<OperLogEntity> page, String operator, String tenantId, String title) {
        LambdaQueryWrapper<OperLogEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(operator != null && !operator.isEmpty(), OperLogEntity::getOperator, operator)
                .eq(tenantId != null && !tenantId.isEmpty(), OperLogEntity::getTenantId, tenantId)
                .like(title != null && !title.isEmpty(), OperLogEntity::getTitle, title)
                .orderByDesc(OperLogEntity::getCreateTime);
        return this.page(page, wrapper);
    }
}
