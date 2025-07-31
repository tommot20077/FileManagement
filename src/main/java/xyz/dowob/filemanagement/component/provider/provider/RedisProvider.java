package xyz.dowob.filemanagement.component.provider.provider;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.HideOverLength;
import xyz.dowob.filemanagement.data.response.PagedResponseDTO;
import xyz.dowob.filemanagement.exception.ProcessException;

import java.time.Duration;
import java.util.*;

/**
 * Redis 資料操作提供者，基於 ReactiveRedisTemplate 實現全面的非阻塞 Redis 資料結構操作。
 *
 * <p>此類別封裝 Redis 的各種資料結構操作，包含 String、Hash、Set、List、ZSet 等。
 * 提供統一且簡潔的 API 介面，隱藏 Redis 操作的複雜性。支援批次操作、過期時間設定、
 * 模式匹配查詢及自動序列化處理。</p>
 *
 * <p>特色功能包含：隨機過期時間防止緩存雪崩、分頁響應自動轉換、
 * 分塊集合管理、模式匹配掃描及類型安全的資料轉換。
 * 內建 ObjectMapper 支援複雜對象的序列化與反序列化。</p>
 *
 * <p>過期時間採用隨機化策略，在原始時間基礎上增加隨機係數，
 * 有效防止大量緩存同時過期造成的系統壓力。</p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@Component
@SuppressWarnings("unused")
public class RedisProvider {
    /**
     * 隨機緩存過期時間的最大比例係數，用於防止緩存雪崩現象。
     * <p>
     * 此常數定義了過期時間隨機化的上限值，實際過期時間將在
     * [expireTime, expireTime × 1.2] 的範圍內隨機生成。
     * 這種策略能夠有效分散緩存過期時間，避免大量緩存同時失效。
     * </p>
     * 
     * @see #setExpire(String, Duration)
     */
    private static final float RANDOM_CACHE_EXPIRE_TIME_RATIO = 1.2f;

    /**
     * 反應式 Redis 操作模板，提供非阻塞式的 Redis 資料存取功能。
     * <p>
     * 此模板基於 Spring Data Redis 的 ReactiveRedisTemplate，支援所有 Redis 資料結構操作。
     * 採用非阻塞式 I/O 模式，非常適合高併發和反應式編程環境。
     * 所有操作都返回 Mono 或 Flux 類型，支援反應式流的組合和轉換。
     * </p>
     * 
     * @see ReactiveRedisTemplate
     */
    private final ReactiveRedisTemplate<String, Object> redisTemplate;

    /**
     * Jackson ObjectMapper 實例，用於複雜物件的序列化和反序列化處理。
     * <p>
     * 此組件負責處理 Java 物件與 JSON 之間的轉換，支援簡單類型和泛型類型的轉換。
     * 在 Redis 存儲過程中，用於將複雜物件轉換為可存儲的格式，
     * 並在讀取時將其還原為原始類型。
     * </p>
     * 
     * @see ObjectMapper
     * @see #convertObjectList(Object, Class)
     */
    private final ObjectMapper objectMapper;


    /**
     * 建構 Redis 提供者實例，初始化所需的核心組件。
     * <p>
     * 透過 Spring 的依賴注入機制自動裝配必要的 Redis 操作模板和序列化工具。
     * 此建構函數確保所有 Redis 操作都具備必要的基礎設施。
     * </p>
     * <p>
     * <strong>初始化組件：</strong>
     * </p>
     * <ul>
     *   <li>反應式 Redis 操作模板：提供非阻塞式的 Redis 資料存取</li>
     *   <li>JSON 序列化工具：用於複雜物件的轉換處理</li>
     * </ul>
     * 
     * @param redisTemplate 反應式 Redis 操作模板實例，不得為 null
     * @param objectMapper JSON 序列化工具實例，不得為 null
     * @see ReactiveRedisTemplate
     * @see ObjectMapper
     */
    public RedisProvider(ReactiveRedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }


    /**
     * 將資料存入 Redis 的 String 結構中，並設定過期時間。
     * <p>
     * 此方法提供完整的鍵值存儲功能，支援過期時間設定。過期時間會經過隨機化處理，
     * 有效防止緩存雪崩現象。如果過期時間為 null 或負值，則調用不設過期時間的版本。
     * </p>
     * <p>
     * <strong>過期時間隨機化策略：</strong>
     * </p>
     * <ul>
     *   <li>實際過期時間為 [expireTime, expireTime × 1.2] 的隨機值</li>
     *   <li>避免大量緩存同時過期造成的系統壓力</li>
     *   <li>提升系統在高併發環境下的穩定性</li>
     * </ul>
     * <p>
     * <strong>使用情境：</strong>
     * </p>
     * <ul>
     *   <li>存儲需要定時失效的用戶會話資訊</li>
     *   <li>緩存計算結果以提升性能</li>
     *   <li>存儲臨時狀態資料</li>
     * </ul>
     * 
     * @param key 緩存鍵名，不得為 null 或空字串
     * @param value 要存儲的值，支援任何可序列化的對象
     * @param expireTime 過期時間，為 null 或負值時不設定過期
     * @return Mono&lt;Void&gt; 操作完成信號，成功時完成，失敗時發出異常
     * @see #setValue(String, Object)
     * @see #setExpire(String, Duration)
     * @apiNote 採用非阻塞式操作，適合反應式編程環境
     * @implNote 內部使用 ReactiveRedisTemplate 進行實際操作
     */
    public Mono<Void> setValue(String key, Object value, Duration expireTime) {
        if (expireTime == null || expireTime.isNegative()) {
            return setValue(key, value);
        }
        return redisTemplate.opsForValue().set(key, value).then(setExpire(key, expireTime));
    }


    /**
     * 將資料存入 Redis 的 String 結構中，不設定過期時間。
     * <p>
     * 此方法提供基礎的鍵值存儲功能，適用於需要永久保存的資料。
     * 存儲的資料將一直保持到手動刪除或 Redis 服務重啟。
     * </p>
     * <p>
     * <strong>適用場景：</strong>
     * </p>
     * <ul>
     *   <li>存儲應用程式配置資訊</li>
     *   <li>保存不需要過期的計數器</li>
     *   <li>緩存長期有效的靜態資料</li>
     * </ul>
     * <p>
     * <strong>注意事項：</strong>
     * </p>
     * <ul>
     *   <li>沒有過期時間的資料需要適當管理，避免內存溢出</li>
     *   <li>建議對重要的持久化資料設定定期清理機制</li>
     * </ul>
     * 
     * @param key 緩存鍵名，不得為 null 或空字串
     * @param value 要存儲的值，支援任何可序列化的對象
     * @return Mono&lt;Void&gt; 操作完成信號，成功時完成，失敗時發出異常
     * @see #setValue(String, Object, Duration)
     * @apiNote 此方法為永久存儲，不會自動過期
     * @implNote 使用 ReactiveRedisTemplate.opsForValue().set() 進行存儲
     */
    public Mono<Void> setValue(String key, Object value) {
        return redisTemplate.opsForValue().set(key, value).then();
    }


    /**
     * 為指定的鍵設定隨機化過期時間，有效防止緩存雪崩現象。
     * <p>
     * 此方法實現了過期時間隨機化策略，在原始過期時間基礎上增加隨機係數，
     * 使得大量緩存的過期時間分散在一個時間範圍內，而不是同一時刻。
     * </p>
     * <p>
     * <strong>隨機化算法：</strong>
     * </p>
     * <ol>
     *   <li>生成 1.0 到 1.2 之間的隨機係數</li>
     *   <li>實際過期時間 = 原始時間 × 隨機係數</li>
     *   <li>確保過期時間在合理範圍內變動</li>
     * </ol>
     * <p>
     * <strong>防雪崩效果：</strong>
     * </p>
     * <ul>
     *   <li>避免大量緩存同時失效</li>
     *   <li>減少瞬間的後端壓力</li>
     *   <li>提升系統整體穩定性</li>
     * </ul>
     * <p>
     * <strong>使用範例：</strong>
     * </p>
     * <pre>
     * // 設定 1 小時的隨機過期時間（實際為 1-1.2 小時）
     * setExpire("user:session:123", Duration.ofHours(1))
     * </pre>
     * 
     * @param key 要設定過期時間的緩存鍵名，必須已存在於 Redis 中
     * @param expireTime 基礎過期時間，將在此基礎上進行隨機化
     * @return Mono&lt;Void&gt; 操作完成信號，成功時完成，失敗時發出異常
     * @see #RANDOM_CACHE_EXPIRE_TIME_RATIO
     * @apiNote 此方法為私有方法，僅供內部使用
     * @implNote 使用 Random 生成隨機係數，確保過期時間的分散性
     */
    private Mono<Void> setExpire(String key, Duration expireTime) {
        Random random = new Random();
        float randomRatio = 1.0f + random.nextFloat() * (RANDOM_CACHE_EXPIRE_TIME_RATIO - 1.0f);
        Duration randomExpireTime = Duration.ofSeconds((long) (expireTime.getSeconds() * randomRatio));
        return redisTemplate.expire(key, randomExpireTime).then();
    }


    /**
     * 僅在鍵不存在的情況下設定值，並設定隨機化過期時間。
     * <p>
     * 此方法實現了 Redis 的 SETNX（Set if Not eXists）操作，
     * 結合過期時間設定，常用於實現分散式鎖或防重複操作機制。
     * 只有當指定的鍵不存在時，才會執行設定操作。
     * </p>
     * <p>
     * <strong>原子性保證：</strong>
     * </p>
     * <ul>
     *   <li>檢查鍵存在性和設定值為原子操作</li>
     *   <li>避免並發情況下的競爭條件</li>
     *   <li>確保操作的一致性和可靠性</li>
     * </ul>
     * <p>
     * <strong>典型應用場景：</strong>
     * </p>
     * <ul>
     *   <li><strong>分散式鎖：</strong>實現資源的互斥存取</li>
     *   <li><strong>防重複提交：</strong>避免表單重複提交</li>
     *   <li><strong>限流控制：</strong>實現基於時間窗口的限流</li>
     *   <li><strong>快取預熱：</strong>避免重複載入相同資料</li>
     * </ul>
     * <p>
     * <strong>使用範例：</strong>
     * </p>
     * <pre>
     * // 實現分散式鎖
     * setValueIfAbsent("lock:resource:123", "locked", Duration.ofMinutes(5))
     *     .map(success -&gt; success ? "獲取鎖成功" : "鎖已被占用")
     * </pre>
     * 
     * @param key 緩存鍵名，不得為 null 或空字串
     * @param value 要設定的值，當鍵不存在時存儲此值
     * @param expireTime 過期時間，為 null 或負值時調用無過期時間版本
     * @return Mono&lt;Boolean&gt; 操作結果，true 表示設定成功，false 表示鍵已存在
     * @see #setValueIfAbsent(String, Object)
     * @see #setExpire(String, Duration)
     * @apiNote 適合實現分散式鎖和防重複操作的場景
     * @implNote 使用 ReactiveRedisTemplate.opsForValue().setIfAbsent() 實現
     */
    public Mono<Boolean> setValueIfAbsent(String key, Object value, Duration expireTime) {
        if (expireTime == null || expireTime.isNegative()) {
            return setValueIfAbsent(key, value);
        }
        return redisTemplate.opsForValue().setIfAbsent(key, value).flatMap(result -> setExpire(key, expireTime).thenReturn(result));
    }


    /**
     * 僅在鍵不存在的情況下設定值，不設定過期時間。
     * <p>
     * 此方法提供基礎的條件設定功能，只有當指定鍵在 Redis 中不存在時，
     * 才會執行設定操作。這是 Redis SETNX 指令的直接封裝。
     * </p>
     * <p>
     * <strong>與 setValue 的區別：</strong>
     * </p>
     * <ul>
     *   <li><strong>setValue：</strong>無條件覆蓋現有值</li>
     *   <li><strong>setValueIfAbsent：</strong>僅在鍵不存在時設定</li>
     * </ul>
     * <p>
     * <strong>適用場景：</strong>
     * </p>
     * <ul>
     *   <li>設定預設配置值（不覆蓋已有配置）</li>
     *   <li>初始化計數器（避免重複初始化）</li>
     *   <li>實現簡單的存在性檢查</li>
     * </ul>
     * <p>
     * <strong>注意事項：</strong>
     * </p>
     * <ul>
     *   <li>沒有過期時間，資料將永久保存</li>
     *   <li>需要手動管理資料的生命週期</li>
     * </ul>
     * 
     * @param key 緩存鍵名，不得為 null 或空字串
     * @param value 要設定的值，僅在鍵不存在時才會存儲
     * @return Mono&lt;Boolean&gt; 操作結果，true 表示設定成功，false 表示鍵已存在
     * @see #setValueIfAbsent(String, Object, Duration)
     * @see #setValue(String, Object)
     * @apiNote 此方法為永久存儲，適合設定預設值或初始化資料
     * @implNote 直接使用 ReactiveRedisTemplate.opsForValue().setIfAbsent() 實現
     */
    public Mono<Boolean> setValueIfAbsent(String key, Object value) {
        return redisTemplate.opsForValue().setIfAbsent(key, value);
    }


    /**
     * 根據鍵名從 Redis 的 String 結構中獲取資料。
     * <p>
     * 此方法提供基礎的鍵值查詢功能，返回的是 Object 類型，
     * 需要調用者手動進行類型轉換。如果需要特定類型，建議使用重載版本。
     * </p>
     * <p>
     * <strong>返回值說明：</strong>
     * </p>
     * <ul>
     *   <li>如果鍵存在，返回對應的值</li>
     *   <li>如果鍵不存在，返回空的 Mono</li>
     *   <li>如果鍵已過期，返回空的 Mono</li>
     * </ul>
     * <p>
     * <strong>使用建議：</strong>
     * </p>
     * <ul>
     *   <li>對於已知類型的資料，建議使用 {@link #getValue(String, Class)}</li>
     *   <li>使用 switchIfEmpty() 處理空值情況</li>
     *   <li>結合 map() 進行後續處理</li>
     * </ul>
     * 
     * @param key 要查詢的緩存鍵名，不得為 null 或空字串
     * @return Mono&lt;Object&gt; 包含查詢結果的反應式流，空值時為空 Mono
     * @see #getValue(String, Class)
     * @apiNote 返回的 Object 可能需要類型轉換，建議使用有類型參數的版本
     * @implNote 直接使用 ReactiveRedisTemplate.opsForValue().get() 實現
     */
    public Mono<Object> getValue(String key) {
        return redisTemplate.opsForValue().get(key);
    }


    /**
     * 根據鍵名從 Redis 的 String 結構中獲取資料，並直接轉換為指定類型。
     * <p>
     * 此方法提供類型安全的查詢功能，自動將緩存中的資料轉換為指定的類型。
     * 適用於已知資料類型的查詢場景，避免手動類型轉換的繁瑣。
     * </p>
     * <p>
     * <strong>類型轉換說明：</strong>
     * </p>
     * <ul>
     *   <li>使用 Reactor 的 cast() 方法進行類型轉換</li>
     *   <li>支援所有 Java 原始類型和物件類型</li>
     *   <li>類型不符合時會拋出 ClassCastException</li>
     * </ul>
     * <p>
     * <strong>常用類型範例：</strong>
     * </p>
     * <ul>
     *   <li><strong>String.class：</strong>獲取字串資料</li>
     *   <li><strong>Integer.class：</strong>獲取整數資料</li>
     *   <li><strong>Boolean.class：</strong>獲取布林值</li>
     *   <li><strong>自定義類別：</strong>獲取序列化後的物件</li>
     * </ul>
     * <p>
     * <strong>錯誤處理：</strong>
     * </p>
     * <ul>
     *   <li>類型不符合時拋出 ClassCastException</li>
     *   <li>鍵不存在時返回空 Mono</li>
     *   <li>建議使用 onErrorMap() 處理類型轉換錯誤</li>
     * </ul>
     * 
     * @param <T> 返回資料的泛型類型
     * @param key 要查詢的緩存鍵名，不得為 null 或空字串
     * @param clazz 目標類型的 Class 對象，用於類型轉換
     * @return Mono&lt;T&gt; 包含指定類型資料的反應式流，空值時為空 Mono
     * @throws ClassCastException 當緩存中的資料類型與指定類型不符時
     * @see #getValue(String)
     * @apiNote 適用於已知資料類型的查詢場景，提供類型安全保證
     * @implNote 使用 Reactor 的 cast() 方法進行類型轉換
     */
    public <T> Mono<T> getValue(String key, Class<T> clazz) {
        return redisTemplate.opsForValue().get(key).cast(clazz);
    }


    /**
     * 刪除指定鍵名的 Redis 資料，適用於單一鍵的刪除操作。
     * <p>
     * 此方法能夠刪除 Redis 中任何類型的資料結構（String, Hash, Set, List, ZSet）。
     * 刪除操作是原子性的，不可逆轉，請謹慎使用。
     * </p>
     * <p>
     * <strong>刪除範圍：</strong>
     * </p>
     * <ul>
     *   <li>支援所有 Redis 資料類型（String, Hash, Set, List, ZSet）</li>
     *   <li>一次只能刪除一個鍵</li>
     *   <li>鍵不存在時不會報錯，但返回 0</li>
     * </ul>
     * <p>
     * <strong>使用場景：</strong>
     * </p>
     * <ul>
     *   <li>清理過期或無效的緩存資料</li>
     *   <li>釋放不再需要的資源</li>
     *   <li>重設特定鍵的狀態</li>
     * </ul>
     * <p>
     * <strong>注意事項：</strong>
     * </p>
     * <ul>
     *   <li>刪除操作不可復原，請確認後再執行</li>
     *   <li>大型資料結構的刪除可能會影響性能</li>
     *   <li>建議在低使用時段執行大量刪除操作</li>
     * </ul>
     * 
     * @param key 要刪除的鍵名，不得為 null 或空字串
     * @return Mono&lt;Long&gt; 成功刪除的鍵數量，通常為 0（鍵不存在）或 1（刪除成功）
     * @see #deleteValue(Collection)
     * @see #deleteValue(String...)
     * @apiNote 此方法可刪除任何類型的 Redis 資料結構
     * @implNote 使用 ReactiveRedisTemplate.delete() 進行刪除操作
     */
    public Mono<Long> deleteValue(String key) {
        return redisTemplate.delete(key);
    }


    /**
     * 批量刪除指定鍵名集合中的所有 Redis 資料。
     * <p>
     * 此方法提供高效的批量刪除功能，能夠一次性刪除多個鍵的資料。
     * 相比於多次單一刪除，批量刪除能夠显著減少網路交互次數，提升性能。
     * </p>
     * <p>
     * <strong>性能優勢：</strong>
     * </p>
     * <ul>
     *   <li>減少網路往返次數，提升執行效率</li>
     *   <li>原子性批量操作，確保數據一致性</li>
     *   <li>減少 Redis 服務器的負載壓力</li>
     * </ul>
     * <p>
     * <strong>使用建議：</strong>
     * </p>
     * <ul>
     *   <li>對於大量鍵的刪除，建議分批處理避免阻塞</li>
     *   <li>空集合會返回 0，不會執行實際操作</li>
     *   <li>鍵不存在時不會影響總計數</li>
     * </ul>
     * <p>
     * <strong>使用範例：</strong>
     * </p>
     * <pre>
     * List&lt;String&gt; keysToDelete = Arrays.asList("user:1", "user:2", "user:3");
     * deleteValue(keysToDelete)
     *     .doOnNext(count -&gt; log.info("成功刪除 {} 個鍵", count))
     * </pre>
     * 
     * @param keys 要刪除的鍵名集合，不得為 null，可為空集合
     * @return Mono&lt;Long&gt; 成功刪除的鍵數量，範圍為 0 到 keys.size()
     * @see #deleteValue(String)
     * @see #deleteValue(String...)
     * @apiNote 適合批量清理操作，比單一刪除更高效
     * @implNote 內部將集合轉換為數組後調用 Redis 批量刪除
     */
    public Mono<Long> deleteValue(Collection<String> keys) {
        return redisTemplate.delete(keys.toArray(new String[0]));
    }


    /**
     * 使用可變參數批量刪除指定的多個 Redis 鍵。
     * <p>
     * 此方法提供了更加簡潔的批量刪除語法，允許直接傳遞多個鍵名作為參數，
     * 而不需要先建立集合。適合編譯時已知要刪除的鍵名清單的情況。
     * </p>
     * <p>
     * <strong>語法優勢：</strong>
     * </p>
     * <ul>
     *   <li>支援可變參數，調用更加簡潔</li>
     *   <li>編譯時型別檢查，避免運行時錯誤</li>
     *   <li>不需要手動建立集合或數組</li>
     * </ul>
     * <p>
     * <strong>使用範例：</strong>
     * </p>
     * <pre>
     * // 刪除多個用戶相關的緩存
     * deleteValue("user:profile:123", "user:session:123", "user:preferences:123")
     *     .doOnNext(count -&gt; log.info("刪除了 {} 個用戶緩存", count))
     * 
     * // 單一鍵刪除
     * deleteValue("temp:data:abc")
     * </pre>
     * <p>
     * <strong>注意事項：</strong>
     * </p>
     * <ul>
     *   <li>空參數列表會返回 0，不執行任何操作</li>
     *   <li>當有部分鍵不存在時，只計算成功刪除的數量</li>
     * </ul>
     * 
     * @param keys 要刪除的鍵名列表，可為空或包含多個鍵名
     * @return Mono&lt;Long&gt; 成功刪除的鍵數量
     * @see #deleteValue(String)
     * @see #deleteValue(Collection)
     * @apiNote 提供了更加簡潔的批量刪除語法
     * @implNote 直接使用 ReactiveRedisTemplate.delete(String...) 實現
     */
    public Mono<Long> deleteValue(String... keys) {
        return redisTemplate.delete(keys);
    }


    /**
     * 從 Redis 中獲取序列化的分頁響應資料，並自動轉換為指定類型的分頁物件。
     * <p>
     * 此方法專門用於處理在 Redis 中存儲的分頁資料，能夠自動將 JSON 格式的
     * 分頁回應轉換為強類型的 PagedResponseDTO 對象。適用於緩存分頁查詢結果的場景。
     * </p>
     * <p>
     * <strong>資料轉換流程：</strong>
     * </p>
     * <ol>
     *   <li>從 Redis 中獲取序列化後的 JSON 資料</li>
     *   <li>使用 Jackson ObjectMapper 轉換為泛型化的 PagedResponseDTO</li>
     *   <li>自動處理內部元素的類型轉換</li>
     * </ol>
     * <p>
     * <strong>序列化要求：</strong>
     * </p>
     * <ul>
     *   <li>儲存的資料必須是完整的 PagedResponseDTO JSON 格式</li>
     *   <li>支援嵌套的泛型類型轉換</li>
     *   <li>必須包含正確的分頁元資訊（總數、頁碼等）</li>
     * </ul>
     * <p>
     * <strong>使用場景：</strong>
     * </p>
     * <ul>
     *   <li>緩存分頁查詢結果，減少資料庫壓力</li>
     *   <li>快速返回已計算的分頁資料</li>
     *   <li>提升分頁查詢的回應性能</li>
     * </ul>
     * <p>
     * <strong>錯誤處理：</strong>
     * </p>
     * <ul>
     *   <li>當資料不存在時返回空 Mono</li>
     *   <li>當 JSON 格式不正確時拋出轉換異常</li>
     *   <li>類型不符合時拋出 ClassCastException</li>
     * </ul>
     * 
     * @param <T> 分頁資料的元素類型
     * @param key 存儲分頁資料的緩存鍵名
     * @param clazz 分頁元素的類型 Class 對象
     * @return Mono&lt;PagedResponseDTO&lt;T&gt;&gt; 包含分頁資料的反應式流
     * @see PagedResponseDTO
     * @see ObjectMapper
     * @apiNote 此方法會長時間運行或返回大量資料，被 @HideOverLength 標註
     * @implNote 使用 Jackson 的泛型類型轉換功能進行精確轉換
     */
    @HideOverLength
    public <T> Mono<PagedResponseDTO<T>> getPagedResponseFromValue(String key, Class<T> clazz) {
        return redisTemplate.opsForValue().get(key).map(obj -> {
            JavaType type = objectMapper.getTypeFactory().constructParametricType(PagedResponseDTO.class, clazz);
            return objectMapper.convertValue(obj, type);
        });
    }


    /**
     * 從 Redis 中獲取序列化的列表資料，並轉換為指定類型的反應式流。
     * <p>
     * 此方法專門處理儲存在 Redis String 結構中的集合或列表資料，
     * 能夠自動識別資料格式（單一元素或集合）並進行相應的轉換處理。
     * </p>
     * <p>
     * <strong>智能轉換功能：</strong>
     * </p>
     * <ul>
     *   <li><strong>集合轉換：</strong>當資料為 Collection 時，逐個轉換元素</li>
     *   <li><strong>單一轉換：</strong>當資料為單一對象時，轉換為單元素流</li>
     *   <li><strong>類型檢查：</strong>優先使用直接類型轉換，失敗後使用 ObjectMapper</li>
     * </ul>
     * <p>
     * <strong>支援的資料格式：</strong>
     * </p>
     * <ul>
     *   <li>JSON 序列化的 Java 集合（List, Set 等）</li>
     *   <li>單一 JSON 對象</li>
     *   <li>原始 Java 對象（已反序列化）</li>
     * </ul>
     * <p>
     * <strong>錯誤處理機制：</strong>
     * </p>
     * <ul>
     *   <li>當資料不存在時返回空流 (Flux.empty())</li>
     *   <li>當轉換失敗時拋出 ProcessException 並包含詳細錯誤訊息</li>
     *   <li>自動轉換 null 值為空流</li>
     * </ul>
     * <p>
     * <strong>使用範例：</strong>
     * </p>
     * <pre>
     * // 獲取用戶列表
     * getValueList("users", User.class)
     *     .doOnNext(user -&gt; log.info("用戶：{}", user.getName()))
     *     .collectList()
     * </pre>
     * 
     * @param <T> 列表元素的類型
     * @param key 存儲列表資料的緩存鍵名
     * @param clazz 列表元素的類型 Class 對象
     * @return Flux&lt;T&gt; 包含指定類型元素的反應式流
     * @see #convertObjectList(Object, Class)
     * @see ProcessException.ErrorCode#CONVERT_JSON_TO_TARGET_FAILED
     * @apiNote 此方法具備智能類型轉換能力，能處理多種資料格式
     * @implNote 內部使用 convertObjectList 方法進行精細化轉換處理
     */
    public <T> Flux<T> getValueList(String key, Class<T> clazz) {
        return redisTemplate.opsForValue().get(key).flatMapMany(object -> convertObjectList(object, clazz));
    }


    /**
     * 將任意類型的資料轉換為指定類型的反應式流，支援集合和單一對象的轉換。
     * <p>
     * 此方法是 Redis 資料轉換的核心實現，具備高度的灵活性和健壯性。
     * 能夠處理各種類型的輸入資料，並自動選擇最適合的轉換策略。
     * </p>
     * <p>
     * <strong>智能轉換策略：</strong>
     * </p>
     * <ol>
     *   <li><strong>空值處理：</strong>當 objects 為 null 時，直接返回空流</li>
     *   <li><strong>集合轉換：</strong>當輸入是 Collection 時，逐個轉換元素</li>
     *   <li><strong>單元素轉換：</strong>當輸入是單一對象時，將其轉換為單元素流</li>
     * </ol>
     * <p>
     * <strong>類型轉換優先級：</strong>
     * </p>
     * <ol>
     *   <li><strong>直接轉換：</strong>如果對象已是目標類型，直接轉換</li>
     *   <li><strong>ObjectMapper 轉換：</strong>使用 Jackson 進行複雜物件轉換</li>
     *   <li><strong>錯誤捕獲：</strong>轉換失敗時自動包裝為 ProcessException</li>
     * </ol>
     * <p>
     * <strong>支援的輸入類型：</strong>
     * </p>
     * <ul>
     *   <li>null （返回空流）</li>
     *   <li>Collection&lt;?&gt; 及其子類型（List, Set, Queue 等）</li>
     *   <li>任意單一 Java 對象</li>
     *   <li>JSON 序列化後的資料結構</li>
     * </ul>
     * <p>
     * <strong>錯誤處理機制：</strong>
     * </p>
     * <ul>
     *   <li>當 ObjectMapper 轉換失敗時，拋出 ProcessException</li>
     *   <li>異常訊息包含詳細的類型資訊和原因</li>
     *   <li>使用 Reactor 的 onErrorMap 進行統一錯誤處理</li>
     * </ul>
     * <p>
     * <strong>性能特性：</strong>
     * </p>
     * <ul>
     *   <li>懶性評估：使用 Flux.defer() 確保計算延遲執行</li>
     *   <li>空值優化：使用 switchIfEmpty() 提高空值處理效率</li>
     *   <li>流式轉換：支援大量資料的分批處理</li>
     * </ul>
     * 
     * @param <T> 目標轉換類型
     * @param objects 要轉換的原始資料，可為 null、集合或單一對象
     * @param clazz 目標類型的 Class 對象，用於指導轉換過程
     * @return Flux&lt;T&gt; 包含轉換後指定類型元素的反應式流
     * @see ProcessException.ErrorCode#CONVERT_JSON_TO_TARGET_FAILED
     * @see ObjectMapper#convertValue(Object, Class)
     * @apiNote 此方法為私有方法，僅供內部使用，是資料轉換的核心實現
     * @implNote 使用組合式設計模式，結合多種轉換策略提供最佳效果
     */
    private <T> Flux<T> convertObjectList(Object objects, Class<T> clazz) {
        if (objects == null) {
            return Flux.empty();
        }

        return Flux
                .defer(() -> {
                    if ((objects instanceof Collection<?> collection)) {
                        List<T> finalList = collection.stream().map(object -> {
                            if (clazz.isInstance(object)) {
                                return clazz.cast(object);
                            }
                            return objectMapper.convertValue(object, clazz);
                        }).toList();
                        return Flux.fromIterable(finalList);
                    }
                    T convertedObject = clazz.isInstance(objects) ? clazz.cast(objects) : objectMapper.convertValue(objects, clazz);
                    return Flux.just(convertedObject);
                })
                .switchIfEmpty(Flux.empty())
                .onErrorMap(e -> new ProcessException(ProcessException.ErrorCode.CONVERT_JSON_TO_TARGET_FAILED, e, clazz.getName()));
    }


    /**
     * 從 Redis Hash 結構中獲取指定字段的資料，並直接轉換為指定類型。
     * <p>
     * 此方法提供了 Redis Hash 結構的類型安全訪問方式，適用於已知資料類型的查詢場景。
     * Hash 結構可理解為一個巨大的 Map，其中包含多個 key-value 對。
     * </p>
     * <p>
     * <strong>Hash 結構的優勢：</strong>
     * </p>
     * <ul>
     *   <li>節省的緩存空間：相比於多個 String 鍵，更節省空間</li>
     *   <li>原子性操作：支援在單一 Hash 內的原子操作</li>
     *   <li>部分更新：可以只更新特定字段，而不影響其他字段</li>
     * </ul>
     * <p>
     * <strong>典型應用場景：</strong>
     * </p>
     * <ul>
     *   <li><strong>用戶資料管理：</strong>user:123 -> {name, email, age}</li>
     *   <li><strong>産品屬性：</strong>product:456 -> {price, stock, category}</li>
     *   <li><strong>配置設定：</strong>config:app -> {timeout, maxUsers, debug}</li>
     * </ul>
     * <p>
     * <strong>類型轉換說明：</strong>
     * </p>
     * <ul>
     *   <li>使用 Reactor 的 cast() 進行簡單類型轉換</li>
     *   <li>不支援複雜物件轉換，如需要完整類型轉換建議使用其他重載版本</li>
     * </ul>
     * 
     * @param <T> 返回值的泛型類型
     * @param hashKey Hash 的主鍵名，標識特定的 Hash 結構
     * @param innerKey Hash 內部的字段名，指定要獲取的字段
     * @param clazz 目標類型的 Class 對象，用於類型轉換
     * @return Mono&lt;T&gt; 包含指定類型值的反應式流，空值時為空 Mono
     * @throws ClassCastException 當存儲的資料類型與指定類型不符時
     * @see #getHashMap(String, String)
     * @apiNote 適用於已知字段類型的查詢場景，提供類型安全保證
     * @implNote 使用 ReactiveRedisTemplate.opsForHash().get() 加上 cast() 實現
     */
    public <T> Mono<T> getHashMap(String hashKey, String innerKey, Class<T> clazz) {
        return redisTemplate.opsForHash().get(hashKey, innerKey).cast(clazz);
    }


    /**
     * 對 Redis String 結構中的數值執行原子性自增操作，並設定隨機化過期時間。
     * <p>
     * 此方法結合了 Redis 的 INCR 系列指令與過期時間設定，提供了完整的計數器實現。
     * 被廣泛應用於限流控制、統計計數和分散式計數器等場景。
     * </p>
     * <p>
     * <strong>原子性保證：</strong>
     * </p>
     * <ul>
     *   <li>INCR 操作是原子性的，在並發環境下不會出現競爭條件</li>
     *   <li>讀取-修改-寫入操作在 Redis 內部一次完成</li>
     *   <li>適合實現分散式系統中的精確計數</li>
     * </ul>
     * <p>
     * <strong>自增特性：</strong>
     * </p>
     * <ul>
     *   <li>支援正数、負數和零的增量值</li>
     *   <li>當鍵不存在時，自動初始化為 0 再進行增量</li>
     *   <li>當存儲的值不是數字時，會拋出錯誤</li>
     * </ul>
     * <p>
     * <strong>典型應用場景：</strong>
     * </p>
     * <ul>
     *   <li><strong>限流計數：</strong>記錄每分鐘的 API 訪問次數</li>
     *   <li><strong>統計系統：</strong>累計頁面瀏覽次數、用戶登入次數</li>
     *   <li><strong>序列號生成：</strong>為訂單、使用者等生成獨一 ID</li>
     * </ul>
     * <p>
     * <strong>使用範例：</strong>
     * </p>
     * <pre>
     * // API 限流：每分鐘最多 100 次訪問
     * incrementDelta("rate_limit:user:123", 1, Duration.ofMinutes(1))
     *     .filter(count -&gt; count &lt;= 100)
     *     .switchIfEmpty(Mono.error(new LimitExceededException()))
     * </pre>
     * 
     * @param key 計數器的鍵名，通常包含業務粘示和唯一標識
     * @param delta 增量值，可為正數（增加）、負數（減少）或零（不變）
     * @param expireTime 過期時間，將經過隨機化處理以防止雪崩
     * @return Mono&lt;Long&gt; 自增後的結果值，可用於限流判斷或統計分析
     * @see #incrementDelta(String, long)
     * @see #setExpire(String, Duration)
     * @apiNote 此方法是實現限流、計數功能的最佳選擇
     * @implNote 先執行 INCR，成功後再設定過期時間，確保操作順序
     */
    public Mono<Long> incrementDelta(String key, long delta, Duration expireTime) {
        return incrementDelta(key, delta).flatMap(incrementResult -> setExpire(key, expireTime).thenReturn(incrementResult));
    }


    /**
     * 對 Redis String 結構中的數值執行原子性自增操作，不設定過期時間。
     * <p>
     * 此方法提供了基本的 Redis 計數器功能，適用於需要永久保存的計數資料。
     * 是分散式系統中實現精確計數的基礎手段。
     * </p>
     * <p>
     * <strong>永久計數器的特性：</strong>
     * </p>
     * <ul>
     *   <li>計數值將持續保存直到手動刪除</li>
     *   <li>適合用於長期統計資料的累計</li>
     *   <li>不會因為過期而丟失重要的計數資訊</li>
     * </ul>
     * <p>
     * <strong>常用場景：</strong>
     * </p>
     * <ul>
     *   <li><strong>全域統計：</strong>累計總用戶數、總訂單數</li>
     *   <li><strong>序列號生成：</strong>生成全局唯一的業務 ID</li>
     *   <li><strong>版本計數：</strong>配置版本號、API 版本計數</li>
     * </ul>
     * <p>
     * <strong>與有過期時間版本的區別：</strong>
     * </p>
     * <ul>
     *   <li><strong>永久性：</strong>不會自動過期，需要手動管理生命週期</li>
     *   <li><strong>內存使用：</strong>需要注意大量累計後的內存占用</li>
     * </ul>
     * <p>
     * <strong>使用範例：</strong>
     * </p>
     * <pre>
     * // 生成唯一訂單號
     * incrementDelta("order_sequence", 1)
     *     .map(seq -&gt; "ORDER-" + String.format("%08d", seq))
     * 
     * // 累計用戶登入次數
     * incrementDelta("user:login_count:123", 1)
     * </pre>
     * 
     * @param key 計數器的鍵名，建議使用有意義的命名約定
     * @param delta 增量值，支援正數、負數和零
     * @return Mono&lt;Long&gt; 自增後的新值，可用於後續的業務處理
     * @see #incrementDelta(String, long, Duration)
     * @apiNote 適合用於需要永久保存的計數器場景
     * @implNote 直接使用 ReactiveRedisTemplate.opsForValue().increment() 實現
     */
    public Mono<Long> incrementDelta(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }


    /**
     * 在 Redis Hash 結構中設定指定字段的值，並為整個 Hash 設定隨機化過期時間。
     * <p>
     * 此方法提供了完整的 Hash 字段設定功能，包含過期時間管理。
     * 適用於需要定時失效的結構化資料存儲。
     * </p>
     * <p>
     * <strong>Hash 結構的特性：</strong>
     * </p>
     * <ul>
     *   <li>一個 Hash 可以包含多個字段，每個字段都有独立的值</li>
     *   <li>過期時間是設定在整個 Hash 上，而不是單一字段</li>
     *   <li>支援部分更新，不會影響其他字段的值</li>
     * </ul>
     * <p>
     * <strong>過期時間設定策略：</strong>
     * </p>
     * <ul>
     *   <li>如果過期時間為 null 或負值，則不設定過期</li>
     *   <li>過期時間會經過隨機化處理，防止緩存雪崩</li>
     *   <li>每次設定都會重新計算過期時間</li>
     * </ul>
     * <p>
     * <strong>典型應用場景：</strong>
     * </p>
     * <ul>
     *   <li><strong>用戶會話：</strong>存儲用戶的不同屬性（名稱、郵箱、偏好）</li>
     *   <li><strong>產品信息：</strong>存儲產品的各種屬性（名稱、價格、庫存）</li>
     *   <li><strong>配置管理：</strong>存儲應用程式的各種配置項</li>
     * </ul>
     * <p>
     * <strong>使用範例：</strong>
     * </p>
     * <pre>
     * // 存儲用戶會話資訊，1 小時後過期
     * setHashMap("user:session:123", "lastAccess", Instant.now(), Duration.ofHours(1))
     * setHashMap("user:session:123", "userId", 12345, null) // 不設過期
     * </pre>
     * 
     * @param hashKey Hash 的主鍵名，標識特定的 Hash 結構
     * @param innerKey Hash 內部的字段名，指定要設定的字段
     * @param value 要存儲的值，支援任何可序列化的對象
     * @param expireTime 過期時間，為 null 或負值時不設定過期
     * @return Mono&lt;Void&gt; 操作完成信號，成功時完成，失敗時發出異常
     * @see #setHashMap(String, String, Object)
     * @see #setExpire(String, Duration)
     * @apiNote 過期時間是對整個 Hash 生效，不是單一字段
     * @implNote 先設定字段值，成功後再設定過期時間
     */
    public Mono<Void> setHashMap(String hashKey, String innerKey, Object value, Duration expireTime) {
        if (expireTime == null || expireTime.isNegative()) {
            return setHashMap(hashKey, innerKey, value);
        }
        return redisTemplate.opsForHash().put(hashKey, innerKey, value).then(setExpire(hashKey, expireTime));
    }


    /**
     * 在 Redis Hash 結構中設定指定字段的值，不設定過期時間。
     * <p>
     * 此方法提供了基本的 Hash 字段設定功能，適用於需要永久保存的結構化資料。
     * 是實現物件屬性存儲和管理的理想選擇。
     * </p>
     * <p>
     * <strong>永久存儲的優勢：</strong>
     * </p>
     * <ul>
     *   <li>數據不會因過期而丟失，適合基礎配置資料</li>
     *   <li>減少過期管理的複雜度，操作更簡單</li>
     *   <li>適合用於頻繁訪問的靜態資料</li>
     * </ul>
     * <p>
     * <strong>操作特性：</strong>
     * </p>
     * <ul>
     *   <li>如果 Hash 不存在，會自動建立新的 Hash</li>
     *   <li>如果字段已存在，會覆蓋原有的值</li>
     *   <li>不會影響 Hash 中其他字段的值</li>
     * </ul>
     * <p>
     * <strong>常用場景：</strong>
     * </p>
     * <ul>
     *   <li><strong>用戶資料：</strong>存儲用戶的基本信息（名稱、郵箱、身份）</li>
     *   <li><strong>系統配置：</strong>存儲應用程式的各項配置參數</li>
     *   <li><strong>商品信息：</strong>存儲商品的各種屬性資料</li>
     * </ul>
     * <p>
     * <strong>注意事項：</strong>
     * </p>
     * <ul>
     *   <li>沒有過期時間的資料需要適當管理，避免內存溢出</li>
     *   <li>大量的 Hash 字段可能影響內存使用和性能</li>
     * </ul>
     * <p>
     * <strong>使用範例：</strong>
     * </p>
     * <pre>
     * // 設定用戶資料
     * setHashMap("user:123", "name", "John Doe")
     * setHashMap("user:123", "email", "john@example.com")
     * 
     * // 設定系統配置
     * setHashMap("config:app", "maxUsers", 1000)
     * </pre>
     * 
     * @param hashKey Hash 的主鍵名，標識特定的 Hash 結構
     * @param innerKey Hash 內部的字段名，指定要設定的字段
     * @param value 要存儲的值，支援任何可序列化的對象
     * @return Mono&lt;Void&gt; 操作完成信號，成功時完成，失敗時發出異常
     * @see #setHashMap(String, String, Object, Duration)
     * @see #setHashMapAll(String, Map)
     * @apiNote 此方法為永久存儲，不會自動過期
     * @implNote 使用 ReactiveRedisTemplate.opsForHash().put() 實現
     */
    public Mono<Void> setHashMap(String hashKey, String innerKey, Object value) {
        return redisTemplate.opsForHash().put(hashKey, innerKey, value).then();
    }


    /**
     * 將資料存入 Redis 的 Hash 中
     *
     * @param hashKey Hash 的鍵
     * @param value   Hash 內部的鍵和值
     *
     * @return 回傳 Mono<Void> 對象
     */
    public Mono<Void> setHashMapAll(String hashKey, Map<String, Object> value, Duration expireTime) {
        if (expireTime == null || expireTime.isNegative()) {
            return setHashMapAll(hashKey, value);
        }
        return redisTemplate.opsForHash().putAll(hashKey, value).then(setExpire(hashKey, expireTime));
    }


    /**
     * 將資料存入 Redis 的 Hash 中，此為批量設定
     *
     * @param hashKey Hash 的鍵
     * @param value   Hash 內部的鍵和值
     *
     * @return 回傳 Mono<Void> 對象
     */
    public Mono<Void> setHashMapAll(String hashKey, Map<String, Object> value) {
        return redisTemplate.opsForHash().putAll(hashKey, value).then();
    }


    /**
     * 根據 Hash 的鍵和內部的鍵獲取資料
     *
     * @param hashKey  Hash 的鍵
     * @param innerKey Hash 內部的鍵
     *
     * @return 回傳 Mono<Object> 對象
     */
    public Mono<Object> getHashMap(String hashKey, String innerKey) {
        return redisTemplate.opsForHash().get(hashKey, innerKey);
    }


    /**
     * 取得 Set 中的資料
     *
     * @param key   鍵
     * @param clazz 類型
     *
     * @return 回傳 Mono<T> 對象
     */
    public <T> Flux<T> getSet(String key, Class<T> clazz) {
        return redisTemplate.opsForSet().members(key).flatMap(object -> convertObjectList(object, clazz));
    }


    /**
     * 根據 Hash 的鍵和內部的鍵獲取資料，此適用於列表形式
     *
     * @param hashKey   Hash 的鍵
     * @param innerKeys Hash 內部的鍵的集合
     * @param clazz     類型
     *
     * @return 回傳 Mono<T> 對象
     */
    public <T> Flux<T> getHashMapList(String hashKey, List<String> innerKeys, Class<T> clazz) {
        List<Object> innerKeyList = innerKeys.stream().map(innerKey -> (Object) innerKey).toList();
        return redisTemplate.opsForHash().multiGet(hashKey, innerKeyList).flatMapMany(list -> {
            List<Object> filteredList = list.stream().filter(Objects::nonNull).toList();

            if (filteredList.isEmpty()) {
                return Flux.empty();
            }

            if (clazz.isInstance(filteredList.getFirst())) {
                return Flux.fromIterable(filteredList).cast(clazz);
            }
            return Flux.fromIterable(filteredList).map(object -> objectMapper.convertValue(object, clazz));
        });
    }


    /**
     * 獲取指定Key 中的所有資料
     *
     * @param hashKey    Hash 的鍵
     * @param KeyClass   Key 的類型
     * @param ValueClass Value 的類型
     *
     * @return 回傳 Flux<Map.Entry<K, V>> 對象
     */
    public <K, V> Flux<Map.Entry<K, V>> getAllHashMap(String hashKey, Class<K> KeyClass, Class<V> ValueClass) {
        return redisTemplate.opsForHash().entries(hashKey).map(entry -> {
            Object key = entry.getKey();
            Object value = entry.getValue();
            if (KeyClass.isInstance(key) && ValueClass.isInstance(value)) {
                return Map.entry(KeyClass.cast(key), ValueClass.cast(value));
            }
            return Map.entry(objectMapper.convertValue(key, KeyClass), objectMapper.convertValue(value, ValueClass));
        });
    }


    /**
     * 對 Hash 中的資料進行自增操作
     *
     * @param hashKey  Hash 的鍵
     * @param innerKey Hash 內部的鍵
     * @param delta    自增的數值
     *
     * @return 回傳 Mono<Long> 增加後的值
     */
    public Mono<Long> incrementHashMap(String hashKey, String innerKey, long delta) {
        return redisTemplate.opsForHash().increment(hashKey, innerKey, delta);
    }


    /**
     * 對 Hash 中的資料進行自增操作，並設定過期時間
     *
     * @param hashKey    Hash 的鍵
     * @param innerKey   Hash 內部的鍵
     * @param delta      自增的數值
     * @param expireTime 過期時間
     *
     * @return 回傳 Mono<Long> 增加後的值
     */
    public Mono<Long> incrementHashMap(String hashKey, String innerKey, long delta, Duration expireTime) {
        return redisTemplate
                .opsForHash()
                .increment(hashKey, innerKey, delta)
                .flatMap(incrementResult -> setExpire(hashKey, expireTime).thenReturn(incrementResult));
    }


    /**
     * 獲取 HashMap 中的查詢Key的所有資料
     *
     * @param hashKey Hash 的鍵
     *
     * @return 回傳 Flux<Object> 對象
     */
    public Flux<Object> getHashMapAll(String hashKey) {
        return redisTemplate.opsForHash().values(hashKey);
    }


    /**
     * 獲取 HashMap 中的查詢Key的所有資料
     *
     * @param hashKey Hash 的鍵
     * @param clazz   類型
     *
     * @return 回傳 Flux<T> 對象
     */
    public <T> Flux<T> getHashMapAll(String hashKey, Class<T> clazz) {
        return redisTemplate.opsForHash().values(hashKey).cast(clazz);
    }


    /**
     * 依照通配符獲取 HashMap 中的資料
     * 其中，pattern 為通配符，clazz 為資料的類型
     *
     * @param hashKey Hash 的鍵
     * @param pattern 通配符
     * @param clazz   類型
     * @param <T>     泛型
     *
     * @return 回傳 Flux<Map.Entry<String, T>> 對象，其中 Key 為 Hash 的內部鍵，Value 為 Hash 的內部值
     */
    public <T> Flux<Map.Entry<String, T>> getHashMapByPattern(String hashKey, String pattern, Class<T> clazz) {
        ScanOptions patternOptions = ScanOptions.scanOptions().match(pattern).build();
        return redisTemplate.opsForHash().scan(hashKey, patternOptions).flatMap(entry -> {
            Object key = entry.getKey();
            Object value = entry.getValue();
            if (key == null || value == null) {
                return Flux.empty();
            }
            return Flux.just(Map.entry(key.toString(), objectMapper.convertValue(value, clazz)));
        });
    }


    /**
     * 刪除 Hash 中指定外部Key中內部Key的資料
     *
     * @param key      Hash 的鍵
     * @param innerKey Hash 內部的鍵
     *
     * @return 回傳 Mono <Long> 刪除的資料數量
     */
    public Mono<Long> deleteHash(String key, String innerKey) {
        return redisTemplate.opsForHash().remove(key, innerKey);
    }


    /**
     * 刪除 Hash 中指定外部Key中內部Key的資料，此為批量刪除
     * 當 innerKey 為空時，則不進行操作
     *
     * @param key      Hash 的鍵
     * @param innerKey Hash 內部的鍵的列表
     *
     * @return 回傳 Mono<Long> 刪除的資料數量
     */
    public Mono<Long> deleteHash(String key, List<String> innerKey) {
        if (innerKey.isEmpty()) {
            return Mono.empty();
        }
        return redisTemplate.opsForHash().remove(key, innerKey.toArray());
    }


    /**
     * 刪除 Hash 指定Key中的所有資料
     *
     * @param key Hash 的鍵
     *
     * @return 回傳 Mono<Boolean> 是否刪除成功
     */
    public Mono<Boolean> deleteHash(String key) {
        return redisTemplate.opsForHash().delete(key);
    }


    /**
     * 將資料存入 Redis 的 Set 中
     *
     * @param key        鍵
     * @param value      值
     * @param expireTime 過期時間
     *
     * @return 回傳 Mono<Void> 對象
     */
    public Mono<Void> setSet(String key, Object value, Duration expireTime) {
        if (expireTime == null || expireTime.isNegative()) {
            return setSet(key, value);
        }
        return redisTemplate.opsForSet().add(key, value).then(setExpire(key, expireTime));
    }


    /**
     * 將資料存入 Redis 的 Set 中
     *
     * @param key   鍵
     * @param value 值
     *
     * @return 回傳 Mono<Void> 對象
     */
    public Mono<Void> setSet(String key, Object value) {
        return redisTemplate.opsForSet().add(key, value).then();
    }


    /**
     * 取得 Set 中的資料
     *
     * @param key 鍵
     *
     * @return 回傳 Mono<Void> 對象
     */
    public Flux<Object> getSet(String key) {
        return redisTemplate.opsForSet().members(key);
    }


    /**
     * 根據需要刪除的數值，刪除 Set 中的資料
     *
     * @param key   鍵
     * @param value 值
     *
     * @return 回傳 Mono<Long> 刪除的資料數量
     */
    public Mono<Long> deleteSet(String key, Object value) {
        return redisTemplate.opsForSet().remove(key, value);
    }


    /**
     * 刪除 Set 中的資料
     *
     * @param key 鍵
     *
     * @return 回傳 Mono<Boolean> 是否刪除成功
     */
    public Mono<Boolean> deleteSet(String key) {
        return redisTemplate.opsForSet().delete(key);
    }


    /**
     * 將資料存入 Redis 的 List 中，並設定過期時間
     *
     * @param key        鍵
     * @param value      值
     * @param expireTime 過期時間
     *
     * @return 回傳 Mono<Void> 對象
     */
    public Mono<Void> setList(String key, Object value, Duration expireTime) {
        if (expireTime == null || expireTime.isNegative()) {
            return setList(key, value);
        }
        return redisTemplate.opsForList().rightPush(key, value).then(setExpire(key, expireTime));
    }


    /**
     * 插入額外的資料到 Set 中
     *
     * @param key   鍵
     * @param value 值
     *
     * @return 回傳 Mono<Void> 對象
     */
    public Mono<Void> setList(String key, Object value) {
        return redisTemplate.opsForList().rightPush(key, value).then();
    }


    /**
     * 獲取 List 中的資料
     *
     * @param key   鍵
     * @param clazz 類型
     *
     * @return 回傳 Flux<T> 對象
     */
    public <T> Flux<T> getList(String key, Class<T> clazz) {
        return getList(key).cast(clazz);
    }


    /**
     * 獲取 List 中的資料
     *
     * @param key 鍵
     *
     * @return 回傳 Flux<Object> 對象
     */
    public Flux<Object> getList(String key) {
        return getList(key, 0, -1);
    }


    /**
     * 獲取 List 中的資料
     *
     * @param key   鍵
     * @param start 起始序號
     * @param end   結束序號
     *
     * @return 回傳 Flux<Object> 對象
     */
    public Flux<Object> getList(String key, long start, long end) {
        return redisTemplate.opsForList().range(key, start, end);
    }


    /**
     * 獲取 List 中的資料
     *
     * @param key   鍵
     * @param start 起始序號
     * @param end   結束序號
     * @param clazz 類型
     *
     * @return 回傳 Flux<T> 對象
     */
    public <T> Flux<T> getListContent(String key, long start, long end, Class<T> clazz) {
        return redisTemplate.opsForList().range(key, start, end).flatMap(object -> convertObjectList(object, clazz)).switchIfEmpty(Flux.empty());
    }


    /**
     * 將資料存入 Redis 的 List 中，並設定過期時間
     *
     * @param key        鍵
     * @param value      值
     * @param isLeft     是否從左邊插入
     * @param expireTime 過期時間
     *
     * @return 回傳 Mono<Void> 對象
     */
    public Mono<Void> insertList(String key, Object value, Boolean isLeft, Duration expireTime) {
        if (expireTime == null || expireTime.isNegative()) {
            return insertList(key, value, isLeft);
        }
        return insertList(key, value, isLeft).then(setExpire(key, expireTime));
    }


    /**
     * 將資料存入 Redis 的 List 中
     *
     * @param key    鍵
     * @param value  值
     * @param isLeft 是否從左邊插入
     *
     * @return 回傳 Mono<Void> 對象
     */
    public Mono<Void> insertList(String key, Object value, Boolean isLeft) {
        if (isLeft) {
            return redisTemplate.opsForList().leftPush(key, value).then();
        }
        return redisTemplate.opsForList().rightPush(key, value).then();
    }


    /**
     * 根據目標數值，刪除 List 中的資料
     *
     * @param key   鍵
     * @param value 值
     *
     * @return 回傳 Mono<Long> 刪除的資料數量
     */
    public Mono<Long> deleteList(String key, Object value) {
        return redisTemplate.opsForList().remove(key, 1, value);
    }


    /**
     * 刪除 List 中的資料
     *
     * @param key 鍵
     *
     * @return 回傳 Mono<Boolean> 是否刪除成功
     */
    public Mono<Boolean> deleteList(String key) {
        return redisTemplate.opsForList().delete(key);
    }


    /**
     * 新增資料到 Redis 的 Zset 中，並設定過期時間
     *
     * @param key        鍵
     * @param value      值
     * @param score      序號
     * @param expireTime 過期時間
     *
     * @return 回傳 Mono<Void> 對象
     */
    public Mono<Void> setZset(String key, Object value, double score, Duration expireTime) {
        if (expireTime == null || expireTime.isNegative()) {
            return setZset(key, value, score);
        }
        return setZset(key, value, score).then(setExpire(key, expireTime));
    }


    /**
     * 新增資料到 Redis 的 Zset 中
     *
     * @param key   鍵
     * @param value 值
     * @param score 序號
     *
     * @return 回傳 Mono<Void> 對象
     */
    public Mono<Void> setZset(String key, Object value, double score) {
        return redisTemplate.opsForZSet().add(key, value, score).then();
    }


    /**
     * 獲取 Zset 中的資料
     *
     * @param key 鍵
     *
     * @return 回傳 Flux<Object> 對象
     */
    public Flux<Object> getZset(String key) {
        return redisTemplate.opsForZSet().range(key, Range.from(Range.Bound.inclusive(0L)).to(Range.Bound.unbounded()));
    }


    /**
     * 獲取 Zset 中的資料
     *
     * @param key   鍵
     * @param start 起始序號
     * @param end   結束序號
     *
     * @return 回傳 Flux<Object> 對象
     */
    public Flux<Object> getZset(String key, long start, long end) {
        return redisTemplate.opsForZSet().range(key, Range.from(Range.Bound.inclusive(start)).to(Range.Bound.inclusive(end)));
    }


    /**
     * 獲取 Zset 中的資料並轉換成分頁響應
     *
     * @param key   鍵
     * @param page  頁數
     * @param clazz 類型
     *
     * @return 回傳 Mono<PagedResponseDTO<T>> 對象
     */
    @HideOverLength
    public <T> Mono<PagedResponseDTO<T>> getPagedResponseFromZset(String key, int page, Class<T> clazz) {
        return redisTemplate.opsForZSet().rangeByScore(key, Range.just((double) page)).next().flatMap(obj -> {
            JavaType type = objectMapper.getTypeFactory().constructParametricType(PagedResponseDTO.class, clazz);
            PagedResponseDTO<T> pagedResponseDTO = objectMapper.convertValue(obj, type);
            return Mono.just(pagedResponseDTO);
        });
    }


    /**
     * 獲取 Zset 中的資料，並轉換成列表
     *
     * @param key   鍵
     * @param page  頁數
     * @param clazz 類型
     *
     * @return 回傳 Flux<T> 對象
     */
    @HideOverLength
    public <T> Flux<T> getListFromZset(String key, int page, Class<T> clazz) {
        return redisTemplate.opsForZSet().rangeByScore(key, Range.just((double) page)).next().flatMapMany(object -> convertObjectList(object, clazz));
    }


    /**
     * 根據目標數值，刪除 Zset 中的資料
     *
     * @param key   鍵
     * @param value 值
     *
     * @return 回傳 Mono<Long> 刪除的資料數量
     */
    public Mono<Long> deleteZset(String key, Object... value) {
        return redisTemplate.opsForZSet().remove(key, value);
    }


    /**
     * 刪除 Zset 中的資料
     *
     * @param key 鍵
     *
     * @return 回傳 Mono<Long> 刪除的資料數量
     */
    public Mono<Long> deleteZset(String key) {
        return redisTemplate.opsForZSet().removeRange(key, Range.unbounded());
    }


    /**
     * 根據目標數值，刪除 Zset 中的資料
     *
     * @param key   鍵
     * @param start 起始序號
     * @param end   結束序號
     *
     * @return 回傳 Mono<Long> 刪除的資料數量
     */
    public Mono<Long> deleteZset(String key, long start, long end) {
        return redisTemplate.opsForZSet().removeRange(key, Range.from(Range.Bound.inclusive(start)).to(Range.Bound.inclusive(end)));
    }


    /**
     * 生成一個分塊集合，用於標記分塊的完成情況
     *
     * @param key         鍵
     * @param totalChunks 分塊數量
     *
     * @return 回傳 Mono<Void> 對象
     */
    public Mono<Void> generateChunkSet(String key, int totalChunks) {
        return Flux.range(1, totalChunks).flatMap(index -> setSet(key, index)).then();
    }


    /**
     * 刪除 Redis 中的資料，此刪除方法會檢查所有的資料類型找出對應的鍵並刪除
     *
     * @param key 鍵
     *
     * @return 回傳 Mono<Void> 對象
     */
    public Mono<Long> delete(String key) {
        return redisTemplate.delete(key);
    }


    /**
     * 依照通配符刪除 Redis 中的資料
     *
     * @param pattern 通配符
     *
     * @return 回傳 Mono<Void> 對象
     */
    public Mono<Long> deleteByPattern(String pattern) {
        return redisTemplate.keys(pattern).collectList().flatMap(keys -> redisTemplate.delete(keys.toArray(String[]::new)));
    }


    /**
     * 確認分塊是否尚未完成
     *
     * @param key        鍵
     * @param chunkIndex 分塊序號
     *
     * @return 回傳 Mono<Boolean> 對象
     */
    public Mono<Boolean> isChunkSetPending(String key, int chunkIndex) {
        return redisTemplate.opsForSet().isMember(key, chunkIndex);
    }
}
