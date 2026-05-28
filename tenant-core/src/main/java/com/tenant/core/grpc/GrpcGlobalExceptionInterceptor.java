package com.tenant.core.grpc;

import io.grpc.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * gRPC全局异常拦截器
 * 统一捕获gRPC服务端业务方法抛出的未处理异常，转换为标准gRPC Status码返回给客户端
 * <p>错误码映射规则：
 * <ul>
 *   <li>{@link IllegalArgumentException} → {@link Status.Code#INVALID_ARGUMENT}</li>
 *   <li>{@link IllegalStateException} / {@link UnsupportedOperationException} → {@link Status.Code#FAILED_PRECONDITION}</li>
 *   <li>{@link SecurityException} → {@link Status.Code#PERMISSION_DENIED}</li>
 *   <li>{@link java.util.NoSuchElementException} / {@link jakarta.persistence.EntityNotFoundException} → {@link Status.Code#NOT_FOUND}</li>
 *   <li>其他运行时异常 → {@link Status.Code#INTERNAL}</li>
 * </ul>
 * <p>优先级：最低（最后执行，最先包装），确保能捕获所有上游拦截器和业务方法抛出的异常
 *
 * @author Aze
 */
@Slf4j
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class GrpcGlobalExceptionInterceptor implements ServerInterceptor {

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        ServerCall<ReqT, RespT> wrappedCall = new ForwardingServerCall.SimpleForwardingServerCall<>(call) {
            @Override
            public void close(Status status, Metadata trailers) {
                if (!status.isOk() && status.getCause() != null) {
                    log.warn("gRPC调用异常：method={}, status={}, cause={}",
                            call.getMethodDescriptor().getFullMethodName(), status.getCode(), status.getCause().getMessage());
                }
                super.close(status, trailers);
            }
        };

        ServerCall.Listener<ReqT> listener;
        try {
            listener = next.startCall(wrappedCall, headers);
        } catch (Exception e) {
            log.error("gRPC startCall异常：method={}", call.getMethodDescriptor().getFullMethodName(), e);
            wrappedCall.close(toGrpcStatus(e), new Metadata());
            return new ServerCall.Listener<>() {
            };
        }

        return new ExceptionHandlingListener<>(listener, wrappedCall);
    }

    /**
     * 将Java异常转换为标准gRPC Status
     */
    static Status toGrpcStatus(Exception e) {
        Status status;
        if (e instanceof IllegalArgumentException) {
            status = Status.INVALID_ARGUMENT.withDescription(e.getMessage()).withCause(e);
        } else if (e instanceof IllegalStateException || e instanceof UnsupportedOperationException) {
            status = Status.FAILED_PRECONDITION.withDescription(e.getMessage()).withCause(e);
        } else if (e instanceof SecurityException) {
            status = Status.PERMISSION_DENIED.withDescription(e.getMessage()).withCause(e);
        } else if (e instanceof java.util.NoSuchElementException
                || isEntityNotFound(e)) {
            status = Status.NOT_FOUND.withDescription(e.getMessage()).withCause(e);
        } else if (e instanceof io.grpc.StatusRuntimeException) {
            status = ((io.grpc.StatusRuntimeException) e).getStatus();
        } else {
            status = Status.INTERNAL.withDescription("服务器内部错误").withCause(e);
        }
        return status;
    }

    private static boolean isEntityNotFound(Exception e) {
        return e.getClass().getSimpleName().contains("EntityNotFound")
                || e.getClass().getSimpleName().contains("NotFound");
    }

    /**
     * 包装Listener，在消息处理和方法执行阶段捕获异常
     */
    private static class ExceptionHandlingListener<ReqT> extends ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT> {

        private final ServerCall<?, ?> call;

        ExceptionHandlingListener(ServerCall.Listener<ReqT> delegate, ServerCall<?, ?> call) {
            super(delegate);
            this.call = call;
        }

        @Override
        public void onHalfClose() {
            try {
                super.onHalfClose();
            } catch (Exception e) {
                log.error("gRPC onHalfClose异常：method={}", call.getMethodDescriptor().getFullMethodName(), e);
                call.close(toGrpcStatus(e), new Metadata());
            }
        }

        @Override
        public void onMessage(ReqT message) {
            try {
                super.onMessage(message);
            } catch (Exception e) {
                log.error("gRPC onMessage异常：method={}", call.getMethodDescriptor().getFullMethodName(), e);
                call.close(toGrpcStatus(e), new Metadata());
            }
        }

        @Override
        public void onReady() {
            try {
                super.onReady();
            } catch (Exception e) {
                log.error("gRPC onReady异常：method={}", call.getMethodDescriptor().getFullMethodName(), e);
                call.close(toGrpcStatus(e), new Metadata());
            }
        }
    }
}
