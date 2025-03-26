package xyz.dowob.filemanagement.component.provider.providerImplement.cacheprovider;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.CacheProviderType;
import xyz.dowob.filemanagement.annotation.SkipRecord;
import xyz.dowob.filemanagement.component.provider.provider.RedisProvider;
import xyz.dowob.filemanagement.component.provider.providerInterface.CacheProvider;
import xyz.dowob.filemanagement.config.properties.CacheProperties;
import xyz.dowob.filemanagement.customenum.CacheProviderEnum;
import xyz.dowob.filemanagement.data.file.dto.UserFileListDTO;

import java.time.Duration;
import java.util.*;

/**
 * 用戶文件列表緩存提供者，用於提供用戶文件列表的緩存操作
 * 此類並沒有提供獲取單個緩存值的方法，因為用戶文件列表緩存是以列表的形式存儲的
 *
 * @author yuan
 * @program FileManagement
 * @ClassName UserFileListCacheProvider
 * @create 2025/3/16
 * @Version 1.0
 **/
@Component
@SkipRecord
@CacheProviderType(CacheProviderEnum.USER_FILE_LIST_CACHE)
@ConditionalOnProperty(prefix = "cache", name = "enable-user-file-list-cache", havingValue = "true", matchIfMissing = true)
public class FileListCacheProviderImpl implements CacheProvider {

    /**
     * Redis提供者
     */
    private final RedisProvider redisProvider;

    /**
     * 默認過期時間
     */
    private final Duration DEFAULT_EXPIRE_TIME;

    public FileListCacheProviderImpl(RedisProvider redisProvider, CacheProperties cacheProperties) {
        this.redisProvider = redisProvider;
        this.DEFAULT_EXPIRE_TIME = Duration.ofMinutes(cacheProperties.getFileListCacheExpireTime());
    }


    /**
     * 查詢緩存集合 (返回一個 List)
     *
     * @param key   緩存鍵集合
     * @param clazz 值的類型
     * @param <T>   泛型類型
     *
     * @return Mono<List < T>>
     */
    @Override
    public <T> Mono<List<T>> getAsList(String key, Class<T> clazz) {
        return redisProvider.getList(key, clazz).collectList();
    }

    /**
     * 批量查詢緩存列表 (返回一個 Map，內部為列表)
     *
     * @param keys  緩存鍵集合
     * @param clazz 值的類型
     * @param <T>   泛型類型
     *
     * @return Mono<Map < String, List < T>>>
     */
    public <T> Mono<Map<String, List<T>>> getAllAsMapList(Collection<String> keys, Class<T> clazz) {
        HashMap<String, List<T>> map = new HashMap<>();
        return Flux.fromIterable(keys).flatMap(key -> getAsList(key, clazz).doOnNext(list -> map.put(key, list))).then(Mono.just(map));
    }


    /**
     * 設定單個緩存值
     *
     * @param key    緩存鍵
     * @param value  緩存值
     * @param expire 可選的過期時間
     *
     * @return Mono<Void>
     */
    @Override
    public Mono<Void> set(String key, Object value, Duration expire) {
        Duration chooseTime = Objects.requireNonNullElse(expire, DEFAULT_EXPIRE_TIME);
        if (value instanceof Collection<?> c) {
            return Flux.fromIterable(c).flatMap(o -> redisProvider.insertList(key, o, false, chooseTime)).then();
        }
        if (value instanceof UserFileListDTO dto) {
            return redisProvider.insertList(key, dto, false, chooseTime).then();
        }
        return Mono.error(new UnsupportedOperationException("不支持的操作類型: " + value.getClass().getName()));
    }


    /**
     * 批量設置緩存值
     *
     * @param keyValues key-value 鍵值對
     * @param expire    可選的過期時間
     *
     * @return Mono<Void>
     */
    @Override
    public Mono<Void> setAll(Map<String, Object> keyValues, Duration expire) {
        Duration chooseTime = Objects.requireNonNullElse(expire, DEFAULT_EXPIRE_TIME);
        return Flux.fromIterable(keyValues.entrySet()).flatMap(entry -> {
            if (entry.getValue() instanceof Collection<?> c) {
                return Flux.fromIterable(c).flatMap(o -> redisProvider.insertList(entry.getKey(), o, false, chooseTime));
            }
            if (entry.getValue() instanceof UserFileListDTO dto) {
                return redisProvider.insertList(entry.getKey(), dto, false, chooseTime);
            }
            return Mono.error(new UnsupportedOperationException("不支持的操作類型: " + entry.getValue().getClass().getName()));
        }).then();
    }

    /**
     * 刪除單個緩存鍵
     *
     * @param key 緩存鍵
     *
     * @return Mono<Void>
     */
    @Override
    public Mono<Void> delete(String key) {
        return redisProvider.deleteList(key);
    }

    /**
     * 批量刪除緩存鍵
     *
     * @param keys 緩存鍵集合
     *
     * @return Mono<Void>
     */
    @Override
    public Mono<Void> deleteAll(Collection<String> keys) {
        return Flux.fromIterable(keys).flatMap(this::delete).then();
    }

    /**
     * 獲取緩存數據的默認過期時間
     *
     * @return Duration 默認過期時間
     */
    @Override
    @SkipRecord
    public Duration getDefaultExpire() {
        return DEFAULT_EXPIRE_TIME;
    }
}
