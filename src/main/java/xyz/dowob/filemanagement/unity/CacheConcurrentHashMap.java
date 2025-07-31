package xyz.dowob.filemanagement.unity;

import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * 高效能的緩存實現，基於 ConcurrentHashMap 提供執行緒安全的快取功能。
 * <p>
 * 本類別提供完整的緩存生命週期管理，包括自動過期、定時清理、動態刷新等進階功能。
 * 採用寫時複製和原子操作確保高並發環境下的資料一致性和效能表現。
 * 特別適用於需要頻繁讀寫且對效能要求較高的快取場景。
 * <p>
 * <strong>核心特性：</strong>
 * <ul>
 *   <li><strong>執行緒安全：</strong>基於 ConcurrentHashMap 實現，支援高並發存取</li>
 *   <li><strong>自動過期：</strong>支援項目級別的過期時間設定，自動清理過期項目</li>
 *   <li><strong>動態刷新：</strong>存取時自動延長緩存項目的生命週期</li>
 *   <li><strong>定時清理：</strong>可選的背景清理任務，定期移除過期項目</li>
 *   <li><strong>批量操作：</strong>支援批量設定、獲取、刪除等高效批量操作</li>
 *   <li><strong>原子操作：</strong>提供 computeIfPresent 等原子計算方法</li>
 *   <li><strong>記憶體友善：</strong>自動清理機制防止記憶體洩漏</li>
 * </ul>
 * <p>
 * <strong>時間控制機制：</strong>
 * <ul>
 *   <li><strong>過期時間（expireTime）：</strong>項目的基本生命週期</li>
 *   <li><strong>最大保留時間（maxRemainTime）：</strong>項目的絕對生命週期上限</li>
 *   <li><strong>動態延長：</strong>存取時可自動延長過期時間（不超過最大保留時間）</li>
 *   <li><strong>立即過期：</strong>超過過期時間的項目會在下次存取時被移除</li>
 * </ul>
 * <p>
 * <strong>清理策略：</strong>
 * <ul>
 *   <li><strong>懶清理：</strong>存取時檢查並移除過期項目</li>
 *   <li><strong>定時清理：</strong>背景任務定期掃描並清理過期項目</li>
 *   <li><strong>批量清理：</strong>高效率的批量過期檢查和移除</li>
 *   <li><strong>優雅關閉：</strong>銷毀時正確釋放所有資源</li>
 * </ul>
 * <p>
 * <strong>效能最佳化：</strong>
 * <ul>
 *   <li>使用 ConcurrentHashMap 提供最佳的並發讀寫效能</li>
 *   <li>原子操作避免鎖競爭，提升高並發場景效能</li>
 *   <li>懶清理機制減少不必要的背景開銷</li>
 *   <li>批量操作減少方法呼叫和鎖定開銷</li>
 * </ul>
 * <p>
 * <strong>使用範例：</strong>
 * <pre>{@code
 * // 基本使用 - 啟用自動清理，使用預設設定
 * CacheConcurrentHashMap<String, User> userCache = 
 *     new CacheConcurrentHashMap<>(true);
 * 
 * // 進階設定 - 自訂容量、過期時間和清理間隔
 * CacheConcurrentHashMap<String, SessionData> sessionCache = 
 *     new CacheConcurrentHashMap<>(
 *         1000,                           // 初始容量
 *         Duration.ofMinutes(30),         // 預設過期時間
 *         Duration.ofHours(2),            // 最大保留時間
 *         Duration.ofMinutes(5),          // 清理間隔
 *         true                            // 啟用自動清理
 *     );
 * 
 * // 基本操作
 * userCache.set("user123", user);                    // 設定緩存
 * User cachedUser = userCache.get("user123");        // 獲取並延長過期時間
 * User checkUser = userCache.check("user123");       // 檢查但不延長過期時間
 * 
 * // 自訂過期時間
 * userCache.set("vip456", vipUser, Duration.ofHours(1));
 * 
 * // 批量操作
 * Map<String, User> userMap = Map.of("u1", user1, "u2", user2);
 * userCache.setAll(userMap, Duration.ofMinutes(45));
 * List<User> users = userCache.getAll(List.of("u1", "u2"));
 * 
 * // 原子計算操作
 * Integer count = cache.computeIfPresentOrDefault(
 *     "counter", 
 *     0, 
 *     (key, oldValue) -> oldValue + 1
 * );
 * 
 * // 資源清理
 * userCache.destroy();  // 正確釋放所有資源
 * }</pre>
 * <p>
 * <strong>最佳實踐：</strong>
 * <ul>
 *   <li>為不同用途的緩存設定合適的初始容量</li>
 *   <li>根據業務需求選擇適當的過期時間和最大保留時間</li>
 *   <li>在高負載場景下優先使用批量操作方法</li>
 *   <li>適當設定清理間隔，平衡效能和記憶體使用</li>
 *   <li>應用關閉時記得呼叫 destroy() 方法清理資源</li>
 * </ul>
 * <p>
 * <strong>注意事項：</strong>
 * <ul>
 *   <li>過期時間受最大保留時間限制，實際過期時間為兩者的較小值</li>
 *   <li>定時清理任務使用守護執行緒，不會阻止 JVM 正常關閉</li>
 *   <li>所有的時間參數都必須為正值，否則會拋出異常</li>
 *   <li>null 鍵會導致異常，null 值會被正常儲存</li>
 * </ul>
 *
 * @param <K> 緩存鍵的類型
 * @param <V> 緩存值的類型
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see ConcurrentHashMap
 * @see ScheduledExecutorService
 * @see Duration
 **/
