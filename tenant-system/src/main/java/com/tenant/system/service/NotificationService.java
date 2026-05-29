package com.tenant.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tenant.core.tenant.TenantContextHolder;
import com.tenant.system.entity.NotificationEntity;
import com.tenant.system.entity.UserNotificationEntity;
import com.tenant.system.mapper.NotificationMapper;
import com.tenant.system.mapper.UserNotificationMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 通知服务
 * <p>提供站内信的发送、查询、标记已读、批量已读等功能
 * <p>租户隔离：所有操作自动附加当前租户ID过滤
 *
 * @author Aze
 */
@Slf4j
@Service
public class NotificationService {

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private UserNotificationMapper userNotificationMapper;

    /**
     * 消息类型常量
     */
    public static final int TYPE_SYSTEM = 1;       // 系统通知
    public static final int TYPE_TENANT_WARN = 2;  // 租户预警
    public static final int TYPE_OPERATION = 3;    // 操作提醒

    /**
     * 发送系统通知（广播给租户下所有用户）
     *
     * @param title   标题
     * @param content 内容
     * @param userIds 用户ID列表
     */
    @Transactional(rollbackFor = Exception.class)
    public void sendNotification(String title, String content, int notificationType, List<Long> userIds) {
        String tenantId = TenantContextHolder.getCurrentTenantId();

        // 1. 创建通知记录
        NotificationEntity notificationEntity = new NotificationEntity();
        notificationEntity.setTitle(title);
        notificationEntity.setContent(content);
        notificationEntity.setNotificationType(notificationType);
        notificationEntity.setSender("system");
        notificationEntity.setTenantId(tenantId != null ? tenantId : "default_tenant");
        notificationEntity.setIsRead(0);
        notificationMapper.insert(notificationEntity);

        // 2. 创建用户-通知关联记录
        for (Long userId : userIds) {
            UserNotificationEntity userNotificationEntity = new UserNotificationEntity();
            userNotificationEntity.setNotificationId(notificationEntity.getId());
            userNotificationEntity.setUserId(userId);
            userNotificationEntity.setTenantId(tenantId != null ? tenantId : "default_tenant");
            userNotificationEntity.setIsRead(0);
            userNotificationMapper.insert(userNotificationEntity);
        }

        log.info("发送通知: title={}, type={}, tenantId={}, userCount={}",
                title, notificationType, tenantId, userIds.size());
    }

    /**
     * 发送租户预警通知
     *
     * @param tenantId 租户ID
     * @param title    标题
     * @param content  内容
     * @param userIds  用户ID列表
     */
    @Transactional(rollbackFor = Exception.class)
    public void sendTenantWarning(String tenantId, String title, String content, List<Long> userIds) {
        NotificationEntity notificationEntity = new NotificationEntity();
        notificationEntity.setTitle(title);
        notificationEntity.setContent(content);
        notificationEntity.setNotificationType(TYPE_TENANT_WARN);
        notificationEntity.setSender("system");
        notificationEntity.setTenantId(tenantId);
        notificationEntity.setIsRead(0);
        notificationMapper.insert(notificationEntity);

        for (Long userId : userIds) {
            UserNotificationEntity userNotificationEntity = new UserNotificationEntity();
            userNotificationEntity.setNotificationId(notificationEntity.getId());
            userNotificationEntity.setUserId(userId);
            userNotificationEntity.setTenantId(tenantId);
            userNotificationEntity.setIsRead(0);
            userNotificationMapper.insert(userNotificationEntity);
        }

        log.warn("租户预警: tenantId={}, title={}", tenantId, title);
    }

