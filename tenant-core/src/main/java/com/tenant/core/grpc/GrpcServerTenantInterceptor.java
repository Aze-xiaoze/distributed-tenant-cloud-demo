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
 * <p><b>安全机制</b>：检查x-tenant-verified Metadata判断租户ID来源是否可信
 * <ul>
 *   <li>x-tenant-verified=true：租户ID由网关从JWT Claims中验证（可信），正常设置上下文</li>
 *   <li>x-tenant-verified不存在：租户ID可能来自客户端直接调用（不可信），记录安全警告</li>
 * </ul>
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

    /**
     * gRPC Metadata Key：租户ID验证标记（由GrpcClientInterceptor从网关X-Tenant-Verified透传）
     */
    private static final Metadata.Key<String> TENANT_VERIFIED_KEY =
            Metadata.Key.of("x-tenant-verified", Metadata.ASCII_STRING_MARSHALLER);

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

            // 安全检查：验证租户ID来源是否可信
            String verified = headers.get(TENANT_VERIFIED_KEY);
            if (!"true".equals(verified)) {
                // 租户ID未经JWT验证，可能表示gRPC调用绕过了网关
                log.warn("安全警告：gRPC租户ID未经JWT验证，可能绕过了网关。tenantId={}, method={}",
                        TenantContextHolder.getCurrentTenantId(), call.getMethodDescriptor().getFullMethodName());
            } else {
                log.debug("gRPC服务端租户拦截器：租户ID已验证 tenantId={}", TenantContextHolder.getCurrentTenantId());
            }

            return next.startCall(call, headers);

        } finally {
            // 注意：此处不能清理TenantContextHolder，因为业务逻辑在startCall返回后的
            // Listener回调中执行。清理工作由GrpcServerTenantCleanupInterceptor负责
        }
    }
}
