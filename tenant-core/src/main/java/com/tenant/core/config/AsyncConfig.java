package com.tenant.core.config;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务线程池配置
 * <p>为操作日志、邮件发送等异步场景提供独立的线程池，避免与业务线程竞争
 * <p>线程池策略：CallerRunsPolicy（队列满时由调用线程自己执行，防止任务丢失）
 *
 * @author Aze
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 操作日志异步写入线程池
     * <p>核心线程数：2，最大线程数：10，队列容量：500
     * <p>使用CallerRunsPolicy作为拒绝策略，确保日志不丢失
     */
    @Bean("logExecutor")
    public Executor logExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("log-async-");
        // 队列满时由调用线程执行，避免日志丢失
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        // 添加 MDC 上下文传递（确保异步线程中能获取 TraceId、租户ID等链路信息）
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.initialize();
        return executor;
    }

    /**
     * 通用异步任务线程池
     * <p>用于邮件发送、通知推送等非日志异步任务
     */
    @Bean("taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("task-async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        // 添加 MDC 上下文传递（确保异步线程中能获取 TraceId、租户ID等链路信息）
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.initialize();
        return executor;
    }

    /**
     * MDC 上下文传递装饰器
     * <p>解决异步线程无法继承主线程 MDC 上下文的问题（TraceId、租户ID等）
     * <p>原理：在任务提交时捕获主线程 MDC，在异步线程执行时恢复，执行完毕后清理
     */
    private static class MdcTaskDecorator implements org.springframework.core.task.TaskDecorator {
        @Override
        public Runnable decorate(Runnable runnable) {
            // 在主线程中捕获 MDC 上下文
            Map<String, String> contextMap = MDC.getCopyOfContextMap();
            return () -> {
                try {
                    // 在异步线程中恢复 MDC 上下文
                    if (contextMap != null) {
                        MDC.setContextMap(contextMap);
                    }
                    // 执行实际任务
                    runnable.run();
                } finally {
                    // 清理异步线程的 MDC，防止线程池复用时的数据污染
                    MDC.clear();
                }
            };
        }
    }
}
