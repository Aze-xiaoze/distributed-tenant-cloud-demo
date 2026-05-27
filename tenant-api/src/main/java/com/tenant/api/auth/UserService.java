package com.tenant.api.auth;

import com.tenant.common.vo.Result;
import com.tenant.api.fallback.UserServiceFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 用户服务Feign接口
 * 定义其他微服务调用tenant-auth用户接口的远程契约
 * <p>Feign配置说明：
 * <ul>
 *   <li>name — 目标服务在Nacos中注册的服务名，用于服务发现</li>
 *   <li>path — 统一路径前缀，所有方法的路径会自动加上此前缀</li>
 *   <li>fallbackFactory — 降级工厂，当远程调用失败时返回兜底响应</li>
 * </ul>
 * <p>调用示例：tenant-system通过注入此接口获取tenant-auth中的用户信息
 *
 * @author Aze
 */
@FeignClient(
        name = "tenant-auth",
        path = "/user",
        fallbackFactory = UserServiceFallbackFactory.class
)
public interface UserService {

    /**
     * 根据用户名获取用户信息
     *
     * @param username 用户名
     * @return 用户信息
     */
    @GetMapping("/username/{username}")
    Result<Object> getUserByUsername(@PathVariable("username") String username);

    /**
     * 根据用户ID获取用户信息
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    @GetMapping("/id/{userId}")
    Result<Object> getUserById(@PathVariable("userId") Long userId);
}