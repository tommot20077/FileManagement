package xyz.dowob.filemanagement.component.provider.providerInterface;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;

/**
 * 緩存提供者接口，定義了緩存操作的基本方法
 * 後續實現類將根據具體的緩存服務器進行實現
 * 主要功能包括獲取緩存數據、設定緩存數據、刪除緩存數據等操作
 *
 * @author yuan
 * @program FileManagement
 * @ClassName CacheProvider
 * @create 2025/3/10
 * @Version 1.0
 **/

public interface CacheProvider {

    /**
     * 根據key獲取緩存數據
     *
     * @param key   key
     * @param clazz 類型
     * @param <T>   泛型
     *
     * @return Mono<T>
     */
    <T> Mono<T> get(String key, Class<T> clazz);

    /**
     * 根據key獲取緩存數據，此為批量查詢
     *
     * @param key   key集合
     * @param clazz 類型
     * @param <T>   泛型
     *
     * @return Flux<T>
     */
    <T> Flux<T> getAll(Collection<String> key, Class<T> clazz);

    /**
     * 設定緩存數據
     *
     * @param key    查詢key
     * @param value  存儲value
     * @param expire 過期時間
     *
     * @return Mono<Void>
     */
    Mono<Void> set(String key, Object value, Duration... expire);

    /**
     * 設定緩存數據，此為批量設定
     *
     * @param value  key-value 集合
     * @param expire 過期時間
     *
     * @return Mono<Void>
     */
    Mono<Void> setAll(Map<String, Object> value, Duration... expire);

    /**
     * 刪除緩存數據
     *
     * @param key 查詢key
     *
     * @return Mono<Void>
     */
    Mono<Void> delete(String key);

    /**
     * 刪除緩存數據，此為批量刪除
     *
     * @param key key集合
     *
     * @return Mono<Void>
     */
    Mono<Void> deleteAll(Collection<String> key);

    /**
     * 獲取緩存數據的默認過期時間
     *
     * @return Duration 默認過期時間
     */
    Duration getDefaultExpire();
}
