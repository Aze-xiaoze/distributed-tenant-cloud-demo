
package com.tenant.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tenant.api.grpc.user.GetUserByIdRequest;
import com.tenant.api.grpc.user.GetUserByUsernameRequest;
import com.tenant.api.grpc.user.UserGrpcServiceGrpc;
import com.tenant.api.grpc.user.UserResponse;
import com.tenant.common.vo.ResultVO;
import com.tenant.core.security.TokenBlacklistService;
import com.tenant.system.entity.SysUserEntity;
import com.tenant.system.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

/**
 * 系统用户管理控制器
 * 提供用户分页查询、详情查询、状态更新等管理接口
 * <p>接口前缀：{@code /user}（经网关StripPrefix=1后映射为 {@code /system/user/**}）
 * <p>租户隔离：所有查询由MyBatis-Plus多租户插件自动过滤，仅返回当前租户的数据
 *
 * @author Aze
 */
@RestController
@RequestMapping("/user")
public class SysUserController {

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private UserGrpcServiceGrpc.UserGrpcServiceBlockingStub userGrpcStub;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    /**
     * JWT过期时间（毫秒），用于设置用户全量吊销的TTL
     */
    @Value("${jwt.expiration:3600000}")
    private long jwtExpiration;

    /**
     * 分页查询用户列表
     * 租户过滤由MyBatis-Plus插件自动处理
     *
     * @param current 当前页
     * @param size    每页大小
     * @return 分页结果
     */
    @GetMapping("/page")
    public ResultVO<Page<SysUserEntity>> page(@RequestParam(defaultValue = "1") Long current,
                                              @RequestParam(defaultValue = "10") Long size) {
        Page<SysUserEntity> page = new Page<>(current, size);
        Page<SysUserEntity> result = sysUserService.page(page);
        return ResultVO.success(result);
    }

    /**
     * 根据ID获取用户信息（跨服务gRPC调用）
     * <p>通过gRPC调用tenant-auth服务获取用户信息
     *
     * @param id 用户ID
     * @return 用户信息
     */
    @GetMapping("/grpc/{id}")
    public ResultVO<Object> getGrpcById(@PathVariable Long id) {
        GetUserByIdRequest request = GetUserByIdRequest.newBuilder()
                .setUserId(id)
                .build();
        UserResponse response = userGrpcStub.getUserById(request);
        if (response.getCode() == 200 && response.hasData()) {
            return ResultVO.success(response.getData());
        }
        return ResultVO.error(response.getMessage());
    }

    /**
     * 根据用户名获取用户信息（跨服务gRPC调用）
     * <p>通过gRPC调用tenant-auth服务获取用户信息，
     * 用于需要跨服务获取认证模块用户数据的场景
     *
     * @param username 用户名
     * @return 用户信息
     */
    @GetMapping("/username/{username}")
    public ResultVO<Object> getByUsername(@PathVariable String username) {
        // 通过gRPC调用tenant-auth服务获取用户信息
        GetUserByUsernameRequest request = GetUserByUsernameRequest.newBuilder()
                .setUsername(username)
                .build();
        UserResponse response = userGrpcStub.getUserByUsername(request);
        if (response.getCode() == 200 && response.hasData()) {
            return ResultVO.success(response.getData());
        }
        return ResultVO.error(response.getMessage());
    }

    /**
     * 更新用户状态
     *
     * @param id     用户ID
     * @param status 状态值
     * @return 操作结果
     */
    @PutMapping("/{id}/status")
    public ResultVO<String> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        // 参数校验：状态值只能为0或1
        if (status != 0 && status != 1) {
            return ResultVO.error("状态值只能为0（禁用）或1（启用）");
        }
        SysUserEntity user = new SysUserEntity();
        user.setId(id);
        user.setStatus(status);
        boolean success = sysUserService.updateById(user);
        // 禁用用户时，强制下线该用户的所有Token
        if (success && status == 0) {
            SysUserEntity existingUser = sysUserService.getById(id);
            if (existingUser != null) {
                tokenBlacklistService.revokeAllUserTokens(
                        existingUser.getUsername(),
                        System.currentTimeMillis(),
                        jwtExpiration
                );
            }
        }
        return success ? ResultVO.success("操作成功") : ResultVO.error("操作失败");
    }

    /**
     * 强制下线指定用户
     * 将该用户的所有Token加入黑名单，需要重新登录
     *
     * @param id 用户ID
     * @return 操作结果
     */
    @PostMapping("/{id}/force-logout")
    public ResultVO<String> forceLogout(@PathVariable Long id) {
        SysUserEntity user = sysUserService.getById(id);
        if (user == null) {
            return ResultVO.error("用户不存在");
        }
        tokenBlacklistService.revokeAllUserTokens(
                user.getUsername(),
                System.currentTimeMillis(),
                jwtExpiration
        );
        return ResultVO.success("已强制下线用户：" + user.getUsername());
    }
}
