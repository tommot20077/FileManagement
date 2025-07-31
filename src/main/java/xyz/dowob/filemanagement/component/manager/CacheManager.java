package xyz.dowob.filemanagement.component.manager;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import xyz.dowob.filemanagement.annotation.CacheProviderType;
import xyz.dowob.filemanagement.annotation.SkipRecord;
import xyz.dowob.filemanagement.component.provider.provider.RedisProvider;
import xyz.dowob.filemanagement.component.provider.providerInterface.CacheProvider;
import xyz.dowob.filemanagement.customenum.CacheProviderEnum;
import xyz.dowob.filemanagement.data.datainterface.FluxContainer;
import xyz.dowob.filemanagement.functionInterface.CacheRule;
import xyz.dowob.filemanagement.unity.LogUnity;

import java.lang.reflect.AnnotatedElement;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 統一緩存管理器，負責協調和管理多種緩存提供者的策略實現。
 *
 * <p>本類採用策略模式設計，提供了靈活且高效的緩存操作機制，
 * 是整個應用程式緩存系統的核心協調者。主要職責包括：</p>
 *
 * <h3>核心功能特性：</h3>
 * <ul>
 *   <li><strong>多策略支援</strong>：根據 {@link CacheProviderEnum} 動態選取對應的 {@link CacheProvider}</li>
 *   <li><strong>反應式程式設計</strong>：全面支援 {@link Mono} 和 {@link Flux} 的異步操作</li>
 *   <li><strong>分散式鎖機制</strong>：使用 Redis 實現分散式鎖，確保緩存操作的原子性</li>
 *   <li><strong>延遲雙刪</strong>：實現延遲雙刪機制，有效防止緩存與資料庫的不一致</li>
 *   <li><strong>泛型支援</strong>：提供完整的泛型支援，確保類型安全</li>
 *   <li><strong>批量操作</strong>：支援高效的批量緩存讀寫操作</li>
 * </ul>
 *
 * <h3>緩存操作模式：</h3>
 * <ul>
 *   <li><strong>基本 CRUD</strong>：提供標準的增刪改查操作</li>
 *   <li><strong>Cache-Aside</strong>：支援穿透式緩存模式</li>
 *   <li><strong>Write-Behind</strong>：提供異步寫入機制</li>
 *   <li><strong>自訂規則</strong>：支援複雜的緩存規則應用邏輯</li>
 * </ul>
 *
 * <h3>分散式鎖特性：</h3>
 * <p>所有涉及寫操作的方法都會自動獲取分散式鎖，確保多實例環境下的資料一致性。
 * 鎖的獲取採用「全有或全無」策略，避免部分獲取鎖導致的死鎖問題。</p>
 *
 * <h3>使用範例：</h3>
 * <pre>{@code
 * // 單一值緩存
 * Mono<User> user = cacheManager.runAndSetCache(
 *     "user:123", User.class, CacheProviderEnum.USER_CACHE,
 *     userService.findById(123),
 *     List.of(generateCacheRule("user:123", CacheProviderEnum.USER_CACHE))
 * );
 *
 * // 批量緩存操作
 * Flux<User> users = cacheManager.runAndSetCache(
 *     Arrays.asList("user:1", "user:2"), User.class, CacheProviderEnum.USER_CACHE,
 *     userService.findByIds(Arrays.asList(1, 2)),
 *     cacheRules
 * );
 * }</pre>
 *
 * @author yuan
 * @version 1.0
 * @see CacheProvider
 * @see CacheProviderEnum
 * @see RedisProvider
 * @see CacheRule
 * @since 1.0
 */
@Component
@SuppressWarnings("unused")
public class CacheManager {
    /**
     * Redis 分散式鎖的鍵前綴。
     *
     * <p>用於構建分散式鎖的完整鍵名，格式為：{@code cache_lock:提供者類型:業務鍵}。
     * 此前綴確保緩存鎖與其他業務鎖的鍵不會發生命名衝突。</p>
     *
     * @see #executeWithLock(Collection, CacheProviderEnum, Function)
     */
    private static final String LOCK_PREFIX = "cache_lock:";

    /**
     * Redis 分散式鎖的預設值。
     *
     * <p>當成功獲取鎖時，會將此值設定到 Redis 中作為鎖的標識。
     * 使用固定值可簡化鎖的管理邏輯，避免複雜的所有權驗證。</p>
     */
    private static final String LOCK_VALUE = "locked";

    /**
     * Redis 分散式鎖的預設過期時間。
     *
     * <p>設定為 1 分鐘，防止因程式異常退出導致的死鎖問題。
     * 過期時間應該根據實際業務操作的複雜度進行調整：</p>
     * <ul>
     *   <li>簡單緩存操作：30秒 - 1分鐘</li>
     *   <li>複雜批量操作：2-5分鐘</li>
     *   <li>長時間處理：需要實現鎖續約機制</li>
     * </ul>
     */
    private static final Duration DEFAULT_EXPIRE_TIME = Duration.ofMinutes(1);

    /**
     * 延遲雙刪機制的預設延遲時間。
     *
     * <p>設定為 3 秒，這個時間間隔是基於以下考量：</p>
     * <ul>
     *   <li>足夠大，確保資料庫主從同步完成</li>
     *   <li>足夠小，減少緩存不一致的時間窗口</li>
     *   <li>適合大多數業務場景的資料同步延遲</li>
     * </ul>
     *
     * <p>實際使用時可根據資料庫同步延遲調整此值。</p>
     *
     * @see #deleteCacheWithDelayedDoubleDelete(String, CacheProviderEnum)
     */
    private static final Duration DEFAULT_DELAYED_DELETE_TIME = Duration.ofSeconds(3);

    /**
     * 緩存提供者策略映射表。
     *
     * <p>使用 {@link EnumMap} 儲存不同類型的緩存提供者實例，提供高效的策略查找機制。
     * 映射關係由 {@link CacheProviderType} 註解自動建立，支援運行時動態註冊新的提供者。</p>
     *
     * <p>支援的緩存提供者類型：</p>
     * <ul>
     *   <li>{@link CacheProviderEnum#USER_CACHE} - 使用者資料緩存提供者</li>
     *   <li>{@link CacheProviderEnum#FILE_STREAM_CACHE} - 檔案流緩存提供者</li>
     *   <li>{@link CacheProviderEnum#USER_FILE_LIST_CACHE} - 使用者檔案列表緩存提供者</li>
     * </ul>
     *
     * @see CacheProviderEnum
     * @see CacheProviderType
     */
    private static final EnumMap<CacheProviderEnum, CacheProvider> cacheProviderMap = new EnumMap<>(CacheProviderEnum.class);

    /**
     * Redis 操作提供者。
     *
     * <p>專門用於分散式鎖的管理，提供原子性的 Redis 操作能力。
     * 主要用於以下場景：</p>
     * <ul>
     *   <li>獲取和釋放分散式鎖</li>
     *   <li>設定帶過期時間的鍵值對</li>
     *   <li>批量刪除操作</li>
     * </ul>
     *
     * @see RedisProvider
     */
    private final RedisProvider redisProvider;


    /**
     * 緩存管理器建構子，負責初始化緩存提供者策略映射。
     *
     * <p>建構過程會自動掃描所有 {@link CacheProvider} 實例，
     * 根據其 {@link CacheProviderType} 註解建立策略映射關係。</p>
     *
     * <p><strong>初始化流程：</strong></p>
     * <ol>
     *   <li>注入 Redis 提供者用於分散式鎖管理</li>
     *   <li>遍歷所有緩存提供者實例</li>
     *   <li>透過反射機制獲取 {@link CacheProviderType} 註解</li>
     *   <li>建立枚舉類型到提供者實例的映射關係</li>
     * </ol>
     *
     * <p><strong>註解掃描機制：</strong></p>
     * <p>使用 {@link AnnotatedElementUtils#findMergedAnnotation} 確保能夠正確處理：</p>
     * <ul>
     *   <li>直接標註的註解</li>
     *   <li>元註解（meta-annotation）</li>
     *   <li>合成註解（composed annotation）</li>
     * </ul>
     *
     * @param cacheProviderList Spring 容器中所有的緩存提供者實例列表
     * @param redisProvider     Redis 操作提供者，用於分散式鎖管理
     *
     * @see CacheProviderType
     * @see AnnotatedElementUtils#findMergedAnnotation(AnnotatedElement, Class)
     */
    public CacheManager(List<CacheProvider> cacheProviderList, RedisProvider redisProvider) {
        this.redisProvider = redisProvider;
        for (CacheProvider provider : cacheProviderList) {
            CacheProviderType cacheProviderType = AnnotatedElementUtils.findMergedAnnotation(provider.getClass(), CacheProviderType.class);
            if (cacheProviderType != null) {
                cacheProviderMap.put(cacheProviderType.value(), provider);
            }
        }
    }


