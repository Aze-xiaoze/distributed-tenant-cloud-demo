package com.tenant.system.config;

import com.tenant.api.grpc.user.UserGrpcServiceGrpc;
import com.tenant.api.interceptor.GrpcClientInterceptor;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * gRPC客户端配置
 * 配置tenant-system作为gRPC客户端连接tenant-auth服务
 * <p>替代原Feign客户端注入方式，使用gRPC Stub进行服务间调用
 * <p>由 net.devh grpc-spring-boot-starter 自动完成：
 * <ul>
 *   <li>从Nacos服务发现获取tenant-auth实例</li>
 *   <li>负载均衡选择实例</li>
 *   <li>应用GrpcClientInterceptor透传JWT和租户ID</li>
 * </ul>
 *
 * @author Aze
 */
@Configuration
public class GrpcClientConfig {

    /**
     * 用户服务gRPC阻塞式Stub
     * 使用@GrpcClient注解指定目标服务名（与Nacos注册名一致）
     * <p>调用示例：
     * <pre>
     * UserResponse response = userStub.getUserByUsername(
     *     GetUserByUsernameRequest.newBuilder().setUsername("admin").build());
     * </pre>
     */
    @GrpcClient("tenant-auth")
    private UserGrpcServiceGrpc.UserGrpcServiceBlockingStub userStub;

    /**
     * 将gRPC Stub注册为Bean，供业务代码注入使用
     *
     * @return 用户服务gRPC阻塞式Stub
     */
    @Bean
    public UserGrpcServiceGrpc.UserGrpcServiceBlockingStub userGrpcStub() {
        return userStub;
    }
}
