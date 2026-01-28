package io.cklau1001.workflow1.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Slf4j
@Configuration
@EnableScheduling
@Profile("SCHEDULER")
public class SchedulerConfig {

    @Bean
    public ThreadPoolTaskScheduler wfeScheduler() {
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(2);
        threadPoolTaskScheduler.setThreadNamePrefix("Scheduler-");
        threadPoolTaskScheduler.initialize();

        log.info("[wfeScheduler]: initilized, poolSize={}", threadPoolTaskScheduler.getPoolSize());
        return threadPoolTaskScheduler;
    }
}