    /**
     * 靜態方法：根據物件屬性生成緩存規則。
     *
     * <p>此方法提供了一種便利的方式來建立緩存規則，
     * 通過函數式介面從物件中提取屬性作為緩存鍵。</p>
     *
     * <p><strong>使用範例：</strong></p>
     * <pre>{@code
     * // 根據用戶 ID 建立緩存規則
     * CacheRule<User> userCacheRule = CacheManager.generateCacheRule(
     *     user -> "user:" + user.getId(),
     *     CacheProviderEnum.USER_CACHE
     * );
     *
     * // 根據檔案 ID 建立緩存規則
     * CacheRule<FileStream> fileCacheRule = CacheManager.generateCacheRule(
     *     file -> "file:" + file.getId(),
     *     CacheProviderEnum.FILE_STREAM_CACHE
     * );
     * }</pre>
     *
     * <p><strong>適用場景：</strong></p>
     * <ul>
     *   <li>實體物件的主鍵緩存</li>
     *   <li>基於業務屬性的緩存鍵生成</li>
     *   <li>動態緩存鍵策略</li>
     * </ul>
     *
     * <p><strong>注意事項：</strong></p>
     * <ul>
     *   <li>確保提取器函數不會回傳 {@code null}</li>
     *   <li>提取的屬性應該具有唯一性</li>
     *   <li>避免使用可變的屬性作為緩存鍵</li>
     * </ul>
     *
     * @param <R>               物件的類型
     * @param keyExtractor      從物件中提取緩存鍵的函數
     * @param cacheProviderEnum 緩存提供者類型
     *
     * @return {@link CacheRule<R>} 生成的緩存規則
     *
     * @see CacheRule
     * @see SkipRecord
     */
    @SkipRecord
    public static <R> CacheRule<R> generateCacheRule(Function<R, ?> keyExtractor, CacheProviderEnum cacheProviderEnum) {
        return (value, expire) -> {
            String key = keyExtractor.apply(value).toString();
            return Optional
                    .ofNullable(cacheProviderMap.get(cacheProviderEnum))
                    .map(provider -> provider.set(key, value, expire))
                    .orElseGet(Mono::empty);
        };
    }


    /**
     * 設定或替換指定類型的緩存提供者。
     *
     * <p>此方法允許在運行時動態註冊或替換緩存提供者，
     * 主要用於以下場景：</p>
     * <ul>
     *   <li>測試環境中替換為模擬實現</li>
     *   <li>運行時切換緩存策略</li>
     *   <li>動態載入新的緩存提供者</li>
     *   <li>緩存提供者的熱升級</li>
     * </ul>
     *
     * <p><strong>注意事項：</strong></p>
     * <ul>
     *   <li>替換操作是立即生效的，正在進行的操作不受影響</li>
     *   <li>建議在系統初始化或維護期間進行替換</li>
     *   <li>替換後的提供者需要實現相同的介面契約</li>
     * </ul>
     *
     * @param cacheProviderEnum 緩存提供者的類型枚舉
     * @param cacheProvider     新的緩存提供者實例
     *
     * @see CacheProviderEnum
     * @see CacheProvider
     */
    public void setCacheProvider(CacheProviderEnum cacheProviderEnum, CacheProvider cacheProvider) {
        cacheProviderMap.put(cacheProviderEnum, cacheProvider);
    }


    /**
     * 根據類型枚舉獲取對應的緩存提供者實例。
     *
     * <p>此方法提供策略模式的核心查找邏輯，
     * 根據業務需求動態選擇適當的緩存實現。</p>
     *
     * <p><strong>使用場景：</strong></p>
     * <ul>
     *   <li>需要直接操作特定緩存提供者</li>
     *   <li>實現自訂的緩存邏輯</li>
     *   <li>調試和監控特定緩存提供者的狀態</li>
     * </ul>
     *
     * @param cacheProviderEnum 緩存提供者的類型枚舉
     *
     * @return 對應的緩存提供者實例，若未找到則回傳 {@code null}
     *
     * @see CacheProviderEnum
     * @see CacheProvider
     */
    public CacheProvider getCacheProvider(CacheProviderEnum cacheProviderEnum) {
        return cacheProviderMap.get(cacheProviderEnum);
    }


    /**
     * 異步獲取單一緩存值。
     *
     * <p>此方法用於查詢單一緩存項目，適用於簡單的鍵值查詢場景。
     * 採用反應式程式設計模式，提供非阻塞的緩存存取能力。</p>
     *
     * <p><strong>適用場景：</strong></p>
     * <ul>
     *   <li>用戶資訊查詢</li>
     *   <li>配置項目讀取</li>
     *   <li>簡單的業務物件獲取</li>
     * </ul>
     *
     * <p><strong>錯誤處理：</strong></p>
     * <p>當指定的緩存提供者不存在時，方法會回傳空的 {@link Mono}，
     * 不會拋出異常，確保呼叫方的程式流程不被中斷。</p>
     *
     * @param <R>               緩存值的類型
     * @param key               緩存鍵，不能為 {@code null}
     * @param clazz             預期的回傳類型，用於反序列化
     * @param cacheProviderEnum 緩存提供者類型
     *
     * @return {@link Mono<R>} 包含緩存值的反應式物件，若緩存不存在或提供者不存在則為空
     *
     * @see Mono
     * @see CacheProvider#get(String, Class)
     */
    public <R> Mono<R> getCacheMono(String key, Class<R> clazz, CacheProviderEnum cacheProviderEnum) {
        return Optional.ofNullable(cacheProviderMap.get(cacheProviderEnum)).map(provider -> provider.get(key, clazz)).orElseGet(Mono::empty);
    }


    /**
     * 異步獲取集合類型的緩存值。
     *
     * <p>此方法專門用於獲取以列表形式儲存的緩存資料，
     * 並將結果轉換為 {@link Flux} 流式處理。</p>
     *
     * <p><strong>適用場景：</strong></p>
     * <ul>
     *   <li>用戶列表查詢</li>
     *   <li>商品目錄獲取</li>
     *   <li>配置項目列表</li>
     *   <li>任何需要流式處理的集合資料</li>
     * </ul>
     *
     * <p><strong>處理流程：</strong></p>
     * <ol>
     *   <li>從緩存提供者獲取列表形式的資料</li>
     *   <li>將列表轉換為 {@link Flux} 流</li>
     *   <li>支援後續的流式操作（過濾、映射、聚合等）</li>
     * </ol>
     *
     * @param <R>               集合元素的類型
     * @param key               緩存鍵，不能為 {@code null}
     * @param clazz             集合元素的類型，用於反序列化
     * @param cacheProviderEnum 緩存提供者類型
     *
     * @return {@link Flux<R>} 包含緩存資料的反應式流，若緩存不存在或提供者不存在則為空流
     *
     * @see Flux
     * @see CacheProvider#getAsList(String, Class)
     */
    public <R> Flux<R> getCacheFlux(String key, Class<R> clazz, CacheProviderEnum cacheProviderEnum) {
        return Optional
                .ofNullable(cacheProviderMap.get(cacheProviderEnum))
                .map(provider -> provider.getAsList(key, clazz).flatMapMany(Flux::fromIterable))
                .orElseGet(Flux::empty);
    }


    /**
     * 批量獲取以列表形式組織的緩存資料。
     *
     * <p>此方法用於一次性查詢多個鍵對應的列表類型緩存，
     * 回傳結果為 {@code Map<String, List<R>>} 的形式，
     * 其中鍵為原始查詢鍵，值為對應的列表資料。</p>
     *
     * <p><strong>適用場景：</strong></p>
     * <ul>
     *   <li>多個用戶的訂單列表批量查詢</li>
     *   <li>不同分類的商品列表獲取</li>
     *   <li>多個群組的成員列表查詢</li>
     * </ul>
     *
     * <p><strong>效能優勢：</strong></p>
     * <p>相比多次單獨查詢，批量操作能夠：</p>
     * <ul>
     *   <li>減少網路往返次數</li>
     *   <li>提高緩存提供者的處理效率</li>
     *   <li>降低系統整體延遲</li>
     * </ul>
     *
     * @param <R>               列表元素的類型
     * @param keys              緩存鍵的集合，不能為 {@code null} 或空
     * @param clazz             列表元素的類型，用於反序列化
     * @param cacheProviderEnum 緩存提供者類型
     *
     * @return Mono<Map < String, List < R>>> 包含鍵到列表映射的反應式物件
     *
     * @see CacheProvider#getAllAsMapList(Collection, Class)
     */
    public <R> Mono<Map<String, List<R>>> getListCaches(Collection<String> keys, Class<R> clazz, CacheProviderEnum cacheProviderEnum) {
        return Optional
                .ofNullable(cacheProviderMap.get(cacheProviderEnum))
                .map(provider -> provider.getAllAsMapList(keys, clazz))
                .orElseGet(Mono::empty);
    }


    /**
     * 批量獲取緩存資料並合併為單一資料流。
     *
     * <p>此方法將多個鍵對應的緩存值合併為一個連續的 {@link Flux} 流，
     * 忽略鍵的對應關係，適用於需要統一處理所有結果的場景。</p>
     *
     * <p><strong>與 {@link #getCaches} 的差異：</strong></p>
     * <ul>
     *   <li>{@code getCaches}：保持鍵值對應關係，回傳 Map</li>
     *   <li>{@code getCachesAsConcat}：捨棄鍵對應關係，回傳合併流</li>
     * </ul>
     *
     * <p><strong>適用場景：</strong></p>
     * <ul>
     *   <li>需要對所有緩存值執行相同操作</li>
     *   <li>聚合統計多個緩存的資料</li>
     *   <li>不關心資料來源鍵的批量處理</li>
     * </ul>
     *
     * <p><strong>空值處理：</strong></p>
     * <p>當查詢結果為空時，方法會回傳空的 {@link HashMap}，
     * 然後轉換為空的 {@link Flux}，確保流式處理的連續性。</p>
     *
     * @param <R>               緩存值的類型
     * @param keys              緩存鍵的集合，不能為 {@code null}
     * @param clazz             緩存值的類型，用於反序列化
     * @param cacheProviderEnum 緩存提供者類型
     *
     * @return {@link Flux<R>} 包含所有緩存值的合併流
     *
     * @see #getCaches(Collection, Class, CacheProviderEnum)
     */
    public <R> Flux<R> getCachesAsConcat(Collection<String> keys, Class<R> clazz, CacheProviderEnum cacheProviderEnum) {
        return getCaches(keys, clazz, cacheProviderEnum)
                .switchIfEmpty(Mono.just(new HashMap<>()))
                .flatMapMany(map -> Flux.fromIterable(map.values()));
    }


