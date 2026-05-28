package com.tenant.system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * tenant-system 系统管理服务启动类
 * 负责用户、角色、菜单等系统管理功能
 * <p>注解说明：
 * <ul>
 *   <li>{@link SpringBootApplication} — Spring Boot自动配置</li>
 *   <li>{@link EnableDiscoveryClient} — 向Nacos注册服务实例</li>
 *   <li>{@link EnableScheduling} — 启用定时任务调度</li>
 *   <li>{@link EnableAsync} — 启用异步方法执行（邮件发送等）</li>
 * </ul>
 * <p>服务间通信已从Feign切换为gRPC，通过net.devh grpc-spring-boot-starter自动配置gRPC客户端
 *
 * @author Aze
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
@EnableAsync
public class TenantSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(TenantSystemApplication.class, args);
    }

}