package io.cklau1001.workflow1.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

@Slf4j
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class DBConfig {

    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> Optional.of("SYSTEM");
    }


}