    /**
     * 批量獲取緩存資料，保持鍵值對應關係。
     *
     * <p>此方法是批量緩存查詢的核心實現，
     * 回傳 {@code Map<String, R>} 形式的結果，
     * 保持查詢鍵與對應值的映射關係。</p>
     *
     * <p><strong>回傳值特性：</strong></p>
     * <ul>
     *   <li>鍵：原始查詢的緩存鍵</li>
     *   <li>值：對應的緩存資料</li>
     *   <li>不存在的鍵不會出現在結果 Map 中</li>
     * </ul>
     *
     * <p><strong>適用場景：</strong></p>
     * <ul>
     *   <li>需要知道哪些鍵有對應緩存資料</li>
     *   <li>後續需要基於鍵進行不同處理</li>
     *   <li>緩存命中率統計</li>
     * </ul>
     *
     * @param <R>               緩存值的類型
     * @param keys              緩存鍵的集合，不能為 {@code null}
     * @param clazz             緩存值的類型，用於反序列化
     * @param cacheProviderEnum 緩存提供者類型
     *
     * @return Mono<Map < String, R>> 包含鍵值對應關係的反應式物件
     *
     * @see CacheProvider#getAllAsMap(Collection, Class)
     */
    public <R> Mono<Map<String, R>> getCaches(Collection<String> keys, Class<R> clazz, CacheProviderEnum cacheProviderEnum) {
        return Optional.ofNullable(cacheProviderMap.get(cacheProviderEnum)).map(provider -> provider.getAllAsMap(keys, clazz)).orElseGet(Mono::empty);
    }


    /**
     * 設定單一緩存值，使用提供者預設的過期時間。
     *
     * <p>此方法是最簡單的緩存設定操作，
     * 適用於不需要特別指定過期時間的場景。</p>
     *
     * <p><strong>過期時間策略：</strong></p>
     * <p>使用緩存提供者內建的預設過期時間，
     * 不同提供者的預設策略可能不同：</p>
     * <ul>
     *   <li>Redis：通常為永不過期或配置的預設 TTL</li>
     *   <li>本地緩存：根據 LRU 或時間策略自動清理</li>
     * </ul>
     *
     * @param key               緩存鍵，不能為 {@code null}
     * @param value             要緩存的值，可以為 {@code null}（根據提供者實現決定）
     * @param cacheProviderEnum 緩存提供者類型
     *
     * @return {@link Mono<Void>} 表示設定操作完成的反應式物件
     *
     * @see #setCache(String, Object, CacheProviderEnum, Duration)
     */
    public Mono<Void> setCache(String key, Object value, CacheProviderEnum cacheProviderEnum) {
        return setCache(key, value, cacheProviderEnum, null);
    }


    /**
     * 設定單一緩存值並指定過期時間。
     *
     * <p>此方法在分散式鎖保護下執行緩存設定操作，
     * 確保在併發環境中的資料一致性。</p>
     *
     * <p><strong>分散式鎖機制：</strong></p>
     * <ul>
     *   <li>自動獲取基於鍵的分散式鎖</li>
     *   <li>確保同一時刻只有一個實例能夠修改特定緩存</li>
     *   <li>操作完成後自動釋放鎖</li>
     *   <li>獲取鎖失敗時靜默跳過操作</li>
     * </ul>
     *
     * <p><strong>過期時間處理：</strong></p>
     * <ul>
     *   <li>{@code null}：使用提供者預設過期策略</li>
     *   <li>正值：設定明確的過期時間</li>
     *   <li>零或負值：根據提供者實現決定（通常為立即過期或永不過期）</li>
     * </ul>
     *
     * @param key               緩存鍵，不能為 {@code null}
     * @param value             要緩存的值
     * @param cacheProviderEnum 緩存提供者類型
     * @param expire            過期時間，{@code null} 表示使用預設策略
     *
     * @return {@link Mono<Void>} 表示設定操作完成的反應式物件
     *
     * @see #executeWithLock(Collection, CacheProviderEnum, Function)
     * @see CacheProvider#set(String, Object, Duration)
     */
    public Mono<Void> setCache(String key, Object value, CacheProviderEnum cacheProviderEnum, Duration expire) {
        return Optional
                .ofNullable(cacheProviderMap.get(cacheProviderEnum))
                .map(provider -> executeWithLock(Collections.singletonList(key), cacheProviderEnum, acquiredKeys -> provider.set(key, value, expire)))
                .orElseGet(Mono::empty);
    }


    /**
     * 在分散式鎖保護下執行緩存操作的通用方法。
     *
     * <p>此方法是緩存管理器的核心基礎設施，
     * 為所有寫操作提供分散式鎖保護，確保資料一致性。</p>
     *
     * <p><strong>鎖獲取策略：</strong></p>
     * <ul>
     *   <li><strong>全有或全無</strong>：必須獲取所有相關鍵的鎖才能執行操作</li>
     *   <li><strong>排序獲取</strong>：對鍵進行排序後依序獲取鎖，避免死鎖</li>
     *   <li><strong>去重處理</strong>：自動去除重複的鍵，提高效率</li>
     *   <li><strong>自動釋放</strong>：使用 {@code Mono.usingWhen} 確保鎖的可靠釋放</li>
     * </ul>
     *
     * <p><strong>錯誤處理機制：</strong></p>
     * <ul>
     *   <li>獲取鎖失敗：記錄日誌並跳過操作，不拋出異常</li>
     *   <li>操作執行失敗：確保鎖仍能正確釋放</li>
     *   <li>操作取消：清理已獲取的資源</li>
     * </ul>
     *
     * <p><strong>鎖鍵格式：</strong></p>
     * <p>{@code cache_lock:提供者類型:業務鍵}</p>
     *
     * @param keys              需要鎖定的業務鍵集合
     * @param cacheProviderEnum 緩存提供者類型，用於鎖鍵的構建
     * @param operation         在鎖保護下執行的操作，接收成功獲取鎖的鍵集合
     *
     * @return {@link Mono<Void>} 表示操作完成的反應式物件
     *
     * @see #tryAcquireAllLocks(List)
     * @see #releaseRedisLocksInternal(Collection)
     */
    private Mono<Void> executeWithLock(Collection<String> keys, CacheProviderEnum cacheProviderEnum, Function<Collection<String>, Mono<Void>> operation) {
        if (keys == null || keys.isEmpty()) {
            return Mono.empty();
        }

        List<String> lockKeys = keys
                .stream()
                .map(key -> LOCK_PREFIX + cacheProviderEnum.name() + ":" + key)
                .sorted()
                .distinct()
                .collect(Collectors.toList());

        return Mono.usingWhen(tryAcquireAllLocks(lockKeys), acquiredLocks -> {
                                  if (acquiredLocks.isEmpty()) {
                                      LogUnity.debug("無法獲取鎖，跳過操作。緩存鍵: %s", keys);
                                      return Mono.empty();
                                  }
                                  return operation.apply(keys);
                              }, acquiredLocks -> releaseRedisLocksInternal(acquiredLocks).doOnSuccess(v -> {
                                  LogUnity.debug("成功釋放鎖: %s", acquiredLocks);
                              }), (acquiredLocks, throwable) -> releaseRedisLocksInternal(acquiredLocks).doOnSuccess(v -> {
                                  LogUnity.debug("錯誤情況下成功釋放鎖: %s", acquiredLocks);
                              }), acquiredLocks -> releaseRedisLocksInternal(acquiredLocks).doOnSuccess(v -> {
                                  LogUnity.debug("取消情況下成功釋放鎖: %s", acquiredLocks);
                              })
        );
    }


