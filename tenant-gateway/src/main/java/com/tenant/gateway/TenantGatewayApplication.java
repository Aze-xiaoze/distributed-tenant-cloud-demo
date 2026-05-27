package com.tenant.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * tenant-gateway 网关服务启动类
 * 负责路由转发、租户识别、JWT校验、跨域配置
 * <p>基于Spring Cloud Gateway（WebFlux），不引入spring-boot-starter-web
 * <p>注解说明：
 * <ul>
 *   <li>{@link SpringBootApplication} — Spring Boot自动配置</li>
 *   <li>{@link EnableDiscoveryClient} — 向Nacos注册网关服务实例</li>
 * </ul>
 *
 * @author Aze
 */
@SpringBootApplication
@EnableDiscoveryClient
public class TenantGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(TenantGatewayApplication.class, args);
    }

}