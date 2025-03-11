package xyz.dowob.filemanagement.component.provider.providerImplement;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.component.provider.provider.RedisProvider;
import xyz.dowob.filemanagement.component.provider.providerInterface.CacheProvider;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;

/**
 * 用戶緩存提供者實現類，用於提供用戶緩存的操作
 * 實現了CacheProvider接口，提供了緩存操作的具體實現
 * 並使用RedisProvider提供的操作方法實現具體的緩存操作
 * 用戶緩存的key前綴為"user_info"，用戶緩存的默認過期時間為1小時
 * 用戶緩存的key-value為用戶ID-用戶信息
 *
 * @author yuan
 * @program FileManagement
 * @ClassName UserCacheProviderImpl
 * @create 2025/3/10
 * @Version 1.0
 **/
@Component
@RequiredArgsConstructor
public class UserCacheProviderImpl implements CacheProvider {
    /**
     * Redis操作提供者
     */
    private final RedisProvider redisProvider;

    /**
     * 緩存前綴
     */
    private final String USER_INFO_CACHE_PREFIX = "user_info";

    /**
     * 默認過期時間
     */
    private final Duration DEFAULT_EXPIRE = Duration.ofHours(1);

    /**
     * 根據key獲取緩存數據
     *
     * @param hashKey   key
     * @param clazz 類型
     *
     * @return Mono<T>
     */
    @Override
    public <T> Mono<T> get(String hashKey, Class<T> clazz) {
        return redisProvider.getHashMap(USER_INFO_CACHE_PREFIX, hashKey, clazz);
    }

    /**
     * 根據key獲取緩存數據，此為批量查詢
     *
     * @param hashKeys   key集合
     * @param clazz 類型
     *
     * @return Flux<T>
     */
    @Override
    public <T> Flux<T> getAll(Collection<String> hashKeys, Class<T> clazz) {
        if (hashKeys == null || hashKeys.isEmpty()) {
            return redisProvider.getHashMapAll(USER_INFO_CACHE_PREFIX, clazz);
        }
        return redisProvider.getHashMapList(USER_INFO_CACHE_PREFIX, hashKeys.stream().toList(), clazz);
    }

    /**
     * 設定緩存數據
     *
     * @param hashKey    查詢key
     * @param value  存儲value
     * @param expire 過期時間
     *
     * @return Mono<Void>
     */
    @Override
    public Mono<Void> set(String hashKey, Object value, Duration... expire) {
        if (expire == null || expire.length == 0) {
            return redisProvider.setHashMap(USER_INFO_CACHE_PREFIX, hashKey, value, DEFAULT_EXPIRE);
        }
        return redisProvider.setHashMap(USER_INFO_CACHE_PREFIX, hashKey, value, expire[0]);
    }

    /**
     * 設定緩存數據，此為批量設定
     *
     * @param keyValues  key-value 集合
     * @param expire 過期時間
     *
     * @return Mono<Void>
     */
    @Override
    public Mono<Void> setAll(Map<String, Object> keyValues, Duration... expire) {
        if (expire == null || expire.length == 0) {
            return redisProvider.setHashMapAll(USER_INFO_CACHE_PREFIX, keyValues, DEFAULT_EXPIRE);
        }
        return redisProvider.setHashMapAll(USER_INFO_CACHE_PREFIX, keyValues, expire[0]);
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
        return redisProvider.deleteHash(USER_INFO_CACHE_PREFIX, hashKey);
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
        return redisProvider.deleteHash(USER_INFO_CACHE_PREFIX, hashKeys.stream().toList());
    }

    /**
     * 獲取緩存數據的默認過期時間
     *
     * @return Duration
     */
    @Override
    public Duration getDefaultExpire() {
        return DEFAULT_EXPIRE;
    }
}
