package xyz.dowob.filemanagement.component.provider.provider;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.component.provider.providerInterface.CacheProvider;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;

/**
 * Redis緩存提供者抽象類，用於提供緩存操作的具體實現
 * 這類主要對於使用Redis作為緩存的操作進行了封裝，提供對於緩存操作的初步實現
 * 減少了具體緩存操作的重複代碼
 * 此類實現了CacheProvider接口，提供了緩存操作的具體實現，並使用RedisProvider提供的操作方法實現具體的緩存操作
 *
 * @author yuan
 * @program FileManagement
 * @ClassName AbstractCacheProvider
 * @create 2025/3/15
 * @Version 1.0
 **/

@Setter
@Getter
public abstract class AbstractRedisCacheProvider implements CacheProvider {
    /**
     * Redis操作提供者
     */
    private final RedisProvider redisProvider;

    /**
     * 緩存前綴
     */
    private String CACHE_PREFIX;

    /**
     * 默認過期時間，單位為分鐘
     */
    private int DEFAULT_EXPIRE_TIME;

    /**
     * 用戶緩存提供者實現類的構造方法
     *
     * @param redisProvider Redis操作提供者
     */
    public AbstractRedisCacheProvider(@NotNull RedisProvider redisProvider) {
        this.redisProvider = redisProvider;
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
    public <T> Flux<T> getAll(Collection<String> hashKeys, Class<T> clazz) {
        if (hashKeys == null || hashKeys.isEmpty()) {
            return redisProvider.getHashMapAll(CACHE_PREFIX, clazz);
        }
        return redisProvider.getHashMapList(CACHE_PREFIX, hashKeys.stream().toList(), clazz);
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
    public Mono<Void> set(String hashKey, Object value, Duration... expire) {
        if (expire == null || expire.length == 0) {
            return redisProvider.setHashMap(CACHE_PREFIX, hashKey, value, Duration.ofMinutes(DEFAULT_EXPIRE_TIME));
        }
        return redisProvider.setHashMap(CACHE_PREFIX, hashKey, value, expire[0]);
    }

    /**
     * 設定緩存數據，此為批量設定
     *
     * @param keyValues key-value 集合
     * @param expire    過期時間
     *
     * @return Mono<Void>
     */
    public Mono<Void> setAll(Map<String, Object> keyValues, Duration... expire) {
        if (expire == null || expire.length == 0) {
            return redisProvider.setHashMapAll(CACHE_PREFIX, keyValues, Duration.ofMinutes(DEFAULT_EXPIRE_TIME));
        }
        return redisProvider.setHashMapAll(CACHE_PREFIX, keyValues, expire[0]);
    }

    /**
     * 刪除緩存數據
     *
     * @param hashKey 查詢key
     *
     * @return Mono<Void>
     */
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
    public Duration getDefaultExpire() {
        return Duration.ofMinutes(DEFAULT_EXPIRE_TIME);
    }
}