    /**
     * 查询用户的通知列表（分页）
     *
     * @param userId   用户ID
     * @param current  当前页
     * @param size     每页大小
     * @param isRead   已读状态（null表示全部）
     * @return 分页结果
     */
    public Page<NotificationEntity> getUserNotifications(Long userId, Long current, Long size, Integer isRead) {
        // 先查用户关联表获取 notificationId 列表
        LambdaQueryWrapper<UserNotificationEntity> userNotiWrapper = new LambdaQueryWrapper<>();
        userNotiWrapper.eq(UserNotificationEntity::getUserId, userId);
        if (isRead != null) {
            userNotiWrapper.eq(UserNotificationEntity::getIsRead, isRead);
        }
        userNotiWrapper.orderByDesc(UserNotificationEntity::getCreateTime);

        Page<UserNotificationEntity> userNotiPage = userNotificationMapper.selectPage(
                new Page<>(current, size), userNotiWrapper);

        // 转换为通知分页
        Page<NotificationEntity> result = new Page<>(current, size, userNotiPage.getTotal());
        List<NotificationEntity> notificationEntities = userNotiPage.getRecords().stream()
                .map(un -> notificationMapper.selectById(un.getNotificationId()))
                .filter(Objects::nonNull)
                .toList();
        result.setRecords(notificationEntities);
        return result;
    }

    /**
     * 标记单条通知为已读
     *
     * @param userId         用户ID
     * @param notificationId 通知ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(Long userId, Long notificationId) {
        String tenantId = TenantContextHolder.getCurrentTenantId();

        // 更新用户-通知关联表
        LambdaQueryWrapper<UserNotificationEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserNotificationEntity::getUserId, userId)
                .eq(UserNotificationEntity::getNotificationId, notificationId)
                .eq(UserNotificationEntity::getIsRead, 0);
        UserNotificationEntity update = new UserNotificationEntity();
        update.setIsRead(1);
        update.setReadTime(LocalDateTime.now());
        userNotificationMapper.update(update, wrapper);

        // 更新通知表已读状态
        NotificationEntity notiUpdate = new NotificationEntity();
        notiUpdate.setId(notificationId);
        notiUpdate.setIsRead(1);
        notiUpdate.setReadTime(LocalDateTime.now());
        notificationMapper.updateById(notiUpdate);
    }

    /**
     * 批量标记所有未读通知为已读
     *
     * @param userId   用户ID
     * @param tenantId 租户ID
     * @return 标记已读的条数
     */
    @Transactional(rollbackFor = Exception.class)
    public int markAllAsRead(Long userId, String tenantId) {
        return userNotificationMapper.markAllAsRead(userId, tenantId);
    }

    /**
     * 获取用户未读通知数量
     *
     * @param userId 用户ID
     * @return 未读数量
     */
    public long getUnreadCount(Long userId) {
        LambdaQueryWrapper<UserNotificationEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserNotificationEntity::getUserId, userId)
                .eq(UserNotificationEntity::getIsRead, 0);
        return userNotificationMapper.selectCount(wrapper);
    }

    /**
     * 删除通知
     *
     * @param notificationId 通知ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteNotification(Long notificationId) {
        // 删除用户关联
        LambdaQueryWrapper<UserNotificationEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserNotificationEntity::getNotificationId, notificationId);
        userNotificationMapper.delete(wrapper);

        // 删除通知本身
        notificationMapper.deleteById(notificationId);
    }

    /**
     * 清理过期通知（7天前已读的通知）
     *
     * @return 清理的条数
     */
    @Transactional(rollbackFor = Exception.class)
    public int cleanExpiredNotifications() {
        LocalDateTime expireTime = LocalDateTime.now().minusDays(7);

        // 查询过期的已读通知
        LambdaQueryWrapper<NotificationEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NotificationEntity::getIsRead, 1)
                .lt(NotificationEntity::getReadTime, expireTime);

        List<NotificationEntity> expiredList = notificationMapper.selectList(wrapper);
        for (NotificationEntity notificationEntity : expiredList) {
            deleteNotification(notificationEntity.getId());
        }

        log.info("清理过期通知: count={}", expiredList.size());
        return expiredList.size();
    }
}
