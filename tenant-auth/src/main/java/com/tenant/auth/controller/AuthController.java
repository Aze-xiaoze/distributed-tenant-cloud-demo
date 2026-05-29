package com.tenant.auth.controller;

import com.tenant.auth.config.properties.JwtProperties;
import com.tenant.auth.dto.LoginRequestDTO;
import com.tenant.auth.dto.RegisterRequestDTO;
import com.tenant.auth.entity.TenantInfoEntity;
import com.tenant.auth.entity.UserEntity;
import com.tenant.auth.mapper.RolePermissionMapper;
import com.tenant.auth.mapper.TenantInfoMapper;
import com.tenant.auth.service.UserService;
import com.tenant.auth.util.JwtUtil;
import com.tenant.common.vo.ResultVO;
import com.tenant.core.log.LoginLogService;
import com.tenant.core.security.PasswordValidator;
import com.tenant.core.security.RefreshTokenService;
import com.tenant.core.security.TokenBlacklistService;
import com.tenant.core.tenant.TenantValidator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

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

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final TokenBlacklistService tokenBlacklistService;
    private final RefreshTokenService refreshTokenService;
    private final LoginLogService loginLogService;
    private final TenantInfoMapper tenantInfoMapper;
    private final TenantValidator tenantValidator;
    private final RolePermissionMapper rolePermissionMapper;
    private final StringRedisTemplate redisTemplate;

    public AuthController(UserService userService,
                          JwtUtil jwtUtil,
                          JwtProperties jwtProperties,
                          TokenBlacklistService tokenBlacklistService,
                          RefreshTokenService refreshTokenService,
                          LoginLogService loginLogService,
                          TenantInfoMapper tenantInfoMapper,
                          TenantValidator tenantValidator,
                          RolePermissionMapper rolePermissionMapper,
                          StringRedisTemplate redisTemplate) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.jwtProperties = jwtProperties;
        this.tokenBlacklistService = tokenBlacklistService;
        this.refreshTokenService = refreshTokenService;
        this.loginLogService = loginLogService;
        this.tenantInfoMapper = tenantInfoMapper;
        this.tenantValidator = tenantValidator;
        this.rolePermissionMapper = rolePermissionMapper;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 用户登录
     * 通过请求体接收登录信息，查询用户角色权限后返回JWT令牌
     *
     * @param loginRequest 登录请求
     * @return 认证结果，包含JWT令牌、租户ID、角色和权限
     */
    @PostMapping("/login")
    public ResultVO<Map<String, Object>> login(@Valid @RequestBody LoginRequestDTO loginRequest) {
        String username = loginRequest.getUsername();

        // 检查用户是否被锁定（连续登录失败过多）
        if (loginLogService.isUserLocked(username)) {
            loginLogService.logLoginFailure(username, null, "账户已锁定，请15分钟后重试");
            return ResultVO.error("账户已锁定，连续登录失败次数过多，请15分钟后重试");
        }

        // 检查IP是否被封禁
        String clientIp = getClientIp();
        if (clientIp != null && loginLogService.isIpBanned(clientIp)) {
            loginLogService.logLoginFailure(username, null, "IP已被封禁");
            return ResultVO.error("当前IP登录失败次数过多，请30分钟后重试");
        }

        // 验证用户凭据
        UserEntity userEntity = userService.authenticate(loginRequest.getUsername(), loginRequest.getPassword());

        if (userEntity == null) {
            // 记录登录失败日志
            loginLogService.logLoginFailure(username, null, "用户名或密码错误");
            return ResultVO.error("用户名或密码错误");
        }

        // 校验租户状态和过期时间
        TenantInfoEntity tenantInfoEntity = tenantInfoMapper.selectByTenantCode(userEntity.getTenantId());
        if (tenantInfoEntity != null) {
            TenantValidator.TenantValidationResult tenantResult =
                    tenantValidator.validateTenant(userEntity.getTenantId(), tenantInfoEntity.getExpireTime(), tenantInfoEntity.getStatus());
            if (!tenantResult.isValid()) {
                loginLogService.logLoginFailure(username, userEntity.getTenantId(), tenantResult.getErrorMessage());
                return ResultVO.error(tenantResult.getErrorMessage());
            }
        }

        // 查询用户角色和权限
        List<String> roles = rolePermissionMapper.selectRoleCodesByUserId(userEntity.getId());
        List<String> permissions = rolePermissionMapper.selectPermissionsByUserId(userEntity.getId());

        // 生成AccessToken（包含用户名、租户ID、角色和权限）
        String token = jwtUtil.generateToken(userEntity.getUsername(), userEntity.getTenantId(), roles, permissions);

        // 生成RefreshToken（仅包含用户名和租户ID，有效期长）
        String refreshToken = jwtUtil.generateRefreshToken(userEntity.getUsername(), userEntity.getTenantId());
        refreshTokenService.storeRefreshToken(userEntity.getUsername(), refreshToken, jwtProperties.getRefreshExpiration());

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("refreshToken", refreshToken);
        data.put("tokenExpiresIn", jwtProperties.getExpiration() / 1000); // 秒
        data.put("refreshExpiresIn", jwtProperties.getRefreshExpiration() / 1000); // 秒
        data.put("tenantId", userEntity.getTenantId());
        data.put("username", userEntity.getUsername());
        data.put("roles", roles);
        data.put("permissions", permissions);

        // 记录登录成功日志
        loginLogService.logLoginSuccess(userEntity.getUsername(), userEntity.getTenantId(), "登录成功");

        return ResultVO.success("登录成功", data);
    }

    /**
     * 用户注册
     *
     * @param registerRequestDTO 注册请求
     * @return 注册结果
     */
    @PostMapping("/register")
    public ResultVO<String> register(@Valid @RequestBody RegisterRequestDTO registerRequestDTO) {
        // 密码强度校验（超出注解校验范围的复杂度规则）
        PasswordValidator.PasswordValidationResult pwdResult =
                PasswordValidator.validate(registerRequestDTO.getPassword(), registerRequestDTO.getUsername());
        if (!pwdResult.isValid()) {
            return ResultVO.error("密码不符合安全策略：" + pwdResult.getErrors());
        }

        // 验证用户是否已存在
        UserEntity existingUserEntity = userService.getUserByUsername(registerRequestDTO.getUsername());
        if (existingUserEntity != null) {
            return ResultVO.error("用户名已存在");
        }

        // 构建用户实体
        UserEntity userEntity = new UserEntity();
        userEntity.setUsername(registerRequestDTO.getUsername());
        userEntity.setPassword(registerRequestDTO.getPassword());
        userEntity.setNickname(registerRequestDTO.getNickname());
        userEntity.setEmail(registerRequestDTO.getEmail());
        userEntity.setPhone(registerRequestDTO.getPhone());
        userEntity.setStatus(1);

        // 执行注册逻辑（租户ID由上下文自动注入）
        boolean success = userService.registerUser(userEntity);
        if (success) {
            return ResultVO.success("注册成功");
        } else {
            return ResultVO.error("注册失败");
        }
    }

    /**
     * 验证令牌
     *
     * @param token JWT令牌
     * @return 验证结果
     */
    @GetMapping("/validate-token")
    public ResultVO<Boolean> validateToken(@RequestParam String token) {
        try {
            String username = jwtUtil.getUsernameFromToken(token);
            boolean isValid = jwtUtil.validateToken(token, username);
            // 额外检查Token是否已被吊销
            if (isValid) {
                String jti = jwtUtil.getJtiFromToken(token);
                long issuedAt = jwtUtil.getIssuedAtMillisFromToken(token);
                isValid = !tokenBlacklistService.isTokenRevoked(jti, username, issuedAt);
            }
            return ResultVO.success(isValid);
        } catch (Exception e) {
            return ResultVO.error("令牌验证失败");
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
    public ResultVO<String> logout(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);
        if (token == null) {
            return ResultVO.error(401, "未提供认证令牌");
        }

        try {
            String jti = jwtUtil.getJtiFromToken(token);
            long issuedAt = jwtUtil.getIssuedAtMillisFromToken(token);
            long expiration = jwtUtil.getExpirationDateFromToken(token).getTime();
            long remainingMs = expiration - System.currentTimeMillis();

            if (remainingMs > 0) {
                tokenBlacklistService.revokeToken(jti, remainingMs);
            }

            // 同时吊销RefreshToken
            String username = jwtUtil.getUsernameFromToken(token);
            refreshTokenService.revokeRefreshToken(username);

            return ResultVO.success("注销成功");
        } catch (Exception e) {
            return ResultVO.error("注销失败：" + e.getMessage());
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

    /**
     * 获取客户端真实IP（支持代理透传）
     */
    private String getClientIp() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 刷新AccessToken
     * <p>使用RefreshToken获取新的AccessToken和新的RefreshToken（旋转机制）
     * <p>流程：
     * <ol>
     *   <li>验证RefreshToken是否有效（Redis双映射校验 + JWT签名校验）</li>
     *   <li>重新查询用户角色和权限</li>
     *   <li>生成新的AccessToken和RefreshToken</li>
     *   <li>旧RefreshToken自动失效（被新Token替换）</li>
     * </ol>
     *
     * @param request HTTP请求
     * @return 新的AccessToken和RefreshToken
     */
    @PostMapping("/refresh-token")
    public ResultVO<Map<String, Object>> refreshToken(HttpServletRequest request) {
        String refreshToken = extractTokenFromRequest(request);
        if (refreshToken == null) {
            return ResultVO.error(401, "未提供刷新令牌");
        }

        try {
            // 1. 验证RefreshToken的JWT签名和过期时间
            if (!jwtUtil.isRefreshToken(refreshToken)) {
                return ResultVO.error(401, "提供的令牌不是RefreshToken");
            }

            String username = jwtUtil.getUsernameFromToken(refreshToken);
            if (!jwtUtil.validateToken(refreshToken, username)) {
                return ResultVO.error(401, "RefreshToken已过期，请重新登录");
            }

            // 2. 并发保护：防止同一RefreshToken被并发使用（旋转机制下并发会导致Token失效）
            String jti = jwtUtil.getJtiFromToken(refreshToken);
            String lockKey = "refresh_token_lock:" + jti;
            Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, java.util.concurrent.TimeUnit.SECONDS);
            if (Boolean.FALSE.equals(locked)) {
                log.warn("RefreshToken正在被使用（并发刷新），username={}", username);
                return ResultVO.error(429, "刷新令牌正在使用，请稍后重试");
            }

            try {
                // 3. 验证RefreshToken在Redis中是否有效
                String validatedUsername = refreshTokenService.validateRefreshToken(refreshToken);
                if (validatedUsername == null) {
                    return ResultVO.error(401, "RefreshToken无效或已被吊销，请重新登录");
                }

                // 4. 重新查询用户信息和角色权限
                UserEntity userEntity = userService.getUserByUsername(validatedUsername);
                if (userEntity == null || userEntity.getStatus() != 1) {
                    return ResultVO.error(401, "用户不存在或已被禁用");
                }

                List<String> roles = rolePermissionMapper.selectRoleCodesByUserId(userEntity.getId());
                List<String> permissions = rolePermissionMapper.selectPermissionsByUserId(userEntity.getId());

                // 5. 生成新的AccessToken和RefreshToken（旋转机制：每次刷新都生成新RefreshToken）
                String newAccessToken = jwtUtil.generateToken(userEntity.getUsername(), userEntity.getTenantId(), roles, permissions);
                String newRefreshToken = jwtUtil.generateRefreshToken(userEntity.getUsername(), userEntity.getTenantId());
                refreshTokenService.storeRefreshToken(userEntity.getUsername(), newRefreshToken, jwtProperties.getRefreshExpiration());

                Map<String, Object> data = new HashMap<>();
                data.put("token", newAccessToken);
                data.put("refreshToken", newRefreshToken);
                data.put("tokenExpiresIn", jwtProperties.getExpiration() / 1000);
                data.put("refreshExpiresIn", jwtProperties.getRefreshExpiration() / 1000);
                data.put("tenantId", userEntity.getTenantId());
                data.put("username", userEntity.getUsername());
                data.put("roles", roles);
                data.put("permissions", permissions);

                return ResultVO.success("刷新成功", data);
            } finally {
                // 释放并发锁
                redisTemplate.delete(lockKey);
            }

        } catch (Exception e) {
            return ResultVO.error(401, "RefreshToken刷新失败：" + e.getMessage());
        }
    }
}