package com.tenant.core.log;

import lombok.Getter;

import java.lang.annotation.*;

/**
 * 操作日志注解
 * 标注在Controller方法上，自动记录操作日志到sys_oper_log表
 * <p>使用示例：
 * <pre>
 * &#64;OperLog(title = "用户管理", operationType = OperLog.OperationType.INSERT)
 * &#64;PostMapping("/user")
 * public ResultVO&lt;String&gt; addUser(@RequestBody User user) { ... }
 * </pre>
 *
 * @author Aze
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperLog {

    /**
     * 操作模块（如"用户管理"、"角色管理"）
     */
    String title() default "";

    /**
     * 操作类型
     */
    OperationType operationType() default OperationType.QUERY;

    /**
     * 是否保存请求参数
     */
    boolean saveRequestParams() default true;

    /**
     * 是否保存响应结果
     */
    boolean saveResponseResult() default true;

    /**
     * 是否排除敏感参数（如password字段）
     */
    boolean excludeSensitiveParams() default true;

    /**
     * 操作类型枚举
     */
    @Getter
    enum OperationType {
        /** 查询 */
        QUERY(4),
        /** 新增 */
        INSERT(1),
        /** 修改 */
        UPDATE(2),
        /** 删除 */
        DELETE(3),
        /** 导出 */
        EXPORT(5),
        /** 导入 */
        IMPORT(6);

        private final int code;

        OperationType(int code) {
            this.code = code;
        }

    }
}