    /**
     * 嘗試獲取所有指定的 Redis 分散式鎖。
     *
     * <p>此方法實現「全有或全無」的鎖獲取策略，
     * 確保要麼獲取所有需要的鎖，要麼一個都不獲取，
     * 有效避免部分獲取導致的死鎖問題。</p>
     *
     * <p><strong>獲取流程：</strong></p>
     * <ol>
     *   <li><strong>序列化獲取</strong>：使用 {@code concatMap} 確保按順序獲取鎖</li>
     *   <li><strong>原子性檢查</strong>：使用 Redis 的 {@code SET NX EX} 指令</li>
     *   <li><strong>失敗回滾</strong>：任一鎖獲取失敗時釋放所有已獲取的鎖</li>
     *   <li><strong>結果回傳</strong>：成功時回傳所有鎖鍵，失敗時回傳空列表</li>
     * </ol>
     *
     * <p><strong>錯誤容忍：</strong></p>
     * <p>單個鎖獲取時的網路錯誤或 Redis 異常不會中斷整個流程，
     * 而是被視為獲取失敗，觸發回滾邏輯。</p>
     *
     * <p><strong>日誌記錄：</strong></p>
     * <ul>
     *   <li>TRACE 級別：記錄每個鎖的獲取狀態</li>
     *   <li>DEBUG 級別：記錄整體獲取結果</li>
     *   <li>WARN 級別：記錄獲取過程中的異常</li>
     * </ul>
     *
     * @param lockKeys 需要獲取的完整鎖鍵列表（包含前綴）
     *
     * @return Mono<List<String>> 成功獲取的鎖鍵列表，失敗時為空列表
     *
     * @see RedisProvider#setValueIfAbsent(String, Object, Duration)
     */
    private Mono<List<String>> tryAcquireAllLocks(List<String> lockKeys) {
        List<String> acquiredLocks = new ArrayList<>();

        return Flux
                .fromIterable(lockKeys)
                .concatMap(lockKey -> redisProvider.setValueIfAbsent(lockKey, LOCK_VALUE, DEFAULT_EXPIRE_TIME).doOnSuccess(acquired -> {
                    if (Boolean.TRUE.equals(acquired)) {
                        acquiredLocks.add(lockKey);
                        LogUnity.trace("成功獲取鎖: %s", lockKey);
                    } else {
                        LogUnity.trace("鎖已被佔用: %s", lockKey);
                    }
                }).onErrorResume(e -> {
                    LogUnity.warn("獲取鎖時發生錯誤: %s", e, lockKey);
                    return Mono.just(false);
                }))
                .all(Boolean.TRUE::equals)
                .flatMap(allAcquired -> {
                    if (Boolean.TRUE.equals(allAcquired)) {
                        LogUnity.debug("成功獲取所有鎖: %s", acquiredLocks);
                        return Mono.just(acquiredLocks);
                    } else {
                        LogUnity.debug("無法獲取全部鎖，釋放已獲取的鎖: %s", acquiredLocks);
                        return releaseRedisLocksInternal(acquiredLocks).thenReturn(Collections.emptyList());
                    }
                });
    }


    /**
     * 釋放多個 Redis 分散式鎖的內部實現方法。
     *
     * <p>此方法負責安全、可靠地釋放已獲取的 Redis 鎖，
     * 確保即使在異常情況下也不會導致死鎖。</p>
     *
     * <p><strong>釋放特性：</strong></p>
     * <ul>
     *   <li><strong>批量操作</strong>：一次性釋放多個鎖，提高效率</li>
     *   <li><strong>異常容忍</strong>：釋放失敗不會影響業務邏輯執行</li>
     *   <li><strong>日誌追蹤</strong>：詳細記錄釋放過程和結果</li>
     *   <li><strong>空值安全</strong>：正確處理空集合或 null 輸入</li>
     * </ul>
     *
     * <p><strong>錯誤處理策略：</strong></p>
     * <p>釋放鎖時發生的任何錯誤都會被記錄但不會傳播，
     * 確保：</p>
     * <ul>
     *   <li>業務邏輯不受鎖釋放失敗的影響</li>
     *   <li>其他成功釋放的鎖不受影響</li>
     *   <li>系統整體穩定性得到保障</li>
     * </ul>
     *
     * <p><strong>日誌級別：</strong></p>
     * <ul>
     *   <li>TRACE：記錄釋放的鎖數量和詳細資訊</li>
     *   <li>WARN：記錄釋放過程中的異常</li>
     * </ul>
     *
     * @param lockKeys 需要釋放的完整鎖鍵集合（包含前綴）
     *
     * @return {@link Mono<Void>} 表示釋放操作完成的反應式物件
     *
     * @see RedisProvider#deleteValue(Collection)
     */
    private Mono<Void> releaseRedisLocksInternal(Collection<String> lockKeys) {
        if (lockKeys == null || lockKeys.isEmpty()) {
            return Mono.empty();
        }

        return redisProvider.deleteValue(lockKeys).doOnSuccess(deletedCount -> {
            if (deletedCount != null && deletedCount > 0) {
                LogUnity.trace("成功釋放 %d 個鎖: %s", deletedCount, lockKeys);
            }
        }).onErrorResume(e -> {
            LogUnity.warn("釋放鎖: %s 時發生錯誤: ", e, lockKeys);
            return Mono.empty();
        }).then();
    }


    /**
     * 批量設定緩存值，使用提供者預設的過期時間。
     *
     * <p>此方法提供高效的批量緩存設定能力，
     * 相比多次單獨設定具有更好的效能表現。</p>
     *
     * <p><strong>效能優勢：</strong></p>
     * <ul>
     *   <li>減少網路往返次數</li>
     *   <li>批量獲取分散式鎖，降低鎖競爭</li>
     *   <li>提供者層面的批量最佳化</li>
     * </ul>
     *
     * @param keyValues         鍵值對映射，鍵為緩存鍵，值為要緩存的資料
     * @param cacheProviderEnum 緩存提供者類型
     *
     * @return {@link Mono<Void>} 表示批量設定操作完成的反應式物件
     *
     * @see #setCaches(Map, CacheProviderEnum, Duration)
     */
    public Mono<Void> setCaches(Map<String, Object> keyValues, CacheProviderEnum cacheProviderEnum) {
        return setCaches(keyValues, cacheProviderEnum, null);
    }


    /**
     * 批量設定緩存值並指定統一的過期時間。
     *
     * <p>此方法在分散式鎖保護下執行批量緩存設定，
     * 確保所有相關鍵的操作都是原子性的。</p>
     *
     * <p><strong>原子性保證：</strong></p>
     * <ul>
     *   <li>獲取所有相關鍵的分散式鎖</li>
     *   <li>批量執行設定操作</li>
     *   <li>統一釋放所有鎖</li>
     * </ul>
     *
     * <p><strong>適用場景：</strong></p>
     * <ul>
     *   <li>用戶資料的批量初始化</li>
     *   <li>配置項目的批量更新</li>
     *   <li>定時任務的批量緩存重新整理</li>
     * </ul>
     *
     * @param keyValues         鍵值對映射，所有項目使用相同的過期時間
     * @param cacheProviderEnum 緩存提供者類型
     * @param expire            統一的過期時間，{@code null} 表示使用預設策略
     *
     * @return {@link Mono<Void>} 表示批量設定操作完成的反應式物件
     *
     * @see CacheProvider#setAll(Map, Duration)
     * @see #executeWithLock(Collection, CacheProviderEnum, Function)
     */
    public Mono<Void> setCaches(Map<String, Object> keyValues, CacheProviderEnum cacheProviderEnum, Duration expire) {
        return Optional
                .ofNullable(cacheProviderMap.get(cacheProviderEnum))
                .map(provider -> executeWithLock(keyValues.keySet(), cacheProviderEnum, acquiredKeys -> provider.setAll(keyValues, expire)))
                .orElseGet(Mono::empty);
    }


    /**
     * 同步刪除單一緩存值。
     *
     * <p>此方法提供最簡單的緩存刪除操作，
     * 採用同步模式確保刪除操作立即完成。</p>
     *
     * <p><strong>同步 vs 異步：</strong></p>
     * <ul>
     *   <li><strong>同步刪除</strong>：等待刪除操作完成才回傳結果</li>
     *   <li><strong>異步刪除</strong>：立即回傳，刪除操作在背景執行</li>
     * </ul>
     *
     * @param key               要刪除的緩存鍵
     * @param cacheProviderEnum 緩存提供者類型
     *
     * @return {@link Mono<Void>} 表示刪除操作完成的反應式物件
     *
     * @see #deleteCache(String, CacheProviderEnum, boolean)
     */
    public Mono<Void> deleteCache(String key, CacheProviderEnum cacheProviderEnum) {
        return deleteCache(key, cacheProviderEnum, false);
    }


    /**
     * 刪除單一緩存值，可選擇同步或異步模式。
     *
     * <p>此方法在分散式鎖保護下執行緩存刪除操作，
     * 支援同步和異步兩種執行模式。</p>
     *
     * <p><strong>執行模式選擇：</strong></p>
     * <ul>
     *   <li><strong>同步模式（{@code isAsync = false}）</strong>：
     *       <ul>
     *         <li>刪除操作在當前線程執行</li>
     *         <li>呼叫方等待刪除完成</li>
     *         <li>適用於需要立即確認刪除結果的場景</li>
     *       </ul>
     *   </li>
     *   <li><strong>異步模式（{@code isAsync = true}）</strong>：
     *       <ul>
     *         <li>刪除操作在彈性線程池執行</li>
     *         <li>呼叫方立即獲得回應</li>
     *         <li>適用於對刪除延遲不敏感的場景</li>
     *       </ul>
     *   </li>
     * </ul>
     *
     * <p><strong>分散式鎖保護：</strong></p>
     * <p>無論選擇哪種模式，刪除操作都會在分散式鎖保護下執行，
     * 確保併發環境中的操作安全性。</p>
     *
     * @param key               要刪除的緩存鍵
     * @param cacheProviderEnum 緩存提供者類型
     * @param isAsync           是否使用異步模式執行刪除操作
     *
     * @return {@link Mono<Void>} 表示刪除操作完成（或開始）的反應式物件
     *
     * @see Schedulers#boundedElastic()
     * @see CacheProvider#delete(String)
     */
    public Mono<Void> deleteCache(String key, CacheProviderEnum cacheProviderEnum, boolean isAsync) {
        return Optional.ofNullable(cacheProviderMap.get(cacheProviderEnum)).map(provider -> {
            return executeWithLock(Collections.singletonList(key), cacheProviderEnum, acquiredKeys -> {
                                       Mono<Void> action = provider.delete(key);
                                       return isAsync ? action.subscribeOn(Schedulers.boundedElastic()) : action;
                                   }
            );
        }).orElseGet(Mono::empty);
    }