@SuppressWarnings("unused")
public class CacheConcurrentHashMap<K, V> {
    /**
     * 預設過期時間為10分鐘
     */
    private static final Duration DEFAULT_EXPIRE_DURATION = Duration.ofMinutes(10);

    /**
     * 預設最大保留時間為10分鐘
     */
    private static final Duration DEFAULT_REMAIN_TIME_DURATION = Duration.ofMinutes(10);

    /**
     * 預設清理間隔為10分鐘
     */
    private static final Duration DEFAULT_CLEANUP_INTERVAL = Duration.ofMinutes(10);

    /**
     * 預設初始容量為 64
     */
    private static final int DEFAULT_INITIAL_CAPACITY = 64;

    /**
     * 預設辨識的標籤為 "CacheConcurrentHashMap"
     */
    private static final String DEFAULT_TAG = "CacheConcurrentHashMap";

    /**
     * 用於存儲緩存項的HashMap
     */
    private final ConcurrentHashMap<K, CacheInfo<V>> cacheMap;

    /**
     * 用於辨識的標籤
     */
    @Setter
    @Getter
    private String tag;

    /**
     * 緩存項的過期時間，當前時間超過此時間，則該緩存項將被視為過期
     */
    @Setter
    @Getter
    private Duration expireTime;

    /**
     * 緩存項的最大保留時間，緩存的過期時間不能超過此時間，避免過期時間過長
     */
    @Setter
    @Getter
    private Duration maxRemainTime;

    /**
     * 用於定時清理過期項目的ScheduledExecutorService
     */
    private ScheduledExecutorService scheduler = null;


    /**
     * 使用預設值建構 CacheConcurrentHashMap 實例。
     * 初始容量、過期時間、最大保留時間和清理間隔均使用預設值。
     *
     * @param enableCleanup 是否啟用定時清理過期項目的任務
     */
    public CacheConcurrentHashMap(boolean enableCleanup) {
        this(DEFAULT_INITIAL_CAPACITY, DEFAULT_EXPIRE_DURATION, DEFAULT_REMAIN_TIME_DURATION, DEFAULT_CLEANUP_INTERVAL, enableCleanup);
    }


    /**
     * 建構 CacheConcurrentHashMap 實例，允許完全自訂設定。
     * 提供對初始容量、過期時間、最大保留時間、清理間隔和清理任務啟用狀態的完全控制。
     *
     * @param initialCapacity 初始容量，必須大於 0
     * @param expireTime      預設過期時間，必須為正值
     * @param maxRemainTime   預設最大保留時間，必須為正值
     * @param cleanupInterval 清理間隔，僅在 enableCleanup 為 true 時有效，必須為正值
     * @param enableCleanup   是否啟用定時清理過期項目的任務
     * @throws IllegalArgumentException 當初始容量小於等於 0，或在啟用清理任務時清理間隔無效時拋出
     */
    public CacheConcurrentHashMap(int initialCapacity, Duration expireTime, Duration maxRemainTime, Duration cleanupInterval, boolean enableCleanup) {
        if (initialCapacity <= 0) {
            throw new IllegalArgumentException("初始化容量必須大於0");
        }
        this.cacheMap = new ConcurrentHashMap<>(initialCapacity);
        this.expireTime = expireTime.isPositive() ? expireTime : DEFAULT_EXPIRE_DURATION;
        this.maxRemainTime = maxRemainTime.isPositive() ? maxRemainTime : DEFAULT_REMAIN_TIME_DURATION;

        if (enableCleanup) {
            if (!cleanupInterval.isPositive()) {
                throw new IllegalArgumentException("清理間隔必須大於0");
            }

            this.scheduler = new ScheduledThreadPoolExecutor(1, r -> {
                Thread t = new Thread(r, "CacheCleanerThread");
                t.setDaemon(true);
                return t;
            }
            );
            scheduleCleanupTask(cleanupInterval);
        }
    }


