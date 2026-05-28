package com.tenant.core.log;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录日志实体
 * 对应数据库表 sys_login_log
 * <p>记录每次登录尝试（成功/失败），用于安全审计和异常登录检测
 *
 * @author Aze
 */
@Data
@TableName("sys_login_log")
public class LoginLogEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名 */
    private String username;

    /** 租户ID */
    private String tenantId;

    /** 登录IP */
    private String ip;

    /** 登录地点 */
    private String location;

    /** 浏览器 */
    private String browser;

    /** 操作系统 */
    private String os;

    /** 登录状态（1-成功，0-失败） */
    private Integer loginStatus;

    /** 登录消息 */
    private String loginMessage;

    /** 登录时间 */
    private LocalDateTime loginTime;
}