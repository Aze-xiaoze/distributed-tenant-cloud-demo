-- ============================================================
-- 分布式多租户云平台 — 数据库初始化脚本
-- 数据库：tenant_cloud
-- 字符集：utf8mb4
-- 排序规则：utf8mb4_general_ci
-- ============================================================

CREATE DATABASE IF NOT EXISTS `tenant_cloud` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `tenant_cloud`;

-- ============================================================
-- 1. 租户管理表（共享表，不进行租户过滤）
-- ============================================================
DROP TABLE IF EXISTS `tenants`;
CREATE TABLE `tenants` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '租户ID',
    `tenant_code`     VARCHAR(64)   NOT NULL COMMENT '租户编码（唯一标识）',
    `tenant_name`     VARCHAR(128)  NOT NULL COMMENT '租户名称',
    `contact_name`    VARCHAR(64)   DEFAULT NULL COMMENT '联系人',
    `contact_phone`   VARCHAR(20)   DEFAULT NULL COMMENT '联系电话',
    `contact_email`   VARCHAR(128)  DEFAULT NULL COMMENT '联系邮箱',
    `status`          TINYINT       NOT NULL DEFAULT 1 COMMENT '状态（1-启用，0-禁用）',
    `expire_time`     DATETIME      DEFAULT NULL COMMENT '过期时间',
    `max_users`       INT           NOT NULL DEFAULT 100 COMMENT '最大用户数',
    `remark`          VARCHAR(512)  DEFAULT NULL COMMENT '备注',
    `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`         TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除（0-未删除，1-已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_code` (`tenant_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户管理表';

-- 初始化默认租户
INSERT INTO `tenants` (`id`, `tenant_code`, `tenant_name`, `contact_name`, `status`, `remark`) VALUES
(1, 'default_tenant', '默认租户', '系统管理员', 1, '系统默认租户'),
(2, 'tenant_001', '示例租户A', '张三', 1, '示例租户'),
(3, 'tenant_002', '示例租户B', '李四', 1, '示例租户');

-- ============================================================
-- 2. 用户表（需要租户过滤）
-- ============================================================
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username`        VARCHAR(64)   NOT NULL COMMENT '用户名',
    `password`        VARCHAR(128)  NOT NULL COMMENT '密码（BCrypt加密）',
    `nickname`        VARCHAR(64)   DEFAULT NULL COMMENT '昵称',
    `email`           VARCHAR(128)  DEFAULT NULL COMMENT '邮箱',
    `phone`           VARCHAR(20)   DEFAULT NULL COMMENT '手机号',
    `avatar`          VARCHAR(256)  DEFAULT NULL COMMENT '头像URL',
    `status`          TINYINT       NOT NULL DEFAULT 1 COMMENT '状态（1-启用，0-禁用）',
    `tenant_id`       VARCHAR(64)   NOT NULL DEFAULT 'default_tenant' COMMENT '租户ID',
    `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`         TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除（0-未删除，1-已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username_tenant` (`username`, `tenant_id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 初始化管理员用户（密码：admin123）
INSERT INTO `sys_user` (`id`, `username`, `password`, `nickname`, `status`, `tenant_id`) VALUES
(1, 'admin', '$2a$10$N.ZOn9G6/YLFixAOPMg/h.z7pCu6v2XyFDtC4q.jeeGM/TEZyj3Cq', '超级管理员', 1, 'default_tenant'),
(2, 'user_a', '$2a$10$N.ZOn9G6/YLFixAOPMg/h.z7pCu6v2XyFDtC4q.jeeGM/TEZyj3Cq', '租户A管理员', 1, 'tenant_001'),
(3, 'user_b', '$2a$10$N.ZOn9G6/YLFixAOPMg/h.z7pCu6v2XyFDtC4q.jeeGM/TEZyj3Cq', '租户B管理员', 1, 'tenant_002');

-- ============================================================
-- 3. 角色表（共享表，不进行租户过滤）
-- ============================================================
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    `role_code`       VARCHAR(64)   NOT NULL COMMENT '角色编码',
    `role_name`       VARCHAR(128)  NOT NULL COMMENT '角色名称',
    `description`     VARCHAR(512)  DEFAULT NULL COMMENT '角色描述',
    `status`          TINYINT       NOT NULL DEFAULT 1 COMMENT '状态（1-启用，0-禁用）',
    `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`         TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除（0-未删除，1-已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 初始化角色
INSERT INTO `sys_role` (`id`, `role_code`, `role_name`, `description`) VALUES
(1, 'SUPER_ADMIN', '超级管理员', '拥有所有权限'),
(2, 'TENANT_ADMIN', '租户管理员', '管理租户内所有功能'),
(3, 'TENANT_USER', '租户普通用户', '租户内基本操作权限');

-- ============================================================
-- 4. 用户角色关联表（需要租户过滤）
-- ============================================================
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `user_id`         BIGINT        NOT NULL COMMENT '用户ID',
    `role_id`         BIGINT        NOT NULL COMMENT '角色ID',
    `tenant_id`       VARCHAR(64)   NOT NULL DEFAULT 'default_tenant' COMMENT '租户ID',
    `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_role_id` (`role_id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- 初始化用户角色
INSERT INTO `sys_user_role` (`user_id`, `role_id`, `tenant_id`) VALUES
(1, 1, 'default_tenant'),
(2, 2, 'tenant_001'),
(3, 2, 'tenant_002');

-- ============================================================
-- 5. 菜单表（共享表，不进行租户过滤）
-- ============================================================
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
    `parent_id`       BIGINT        NOT NULL DEFAULT 0 COMMENT '父菜单ID（0为顶级）',
    `menu_name`       VARCHAR(128)  NOT NULL COMMENT '菜单名称',
    `menu_type`       TINYINT       NOT NULL COMMENT '菜单类型（1-目录，2-菜单，3-按钮）',
    `path`            VARCHAR(256)  DEFAULT NULL COMMENT '路由路径',
    `permission`      VARCHAR(128)  DEFAULT NULL COMMENT '权限标识',
    `icon`            VARCHAR(64)   DEFAULT NULL COMMENT '图标',
    `sort_order`      INT           NOT NULL DEFAULT 0 COMMENT '排序',
    `visible`         TINYINT       NOT NULL DEFAULT 1 COMMENT '是否可见（1-可见，0-隐藏）',
    `status`          TINYINT       NOT NULL DEFAULT 1 COMMENT '状态（1-启用，0-禁用）',
    `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`         TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除（0-未删除，1-已删除）',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单表';

-- 初始化菜单
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `path`, `permission`, `sort_order`) VALUES
(1, 0, '系统管理', 1, '/system', NULL, 1),
(2, 1, '用户管理', 2, '/system/user', 'system:user:list', 1),
(3, 1, '角色管理', 2, '/system/role', 'system:role:list', 2),
(4, 1, '菜单管理', 2, '/system/menu', 'system:menu:list', 3),
(5, 2, '用户新增', 3, NULL, 'system:user:add', 1),
(6, 2, '用户修改', 3, NULL, 'system:user:edit', 2),
(7, 2, '用户删除', 3, NULL, 'system:user:delete', 3);

