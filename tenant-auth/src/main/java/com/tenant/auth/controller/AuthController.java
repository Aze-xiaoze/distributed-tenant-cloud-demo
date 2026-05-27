package com.tenant.auth.controller;

import com.tenant.auth.dto.LoginRequest;
import com.tenant.auth.dto.RegisterRequest;
import com.tenant.auth.entity.User;
import com.tenant.auth.service.UserService;
import com.tenant.auth.util.JwtUtil;
import com.tenant.common.vo.Result;
import com.tenant.core.tenant.TenantContextHolder;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 * 提供用户登录、注册、令牌验证等认证相关的REST接口
 * <p>接口前缀：{@code /auth}
 * <p>放行规则：登录({@code /auth/login})、注册({@code /auth/register})、令牌验证({@code /auth/validate-token}) 无需认证
 *
 * @author Aze
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 用户登录
     * 通过请求体接收登录信息，返回JWT令牌
     *
     * @param loginRequest 登录请求
     * @return 认证结果，包含JWT令牌和租户ID
     */
    @PostMapping("/login")
    public Result<Map<String, String>> login(@Valid @RequestBody LoginRequest loginRequest) {
        // 验证用户凭据
        User user = userService.authenticate(loginRequest.getUsername(), loginRequest.getPassword());

        if (user == null) {
            return Result.error("用户名或密码错误");
        }

        // 生成JWT令牌（包含用户名和租户ID）
        String token = jwtUtil.generateToken(user.getUsername(), user.getTenantId());

        Map<String, String> data = new HashMap<>();
        data.put("token", token);
        data.put("tenantId", user.getTenantId());
        data.put("username", user.getUsername());

        return Result.success("登录成功", data);
    }

    /**
     * 用户注册
     *
     * @param registerRequest 注册请求
     * @return 注册结果
     */
    @PostMapping("/register")
    public Result<String> register(@Valid @RequestBody RegisterRequest registerRequest) {
        // 验证用户是否已存在
        User existingUser = userService.getUserByUsername(registerRequest.getUsername());
        if (existingUser != null) {
            return Result.error("用户名已存在");
        }

        // 构建用户实体
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setPassword(registerRequest.getPassword());
        user.setNickname(registerRequest.getNickname());
        user.setEmail(registerRequest.getEmail());
        user.setPhone(registerRequest.getPhone());
        user.setStatus(1);

        // 执行注册逻辑（租户ID由上下文自动注入）
        boolean success = userService.registerUser(user);
        if (success) {
            return Result.success("注册成功");
        } else {
            return Result.error("注册失败");
        }
    }

    /**
     * 验证令牌
     *
     * @param token JWT令牌
     * @return 验证结果
     */
    @GetMapping("/validate-token")
    public Result<Boolean> validateToken(@RequestParam String token) {
        try {
            String username = jwtUtil.getUsernameFromToken(token);
            boolean isValid = jwtUtil.validateToken(token, username);
            return Result.success(isValid);
        } catch (Exception e) {
            return Result.error("令牌验证失败");
        }
    }
}