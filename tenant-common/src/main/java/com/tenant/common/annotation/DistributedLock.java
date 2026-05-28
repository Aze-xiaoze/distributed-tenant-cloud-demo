package com.tenant.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 分布式锁注解
 * <p>标注在方法上，通过 Redis SET NX EX 实现分布式互斥锁，防止并发场景下的资源竞争
 * <p>使用示例：
 * <pre>
 *     @DistributedLock(lockKey = "'order:create:' + #orderId", expireTime = 30)
 *     public void createOrder(String orderId) { ... }
 * </pre>
 * <p>SpEL 表达式支持：
 * <ul>
 *   <li>{@code #paramName} — 方法参数引用</li>
 *   <li>{@code 'staticPrefix:' + #paramName} — 混合静态前缀和参数</li>
 * </ul>
 * <p><b>注意</b>：锁 key 会自动添加前缀 {@code lock:}，避免与其他 Redis key 冲突
 *
 * @author Aze
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {

    /**
     * 锁的 Redis Key（支持 SpEL 表达式）
     * <p>最终 key = {@code lock:{lockKey}}
     */
    String lockKey();

    /**
     * 锁过期时间（秒），防止死锁
     * <p>默认 30 秒，建议根据业务执行耗时设置，过长浪费 Redis 资源，过短可能锁提前释放
     */
    long expireTime() default 30;

    /**
     * 获取锁等待时间（毫秒）
     * <p>0 表示不等待，立即返回（获取不到锁时抛出异常）
     * <p>设置 >0 时，会自旋等待直到超时
     */
    long waitTime() default 0;
}