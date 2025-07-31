package xyz.dowob.filemanagement.component.provider.providerImplement.cacheprovider;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.CacheProviderType;
import xyz.dowob.filemanagement.component.provider.provider.RedisProvider;
import xyz.dowob.filemanagement.component.provider.providerInterface.CacheProvider;
import xyz.dowob.filemanagement.config.properties.CacheProperties;
import xyz.dowob.filemanagement.customenum.CacheProviderEnum;
import xyz.dowob.filemanagement.data.file.dto.UserFileListDTO;

import java.time.Duration;
import java.util.*;

/**
 * 基於 Redis 的使用者檔案清單快取提供者實現。
 * 
 * <p>此實現專門處理 {@link UserFileListDTO} 類型的快取操作，透過 Redis List 資料結構
 * 儲存使用者檔案清單資訊。所有快取項目具有可配置的過期時間，支援反應式程式設計模式，
 * 提供非阻塞的快取存取操作。
 * 
 * <p>快取策略特性：
 * <ul>
 * <li>使用 Redis List 作為底層儲存結構</li>
 * <li>支援批次操作以提高效能</li>
 * <li>具有自動過期機制防止記憶體洩漏</li>
 * <li>針對檔案清單資料結構最佳化</li>
 * </ul>
 * 
 * <p>此實現不提供單一值的快取操作，僅支援清單形式的資料操作。
 * 所有操作均為非同步執行，返回 Mono 或 Flux 類型。
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@Component
@CacheProviderType(CacheProviderEnum.USER_FILE_LIST_CACHE)
@ConditionalOnProperty(prefix = "cache", name = "enable-user-file-list-cache", havingValue = "true", matchIfMissing = true)
public class FileListCacheProviderImpl implements CacheProvider {

    /**
     * Redis 操作提供者，用於執行底層 Redis 資料存取操作
     */
    private final RedisProvider redisProvider;

    /**
     * 預設快取過期時間，從應用程式配置中載入
     */
    private final Duration DEFAULT_EXPIRE_TIME;

    /**
     * 建構檔案清單快取提供者實例。
     * 
     * <p>初始化 Redis 操作提供者與預設過期時間配置。驗證過期時間必須為正值，
     * 確保快取項目能夠正確設定過期機制。
     *
     * @param redisProvider Redis 操作提供者，用於執行底層快取操作
     * @param cacheProperties 快取配置屬性，包含過期時間等設定值
     * @throws IllegalArgumentException 當檔案清單快取過期時間不為正值時拋出
     */
    public FileListCacheProviderImpl(RedisProvider redisProvider, CacheProperties cacheProperties) {
        Assert.isTrue(cacheProperties.getFileListCacheExpireTime().isPositive(), "檔案列表緩存過期時間必須大於0");
        this.redisProvider = redisProvider;
        this.DEFAULT_EXPIRE_TIME = cacheProperties.getFileListCacheExpireTime();
    }


    /**
     * 根據指定鍵值取得快取清單資料。
     * 
     * <p>從 Redis 中擷取指定鍵的清單資料，並將結果收集為 List 回傳。
     * 此方法為非同步操作，當快取鍵不存在時將回傳空清單。
     *
     * @param key 快取鍵，用於識別特定的快取項目
     * @param clazz 目標類型的 Class 物件，用於反序列化快取資料
     * @param <T> 快取資料的泛型類型
     * @return 包含快取清單資料的 Mono，若快取不存在則為空清單
     */
    @Override
    public <T> Mono<List<T>> getAsList(String key, Class<T> clazz) {
        return redisProvider.getList(key, clazz).collectList();
    }


    /**
     * 批次查詢多個快取鍵的清單資料。
     * 
     * <p>對指定的快取鍵集合進行批次查詢，將每個鍵對應的清單資料組織為 Map 結構回傳。
     * 此方法使用反應式流處理，能有效處理大量快取鍵的查詢操作。
     *
     * @param keys 快取鍵集合，指定要查詢的所有快取項目
     * @param clazz 目標類型的 Class 物件，用於反序列化快取資料
     * @param <T> 快取資料的泛型類型
     * @return 包含所有查詢結果的 Mono，以鍵值對應清單的 Map 形式呈現
     */
    public <T> Mono<Map<String, List<T>>> getAllAsMapList(Collection<String> keys, Class<T> clazz) {
        HashMap<String, List<T>> map = new HashMap<>();
        return Flux.fromIterable(keys).flatMap(key -> getAsList(key, clazz).doOnNext(list -> map.put(key, list))).then(Mono.just(map));
    }


    /**
     * 設定快取項目的值與過期時間。
     * 
     * <p>支援 Collection 集合類型與 {@link UserFileListDTO} 類型的資料儲存。
     * 對於集合類型資料，會逐一插入到 Redis List 中；對於單一 DTO 物件，
     * 直接插入到指定鍵的 List 結構。若未指定過期時間則使用預設值。
     *
     * @param key 快取鍵，用於標識快取項目
     * @param value 快取值，支援 Collection 或 UserFileListDTO 類型
     * @param expire 過期時間，若為 null 則使用預設過期時間
     * @return 表示操作完成的 Mono
     * @throws UnsupportedOperationException 當提供不支援的資料類型時拋出
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
     * 批次設定多個快取項目的值與過期時間。
     * 
     * <p>對多個鍵值對進行批次快取設定操作。每個值的類型必須為 Collection 或
     * {@link UserFileListDTO}，否則將產生錯誤。所有快取項目使用相同的過期時間，
     * 若未指定則使用預設過期時間。此方法使用反應式流進行並行處理以提升效能。
     *
     * @param keyValues 包含快取鍵與對應值的 Map 集合
     * @param expire 過期時間，若為 null 則使用預設過期時間
     * @return 表示所有設定操作完成的 Mono
     * @throws UnsupportedOperationException 當任一值為不支援的資料類型時拋出
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
     * 刪除指定的快取項目。
     * 
     * <p>從 Redis 中移除指定鍵的快取清單資料。此操作為非同步執行，
     * 若指定的快取鍵不存在，操作仍會正常完成而不產生錯誤。
     *
     * @param key 要刪除的快取鍵
     * @return 表示刪除操作完成的 Mono
     */
    @Override
    public Mono<Void> delete(String key) {
        return redisProvider.deleteList(key).then();
    }


    /**
     * 批次刪除多個快取項目。
     * 
     * <p>對指定的快取鍵集合進行批次刪除操作。使用反應式流處理方式，
     * 並行執行多個刪除操作以提升效能。即使部分快取鍵不存在，
     * 整體操作仍會正常完成。
     *
     * @param keys 要刪除的快取鍵集合
     * @return 表示所有刪除操作完成的 Mono
     */
    @Override
    public Mono<Void> deleteAll(Collection<String> keys) {
        return Flux.fromIterable(keys).flatMap(this::delete).then();
    }


    /**
     * 取得快取資料的預設過期時間。
     * 
     * <p>回傳此快取提供者實例的預設過期時間設定值，該值在建構時從
     * 應用程式配置中載入。此時間將用於所有未明確指定過期時間的快取操作。
     *
     * @return 預設過期時間的 Duration 物件
     */
    @Override
    public Duration getDefaultExpire() {
        return DEFAULT_EXPIRE_TIME;
    }
}
