package com.tenant.core.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * MDC 链路追踪过滤器
 * <p>从 HTTP 请求头中提取 {@code X-Request-Id}（由网关 {@link com.tenant.gateway.filter.TraceIdGatewayFilter} 生成/透传），
 * 写入 SLF4J MDC 上下文，使日志中可通过 {@code %X{requestId}} 输出链路 ID，实现跨服务日志关联排查
 * <p>清理机制：请求结束后从 MDC 中移除 requestId，防止线程复用时污染后续请求
 * <p>注册方式：由 {@link com.tenant.core.config.WebMvcConfig} 通过 FilterRegistrationBean 统一注册
 *
 * @author Aze
 */
@Component
public class MdcFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(MdcFilter.class);

    /**
     * 链路追踪 ID 请求头名称（与网关过滤器保持一致）
     */
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    /**
     * MDC 中存储链路 ID 的键名
     */
    public static final String REQUEST_ID_MDC_KEY = "requestId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String requestId = httpRequest.getHeader(REQUEST_ID_HEADER);

        try {
            if (requestId != null && !requestId.isEmpty()) {
                MDC.put(REQUEST_ID_MDC_KEY, requestId);
                log.debug("MDC 设置链路 ID: {}", requestId);
            }
            chain.doFilter(request, response);
        } finally {
            // 清理 MDC，防止线程复用导致日志污染
            MDC.remove(REQUEST_ID_MDC_KEY);
        }
    }
}
