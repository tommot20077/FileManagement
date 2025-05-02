package xyz.dowob.filemanagement.unity;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * 一個基於HashMap的緩存實現，支持過期時間和最大保留時間的設置。
 * 封裝了HashMap，提供了方法來設置、獲取和刪除緩存項。
 * 並且支持定時清理過期項目。
 * 會將用戶儲存的數據和過期時間封裝成CacheInfo對象，並存儲在HashMap中。
 * 整個緩存的設計是線程安全的，使用了synchronized和ReentrantLock來保證多線程環境下的安全性。
 *
 * @author yuan
 * @program FileManagement
 * @ClassName CacheConcurrentHashMap
 * @create 2025/4/22
 * @Version 1.0
 **/
@SuppressWarnings("unused")
public class CacheConcurrentHashMap<K, V> {
    /**
     * 預設過期時間為10分鐘
     */
    private final static Duration DEFAULT_EXPIRE_DURATION = Duration.ofMinutes(10);

    /**
     * 預設最大保留時間為10分鐘
     */
    private final static Duration DEFAULT_REMAIN_TIME_DURATION = Duration.ofMinutes(10);

    /**
     * 預設清理間隔為10分鐘
     */
    private final static Duration DEFAULT_CLEANUP_INTERVAL = Duration.ofMinutes(10);

    /**
     * 預設初始容量為1024
     */
    private final static int DEFAULT_INITIAL_CAPACITY = 64;

    /**
     * 用於存儲緩存項的HashMap
     */
    private final HashMap<K, CacheInfo<V>> cacheMap;

    /**
     * 用於存儲每個鍵的鎖的ConcurrentHashMap
     */
    private final ConcurrentHashMap<K, ReentrantLock> lockMap;

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
     * CacheConcurrentHashMap的構造函數
     * 全部使用預設值
     *
     * @param enableCleanup 是否啟用清理任務
     */
    public CacheConcurrentHashMap(boolean enableCleanup) {
        this(DEFAULT_INITIAL_CAPACITY, DEFAULT_EXPIRE_DURATION, DEFAULT_REMAIN_TIME_DURATION, DEFAULT_CLEANUP_INTERVAL, enableCleanup);
    }

    /**
     * CacheConcurrentHashMap的構造函數
     * 自訂初始容量，其餘使用預設值
     *
     * @param initialCapacity 初始容量
     * @param enableCleanup   是否啟用清理任務
     */
    public CacheConcurrentHashMap(int initialCapacity, boolean enableCleanup) {
        this(initialCapacity, DEFAULT_EXPIRE_DURATION, DEFAULT_REMAIN_TIME_DURATION, DEFAULT_CLEANUP_INTERVAL, enableCleanup);
    }


    /**
     * CacheConcurrentHashMap的構造函數
     * 自訂初始容量和過期時間，其餘使用預設值
     *
     * @param initialCapacity 初始容量
     * @param expireTime      預設過期時間
     * @param enableCleanup   是否啟用清理任務
     */
    public CacheConcurrentHashMap(int initialCapacity, Duration expireTime, boolean enableCleanup) {
        this(initialCapacity, expireTime, expireTime, DEFAULT_CLEANUP_INTERVAL, enableCleanup);
    }


    /**
     * CacheConcurrentHashMap的構造函數
     * 自訂初始容量、過期時間和最大保留時間，其餘使用預設值
     *
     * @param initialCapacity 初始容量
     * @param expireTime      預設過期時間
     * @param maxRemainTime   預設最大保留時間
     */
    public CacheConcurrentHashMap(int initialCapacity, Duration expireTime, Duration maxRemainTime) {
        this(initialCapacity, expireTime, maxRemainTime, DEFAULT_CLEANUP_INTERVAL, true);
    }