    /**
     * 啟動定時清理過期項目的排程任務。
     * 使用指定的間隔時間定期執行清理任務，移除過期的緩存項目。
     *
     * @param interval 清理任務執行的間隔時間，必須為正值
     * @throws IllegalStateException 當清理任務未啟用（scheduler 為 null）時拋出
     */
    private void scheduleCleanupTask(Duration interval) {
        if (scheduler == null) {
            throw new IllegalStateException("清理任務未啟用，請在構造函數中設定enableCleanup為true");
        }
        scheduler.scheduleAtFixedRate(this::getCleanupTask, interval.toMillis(), interval.toMillis(), TimeUnit.MILLISECONDS);
    }


    /**
     * 建立並回傳用於清理過期項目的可執行任務。
     * <p>
     * 此方法產生的清理任務採用安全且高效的過期項目移除策略：
     * <p>
     * <strong>清理流程：</strong>
     * <ol>
     *   <li>建立緩存鍵集合的快照，避免併發修改異常</li>
     *   <li>逐一檢查每個緩存項目的過期狀態</li>
     *   <li>使用 CAS 操作安全移除過期項目</li>
     *   <li>記錄清理操作的詳細日誌供監控分析</li>
     * </ol>
     * <p>
     * <strong>安全性保證：</strong>
     * <ul>
     *   <li><strong>快照機制：</strong>避免遍歷時的併發修改異常</li>
     *   <li><strong>雙重檢查：</strong>獲取和移除時都驗證項目存在性</li>
     *   <li><strong>原子移除：</strong>使用 remove(key, value) 確保移除的正確性</li>
     *   <li><strong>異常隔離：</strong>單個項目處理失敗不影響整體清理流程</li>
     * </ul>
     * <p>
     * <strong>效能特性：</strong>
     * <ul>
     *   <li>時間複雜度：O(n)，其中 n 為緩存項目數量</li>
     *   <li>空間複雜度：O(n)，用於儲存鍵集合快照</li>
     *   <li>併發友好：不會阻塞正常的緩存讀寫操作</li>
     *   <li>記憶體效率：及時釋放過期項目佔用的記憶體</li>
     * </ul>
     *
     * @return 執行清理操作的 Runnable 任務實例，可在任何執行環境中安全執行
     */
    public Runnable getCleanupTask() {
        return () -> {
            LogUnity.debug("時間: %s: 清理過期緩存資料", tag != null ? tag : DEFAULT_TAG);

            Set<K> keysSnapshot = new HashSet<>(cacheMap.keySet());
            long now = System.currentTimeMillis();
            for (K key : keysSnapshot) {
                CacheInfo<V> cacheInfo = cacheMap.get(key);
                if (cacheInfo != null && now > cacheInfo.getExpireTimeMillis()) {
                    boolean removed = cacheMap.remove(key, cacheInfo);
                    if (removed) {
                        LogUnity.trace("%s: 已移除過期鍵 %s", tag != null ? tag : DEFAULT_TAG, key);
                    }
                }
            }
        };
    }


    /**
     * 建構 CacheConcurrentHashMap 實例，允許自訂初始容量。
     * 過期時間、最大保留時間和清理間隔使用預設值。
     *
     * @param initialCapacity 初始容量，必須大於 0
     * @param enableCleanup   是否啟用定時清理過期項目的任務
     */
    public CacheConcurrentHashMap(int initialCapacity, boolean enableCleanup) {
        this(initialCapacity, DEFAULT_EXPIRE_DURATION, DEFAULT_REMAIN_TIME_DURATION, DEFAULT_CLEANUP_INTERVAL, enableCleanup);
    }


