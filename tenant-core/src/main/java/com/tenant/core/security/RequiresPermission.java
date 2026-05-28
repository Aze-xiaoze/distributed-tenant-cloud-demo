package com.tenant.core.security;

import java.lang.annotation.*;

/**
 * 权限校验注解
 * 标注在Controller方法上，声明所需的权限标识，由{@link PermissionAspect}切面自动校验
 * <p>权限标识格式：{@code 模块:资源:操作}，与sys_menu表的permission字段对应
 * <p>使用示例：
 * <pre>
 * &#64;RequiresPermission("system:user:add")
 * &#64;PostMapping("/add")
 * public Result&lt;String&gt; addUser() { ... }
 * </pre>
 * <p>权限来源：从请求头X-User-Permissions中获取（由网关从JWT解析后设置），
 * 或从Spring Security上下文的GrantedAuthority中提取
 * <p>校验逻辑：当前用户必须拥有注解中指定的权限标识，否则抛出403异常
 *
 * @author Aze
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresPermission {

    /**
     * 所需权限标识
     * <p>支持多个权限，默认要求全部满足（AND关系）
     */
    String[] value();

    /**
     * 多个权限之间的逻辑关系
     * <p>true=任一满足即可（OR），false=全部满足才可（AND，默认）
     */
    boolean logicalOr() default false;
}