    /**
     * CacheConcurrentHashMap的構造函數
     *
     * @param initialCapacity 初始容量
     * @param expireTime      預設過期時間
     * @param maxRemainTime   預設最大保留時間
     * @param cleanupInterval 清理間隔，此值僅在enableCleanup為true時有效
     * @param enableCleanup   是否啟用清理任務
     *
     * @throws IllegalArgumentException 當前容量、過期時間、最大保留時間或清理間隔小於等於0時，拋出異常
     */
    public CacheConcurrentHashMap(int initialCapacity, Duration expireTime, Duration maxRemainTime, Duration cleanupInterval, boolean enableCleanup) {
        if (initialCapacity <= 0) {
            throw new IllegalArgumentException("初始化容量必須大於0");
        }
        this.cacheMap = new HashMap<>(initialCapacity);
        this.lockMap = new ConcurrentHashMap<>(initialCapacity);
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
            });
            scheduleCleanupTask(cleanupInterval);
        }
    }


    /**
     * 設置緩存項的值和過期時間
     *
     * @param key       鍵
     * @param value     值
     * @param expire    過期時間
     * @param maxRemain 最大保留時間
     *
     * @throws IllegalArgumentException 當前鍵為null，過期時間小於等於0，或最大保留時間小於等於0時，拋出異常
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

        ReentrantLock lock = lockMap.computeIfAbsent(key, k -> new ReentrantLock());
        lock.lock();
        try {
            long expireTime = System.currentTimeMillis() + Math.min(expire.toMillis(), maxRemain.toMillis());
            cacheMap.put(key, new CacheInfo<>(value, expireTime));
        } finally {
            lock.unlock();
            cleanupLock(key, lock);
        }
    }


    /**
     * 設定緩存並使用預設的過期時間和最大保留時間
     *
     * @param key   鍵
     * @param value 值
     */
    public void set(K key, V value) {
        this.set(key, value, expireTime, maxRemainTime);
    }


    /**
     * 設置緩存項的值和過期時間
     *
     * @param key    鍵
     * @param value  值
     * @param expire 過期時間
     */
    public void set(K key, V value, Duration expire) {
        this.set(key, value, expire, maxRemainTime);
    }


    /**
     * 批量設置緩存項的值和過期時間
     *
     * @param kvMap     鍵值對的映射
     * @param expire    過期時間
     * @param maxRemain 最大保留時間
     */
    public void setAll(Map<K, V> kvMap, Duration expire, Duration maxRemain) {
        kvMap.forEach((key, value) -> this.set(key, value, expire, maxRemain));
    }


    /**
     * 批量設置緩存項的值和過期時間，並使用預設的最大保留時間
     *
     * @param kvMap  鍵值對的映射
     * @param expire 過期時間
     */
    public void setAll(Map<K, V> kvMap, Duration expire) {
        kvMap.forEach((key, value) -> this.set(key, value, expire, maxRemainTime));
    }


    /**
     * 獲取緩存項的值並刷新過期時間
     *
     * @param key 鍵
     *
     * @return 緩存項的值
     */
    public V get(K key) {
        return this.get(key, expireTime);
    }


    /**
     * 獲取封裝紀錄並刷新過期時間
     *
     * @param key 鍵
     *
     * @return 緩存項的值
     */
    public CacheInfo<V> getInfo(K key) {
        return getAndRefresh(key, expireTime);
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
     * 獲取封裝紀錄並指定緩存的過期時間
     *
     * @param key    鍵
     * @param expire 過期時間
     *
     * @return 緩存項的值
     */
    public CacheInfo<V> getInfo(K key, Duration expire) {
        return getAndRefresh(key, expire);
    }


    /**
     * 獲取緩存項的值但不刷新過期時間
     *
     * @param key 鍵
     *
     * @return 緩存項的值
     */
    public V check(K key) {
        ReentrantLock lock = lockMap.computeIfAbsent(key, k -> new ReentrantLock());
        lock.lock();
        try {
            CacheInfo<V> cacheInfo = cacheMap.get(key);
            if (cacheInfo == null || System.currentTimeMillis() > cacheInfo.getExpireTimeMillis()) {
                cacheMap.remove(key);
                return null;
            }
            return cacheInfo.getValue();
        } finally {
            lock.unlock();
            cleanupLock(key, lock);
        }
    }


    /**
     * 獲取緩存項的封裝紀錄但不刷新過期時間
     *
     * @param key 鍵
     *
     * @return 緩存項的封裝紀錄
     */
    public CacheInfo<V> checkInfo(K key) {
        ReentrantLock lock = lockMap.computeIfAbsent(key, k -> new ReentrantLock());
        lock.lock();
        try {
            CacheInfo<V> cacheInfo = cacheMap.get(key);
            if (cacheInfo == null || System.currentTimeMillis() > cacheInfo.getExpireTimeMillis()) {
                cacheMap.remove(key);
                return null;
            }
            return cacheInfo;
        } finally {
            lock.unlock();
            cleanupLock(key, lock);
        }
    }


    /**
     * 獲取緩存項的值，當前緩存項不存在或過期時，返回預設值
     *
     * @param key          鍵
     * @param defaultValue 預設值
     *
     * @return 緩存項的值或預設值
     */
    public V checkOrDefault(K key, V defaultValue) {
        V value = this.check(key);
        return value == null ? defaultValue : value;
    }


    /**
     * 批量獲取緩存項的值
     *
     * @param keys 鍵的列表
     *
     * @return 緩存項的值的列表
     */
    public List<V> getAll(List<K> keys) {
        return keys.stream().map(this::get).collect(Collectors.toList());
    }


    /**
     * 批量獲取緩存項的值並指定緩存的過期時間
     *
     * @param keys   鍵的列表
     * @param expire 過期時間
     *
     * @return 緩存項的值的列表
     */
    public List<V> getAll(List<K> keys, Duration expire) {
        return keys.stream().map(key -> this.get(key, expire)).collect(Collectors.toList());
    }


    /**
     * 批量獲取緩存項的封裝紀錄並指定緩存的過期時間
     *
     * @param keys 鍵的列表
     *
     * @return 緩存項的封裝紀錄的列表
     */
    public List<CacheInfo<V>> getAllInfo(List<K> keys, Duration expire) {
        return keys.stream().map(key -> this.getInfo(key, expire)).collect(Collectors.toList());
    }

    /**
     * 獲取緩存項的值並不刷新過期時間
     *
     * @param keys 鍵的列表
     *
     * @return 緩存項的值的列表
     */
    public List<V> checkAll(List<K> keys) {
        return keys.stream().map(this::check).collect(Collectors.toList());
    }


    /**
     * 獲取緩存項的封裝紀錄並不刷新過期時間
     *
     * @param keys 鍵的列表
     *
     * @return 緩存項的封裝紀錄的列表
     */
    public List<CacheInfo<V>> checkAllInfo(List<K> keys) {
        return keys.stream().map(this::checkInfo).collect(Collectors.toList());
    }


    /**
     * 原子性地對指定鍵的緩存值進行計算或初始化。
     * 如果鍵存在且未過期，則應用 computeFunction 更新值；否則使用 initValue 初始化。
     * 此為重寫方法，使用預設的過期時間。
     *
     * @param key             鍵
     * @param initValue       初始化值（當鍵不存在或過期時使用）
     * @param computeFunction 計算函數，接受當前值和鍵，返回新值
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
     * @param computeFunction 計算函數，接受當前值和鍵，返回新值
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

        ReentrantLock lock = lockMap.computeIfAbsent(key, k -> new ReentrantLock());
        lock.lock();
        try {
            CacheInfo<V> cacheInfo = cacheMap.get(key);
            V value;
            long expireTimeMillis = System.currentTimeMillis() + Math.min(expire.toMillis(), maxRemainTime.toMillis());

            if (cacheInfo == null || System.currentTimeMillis() > cacheInfo.getExpireTimeMillis()) {
                value = initValue;
            } else {
                value = computeFunction.apply(key, cacheInfo.getValue());
            }

            cacheMap.put(key, new CacheInfo<>(value, expireTimeMillis));
            return value;
        } finally {
            lock.unlock();
            cleanupLock(key, lock);
        }
    }


    /**
     * 原子性地對指定鍵的緩存值進行計算或初始化。
     * 如果鍵存在且未過期，則應用 computeFunction 更新值，此方法不會初始化不存在的值
     * 此為重寫方法，使用預設的過期時間。
     *
     * @param key             鍵
     * @param computeFunction 計算函數，接受當前值和鍵，返回新值
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
     * @param computeFunction 計算函數，接受當前值和鍵，返回新值
     *
     * @return 更新後的緩存值
     */
    public V computeIfPresent(K key, Duration expire, BiFunction<? super K, ? super V, V> computeFunction) {
        ReentrantLock lock = lockMap.computeIfAbsent(key, k -> new ReentrantLock());
        lock.lock();
        try {
            CacheInfo<V> cacheInfo = cacheMap.get(key);
            if (cacheInfo == null || System.currentTimeMillis() > cacheInfo.getExpireTimeMillis()) {
                cacheMap.remove(key);
                return null;
            }
            V newValue = computeFunction.apply(key, cacheInfo.getValue());
            long expireTimeMillis = System.currentTimeMillis() + Math.min(expire.toMillis(), maxRemainTime.toMillis());
            cacheMap.put(key, new CacheInfo<>(newValue, expireTimeMillis));
            return newValue;
        } finally {
            lock.unlock();
            cleanupLock(key, lock);
        }

    }


    /**
     * 刪除緩存項
     *
     * @param key 鍵
     */
    public void remove(K key) {
        ReentrantLock lock = lockMap.computeIfAbsent(key, k -> new ReentrantLock());
        lock.lock();
        try {
            cacheMap.remove(key);
            lockMap.remove(key);
        } finally {
            lock.unlock();
        }
    }


    /**
     * 批量刪除緩存項
     *
     * @param keys 鍵的列表
     */
    public void removeAll(List<K> keys) {
        keys.forEach(this::remove);
    }


    /**
     * 批量設置緩存項的值和過期時間，並使用預設的過期時間和最大保留時間
     *
     * @param key    鍵
     * @param expire 過期時間
     */
    private CacheInfo<V> getAndRefresh(K key, Duration expire) {
        ReentrantLock lock = lockMap.computeIfAbsent(key, k -> new ReentrantLock());
        lock.lock();
        try {
            CacheInfo<V> cacheInfo = cacheMap.get(key);
            if (cacheInfo == null || System.currentTimeMillis() > cacheInfo.getExpireTimeMillis()) {
                cacheMap.remove(key);
                lockMap.remove(key);
                return null;
            }
            long newExpireTime = System.currentTimeMillis() + Math.min(expire.toMillis(), maxRemainTime.toMillis());
            cacheInfo.setExpireTimeMillis(newExpireTime);
            return cacheInfo;
        } finally {
            lock.unlock();
            cleanupLock(key, lock);
        }
    }


    /**
     * 清理鎖
     * 當鎖的排隊長度為0且鎖未被鎖定，並且緩存項不存在時，則移除鎖
     *
     * @param key  鍵
     * @param lock 鎖
     */
    private void cleanupLock(K key, ReentrantLock lock) {
        if (lock.getQueueLength() == 0 && !lock.isLocked() && cacheMap.get(key) == null) {
            lockMap.remove(key);
        }
    }


    /**
     * 定時清理過期項目的啟動方法
     *
     * @param interval 清理間隔時間
     *
     * @throws IllegalStateException 當前清理任務未啟用時，拋出異常
     */
    private void scheduleCleanupTask(Duration interval) {
        if (scheduler == null) {
            throw new IllegalStateException("清理任務未啟用，請在構造函數中設置enableCleanup為true");
        }
        scheduler.scheduleAtFixedRate(this::getCleanupTask, interval.toMillis(), interval.toMillis(), TimeUnit.MILLISECONDS);
    }


    /**
     * 獲取清理過期項目的任務
     *
     * @return 清理過期項目的任務
     */
    public Runnable getCleanupTask() {
        return () -> {
            LogUnity.debug("清理過期緩存資料");
            synchronized (cacheMap) {
                long currentTime = System.currentTimeMillis();
                Set<K> expiredKeys = cacheMap
                        .entrySet()
                        .stream()
                        .filter(entry -> entry.getValue().getExpireTimeMillis() < currentTime)
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toSet());
                expiredKeys.forEach(key -> {
                    cacheMap.remove(key);
                    ReentrantLock lock = lockMap.get(key);
                    if (lock != null && lock.getQueueLength() == 0 && !lock.isLocked()) {
                        lockMap.remove(key);
                    }
                });
            }
        };
    }


    /**
     * 銷毀緩存提供的資源
     * 將清除所有緩存項，並關閉ScheduledExecutorService
     */
    public void destroy() {
        synchronized (cacheMap) {
            cacheMap.clear();
        }
        lockMap.clear();
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
    }


    /**
     * 緩存對象封裝類，用於存儲緩存項的值和過期時間
     * 當前時間超過expireTimeMillis，則該緩存項將被視為過期
     *
     * @param <V> 緩存項的值類型
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
         * CacheInfo的構造函數
         *
         * @param value      緩存項的值
         * @param expireTime 緩存項的過期時間
         */
        public CacheInfo(V value, long expireTime) {
            this.value = value;
            this.expireTimeMillis = expireTime;
        }


        /**
         * 重寫toString方法，返回緩存項的值和過期時間
         */
        @Override
        public String toString() {
            return "CacheInfo{" + "value=" + value + ", expireTimeMillis=" + expireTimeMillis + '}';
        }
    }
}
