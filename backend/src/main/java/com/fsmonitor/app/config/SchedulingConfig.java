package com.fsmonitor.app.config;

import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.Executor;

@Configuration
@EnableScheduling
@EnableAsync
public class SchedulingConfig implements AsyncConfigurer {

    private static final int SCHEDULER_POOL_SIZE = 10;
    private static final int MAIL_EXECUTOR_CORE_SIZE = 4;
    private static final int MAIL_EXECUTOR_MAX_SIZE = 10;
    private static final int MAIL_QUEUE_CAPACITY = 100;

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(SCHEDULER_POOL_SIZE);
        scheduler.setThreadNamePrefix("fsmonitor-");
        scheduler.setRemoveOnCancelPolicy(true);
        scheduler.setErrorHandler(t ->
            LoggerFactory.getLogger("fsmonitor.scheduler")
                .error("Scheduled task error: {}", t.getMessage(), t));
        scheduler.initialize();
        return scheduler;
    }

    @Bean(name = "mailTaskExecutor")
    public ThreadPoolTaskExecutor mailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(MAIL_EXECUTOR_CORE_SIZE);
        executor.setMaxPoolSize(MAIL_EXECUTOR_MAX_SIZE);
        executor.setQueueCapacity(MAIL_QUEUE_CAPACITY);
        executor.setThreadNamePrefix("mail-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /** Dedicated executor for controller-triggered scans so HTTP requests return fast. */
    @Bean(name = "monitorTaskExecutor")
    public ThreadPoolTaskExecutor monitorTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("monitor-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return mailTaskExecutor();
    }
}
