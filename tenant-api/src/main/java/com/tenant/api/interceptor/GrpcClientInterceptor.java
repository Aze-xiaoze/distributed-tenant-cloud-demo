package com.tenant.api.interceptor;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * gRPC客户端拦截器
 * 替代原 FeignRequestInterceptor，将JWT令牌和租户ID通过gRPC Metadata透传到下游服务
 * <p><b>安全机制</b>：优先从TenantContextHolder获取已验证的租户ID（由JWT过滤器设置），
 * 其次从HTTP请求头获取（网关已验证的X-Tenant-ID）。同时透传X-Tenant-Verified标记，
 * 让gRPC服务端知道租户ID来源是否可信
 * <p>透传机制：
 * <ul>
 *   <li>租户ID：优先TenantContextHolder → 其次X-Tenant-ID请求头</li>
 *   <li>授权令牌：从Authorization请求头透传</li>
 *   <li>租户验证标记：从X-Tenant-Verified请求头透传</li>
 * </ul>
 * <p><b>注意</b>：异步线程中需额外传递RequestAttributes
 *
 * @author Aze
 */
@Slf4j
@Component
public class GrpcClientInterceptor implements ClientInterceptor {

    /**
     * gRPC Metadata Key：授权令牌
     */
    private static final Metadata.Key<String> AUTHORIZATION_KEY =
            Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

    /**
     * gRPC Metadata Key：租户ID
     */
    private static final Metadata.Key<String> TENANT_ID_KEY =
            Metadata.Key.of("x-tenant-id", Metadata.ASCII_STRING_MARSHALLER);

    /**
     * gRPC Metadata Key：租户ID验证标记（与网关X-Tenant-Verified对应）
     */
    private static final Metadata.Key<String> TENANT_VERIFIED_KEY =
            Metadata.Key.of("x-tenant-verified", Metadata.ASCII_STRING_MARSHALLER);

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String TENANT_HEADER = "X-Tenant-ID";
    private static final String TENANT_VERIFIED_HEADER = "X-Tenant-Verified";

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions,
            Channel next) {

        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
                next.newCall(method, callOptions)) {

            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                // 从Servlet请求上下文透传（HTTP请求线程内调用场景）
                ServletRequestAttributes attributes =
                        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

                if (attributes != null) {
                    HttpServletRequest request = attributes.getRequest();

                    // 透传Authorization头（JWT令牌）
                    String authorization = request.getHeader(AUTHORIZATION_HEADER);
                    if (authorization != null && !authorization.isEmpty()) {
                        headers.put(AUTHORIZATION_KEY, authorization);
                    }

                    // 透传租户ID：优先使用X-Tenant-ID请求头（网关已验证），保证租户ID一致性
                    String tenantId = request.getHeader(TENANT_HEADER);
                    if (tenantId != null && !tenantId.isEmpty()) {
                        headers.put(TENANT_ID_KEY, tenantId);
                    }

                    // 透传租户验证标记：让gRPC服务端知道租户ID来源是否可信
                    String verified = request.getHeader(TENANT_VERIFIED_HEADER);
                    if (verified != null && !verified.isEmpty()) {
                        headers.put(TENANT_VERIFIED_KEY, verified);
                    }
                }

                log.debug("gRPC客户端拦截器：透传metadata, tenantId={}, verified={}, hasAuth={}",
                        headers.get(TENANT_ID_KEY), headers.get(TENANT_VERIFIED_KEY),
                        headers.get(AUTHORIZATION_KEY) != null);

                super.start(responseListener, headers);
            }
        };
    }
}
