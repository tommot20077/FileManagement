package xyz.dowob.filemanagement.component.filter.requestlimiter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.async.RedisAsyncCommands;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.component.filter.ClientIpFilter;
import xyz.dowob.filemanagement.component.provider.provider.RedisProvider;
import xyz.dowob.filemanagement.config.properties.GlobalProperties;
import xyz.dowob.filemanagement.convert.StringByteCodeMapper;
import xyz.dowob.filemanagement.exception.LimitationException;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.unity.LogUnity;
import xyz.dowob.filemanagement.unity.ResponseUnity;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis 請求限制器過濾器，透過 Bucket4j 以及 Redis 實現
 * 這個過濾器會在請求進入時檢查用戶的請求次數是否超過限制
 * 如果超過限制，則會返回 429 Too Many Requests 錯誤，如果沒有超過限制，則會將請求放行
 * 採用窗口令牌桶算法進行請求限制，在任意週期內，請求次數不會超過限制
 * 實現了 WebFilter 接口，並在過濾器鏈中處理請求，以及 ResponseUnity 接口，內部封裝一些常用的響應方法
 * 此類僅在啟用 Redis 請求限制器時生效 {@link GlobalProperties.RequestLimiter}
 *
 * @author yuan
 * @program FileManagement
 * @ClassName RequestFilter
 * @create 2025/4/21
 * @Version 1.0
 **/
@Component
@ConditionalOnProperty(prefix = "global", name = "request-limiter.type", havingValue = "redis", matchIfMissing = true)
public class RedisRequestLimiterFilter implements WebFilter, ResponseUnity {
    /**
     * 緩存鍵的前綴
     */
    private final static String KEY_PREFIX = "ip-request-limiter:";

    /**
     * 禁止IP的緩存鍵前綴
     */
    private final static String IP_BAN_COUNT_KEY_PREFIX = "request-ban-ip:";

    /**
     * 請求限制的上限，當請求次數超過這個值時，會返回 429 Too Many Requests 錯誤
     * 當設置為值小於等於 0 時，則使用預設值: Integer.MAX_VALUE
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
     * Redis 提供者，內部封裝了 Redis 的操作
     */
    private final RedisProvider redisProvider;

    /**
     * 是否啟用禁止IP
     * 當設置為true時，會檢查請求的IP是否在禁止列表中
     */
    private final boolean isEnableBanIp;

    /**
     * 失敗次數，當請求次數超過這個值時，會禁止IP訪問
     * 當設置為值小於等於 0 時，則使用預設值: 5
     * 僅在啟用禁止IP時生效
     */
    private int failureCount = 5;

    /**
     * 禁止IP的封禁時間
     * 當設置為值小於等於 0 時，則使用預設值: 1小時
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
     * Redis 請求限制器的代理管理器
     * 使用 Lettuce 實現的 Redis 客戶端
     * 這邊使用IP地址作為請求限制的鍵
     */
    private final ProxyManager<String> proxyManager;

    /**
     * Jackson ObjectMapper 實例，用於序列化和反序列化 JSON
     */
    private final ObjectMapper objectMapper;

