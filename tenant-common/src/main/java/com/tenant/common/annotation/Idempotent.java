package com.tenant.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口幂等性注解
 * <p>标注在方法上，防止同一请求短时间内重复提交导致业务数据异常
 * <p>实现原理：请求首次到达时在 Redis 中写入标识（SET NX EX），重复请求检测到标识存在则拒绝
 * <p>使用示例：
 * <pre>
 *     &#064;Idempotent(key  = "'order:create:' + #orderId", expireTime = 5)
 *     public ResultVO&lt;Void&gt; createOrder(@RequestBody OrderRequest request) { ... }
 * </pre>
 * <p>SpEL 表达式支持：
 * <ul>
 *   <li>{@code #paramName} — 方法参数引用</li>
 *   <li>{@code #request.fieldName} — 参数字段引用</li>
 *   <li>{@code 'staticPrefix:' + #paramName} — 混合前缀</li>
 * </ul>
 * <p><b>注意</b>：幂等 key 会自动添加前缀 {@code idempotent:}
 *
 * @author Aze
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /**
     * 幂等校验的 Redis Key（支持 SpEL 表达式）
     * <p>最终 key = {@code idempotent:{key}}
     */
    String key();

    /**
     * 幂等标识过期时间（秒）
     * <p>默认 5 秒，即同一请求在 5 秒内重复提交将被拒绝
     * <p>建议根据业务场景调整：支付类建议 10~30 秒，普通提交 3~5 秒
     */
    long expireTime() default 5;

    /**
     * 重复提交时的提示信息
     */
    String message() default "请勿重复提交";
}