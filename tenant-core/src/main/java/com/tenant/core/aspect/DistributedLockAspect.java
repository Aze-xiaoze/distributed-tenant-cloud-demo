package com.tenant.core.aspect;

import com.tenant.common.annotation.DistributedLock;
import com.tenant.common.enums.ErrorCode;
import com.tenant.common.exception.BusinessException;
import com.tenant.common.util.SpELUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁切面
 * <p>拦截标注 {@link DistributedLock} 注解的方法，通过 Redis SET NX EX 实现分布式互斥锁
 * <p>加锁流程：
 * <ol>
 *   <li>解析 SpEL 表达式生成最终 Redis Key（添加 {@code lock:} 前缀）</li>
 *   <li>使用 {@code SET NX EX} 尝试获取锁，value 为唯一标识（防止误删其他请求的锁）</li>
 *   <li>若设置 {@code waitTime > 0}，自旋等待直到获取锁或超时</li>
 *   <li>方法执行完成后释放锁（校验 value 一致性后删除）</li>
 * </ol>
 * <p><b>安全设计</b>：
 * <ul>
 *   <li>锁 value 使用 UUID，释放时校验一致性，防止误删</li>
 *   <li>锁自动过期（{@code expireTime}），防止死锁</li>
 *   <li>释放锁使用 Lua 脚本原子操作，防止并发问题</li>
 * </ul>
 *
 * @author Aze
 */
@Slf4j
@Aspect
@Component
public class DistributedLockAspect {

    private static final String LOCK_KEY_PREFIX = "lock:";

    private final StringRedisTemplate redisTemplate;

    public DistributedLockAspect(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Around("@annotation(distributedLock)")
    public Object around(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String lockKey = LOCK_KEY_PREFIX + SpELUtil.parse(
                distributedLock.lockKey(), signature.getMethod(), joinPoint.getArgs());

        String lockValue = UUID.randomUUID().toString().replace("-", "");
        long expireTime = distributedLock.expireTime();
        long waitTime = distributedLock.waitTime();

        log.debug("尝试获取分布式锁: key={}, expireTime={}s, waitTime={}ms", lockKey, expireTime, waitTime);

        // 尝试获取锁
        boolean locked = tryLock(lockKey, lockValue, expireTime, waitTime);
        if (!locked) {
            log.warn("获取分布式锁失败: key={}", lockKey);
            throw new BusinessException(ErrorCode.DISTRIBUTED_LOCK_FAILED);
        }

        try {
            log.debug("成功获取分布式锁: key={}, value={}", lockKey, lockValue);
            return joinPoint.proceed();
        } finally {
            // 释放锁（Lua 脳本保证原子性：仅删除 value 匹配的锁）
            releaseLock(lockKey, lockValue);
            log.debug("释放分布式锁: key={}", lockKey);
        }
    }

    /**
     * 尝试获取锁
     * <p>{@code waitTime = 0} 时只尝试一次；{@code waitTime > 0} 时自旋等待
     *
     * @param lockKey    Redis Key
     * @param lockValue  锁标识（UUID）
     * @param expireTime 过期时间（秒）
     * @param waitTime   等待时间（毫秒）
     * @return 是否成功获取锁
     */
    @SuppressWarnings("BusyWait")
    private boolean tryLock(String lockKey, String lockValue, long expireTime, long waitTime) {
        if (waitTime <= 0) {
            return Boolean.TRUE.equals(
                    redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, expireTime, TimeUnit.SECONDS));
        }

        long startTime = System.currentTimeMillis();
        // 使用指数退避策略，减少 Redis 压力
        long sleepTime = 50;
        while (System.currentTimeMillis() - startTime < waitTime) {
            Boolean result = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, lockValue, expireTime, TimeUnit.SECONDS);
            if (Boolean.TRUE.equals(result)) {
                return true;
            }

            try {
                Thread.sleep(sleepTime);
                // 指数退避：每次等待时间翻倍，最大 500ms
                sleepTime = Math.min(sleepTime * 2, 500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    /**
     * 释放锁（原子操作：仅删除 value 一致的锁）
     * <p>使用 Lua 脚本保证"检查 + 删除"的原子性，防止并发时误删其他请求的锁
     *
     * @param lockKey   Redis Key
     * @param lockValue 当前请求的锁标识
     */
    private void releaseLock(String lockKey, String lockValue) {
        String luaScript = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                "return redis.call('del', KEYS[1]) " +
                "else return 0 end";
        redisTemplate.execute(
                new org.springframework.data.redis.core.script.DefaultRedisScript<>(luaScript, Long.class),
                java.util.Collections.singletonList(lockKey),
                lockValue);
    }
}