    /**
     * 同步批量刪除緩存值。
     *
     * <p>此方法提供高效的批量緩存刪除能力，
     * 相比多次單獨刪除具有更好的效能和一致性保證。</p>
     *
     * <p><strong>批量操作優勢：</strong></p>
     * <ul>
     *   <li>減少鎖競爭次數</li>
     *   <li>降低網路往返延遲</li>
     *   <li>保證批量操作的原子性</li>
     * </ul>
     *
     * @param keys              要刪除的緩存鍵集合
     * @param cacheProviderEnum 緩存提供者類型
     *
     * @return {@link Mono<Void>} 表示批量刪除操作完成的反應式物件
     *
     * @see #deleteCaches(Collection, CacheProviderEnum, boolean)
     */
    public Mono<Void> deleteCaches(Collection<String> keys, CacheProviderEnum cacheProviderEnum) {
        return deleteCaches(keys, cacheProviderEnum, false);
    }


    /**
     * 批量刪除緩存值，可選擇同步或異步模式。
     *
     * <p>此方法在分散式鎖保護下執行批量緩存刪除，
     * 確保所有相關鍵的刪除操作都是原子性的。</p>
     *
     * <p><strong>原子性保證：</strong></p>
     * <ul>
     *   <li>一次性獲取所有相關鍵的分散式鎖</li>
     *   <li>批量執行刪除操作</li>
     *   <li>統一釋放所有鎖</li>
     * </ul>
     *
     * <p><strong>效能最佳化：</strong></p>
     * <p>批量刪除在以下方面優於多次單獨刪除：</p>
     * <ul>
     *   <li>鎖獲取次數從 O(n) 降至 O(1)</li>
     *   <li>網路往返次數顯著減少</li>
     *   <li>緩存提供者層面的批量最佳化</li>
     * </ul>
     *
     * @param keys              要刪除的緩存鍵集合
     * @param cacheProviderEnum 緩存提供者類型
     * @param isAsync           是否使用異步模式執行刪除操作
     *
     * @return {@link Mono<Void>} 表示批量刪除操作完成（或開始）的反應式物件
     *
     * @see CacheProvider#deleteAll(Collection)
     */
    public Mono<Void> deleteCaches(Collection<String> keys, CacheProviderEnum cacheProviderEnum, boolean isAsync) {
        return Optional.ofNullable(cacheProviderMap.get(cacheProviderEnum)).map(provider -> {
            return executeWithLock(keys, cacheProviderEnum, acquiredKeys -> {
                                       Mono<Void> action = provider.deleteAll(keys);
                                       return isAsync ? action.subscribeOn(Schedulers.boundedElastic()) : action;
                                   }
            );
        }).orElseGet(Mono::empty);
    }


    /**
     * 使用預設延遲時間的延遲雙刪緩存操作。
     *
     * <p>延遲雙刪是一種高級緩存一致性策略，
     * 專門用於解決緩存與資料庫之間的資料一致性問題。</p>
     *
     * <p><strong>延遲雙刪的工作原理：</strong></p>
     * <ol>
     *   <li><strong>立即刪除</strong>：首先立即刪除緩存</li>
     *   <li><strong>資料庫操作</strong>：執行資料庫更新操作（由呼叫方負責）</li>
     *   <li><strong>延遲刪除</strong>：等待一段時間後再次刪除緩存</li>
     * </ol>
     *
     * <p><strong>解決的問題：</strong></p>
     * <p>在資料庫主從同步存在延遲的環境中，
     * 防止以下情況導致的緩存不一致：</p>
     * <ul>
     *   <li>請求 A 刪除緩存後更新主庫</li>
     *   <li>請求 B 查詢緩存未命中，從從庫讀取舊資料並寫入緩存</li>
     *   <li>主從同步完成，但緩存中仍是舊資料</li>
     * </ul>
     *
     * <p><strong>預設延遲時間：</strong></p>
     * <p>使用 {@link #DEFAULT_DELAYED_DELETE_TIME}（3秒）作為延遲時間。</p>
     *
     * @param key               要刪除的緩存鍵
     * @param cacheProviderEnum 緩存提供者類型
     *
     * @return {@link Mono<Void>} 表示第一次刪除操作完成的反應式物件
     * （第二次刪除在背景異步執行）
     *
     * @see #deleteCacheWithDelayedDoubleDelete(String, CacheProviderEnum, Duration)
     */
    public Mono<Void> deleteCacheWithDelayedDoubleDelete(String key, CacheProviderEnum cacheProviderEnum) {
        return deleteCacheWithDelayedDoubleDelete(key, cacheProviderEnum, DEFAULT_DELAYED_DELETE_TIME);
    }


    /**
     * 使用自訂延遲時間的延遲雙刪緩存操作。
     *
     * <p>此方法允許根據實際的資料庫同步延遲情況，
     * 自訂延遲雙刪的時間間隔。</p>
     *
     * <p><strong>延遲時間選擇建議：</strong></p>
     * <ul>
     *   <li><strong>MySQL 主從同步</strong>：通常 1-5 秒</li>
     *   <li><strong>Redis 叢集同步</strong>：通常 100-500 毫秒</li>
     *   <li><strong>分散式資料庫</strong>：可能需要 10-30 秒</li>
     * </ul>
     *
     * <p><strong>注意事項：</strong></p>
     * <ul>
     *   <li>延遲時間過短：可能無法解決一致性問題</li>
     *   <li>延遲時間過長：增加緩存不一致的時間窗口</li>
     *   <li>建議根據實際監控資料進行調整</li>
     * </ul>
     *
     * @param key               要刪除的緩存鍵
     * @param cacheProviderEnum 緩存提供者類型
     * @param delayTime         第一次和第二次刪除之間的延遲時間
     *
     * @return {@link Mono<Void>} 表示第一次刪除操作完成的反應式物件
     *
     * @see #deleteCacheWithDelayedDoubleDelete(Collection, CacheProviderEnum, Duration)
     */
    public Mono<Void> deleteCacheWithDelayedDoubleDelete(String key, CacheProviderEnum cacheProviderEnum, Duration delayTime) {
        return deleteCacheWithDelayedDoubleDelete(Collections.singletonList(key), cacheProviderEnum, delayTime);
    }


    /**
     * 批量執行延遲雙刪緩存操作，使用自訂延遲時間。
     *
     * <p>此方法是延遲雙刪功能的核心實現，
     * 支援批量操作並提供完整的錯誤處理和日誌記錄。</p>
     *
     * <p><strong>執行流程：</strong></p>
     * <ol>
     *   <li><strong>第一次刪除</strong>：
     *       <ul>
     *         <li>獲取分散式鎖</li>
     *         <li>執行批量刪除操作</li>
     *         <li>釋放鎖</li>
     *         <li>記錄操作結果</li>
     *       </ul>
     *   </li>
     *   <li><strong>延遲等待</strong>：
     *       <ul>
     *         <li>在彈性線程池中等待指定時間</li>
     *         <li>不阻塞主執行流程</li>
     *       </ul>
     *   </li>
     *   <li><strong>第二次刪除</strong>：
     *       <ul>
     *         <li>重新獲取分散式鎖</li>
     *         <li>再次執行批量刪除操作</li>
     *         <li>釋放鎖並記錄結果</li>
     *       </ul>
     *   </li>
     * </ol>
     *
     * <p><strong>錯誤處理策略：</strong></p>
     * <ul>
     *   <li><strong>鎖獲取失敗</strong>：記錄日誌但不中斷流程</li>
     *   <li><strong>刪除操作失敗</strong>：記錄日誌但不影響其他操作</li>
     *   <li><strong>網路異常</strong>：自動重試或優雅降級</li>
     * </ul>
     *
     * <p><strong>日誌追蹤：</strong></p>
     * <ul>
     *   <li>TRACE 級別：詳細的執行步驟和時間資訊</li>
     *   <li>INFO 級別：操作失敗的警告資訊</li>
     * </ul>
     *
     * <p><strong>併發安全：</strong></p>
     * <p>兩次刪除操作都在分散式鎖保護下執行，
     * 確保與其他緩存操作不會發生競爭條件。</p>
     *
     * @param keys              要刪除的緩存鍵集合
     * @param cacheProviderEnum 緩存提供者類型
     * @param delayTime         第一次和第二次刪除之間的延遲時間
     *
     * @return {@link Mono<Void>} 表示第一次刪除操作完成的反應式物件
     * （第二次刪除會在背景異步執行）
     *
     * @see #executeWithLock(Collection, CacheProviderEnum, Function)
     * @see Mono#delay(Duration, reactor.core.scheduler.Scheduler)
     */
    public Mono<Void> deleteCacheWithDelayedDoubleDelete(Collection<String> keys, CacheProviderEnum cacheProviderEnum, Duration delayTime) {
        if (keys == null || keys.isEmpty()) {
            return Mono.empty();
        }

        CacheProvider provider = cacheProviderMap.get(cacheProviderEnum);
        if (provider == null) {
            return Mono.empty();
        }

        Mono<Void> firstDelete = executeWithLock(keys, cacheProviderEnum, acquiredLockKeys -> {
                                                     LogUnity.trace("執行延遲雙刪第一次刪除，緩存鍵: %s，獲取到的鎖: %s", keys, acquiredLockKeys);
                                                     return provider.deleteAll(keys).onErrorResume(e -> {
                                                         LogUnity.info("延遲雙刪第一次刪除失敗，緩存鍵: %s", e, keys);
                                                         return Mono.empty();
                                                     });
                                                 }
        ).doOnError(e -> LogUnity.info("延遲雙刪第一次刪除無法獲取鎖，緩存鍵: %s", keys)).onErrorResume(e -> Mono.empty());

        Mono<Void> delayedDelete = Mono
                .delay(delayTime, Schedulers.boundedElastic())
                .then(executeWithLock(keys, cacheProviderEnum, acquiredLockKeys -> {
                                          LogUnity.trace("執行延遲雙刪第二次刪除（延遲 %s 毫秒），緩存鍵: %s，獲取到的鎖: %s", delayTime.toMillis(), keys, acquiredLockKeys);
                                          return provider.deleteAll(keys).onErrorResume(e -> {
                                              LogUnity.info("延遲雙刪第二次刪除失敗，緩存鍵: %s", e, keys);
                                              return Mono.empty();
                                          });
                                      }
                ))
                .doOnError(e -> LogUnity.info("延遲雙刪第二次刪除無法獲取鎖，緩存鍵: %s", keys))
                .onErrorResume(e -> Mono.empty())
                .subscribeOn(Schedulers.boundedElastic());

        return firstDelete.doOnTerminate(delayedDelete::subscribe);
    }


