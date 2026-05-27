package com.tenant.common.config;

import io.micrometer.core.aop.CountedAspect;
import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 监控配置类
 * 配置Micrometer指标收集和Prometheus导出，为Grafana仪表盘提供数据源
 * <p>提供两类切面：
 * <ul>
 *   <li>{@link TimedAspect} - 方法执行耗时指标（@Timed注解驱动）</li>
 *   <li>{@link CountedAspect} - 方法调用次数指标（@Counted注解驱动）</li>
 * </ul>
 * <p>所有指标自动附加应用名称和环境标签，便于多服务区分
 *
 * @author Aze
 */
@Configuration
public class MonitorConfig {

    /**
     * 自定义Meter注册表
     * 为所有指标添加应用名称标签
     *
     * @return MeterRegistry定制器
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config().commonTags(
                "application", "distributed-tenant-cloud",
                "env", System.getProperty("spring.profiles.active", "dev")
        );
    }

    /**
     * 启用计数切面
     * 用于统计方法调用次数
     *
     * @return CountedAspect实例
     */
    @Bean
    public CountedAspect countedAspect(MeterRegistry meterRegistry) {
        return new CountedAspect(meterRegistry);
    }

    /**
     * 启用耗时切面
     * 用于统计方法执行时间
     *
     * @return TimedAspect实例
     */
    @Bean
    public TimedAspect timedAspect(MeterRegistry meterRegistry) {
        return new TimedAspect(meterRegistry);
    }
}