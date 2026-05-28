package com.tenant.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tenant.system.entity.Notification;
import org.apache.ibatis.annotations.Mapper;

/**
 * 站内信 Mapper 接口
 *
 * @author Aze
 */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {
}