    /**
     * 構造函數，初始化 RedisRequestLimiterFilter
     * 將請求限制器的配置從全局配置中獲取，並引入 Redis 客戶端來進行代理限制
     * 使用 FixedTimeToLive 策略來設置請求限制器的過期時間並添加自定義的序列化器
     * 將限制器轉換成 byte[] 存儲到 Redis 中
     *
     * @param globalProperties 全局配置屬性
     * @param objectMapper     Jackson ObjectMapper 實例
     * @param redisProvider    Redis 提供者
     * @param redisClient      Redis 客戶端
     */
    public RedisRequestLimiterFilter(GlobalProperties globalProperties, ObjectMapper objectMapper, RedisClient redisClient, RedisProvider redisProvider) {
        GlobalProperties.RequestLimiter requestLimiter = globalProperties.getRequestLimiter();

        Assert.isTrue(requestLimiter.getRefillDuration().isPositive(), "請求限制器的補充週期必須大於0");
        Assert.isTrue(requestLimiter.getCleanInterval().isPositive(), "請求限制器的清除時間比率必須大於0");
        Assert.isTrue(requestLimiter.getBanIpDuration().isPositive(), "禁止IP的計算時間必須大於0");
        Assert.isTrue(requestLimiter.getBanExpireDuration().isPositive(), "IP的封禁時間必須大於0");
        Assert.isTrue(requestLimiter.getFailureCount() > 0, "禁止IP的失敗次數必須大於0");

        this.objectMapper = objectMapper;
        this.redisProvider = redisProvider;

        this.limit = requestLimiter.getLimit() <= 0 ? Integer.MAX_VALUE : requestLimiter.getLimit();
        this.refill = requestLimiter.getRefill() < 0 ? limit : requestLimiter.getRefill();

        this.refillDuration = requestLimiter.getRefillDuration();

        ExpirationAfterWriteStrategy strategy = ExpirationAfterWriteStrategy.fixedTimeToLive(requestLimiter.getCleanInterval());
        RedisAsyncCommands<String, byte[]> asyncCommands = redisClient.connect(new StringByteCodeMapper()).async();
        ClientSideConfig clientSideConfig = ClientSideConfig.getDefault().withExpirationAfterWriteStrategy(strategy);
        this.proxyManager = LettuceBasedProxyManager.builderFor(asyncCommands).withClientSideConfig(clientSideConfig).build();

        this.banDuration = requestLimiter.getBanIpDuration();
        this.banExpireDuration = requestLimiter.getBanExpireDuration();

        this.isEnableBanIp = requestLimiter.isEnableBanIp();
        if (this.isEnableBanIp) {
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
     * @return Mono<Void> 異步處理請求
     */
    @NotNull
    @Override
    public Mono<Void> filter(@NotNull ServerWebExchange exchange, @NotNull WebFilterChain chain) {
        Optional<String> clientIpOptional = ClientIpFilter.getClientIpFromExchange(exchange);
        if (clientIpOptional.isEmpty()) {
            LogUnity.debug(exchange, "無法獲取用戶 IP: %s, 拒絕連線", exchange.getRequest().getRemoteAddress());
            exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
            return sendErrorResponse(exchange, objectMapper, ValidationException.ErrorCode.REQUEST_IS_INVALID, "IP 地址");
        }
        String clientIp = clientIpOptional.get();
        String banIpKey = IP_BAN_COUNT_KEY_PREFIX + clientIp;
        Mono<Object> handleBanIp = Mono.empty();
        if (isEnableBanIp) {
            handleBanIp = redisProvider.getValue(banIpKey).switchIfEmpty(Mono.just(0L)).flatMap(c -> {
                long failCount = Long.parseLong(c.toString());
                if (failCount >= failureCount) {
                    LogUnity.debug(exchange, "IP: %s 已暫時禁止訪問", clientIp);
                    exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                    return sendErrorResponse(exchange, objectMapper, ValidationException.ErrorCode.ALREADY_BAN_IP);
                }
                return Mono.just(true);
            });
        }


        return handleBanIp.flatMap(b -> getIpBucket(clientIp).flatMap(bucket -> {
            if (bucket.tryConsume(1)) {
                return chain.filter(exchange);
            }

            LogUnity.info(exchange, "IP: %s 請求超過限制值: %s", clientIp, limit);
            Mono<Void> responseMono = Mono.empty();
            if (isEnableBanIp) {
                responseMono = redisProvider.incrementDelta(banIpKey, 1L, banDuration).flatMap(failCount -> {
                    if (failCount >= failureCount) {
                        LogUnity.warn(exchange, "IP: %s 請求過於頻繁且狀況持續，已暫時禁止訪問", clientIp);
                        return redisProvider.setValue(banIpKey, failCount, banExpireDuration);
                    }
                    return Mono.empty();
                });
            }
            return responseMono.then(Mono.defer(() -> {
                String errorMessage = "當前請求過於頻繁，請稍後再試";
                exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                return sendErrorResponse(exchange, objectMapper, LimitationException.ErrorCode.USER_EXCEED_LIMIT, errorMessage);
            }));
        })).onErrorResume(e -> {
            LogUnity.error(exchange, "無法處理請求，IP: %s, 錯誤信息:", e, clientIp);
            exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
            return sendErrorResponse(exchange, objectMapper, ValidationException.ErrorCode.REQUEST_IS_INVALID, "無");
        });
    }


    /**
     * 獲取 IP 請求限制器的 Bucket
     * 根據用戶的 IP 地址獲取請求限制器的 Bucket
     * 當不存在時，則創建一個新的 Bucket
     *
     * @param clientIp 用戶的 IP 地址
     *
     * @return Mono<Bucket> 異步請求限制器的 Bucket
     */
    private Mono<Bucket> getIpBucket(@NotNull String clientIp) {
        return Mono.just(clientIp).flatMap(s -> {
            BucketConfiguration bucketConfiguration = BucketConfiguration
                    .builder()
                    .addLimit(stage -> stage.capacity(limit).refillGreedy(refill, refillDuration))
                    .build();
            String bucketKey = KEY_PREFIX + clientIp;
            return Mono.just(proxyManager.builder().build(bucketKey, () -> bucketConfiguration));
        });
    }
}
