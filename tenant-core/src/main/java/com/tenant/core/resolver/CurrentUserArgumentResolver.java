package com.tenant.core.resolver;

import com.tenant.common.annotation.CurrentUser;
import com.tenant.common.vo.LoginUserVO;
import com.tenant.core.tenant.TenantContextHolder;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.HashSet;
import java.util.Set;

/**
 * 当前登录用户参数解析器
 * <p>自动从 Spring Security 上下文和租户上下文中提取用户信息，
 * 封装为 {@link LoginUserVO} DTO 注入到标注 {@link CurrentUser} 的 Controller 方法参数中
 * <p>提取逻辑：
 * <ul>
 *   <li>{@code username} → Authentication.getPrincipal()（JWT subject）</li>
 *   <li>{@code tenantId} → TenantContextHolder.getCurrentTenantId()</li>
 *   <li>{@code roles} → Authentication.authorities 中 ROLE_ 开头的，去除前缀</li>
 *   <li>{@code permissions} → Authentication.authorities 中非 ROLE_ 开头的</li>
 * </ul>
 *
 * @author Aze
 */
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String ROLE_PREFIX = "ROLE_";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && parameter.getParameterType().equals(LoginUserVO.class);
    }

    @Override
    public Object resolveArgument(@NotNull MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  @NotNull NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        LoginUserVO loginUserVO = new LoginUserVO();

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            loginUserVO.setAuthenticated(true);
            loginUserVO.setUsername(authentication.getName());

            // 从 TenantContextHolder 获取租户ID（JWT Filter 已设置）
            loginUserVO.setTenantId(TenantContextHolder.getCurrentTenantId());

            // 解析角色和权限
            Set<String> roles = new HashSet<>();
            Set<String> permissions = new HashSet<>();
            authentication.getAuthorities().forEach(authority -> {
                String auth = authority.getAuthority();
                if (auth.startsWith(ROLE_PREFIX)) {
                    roles.add(auth.substring(ROLE_PREFIX.length()));
                } else {
                    permissions.add(auth);
                }
            });
            loginUserVO.setRoles(roles);
            loginUserVO.setPermissions(permissions);
        } else {
            loginUserVO.setAuthenticated(false);
        }

        return loginUserVO;
    }
}