package xyz.dowob.filemanagement.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 安全限制器設定
 * 1. 用於配置斷路器的相關參數，如失敗率、等待時間、半開狀態允許的請求次數、窗口大小等，斷路器配置參數參考：<a
 * href="https://resilience4j.readme.io/docs/circuitbreaker"></a>，當前如果頻繁出現處於開啟狀態的斷路器，可以考慮調整失敗率、等待時間、半開狀態允許的請求次數、窗口大小等參數
 * 2. 用於配置限流器的相關參數，如超時時間、限流器刷新時間、限流器限制的請求次數等
 *
 * @author yuan
 * @program FileManagement
 * @ClassName CircuitBreakerConfig
 * @create 2025/1/24
 * @Version 1.0
 **/
@Configuration
public class CircuitBreakersConfig {
    /**
     * 斷路器配置，用於配置斷路器的相關參數
     * 當前配置失敗率為 50%，等待時間為 10 秒，半開狀態允許的請求次數為 2，窗口大小為 2
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


    /**
     * 限流器配置，用於配置限流器的相關參數
     * 當前配置超時時間為 5 秒，限流器刷新時間為 1 秒，限流器限制的請求次數為 5
     *
     * @return 限流器配置
     */
    @Bean
    public RateLimiterConfig defaultRateLimiterConfig() {
        return RateLimiterConfig.custom().timeoutDuration(Duration.ofSeconds(5)).limitRefreshPeriod(Duration.ofSeconds(1)).limitForPeriod(5).build();
    }

}
