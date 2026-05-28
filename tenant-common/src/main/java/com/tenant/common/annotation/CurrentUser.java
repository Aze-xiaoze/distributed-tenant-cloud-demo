package com.tenant.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 当前登录用户注入注解
 * <p>标注在 Controller 方法参数上，由 {@link com.tenant.core.resolver.CurrentUserArgumentResolver}
 * 自动从 Spring Security 上下文中提取当前登录用户信息并注入
 * <p>使用示例：
 * <pre>
 *     @GetMapping("/profile")
 *     public Result&lt;Object&gt; getProfile(@CurrentUser LoginUser currentUser) {
 *         return Result.success(currentUser);
 *     }
 * </pre>
 * <p>注入的 {@link com.tenant.common.vo.LoginUser} 包含：
 * <ul>
 *   <li>{@code username} - 用户名</li>
 *   <li>{@code tenantId} - 租户ID</li>
 *   <li>{@code roles} - 角色集合</li>
 *   <li>{@code permissions} - 权限集合</li>
 * </ul>
 *
 * @author Aze
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}