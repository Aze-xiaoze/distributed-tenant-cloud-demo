package com.tenant.auth.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT配置属性
 * 从application.yml中读取jwt前缀的配置项，用于JWT令牌的生成与验证
 * <p>配置示例：
 * <pre>
 * jwt:
 *   secret: your-secret-key-at-least-64-bytes-long-for-hs512-algorithm
 *   expiration: 3600000  # 1小时，单位毫秒
 * </pre>
 * <p><b>安全提示</b>：生产环境必须通过环境变量 {@code JWT_SECRET} 注入密钥，禁止使用默认值
 *
 * @author Aze
 */
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * 密钥（HS512算法要求至少64字节/512位）
     * 生产环境必须通过配置中心或环境变量注入，禁止使用默认值
     */
    private String secret = "defaultSecretKeyForTenantCloudPlatformThatIsAtLeast64BytesLongForHS512";

    /**
     * 有效时间（毫秒）
     */
    private Long expiration = 3600000L; // 默认1小时

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public Long getExpiration() {
        return expiration;
    }

    public void setExpiration(Long expiration) {
        this.expiration = expiration;
    }
}