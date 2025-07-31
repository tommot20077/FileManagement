package xyz.dowob.filemanagement.component.provider.providerInterface;

import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 反應式緩存服務提供者統一介面，定義非阻塞緩存操作的標準規範。
 *
 * <p>此介面基於 Spring WebFlux 反應式程式設計模型，提供高效能且低延遲的緩存操作抽象層。
 * 採用策略模式設計，支援多種緩存後端實現的可插拔架構，確保系統的靈活性和可擴展性。</p>
 *
 * <h3>核心設計理念：</h3>
 * <ul>
 *   <li><strong>反應式非阻塞</strong>：所有操作均回傳 {@link Mono} 類型，確保非阻塞特性</li>
 *   <li><strong>類型安全</strong>：完整的泛型支援，編譯時型別檢查</li>
 *   <li><strong>可插拔架構</strong>：透過實現此介面支援不同的緩存技術</li>
 *   <li><strong>統一介面</strong>：為上層應用提供一致的緩存操作體驗</li>
 * </ul>
 *
 * <h3>支援的操作類型：</h3>
 * <ul>
 *   <li><strong>查詢操作</strong>：單一值、列表、批量查詢</li>
 *   <li><strong>寫入操作</strong>：單一設定、批量設定，支援過期時間</li>
 *   <li><strong>刪除操作</strong>：單一刪除、批量刪除</li>
 *   <li><strong>管理操作</strong>：預設過期時間配置</li>
 * </ul>
 *
 * <h3>實現要求：</h3>
 * <ul>
 *   <li>所有實現類必須提供 {@link #getDefaultExpire()} 方法的具體實現</li>
 *   <li>建議重寫預設方法以提供實際的緩存功能</li>
 *   <li>實現類應處理序列化/反序列化邏輯</li>
 *   <li>需要考慮並發安全性和錯誤處理</li>
 * </ul>
 *
 * <h3>常見實現類型：</h3>
 * <ul>
 *   <li><strong>Redis 實現</strong>：基於 Redis 的分散式緩存</li>
 *   <li><strong>本地記憶體實現</strong>：基於 JVM 記憶體的本地緩存</li>
 *   <li><strong>混合實現</strong>：結合多級緩存的實現</li>
 * </ul>
 *
 * <h3>使用範例：</h3>
 * <pre>{@code
 * // 注入緩存提供者
 * @Autowired
 * private CacheProvider cacheProvider;
 *
 * // 單一值操作
 * Mono<User> user = cacheProvider.get("user:123", User.class);
 * Mono<Void> setResult = cacheProvider.set("user:123", userObj, Duration.ofHours(1));
 *
 * // 批量操作
 * Mono<Map<String, User>> users = cacheProvider.getAllAsMap(userKeys, User.class);
 * Mono<Void> setAllResult = cacheProvider.setAll(userMap, Duration.ofMinutes(30));
 * }</pre>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see Mono
 * @see Duration
 * @see xyz.dowob.filemanagement.annotation.CacheProviderType
 * @see xyz.dowob.filemanagement.component.manager.CacheManager
 */

public interface CacheProvider {

    /**
     * 異步獲取單一緩存值。
     *
     * <p>此方法用於查詢指定鍵對應的單一緩存值，支援任意類型的資料反序列化。
     * 若緩存不存在，回傳空的 {@link Mono}。</p>
     *
     * <p><strong>適用場景：</strong></p>
     * <ul>
     *   <li>用戶資訊查詢</li>
     *   <li>系統配置讀取</li>
     *   <li>業務物件快速存取</li>
     * </ul>
     *
     * <p><strong>實現注意事項：</strong></p>
     * <ul>
     *   <li>需要處理鍵不存在的情況</li>
     *   <li>應正確處理序列化異常</li>
     *   <li>建議支援型別安全的反序列化</li>
     * </ul>
     *
     * @param <T> 緩存值的泛型類型
     * @param key 緩存鍵，不能為 {@code null}
     * @param clazz 預期的值類型，用於反序列化
     *
     * @return {@link Mono<T>} 包含緩存值的反應式物件，若不存在則為空
     *
     * @see #getAsList(String, Class)
     * @see #getAllAsMap(Collection, Class)
     */
    default <T> Mono<T> get(String key, Class<T> clazz) {
        return Mono.empty();
    }


    /**
     * 異步獲取指定鍵的列表類型緩存。
     *
     * <p>此方法專門用於獲取以列表形式儲存的緩存資料，
     * 適用於需要保持資料順序的場景。</p>
     *
     * <p><strong>適用場景：</strong></p>
     * <ul>
     *   <li>用戶的歷史記錄列表</li>
     *   <li>商品分類下的項目列表</li>
     *   <li>有序的搜尋結果</li>
     * </ul>
     *
     * <p><strong>與 {@link #get(String, Class)} 的差異：</strong></p>
     * <ul>
     *   <li>{@code get}：適用於單一物件</li>
     *   <li>{@code getAsList}：專門處理列表類型資料</li>
     * </ul>
     *
     * <p><strong>實現建議：</strong></p>
     * <ul>
     *   <li>應保持列表元素的原始順序</li>
     *   <li>正確處理空列表和 null 值</li>
     *   <li>考慮大型列表的記憶體使用</li>
     * </ul>
     *
     * @param <T> 列表元素的泛型類型
     * @param key 緩存鍵，不能為 {@code null}
     * @param clazz 列表元素的類型，用於反序列化
     *
     * @return Mono<List<T>> 包含列表的反應式物件，若不存在則為空
     *
     * @see #get(String, Class)
     * @see #getAllAsMapList(Collection, Class)
     */
    default <T> Mono<List<T>> getAsList(String key, Class<T> clazz) {
        return Mono.empty();
    }


    /**
     * 批量獲取多個鍵的緩存值，回傳鍵值對應的 Map。
     *
     * <p>此方法提供高效的批量查詢能力，一次性獲取多個緩存項目，
     * 相比多次單獨查詢具有更好的效能表現。</p>
     *
     * <p><strong>回傳值特性：</strong></p>
     * <ul>
     *   <li>鍵：原始查詢的緩存鍵</li>
     *   <li>值：對應的緩存資料</li>
     *   <li>不存在的鍵不會出現在結果 Map 中</li>
     * </ul>
     *
     * <p><strong>效能優勢：</strong></p>
     * <ul>
     *   <li>減少網路往返次數</li>
     *   <li>降低系統整體延遲</li>
     *   <li>提高緩存系統的處理效率</li>
     * </ul>
     *
     * <p><strong>適用場景：</strong></p>
     * <ul>
     *   <li>多用戶資訊批量查詢</li>
     *   <li>商品資訊批量獲取</li>
     *   <li>需要知道緩存命中情況的場景</li>
     * </ul>
     *
     * <p><strong>實現注意事項：</strong></p>
     * <ul>
     *   <li>應正確處理部分鍵不存在的情況</li>
     *   <li>避免在大量鍵查詢時的記憶體問題</li>
     *   <li>考慮實現批量查詢的原子性</li>
     * </ul>
     *
     * @param <T> 緩存值的泛型類型
     * @param keys 緩存鍵的集合，不能為 {@code null}
     * @param clazz 緩存值的類型，用於反序列化
     *
     * @return Mono<Map<String, T>> 包含鍵值對應關係的反應式物件
     *
     * @see #get(String, Class)
     * @see #getAllAsMapList(Collection, Class)
     */
    default <T> Mono<Map<String, T>> getAllAsMap(Collection<String> keys, Class<T> clazz) {
        return Mono.empty();
    }


    /**
     * 批量獲取多個鍵的列表類型緩存，回傳 Map 結構。
     *
     * <p>此方法結合了批量查詢和列表處理的功能，
     * 適用於需要一次性獲取多個列表類型緩存的場景。</p>
     *
     * <p><strong>回傳結構：</strong></p>
     * <ul>
     *   <li>外層 Map：鍵為原始查詢鍵，值為對應的列表</li>
     *   <li>內層 List：每個鍵對應的具體資料列表</li>
     * </ul>
     *
     * <p><strong>適用場景：</strong></p>
     * <ul>
     *   <li>多個用戶的訂單列表批量查詢</li>
     *   <li>不同分類的商品列表獲取</li>
     *   <li>多個群組的成員列表查詢</li>
     * </ul>
     *
     * <p><strong>與其他方法的關係：</strong></p>
     * <ul>
     *   <li>{@link #getAllAsMap}：批量獲取單一值</li>
     *   <li>{@code getAllAsMapList}：批量獲取列表值</li>
     *   <li>{@link #getAsList}：單一鍵的列表查詢</li>
     * </ul>
     *
     * <p><strong>效能考量：</strong></p>
     * <ul>
     *   <li>適合中等規模的批量列表查詢</li>
     *   <li>需要注意總體資料量對記憶體的影響</li>
     *   <li>建議對大型列表進行分頁處理</li>
     * </ul>
     *
     * @param <T> 列表元素的泛型類型
     * @param keys 緩存鍵的集合，不能為 {@code null}
     * @param clazz 列表元素的類型，用於反序列化
     *
     * @return Mono<Map<String, List<T>>> 包含鍵到列表映射的反應式物件
     *
     * @see #getAllAsMap(Collection, Class)
     * @see #getAsList(String, Class)
     */
    default <T> Mono<Map<String, List<T>>> getAllAsMapList(Collection<String> keys, Class<T> clazz) {
        return Mono.empty();
    }


    /**
     * 異步設定單一緩存值，支援自訂過期時間。
     *
     * <p>此方法是緩存寫入的核心操作，支援任意類型的資料序列化和儲存。
     * 提供靈活的過期時間控制，確保緩存資料的時效性。</p>
     *
     * <p><strong>過期時間處理：</strong></p>
     * <ul>
     *   <li>{@code null}：使用實現類定義的預設過期時間</li>
     *   <li>正值：使用指定的過期時間</li>
     *   <li>零值：立即過期（實際行為依實現而定）</li>
     *   <li>負值：永不過期（如果實現支援）</li>
     * </ul>
     *
     * <p><strong>適用場景：</strong></p>
     * <ul>
     *   <li>用戶登入狀態緩存</li>
     *   <li>臨時計算結果儲存</li>
     *   <li>會話資料管理</li>
     * </ul>
     *
     * <p><strong>實現要求：</strong></p>
     * <ul>
     *   <li>應正確處理值的序列化</li>
     *   <li>需要支援過期時間的設定</li>
     *   <li>建議支援值的覆蓋更新</li>
     *   <li>處理序列化異常情況</li>
     * </ul>
     *
     * <p><strong>併發安全：</strong></p>
     * <p>實現類應確保此操作的執行緒安全性，
     * 特別是在高併發環境下的資料一致性。</p>
     *
     * @param key 緩存鍵，不能為 {@code null}
     * @param value 要緩存的值，可以為 {@code null}（取決於實現）
     * @param expire 過期時間，{@code null} 表示使用預設過期策略
     *
     * @return {@link Mono<Void>} 表示設定操作完成的反應式物件
     *
     * @see #setAll(Map, Duration)
     * @see #getDefaultExpire()
     */
    default Mono<Void> set(String key, Object value, Duration expire) {
        return Mono.empty();
    }


    /**
     * 批量設定多個緩存值，使用統一的過期時間。
     *
     * <p>此方法提供高效的批量緩存設定能力，
     * 相比多次單獨設定具有更好的效能和一致性保證。</p>
     *
     * <p><strong>批量操作優勢：</strong></p>
     * <ul>
     *   <li>減少網路往返次數</li>
     *   <li>提高整體寫入效能</li>
     *   <li>確保批量操作的原子性（如果實現支援）</li>
     *   <li>降低系統資源消耗</li>
     * </ul>
     *
     * <p><strong>適用場景：</strong></p>
     * <ul>
     *   <li>用戶資料的批量初始化</li>
     *   <li>配置項目的批量更新</li>
     *   <li>計算結果的批量緩存</li>
     * </ul>
     *
     * <p><strong>過期時間策略：</strong></p>
     * <p>所有設定的緩存項目將使用相同的過期時間，
     * 這有助於保持相關資料的一致性生命週期。</p>
     *
     * <p><strong>實現注意事項：</strong></p>
     * <ul>
     *   <li>應考慮批量操作的原子性</li>
     *   <li>處理部分設定失敗的情況</li>
     *   <li>避免大批量操作導致的記憶體問題</li>
     *   <li>正確處理空 Map 或 null 輸入</li>
     * </ul>
     *
     * @param keyValues 鍵值對映射，鍵為緩存鍵，值為要緩存的資料
     * @param expire 統一的過期時間，{@code null} 表示使用預設策略
     *
     * @return {@link Mono<Void>} 表示批量設定操作完成的反應式物件
     *
     * @see #set(String, Object, Duration)
     * @see #getDefaultExpire()
     */
    default Mono<Void> setAll(Map<String, Object> keyValues, Duration expire) {
        return Mono.empty();
    }


    /**
     * 異步刪除單一緩存項目。
     *
     * <p>此方法用於移除指定鍵的緩存資料，
     * 是緩存生命週期管理的重要組成部分。</p>
     *
     * <p><strong>刪除特性：</strong></p>
     * <ul>
     *   <li>立即生效：刪除操作立即生效</li>
     *   <li>安全操作：刪除不存在的鍵不會產生錯誤</li>
     *   <li>原子操作：刪除操作應是原子性的</li>
     * </ul>
     *
     * <p><strong>適用場景：</strong></p>
     * <ul>
     *   <li>用戶登出時清理會話</li>
     *   <li>資料更新後移除舊緩存</li>
     *   <li>手動的緩存清理</li>
     * </ul>
     *
     * <p><strong>與過期的差異：</strong></p>
     * <ul>
     *   <li><strong>主動刪除</strong>：立即移除，不等待過期時間</li>
     *   <li><strong>被動過期</strong>：由系統自動處理過期項目</li>
     * </ul>
     *
     * <p><strong>實現要求：</strong></p>
     * <ul>
     *   <li>應正確處理鍵不存在的情況</li>
     *   <li>確保刪除操作的原子性</li>
     *   <li>避免刪除操作產生異常</li>
     * </ul>
     *
     * @param key 要刪除的緩存鍵，不能為 {@code null}
     *
     * @return {@link Mono<Void>} 表示刪除操作完成的反應式物件
     *
     * @see #deleteAll(Collection)
     * @see #set(String, Object, Duration)
     */
    default Mono<Void> delete(String key) {
        return Mono.empty();
    }


    /**
     * 批量刪除多個緩存項目。
     *
     * <p>此方法提供高效的批量緩存刪除能力，
     * 適用於需要清理大量相關緩存的場景。</p>
     *
     * <p><strong>批量刪除優勢：</strong></p>
     * <ul>
     *   <li>效能提升：減少多次單獨刪除的開銷</li>
     *   <li>原子性：確保批量操作的一致性（如果實現支援）</li>
     *   <li>簡化操作：單一調用處理多個刪除需求</li>
     * </ul>
     *
     * <p><strong>適用場景：</strong></p>
     * <ul>
     *   <li>用戶資料的批量清理</li>
     *   <li>過期緩存的批量移除</li>
     *   <li>系統維護期間的緩存清理</li>
     * </ul>
     *
     * <p><strong>安全特性：</strong></p>
     * <ul>
     *   <li>部分鍵不存在不會影響其他鍵的刪除</li>
     *   <li>空集合或 null 輸入應被安全處理</li>
     *   <li>重複鍵的處理應是冪等的</li>
     * </ul>
     *
     * <p><strong>實現注意事項：</strong></p>
     * <ul>
     *   <li>考慮批量操作的原子性</li>
     *   <li>處理大量鍵刪除的效能問題</li>
     *   <li>正確處理部分刪除失敗的情況</li>
     * </ul>
     *
     * @param keys 要刪除的緩存鍵集合，不能為 {@code null}
     *
     * @return {@link Mono<Void>} 表示批量刪除操作完成的反應式物件
     *
     * @see #delete(String)
     * @see #setAll(Map, Duration)
     */
    default Mono<Void> deleteAll(Collection<String> keys) {
        return Mono.empty();
    }


    /**
     * 獲取實現類定義的預設過期時間。
     *
     * <p>此方法是介面中唯一的抽象方法，所有實現類必須提供具體實現。
     * 預設過期時間用於當緩存操作未指定過期時間時的回退策略。</p>
     *
     * <p><strong>設計考量：</strong></p>
     * <ul>
     *   <li>提供合理的預設值，避免緩存永不過期</li>
     *   <li>考慮不同類型緩存的特性</li>
     *   <li>平衡緩存命中率和資料時效性</li>
     * </ul>
     *
     * <p><strong>常見的過期時間設定：</strong></p>
     * <ul>
     *   <li><strong>用戶會話</strong>：30分鐘 - 2小時</li>
     *   <li><strong>配置資料</strong>：1小時 - 24小時</li>
     *   <li><strong>計算結果</strong>：5分鐘 - 1小時</li>
     *   <li><strong>靜態資料</strong>：24小時 - 7天</li>
     * </ul>
     *
     * <p><strong>實現建議：</strong></p>
     * <ul>
     *   <li>可從配置檔案讀取預設值</li>
     *   <li>考慮提供不同級別的預設時間</li>
     *   <li>確保回傳值不為 {@code null}</li>
     * </ul>
     *
     * <p><strong>使用場景：</strong></p>
     * <ul>
     *   <li>作為 {@link #set(String, Object, Duration)} 的回退值</li>
     *   <li>作為 {@link #setAll(Map, Duration)} 的回退值</li>
     *   <li>為緩存管理提供預設策略</li>
     * </ul>
     *
     * @return 預設過期時間，不能為 {@code null}
     *
     * @see #set(String, Object, Duration)
     * @see #setAll(Map, Duration)
     * @see Duration
     */
    Duration getDefaultExpire();
}