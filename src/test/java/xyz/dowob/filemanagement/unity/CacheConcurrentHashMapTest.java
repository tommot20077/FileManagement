package xyz.dowob.filemanagement.unity;

import org.junit.jupiter.api.*;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.BiFunction;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

/**
 * CacheConcurrentHashMap 的單元測試類。
 */
@DisplayName("CacheConcurrentHashMap 邏輯處理測試")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CacheConcurrentHashMapTest {

    private static final Duration SHORT_EXPIRY = Duration.ofMillis(100);

    private static final Duration MEDIUM_EXPIRY = Duration.ofSeconds(1);

    private static final Duration LONG_EXPIRY = Duration.ofMinutes(1);

    private static final Duration CLEANUP_INTERVAL = Duration.ofMillis(50);

    private CacheConcurrentHashMap<String, String> cache;

    private CacheConcurrentHashMap<String, Integer> intCache;


    @BeforeEach
    void setUp() {
        cache = createCache(false, MEDIUM_EXPIRY, LONG_EXPIRY, CLEANUP_INTERVAL);
        cache.setTag("testCache");
        intCache = createCache(false, MEDIUM_EXPIRY, LONG_EXPIRY, CLEANUP_INTERVAL);
        intCache.setTag("testIntCache");
    }


    private <K, V> CacheConcurrentHashMap<K, V> createCache(boolean enableCleanup, Duration expire, Duration maxRemain, Duration cleanupInterval) {
        return new CacheConcurrentHashMap<>(64, expire, maxRemain, cleanupInterval, enableCleanup);
    }


    @AfterEach
    void tearDown() {
        if (cache != null) {
            cache.destroy();
        }
        if (intCache != null) {
            intCache.destroy();
        }
    }


    @Test
    @DisplayName("測試構造函數 - 啟用清理功能且清理間隔有效 - Scheduler 初始化成功")
    void constructor_whenEnableCleanupWithValidInterval_thenSchedulerInitialized() {
        CacheConcurrentHashMap<String, String> cleanupCache = null;
        try {
            cleanupCache = createCache(true, MEDIUM_EXPIRY, LONG_EXPIRY, CLEANUP_INTERVAL);
            assertNotNull(cleanupCache, "緩存實例不應為null");
        } finally {
            if (cleanupCache != null) {
                cleanupCache.destroy();
            }
        }
    }


    @Test
    @DisplayName("測試構造函數 - 啟用清理功能但清理間隔無效（<=0） - 拋出 IllegalArgumentException")
    void constructor_whenEnableCleanupWithInvalidInterval_thenThrowException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                                                              CacheConcurrentHashMap<String, String> cleanupCache = null;
                                                              try {
                                                                  cleanupCache = createCache(true, MEDIUM_EXPIRY, LONG_EXPIRY, Duration.ZERO);
                                                              } finally {
                                                                  if (cleanupCache != null) {
                                                                      cleanupCache.destroy();
                                                                  }
                                                              }
                                                          }, "當清理間隔無效時應拋出 IllegalArgumentException"
        );
        assertEquals("清理間隔必須大於0", exception.getMessage());

        exception = assertThrows(IllegalArgumentException.class, () -> {
                                     CacheConcurrentHashMap<String, String> cleanupCache = null;
                                     try {
                                         cleanupCache = createCache(true, MEDIUM_EXPIRY, LONG_EXPIRY, Duration.ofMillis(-100));
                                     } finally {
                                         if (cleanupCache != null) {
                                             cleanupCache.destroy();
                                         }
                                     }
                                 }, "當清理間隔無效時應拋出 IllegalArgumentException"
        );
        assertEquals("清理間隔必須大於0", exception.getMessage());
    }


    @Test
    @DisplayName("測試構造函數 - 初始容量無效（<=0） - 拋出 IllegalArgumentException")
    void constructor_whenInvalidInitialCapacity_thenThrowException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                                                              new CacheConcurrentHashMap<>(0, false);
                                                          }, "當初始容量無效時應拋出 IllegalArgumentException"
        );
        assertEquals("初始化容量必須大於0", exception.getMessage());

        exception = assertThrows(IllegalArgumentException.class, () -> {
                                     new CacheConcurrentHashMap<>(-10, false);
                                 }, "當初始容量無效時應拋出 IllegalArgumentException"
        );
        assertEquals("初始化容量必須大於0", exception.getMessage());
    }


    @Test
    @DisplayName("測試構造函數 - 過期時間或最大保留時間無效（<=0）使用預設值")
    void constructor_whenInvalidDurations_thenUseDefaults() {
        CacheConcurrentHashMap<String, String> testCache = new CacheConcurrentHashMap<>(64,
                                                                                        Duration.ZERO,
                                                                                        Duration.ZERO,
                                                                                        Duration.ofSeconds(1),
                                                                                        false
        );
        assertEquals(Duration.ofMinutes(10), testCache.getExpireTime(), "無效的過期時間應使用預設值");
        assertEquals(Duration.ofMinutes(10), testCache.getMaxRemainTime(), "無效的最大保留時間應使用預設值");
        testCache.destroy();

        testCache = new CacheConcurrentHashMap<>(64, Duration.ofSeconds(-1), Duration.ofSeconds(-1), Duration.ofSeconds(1), false);
        assertEquals(Duration.ofMinutes(10), testCache.getExpireTime(), "無效的過期時間應使用預設值");
        assertEquals(Duration.ofMinutes(10), testCache.getMaxRemainTime(), "無效的最大保留時間應使用預設值");
        testCache.destroy();
    }


    @Test
    @DisplayName("測試 set 方法 - 正常設置鍵值對，使用預設過期時間")
    void set_whenNormalKeyAndValue_thenStoredSuccessfully() {
        cache.set("key1", "value1");
        assertEquals("value1", cache.check("key1"), "設置後應能立即獲取值");
    }


    @Test
    @DisplayName("測試 set 方法 - 設置 null 鍵")
    void set_whenKeyIsNull_thenThrowException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                                                              cache.set(null, "value");
                                                          }, "設置 null 鍵時應拋出 IllegalArgumentException"
        );
        assertEquals("鍵不能為null", exception.getMessage());
    }


    @Test
    @DisplayName("測試 set 方法 - 設置 null 值")
    void set_whenValueIsNull_thenStoredSuccessfully() {
        cache.set("keyNull", null);
        assertNull(cache.check("keyNull"), "設置 null 值後應能獲取 null");
    }


    @Test
    @DisplayName("測試 set 方法 - 使用無效過期時間（<=0）")
    void set_whenInvalidExpireTime_thenThrowException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                                                              cache.set("key", "value", Duration.ZERO);
                                                          }, "設置無效過期時間時應拋出 IllegalArgumentException"
        );
        assertEquals("過期時間必須大於0", exception.getMessage());

        exception = assertThrows(IllegalArgumentException.class, () -> {
                                     cache.set("key", "value", Duration.ofMillis(-100));
                                 }, "設置無效過期時間時應拋出 IllegalArgumentException"
        );
        assertEquals("過期時間必須大於0", exception.getMessage());
    }


    @Test
    @DisplayName("測試 set 方法 - 使用無效最大保留時間（<=0）")
    void set_whenInvalidMaxRemainTime_thenThrowException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                                                              cache.set("key", "value", MEDIUM_EXPIRY, Duration.ZERO);
                                                          }, "設置無效最大保留時間時應拋出 IllegalArgumentException"
        );
        assertEquals("最大保留時間必須大於0", exception.getMessage());

        exception = assertThrows(IllegalArgumentException.class, () -> {
                                     cache.set("key", "value", MEDIUM_EXPIRY, Duration.ofSeconds(-1));
                                 }, "設置無效最大保留時間時應拋出 IllegalArgumentException"
        );
        assertEquals("最大保留時間必須大於0", exception.getMessage());
    }


    @Test
    @DisplayName("測試 set 方法 - 過期時間超過最大保留時間")
    void set_whenExpireTimeExceedsMaxRemain_thenUseMaxRemain() throws InterruptedException {
        cache.set("keyLimited", "valueLimited", LONG_EXPIRY, SHORT_EXPIRY);
        assertEquals("valueLimited", cache.check("keyLimited"), "設置後應能立即獲取值");

        Thread.sleep(SHORT_EXPIRY.toMillis() + 50);

        assertNull(cache.check("keyLimited"), "值應因達到最大保留時間而過期");
    }


    @Test
    @DisplayName("測試 set 方法 - 重複設置同一個鍵")
    void set_whenSettingSameKeyMultipleTimes_thenValueIsOverwritten() {
        cache.set("keyRepeat", "value1");
        assertEquals("value1", cache.check("keyRepeat"));
        cache.set("keyRepeat", "value2");
        assertEquals("value2", cache.check("keyRepeat"), "後設置的值應覆蓋先前的值");
    }


    @Test
    @DisplayName("測試 get 方法 - 獲取存在的鍵，應返回值並刷新過期時間")
    void get_whenKeyExists_thenReturnValuAndRefreshExpiry() throws InterruptedException {
        cache.set("keyGet", "valueGet", SHORT_EXPIRY);
        assertEquals("valueGet", cache.check("keyGet"), "設置後應能立即獲取值");

        Thread.sleep(SHORT_EXPIRY.toMillis() / 2);

        assertEquals("valueGet", cache.get("keyGet"), "get() 應返回值");

        Thread.sleep(SHORT_EXPIRY.toMillis() / 2 + 10);

        assertEquals("valueGet", cache.check("keyGet"), "get() 應已刷新過期時間，值仍然存在");

        Thread.sleep(MEDIUM_EXPIRY.toMillis() + 50);
        assertNull(cache.check("keyGet"), "值最終應過期");
    }


    @Test
    @DisplayName("測試 get 方法 - 獲取不存在的鍵")
    void get_whenKeyDoesNotExist_thenReturnNull() {
        assertNull(cache.get("nonExistentKey"), "獲取不存在的鍵時應返回 null");
    }


    @Test
    @DisplayName("測試 get 方法 - 獲取已過期的鍵")
    void get_whenKeyIsExpired_thenReturnNull() throws InterruptedException {
        cache.set("keyExpired", "valueExpired", SHORT_EXPIRY);
        assertEquals("valueExpired", cache.check("keyExpired"));

        Thread.sleep(SHORT_EXPIRY.toMillis() + 50);

        assertNull(cache.get("keyExpired"), "獲取已過期的鍵時應返回 null");
        assertNull(cache.check("keyExpired"), "過期的鍵應已被移除");
    }


    @Test
    @DisplayName("測試 get 方法 - 使用自訂過期時間刷新")
    void get_whenUsingCustomExpiryOnGet_thenRefreshesWithCustomExpiry() throws InterruptedException {
        Duration customRefreshExpiry = Duration.ofSeconds(2);
        cache.set("keyCustomGet", "valueCustomGet", SHORT_EXPIRY);
        assertEquals("valueCustomGet", cache.check("keyCustomGet"));

        Thread.sleep(SHORT_EXPIRY.toMillis() / 2);

        assertEquals("valueCustomGet", cache.get("keyCustomGet", customRefreshExpiry));

        Thread.sleep(SHORT_EXPIRY.toMillis());

        assertEquals("valueCustomGet", cache.check("keyCustomGet"), "值應因自訂刷新而仍然存在");

        Thread.sleep(customRefreshExpiry.toMillis());
        assertNull(cache.check("keyCustomGet"), "值最終應根據自訂刷新時間過期");
    }


    @Test
    @DisplayName("測試 check 方法 - 檢查存在的鍵，應返回值且不刷新過期時間")
    void check_whenKeyExists_thenReturnValuWithoutRefreshingExpiry() throws InterruptedException {
        cache.set("keyCheck", "valueCheck", SHORT_EXPIRY);
        assertEquals("valueCheck", cache.check("keyCheck"), "check() 應返回值");

        Thread.sleep(SHORT_EXPIRY.toMillis() + 50);

        assertNull(cache.check("keyCheck"), "check() 不應刷新過期時間，值應已過期");
    }


    @Test
    @DisplayName("測試 check 方法 - 檢查不存在的鍵")
    void check_whenKeyDoesNotExist_thenReturnNull() {
        assertNull(cache.check("nonExistentKeyCheck"), "檢查不存在的鍵時應返回 null");
    }


    @Test
    @DisplayName("測試 check 方法 - 檢查已過期的鍵")
    void check_whenKeyIsExpired_thenReturnNull() throws InterruptedException {
        cache.set("keyCheckExpired", "valueCheckExpired", SHORT_EXPIRY);
        assertEquals("valueCheckExpired", cache.check("keyCheckExpired"));

        Thread.sleep(SHORT_EXPIRY.toMillis() + 50);

        assertNull(cache.check("keyCheckExpired"), "檢查已過期的鍵時應返回 null");
    }


    @Test
    @DisplayName("測試 checkOrDefault 方法 - 檢查存在的鍵")
    void checkOrDefault_whenKeyExists_thenReturnActualValue() {
        cache.set("keyCheckDef", "actualValue");
        assertEquals("actualValue", cache.checkOrDefault("keyCheckDef", "defaultValue"), "存在時應返回實際值");
    }


    @Test
    @DisplayName("測試 checkOrDefault 方法 - 檢查不存在的鍵")
    void checkOrDefault_whenKeyDoesNotExist_thenReturnDefaultValue() {
        assertEquals("defaultValue", cache.checkOrDefault("nonExistentCheckDef", "defaultValue"), "不存在時應返回預設值");
    }


    @Test
    @DisplayName("測試 checkOrDefault 方法 - 檢查已過期的鍵")
    void checkOrDefault_whenKeyIsExpired_thenReturnDefaultValue() throws InterruptedException {
        cache.set("keyCheckDefExpired", "actualValue", SHORT_EXPIRY);
        assertEquals("actualValue", cache.check("keyCheckDefExpired"));

        Thread.sleep(SHORT_EXPIRY.toMillis() + 50);

        assertEquals("defaultValue", cache.checkOrDefault("keyCheckDefExpired", "defaultValue"), "過期時應返回預設值");
    }


    @Test
    @DisplayName("測試 remove 方法 - 移除存在的鍵")
    void remove_whenKeyExists_thenRemovesSuccessfully() {
        cache.set("keyRemove", "valueRemove");
        assertEquals("valueRemove", cache.check("keyRemove"));
        cache.remove("keyRemove");
        assertNull(cache.check("keyRemove"), "移除後鍵應不存在");
    }


    @Test
    @DisplayName("測試 remove 方法 - 移除不存在的鍵")
    void remove_whenKeyDoesNotExist_thenDoesNothing() {
        assertNull(cache.check("nonExistentRemove"));
        assertDoesNotThrow(() -> cache.remove("nonExistentRemove"), "移除不存在的鍵不應拋出異常");
        assertNull(cache.check("nonExistentRemove"));
    }


    @Test
    @DisplayName("測試 remove 方法 - 移除 null 鍵")
    void remove_whenKeyIsNull_thenThrowsException() {
        assertThrows(NullPointerException.class, () -> {
                         cache.remove(null);
                     }, "移除 null 鍵時應拋出 NullPointerException (來自 ConcurrentHashMap)"
        );
    }


    @Test
    @DisplayName("測試自動清理：過期項目應被清理任務移除")
    void cleanupTask_whenItemsExpire_thenRemovedByCleanup() {
        CacheConcurrentHashMap<String, String> cleanupCache = null;
        try {
            cleanupCache = createCache(true, SHORT_EXPIRY, MEDIUM_EXPIRY, CLEANUP_INTERVAL);
            cleanupCache.set("keyClean1", "valueClean1");
            cleanupCache.set("keyClean2", "valueClean2");
            assertEquals("valueClean1", cleanupCache.check("keyClean1"));
            assertEquals("valueClean2", cleanupCache.check("keyClean2"));

            final CacheConcurrentHashMap<String, String> finalCleanupCache = cleanupCache;
            await().atMost(CLEANUP_INTERVAL.multipliedBy(4)).pollInterval(CLEANUP_INTERVAL.dividedBy(2)).untilAsserted(() -> {
                assertNull(finalCleanupCache.check("keyClean1"), "keyClean1 應已被清理");
                assertNull(finalCleanupCache.check("keyClean2"), "keyClean2 應已被清理");
            });

        } finally {
            if (cleanupCache != null) {
                cleanupCache.destroy();
            }
        }
    }


    @Test
    @DisplayName("測試手動觸發清理：過期項目應被移除")
    void manualCleanup_whenItemsExpire_thenRemovedByManualTrigger() throws InterruptedException {
        cache.set("keyManualClean", "valueManualClean", SHORT_EXPIRY);
        assertEquals("valueManualClean", cache.check("keyManualClean"));

        Thread.sleep(70);
        assertNotNull(cache.checkInfo("keyManualClean"), "項目應仍然在 map 中");

        Thread.sleep(50);
        Runnable cleanupTask = cache.getCleanupTask();
        cleanupTask.run();

        Thread.sleep(50);
        assertNull(cache.checkInfo("keyManualClean"), "手動清理後，過期項目應被移除");
    }


    @Test
    @DisplayName("測試併發 set/get：多線程同時讀寫應保持一致性")
    void concurrentSetGet_whenMultipleThreadsAccess_thenMaintainConsistency() throws InterruptedException {
        int threadCount = 100;
        int operationsPerThread = 1000;
        CacheConcurrentHashMap<String, String> longCache = createCache(false, LONG_EXPIRY, LONG_EXPIRY, CLEANUP_INTERVAL);
        ConcurrentHashMap<String, String> expectedValues = new ConcurrentHashMap<>();
        CountDownLatch latch = new CountDownLatch(threadCount);

        try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
            for (int i = 0; i < threadCount; i++) {
                final int threadId = i;
                executor.submit(() -> {
                    try {
                        for (int j = 0; j < operationsPerThread; j++) {
                            String key = "key-" + threadId + "-" + j;
                            String value = "value-" + threadId + "-" + j;
                            expectedValues.put(key, value);
                            longCache.set(key, value, LONG_EXPIRY);

                            String retrievedValue = longCache.get(key);
                            assertEquals(value, retrievedValue, "併發讀取應獲取正確的值 for key: " + key);

                            if (j % 10 == 0 && threadId > 0) {
                                String otherKey = "key-" + (threadId - 1) + "-" + j;
                                cache.check(otherKey);
                            }
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            assertTrue(latch.await(15, TimeUnit.SECONDS), "線程應在超時前完成");
            executor.shutdown();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "ExecutorService 應正常關閉");

            Thread.sleep(100);

            await().atMost(500, TimeUnit.MILLISECONDS).untilAsserted(() -> {
                for (Map.Entry<String, String> entry : expectedValues.entrySet()) {
                    String key = entry.getKey();
                    String expectedValue = entry.getValue();
                    String actualValue = longCache.check(key);
                    assertEquals(expectedValue, actualValue, "最終檢查：所有鍵應具有正確的值 for key: " + key);
                }
            });
        }
    }


    @Test
    @DisplayName("測試併發 computeIfPresent：多線程同時計算應正確更新")
    void concurrentCompute_whenMultipleThreadsCompute_thenUpdateCorrectly() throws InterruptedException {
        int threadCount = 10;
        int incrementsPerThread = 100;
        String computeKey = "computeKey";
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        intCache.set(computeKey, 0, LONG_EXPIRY);

        BiFunction<String, Integer, Integer> incrementFunction = (k, v) -> (v == null) ? 1 : v + 1;

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < incrementsPerThread; j++) {
                        intCache.computeIfPresent(computeKey, incrementFunction);
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(15, TimeUnit.SECONDS), "線程應在超時前完成");
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "ExecutorService 應正常關閉");

        Integer finalValue = intCache.get(computeKey);
        assertNotNull(finalValue, "計算後的鍵不應為 null");
        assertEquals(threadCount * incrementsPerThread, finalValue.intValue(), "併發計算後的最終值應正確");
    }


    @Test
    @DisplayName("測試併發 computeIfPresentOrDefault：多線程同時計算/初始化應正確")
    void concurrentComputeOrDefault_whenMultipleThreadsCompute_thenUpdateCorrectly() throws InterruptedException {
        int threadCount = 10;
        int incrementsPerThread = 100;
        String computeKey = "computeKeyOD";
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        BiFunction<String, Integer, Integer> incrementFunction = (k, v) -> v + 1;

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < incrementsPerThread; j++) {
                        intCache.computeIfPresentOrDefault(computeKey, 1, LONG_EXPIRY, incrementFunction);
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(15, TimeUnit.SECONDS), "線程應在超時前完成");
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "ExecutorService 應正常關閉");

        Integer finalValue = intCache.get(computeKey);
        assertNotNull(finalValue, "計算後的鍵不應為 null");

        assertEquals(threadCount * incrementsPerThread, finalValue.intValue(), "併發計算/初始化後的最終值應正確");
    }


    @Test
    @DisplayName("測試 setAll 方法：批量設置鍵值對")
    void setAll_whenGivenMap_thenAllKeysSet() {
        Map<String, String> items = new HashMap<>();
        items.put("bulkKey1", "bulkValue1");
        items.put("bulkKey2", "bulkValue2");
        items.put("bulkKey3", "bulkValue3");

        cache.setAll(items, MEDIUM_EXPIRY);

        assertEquals("bulkValue1", cache.check("bulkKey1"));
        assertEquals("bulkValue2", cache.check("bulkKey2"));
        assertEquals("bulkValue3", cache.check("bulkKey3"));
    }


    @Test
    @DisplayName("測試 setAll 方法：使用空 Map")
    void setAll_whenMapIsEmpty_thenDoesNothing() {
        Map<String, String> emptyMap = new HashMap<>();
        assertDoesNotThrow(() -> cache.setAll(emptyMap, MEDIUM_EXPIRY));
    }


    @Test
    @DisplayName("測試 setAll 方法：Map 中包含 null 鍵")
    void setAll_whenMapContainsKeyNull_thenThrowsException() {
        Map<String, String> items = new HashMap<>();
        items.put("bulkKey1", "bulkValue1");
        items.put(null, "nullKeyValue");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                                                              cache.setAll(items, MEDIUM_EXPIRY);
                                                          }
        );
        assertEquals("鍵不能為null", exception.getMessage());

        assertNull(cache.check("bulkKey1"), "操作失敗後，先前 Map 中的鍵不應存在（或取決於迭代順序）");
    }


    @Test
    @DisplayName("測試 getAll 方法：批量獲取存在的鍵")
    void getAll_whenKeysExist_thenReturnAllValues() {
        cache.set("getKey1", "getValue1");
        cache.set("getKey2", "getValue2");
        cache.set("getKey3", "getValue3");

        List<String> keysToGet = List.of("getKey1", "getKey2", "nonExistentGetAll");
        List<String> expectedValues = Arrays.asList("getValue1", "getValue2", null);

        List<String> actualValues = cache.getAll(keysToGet);

        assertEquals(expectedValues, actualValues, "getAll 應返回對應的值列表，不存在的鍵返回 null");
    }


    @Test
    @DisplayName("測試 getAll 方法：使用空列表")
    void getAll_whenKeyListIsEmpty_thenReturnEmptyList() {
        List<String> emptyList = List.of();
        List<String> result = cache.getAll(emptyList);
        assertTrue(result.isEmpty(), "getAll 使用空列表應返回空列表");
    }


    @Test
    @DisplayName("測試 getAll 方法：列表包含 null 鍵")
    void getAll_whenKeyListContainsNull_thenHandlesNullKey() {

        List<String> keysWithNull = Arrays.asList("getKey1", null, "getKey2");
        cache.set("getKey1", "getValue1");
        cache.set("getKey2", "getValue2");

        List<String> expectedValues = Arrays.asList("getValue1", null, "getValue2");
        List<String> actualValues = cache.getAll(keysWithNull);
        assertEquals(expectedValues, actualValues, "getAll 應返回對應的值列表，null 鍵應返回 null");
    }


    @Test
    @DisplayName("測試 getInfo 方法：獲取存在的鍵的 CacheInfo")
    void getInfo_whenKeyExists_thenReturnCacheInfo() {
        long startTime = System.currentTimeMillis();
        cache.set("keyInfo", "valueInfo", MEDIUM_EXPIRY);
        long endTime = System.currentTimeMillis();

        CacheConcurrentHashMap.CacheInfo<String> info = cache.getInfo("keyInfo");

        assertNotNull(info, "getInfo 應返回 CacheInfo 對象");
        assertEquals("valueInfo", info.getValue(), "CacheInfo 中的值應正確");
        assertTrue(info.getExpireTimeMillis() >= startTime + MEDIUM_EXPIRY.toMillis(), "CacheInfo 中的過期時間應正確設置 (考慮延遲)");
        assertTrue(info.getExpireTimeMillis() <= endTime + MEDIUM_EXPIRY.toMillis() + 100, "CacheInfo 中的過期時間應正確設置 (考慮延遲上限)");
    }


    @Test
    @DisplayName("測試 getInfo 方法：獲取不存在鍵的 CacheInfo")
    void getInfo_whenKeyDoesNotExist_thenReturnNull() {
        assertNull(cache.getInfo("nonExistentInfo"), "getInfo 對於不存在的鍵應返回 null");
    }


    @Test
    @DisplayName("測試 getInfo 方法：獲取已過期鍵的 CacheInfo")
    void getInfo_whenKeyExpired_thenReturnNull() throws InterruptedException {
        cache.set("keyInfoExpired", "valueInfoExpired", SHORT_EXPIRY);
        Thread.sleep(SHORT_EXPIRY.toMillis() + 50);
        assertNull(cache.getInfo("keyInfoExpired"), "getInfo 對於已過期的鍵應返回 null");
    }


    @Test
    @DisplayName("測試 checkInfo 方法：檢查存在的鍵的 CacheInfo，不刷新過期")
    void checkInfo_whenKeyExists_thenReturnCacheInfoWithoutRefresh() throws InterruptedException {
        cache.set("keyCheckInfo", "valueCheckInfo", SHORT_EXPIRY);
        CacheConcurrentHashMap.CacheInfo<String> info1 = cache.checkInfo("keyCheckInfo");
        assertNotNull(info1);
        long expiry1 = info1.getExpireTimeMillis();

        Thread.sleep(SHORT_EXPIRY.toMillis() / 2);

        CacheConcurrentHashMap.CacheInfo<String> info2 = cache.checkInfo("keyCheckInfo");
        assertNotNull(info2);
        long expiry2 = info2.getExpireTimeMillis();

        assertEquals(expiry1, expiry2, "checkInfo 不應刷新過期時間");

        Thread.sleep(SHORT_EXPIRY.toMillis() / 2 + 50);
        assertNull(cache.checkInfo("keyCheckInfo"), "值應因未刷新而過期");
    }


    @Test
    @DisplayName("測試 checkInfo 方法：檢查不存在鍵的 CacheInfo")
    void checkInfo_whenKeyDoesNotExist_thenReturnNull() {
        assertNull(cache.checkInfo("nonExistentCheckInfo"), "checkInfo 對於不存在的鍵應返回 null");
    }


    @Test
    @DisplayName("測試 checkInfo 方法：檢查已過期鍵的 CacheInfo")
    void checkInfo_whenKeyExpired_thenReturnNull() throws InterruptedException {
        cache.set("keyCheckInfoExpired", "valueCheckInfoExpired", SHORT_EXPIRY);
        Thread.sleep(SHORT_EXPIRY.toMillis() + 50);
        assertNull(cache.checkInfo("keyCheckInfoExpired"), "checkInfo 對於已過期的鍵應返回 null");
    }


    @Test
    @DisplayName("測試 computeIfPresent 方法：鍵存在時執行計算")
    void computeIfPresent_whenKeyExists_thenComputesValue() {
        intCache.set("computeKey1", 10);
        Integer result = intCache.computeIfPresent("computeKey1", (k, v) -> v * 2);
        assertEquals(20, result, "計算後的值應為 20");
        assertEquals(20, intCache.get("computeKey1"), "緩存中的值應更新為 20");
    }


    @Test
    @DisplayName("測試 computeIfPresent 方法：鍵不存在時不執行計算")
    void computeIfPresent_whenKeyDoesNotExist_thenReturnsNull() {
        Integer result = intCache.computeIfPresent("nonExistentCompute", (k, v) -> v * 2);
        assertNull(result, "鍵不存在時應返回 null");
        assertNull(intCache.get("nonExistentCompute"), "鍵不存在時緩存中不應有值");
    }


    @Test
    @DisplayName("測試 computeIfPresent 方法：鍵已過期時不執行計算")
    void computeIfPresent_whenKeyExpired_thenReturnsNull() throws InterruptedException {
        intCache.set("computeExpired", 5, SHORT_EXPIRY);
        Thread.sleep(SHORT_EXPIRY.toMillis() + 50);
        Integer result = intCache.computeIfPresent("computeExpired", (k, v) -> v * 2);
        assertNull(result, "鍵過期時應返回 null");
        assertNull(intCache.get("computeExpired"), "鍵過期時緩存中不應有值");
    }


    @Test
    @DisplayName("測試 computeIfPresent 方法：計算函數返回 null")
    void computeIfPresent_whenFunctionReturnsNull_thenRemovesEntry() {
        intCache.set("computeToNull", 100);
        Integer result = intCache.computeIfPresent("computeToNull", (k, v) -> null);

        assertNull(result, "計算結果為 null 時應返回 null");
        System.out.println("computeToNull: " + intCache.get("computeToNull"));
        assertNull(intCache.get("computeToNull"), "緩存中應存儲 null 值");
        System.out.println("checkInfo: " + intCache.checkInfo("computeToNull"));
        assertNull(intCache.checkInfo("computeToNull"), "CacheInfo 應為 null");
    }


    @Test
    @DisplayName("測試 computeIfPresentOrDefault 方法：鍵存在時執行計算")
    void computeIfPresentOrDefault_whenKeyExists_thenComputesValue() {
        intCache.set("computeODKey1", 10);
        intCache.setExpireTime(Duration.ofSeconds(5));
        intCache.setMaxRemainTime(Duration.ofSeconds(5));
        Integer result = intCache.computeIfPresentOrDefault("computeODKey1", 999, (k, v) -> v * 2);
        assertEquals(20, result, "計算後的值應為 20");
        assertEquals(20, intCache.get("computeODKey1"), "緩存中的值應更新為 20");
    }


    @Test
    @DisplayName("測試 computeIfPresentOrDefault 方法：鍵不存在時使用預設值")
    void computeIfPresentOrDefault_whenKeyDoesNotExist_thenUsesDefaultValue() {
        Integer result = intCache.computeIfPresentOrDefault("nonExistentComputeOD", 55, (k, v) -> v * 2);
        assertEquals(55, result, "鍵不存在時應返回預設值");
        assertEquals(55, intCache.get("nonExistentComputeOD"), "緩存中應存儲預設值");
    }


    @Test
    @DisplayName("測試 computeIfPresentOrDefault 方法：鍵已過期時使用預設值")
    void computeIfPresentOrDefault_whenKeyExpired_thenUsesDefaultValue() throws InterruptedException {
        intCache.set("computeODExpired", 5, SHORT_EXPIRY);
        Thread.sleep(SHORT_EXPIRY.toMillis() + 50);
        Integer result = intCache.computeIfPresentOrDefault("computeODExpired", 77, (k, v) -> v * 2);
        assertEquals(77, result, "鍵過期時應返回預設值");
        assertEquals(77, intCache.get("computeODExpired"), "緩存中應存儲預設值");
    }


    @Test
    @DisplayName("測試 computeIfPresentOrDefault 方法：鍵存在但計算函數返回 null")
    void computeIfPresentOrDefault_whenKeyExistsAndFunctionReturnsNull_thenStoresNull() {
        intCache.set("computeODToNull", 100);
        Integer result = intCache.computeIfPresentOrDefault("computeODToNull", 999, (k, v) -> null);
        assertNull(result, "計算結果為 null 時應返回 null");
        assertNull(intCache.get("computeODToNull"), "緩存中應存儲 null 值");
        assertNull(intCache.checkInfo("computeODToNull"), "CacheInfo 不應存在");
    }


    @Test
    @DisplayName("測試 computeIfPresentOrDefault 方法：鍵不存在且計算函數為 null（不應調用）")
    void computeIfPresentOrDefault_whenKeyDoesNotExistAndFunctionIsNull_thenUsesDefaultValue() {
        Integer result = intCache.computeIfPresentOrDefault("nonExistentComputeODNullFunc", 88, null);
        assertEquals(88, result, "鍵不存在時應返回預設值，即使函數為 null");
        assertEquals(88, intCache.get("nonExistentComputeODNullFunc"), "緩存中應存儲預設值");
    }


    @Test
    @DisplayName("測試 computeIfPresentOrDefault 方法：使用無效過期時間")
    void computeIfPresentOrDefault_whenInvalidExpiry_thenThrowsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
                                                       intCache.computeIfPresentOrDefault("key", 1, Duration.ZERO, (k, v) -> v + 1);
                                                   }
        );
        assertEquals("過期時間必須大於0", ex.getMessage());

        ex = assertThrows(IllegalArgumentException.class, () -> {
                              intCache.computeIfPresentOrDefault("key", 1, Duration.ofSeconds(-1), (k, v) -> v + 1);
                          }
        );
        assertEquals("過期時間必須大於0", ex.getMessage());
    }


    @Test
    @DisplayName("測試 destroy 方法：銷毀後緩存應為空且調度器關閉")
    void destroy_whenCalled_thenCacheIsEmptyAndSchedulerShutdown() {
        CacheConcurrentHashMap<String, String> cleanupCache = createCache(true, MEDIUM_EXPIRY, LONG_EXPIRY, CLEANUP_INTERVAL);
        cleanupCache.set("keyDestroy", "valueDestroy");
        assertNotNull(cleanupCache.check("keyDestroy"));

        cleanupCache.destroy();

        assertNull(cleanupCache.check("keyDestroy"), "銷毀後緩存應為空");
        assertDoesNotThrow(() -> cleanupCache.set("keyAfterDestroy", "value"), "銷毀後基本操作不應因資源關閉而異常（除非實現改變）");
    }


    @Test
    @DisplayName("測試異步獲取：使用 Mono 包裝 get 操作（示例）")
    void asyncGet_whenUsingMono_thenWorksAsynchronously() {
        String key = "asyncKey";
        String value = "asyncValue";
        cache.set(key, value);

        Mono<String> asyncResult = Mono.fromCallable(() -> cache.get(key)).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic());

        StepVerifier.create(asyncResult).expectNext(value).verifyComplete();
    }
}
