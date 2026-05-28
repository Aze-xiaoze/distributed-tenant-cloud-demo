package com.tenant.core.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 字段自动填充处理器
 * <p>对实体类的公共字段进行自动赋值，避免每个业务方法手动设置：
 * <ul>
 *   <li><b>插入时自动填充</b>：createTime、updateTime</li>
 *   <li><b>更新时自动填充</b>：updateTime</li>
 * </ul>
 * <p>使用 {@link com.baomidou.mybatisplus.annotation.FieldFill} 注解标记需要自动填充的字段，
 * 例如：{@code @TableField(fill = FieldFill.INSERT)}
 * <p>对于 createBy/updateBy 字段，若实体中存在则自动填充，不存在则安全跳过
 *
 * @author Aze
 */
@Slf4j
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        log.debug("自动填充插入字段: {}", metaObject.getOriginalObject().getClass().getSimpleName());

        // 严格填充：字段存在且值为 null 时才填充
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());

        // 可选字段：createBy / updateBy（若实体中存在则填充）
        this.strictInsertFill(metaObject, "createBy", String.class, getCurrentUsername());
        this.strictInsertFill(metaObject, "updateBy", String.class, getCurrentUsername());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        log.debug("自动填充更新字段: {}", metaObject.getOriginalObject().getClass().getSimpleName());

        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        this.strictUpdateFill(metaObject, "updateBy", String.class, getCurrentUsername());
    }

    /**
     * 获取当前操作用户名
     * <p>优先从 Spring Security 上下文中获取，获取不到则返回 "system"
     *
     * @return 当前用户名或 system
     */
    private String getCurrentUsername() {
        try {
            org.springframework.security.core.Authentication authentication =
                    org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                    && !"anonymousUser".equals(authentication.getPrincipal())) {
                return authentication.getName();
            }
        } catch (Exception e) {
            // SecurityContext 不可用时不抛异常
            log.trace("获取当前用户失败", e);
        }
        return "system";
    }
}
