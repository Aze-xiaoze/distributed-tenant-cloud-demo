package com.tenant.auth.controller;

import com.tenant.api.grpc.user.*;
import com.tenant.api.grpc.user.UserGrpcServiceGrpc.UserGrpcServiceImplBase;
import com.tenant.auth.entity.UserEntity;
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
        UserEntity userEntity = userService.getUserByUsername(request.getUsername());

        UserResponse.Builder responseBuilder = UserResponse.newBuilder();

        if (userEntity != null) {
            responseBuilder.setCode(200)
                    .setMessage("操作成功")
                    .setData(buildUserData(userEntity));
        } else {
            responseBuilder.setCode(400)
                    .setMessage("用户不存在");
        }

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
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
        UserEntity userEntity = userService.getUserById(request.getUserId());

        UserResponse.Builder responseBuilder = UserResponse.newBuilder();

        if (userEntity != null) {
            responseBuilder.setCode(200)
                    .setMessage("操作成功")
                    .setData(buildUserData(userEntity));
        } else {
            responseBuilder.setCode(400)
                    .setMessage("用户不存在");
        }

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    /**
     * 将User实体转换为Protobuf UserData（不含密码字段，安全传输）
     *
     * @param userEntity 用户实体
     * @return Protobuf UserData
     */
    private UserData buildUserData(UserEntity userEntity) {
        UserData.Builder builder = UserData.newBuilder()
                .setId(userEntity.getId())
                .setUsername(userEntity.getUsername() != null ? userEntity.getUsername() : "")
                .setStatus(userEntity.getStatus() != null ? userEntity.getStatus() : 0)
                .setTenantId(userEntity.getTenantId() != null ? userEntity.getTenantId() : "");

        if (userEntity.getNickname() != null) {
            builder.setNickname(userEntity.getNickname());
        }
        if (userEntity.getEmail() != null) {
            builder.setEmail(userEntity.getEmail());
        }
        if (userEntity.getPhone() != null) {
            builder.setPhone(userEntity.getPhone());
        }

        return builder.build();
    }
}
