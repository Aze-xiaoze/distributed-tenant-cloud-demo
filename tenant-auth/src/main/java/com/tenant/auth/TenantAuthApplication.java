package com.tenant.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * tenant-auth 认证授权服务启动类
 * 负责用户登录、注册、JWT令牌签发与验证
 * <p>注解说明：
 * <ul>
 *   <li>{@link SpringBootApplication} — Spring Boot自动配置</li>
 *   <li>{@link EnableDiscoveryClient} — 向Nacos注册服务实例</li>
 *   <li>{@link EnableFeignClients} — 扫描com.tenant.api包下的Feign客户端</li>
 * </ul>
 *
 * @author Aze
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.tenant.api")
public class TenantAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(TenantAuthApplication.class, args);
    }

}