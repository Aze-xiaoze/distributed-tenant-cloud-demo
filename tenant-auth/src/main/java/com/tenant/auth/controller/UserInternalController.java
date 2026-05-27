package com.tenant.auth.controller;

import com.tenant.auth.entity.User;
import com.tenant.auth.service.UserService;
import com.tenant.common.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户内部接口控制器
 * 供微服务间Feign远程调用，路径与tenant-api中 {@link com.tenant.api.auth.UserService} 接口定义对齐
 * <p>接口前缀：{@code /user}
 * <p>安全说明：此接口需JWT认证，由 {@link com.tenant.auth.filter.JwtAuthenticationFilter} 校验令牌；
 * Feign调用时通过 {@link com.tenant.api.interceptor.FeignRequestInterceptor} 自动透传JWT
 *
 * @author Aze
 */
@RestController
@RequestMapping("/user")
public class UserInternalController {

    @Autowired
    private UserService userService;

    /**
     * 根据用户名获取用户信息
     * 对应 Feign 接口: UserService#getUserByUsername
     *
     * @param username 用户名
     * @return 用户信息
     */
    @GetMapping("/username/{username}")
    public Result<Object> getUserByUsername(@PathVariable("username") String username) {
        User user = userService.getUserByUsername(username);
        if (user != null) {
            // 隐藏密码字段
            user.setPassword(null);
            return Result.success(user);
        }
        return Result.error("用户不存在");
    }

    /**
     * 根据用户ID获取用户信息
     * 对应 Feign 接口: UserService#getUserById
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    @GetMapping("/id/{userId}")
    public Result<Object> getUserById(@PathVariable("userId") Long userId) {
        User user = userService.getUserById(userId);
        if (user != null) {
            // 隐藏密码字段
            user.setPassword(null);
            return Result.success(user);
        }
        return Result.error("用户不存在");
    }
}
