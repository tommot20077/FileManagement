package xyz.dowob.filemanagement.component.provider.providerInterface;

import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
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
 * @Version 1.1
 **/

public interface CacheProvider {

    /**
     * 獲取單個緩存值
     *
     * @param key   緩存鍵
     * @param clazz 值的類型
     * @param <T>   泛型類型
     *
     * @return Mono<T>
     */
    default <T> Mono<T> get(String key, Class<T> clazz) {
        return Mono.empty();
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
    default <T> Mono<List<T>> getAsList(String key, Class<T> clazz) {
        return Mono.empty();
    }


    /**
     * 批量查詢緩存 (返回一個 Map)
     *
     * @param keys  緩存鍵集合
     * @param clazz 值的類型
     * @param <T>   泛型類型
     *
     * @return Mono<Map < String, T>>
     */
    default <T> Mono<Map<String, T>> getAllAsMap(Collection<String> keys, Class<T> clazz) {
        return Mono.empty();
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
    default <T> Mono<Map<String, List<T>>> getAllAsMapList(Collection<String> keys, Class<T> clazz) {
        return Mono.empty();
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
    default Mono<Void> set(String key, Object value, Duration expire) {
        return Mono.empty();
    }


    /**
     * 批量設置緩存值
     *
     * @param keyValues key-value 鍵值對
     * @param expire    可選的過期時間
     *
     * @return Mono<Void>
     */
    default Mono<Void> setAll(Map<String, Object> keyValues, Duration expire) {
        return Mono.empty();
    }


    /**
     * 刪除單個緩存鍵
     *
     * @param key 緩存鍵
     *
     * @return Mono<Void>
     */
    default Mono<Void> delete(String key) {
        return Mono.empty();
    }


    /**
     * 批量刪除緩存鍵
     *
     * @param keys 緩存鍵集合
     *
     * @return Mono<Void>
     */
    default Mono<Void> deleteAll(Collection<String> keys) {
        return Mono.empty();
    }


    /**
     * 獲取默認過期時間
     *
     * @return 默認過期時間
     */
    Duration getDefaultExpire();
}
