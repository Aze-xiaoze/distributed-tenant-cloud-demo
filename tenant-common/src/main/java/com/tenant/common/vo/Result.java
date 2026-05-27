package com.tenant.common.vo;

import java.io.Serializable;

/**
 * 通用响应结果封装
 * 用于统一API响应格式，所有接口均返回此对象
 *
 * <p>响应结构：
 * <ul>
 *   <li>code - 响应码（200成功，400业务失败，500系统错误）</li>
 *   <li>message - 响应消息</li>
 *   <li>data - 响应数据（泛型）</li>
 *   <li>timestamp - 响应时间戳</li>
 * </ul>
 *
 * @param <T> 响应数据类型
 * @author Aze
 */
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 响应码（200-成功，400-业务失败，500-系统错误）
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 时间戳
     */
    private Long timestamp;

    public Result() {
        this.timestamp = System.currentTimeMillis();
    }

    public Result(Integer code, String message) {
        this();
        this.code = code;
        this.message = message;
    }

    public Result(Integer code, String message, T data) {
        this(code, message);
        this.data = data;
    }

    /**
     * 成功响应，无数据
     * <p>用于无需返回数据的操作，如删除、更新等
     *
     * @param <T> 响应类型
     * @return 成功结果（code=200, message="操作成功", data=null）
     */
    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功");
    }

    /**
     * 成功响应，带数据
     * <p>用于查询类接口，直接返回业务数据
     *
     * @param data 响应数据
     * @param <T>  响应类型
     * @return 成功结果（code=200, message="操作成功", data=传入数据）
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }

    /**
     * 成功响应，带消息和数据
     * <p>用于需要自定义提示信息的成功场景，如"登录成功"并返回令牌数据
     *
     * @param message 响应消息
     * @param data    响应数据
     * @param <T>     响应类型
     * @return 成功结果（code=200, message=传入消息, data=传入数据）
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data);
    }

    /**
     * 失败响应（系统错误）
     * <p>用于系统级异常，如空指针、数据库错误等
     *
     * @param message 错误消息
     * @param <T>     响应类型
     * @return 失败结果（code=500, message=传入消息）
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(500, message);
    }

    /**
     * 失败响应，带错误码
     * <p>用于需要自定义错误码的场景，如401未认证、403无权限等
     *
     * @param code    错误码（如401、403、500等）
     * @param message 错误消息
     * @param <T>     响应类型
     * @return 失败结果（code=传入错误码, message=传入消息）
     */
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message);
    }

    /**
     * 业务失败响应
     * <p>用于业务逻辑校验不通过，如"用户名已存在"、"参数错误"等
     *
     * @param message 错误消息
     * @param <T>     响应类型
     * @return 业务失败结果（code=400, message=传入消息）
     */
    public static <T> Result<T> fail(String message) {
        return new Result<>(400, message);
    }

    // getter和setter方法
    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}