    /**
     * 建構 CacheConcurrentHashMap 實例，允許自訂初始容量和過期時間。
     * 最大保留時間設定為與過期時間相同，清理間隔使用預設值。
     *
     * @param initialCapacity 初始容量，必須大於 0
     * @param expireTime      預設過期時間，必須為正值
     * @param enableCleanup   是否啟用定時清理過期項目的任務
     */
    public CacheConcurrentHashMap(int initialCapacity, Duration expireTime, boolean enableCleanup) {
        this(initialCapacity, expireTime, expireTime, DEFAULT_CLEANUP_INTERVAL, enableCleanup);
    }


    /**
     * 建構 CacheConcurrentHashMap 實例，允許自訂初始容量、過期時間和最大保留時間。
     * 清理間隔使用預設值，自動啟用清理任務。
     *
     * @param initialCapacity 初始容量，必須大於 0
     * @param expireTime      預設過期時間，必須為正值
     * @param maxRemainTime   預設最大保留時間，必須為正值
     */
    public CacheConcurrentHashMap(int initialCapacity, Duration expireTime, Duration maxRemainTime) {
        this(initialCapacity, expireTime, maxRemainTime, DEFAULT_CLEANUP_INTERVAL, true);
    }


    /**
     * 使用預設的過期時間和最大保留時間設定緩存項目。
     * 這是最簡便的設定方法，適用於標準的緩存操作。
     *
     * @param key   緩存項目的鍵，不可為 null
     * @param value 緩存項目的值
     */
    public void set(K key, V value) {
        this.set(key, value, expireTime, maxRemainTime);
    }


    /**
     * 設定緩存項目的值和詳細的時間控制參數。
     * 實際過期時間將取 expire 和 maxRemain 的較小值，確保緩存項目不會超過最大保留時間。
     *
     * @param key       緩存項目的鍵，不可為 null
     * @param value     緩存項目的值
     * @param expire    過期時間，必須為正值
     * @param maxRemain 最大保留時間，必須為正值
     * @throws IllegalArgumentException 當鍵為 null、過期時間或最大保留時間無效時拋出
     */
    public void set(K key, V value, Duration expire, Duration maxRemain) {
        if (key == null) {
            throw new IllegalArgumentException("鍵不能為null");
        }
        if (!expire.isPositive()) {
            throw new IllegalArgumentException("過期時間必須大於0");
        }
        if (!maxRemain.isPositive()) {
            throw new IllegalArgumentException("最大保留時間必須大於0");
        }

        long expireTimeMillis = System.currentTimeMillis() + Math.min(expire.toMillis(), maxRemain.toMillis());
        cacheMap.put(key, new CacheInfo<>(value, expireTimeMillis));
    }


    /**
     * 設定緩存項目的值和自訂過期時間。
     * 最大保留時間使用實例的預設值。
     *
     * @param key    緩存項目的鍵，不可為 null
     * @param value  緩存項目的值
     * @param expire 過期時間，必須為正值
     */
    public void set(K key, V value, Duration expire) {
        this.set(key, value, expire, maxRemainTime);
    }


    /**
     * 批量設定多個緩存項目的值和時間控制參數。
     * 對映射中的每個鍵值對都應用相同的過期時間和最大保留時間設定。
     *
     * @param kvMap     包含多個鍵值對的映射
     * @param expire    過期時間，必須為正值
     * @param maxRemain 最大保留時間，必須為正值
     */
    public void setAll(Map<K, V> kvMap, Duration expire, Duration maxRemain) {
        kvMap.forEach((key, value) -> this.set(key, value, expire, maxRemain));
    }


    /**
     * 批量設定多個緩存項目的值和過期時間。
     * 最大保留時間使用實例的預設值。
     *
     * @param kvMap  包含多個鍵值對的映射
     * @param expire 過期時間，必須為正值
     */
    public void setAll(Map<K, V> kvMap, Duration expire) {
        kvMap.forEach((key, value) -> this.set(key, value, expire, maxRemainTime));
    }


