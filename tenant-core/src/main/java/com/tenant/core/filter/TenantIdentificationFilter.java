package com.tenant.core.filter;

import com.tenant.core.tenant.TenantContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * 多租户识别过滤器
 * 从HTTP请求头中提取租户ID（X-Tenant-ID），设置到 {@link TenantContextHolder} 线程上下文中
 * <p>此过滤器作为Servlet Filter自动注册，在所有请求处理前执行，
 * 为后续的MyBatis-Plus多租户插件、业务代码提供租户上下文
 * <p><b>安全机制</b>：检查X-Tenant-Verified请求头判断租户ID来源
 * <ul>
 *   <li>X-Tenant-Verified=true：租户ID由网关从JWT Claims中提取（可信），正常设置上下文</li>
 *   <li>X-Tenant-Verified不存在：租户ID可能来自客户端请求头（不可信），记录安全警告，
 *       可能表示请求绕过了网关直接访问下游服务</li>
 * </ul>
 * <p><b>ThreadLocal清理责任</b>：本过滤器是唯一负责清理TenantContextHolder的地方，
 * 其他过滤器（如JwtAuthenticationFilter）只负责设置，不清理，避免重复清理导致后续代码取不到值
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

    private static final Logger log = LoggerFactory.getLogger(TenantIdentificationFilter.class);

    /**
     * 租户ID请求头名称
     */
    private static final String TENANT_HEADER_NAME = "X-Tenant-ID";

    /**
     * 租户ID来源验证标记头（由网关AuthGatewayFilter设置）
     */
    private static final String TENANT_VERIFIED_HEADER = "X-Tenant-Verified";

    /**
     * 执行过滤逻辑
     * 从请求头中提取租户ID并设置到上下文中，同时检查租户ID来源是否可信
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
                // 安全检查：验证租户ID来源
                String verified = httpRequest.getHeader(TENANT_VERIFIED_HEADER);
                if (!"true".equals(verified)) {
                    // 租户ID未经JWT验证，可能表示请求绕过了网关
                    log.warn("安全警告：租户ID未经JWT验证，可能绕过了网关。tenantId={}, path={}",
                            tenantId, httpRequest.getRequestURI());
                }
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