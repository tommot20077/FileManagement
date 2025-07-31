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
 * 基於 Redis 的用戶快取提供者實現。
 * <p>
 * 此實現透過 Redis 雜湊表結構提供高效能的用戶資訊快取服務。快取採用雜湊表設計，
 * 支援單一和批量操作，具備自動過期機制和空值安全處理。
 * <p>
 * 快取結構設計：
 * <ul>
 *   <li>快取前綴：由設定檔案配置，用於區分不同類型的快取資料</li>
 *   <li>快取鍵：使用用戶 ID 作為雜湊鍵</li>
 *   <li>快取值：序列化的用戶物件資訊</li>
 *   <li>過期時間：可自訂或使用預設值，確保資料時效性</li>
 * </ul>
 * <p>
 * 條件性載入機制：
 * <ul>
 *   <li>透過 @ConditionalOnProperty 註解實現條件性載入</li>
 *   <li>當 cache.enable-user-info-cache 設定為 true 時才會載入此 Bean</li>
 *   <li>若未設定該屬性，預設為啟用狀態</li>
 * </ul>
 * <p>
 * 此實現確保執行緒安全性，所有操作均為非阻塞反應式操作，適用於高併發環境。
 * 支援空值檢查、集合操作的邊界條件處理，以及自動的資源清理機制。
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see CacheProvider
 * @see RedisProvider
 * @see CacheProperties
 */
@Component
@CacheProviderType(CacheProviderEnum.USER_CACHE)
@ConditionalOnProperty(prefix = "cache", name = "enable-user-info-cache", havingValue = "true", matchIfMissing = true)
public class UserCacheProviderImpl implements CacheProvider {
    /**
     * Redis 操作提供者，負責執行底層的 Redis 資料操作。
     * <p>
     * 透過此提供者執行雜湊表的讀取、寫入、刪除等操作，
     * 確保與 Redis 伺服器的高效通訊和資料一致性。
     */
    private final RedisProvider redisProvider;

    /**
     * 用戶快取的鍵前綴，用於在 Redis 中區分不同類型的快取資料。
     * <p>
     * 此前綴從快取設定檔案中載入，確保用戶快取與系統其他快取資料的命名空間隔離。
     * 實際的 Redis 鍵格式為：{CACHE_PREFIX}:{hashKey}
     */
    private final String CACHE_PREFIX;

    /**
     * 用戶快取的預設過期時間，當未指定特定過期時間時使用此值。
     * <p>
     * 此時間從快取設定檔案中載入，必須為正數值。過期機制確保快取資料的時效性，
     * 避免長期儲存過時的用戶資訊。
     */
    private final Duration DEFAULT_EXPIRE_TIME;

    /**
     * 建構用戶快取提供者實例。
     * <p>
     * 初始化 Redis 提供者和快取設定參數，包含快取前綴和預設過期時間。
     * 在建構過程中會驗證設定參數的有效性，確保快取服務的正常運作。
     *
     * @param redisProvider Redis 操作提供者，不可為 null
     * @param cacheProperties 快取設定物件，包含用戶快取的相關設定參數
     * @throws IllegalArgumentException 當用戶資訊快取過期時間不為正數時拋出
     */
    public UserCacheProviderImpl(RedisProvider redisProvider, CacheProperties cacheProperties) {
        Assert.isTrue(cacheProperties.getUserInfoCacheExpireTime().isPositive(), "用戶資訊緩存過期時間必須大於0");

        this.redisProvider = redisProvider;
        this.CACHE_PREFIX = cacheProperties.getUserInfoCachePrefix();
        this.DEFAULT_EXPIRE_TIME = cacheProperties.getUserInfoCacheExpireTime();
    }


    /**
     * 從快取中獲取指定鍵的用戶資料。
     * <p>
     * 透過雜湊鍵從 Redis 雜湊表中檢索資料，並將其反序列化為指定類型的物件。
     * 若快取中不存在該鍵或資料已過期，則返回空的 Mono。
     *
     * @param <T> 返回資料的類型參數
     * @param hashKey 雜湊鍵，通常為用戶 ID，不可為 null 或空字串
     * @param clazz 目標類型的 Class 物件，用於反序列化，不可為 null
     * @return 包含用戶資料的 Mono，若快取中無資料則為空 Mono
     */
    public <T> Mono<T> get(String hashKey, Class<T> clazz) {
        return redisProvider.getHashMap(CACHE_PREFIX, hashKey, clazz);
    }