    /**
     * 使用預設延遲時間的批量延遲雙刪緩存操作。
     *
     * <p>此方法為 {@link #deleteCacheWithDelayedDoubleDelete(Collection, CacheProviderEnum, Duration)}
     * 的便利方法，使用系統預設的延遲時間。</p>
     *
     * <p><strong>適用場景：</strong></p>
     * <ul>
     *   <li>大部分標準的緩存一致性需求</li>
     *   <li>資料庫同步延遲在合理範圍內的環境</li>
     *   <li>不需要特別調整延遲時間的批量操作</li>
     * </ul>
     *
     * @param keys              要刪除的緩存鍵集合
     * @param cacheProviderEnum 緩存提供者類型
     *
     * @return {@link Mono<Void>} 表示第一次刪除操作完成的反應式物件
     *
     * @see #DEFAULT_DELAYED_DELETE_TIME
     * @see #deleteCacheWithDelayedDoubleDelete(Collection, CacheProviderEnum, Duration)
     */
    public Mono<Void> deleteCachesWithDelayedDoubleDelete(Collection<String> keys, CacheProviderEnum cacheProviderEnum) {
        return deleteCacheWithDelayedDoubleDelete(keys, cacheProviderEnum, DEFAULT_DELAYED_DELETE_TIME);
    }


    /**
     * 嘗試獲取單個 Redis 鎖的已棄用方法。
     *
     * <p><strong>棄用原因：</strong></p>
     * <ul>
     *   <li>缺乏統一的鎖管理機制</li>
     *   <li>無法處理多鍵操作的原子性需求</li>
     *   <li>錯誤處理不夠完善</li>
     * </ul>
     *
     * <p><strong>替代方案：</strong></p>
     * <p>使用 {@link #executeWithLock(Collection, CacheProviderEnum, Function)}
     * 方法，它提供更完善的鎖管理和錯誤處理機制。</p>
     *
     * @param key               業務鍵
     * @param cacheProviderEnum 緩存提供者類型
     *
     * @return {@link Mono<Boolean>} 鎖獲取結果
     *
     * @see #executeWithLock(Collection, CacheProviderEnum, Function)
     * @deprecated 使用 {@link #executeWithLock(Collection, CacheProviderEnum, Function)} 替代
     */
    @Deprecated
    private Mono<Boolean> tryAcquireRedisLock(String key, CacheProviderEnum cacheProviderEnum) {
        String lockKey = LOCK_PREFIX + cacheProviderEnum.name() + ":" + key;
        return redisProvider.setValueIfAbsent(lockKey, LOCK_VALUE, DEFAULT_EXPIRE_TIME).onErrorResume(e -> {
            LogUnity.warn("無法獲取 Redis 鎖: %s", e, lockKey);
            return Mono.just(false);
        });
    }


    /**
     * 釋放單個 Redis 鎖的已棄用方法。
     *
     * @param key               業務鍵
     * @param cacheProviderEnum 緩存提供者類型
     *
     * @return {@link Mono<Void>} 釋放操作結果
     *
     * @see #executeWithLock(Collection, CacheProviderEnum, Function)
     * @deprecated 使用 {@link #executeWithLock(Collection, CacheProviderEnum, Function)} 替代
     */
    @Deprecated
    private Mono<Void> releaseRedisLock(String key, CacheProviderEnum cacheProviderEnum) {
        return releaseRedisLock(Collections.singletonList(key), cacheProviderEnum);
    }


    /**
     * 釋放多個 Redis 鎖的已棄用方法。
     *
     * @param keys              業務鍵集合
     * @param cacheProviderEnum 緩存提供者類型
     *
     * @return {@link Mono<Void>} 釋放操作結果
     *
     * @see #executeWithLock(Collection, CacheProviderEnum, Function)
     * @deprecated 使用 {@link #executeWithLock(Collection, CacheProviderEnum, Function)} 替代
     */
    @Deprecated
    private Mono<Void> releaseRedisLock(Collection<String> keys, CacheProviderEnum cacheProviderEnum) {
        if (keys == null || keys.isEmpty()) {
            return Mono.empty();
        }
        List<String> lockKeyList = keys
                .stream()
                .map(key -> LOCK_PREFIX + cacheProviderEnum.name() + ":" + key)
                .distinct()
                .collect(Collectors.toList());
        return releaseRedisLocksInternal(lockKeyList);
    }


    /**
     * 嘗試獲取多個 Redis 鎖的已棄用方法。
     *
     * @param keys              業務鍵集合
     * @param cacheProviderEnum 緩存提供者類型
     *
     * @return {@link Mono<Boolean>} 鎖獲取結果
     *
     * @see #executeWithLock(Collection, CacheProviderEnum, Function)
     * @deprecated 使用 {@link #executeWithLock(Collection, CacheProviderEnum, Function)} 替代
     */
    @Deprecated
    private Mono<Boolean> tryAcquireRedisLocks(Collection<String> keys, CacheProviderEnum cacheProviderEnum) {
        if (keys == null || keys.isEmpty()) {
            return Mono.just(true);
        }
        List<String> lockKeyList = keys
                .stream()
                .map(key -> LOCK_PREFIX + cacheProviderEnum.name() + ":" + key)
                .sorted()
                .distinct()
                .collect(Collectors.toList());

        return tryAcquireAllLocks(lockKeyList).map(acquiredLocks -> !acquiredLocks.isEmpty());
    }


    /**
     * 執行 Cache-Aside 模式的單值緩存操作，使用預設過期時間。
     *
     * <p>此方法實現了經典的 Cache-Aside（旁路緩存）模式：</p>
     * <ol>
     *   <li><strong>查詢緩存</strong>：首先嘗試從緩存獲取資料</li>
     *   <li><strong>緩存命中</strong>：若命中則直接回傳緩存資料</li>
     *   <li><strong>緩存未命中</strong>：若未命中則執行源查詢邏輯</li>
     *   <li><strong>寫入緩存</strong>：將查詢結果根據緩存規則寫入緩存</li>
     *   <li><strong>回傳結果</strong>：回傳查詢到的資料</li>
     * </ol>
     *
     * <p><strong>緩存規則應用：</strong></p>
     * <p>此方法支援複雜的緩存規則邏輯，允許：</p>
     * <ul>
     *   <li>根據資料內容決定緩存策略</li>
     *   <li>設定不同的緩存鍵和過期時間</li>
     *   <li>實現條件性緩存（如只緩存特定條件的資料）</li>
     * </ul>
     *
     * <p><strong>適用場景：</strong></p>
     * <ul>
     *   <li>用戶資訊查詢</li>
     *   <li>商品詳情獲取</li>
     *   <li>配置項目讀取</li>
     *   <li>任何需要緩存的單值查詢</li>
     * </ul>
     *
     * @param <R>               回傳資料的類型
     * @param key               緩存鍵，同時用於查詢和儲存
     * @param clazz             預期的回傳類型，用於緩存反序列化
     * @param cacheProviderEnum 緩存提供者類型
     * @param source            當緩存未命中時執行的資料源查詢邏輯
     * @param cacheRules        緩存規則列表，定義如何儲存查詢結果
     *
     * @return {@link Mono<R>} 包含查詢結果的反應式物件
     *
     * @see #runAndSetCache(String, Class, CacheProviderEnum, Mono, List, Duration)
     * @see CacheRule
     */
    public <R> Mono<R> runAndSetCache(String key, Class<R> clazz, CacheProviderEnum cacheProviderEnum, Mono<? extends R> source, List<CacheRule<R>> cacheRules) {
        return runAndSetCache(key, clazz, cacheProviderEnum, source, cacheRules, null);
    }


