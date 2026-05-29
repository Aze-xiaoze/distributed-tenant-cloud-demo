package com.tenant.auth.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT配置属性
 * 从application.yml中读取jwt前缀的配置项，用于JWT令牌的生成与验证
 * <p>配置示例：
 * <pre>
 * jwt:
 *   secret: your-secret-key-at-least-64-bytes-long-for-hs512-algorithm
 *   expiration: 1800000     # AccessToken 30分钟
 *   refresh-expiration: 604800000  # RefreshToken 7天
 * </pre>
 * <p><b>安全提示</b>：生产环境必须通过环境变量 {@code JWT_SECRET} 注入密钥，禁止使用默认值
 *
 * @author Aze
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * 密钥（HS512算法要求至少64字节/512位）
     * 生产环境必须通过配置中心或环境变量注入，禁止使用默认值
     */
    private String secret = "defaultSecretKeyForTenantCloudPlatformThatIsAtLeast64BytesLongForHS512";

    /**
     * AccessToken有效时间（毫秒），默认30分钟
     * <p>AccessToken用于API鉴权，有效期较短以降低泄露风险
     */
    private Long expiration = 1800000L;

    /**
     * RefreshToken有效时间（毫秒），默认7天
     * <p>RefreshToken仅用于刷新AccessToken，不携带权限信息，
     * 有效期较长以减少用户登录频率
     */
    private Long refreshExpiration = 604800000L;
}