-- ============================================================
-- 6. 角色菜单关联表（共享表，不进行租户过滤）
-- ============================================================
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `role_id`         BIGINT        NOT NULL COMMENT '角色ID',
    `menu_id`         BIGINT        NOT NULL COMMENT '菜单ID',
    `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_role_id` (`role_id`),
    KEY `idx_menu_id` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';

-- 初始化角色菜单（超级管理员拥有所有菜单）
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7),
(2, 1), (2, 2), (2, 3), (2, 5), (2, 6);

-- ============================================================
-- 7. 系统字典表（共享表，不进行租户过滤）
-- ============================================================
DROP TABLE IF EXISTS `sys_dict`;
CREATE TABLE `sys_dict` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '字典ID',
    `dict_type`       VARCHAR(64)   NOT NULL COMMENT '字典类型',
    `dict_label`      VARCHAR(128)  NOT NULL COMMENT '字典标签',
    `dict_value`      VARCHAR(128)  NOT NULL COMMENT '字典值',
    `sort_order`      INT           NOT NULL DEFAULT 0 COMMENT '排序',
    `status`          TINYINT       NOT NULL DEFAULT 1 COMMENT '状态（1-启用，0-禁用）',
    `remark`          VARCHAR(512)  DEFAULT NULL COMMENT '备注',
    `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`         TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除（0-未删除，1-已删除）',
    PRIMARY KEY (`id`),
    KEY `idx_dict_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统字典表';

-- 初始化字典数据
INSERT INTO `sys_dict` (`dict_type`, `dict_label`, `dict_value`, `sort_order`) VALUES
('user_status', '启用', '1', 1),
('user_status', '禁用', '0', 2),
('tenant_status', '启用', '1', 1),
('tenant_status', '禁用', '0', 2),
('menu_type', '目录', '1', 1),
('menu_type', '菜单', '2', 2),
('menu_type', '按钮', '3', 3);

-- ============================================================
-- 8. 系统配置表（共享表，不进行租户过滤）
-- ============================================================
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '配置ID',
    `config_key`      VARCHAR(128)  NOT NULL COMMENT '配置键',
    `config_value`    VARCHAR(512)  NOT NULL COMMENT '配置值',
    `description`     VARCHAR(512)  DEFAULT NULL COMMENT '配置描述',
    `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- 初始化系统配置
INSERT INTO `sys_config` (`config_key`, `config_value`, `description`) VALUES
('jwt.secret', 'defaultSecretKeyForTenantCloudPlatformThatIsAtLeast64BytesLongForHS512', 'JWT签名密钥'),
('jwt.expiration', '3600000', 'JWT过期时间（毫秒）'),
('tenant.default-id', 'default_tenant', '默认租户ID'),
('tenant.enable', 'true', '是否启用多租户');
