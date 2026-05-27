package com.tenant.auth.controller;

import com.tenant.api.grpc.user.*;
import com.tenant.api.grpc.user.UserGrpcServiceGrpc.UserGrpcServiceImplBase;
import com.tenant.auth.entity.User;
import com.tenant.auth.service.UserService;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 用户gRPC服务端实现
 * 替代原 UserInternalController（REST接口），实现gRPC Protobuf生成的 UserGrpcServiceImplBase
 * <p>通信方式：gRPC二进制协议（Protobuf序列化），替代原Feign的HTTP/JSON
 * <p>租户上下文：由 GrpcServerTenantInterceptor 从Metadata中提取并设置到TenantContextHolder
 * <p>安全说明：gRPC调用由客户端拦截器透传JWT，服务端租户拦截器提取租户ID；
 * 如需JWT认证校验，可额外添加gRPC服务端认证拦截器
 *
 * @author Aze
 */
@Slf4j
@GrpcService
public class UserInternalController extends UserGrpcServiceImplBase {

    @Autowired
    private UserService userService;

    /**
     * 根据用户名获取用户信息
     * 对应原 Feign 接口: UserService#getUserByUsername
     *
     * @param request          请求（包含username）
     * @param responseObserver 响应观察者
     */
    @Override
    public void getUserByUsername(GetUserByUsernameRequest request,
                                  StreamObserver<UserResponse> responseObserver) {
        try {
            User user = userService.getUserByUsername(request.getUsername());

            UserResponse.Builder responseBuilder = UserResponse.newBuilder();

            if (user != null) {
                responseBuilder.setCode(200)
                        .setMessage("操作成功")
                        .setData(buildUserData(user));
            } else {
                responseBuilder.setCode(400)
                        .setMessage("用户不存在");
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("gRPC getUserByUsername调用失败", e);
            responseObserver.onNext(UserResponse.newBuilder()
                    .setCode(500)
                    .setMessage("用户服务内部错误: " + e.getMessage())
                    .build());
            responseObserver.onCompleted();
        }
    }

    /**
     * 根据用户ID获取用户信息
     * 对应原 Feign 接口: UserService#getUserById
     *
     * @param request          请求（包含user_id）
     * @param responseObserver 响应观察者
     */
    @Override
    public void getUserById(GetUserByIdRequest request,
                            StreamObserver<UserResponse> responseObserver) {
        try {
            User user = userService.getUserById(request.getUserId());

            UserResponse.Builder responseBuilder = UserResponse.newBuilder();

            if (user != null) {
                responseBuilder.setCode(200)
                        .setMessage("操作成功")
                        .setData(buildUserData(user));
            } else {
                responseBuilder.setCode(400)
                        .setMessage("用户不存在");
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("gRPC getUserById调用失败", e);
            responseObserver.onNext(UserResponse.newBuilder()
                    .setCode(500)
                    .setMessage("用户服务内部错误: " + e.getMessage())
                    .build());
            responseObserver.onCompleted();
        }
    }

    /**
     * 将User实体转换为Protobuf UserData（不含密码字段，安全传输）
     *
     * @param user 用户实体
     * @return Protobuf UserData
     */
    private UserData buildUserData(User user) {
        UserData.Builder builder = UserData.newBuilder()
                .setId(user.getId())
                .setUsername(user.getUsername() != null ? user.getUsername() : "")
                .setStatus(user.getStatus() != null ? user.getStatus() : 0)
                .setTenantId(user.getTenantId() != null ? user.getTenantId() : "");

        if (user.getNickname() != null) {
            builder.setNickname(user.getNickname());
        }
        if (user.getEmail() != null) {
            builder.setEmail(user.getEmail());
        }
        if (user.getPhone() != null) {
            builder.setPhone(user.getPhone());
        }

        return builder.build();
    }
}
