package com.tenant.common.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;

/**
 * Spring Security 统一响应工具类
 * <p>提取认证入口（AuthenticationEntryPoint）和访问拒绝处理器（AccessDeniedHandler）的通用逻辑，
 * 避免各微服务重复编写相同的 JSON 响应代码
 * <p>使用示例：
 * <pre>{@code
 * .exceptionHandling(exceptions -> exceptions
 *     .authenticationEntryPoint(SecurityResponseUtil.unauthorizedEntryPoint())
 *     .accessDeniedHandler(SecurityResponseUtil.accessDeniedHandler())
 * )
 * }</pre>
 *
 * @author Aze
 */
public final class SecurityResponseUtil {

    private SecurityResponseUtil() {
        throw new UnsupportedOperationException("工具类禁止实例化");
    }

    /**
     * 未认证响应入口
     * <p>返回 401 状态码和标准 JSON 响应格式
     *
     * @return AuthenticationEntryPoint 实例
     */
    public static AuthenticationEntryPoint unauthorizedEntryPoint() {
        return (request, response, authException) ->
                writeJsonResponse(response, 401, "未认证，请先登录");
    }

    /**
     * 未认证响应入口（自定义消息）
     *
     * @param message 自定义错误消息
     * @return AuthenticationEntryPoint 实例
     */
    public static AuthenticationEntryPoint unauthorizedEntryPoint(String message) {
        return (request, response, authException) ->
                writeJsonResponse(response, 401, message);
    }

    /**
     * 无权限访问处理器
     * <p>返回 403 状态码和标准 JSON 响应格式
     *
     * @return AccessDeniedHandler 实例
     */
    public static AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) ->
                writeJsonResponse(response, 403, "无权限访问");
    }

    /**
     * 无权限访问处理器（自定义消息）
     *
     * @param message 自定义错误消息
     * @return AccessDeniedHandler 实例
     */
    public static AccessDeniedHandler accessDeniedHandler(String message) {
        return (request, response, accessDeniedException) ->
                writeJsonResponse(response, 403, message);
    }

    /**
     * 写入标准 JSON 响应
     *
     * @param response  HTTP 响应对象
     * @param statusCode HTTP 状态码
     * @param message    错误消息
     * @throws IOException 写入异常
     */
    private static void writeJsonResponse(HttpServletResponse response, int statusCode, String message) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(statusCode);
        response.getWriter().write(
                "{\"code\":" + statusCode +
                        ",\"message\":\"" + message + "\"" +
                        ",\"timestamp\":" + System.currentTimeMillis() + "}"
        );
    }
}