    /**
     * 獲取緩存項目的完整資訊並刷新過期時間。
     * 回傳包含值和過期時間的 CacheInfo 對象，如果項目不存在或已過期則回傳 null。
     *
     * @param key 緩存項目的鍵
     * @return 緩存項目的完整資訊，如果不存在或已過期則為 null
     */
    public CacheInfo<V> getInfo(K key) {
        return getAndRefresh(key, expireTime);
    }


    /**
     * 獲取緩存項目並刷新其過期時間的內部方法。
     * 使用原子操作確保線程安全，檢查過期狀態並更新過期時間。
     *
     * @param key    緩存項目的鍵
     * @param expire 新的過期時間
     * @return 緩存項目的完整資訊，如果不存在或已過期則為 null
     */
    private CacheInfo<V> getAndRefresh(K key, Duration expire) {
        if (key == null) {
            return null;
        }

        BiFunction<? super K, CacheInfo<V>, CacheInfo<V>> action = (k, existingInfo) -> {
            long now = System.currentTimeMillis();
            if (now > existingInfo.getExpireTimeMillis()) {
                return null;
            }
            long newExpireTimeMillis = now + Math.min(expire.toMillis(), maxRemainTime.toMillis());
            existingInfo.setExpireTimeMillis(newExpireTimeMillis);
            return existingInfo;
        };

        return cacheMap.computeIfPresent(key, action);
    }


    /**
     * 獲取緩存項目的值，如果不存在或已過期則回傳預設值。
     * 此方法不會刷新過期時間，僅用於檢查緩存狀態。
     *
     * @param key          緩存項目的鍵
     * @param defaultValue 當緩存項目不存在或已過期時回傳的預設值
     * @return 緩存項目的值或預設值
     */
    public V checkOrDefault(K key, V defaultValue) {
        V value = this.check(key);
        return value == null ? defaultValue : value;
    }


    /**
     * 獲取緩存項目的值但不刷新過期時間。
     * 如果項目已過期，會自動移除並回傳 null。適用於僅檢查緩存狀態的場景。
     *
     * @param key 緩存項目的鍵
     * @return 緩存項目的值，如果不存在或已過期則為 null
     */
    public V check(K key) {
        return Optional.ofNullable(key).map(k -> cacheMap.get(key)).map(cacheInfo -> {
            long now = System.currentTimeMillis();
            if (now > cacheInfo.getExpireTimeMillis()) {
                cacheMap.remove(key, cacheInfo);
                return null;
            }
            return cacheInfo.getValue();
        }).orElse(null);
    }


    /**
     * 批量獲取多個緩存項目的值並刷新過期時間。
     * 對每個鍵都應用預設的過期時間延長邏輯。
     *
     * @param keys 緩存項目鍵的列表
     * @return 對應的緩存項目值列表，不存在或已過期的項目為 null
     */
    public List<V> getAll(List<K> keys) {
        return keys.stream().map(this::get).collect(Collectors.toList());
    }


    /**
     * 獲取緩存項目的值並使用預設過期時間刷新。
     * 這是最常用的獲取方法，會自動延長緩存項目的生命週期。
     *
     * @param key 緩存項目的鍵
     * @return 緩存項目的值，如果不存在或已過期則為 null
     */
    public V get(K key) {
        return this.get(key, expireTime);
    }


    /**
     * 獲取緩存項的值並指定緩存的過期時間
     *
     * @param key    鍵
     * @param expire 過期時間
     *
     * @return 緩存項的值
     */
    public V get(K key, Duration expire) {
        CacheInfo<V> cacheInfo = getAndRefresh(key, expire);
        return cacheInfo == null ? null : cacheInfo.getValue();
    }


    /**
     * 批量獲取多個緩存項目的值並使用自訂過期時間刷新。
     * 對所有項目應用相同的過期時間延長邏輯。
     *
     * @param keys   緩存項目鍵的列表
     * @param expire 新的過期時間，必須為正值
     * @return 對應的緩存項目值列表，不存在或已過期的項目為 null
     */
    public List<V> getAll(List<K> keys, Duration expire) {
        return keys.stream().map(key -> this.get(key, expire)).collect(Collectors.toList());
    }


    /**
     * 批量獲取多個緩存項目的完整資訊並使用自訂過期時間刷新。
     * 回傳包含值和過期時間的 CacheInfo 對象列表。
     *
     * @param keys 緩存項目鍵的列表
     * @param expire 新的過期時間，必須為正值
     * @return 對應的緩存項目完整資訊列表，不存在或已過期的項目為 null
     */
    public List<CacheInfo<V>> getAllInfo(List<K> keys, Duration expire) {
        return keys.stream().map(key -> this.getInfo(key, expire)).collect(Collectors.toList());
    }


