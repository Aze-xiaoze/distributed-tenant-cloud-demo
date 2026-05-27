package com.tenant.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tenant.system.entity.SysConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统配置Mapper接口
 * <p>配置表为共享表，不进行租户过滤
 *
 * @author Aze
 */
@Mapper
public interface SysConfigMapper extends BaseMapper<SysConfig> {
}
