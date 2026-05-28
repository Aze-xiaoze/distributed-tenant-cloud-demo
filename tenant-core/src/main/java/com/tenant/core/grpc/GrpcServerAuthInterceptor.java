package com.tenant.core.grpc;

import com.tenant.common.security.JwtTokenClaims;
import com.tenant.common.security.JwtTokenParser;
import io.grpc.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * gRPC服务端JWT认证拦截器
 * <p>从gRPC Metadata中提取Authorization头，验证JWT签名的有效性，
 * 防止未授权调用直接访问gRPC服务（绕过网关的场景）
 * <p>校验流程：
 * <ol>
 *   <li>从Metadata中提取authorization</li>
 *   <li>使用JwtTokenParser验证JWT签名</li>
 *   <li>验证失败返回UNAUTHENTICATED状态码</li>
 *   <li>验证成功继续执行后续拦截器</li>
 * </ol>
 * <p>优先级：最高（@Order(Ordered.HIGHEST_PRECEDENCE)），在租户拦截器之前执行，
 * 确保只有经过JWT验证的请求才能进入租户上下文设置阶段
 * <p>注意：本拦截器仅做签名验证，不校验Token是否被吊销（黑名单检查由调用方负责）
 *
 * @author Aze
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GrpcServerAuthInterceptor implements ServerInterceptor {

    /**
     * gRPC Metadata Key：授权令牌
     */
    private static final Metadata.Key<String> AUTHORIZATION_KEY =
            Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

    @Value("${jwt.secret:defaultSecretKeyForTenantCloudPlatformThatIsAtLeast64BytesLongForHS512}")
    private String secret;

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        // 提取Authorization头
        String authHeader = headers.get(AUTHORIZATION_KEY);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("gRPC调用缺少有效的Authorization头：method={}",
                    call.getMethodDescriptor().getFullMethodName());
            call.close(Status.UNAUTHENTICATED.withDescription("缺少认证令牌"), new Metadata());
            return new ServerCall.Listener<>() {
            };
        }

        String token = authHeader.substring(7);
        JwtTokenParser parser = new JwtTokenParser(secret);
        JwtTokenClaims claims = parser.parseToken(token);

        if (claims == null) {
            log.warn("gRPC调用JWT解析失败：method={}",
                    call.getMethodDescriptor().getFullMethodName());
            call.close(Status.UNAUTHENTICATED.withDescription("无效的认证令牌"), new Metadata());
            return new ServerCall.Listener<>() {
            };
        }

        // 额外校验：RefreshToken不能用于gRPC服务调用
        if (claims.isRefreshToken()) {
            log.warn("gRPC调用禁止使用RefreshToken：method={}, username={}",
                    call.getMethodDescriptor().getFullMethodName(), claims.getUsername());
            call.close(Status.PERMISSION_DENIED.withDescription("RefreshToken不能用于服务调用"), new Metadata());
            return new ServerCall.Listener<>() {
            };
        }

        log.debug("gRPC服务端JWT认证通过：method={}, username={}",
                call.getMethodDescriptor().getFullMethodName(), claims.getUsername());

        return next.startCall(call, headers);
    }
}
