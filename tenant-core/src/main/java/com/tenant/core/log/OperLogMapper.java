package com.tenant.core.log;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作日志Mapper
 * 提供操作日志的持久化操作
 *
 * @author Aze
 */
@Mapper
public interface OperLogMapper extends BaseMapper<OperLogEntity> {
}