    /**
     * 執行 Cache-Aside 模式的單值緩存操作，使用指定的過期時間。
     *
     * <p>此方法是 Cache-Aside 模式的完整實現，
     * 提供了靈活的緩存規則應用和過期時間控制。</p>
     *
     * <p><strong>緩存寫入機制：</strong></p>
     * <ul>
     *   <li><strong>異步寫入</strong>：緩存寫入在彈性線程池中異步執行</li>
     *   <li><strong>非阻塞</strong>：不會阻塞主要的資料回傳流程</li>
     *   <li><strong>錯誤隔離</strong>：緩存寫入失敗不影響業務結果</li>
     * </ul>
     *
     * <p><strong>鎖保護機制：</strong></p>
     * <p>緩存寫入操作會在分散式鎖保護下執行，
     * 防止併發環境中的資料競爭。</p>
     *
     * <p><strong>過期時間策略：</strong></p>
     * <ul>
     *   <li>{@code null}：使用緩存提供者的預設過期策略</li>
     *   <li>明確值：使用指定的過期時間</li>
     *   <li>零值：根據提供者實現決定（通常為立即過期）</li>
     * </ul>
     *
     * @param <R>               回傳資料的類型
     * @param key               緩存鍵，同時用於查詢和儲存
     * @param clazz             預期的回傳類型，用於緩存反序列化
     * @param cacheProviderEnum 緩存提供者類型
     * @param source            當緩存未命中時執行的資料源查詢邏輯
     * @param cacheRules        緩存規則列表，定義如何儲存查詢結果
     * @param expire            緩存過期時間，{@code null} 表示使用預設策略
     *
     * @return {@link Mono<R>} 包含查詢結果的反應式物件
     *
     * @see #applyCacheRule(List, Object, Duration)
     * @see CacheProvider#get(String, Class)
     */
    public <R> Mono<R> runAndSetCache(String key, Class<R> clazz, CacheProviderEnum cacheProviderEnum, Mono<? extends R> source, List<CacheRule<R>> cacheRules, Duration expire) {
        return Optional
                .ofNullable(cacheProviderMap.get(cacheProviderEnum))
                .map(provider -> provider.get(key, clazz).switchIfEmpty(Mono.defer(() -> source.doOnNext(value -> {
                    executeWithLock(Collections.singletonList(key), cacheProviderEnum, acquiredKeys -> applyCacheRule(cacheRules, value, expire))
                            .subscribeOn(Schedulers.boundedElastic())
                            .subscribe();
                }))))
                .orElseGet(() -> source.cast(clazz));
    }


    /**
     * 應用緩存規則到特定資料值的核心方法。
     *
     * <p>此方法負責將自訂的緩存規則應用到查詢結果上，
     * 支援複雜的緩存策略和條件邏輯。</p>
     *
     * <p><strong>特殊類型處理：</strong></p>
     * <ul>
     *   <li><strong>{@link FluxContainer}</strong>：
     *       <ul>
     *         <li>等待內部 Flux 完成</li>
     *         <li>在彈性線程池中應用緩存規則</li>
     *         <li>適用於包裝 Flux 的容器類型</li>
     *       </ul>
     *   </li>
     *   <li><strong>{@link Flux}</strong>：
     *       <ul>
     *         <li>等待流完成</li>
     *         <li>異步應用緩存規則</li>
     *         <li>適用於直接的 Flux 類型</li>
     *       </ul>
     *   </li>
     *   <li><strong>一般物件</strong>：
     *       <ul>
     *         <li>直接應用所有緩存規則</li>
     *         <li>使用 {@code Mono.when} 等待所有規則完成</li>
     *       </ul>
     *   </li>
     * </ul>
     *
     * <p><strong>執行特性：</strong></p>
     * <ul>
     *   <li><strong>並行執行</strong>：多個緩存規則並行應用</li>
     *   <li><strong>異步處理</strong>：所有操作在彈性線程池執行</li>
     *   <li><strong>錯誤隔離</strong>：單個規則失敗不影響其他規則</li>
     * </ul>
     *
     * @param <R>        資料值的類型
     * @param cacheRules 要應用的緩存規則列表
     * @param value      需要應用緩存規則的資料值
     * @param expire     緩存過期時間
     *
     * @return {@link Mono<Void>} 表示所有規則應用完成的反應式物件
     *
     * @see CacheRule#apply(Object, Duration)
     * @see FluxContainer
     * @see Schedulers#boundedElastic()
     */
    private <R> Mono<Void> applyCacheRule(List<CacheRule<R>> cacheRules, R value, Duration expire) {
        if (value instanceof FluxContainer fluxContainer) {
            return fluxContainer
                    .getFlux()
                    .then()
                    .doOnSuccess(v -> cacheRules.forEach(rule -> rule.apply(value, expire).subscribeOn(Schedulers.boundedElastic()).subscribe()))
                    .subscribeOn(Schedulers.boundedElastic());
        }
        if (value instanceof Flux) {
            return ((Flux<?>) value)
                    .then()
                    .doOnSuccess(v -> cacheRules.forEach(rule -> rule.apply(value, expire).subscribeOn(Schedulers.boundedElastic()).subscribe()))
                    .subscribeOn(Schedulers.boundedElastic());
        }
        return Mono.when(cacheRules.stream().map(rule -> rule.apply(value, expire)).toList()).subscribeOn(Schedulers.boundedElastic());
    }


    /**
     * 執行 Cache-Aside 模式的流式緩存操作，使用預設過期時間。
     *
     * <p>此方法實現了針對 {@link Flux} 流式資料的 Cache-Aside 模式，
     * 適用於需要緩存集合或列表類型資料的場景。</p>
     *
     * <p><strong>與單值緩存的差異：</strong></p>
     * <ul>
     *   <li>支援流式資料的批量查詢</li>
     *   <li>可能涉及多個緩存鍵的操作</li>
     *   <li>回傳連續的資料流而非單一值</li>
     * </ul>
     *
     * <p><strong>適用場景：</strong></p>
     * <ul>
     *   <li>用戶列表查詢</li>
     *   <li>商品目錄獲取</li>
     *   <li>搜尋結果緩存</li>
     *   <li>分頁資料緩存</li>
     * </ul>
     *
     * @param <R>               流中元素的類型
     * @param keys              緩存鍵集合，用於查詢相關的緩存資料
     * @param clazz             流中元素的類型，用於緩存反序列化
     * @param cacheProviderEnum 緩存提供者類型
     * @param source            當緩存未命中時執行的資料源查詢邏輯
     * @param cacheRules        緩存規則列表，定義如何儲存查詢結果
     *
     * @return {@link Flux<R>} 包含查詢結果的反應式流
     *
     * @see #runAndSetCache(Collection, Class, CacheProviderEnum, Flux, List, Duration)
     */
    public <R> Flux<R> runAndSetCache(Collection<String> keys, Class<R> clazz, CacheProviderEnum cacheProviderEnum, Flux<? extends R> source, List<CacheRule<R>> cacheRules) {
        return runAndSetCache(keys, clazz, cacheProviderEnum, source, cacheRules, null);
    }


    /**
     * 執行 Cache-Aside 模式的流式緩存操作，使用指定的過期時間。
     *
     * <p>此方法實現了複雜的流式資料緩存邏輯，
     * 支援部分緩存命中的智能處理。</p>
     *
     * <p><strong>緩存命中策略：</strong></p>
     * <ul>
     *   <li><strong>完全命中</strong>：所有請求的鍵都有對應緩存時，直接回傳緩存資料</li>
     *   <li><strong>部分命中或完全未命中</strong>：執行源查詢邏輯並應用緩存規則</li>
     * </ul>
     *
     * <p><strong>資料流處理：</strong></p>
     * <ol>
     *   <li>批量查詢所有相關緩存</li>
     *   <li>檢查緩存命中率</li>
     *   <li>若完全命中，直接回傳緩存資料流</li>
     *   <li>否則執行源查詢，收集所有結果</li>
     *   <li>異步應用緩存規則到每個結果項目</li>
     *   <li>回傳完整的結果流</li>
     * </ol>
     *
     * @param <R>               流中元素的類型
     * @param keys              緩存鍵集合
     * @param clazz             流中元素的類型
     * @param cacheProviderEnum 緩存提供者類型
     * @param source            資料源查詢邏輯
     * @param cacheRules        緩存規則列表
     * @param expire            緩存過期時間
     *
     * @return {@link Flux<R>} 包含查詢結果的反應式流
     *
     * @see CacheProvider#getAllAsMap(Collection, Class)
     */
    public <R> Flux<R> runAndSetCache(Collection<String> keys, Class<R> clazz, CacheProviderEnum cacheProviderEnum, Flux<? extends R> source, List<CacheRule<R>> cacheRules, Duration expire) {
        return Optional.ofNullable(cacheProviderMap.get(cacheProviderEnum)).map(provider -> provider.getAllAsMap(keys, clazz).flatMapMany(map -> {
            if (map.size() == keys.size()) {
                return Flux.fromIterable(map.values());
            }
            return Flux.defer(() -> source.collectList().flatMapMany(valueList -> {
                executeWithLock(keys,
                                cacheProviderEnum,
                                acquiredKeys -> Flux.fromIterable(valueList).flatMap(value -> applyCacheRule(cacheRules, value, expire)).then()
                ).subscribeOn(Schedulers.boundedElastic()).subscribe();
                return Flux.fromIterable(valueList);
            }));
        })).orElseGet(() -> source.cast(clazz));
    }


