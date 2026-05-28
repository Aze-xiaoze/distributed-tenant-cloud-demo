package com.tenant.core.config;

import com.tenant.core.filter.MdcFilter;
import com.tenant.core.resolver.CurrentUserArgumentResolver;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Web MVC 全局配置
 * <p>统一注册 Servlet Filter、配置消息转换器等
 * <ul>
 *   <li>字符编码过滤器：强制 UTF-8，防止中文乱码</li>
 *   <li>MDC 链路追踪过滤器：注册顺序控制，确保在 CharacterEncodingFilter 之后执行</li>
 * </ul>
 *
 * @author Aze
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final CurrentUserArgumentResolver currentUserArgumentResolver;

    public WebMvcConfig(CurrentUserArgumentResolver currentUserArgumentResolver) {
        this.currentUserArgumentResolver = currentUserArgumentResolver;
    }

    /**
     * 注册字符编码过滤器
     * <p>强制请求和响应使用 UTF-8 编码，解决中文乱码问题
     * <p>优先级最高（Ordered.HIGHEST_PRECEDENCE），确保最先执行
     */
    @Bean
    public FilterRegistrationBean<jakarta.servlet.Filter> characterEncodingFilter() {
        FilterRegistrationBean<jakarta.servlet.Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new org.springframework.web.filter.CharacterEncodingFilter("UTF-8", true, true));
        registration.addUrlPatterns("/*");
        registration.setName("characterEncodingFilter");
        registration.setOrder(org.springframework.core.Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    /**
     * 注册 MDC 链路追踪过滤器
     * <p>将请求头中的 X-Request-Id 写入 SLF4J MDC，实现日志链路追踪
     * <p>优先级次于字符编码过滤器，确保请求编码正确后再处理
     *
     * @param mdcFilter MDC 过滤器实例
     * @return FilterRegistrationBean
     */
    @Bean
    public FilterRegistrationBean<jakarta.servlet.Filter> mdcFilterRegistration(MdcFilter mdcFilter) {
        FilterRegistrationBean<jakarta.servlet.Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(mdcFilter);
        registration.addUrlPatterns("/*");
        registration.setName("mdcFilter");
        registration.setOrder(org.springframework.core.Ordered.HIGHEST_PRECEDENCE + 1);
        return registration;
    }

    /**
     * 注册自定义参数解析器
     * <p>将 {@link CurrentUserArgumentResolver} 注册到 Spring MVC，
     * 使 Controller 中标注 {@link com.tenant.common.annotation.CurrentUser} 的参数自动注入当前用户
     */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserArgumentResolver);
    }
}
