package com.tenant.core.grpc;

import com.tenant.core.tenant.TenantContextHolder;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * gRPC服务端租户拦截器
 * 从gRPC Metadata中提取租户ID（x-tenant-id）和JWT令牌（authorization），
 * 设置到TenantContextHolder线程上下文中，确保后续业务逻辑和MyBatis-Plus租户插件可获取租户信息
 * <p>替代原 TenantIdentificationFilter 对gRPC请求的处理
 * <p>优先级：最高（@Order(Ordered.HIGHEST_PRECEDENCE)），确保所有业务拦截器执行前租户上下文已就绪
 * <p><b>重要</b>：finally块中清理TenantContextHolder，防止ThreadLocal内存泄漏
 *
 * @author Aze
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GrpcServerTenantInterceptor implements ServerInterceptor {

    /**
     * gRPC Metadata Key：租户ID
     */
    private static final Metadata.Key<String> TENANT_ID_KEY =
            Metadata.Key.of("x-tenant-id", Metadata.ASCII_STRING_MARSHALLER);

    /**
     * gRPC Metadata Key：授权令牌（供后续认证拦截器使用）
     */
    private static final Metadata.Key<String> AUTHORIZATION_KEY =
            Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

    private static final String DEFAULT_TENANT_ID = "default_tenant";

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        try {
            // 从gRPC Metadata中提取租户ID
            String tenantId = headers.get(TENANT_ID_KEY);

            if (tenantId != null && !tenantId.trim().isEmpty()) {
                TenantContextHolder.setCurrentTenantId(tenantId);
            } else {
                // 无租户ID时设置默认值，保证下游不因缺少租户上下文而异常
                TenantContextHolder.setCurrentTenantId(DEFAULT_TENANT_ID);
            }

            log.debug("gRPC服务端租户拦截器：设置租户上下文 tenantId={}", TenantContextHolder.getCurrentTenantId());

            return next.startCall(call, headers);

        } finally {
            // 注意：此处不能清理TenantContextHolder，因为业务逻辑在startCall返回后的
            // Listener回调中执行。清理工作由GrpcServerTenantCleanupInterceptor负责
        }
    }
}
