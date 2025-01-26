package xyz.dowob.filemanagement.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author yuan
 * @program FileManagement
 * @ClassName CircuitBreakerConfig
 * @create 2025/1/24
 * @Version 1.0
 **/
@Configuration
public class CircuitBreakersConfig {
    @Bean
    public CircuitBreakerConfig circuitBreakerConfig() {
        return CircuitBreakerConfig
                .custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(java.time.Duration.ofSeconds(10))
                .permittedNumberOfCallsInHalfOpenState(2)
                .slidingWindowSize(2)
                .build();
    }
}
