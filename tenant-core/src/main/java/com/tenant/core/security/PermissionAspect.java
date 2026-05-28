package com.tenant.core.security;

import com.tenant.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 权限校验切面
 * 拦截标注{@link RequiresPermission}注解的Controller方法，校验当前用户是否拥有所需权限
 * <p>权限来源（按优先级）：
 * <ol>
 *   <li>Spring Security上下文中的GrantedAuthority（由JWT过滤器设置）</li>
 *   <li>HTTP请求头X-User-Permissions（由网关从JWT解析后设置，逗号分隔）</li>
 * </ol>
 * <p>校验逻辑：
 * <ul>
 *   <li>logicalOr=false（默认）：必须拥有所有指定权限（AND关系）</li>
 *   <li>logicalOr=true：只需拥有任一指定权限（OR关系）</li>
 * </ul>
 * <p>超级管理员（ROLE_SUPER_ADMIN）自动拥有所有权限，无需校验
 *
 * @author Aze
 */
@Aspect
@Component
public class PermissionAspect {

    private static final Logger log = LoggerFactory.getLogger(PermissionAspect.class);

    /**
     * 超级管理员角色，自动拥有所有权限
     */
    private static final String SUPER_ADMIN_ROLE = com.tenant.common.constant.TenantConstants.ROLE_SUPER_ADMIN;

    /**
     * 请求头中的权限标识Key（由网关从JWT解析后设置）
     */
    private static final String PERMISSIONS_HEADER = "X-User-Permissions";

    /**
     * 环绕通知：拦截{@link RequiresPermission}注解标注的方法
     *
     * @param joinPoint 切点
     * @return 方法执行结果
     * @throws Throwable 方法执行异常或权限校验失败
     */
    @Around("@annotation(com.tenant.core.security.RequiresPermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取方法上的注解
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequiresPermission annotation = method.getAnnotation(RequiresPermission.class);

        if (annotation == null) {
            return joinPoint.proceed();
        }

        String[] requiredPermissions = annotation.value();
        boolean logicalOr = annotation.logicalOr();

        // 获取当前用户的权限集合
        Set<String> userPermissions = getCurrentUserPermissions();

        // 超级管理员直接放行
        if (userPermissions.contains(SUPER_ADMIN_ROLE)) {
            return joinPoint.proceed();
        }

        // 校验权限
        boolean hasPermission;
        if (logicalOr) {
            // OR关系：任一权限满足即可
            hasPermission = Arrays.stream(requiredPermissions)
                    .anyMatch(userPermissions::contains);
        } else {
            // AND关系：所有权限必须全部满足
            hasPermission = Arrays.stream(requiredPermissions)
                    .allMatch(userPermissions::contains);
        }

        if (!hasPermission) {
            String username = getCurrentUsername();
            log.warn("权限校验失败：username={}, required={}, actual={}, method={}",
                    username, Arrays.toString(requiredPermissions), userPermissions,
                    method.getName());
            throw new BusinessException(403, "无权限访问，需要权限：" + String.join(",", requiredPermissions));
        }

        return joinPoint.proceed();
    }

    /**
     * 获取当前用户的权限集合
     * <p>优先从Spring Security上下文获取，其次从请求头获取
     *
     * @return 权限标识集合
     */
    private Set<String> getCurrentUserPermissions() {
        // 1. 从Spring Security上下文获取
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getAuthorities() != null) {
            Set<String> permissions = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toSet());

            // 如果Security上下文中有非ROLE_前缀的权限，说明角色已正确设置
            boolean hasNonRolePermissions = permissions.stream()
                    .anyMatch(p -> !p.startsWith("ROLE_"));
            if (hasNonRolePermissions || permissions.contains(SUPER_ADMIN_ROLE)) {
                return permissions;
            }
        }

        // 2. 从请求头获取（网关设置的X-User-Permissions）
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String permissionsHeader = request.getHeader(PERMISSIONS_HEADER);
            if (permissionsHeader != null && !permissionsHeader.isEmpty()) {
                return Arrays.stream(permissionsHeader.split(","))
                        .map(String::trim)
                        .filter(p -> !p.isEmpty())
                        .collect(Collectors.toSet());
            }
        }

        return Set.of();
    }

    /**
     * 获取当前用户名
     *
     * @return 用户名
     */
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getName() != null) {
            return authentication.getName();
        }
        return "anonymous";
    }
}