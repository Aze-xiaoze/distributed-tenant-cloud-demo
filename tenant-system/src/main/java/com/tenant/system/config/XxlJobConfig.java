package com.tenant.system.config;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * XXL-Job 分布式任务调度配置类
 * <p>仅在配置了 {@code xxl.job.admin.addresses} 时激活
 * <p>配置属性：
 * <ul>
 *   <li>{@code xxl.job.admin.addresses} - 调度中心地址</li>
 *   <li>{@code xxl.job.accessToken} - 访问令牌</li>
 *   <li>{@code xxl.job.executor.appname} - 执行器名称</li>
 *   <li>{@code xxl.job.executor.address} - 执行器地址</li>
 *   <li>{@code xxl.job.executor.ip} - 执行器IP</li>
 *   <li>{@code xxl.job.executor.port} - 执行器端口</li>
 *   <li>{@code xxl.job.executor.logpath} - 日志路径</li>
 *   <li>{@code xxl.job.executor.logretentiondays} - 日志保留天数</li>
 * </ul>
 *
 * @author Aze
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "xxl.job.admin", name = "addresses")
public class XxlJobConfig {

    @Value("${xxl.job.admin.addresses:}")
    private String adminAddresses;

    @Value("${xxl.job.accessToken:}")
    private String accessToken;

    @Value("${xxl.job.executor.appname:tenant-system-executor}")
    private String appname;

    @Value("${xxl.job.executor.address:}")
    private String address;

    @Value("${xxl.job.executor.ip:}")
    private String ip;

    @Value("${xxl.job.executor.port:9999}")
    private int port;

    @Value("${xxl.job.executor.logpath:/data/logs/xxl-job/jobhandler}")
    private String logPath;

    @Value("${xxl.job.executor.logretentiondays:30}")
    private int logRetentionDays;

    @Bean
    public XxlJobSpringExecutor xxlJobExecutor() {
        log.info(">>>>>>>>>>> xxl-job config init.");
        XxlJobSpringExecutor xxlJobSpringExecutor = new XxlJobSpringExecutor();
        xxlJobSpringExecutor.setAdminAddresses(adminAddresses);
        xxlJobSpringExecutor.setAppname(appname);
        xxlJobSpringExecutor.setAddress(address);
        xxlJobSpringExecutor.setIp(ip);
        xxlJobSpringExecutor.setPort(port);
        xxlJobSpringExecutor.setAccessToken(accessToken);
        xxlJobSpringExecutor.setLogPath(logPath);
        xxlJobSpringExecutor.setLogRetentionDays(logRetentionDays);
        return xxlJobSpringExecutor;
    }
}
