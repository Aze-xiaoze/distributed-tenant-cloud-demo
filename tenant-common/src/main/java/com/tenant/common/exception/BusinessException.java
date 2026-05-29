package com.tenant.common.exception;

import com.tenant.common.enums.ErrorCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

/**
 * 业务异常类
 * 用于表示业务逻辑层面的异常，如参数校验失败、业务规则违反等
 * <p>与系统异常（RuntimeException）区分，业务异常默认错误码400，
 * 会被{@link GlobalExceptionHandler}捕获并返回业务失败响应
 *
 * @author Aze
 */
@Setter
@Getter
public class BusinessException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    private Integer code;

    public BusinessException() {
        super();
    }

    public BusinessException(String message) {
        super(message);
        this.code = ErrorCode.BUSINESS_ERROR.getCode(); // 默认业务错误码
    }

    /**
     * 使用统一错误码枚举构造异常
     *
     * @param errorCode 错误码枚举
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    /**
     * 使用统一错误码枚举构造异常（自定义消息）
     *
     * @param errorCode 错误码枚举
     * @param message   自定义错误消息
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.code = ErrorCode.BUSINESS_ERROR.getCode(); // 默认业务错误码
    }

}