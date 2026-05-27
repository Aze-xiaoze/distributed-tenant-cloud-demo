package com.tenant.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tenant.system.entity.Dict;
import org.apache.ibatis.annotations.Mapper;

/**
 * 字典Mapper接口
 * <p>字典表为共享表，不进行租户过滤
 *
 * @author Aze
 */
@Mapper
public interface DictMapper extends BaseMapper<Dict> {
}
