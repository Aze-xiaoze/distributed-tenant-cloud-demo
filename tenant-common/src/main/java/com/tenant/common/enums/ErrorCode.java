package com.tenant.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一错误码枚举
 * <p>定义全局业务错误码规范，避免硬编码
 * <p>错误码格式：5 位数字，按模块划分
 * <ul>
 *   <li>1xxxx: 系统级错误（如 10000 系统繁忙）</li>
 *   <li>2xxxx: 认证授权错误（如 20001 未认证）</li>
 *   <li>3xxxx: 用户相关错误（如 30001 用户不存在）</li>
 *   <li>4xxxx: 租户相关错误（如 40001 租户不存在）</li>
 *   <li>5xxxx: 权限相关错误（如 50001 无权限访问）</li>
 *   <li>6xxxx: 系统配置错误（如 60001 配置不存在）</li>
 * </ul>
 *
 * @author Aze
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    // ==================== 系统级错误 (1xxxx) ====================
    SUCCESS(0, "操作成功"),
    SYSTEM_ERROR(10000, "系统繁忙，请稍后重试"),
    PARAM_ERROR(10001, "参数错误"),
    PARAM_VALID_FAILED(10002, "参数校验失败"),
    IDEMPOTENT_FAILED(10003, "重复请求，请稍后重试"),
    DISTRIBUTED_LOCK_FAILED(10004, "系统繁忙，请稍后重试"),

    // ==================== 认证授权错误 (2xxxx) ====================
    UNAUTHORIZED(20001, "未认证，请先登录"),
    TOKEN_EXPIRED(20002, "令牌已过期，请重新登录"),
    TOKEN_INVALID(20003, "令牌无效，请重新登录"),
    LOGIN_FAILED(20004, "用户名或密码错误"),
    ACCOUNT_DISABLED(20005, "账号已被禁用"),
    REFRESH_TOKEN_INVALID(20006, "刷新令牌无效，请重新登录"),

    // ==================== 用户相关错误 (3xxxx) ====================
    USER_NOT_FOUND(30001, "用户不存在"),
    USER_ALREADY_EXISTS(30002, "用户已存在"),
    USER_PASSWORD_ERROR(30003, "密码错误"),
    USER_ACCOUNT_LOCKED(30004, "账号已被锁定"),

    // ==================== 租户相关错误 (4xxxx) ====================
    TENANT_NOT_FOUND(40001, "租户不存在"),
    TENANT_EXPIRED(40002, "租户已过期，请联系管理员续费"),
    TENANT_DISABLED(40003, "租户已被禁用"),
    TENANT_ID_MISSING(40004, "缺少租户ID"),

    // ==================== 权限相关错误 (5xxxx) ====================
    PERMISSION_DENIED(50001, "无权限访问"),
    ROLE_NOT_FOUND(50002, "角色不存在"),
    MENU_NOT_FOUND(50003, "菜单不存在"),

    // ==================== 系统配置错误 (6xxxx) ====================
    CONFIG_NOT_FOUND(60001, "配置不存在"),
    DICT_NOT_FOUND(60002, "字典不存在"),

    // ==================== 通用业务错误 (9xxxx) ====================
    BUSINESS_ERROR(90000, "业务操作失败"),
    DATA_NOT_FOUND(90001, "数据不存在"),
    DATA_ALREADY_EXISTS(90002, "数据已存在"),
    OPERATION_FAILED(90003, "操作失败");

    /**
     * 错误码
     */
    private final Integer code;

    /**
     * 错误消息
     */
    private final String message;

    /**
     * 根据错误码获取枚举
     *
     * @param code 错误码
     * @return 错误码枚举
     */
    public static ErrorCode getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ErrorCode errorCode : values()) {
            if (errorCode.getCode().equals(code)) {
                return errorCode;
            }
        }
        return null;
    }
}
