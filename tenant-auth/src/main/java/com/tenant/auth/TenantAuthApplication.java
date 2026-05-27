package com.tenant.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * tenant-auth 认证授权服务启动类
 * 负责用户登录、注册、JWT令牌签发与验证
 * <p>注解说明：
 * <ul>
 *   <li>{@link SpringBootApplication} — Spring Boot自动配置</li>
 *   <li>{@link EnableDiscoveryClient} — 向Nacos注册服务实例</li>
 * </ul>
 * <p>服务间通信已从Feign切换为gRPC，通过net.devh grpc-spring-boot-starter自动配置gRPC服务端
 *
 * @author Aze
 */
@SpringBootApplication
@EnableDiscoveryClient
public class TenantAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(TenantAuthApplication.class, args);
    }

}