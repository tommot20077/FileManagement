package xyz.dowob.filemanagement.component.provider.providerImplement.cacheprovider;

import io.jsonwebtoken.lang.Assert;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.CacheProviderType;
import xyz.dowob.filemanagement.component.provider.provider.RedisProvider;
import xyz.dowob.filemanagement.component.provider.providerInterface.CacheProvider;
import xyz.dowob.filemanagement.config.properties.CacheProperties;
import xyz.dowob.filemanagement.customenum.CacheProviderEnum;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 用戶緩存提供者實現類，用於提供用戶緩存的操作
 * 實現了CacheProvider接口以及繼承AbstractRedisCacheProvider，對於用戶緩存的操作進行了封裝
 * 提供了緩存操作的具體實現
 * 此類透過緩存設定enable-user-cache來判斷是否啟用用戶緩存，當開啟時此類才會生效，默認開啟 {@link xyz.dowob.filemanagement.config.properties.CacheProperties}
 * 用戶緩存的key前綴以及緩存的默認過期時間來自於用戶設定
 * 用戶緩存的key-value為用戶ID-用戶信息
 *
 * @author yuan
 * @program FileManagement
 * @ClassName UserCacheProviderImpl
 * @create 2025/3/10
 * @Version 1.0
 **/
@Component
@CacheProviderType(CacheProviderEnum.USER_CACHE)
@ConditionalOnProperty(prefix = "cache", name = "enable-user-info-cache", havingValue = "true", matchIfMissing = true)
public class UserCacheProviderImpl implements CacheProvider {
    /**
     * Redis操作提供者
     */
    private final RedisProvider redisProvider;

    /**
     * 緩存前綴
     */
    private final String CACHE_PREFIX;

    /**
     * 默認過期時間
     */
    private final Duration DEFAULT_EXPIRE_TIME;

    /**
     * 用戶緩存提供者實現類的構造方法
     *
     * @param redisProvider Redis操作提供者
     */
    public UserCacheProviderImpl(RedisProvider redisProvider, CacheProperties cacheProperties) {
        Assert.isTrue(cacheProperties.getUserInfoCacheExpireTime().isPositive(), "用戶資訊緩存過期時間必須大於0");

        this.redisProvider = redisProvider;
        this.CACHE_PREFIX = cacheProperties.getUserInfoCachePrefix();
        this.DEFAULT_EXPIRE_TIME = cacheProperties.getUserInfoCacheExpireTime();
    }


    /**
     * 根據key獲取緩存數據
     *
     * @param hashKey key
     * @param clazz   類型
     *
     * @return Mono<T>
     */
    public <T> Mono<T> get(String hashKey, Class<T> clazz) {
        return redisProvider.getHashMap(CACHE_PREFIX, hashKey, clazz);
    }


    /**
     * 根據key獲取緩存數據，此為批量查詢
     *
     * @param hashKeys key集合
     * @param clazz    類型
     *
     * @return Flux<T>
     */
    public <T> Mono<Map<String, T>> getAllAsMap(Collection<String> hashKeys, Class<T> clazz) {
        if (hashKeys == null || hashKeys.isEmpty()) {
            return redisProvider.getAllHashMap(CACHE_PREFIX, String.class, clazz).collectMap(Map.Entry::getKey, Map.Entry::getValue);
        }
        Map<String, T> resultMap = new HashMap<>();
        return Flux.fromIterable(hashKeys).flatMap(hashKey -> redisProvider.getHashMap(CACHE_PREFIX, hashKey, clazz).map(value -> {
            resultMap.put(hashKey, value);
            return Mono.empty();
        })).then(Mono.just(resultMap));
    }


    /**
     * 設定緩存數據
     *
     * @param hashKey 查詢key
     * @param value   存儲value
     * @param expire  過期時間
     *
     * @return Mono<Void>
     */
    @Override
    public Mono<Void> set(String hashKey, Object value, Duration expire) {
        Duration chooseTime = Objects.requireNonNullElse(expire, DEFAULT_EXPIRE_TIME);
        return redisProvider.setHashMap(CACHE_PREFIX, hashKey, value, chooseTime);
    }


    /**
     * 設定緩存數據，此為批量設定
     *
     * @param keyValues key-value 集合
     * @param expire    過期時間
     *
     * @return Mono<Void>
     */
    @Override
    public Mono<Void> setAll(Map<String, Object> keyValues, Duration expire) {
        Duration chooseTime = Objects.requireNonNullElse(expire, DEFAULT_EXPIRE_TIME);
        return redisProvider.setHashMapAll(CACHE_PREFIX, keyValues, chooseTime);

    }


    /**
     * 刪除緩存數據
     *
     * @param hashKey 查詢key
     *
     * @return Mono<Void>
     */
    @Override
    public Mono<Void> delete(String hashKey) {
        return redisProvider.deleteHash(CACHE_PREFIX, hashKey);
    }


    /**
     * 刪除緩存數據，此為批量刪除
     *
     * @param hashKeys key集合
     *
     * @return Mono<Void>
     */
    @Override
    public Mono<Void> deleteAll(Collection<String> hashKeys) {
        if (hashKeys == null || hashKeys.isEmpty()) {
            return redisProvider.deleteHash(CACHE_PREFIX);
        }
        return redisProvider.deleteHash(CACHE_PREFIX, hashKeys.stream().toList());
    }


    /**
     * 獲取緩存數據的默認過期時間
     *
     * @return Duration
     */
    @Override
    public Duration getDefaultExpire() {
        return DEFAULT_EXPIRE_TIME;
    }
}
