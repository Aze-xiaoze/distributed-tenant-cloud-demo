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
 * <p>透传机制：
 * <ul>
 *   <li>从Servlet请求上下文获取Authorization头和X-Tenant-ID头（适用于HTTP请求线程内调用）</li>
 *   <li>若不在HTTP请求上下文中，租户ID从X-Tenant-ID请求头获取</li>
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

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String TENANT_HEADER = "X-Tenant-ID";

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

                    // 透传租户ID头
                    String tenantId = request.getHeader(TENANT_HEADER);
                    if (tenantId != null && !tenantId.isEmpty()) {
                        headers.put(TENANT_ID_KEY, tenantId);
                    }
                }

                log.debug("gRPC客户端拦截器：透传metadata, tenantId={}, hasAuth={}",
                        headers.get(TENANT_ID_KEY), headers.get(AUTHORIZATION_KEY) != null);

                super.start(responseListener, headers);
            }
        };
    }
}
