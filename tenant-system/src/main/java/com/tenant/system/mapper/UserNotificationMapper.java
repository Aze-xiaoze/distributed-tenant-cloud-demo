package com.tenant.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tenant.system.entity.UserNotification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 用户消息关联 Mapper 接口
 *
 * @author Aze
 */
@Mapper
public interface UserNotificationMapper extends BaseMapper<UserNotification> {

    /**
     * 批量标记已读（将指定用户的所有未读消息标记为已读）
     *
     * @param userId   用户ID
     * @param tenantId 租户ID
     * @return 更新行数
     */
    @Update("UPDATE sys_user_notification SET is_read = 1, read_time = NOW() " +
            "WHERE user_id = #{userId} AND tenant_id = #{tenantId} AND is_read = 0")
    int markAllAsRead(@Param("userId") Long userId, @Param("tenantId") String tenantId);
}
