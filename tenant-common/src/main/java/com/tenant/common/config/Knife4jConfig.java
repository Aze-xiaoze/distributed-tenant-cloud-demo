package com.tenant.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j/OpenAPI3 接口文档配置
 * <p>访问地址：http://{host}:{port}/doc.html
 * <p>功能：
 * <ul>
 *   <li>API文档自动生成（基于SpringDoc + OpenAPI 3.0）</li>
 *   <li>支持Bearer Token认证（在Authorize按钮中输入JWT）</li>
 *   <li>接口分组（按Controller自动分组）</li>
 * </ul>
 * <p>各服务在application.yml中自定义 service.name 和 service.version
 *
 * @author Aze
 */
@Configuration
public class Knife4jConfig {

    /**
     * API基本信息
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("分布式多租户企业级平台 API")
                        .version("1.0.0")
                        .description("基于Spring Boot 3 + Spring Cloud Alibaba的分布式多租户SaaS平台API文档")
                        .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0")))
                // 全局安全要求：所有接口默认需要Bearer Token认证
                .addSecurityItem(new SecurityRequirement().addList("Bearer Token"))
                // 定义Bearer Token安全方案
                .schemaRequirement("Bearer Token", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .in(SecurityScheme.In.HEADER)
                        .name("Authorization")
                        .description("请输入JWT令牌（格式：Bearer {token}）"));
    }
}