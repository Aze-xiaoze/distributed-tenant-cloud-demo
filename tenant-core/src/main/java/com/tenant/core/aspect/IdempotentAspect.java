package com.tenant.core.aspect;

import com.tenant.common.annotation.Idempotent;
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

import java.util.concurrent.TimeUnit;

/**
 * 接口幂等性切面
 * <p>拦截标注 {@link Idempotent} 注解的方法，通过 Redis SET NX EX 实现幂等性校验
 * <p>执行流程：
 * <ol>
 *   <li>解析 SpEL 表达式生成最终 Redis Key（添加 {@code idempotent:} 前缀）</li>
 *   <li>使用 {@code SET NX EX} 写入幂等标识（首次请求成功写入，重复请求返回 false）</li>
 *   <li>若标识已存在（重复请求），抛出 {@link BusinessException} 拒绝执行</li>
 *   <li>方法正常执行，幂等标识自动过期</li>
 * </ol>
 *
 * @author Aze
 */
@Slf4j
@Aspect
@Component
public class IdempotentAspect {

    private static final String IDEMPOTENT_KEY_PREFIX = "idempotent:";

    private final StringRedisTemplate redisTemplate;

    public IdempotentAspect(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String key = IDEMPOTENT_KEY_PREFIX + SpELUtil.parse(
                idempotent.key(), signature.getMethod(), joinPoint.getArgs());

        long expireTime = idempotent.expireTime();

        // SET NX EX：首次请求写入成功，重复请求 key 已存在则写入失败
        Boolean result = redisTemplate.opsForValue()
                .setIfAbsent(key, "1", expireTime, TimeUnit.SECONDS);

        if (Boolean.FALSE.equals(result)) {
            log.warn("幂等性校验失败（重复请求）: key={}", key);
            throw new BusinessException(ErrorCode.IDEMPOTENT_FAILED, idempotent.message());
        }

        log.debug("幂等性校验通过: key={}, expireTime={}s", key, expireTime);
        return joinPoint.proceed();
    }
}