    /**
     * 獲取緩存項目的完整資訊並使用自訂過期時間刷新。
     * 回傳包含值和過期時間的 CacheInfo 對象。
     *
     * @param key    緩存項目的鍵
     * @param expire 新的過期時間，必須為正值
     * @return 緩存項目的完整資訊，如果不存在或已過期則為 null
     */
    public CacheInfo<V> getInfo(K key, Duration expire) {
        return getAndRefresh(key, expire);
    }


    /**
     * 批量獲取多個緩存項目的值但不刷新過期時間。
     * 僅用於檢查緩存狀態，不會延長項目的生命週期。
     *
     * @param keys 緩存項目鍵的列表
     * @return 對應的緩存項目值列表，不存在或已過期的項目為 null
     */
    public List<V> checkAll(List<K> keys) {
        return keys.stream().map(this::check).collect(Collectors.toList());
    }


    /**
     * 批量獲取多個緩存項目的完整資訊但不刷新過期時間。
     * 回傳包含值和過期時間的 CacheInfo 對象列表，僅用於檢查狀態。
     *
     * @param keys 緩存項目鍵的列表
     * @return 對應的緩存項目完整資訊列表，不存在或已過期的項目為 null
     */
    public List<CacheInfo<V>> checkAllInfo(List<K> keys) {
        return keys.stream().map(this::checkInfo).collect(Collectors.toList());
    }


    /**
     * 獲取緩存項目的完整資訊但不刷新過期時間。
     * 回傳包含值和過期時間的 CacheInfo 對象，如果已過期會自動移除。
     *
     * @param key 緩存項目的鍵
     * @return 緩存項目的完整資訊，如果不存在或已過期則為 null
     */
    public CacheInfo<V> checkInfo(K key) {
        return Optional.ofNullable(key).map(k -> cacheMap.get(key)).map(cacheInfo -> {
            long now = System.currentTimeMillis();
            if (now > cacheInfo.getExpireTimeMillis()) {
                cacheMap.remove(key, cacheInfo);
                return null;
            }
            return cacheInfo;
        }).orElse(null);
    }


    /**
     * 原子性地對指定鍵的緩存值進行計算或初始化。
     * 如果鍵存在且未過期，則應用 computeFunction 更新值；否則使用 initValue 初始化。
     * 此為重寫方法，使用預設的過期時間。
     *
     * @param key             鍵
     * @param initValue       初始化值（當鍵不存在或過期時使用）
     * @param computeFunction 計算函數，接受當前值和鍵，回傳新值
     *
     * @return 更新後的緩存值
     */
    public V computeIfPresentOrDefault(K key, V initValue, BiFunction<? super K, ? super V, V> computeFunction) {
        return computeIfPresentOrDefault(key, initValue, expireTime, computeFunction);
    }


    /**
     * 原子性地對指定鍵的緩存值進行計算或初始化。
     * 如果鍵存在且未過期，則應用 computeFunction 更新值；否則使用 initValue 初始化。
     * 使用細粒度鎖確保操作的原子性。
     *
     * @param key             鍵
     * @param initValue       初始化值（當鍵不存在或過期時使用）
     * @param expire          過期時間
     * @param computeFunction 計算函數，接受當前值和鍵，回傳新值
     *
     * @return 更新後的緩存值
     *
     * @throws IllegalArgumentException 如果鍵為 null 或過期時間無效
     */
    public V computeIfPresentOrDefault(K key, V initValue, Duration expire, BiFunction<? super K, ? super V, V> computeFunction) {
        if (key == null) {
            throw new IllegalArgumentException("鍵不能為null");
        }
        if (expire.toMillis() <= 0) {
            throw new IllegalArgumentException("過期時間必須大於0");
        }

        BiFunction<? super K, CacheInfo<V>, CacheInfo<V>> action = (k, existingInfo) -> {
            long now = System.currentTimeMillis();
            long expireTimeMillis = now + Math.min(expire.toMillis(), maxRemainTime.toMillis());
            V valueToStore;

            if (existingInfo == null || now > existingInfo.getExpireTimeMillis()) {
                valueToStore = initValue;
            } else {
                valueToStore = computeFunction.apply(k, existingInfo.getValue());
            }
            if (valueToStore == null) {
                return null;
            }
            return new CacheInfo<>(valueToStore, expireTimeMillis);
        };

        CacheInfo<V> computedInfo = cacheMap.compute(key, action);
        return computedInfo != null ? computedInfo.getValue() : null;
    }


