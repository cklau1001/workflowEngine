package io.cklau1001.workflow1.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ExecutorService;


@Slf4j
@Configuration
public class ThreadPoolConfig {

    // @Bean(destroyMethod = "shutdown")
    @Bean
    public ExecutorService wfeExecutorService() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("WFE-Executor-");
        executor.initialize();
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        log.info("[wfeExecutorService]: threadpool created");
        return executor.getThreadPoolExecutor();
    }
}
