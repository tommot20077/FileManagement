package xyz.dowob.filemanagement.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 斷路器配置，用於配置斷路器的相關參數，如失敗率、等待時間、半開狀態允許的請求次數、窗口大小等
 * 斷路器配置參數參考：<a href="https://resilience4j.readme.io/docs/circuitbreaker"></a>
 * 當前如果頻繁出現處於開啟狀態的斷路器，可以考慮調整失敗率、等待時間、半開狀態允許的請求次數、窗口大小等參數
 * @author yuan
 * @program FileManagement
 * @ClassName CircuitBreakerConfig
 * @create 2025/1/24
 * @Version 1.0
 **/
@Configuration
public class CircuitBreakersConfig {
    /**
     * 斷路器配置
     *
     * @return 斷路器配置
     */
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
