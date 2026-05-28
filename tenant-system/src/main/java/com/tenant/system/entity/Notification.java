package com.tenant.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 站内信实体类
 * 映射数据库 sys_notification 表，用于系统通知、租户预警、操作提醒等消息
 * <p>需要租户过滤，每条通知属于特定租户
 *
 * @author Aze
 */
@Data
@TableName("sys_notification")
public class Notification implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 消息标题
     */
    private String title;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息类型（1-系统通知，2-租户预警，3-操作提醒）
     */
    private Integer notificationType;

    /**
     * 发送人（null表示系统消息）
     */
    private String sender;

    /**
     * 租户ID
     */
    private String tenantId;

    /**
     * 是否已读（0-未读，1-已读）
     */
    private Integer isRead;

    /**
     * 阅读时间
     */
    private LocalDateTime readTime;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