    /**
     * 批量獲取多個鍵的快取資料，返回鍵值對映射。
     * <p>
     * 支援兩種查詢模式：
     * <ul>
     *   <li>當提供具體鍵集合時，僅檢索指定的鍵</li>
     *   <li>當鍵集合為空或 null 時，檢索該快取前綴下的所有資料</li>
     * </ul>
     * <p>
     * 此方法透過反應式流程式處理，能夠有效處理大量資料的並行檢索，
     * 並將結果彙整為 Map 結構以便後續處理。
     *
     * @param <T> 返回資料的類型參數
     * @param hashKeys 要查詢的雜湊鍵集合，可為 null 或空集合
     * @param clazz 目標類型的 Class 物件，用於反序列化，不可為 null
     * @return 包含鍵值對映射的 Mono，鍵為雜湊鍵，值為對應的用戶資料
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
     * 設定單一鍵值對的快取資料。
     * <p>
     * 將指定的資料物件序列化後儲存至 Redis 雜湊表中，並設定相應的過期時間。
     * 若未提供過期時間，則使用預設的過期時間設定。
     * <p>
     * 此操作會覆蓋已存在的同鍵資料，並重新設定過期時間。
     *
     * @param hashKey 雜湊鍵，通常為用戶 ID，不可為 null 或空字串
     * @param value 要儲存的資料物件，不可為 null
     * @param expire 快取過期時間，若為 null 則使用預設過期時間
     * @return 表示操作完成的 Mono<Void>
     */
    @Override
    public Mono<Void> set(String hashKey, Object value, Duration expire) {
        Duration chooseTime = Objects.requireNonNullElse(expire, DEFAULT_EXPIRE_TIME);
        return redisProvider.setHashMap(CACHE_PREFIX, hashKey, value, chooseTime);
    }


    /**
     * 批量設定多個鍵值對的快取資料。
     * <p>
     * 將多個資料物件批量序列化並儲存至 Redis 雜湊表中，所有資料共用相同的過期時間。
     * 此操作比多次呼叫單一設定方法更具效率，適用於大量資料的初始化或更新場景。
     * <p>
     * 若映射中包含已存在的鍵，將會覆蓋原有資料並重新設定過期時間。
     *
     * @param keyValues 包含鍵值對的映射，鍵為雜湊鍵，值為要儲存的資料物件，不可為 null
     * @param expire 所有資料的統一過期時間，若為 null 則使用預設過期時間
     * @return 表示操作完成的 Mono<Void>
     */
    @Override
    public Mono<Void> setAll(Map<String, Object> keyValues, Duration expire) {
        Duration chooseTime = Objects.requireNonNullElse(expire, DEFAULT_EXPIRE_TIME);
        return redisProvider.setHashMapAll(CACHE_PREFIX, keyValues, chooseTime);

    }


    /**
     * 刪除指定鍵的快取資料。
     * <p>
     * 從 Redis 雜湊表中移除指定雜湊鍵對應的資料項目。
     * 若該鍵不存在，操作仍會正常完成而不會產生錯誤。
     * <p>
     * 此操作為原子性操作，確保資料的一致性。
     *
     * @param hashKey 要刪除的雜湊鍵，通常為用戶 ID，不可為 null 或空字串
     * @return 表示操作完成的 Mono<Void>
     */
    @Override
    public Mono<Void> delete(String hashKey) {
        return redisProvider.deleteHash(CACHE_PREFIX, hashKey).then();
    }


    /**
     * 批量刪除多個鍵的快取資料。
     * <p>
     * 支援兩種刪除模式：
     * <ul>
     *   <li>當提供具體鍵集合時，僅刪除指定的鍵</li>
     *   <li>當鍵集合為空或 null 時，清空該快取前綴下的所有資料</li>
     * </ul>
     * <p>
     * 批量刪除操作為原子性操作，比多次呼叫單一刪除方法更具效率。
     * 不存在的鍵會被忽略，不會影響操作的執行。
     *
     * @param hashKeys 要刪除的雜湊鍵集合，若為 null 或空集合則清空整個快取
     * @return 表示操作完成的 Mono<Void>
     */
    @Override
    public Mono<Void> deleteAll(Collection<String> hashKeys) {
        if (hashKeys == null || hashKeys.isEmpty()) {
            return redisProvider.deleteHash(CACHE_PREFIX).then();
        }
        return redisProvider.deleteHash(CACHE_PREFIX, hashKeys.stream().toList()).then();
    }


    /**
     * 獲取用戶快取的預設過期時間。
     * <p>
     * 返回從設定檔案中載入的預設過期時間值，此值在物件建構時確定且不可變更。
     * 當執行快取操作時未指定特定過期時間時，將使用此預設值。
     *
     * @return 預設過期時間 Duration 物件，保證為正數值
     */
    @Override
    public Duration getDefaultExpire() {
        return DEFAULT_EXPIRE_TIME;
    }
}
