-- ============================================================
-- Flyway 迁移脚本：V1.1.0__add_notification_tables.sql
-- 描述：新增站内信和消息通知相关表
-- 作者：Aze
-- 日期：2026-05-28
-- ============================================================

-- ============================================================
-- 11. 站内信表（需要租户过滤）
-- ============================================================
CREATE TABLE IF NOT EXISTS `sys_notification` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `title`           VARCHAR(256)  NOT NULL COMMENT '消息标题',
    `content`         TEXT          NOT NULL COMMENT '消息内容',
    `notification_type` TINYINT     NOT NULL DEFAULT 1 COMMENT '消息类型（1-系统通知，2-租户预警，3-操作提醒）',
    `sender`          VARCHAR(64)   DEFAULT NULL COMMENT '发送人（null表示系统消息）',
    `tenant_id`       VARCHAR(64)   NOT NULL DEFAULT 'default_tenant' COMMENT '租户ID',
    `is_read`         TINYINT       NOT NULL DEFAULT 0 COMMENT '是否已读（0-未读，1-已读）',
    `read_time`       DATETIME      DEFAULT NULL COMMENT '阅读时间',
    `expire_time`     DATETIME      DEFAULT NULL COMMENT '过期时间',
    `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_id` (`tenant_id`),
    KEY `idx_is_read` (`is_read`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内信表';

-- ============================================================
-- 12. 用户消息关联表（需要租户过滤）
-- ============================================================
CREATE TABLE IF NOT EXISTS `sys_user_notification` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `notification_id` BIGINT        NOT NULL COMMENT '消息ID',
    `user_id`         BIGINT        NOT NULL COMMENT '用户ID',
    `tenant_id`       VARCHAR(64)   NOT NULL DEFAULT 'default_tenant' COMMENT '租户ID',
    `is_read`         TINYINT       NOT NULL DEFAULT 0 COMMENT '是否已读',
    `read_time`       DATETIME      DEFAULT NULL COMMENT '阅读时间',
    `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_notification_id` (`notification_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户消息关联表';