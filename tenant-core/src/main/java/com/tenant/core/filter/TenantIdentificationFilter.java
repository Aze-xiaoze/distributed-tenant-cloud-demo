package com.tenant.core.filter;

import com.tenant.core.tenant.TenantContextHolder;
import org.springframework.stereotype.Component;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * 多租户识别过滤器
 * 从HTTP请求头中提取租户ID（X-Tenant-ID），设置到 {@link TenantContextHolder} 线程上下文中
 * <p>此过滤器作为Servlet Filter自动注册，在所有请求处理前执行，
 * 为后续的MyBatis-Plus多租户插件、业务代码提供租户上下文
 * <p>与 {@code JwtAuthenticationFilter} 的关系：
 * <ul>
 *   <li>本过滤器从请求头提取租户ID — 适用于未携带JWT的公开接口（如登录/注册）</li>
 *   <li>JwtAuthenticationFilter从JWT Token中提取租户ID — 适用于已认证接口</li>
 *   <li>两者互补，JWT中的租户ID优先级更高（后执行会覆盖）</li>
 * </ul>
 *
 * @author Aze
 */
@Component
public class TenantIdentificationFilter implements Filter {

    /**
     * 租户ID请求头名称
     */
    private static final String TENANT_HEADER_NAME = "X-Tenant-ID";

    /**
     * 执行过滤逻辑
     * 从请求头中提取租户ID并设置到上下文中
     *
     * @param request  请求对象
     * @param response 响应对象
     * @param chain    过滤器链
     * @throws IOException      IO异常
     * @throws ServletException Servlet异常
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        
        try {
            // 从请求头中获取租户ID
            String tenantId = httpRequest.getHeader(TENANT_HEADER_NAME);
            
            if (tenantId != null && !tenantId.trim().isEmpty()) {
                // 设置租户ID到上下文
                TenantContextHolder.setCurrentTenantId(tenantId);
            }
            
            // 继续执行过滤器链
            chain.doFilter(request, response);
        } finally {
            // 请求结束后清理租户上下文，防止内存泄漏
            TenantContextHolder.clear();
        }
    }
}