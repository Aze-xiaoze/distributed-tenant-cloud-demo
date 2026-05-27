package com.tenant.system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * tenant-system 系统管理服务启动类
 * 负责用户、角色、菜单等系统管理功能
 * <p>注解说明：
 * <ul>
 *   <li>{@link SpringBootApplication} — Spring Boot自动配置</li>
 *   <li>{@link EnableDiscoveryClient} — 向Nacos注册服务实例</li>
 * </ul>
 * <p>服务间通信已从Feign切换为gRPC，通过net.devh grpc-spring-boot-starter自动配置gRPC客户端
 *
 * @author Aze
 */
@SpringBootApplication
@EnableDiscoveryClient
public class TenantSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(TenantSystemApplication.class, args);
    }

}