    /**
     * 執行 Cache-Aside 模式的 Map 型緩存操作，使用預設過期時間。
     *
     * <p>此方法專門用於批量查詢場景，
     * 回傳結果保持鍵值對應關係，便於後續的條件處理。</p>
     *
     * <p><strong>與流式緩存的差異：</strong></p>
     * <ul>
     *   <li>保持查詢鍵與結果的對應關係</li>
     *   <li>便於判斷哪些鍵有對應的資料</li>
     *   <li>支援部分結果的條件處理</li>
     * </ul>
     *
     * <p><strong>適用場景：</strong></p>
     * <ul>
     *   <li>多用戶資訊批量查詢</li>
     *   <li>商品資訊批量獲取</li>
     *   <li>需要知道查詢結果對應關係的場景</li>
     * </ul>
     *
     * @param <R>               結果值的類型
     * @param keys              緩存鍵集合
     * @param clazz             結果值的類型
     * @param cacheProviderEnum 緩存提供者類型
     * @param source            資料源查詢邏輯，接收鍵集合並回傳對應的結果 Map
     * @param cacheRules        緩存規則列表
     *
     * @return Mono<Map<String, R>> 包含鍵值對應關係的查詢結果
     *
     * @see #runAndSetCaches(Collection, Class, CacheProviderEnum, Function, List, Duration)
     */
    public <R> Mono<Map<String, R>> runAndSetCaches(Collection<String> keys, Class<R> clazz, CacheProviderEnum cacheProviderEnum, Function<Collection<String>, Mono<Map<String, R>>> source, List<CacheRule<R>> cacheRules) {
        return runAndSetCaches(keys, clazz, cacheProviderEnum, source, cacheRules, null);
    }


    /**
     * 執行 Cache-Aside 模式的 Map 型緩存操作，使用指定的過期時間。
     *
     * <p>此方法是 Map 型批量緩存查詢的完整實現，
     * 提供高效的批量處理和靈活的緩存規則應用。</p>
     *
     * <p><strong>處理邏輯：</strong></p>
     * <ol>
     *   <li><strong>批量緩存查詢</strong>：一次性查詢所有相關緩存</li>
     *   <li><strong>緩存命中檢查</strong>：檢查是否所有鍵都有對應緩存</li>
     *   <li><strong>條件執行源查詢</strong>：僅在緩存未完全命中時執行</li>
     *   <li><strong>異步緩存寫入</strong>：對每個查詢結果應用緩存規則</li>
     *   <li><strong>回傳完整結果</strong>：回傳包含所有資料的 Map</li>
     * </ol>
     *
     * <p><strong>緩存規則應用：</strong></p>
     * <p>對源查詢回傳的 Map 中的每個值都會應用指定的緩存規則，
     * 允許實現複雜的緩存策略。</p>
     *
     * @param <R>               結果值的類型
     * @param keys              緩存鍵集合
     * @param clazz             結果值的類型
     * @param cacheProviderEnum 緩存提供者類型
     * @param source            資料源查詢邏輯
     * @param cacheRules        緩存規則列表
     * @param expire            緩存過期時間
     *
     * @return Mono<Map<String, R>> 包含查詢結果的反應式物件
     */
    public <R> Mono<Map<String, R>> runAndSetCaches(Collection<String> keys, Class<R> clazz, CacheProviderEnum cacheProviderEnum, Function<Collection<String>, Mono<Map<String, R>>> source, List<CacheRule<R>> cacheRules, Duration expire) {
        return Optional
                .ofNullable(cacheProviderMap.get(cacheProviderEnum))
                .map(provider -> provider.getAllAsMap(keys, clazz).switchIfEmpty(Mono.defer(() -> source.apply(keys).doOnNext(resultMap -> {
                    executeWithLock(keys,
                                    cacheProviderEnum,
                                    acquiredKeys -> Mono.when(resultMap.values().stream().map(r -> applyCacheRule(cacheRules, r, expire)).toList())
                    ).subscribeOn(Schedulers.boundedElastic()).subscribe();
                }))))
                .orElseGet(() -> source.apply(keys));
    }


    /**
     * 執行 Cache-Aside 模式的單鍵流式緩存操作。
     *
     * <p>此方法針對單一緩存鍵但回傳流式資料的場景，
     * 如單一用戶的訂單列表、商品的評論列表等。</p>
     *
     * <p><strong>適用場景：</strong></p>
     * <ul>
     *   <li>用戶的歷史訂單查詢</li>
     *   <li>文章的評論列表</li>
     *   <li>某個分類下的商品列表</li>
     * </ul>
     *
     * @param <R>               流中元素的類型
     * @param key               緩存鍵
     * @param clazz             流中元素的類型
     * @param cacheProviderEnum 緩存提供者類型
     * @param source            資料源查詢邏輯
     * @param cacheRules        緩存規則列表
     *
     * @return {@link Flux<R>} 包含查詢結果的反應式流
     *
     * @see #runAndSetCache(String, Class, CacheProviderEnum, Flux, List, Duration)
     */
    public <R> Flux<R> runAndSetCache(String key, Class<R> clazz, CacheProviderEnum cacheProviderEnum, Flux<? extends R> source, List<CacheRule<R>> cacheRules) {
        return runAndSetCache(key, clazz, cacheProviderEnum, source, cacheRules, null);
    }


    /**
     * 執行 Cache-Aside 模式的單鍵流式緩存操作，使用指定的過期時間。
     *
     * <p>此方法是單鍵流式緩存的完整實現，
     * 支援將列表形式的資料以流的方式處理和緩存。</p>
     *
     * <p><strong>處理流程：</strong></p>
     * <ol>
     *   <li>查詢指定鍵的列表緩存</li>
     *   <li>若緩存存在，將列表轉換為 Flux 流</li>
     *   <li>若緩存不存在，執行源查詢</li>
     *   <li>收集流中的所有元素為列表</li>
     *   <li>對每個元素應用緩存規則</li>
     *   <li>回傳元素流</li>
     * </ol>
     *
     * <p><strong>緩存存儲格式：</strong></p>
     * <p>流式資料會以列表形式儲存在緩存中，
     * 查詢時再轉換回流式處理。</p>
     *
     * @param <R>               流中元素的類型
     * @param key               緩存鍵
     * @param clazz             流中元素的類型
     * @param cacheProviderEnum 緩存提供者類型
     * @param source            資料源查詢邏輯
     * @param cacheRules        緩存規則列表
     * @param expire            緩存過期時間
     *
     * @return {@link Flux<R>} 包含查詢結果的反應式流
     *
     * @see CacheProvider#getAsList(String, Class)
     */
    public <R> Flux<R> runAndSetCache(String key, Class<R> clazz, CacheProviderEnum cacheProviderEnum, Flux<? extends R> source, List<CacheRule<R>> cacheRules, Duration expire) {
        return Optional.ofNullable(cacheProviderMap.get(cacheProviderEnum)).map(provider -> {
            return provider
                    .getAsList(key, clazz)
                    .flatMapMany(Flux::fromIterable)
                    .switchIfEmpty(Flux.defer(() -> source.collectList().flatMapMany(valueList -> {
                        executeWithLock(Collections.singletonList(key),
                                        cacheProviderEnum,
                                        acquiredKeys -> Flux
                                                .fromIterable(valueList)
                                                .flatMap(value -> applyCacheRule(cacheRules, value, expire))
                                                .then()
                        ).subscribeOn(Schedulers.boundedElastic()).subscribe();
                        return Flux.fromIterable(valueList);
                    })));
        }).orElseGet(() -> source.cast(clazz));
    }


    /**
     * 實例方法：根據固定鍵生成緩存規則。
     *
     * <p>此方法用於建立使用固定緩存鍵的緩存規則，
     * 適用於不需要動態生成鍵的場景。</p>
     *
     * <p><strong>使用範例：</strong></p>
     * <pre>{@code
     * // 系統配置緩存
     * CacheRule<SystemConfig> configRule = cacheManager.generateCacheRule(
     *     "system:config",
     *     CacheProviderEnum.USER_CACHE
     * );
     *
     * // 使用者檔案列表緩存
     * CacheRule<List<FileInfo>> fileListRule = cacheManager.generateCacheRule(
     *     "files:user:list",
     *     CacheProviderEnum.USER_FILE_LIST_CACHE
     * );
     * }</pre>
     *
     * <p><strong>適用場景：</strong></p>
     * <ul>
     *   <li>全域配置項目</li>
     *   <li>統計資料緩存</li>
     *   <li>共享的業務資料</li>
     * </ul>
     *
     * @param <R>               物件的類型
     * @param key               固定的緩存鍵
     * @param cacheProviderEnum 緩存提供者類型
     *
     * @return {@link CacheRule<R>} 生成的緩存規則
     *
     * @see CacheRule
     * @see SkipRecord
     */
    @SkipRecord
    public <R> CacheRule<R> generateCacheRule(String key, CacheProviderEnum cacheProviderEnum) {
        return (value, expire) -> Optional
                .ofNullable(cacheProviderMap.get(cacheProviderEnum))
                .map(provider -> provider.set(key, value, expire))
                .orElseGet(Mono::empty);
    }
}