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
 * gRPC服务端租户上下文清理拦截器
 * 在gRPC调用完成后清理TenantContextHolder，防止ThreadLocal内存泄漏
 * <p>执行顺序：最低优先级（@Order(Ordered.LOWEST_PRECEDENCE)），确保在所有业务逻辑完成后执行清理
 * <p>工作原理：包装ServerCall.Listener，在onComplete和onCancel回调中清理租户上下文
 *
 * @author Aze
 */
@Slf4j
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class GrpcServerTenantCleanupInterceptor implements ServerInterceptor {

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        ServerCall.Listener<ReqT> delegate = next.startCall(call, headers);

        return new ServerCall.Listener<ReqT>() {
            @Override
            public void onComplete() {
                try {
                    delegate.onComplete();
                } finally {
                    TenantContextHolder.clear();
                    log.debug("gRPC服务端租户清理拦截器：已清理租户上下文");
                }
            }

            @Override
            public void onCancel() {
                try {
                    delegate.onCancel();
                } finally {
                    TenantContextHolder.clear();
                    log.debug("gRPC服务端租户清理拦截器：调用取消，已清理租户上下文");
                }
            }

            @Override
            public void onMessage(ReqT message) {
                delegate.onMessage(message);
            }

            @Override
            public void onHalfClose() {
                delegate.onHalfClose();
            }

            @Override
            public void onReady() {
                delegate.onReady();
            }
        };
    }
}
