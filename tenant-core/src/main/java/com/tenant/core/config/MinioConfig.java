package com.tenant.core.config;

import io.minio.MinioClient;
import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO 对象存储配置类
 * <p>通过 {@code minio.*} 配置属性连接 MinIO 服务，提供 {@link MinioClient} Bean
 * <p>仅在 {@code minio.url} 配置存在时激活（ConditionalOnProperty），无配置时不创建客户端
 * <p>租户隔离策略：每个租户的文件存储在独立 Bucket 中，Bucket 命名规则为 {@code tenant-{tenantCode}}
 *
 * @author Aze
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "minio")
@ConditionalOnProperty(prefix = "minio", name = "url")
public class MinioConfig {

    /**
     * MinIO 服务地址（如 http://127.0.0.1:9000）
     */
    private String url;

    /**
     * 访问密钥
     */
    private String user;

    /**
     * 密钥
     */
    private String password;

    /**
     * 默认 Bucket 名称（未指定租户时使用）
     */
    private String defaultBucket = "tenant-default";

    /**
     * 文件大小上限（字节），默认 100MB
     */
    private long maxFileSize = 100 * 1024 * 1024;

    /**
     * 创建 MinIO 客户端
     *
     * @return MinioClient 实例
     */
    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(url)
                .credentials(user, password)
                .build();
    }
}
