package com.tenant.auth.controller;

import com.tenant.auth.dto.LoginRequest;
import com.tenant.auth.dto.RegisterRequest;
import com.tenant.auth.entity.User;
import com.tenant.auth.mapper.RolePermissionMapper;
import com.tenant.auth.service.UserService;
import com.tenant.auth.util.JwtUtil;
import com.tenant.common.vo.Result;
import com.tenant.core.security.TokenBlacklistService;
import com.tenant.core.tenant.TenantContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
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

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Autowired
    private RolePermissionMapper rolePermissionMapper;

    /**
     * 用户登录
     * 通过请求体接收登录信息，查询用户角色权限后返回JWT令牌
     *
     * @param loginRequest 登录请求
     * @return 认证结果，包含JWT令牌、租户ID、角色和权限
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest loginRequest) {
        // 验证用户凭据
        User user = userService.authenticate(loginRequest.getUsername(), loginRequest.getPassword());

        if (user == null) {
            return Result.error("用户名或密码错误");
        }

        // 查询用户角色和权限
        List<String> roles = rolePermissionMapper.selectRoleCodesByUserId(user.getId());
        List<String> permissions = rolePermissionMapper.selectPermissionsByUserId(user.getId());

        // 生成JWT令牌（包含用户名、租户ID、角色和权限）
        String token = jwtUtil.generateToken(user.getUsername(), user.getTenantId(), roles, permissions);

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("tenantId", user.getTenantId());
        data.put("username", user.getUsername());
        data.put("roles", roles);
        data.put("permissions", permissions);

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
            // 额外检查Token是否已被吊销
            if (isValid) {
                String jti = jwtUtil.getJtiFromToken(token);
                long issuedAt = jwtUtil.getIssuedAtMillisFromToken(token);
                isValid = !tokenBlacklistService.isTokenRevoked(jti, username, issuedAt);
            }
            return Result.success(isValid);
        } catch (Exception e) {
            return Result.error("令牌验证失败");
        }
    }

    /**
     * 用户注销
     * 将当前Token加入黑名单，实现强制下线效果
     * <p>客户端调用此接口后应清除本地存储的Token
     *
     * @param request HTTP请求（用于提取Authorization头中的Token）
     * @return 注销结果
     */
    @PostMapping("/logout")
    public Result<String> logout(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);
        if (token == null) {
            return Result.error(401, "未提供认证令牌");
        }

        try {
            String jti = jwtUtil.getJtiFromToken(token);
            long issuedAt = jwtUtil.getIssuedAtMillisFromToken(token);
            long expiration = jwtUtil.getExpirationDateFromToken(token).getTime();
            long remainingMs = expiration - System.currentTimeMillis();

            if (remainingMs > 0) {
                tokenBlacklistService.revokeToken(jti, remainingMs);
            }

            return Result.success("注销成功");
        } catch (Exception e) {
            return Result.error("注销失败：" + e.getMessage());
        }
    }

    /**
     * 从HTTP请求中提取Bearer Token
     *
     * @param request HTTP请求
     * @return JWT令牌，如果没有则返回null
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}