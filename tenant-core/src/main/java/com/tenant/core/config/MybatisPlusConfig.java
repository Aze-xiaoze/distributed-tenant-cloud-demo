package com.tenant.core.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.tenant.core.tenant.TenantContextHolder;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

/**
 * MyBatis-Plus 插件配置
 * 统一配置多租户SQL拦截器、分页插件、防全表更新插件
 * <p>插件执行顺序（MyBatis-Plus按添加顺序执行）：
 * <ol>
 *   <li>{@link TenantLineInnerInterceptor} - 多租户插件：自动在SQL中拼接 {@code WHERE tenant_id = ?} 条件</li>
 *   <li>{@link PaginationInnerInterceptor} - 分页插件：拦截分页查询自动追加LIMIT/OFFSET</li>
 *   <li>{@link BlockAttackInnerInterceptor} - 防全表更新插件：阻止无WHERE条件的UPDATE/DELETE</li>
 * </ol>
 * <p>租户ID来源：{@link TenantContextHolder#getCurrentTenantId()}，
 * 若上下文中无租户ID则回退到 "default_tenant"
 *
 * @author Aze
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 忽略多租户过滤的表名集合
     * 租户表、字典表、系统配置表等共享数据不需要租户过滤
     */
    private static final Set<String> IGNORE_TENANT_TABLES = Set.of(
            "tenants",
            "sys_dict",
            "sys_config",
            "sys_role",
            "sys_menu",
            "sys_role_menu"
    );

    /**
     * MyBatis-Plus 拦截器配置
     * 按顺序注册：多租户 → 分页 → 防全表更新
     *
     * @return MybatisPlusInterceptor 实例
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 1. 多租户插件 — 自动在SQL中拼接 tenant_id 条件
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {

            /**
             * 获取租户ID值
             * 从TenantContextHolder中获取当前线程的租户ID
             *
             * @return 租户ID表达式
             */
            @Override
            public Expression getTenantId() {
                String tenantId = TenantContextHolder.getCurrentTenantId();
                return new StringValue(tenantId != null ? tenantId : "default_tenant");
            }

            /**
             * 获取租户字段名
             *
             * @return 租户字段名
             */
            @Override
            public String getTenantIdColumn() {
                return "tenant_id";
            }

            /**
             * 判断表是否忽略多租户过滤
             *
             * @param tableName 表名
             * @return true表示忽略（不过滤），false表示需要过滤
             */
            @Override
            public boolean ignoreTable(String tableName) {
                return IGNORE_TENANT_TABLES.contains(tableName.toLowerCase());
            }
        }));

        // 2. 乐观锁插件 — 防止并发更新数据覆盖（实体需添加 @Version 字段）
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

        // 3. 分页插件
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));

        // 4. 防全表更新删除插件 — 阻止没有WHERE条件的UPDATE/DELETE
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());

        return interceptor;
    }
}