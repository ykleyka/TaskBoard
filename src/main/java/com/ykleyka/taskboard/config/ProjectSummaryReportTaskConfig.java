package com.ykleyka.taskboard.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class ProjectSummaryReportTaskConfig {
    @Bean(name = "projectSummaryReportTaskExecutor")
    public Executor projectSummaryReportTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(1_000);
        executor.setThreadNamePrefix("project-summary-report-");
        executor.initialize();
        return executor;
    }
}