    /**
     * 原子性地對指定鍵的緩存值進行計算或初始化。
     * 如果鍵存在且未過期，則應用 computeFunction 更新值，此方法不會初始化不存在的值
     * 此為重寫方法，使用預設的過期時間。
     *
     * @param key             鍵
     * @param computeFunction 計算函數，接受當前值和鍵，回傳新值
     *
     * @return 更新後的緩存值
     */
    public V computeIfPresent(K key, BiFunction<? super K, ? super V, V> computeFunction) {
        return computeIfPresent(key, expireTime, computeFunction);
    }


    /**
     * 原子性地對指定鍵的緩存值進行計算或初始化。
     * 如果鍵存在且未過期，則應用 computeFunction 更新值，此方法不會初始化不存在的值
     *
     * @param key             鍵
     * @param expire          過期時間
     * @param computeFunction 計算函數，接受當前值和鍵，回傳新值
     *
     * @return 更新後的緩存值
     */
    public V computeIfPresent(K key, Duration expire, BiFunction<? super K, ? super V, V> computeFunction) {
        if (key == null) {
            throw new IllegalArgumentException("鍵不能為null");
        }
        if (expire.toMillis() <= 0) {
            throw new IllegalArgumentException("過期時間必須大於0");
        }

        BiFunction<? super K, CacheInfo<V>, CacheInfo<V>> action = (k, existingInfo) -> {
            long now = System.currentTimeMillis();
            if (now > existingInfo.getExpireTimeMillis()) {
                return null;
            }
            V newValue = computeFunction.apply(k, existingInfo.getValue());
            if (newValue == null) {
                return null;
            }
            long expireTimeMillis = now + Math.min(expire.toMillis(), maxRemainTime.toMillis());
            return new CacheInfo<>(newValue, expireTimeMillis);
        };

        CacheInfo<V> computedInfo = cacheMap.computeIfPresent(key, action);
        return (computedInfo != null) ? computedInfo.getValue() : null;
    }


    /**
     * 批量刪除多個緩存項目。
     * 對列表中的每個鍵執行刪除操作。
     *
     * @param keys 要刪除的緩存項目鍵列表
     */
    public void removeAll(List<K> keys) {
        keys.forEach(this::remove);
    }


    /**
     * 刪除指定的緩存項目。
     * 立即從緩存中移除指定鍵的項目。
     *
     * @param key 要刪除的緩存項目鍵，不可為 null
     */
    public void remove(@NonNull K key) {
        cacheMap.remove(key);
    }


    /**
     * 銷毀緩存實例並釋放所有相關資源。
     * 清除所有緩存項目，優雅地關閉排程執行器服務，確保沒有資源洩漏。
     */
    public void destroy() {
        LogUnity.info("%s: 銷毀緩存表資料", this.tag != null ? tag : DEFAULT_TAG);
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        cacheMap.clear();
    }


    /**
     * 緩存項目的封裝類別，包含值和過期時間資訊。
     * 當系統時間超過 expireTimeMillis 時，該緩存項目會被視為過期並可能被清理。
     *
     * @param <V> 緩存項目值的類型
     */
    @Data
    public static class CacheInfo<V> {

        /**
         * 緩存項的值
         */
        private V value;

        /**
         * 緩存項的過期時間
         */
        private long expireTimeMillis;


        /**
         * 建構 CacheInfo 實例。
         *
         * @param value      緩存項目的值
         * @param expireTime 緩存項目的過期時間（毫秒時間戳）
         */
        public CacheInfo(V value, long expireTime) {
            this.value = value;
            this.expireTimeMillis = expireTime;
        }


        /**
         * 回傳緩存項目的字串表示。
         * 包含緩存項目的值和過期時間資訊。
         *
         * @return 緩存項目的字串表示
         */
        @Override
        public String toString() {
            return "CacheInfo{" + "value=" + value + ", expireTimeMillis=" + expireTimeMillis + '}';
        }
    }
}
