package com.tenant.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tenant.system.entity.Menu;
import org.apache.ibatis.annotations.Mapper;

/**
 * 菜单Mapper接口
 * <p>菜单表为共享表，不进行租户过滤
 *
 * @author Aze
 */
@Mapper
public interface MenuMapper extends BaseMapper<Menu> {
}
