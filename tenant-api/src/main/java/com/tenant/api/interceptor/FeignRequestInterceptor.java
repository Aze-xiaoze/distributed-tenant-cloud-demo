package com.tenant.api.interceptor;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Feign请求拦截器
 * 将当前HTTP请求中的Authorization（JWT令牌）和X-Tenant-ID（租户ID）请求头
 * 自动透传到Feign远程调用的下游服务，确保微服务间调用时认证和租户信息不丢失
 * <p>工作原理：通过 {@link RequestContextHolder} 获取当前线程绑定的Servlet请求，
 * 提取请求头后写入Feign的 {@link RequestTemplate} 中
 * <p><b>注意</b>：此拦截器仅在Servlet容器线程中有效，异步线程中需额外传递RequestAttributes
 *
 * @author Aze
 */
@Component
public class FeignRequestInterceptor implements RequestInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String TENANT_HEADER = "X-Tenant-ID";

    @Override
    public void apply(RequestTemplate template) {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();

            // 透传Authorization头（JWT令牌）
            String authorization = request.getHeader(AUTHORIZATION_HEADER);
            if (authorization != null) {
                template.header(AUTHORIZATION_HEADER, authorization);
            }

            // 透传租户ID头
            String tenantId = request.getHeader(TENANT_HEADER);
            if (tenantId != null) {
                template.header(TENANT_HEADER, tenantId);
            }
        }
    }
}
