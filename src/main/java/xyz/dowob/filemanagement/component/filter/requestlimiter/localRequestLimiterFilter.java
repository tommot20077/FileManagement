package xyz.dowob.filemanagement.component.filter.requestlimiter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bucket;
import jakarta.annotation.PreDestroy;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.component.filter.ClientIpFilter;
import xyz.dowob.filemanagement.config.properties.GlobalProperties;
import xyz.dowob.filemanagement.exception.LimitationException;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.unity.CacheConcurrentHashMap;
import xyz.dowob.filemanagement.unity.LogUnity;
import xyz.dowob.filemanagement.unity.ResponseUnity;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 本地請求限制器過濾器，透過 Bucket4j 以及 CacheConcurrentHashMap 實現
 * 這個過濾器會在請求進入時檢查用戶的請求次數是否超過限制
 * 如果超過限制，則會返回 429 Too Many Requests 錯誤，如果沒有超過限制，則會將請求放行
 * 採用窗口令牌桶算法進行請求限制，在任意週期內，請求次數不會超過限制
 * 實現了 WebFilter 接口，並在過濾器鏈中處理請求，以及 ResponseUnity 接口，內部封裝一些常用的響應方法
 * 此類僅在啟用本地請求限制器時生效 {@link GlobalProperties.RequestLimiter}
 * 請注意此限制器僅在本地使用，當服務重啟時，請求限制器以及禁止IP列表會被清除，若須要持久化，請使用 Redis 請求限制器
 *
 * @author yuan
 * @program FileManagement
 * @ClassName localRequestLimiterFilter
 * @create 2025/4/22
 * @Version 1.0
 **/
@Component
@ConditionalOnProperty(prefix = "global", name = "request-limiter.type", havingValue = "local")
public class localRequestLimiterFilter implements WebFilter, ResponseUnity {
    /**
     * 緩存鍵的前綴
     */
    private final static String KEY_PREFIX = "ip-request-limiter:";

    /**
     * 請求限制的上限
     */
    private final int limit;

    /**
     * 補充的令牌數量，會在 {@link #refillDuration} 內逐漸補充到達上限
     * 當設定為值小於等於 0 時，則補充的令牌數量為 {@link #limit}
     */
    private final int refill;

    /**
     * 補充週期
     * 請求令牌會在這個週期內逐漸補充到達上限
     */
    private final Duration refillDuration;

    /**
     * 禁止IP的封禁時間
     * 當設置為值小於等於 0 時，則使用預設值: 1小時
     * 在這段時間內，禁止IP的狀態會被清除
     * 僅在啟用禁止IP時生效
     */
    private final Duration banExpireDuration;

    /**
     * 禁止IP的計算時間
     * 當設置為值小於等於 0 時，則使用預設值: 10分鐘
     * 在本段時間內，請求失敗的次數若超過 {@link #failureCount}，則會禁止IP訪問
     * 僅在啟用禁止IP時生效
     */
    private final Duration banDuration;

    /**
     * 是否啟用禁止IP
     * 當設置為true時，會檢查請求的IP是否在禁止列表中
     */
    private final boolean isEnableBanIp;

    /**
     * 限流器緩存儲存Map，用於存儲每個IP的請求限制器
     */
    private final CacheConcurrentHashMap<String, Bucket> ipBucketLimiterMap;

    /**
     * 失敗次數，當請求次數超過這個值時，會禁止IP訪問
     * 僅在啟用禁止IP時生效
     * 當設置為值小於等於 0 時，則拋出異常
     */
    private int failureCount = 5;

    /**
     * 禁止IP的緩存儲存Map，用於存儲每個IP的禁止狀態
     * 當請求次數超過 {@link #failureCount} 時，會將IP加入禁止列表
     */
    private CacheConcurrentHashMap<String, Long> banIpMap = null;

    /**
     * JSON 轉換器
     * 用於將對象轉換為 JSON 字符串
     */
    private final ObjectMapper objectMapper;


    /**
     * 構造函數，初始化請求限制器
     * 將請求限制器的上限、補充的令牌數量、補充週期等參數進行初始化
     * 若果這些參數小於等於 0，則會使用預設值
     *
     * @param objectMapper     JSON 轉換器
     * @param globalProperties 全局配置屬性
     */
    public localRequestLimiterFilter(ObjectMapper objectMapper, GlobalProperties globalProperties) {
        GlobalProperties.RequestLimiter requestLimiter = globalProperties.getRequestLimiter();

        Assert.isTrue(requestLimiter.getCleanInterval().isPositive(), "請求限制器的清除時間比率必須大於0");
        Assert.isTrue(requestLimiter.getRefillDuration().isPositive(), "請求限制器的補充令牌週期必須大於0");
        Assert.isTrue(requestLimiter.getBanIpDuration().isPositive(), "禁止IP的計算時間必須大於0");
        Assert.isTrue(requestLimiter.getBanExpireDuration().isPositive(), "禁止IP的封禁時間必須大於0");
        Assert.isTrue(requestLimiter.getFailureCount() > 0, "禁止IP的失敗次數必須大於0");

        this.objectMapper = objectMapper;
        Duration cleanupInterval = requestLimiter.getCleanInterval();

        this.limit = requestLimiter.getLimit() <= 0 ? Integer.MAX_VALUE : requestLimiter.getLimit();
        this.refill = requestLimiter.getRefill() <= 0 ? this.limit : requestLimiter.getRefill();
        this.refillDuration = requestLimiter.getRefillDuration();

        this.ipBucketLimiterMap = new CacheConcurrentHashMap<>(1024, this.refillDuration, this.refillDuration, cleanupInterval, false);
        this.ipBucketLimiterMap.setTag("請求限制器緩存表");

        this.banDuration = requestLimiter.getBanIpDuration();
        this.banExpireDuration = requestLimiter.getBanExpireDuration();

        this.isEnableBanIp = requestLimiter.isEnableBanIp();
        if (this.isEnableBanIp) {
            this.banIpMap = new CacheConcurrentHashMap<>(64, this.banDuration, this.banExpireDuration, cleanupInterval, false);
            this.banIpMap.setTag("封禁IP地址緩存表");
            this.failureCount = requestLimiter.getFailureCount();
        }
    }

