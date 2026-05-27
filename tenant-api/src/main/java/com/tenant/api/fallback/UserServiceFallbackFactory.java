package com.tenant.api.fallback;

import com.tenant.api.auth.UserService;
import com.tenant.common.vo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 用户服务Feign降级工厂
 * 当远程调用tenant-auth服务失败时（如服务不可用、超时、网络异常），
 * 提供兜底响应，避免级联故障导致调用方服务崩溃
 * <p>降级策略：返回统一错误提示 "用户服务暂不可用"，不抛出异常
 *
 * @author Aze
 */
@Slf4j
@Component
public class UserServiceFallbackFactory implements FallbackFactory<UserService> {

    @Override
    public UserService create(Throwable cause) {
        log.error("用户服务调用失败，触发降级: {}", cause.getMessage());

        return new UserService() {
            @Override
            public Result<Object> getUserByUsername(String username) {
                return Result.error("用户服务暂不可用，请稍后重试");
            }

            @Override
            public Result<Object> getUserById(Long userId) {
                return Result.error("用户服务暂不可用，请稍后重试");
            }
        };
    }
}
