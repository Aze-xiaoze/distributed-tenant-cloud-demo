package com.tenant.common.exception;

/**
 * 业务异常类
 * 用于表示业务逻辑层面的异常，如参数校验失败、业务规则违反等
 * <p>与系统异常（RuntimeException）区分，业务异常默认错误码400，
 * 会被{@link GlobalExceptionHandler}捕获并返回业务失败响应
 *
 * @author Aze
 */
public class BusinessException extends RuntimeException {

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
        this.code = 400; // 默认业务错误码
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.code = 400; // 默认业务错误码
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }
}