    /**
     * 過濾器方法，處理請求並檢查請求次數是否超過限制
     * 當超過限制時，返回 429 Too Many Requests 錯誤
     * 當沒有超過限制時，將請求放行
     *
     * @param exchange 請求交換對象
     * @param chain    過濾器鏈對象
     *
     * @return Mono<Void> 異步響應對象
     */
    @NotNull
    @Override
    public Mono<Void> filter(@NotNull ServerWebExchange exchange, @NotNull WebFilterChain chain) {
        Optional<String> ipOptional = ClientIpFilter.getClientIpFromExchange(exchange);
        if (ipOptional.isEmpty()) {
            LogUnity.debug(exchange, "無法獲取用戶 IP: %s, 拒絕連線", exchange.getRequest().getRemoteAddress());
            exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
            return sendErrorResponse(exchange, objectMapper, ValidationException.ErrorCode.REQUEST_IS_INVALID, "IP 地址");
        }
        return Mono.just(ipOptional.get()).flatMap(ip -> {
            String key = KEY_PREFIX + ip;

            if (isEnableBanIp) {
                Long failCount = banIpMap.check(key);
                if (failCount != null && failCount >= failureCount) {
                    LogUnity.debug(exchange, "IP: %s 已暫時禁止訪問", ip);
                    exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                    return sendErrorResponse(exchange, objectMapper, ValidationException.ErrorCode.ALREADY_BAN_IP);
                }
            }

            Bucket bucket = ipBucketLimiterMap.get(key);
            if (bucket == null) {
                bucket = Bucket.builder().addLimit(stage -> stage.capacity(limit).refillGreedy(refill, refillDuration)).build();
                ipBucketLimiterMap.set(key, bucket);
            }
            if (bucket.tryConsume(1)) {
                return chain.filter(exchange);
            }

            LogUnity.info(exchange, "IP: %s 請求超過限制值: %s", ip, limit);
            if (isEnableBanIp) {
                AtomicBoolean isBanned = new AtomicBoolean(false);
                banIpMap.computeIfPresentOrDefault(key, 0L, banDuration, (k, currentValue) -> {
                    long newValue = currentValue == null ? 1L : currentValue + 1;
                    if (newValue >= failureCount) {
                        isBanned.set(true);
                        banIpMap.set(key, newValue, banExpireDuration);
                    }
                    return newValue;
                });

                if (isBanned.get()) {
                    LogUnity.warn(exchange, "IP: %s 請求過於頻繁且狀況持續，已暫時禁止訪問", ip);
                }
            }
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return sendErrorResponse(exchange, objectMapper, LimitationException.ErrorCode.USER_EXCEED_LIMIT, "當前請求過於頻繁，請稍後再試");
        }).onErrorResume(e -> {
            LogUnity.error(exchange, "無法處理請求，IP: %s, 錯誤信息: ", e, exchange.getRequest().getRemoteAddress());
            exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
            return sendErrorResponse(exchange, objectMapper, ValidationException.ErrorCode.REQUEST_IS_INVALID, "無");
        });
    }


    /**
     * 清除方法，清除緩存中的請求限制器以及禁止IP列表
     * 會在固定間隔的時間 {@link GlobalProperties.RequestLimiter# cleanInterval} 進行清除
     */
    @Scheduled(fixedDelayString = "#{globalProperties.requestLimiter.cleanInterval.toMillis()}", initialDelayString = "#{globalProperties.requestLimiter.cleanInterval.toMillis()}")
    public void clean() {
        ipBucketLimiterMap.getCleanupTask().run();
        if (isEnableBanIp) {
            banIpMap.getCleanupTask().run();
        }
    }


    /**
     * 銷毀方法，清除緩存
     * 在應用關閉時，清除緩存中的所有請求限制器以及禁止IP列表
     */
    @PreDestroy
    public void destroy() {
        ipBucketLimiterMap.destroy();
        if (isEnableBanIp) {
            banIpMap.destroy();
        }
    }
}
