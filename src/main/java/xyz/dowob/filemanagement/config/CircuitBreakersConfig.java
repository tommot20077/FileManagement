package xyz.dowob.filemanagement.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 基於 Resilience4j 的斷路器和限流器系統設定類。
 *
 * <p>提供斷路器和限流器的 Bean 設定，實現系統安全防護機制。斷路器用於防止級聯故障，
 * 當錯誤率達到閾值時自動開啟保護。限流器控制併發請求數量，防止系統過載。</p>
 *
 * <p>斷路器採用滑動窗口模式統計失敗率，支援開啟、半開啟、關閉三種狀態。
 * 限流器基於令牌桶演算法，提供可設定的刷新週期和請求數量限制。</p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@Configuration
public class CircuitBreakersConfig {
    /**
     * 建立斷路器設定 Bean，定義系統安全防護參數。
     *
     * <p>設定 50% 失敗率閾值、10 秒開啟狀態等待時間、半開狀態允許 2 次請求、
     * 滑動窗口大小 2 次。在服務錯誤率過高時自動開啟保護，防止級聯故障。</p>
     *
     * @return 斷路器設定實例
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
     * 建立限流器設定 Bean，控制並發請求數量。
     *
     * <p>設定 5 秒超時時間、1 秒刷新週期、每個週期內允許 5 次請求。
     * 當請求數量超過限制時阻塞等待，防止系統過載。</p>
     *
     * @return 限流器設定實例
     */
    @Bean
    public RateLimiterConfig defaultRateLimiterConfig() {
        return RateLimiterConfig.custom().timeoutDuration(Duration.ofSeconds(5)).limitRefreshPeriod(Duration.ofSeconds(1)).limitForPeriod(5).build();
    }

}
