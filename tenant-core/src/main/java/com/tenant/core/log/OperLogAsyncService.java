package com.tenant.core.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 操作日志异步写入服务
 * <p>将操作日志的持久化操作从业务线程剥离，使用独立线程池执行，
 * 避免日志写入阻塞业务接口响应
 * <p>使用 {@code @Async("logExecutor")} 指定日志专用线程池
 *
 * @author Aze
 */
@Component
public class OperLogAsyncService {

    private static final Logger log = LoggerFactory.getLogger(OperLogAsyncService.class);

    private final OperLogMapper operLogMapper;

    public OperLogAsyncService(OperLogMapper operLogMapper) {
        this.operLogMapper = operLogMapper;
    }

    /**
     * 异步保存操作日志
     * <p>在独立线程中执行数据库写入，异常不影响业务线程
     *
     * @param logEntity 操作日志实体
     */
    @Async("logExecutor")
    public void saveLogAsync(OperLogEntity logEntity) {
        try {
            operLogMapper.insert(logEntity);
        } catch (Exception e) {
            log.error("操作日志异步保存失败：title={}, operator={}, error={}",
                    logEntity.getTitle(), logEntity.getOperator(), e.getMessage());
        }
    }
}
