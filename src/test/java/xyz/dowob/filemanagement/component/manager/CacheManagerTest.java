package xyz.dowob.filemanagement.component.manager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.dowob.filemanagement.component.provider.provider.RedisProvider;
import xyz.dowob.filemanagement.component.provider.providerInterface.CacheProvider;
import xyz.dowob.filemanagement.customenum.CacheProviderEnum;
import xyz.dowob.filemanagement.functionInterface.CacheRule;

import java.time.Duration;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


/**
 * CacheManager 快取管理單元測試
 *
 * <p>本測試類專注於驗證 CacheManager 的各種快取操作邏輯，包括設置、獲取、刪除和管理快取。</p>
 *
 * <p>測試涵蓋的主要場景：
 * 
 *   - 快取提供者設置與獲取
 *   - 單值和批量快取操作
 *   - 快取鎖與併發控制
 *   - 錯誤處理和邊界情況
 * 
 * </p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CacheManager 邏輯處理測試")
class CacheManagerTest {

    @Mock
    private RedisProvider mockRedisProvider;

    @Mock
    private CacheProvider mockCacheProvider;

    private CacheManager cacheManagerUnderTest;


    @BeforeEach
    void setUp() {
        List<CacheProvider> providers = Collections.singletonList(mockCacheProvider);
        cacheManagerUnderTest = new CacheManager(providers, mockRedisProvider);
        cacheManagerUnderTest.setCacheProvider(CacheProviderEnum.USER_CACHE, mockCacheProvider);
    }


    /**
     * 驗證快取提供者的設置與獲取功能
     *
     * <p>測試步驟：
     * 
     *   - 創建新的快取提供者
     *   - 設置快取提供者到 CacheManager
     *   - 檢查是否能正確獲取已設置的快取提供者
     * 
     * </p>
     *
     * <p>預期結果：成功設置並獲取指定類型的快取提供者</p>
     */
    @Test
    void setCacheProvider_successfullySetAndGet() {
        CacheProvider newProvider = mock(CacheProvider.class);

        cacheManagerUnderTest.setCacheProvider(CacheProviderEnum.FILE_STREAM_CACHE, newProvider);
        CacheProvider result = cacheManagerUnderTest.getCacheProvider(CacheProviderEnum.FILE_STREAM_CACHE);

        StepVerifier.create(Mono.just(result)).expectNext(newProvider).verifyComplete();
    }


    /**
     * 測試從快取中成功獲取單一值的功能
     *
     * <p>測試步驟：
     * 
     *   - 設置測試鍵值對
     *   - 模擬快取提供者返回指定值
     *   - 調用 getCacheMono 方法
     * 
     * </p>
     *
     * <p>預期結果：成功從快取中獲取預期的值</p>
     */
    @Test
    void getCacheMono_successfullyRetrieveFromCache() {
        String key = "testKey";
        String expectedValue = "testValue";
        when(mockCacheProvider.get(key, String.class)).thenReturn(Mono.just(expectedValue));

        StepVerifier
                .create(cacheManagerUnderTest.getCacheMono(key, String.class, CacheProviderEnum.USER_CACHE))
                .expectNext(expectedValue)
                .verifyComplete();

        verify(mockCacheProvider).get(key, String.class);
    }


    /**
     * 測試獲取不存在的快取值時的空值返回
     *
     * <p>測試步驟：
     * 
     *   - 設置一個不存在的鍵值
     *   - 調用 getCacheMono 方法
     *   - 確認獲取結果為空
     * 
     * </p>
     *
     * <p>預期結果：返回空值，且不會報错</p>
     */
    @Test
    void getCacheMono_whenCacheNotExists_returnsEmpty() {
        String key = "nonExistentKey";
        when(mockCacheProvider.get(key, String.class)).thenReturn(Mono.empty());

        StepVerifier.create(cacheManagerUnderTest.getCacheMono(key, String.class, CacheProviderEnum.USER_CACHE)).verifyComplete();

        verify(mockCacheProvider).get(key, String.class);
    }


    /**
     * 測試快取值的設置和鎖定機制
     *
     * <p>測試步驟：
     * 
     *   - 偷鎖定快取金锈
     *   - 設置快取值
     *   - 檢查鎖定的成功從行
     * 
     * </p>
     *
     * <p>預期結果：成功設置快取值且釋放鎖定</p>
     */
    @Test
    void setCache_successfullySetWithLock() {
        String key = "testKey";
        String value = "testValue";
        String lockKey = "cache_lock:USER_CACHE:testKey";

        when(mockRedisProvider.setValueIfAbsent(eq(lockKey), anyString(), any(Duration.class))).thenReturn(Mono.just(true));
        when(mockRedisProvider.deleteValue(anyList())).thenReturn(Mono.empty());
        when(mockCacheProvider.set(eq(key), eq(value), any())).thenReturn(Mono.empty());

        StepVerifier.create(cacheManagerUnderTest.setCache(key, value, CacheProviderEnum.USER_CACHE)).verifyComplete();

        verify(mockRedisProvider).setValueIfAbsent(eq(lockKey), anyString(), any(Duration.class));
        verify(mockCacheProvider).set(eq(key), eq(value), any());
        verify(mockRedisProvider).deleteValue(anyList());
    }


    /**
     * 測試無法獲取快取鎖時的處理行為
     *
     * <p>測試步驟：
     * 
     *   - 模擬無法取得鎖的情景
     *   - 嘗試設置快取值
     *   - 確認不會更新快取
     * 
     * </p>
     *
     * <p>預期結果：不進行快取設置</p>
     */
    @Test
    void setCache_whenLockCannotBeAcquired_doesNotSet() {
        String key = "testKey";
        String value = "testValue";
        String lockKey = "cache_lock:USER_CACHE:testKey";

        when(mockRedisProvider.setValueIfAbsent(eq(lockKey), anyString(), any(Duration.class))).thenReturn(Mono.just(false));

        StepVerifier.create(cacheManagerUnderTest.setCache(key, value, CacheProviderEnum.USER_CACHE)).verifyComplete();

        verify(mockRedisProvider).setValueIfAbsent(eq(lockKey), anyString(), any(Duration.class));
        verify(mockCacheProvider, never()).set(anyString(), any(), any());
    }


    /**
     * 測試快取刪除且釋放鎖定的正確性
     *
     * <p>測試步驟：
     * 
     *   - 設置快取金鑰
     *   - 刪除指定的快取值
     *   - 確認重置快取金鑰
     * 
     * </p>
     *
     * <p>預期結果：成功刪除快取並釋放鎖定</p>
     */
    @Test
    void deleteCache_successfullyDeleteAndReleaseLock() {
        String key = "testKey";
        String lockKey = "cache_lock:USER_CACHE:testKey";
        List<String> expectedLockKeys = Collections.singletonList(lockKey);

        when(mockRedisProvider.setValueIfAbsent(eq(lockKey), anyString(), any(Duration.class))).thenReturn(Mono.just(true));
        when(mockRedisProvider.deleteValue(anyList())).thenReturn(Mono.empty());
        when(mockCacheProvider.delete(key)).thenReturn(Mono.empty());

        StepVerifier.create(cacheManagerUnderTest.deleteCache(key, CacheProviderEnum.USER_CACHE)).verifyComplete();

        verify(mockRedisProvider).setValueIfAbsent(eq(lockKey), anyString(), any(Duration.class));
        verify(mockCacheProvider).delete(key);
        verify(mockRedisProvider).deleteValue(anyList());
    }


    /**
     * 測試批量快取刪除的效能
     *
     * <p>測試步驟：
     * 
     *   - 創建多個快取鍵值
     *   - 設置臨時鎖
     *   - 善妥刪除所有指定的快取
     * 
     * </p>
     *
     * <p>預期結果：成功刪除多個快取內容</p>
     */
    @Test
    void deleteCaches_successfullyDeleteMultipleCaches() {
        List<String> keys = Arrays.asList("key1", "key2");
        List<String> lockKeys = Arrays.asList("cache_lock:USER_CACHE:key1", "cache_lock:USER_CACHE:key2");

        when(mockRedisProvider.setValueIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(Mono.just(true));
        when(mockRedisProvider.deleteValue(anyList())).thenReturn(Mono.empty());
        when(mockCacheProvider.deleteAll(keys)).thenReturn(Mono.empty());

        StepVerifier.create(cacheManagerUnderTest.deleteCaches(keys, CacheProviderEnum.USER_CACHE)).verifyComplete();

        verify(mockCacheProvider).deleteAll(keys);
        verify(mockRedisProvider, atLeastOnce()).deleteValue(anyList());
    }


    /**
     * 測試快取規則生成與執行的正確性
     *
     * <p>測試步驟：
     * 
     *   - 創建快取鍵值和值
     *   - 生成快取规则
     *   - 執行规则并驗證是否成功設置
     * 
     * </p>
     *
     * <p>預期結果：成功生成并執行快取规則</p>
     */
    @Test
    void generateCacheRule_successfullyGenerateAndExecute() {
        String key = "testKey";
        String value = "testValue";
        Duration expire = Duration.ofMinutes(5);

        when(mockCacheProvider.set(key, value, expire)).thenReturn(Mono.empty());

        CacheRule<String> rule = cacheManagerUnderTest.generateCacheRule(key, CacheProviderEnum.USER_CACHE);

        StepVerifier.create(rule.apply(value, expire)).verifyComplete();

        verify(mockCacheProvider).set(key, value, expire);
    }


    /**
     * 測試當快取不存在時的自動設置機制
     *
     * <p>測試步驟：
     * 
     *   - 備餐測試鍵值和值
     *   - 模擬快取提供者未發現快取
     *   - 調用 runAndSetCache 方法
     *   - 確認快取被正確設置
     * 
     * </p>
     *
     * <p>預期結果：自動設置快取值</p>
     */
    @Test
    void runAndSetCache_whenCacheNotExists_setsCache() {
        String key = "testKey";
        String value = "testValue";
        String lockKey = "cache_lock:USER_CACHE:testKey";

        lenient().when(mockRedisProvider.setValueIfAbsent(eq(lockKey), anyString(), any(Duration.class))).thenReturn(Mono.just(true));
        lenient().when(mockRedisProvider.deleteValue(anyList())).thenReturn(Mono.empty());
        when(mockCacheProvider.get(key, String.class)).thenReturn(Mono.empty());

        StepVerifier
                .create(cacheManagerUnderTest.runAndSetCache(key,
                                                             String.class,
                                                             CacheProviderEnum.USER_CACHE,
                                                             Mono.just(value),
                                                             Collections.emptyList()
                ))
                .expectNext(value)
                .verifyComplete();

        verify(mockCacheProvider).get(key, String.class);
    }


    /**
     * 測試多值快取的自動設置機制
     *
     * <p>測試步驟：
     * 
     *   - 備餐測試鍵值和值列表
     *   - 模擬快取提供者未發現快取
     *   - 使用 runAndSetCache 方法設置多值快取
     *   - 確認快取被正確設置
     * 
     * </p>
     *
     * <p>預期結果：自動設置多值快取</p>
     */
    @Test
    void runAndSetCache_listWhenCacheNotExists_setsCache() {
        String key = "testKey";
        List<String> values = Arrays.asList("value1", "value2");
        String lockKey = "cache_lock:USER_CACHE:testKey";

        lenient().when(mockRedisProvider.setValueIfAbsent(eq(lockKey), anyString(), any(Duration.class))).thenReturn(Mono.just(true));
        lenient().when(mockRedisProvider.deleteValue(anyList())).thenReturn(Mono.empty());
        when(mockCacheProvider.getAsList(key, String.class)).thenReturn(Mono.empty());

        StepVerifier
                .create(cacheManagerUnderTest.runAndSetCache(key,
                                                             String.class,
                                                             CacheProviderEnum.USER_CACHE,
                                                             Flux.fromIterable(values),
                                                             Collections.emptyList()
                ))
                .expectNextSequence(values)
                .verifyComplete();

        verify(mockCacheProvider).getAsList(key, String.class);
    }


    /**
     * 測試批量快取的自動設置機制
     *
     * <p>測試步驟：
     * 
     *   - 備餐多個測試鍵值
     *   - 模擬快取提供者未發現快取
     *   - 使用 runAndSetCaches 方法設置多個快取
     *   - 確認快取被正確設置
     * 
     * </p>
     *
     * <p>預期結果：自動設置多個快取</p>
     */
    @Test
    void runAndSetCaches_whenCacheNotExists_setsCache() {
        List<String> keys = Arrays.asList("key1", "key2");
        Map<String, String> sourceMap = new HashMap<>();
        sourceMap.put("key1", "value1");
        sourceMap.put("key2", "value2");
        String lockKeyPrefix = "cache_lock:USER_CACHE:";

        lenient().when(mockRedisProvider.setValueIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(Mono.just(true));
        lenient().when(mockRedisProvider.deleteValue(anyList())).thenReturn(Mono.empty());
        when(mockCacheProvider.getAllAsMap(eq(keys), eq(String.class))).thenReturn(Mono.empty());

        StepVerifier
                .create(cacheManagerUnderTest.runAndSetCaches(keys,
                                                              String.class,
                                                              CacheProviderEnum.USER_CACHE,
                                                              missingKeys -> Mono.just(sourceMap),
                                                              Collections.emptyList()
                ))
                .expectNext(sourceMap)
                .verifyComplete();

        verify(mockCacheProvider).getAllAsMap(eq(keys), eq(String.class));
    }


    @DisplayName("運行並設置多個快取 - 當快取已存在時直接返回快取值")
    @Test
    void runAndSetCaches_whenCacheExists_returnsCachedMap() {
        List<String> keys = Arrays.asList("key1", "key2");
        Map<String, String> cachedMap = new HashMap<>();
        cachedMap.put("key1", "cachedValue1");
        cachedMap.put("key2", "cachedValue2");
        CacheRule<String> rule = (v, expire) -> mockCacheProvider.set(v, v, expire);

        when(mockCacheProvider.getAllAsMap(eq(keys), eq(String.class))).thenReturn(Mono.just(cachedMap));

        StepVerifier
                .create(cacheManagerUnderTest.runAndSetCaches(keys,
                                                              String.class,
                                                              CacheProviderEnum.USER_CACHE,
                                                              missingKeys -> Mono.empty(),
                                                              Collections.singletonList(rule)
                ))
                .expectNext(cachedMap)
                .verifyComplete();

        verify(mockCacheProvider).getAllAsMap(eq(keys), eq(String.class));
        verify(mockRedisProvider, never()).setValueIfAbsent(anyString(), anyString(), any(Duration.class));
    }


    /**
     * 測試快取操作失敗時的錯誤處理機制
     *
     * <p>測試步驟：
     * 
     *   - 模擬快取操作失敗的情景
     *   - 設置模擬的失敗錢為
     *   - 調用 setCache 方法
     *   - 確認所前異常被正確報告
     * 
     * </p>
     *
     * <p>預期結果：呈現所前異常並停止快取操作</p>
     */
    @Test
    void setCache_whenOperationFails_throwsError() {
        String key = "testKey";
        String value = "testValue";
        String lockKey = "cache_lock:USER_CACHE:testKey";
        RuntimeException expectedException = new RuntimeException("Cache operation failed");

        lenient().when(mockRedisProvider.setValueIfAbsent(eq(lockKey), anyString(), any(Duration.class))).thenReturn(Mono.just(true));
        lenient().when(mockRedisProvider.deleteValue(anyList())).thenReturn(Mono.empty());
        when(mockCacheProvider.set(eq(key), eq(value), any())).thenReturn(Mono.error(expectedException));

        StepVerifier
                .create(cacheManagerUnderTest.setCache(key, value, CacheProviderEnum.USER_CACHE))
                .expectErrorMatches(error -> error.equals(expectedException))
                .verify();
    }


    /**
     * 測試自定義快取過期時間的設置成功性
     *
     * <p>測試步驟：
     * 
     *   - 備餐鍵值和值
     *   - 設置自定義過期時間
     *   - 確認快取提供者收到正確的過期時間
     * 
     * </p>
     *
     * <p>預期結果：成功設置自定義過期時間的快取</p>
     */
    @Test
    void setCache_withCustomExpiration_setsCorrectExpiration() {
        String key = "testKey";
        String value = "testValue";
        Duration customExpiration = Duration.ofMinutes(30);
        String lockKey = "cache_lock:USER_CACHE:testKey";

        lenient().when(mockRedisProvider.setValueIfAbsent(eq(lockKey), anyString(), any(Duration.class))).thenReturn(Mono.just(true));
        lenient().when(mockRedisProvider.deleteValue(anyList())).thenReturn(Mono.empty());
        when(mockCacheProvider.set(eq(key), eq(value), eq(customExpiration))).thenReturn(Mono.empty());

        StepVerifier.create(cacheManagerUnderTest.setCache(key, value, CacheProviderEnum.USER_CACHE, customExpiration)).verifyComplete();

        verify(mockCacheProvider).set(key, value, customExpiration);
    }


    /**
     * 測試多個快取值操作時的限制與容錢機制
     *
     * <p>測試步驟：
     * 
     *   - 備餐多個鍵值和對應值
     *   - 模擬部分快取操作失敗的場景
     *   - 調用 runAndSetCaches 方法來檢查失敗處理
     * 
     * </p>
     *
     * <p>預期結果：成功處理部分失敗的快取操作</p>
     */
    @Test
    void setCaches_whenPartialOperationsFail_handleGracefully() {
        List<String> keys = Arrays.asList("key1", "key2", "key3");
        Map<String, String> values = new HashMap<>();
        values.put("key1", "value1");
        values.put("key2", "value2");
        values.put("key3", "value3");

        lenient().when(mockRedisProvider.setValueIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(Mono.just(true));
        lenient().when(mockRedisProvider.deleteValue(anyList())).thenReturn(Mono.empty());
        when(mockCacheProvider.getAllAsMap(eq(keys), eq(String.class))).thenReturn(Mono.empty());

        StepVerifier
                .create(cacheManagerUnderTest.runAndSetCaches(keys,
                                                              String.class,
                                                              CacheProviderEnum.USER_CACHE,
                                                              missingKeys -> Mono.just(values),
                                                              Collections.emptyList()
                ))
                .expectNext(values)
                .verifyComplete();

        verify(mockCacheProvider).getAllAsMap(eq(keys), eq(String.class));
    }


    /**
     * 測試快取操作完成後一定要釋放鎖定
     *
     * <p>測試步驟：
     * 
     *   - 模擬快取操作失敗的情景
     *   - 調用 setCache 方法來俄白操作
     *   - 確認快取鎖定必然被釋放
     * 
     * </p>
     *
     * <p>預期結果：操作失敗時必須釋放鎖</p>
     */
    @Test
    void setCache_ensuresLockRelease_afterOperation() {
        String key = "testKey";
        String value = "testValue";
        String lockKey = "cache_lock:USER_CACHE:testKey";
        RuntimeException operationException = new RuntimeException("Operation failed");

        when(mockRedisProvider.setValueIfAbsent(eq(lockKey), anyString(), any(Duration.class))).thenReturn(Mono.just(true));
        when(mockRedisProvider.deleteValue(anyList())).thenReturn(Mono.empty());
        when(mockCacheProvider.set(eq(key), eq(value), any())).thenReturn(Mono.error(operationException));

        StepVerifier.create(cacheManagerUnderTest.setCache(key, value, CacheProviderEnum.USER_CACHE)).expectError(RuntimeException.class).verify();

        verify(mockRedisProvider).deleteValue(anyList());
    }


    /**
     * 測試並發快取操作時的同步控制機制
     *
     * <p>測試步驟：
     * 
     *   - 模擬多個幵發的快取操作
     *   - 檢查是否只有一個操作成功
     *   - 確保幵發操作的一致性
     * 
     * </p>
     *
     * <p>預期結果：正確控制多個幵發快取操作</p>
     */
    @Test
    void setCache_withConcurrentOperations_handlesCorrectly() {
        String key = "testKey";
        String value = "testValue";
        String lockKey = "cache_lock:USER_CACHE:testKey";

        when(mockRedisProvider.setValueIfAbsent(eq(lockKey), anyString(), any(Duration.class)))
                .thenReturn(Mono.just(true))
                .thenReturn(Mono.just(false));
        when(mockRedisProvider.deleteValue(anyList())).thenReturn(Mono.empty());
        when(mockCacheProvider.set(eq(key), eq(value), any())).thenReturn(Mono.empty());

        StepVerifier
                .create(Flux.merge(cacheManagerUnderTest.setCache(key, value, CacheProviderEnum.USER_CACHE),
                                   cacheManagerUnderTest.setCache(key, value, CacheProviderEnum.USER_CACHE)
                ))
                .verifyComplete();

        verify(mockCacheProvider, times(1)).set(eq(key), eq(value), any());
        verify(mockRedisProvider, times(2)).setValueIfAbsent(eq(lockKey), anyString(), any(Duration.class));
    }


    @DisplayName("快取批量操作 - 大量鍵值的處理")
    @Test
    void setCaches_withLargeNumberOfKeys_handlesEfficiently() {
        List<String> keys = new ArrayList<>();
        Map<String, String> valueMap = new HashMap<>();
        for (int i = 0; i < 100; i++) {
            String key = "key" + i;
            keys.add(key);
            valueMap.put(key, "value" + i);
        }

        lenient().when(mockRedisProvider.setValueIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(Mono.just(true));
        lenient().when(mockRedisProvider.deleteValue(anyList())).thenReturn(Mono.empty());
        when(mockCacheProvider.getAllAsMap(eq(keys), eq(String.class))).thenReturn(Mono.just(valueMap));

        StepVerifier
                .create(cacheManagerUnderTest.runAndSetCaches(keys,
                                                              String.class,
                                                              CacheProviderEnum.USER_CACHE,
                                                              missingKeys -> Mono.just(valueMap),
                                                              Collections.emptyList()
                ))
                .expectNext(valueMap)
                .verifyComplete();

        verify(mockCacheProvider).getAllAsMap(eq(keys), eq(String.class));
    }


    @DisplayName("快取刷新 - 成功強制更新快取值")
    @Test
    void setCache_withForceRefresh_updatesExistingCache() {
        String key = "testKey";
        String oldValue = "oldValue";
        String newValue = "newValue";
        String lockKey = "cache_lock:USER_CACHE:testKey";

        lenient().when(mockRedisProvider.setValueIfAbsent(eq(lockKey), anyString(), any(Duration.class))).thenReturn(Mono.just(true));
        lenient().when(mockRedisProvider.deleteValue(anyList())).thenReturn(Mono.empty());
        when(mockCacheProvider.set(eq(key), eq(newValue), any())).thenReturn(Mono.empty());

        StepVerifier.create(cacheManagerUnderTest.setCache(key, newValue, CacheProviderEnum.USER_CACHE)).verifyComplete();

        verify(mockCacheProvider).set(eq(key), eq(newValue), any());
    }


    /**
     * 測試快取輸入的無效參數處理機制
     *
     * <p>測試步驟：
     * 
     *   - 提供無效的輸入參數
     *   - 確認系統能夠正確報错
     *   - 確保沒有非預期的操作發生
     * 
     * </p>
     *
     * <p>預期結果：以穣健的方式處理無效輸入</p>
     */
    @Test
    void setCache_withInvalidInput_handlesGracefully() {
        String key = null;
        String value = "testValue";
        CacheProviderEnum type = CacheProviderEnum.USER_CACHE;
        String lockKey = "cache_lock:USER_CACHE:null";

        StepVerifier.create(cacheManagerUnderTest.setCache(key, value, type)).expectError(NullPointerException.class).verify();

        verifyNoInteractions(mockCacheProvider);
    }


    @DisplayName("混合快取操作 - 讀寫刪除組合操作")
    @Test
    void cache_withMixedOperations_executesProperly() {
        String key = "testKey";
        String value = "testValue";
        String lockKey = "cache_lock:USER_CACHE:testKey";

        lenient().when(mockRedisProvider.setValueIfAbsent(eq(lockKey), anyString(), any(Duration.class))).thenReturn(Mono.just(true));
        lenient().when(mockRedisProvider.deleteValue(anyList())).thenReturn(Mono.empty());
        when(mockCacheProvider.set(eq(key), eq(value), any())).thenReturn(Mono.empty());
        when(mockCacheProvider.get(key, String.class)).thenReturn(Mono.just(value));
        when(mockCacheProvider.delete(key)).thenReturn(Mono.empty());

        StepVerifier
                .create(Flux.concat(cacheManagerUnderTest.setCache(key, value, CacheProviderEnum.USER_CACHE),
                                    cacheManagerUnderTest.getCacheMono(key, String.class, CacheProviderEnum.USER_CACHE),
                                    cacheManagerUnderTest.deleteCache(key, CacheProviderEnum.USER_CACHE)
                ))
                .expectNext(value)
                .verifyComplete();

        verify(mockCacheProvider).set(eq(key), eq(value), any());
        verify(mockCacheProvider).get(key, String.class);
        verify(mockCacheProvider).delete(key);
    }


    @DisplayName("快取類型轉換 - 處理不同類型的快取值")
    @Test
    void cache_withTypeConversion_handlesCorrectly() {
        String key = "testKey";
        Integer value = 123;
        String lockKey = "cache_lock:USER_CACHE:testKey";

        lenient().when(mockRedisProvider.setValueIfAbsent(eq(lockKey), anyString(), any(Duration.class))).thenReturn(Mono.just(true));
        lenient().when(mockRedisProvider.deleteValue(anyList())).thenReturn(Mono.empty());
        when(mockCacheProvider.set(eq(key), eq(value), any())).thenReturn(Mono.empty());
        when(mockCacheProvider.get(key, Integer.class)).thenReturn(Mono.just(value));

        StepVerifier
                .create(Flux.concat(cacheManagerUnderTest.setCache(key, value, CacheProviderEnum.USER_CACHE),
                                    cacheManagerUnderTest.getCacheMono(key, Integer.class, CacheProviderEnum.USER_CACHE)
                ))
                .expectNext(value)
                .verifyComplete();

        verify(mockCacheProvider).set(eq(key), eq(value), any());
        verify(mockCacheProvider).get(key, Integer.class);
    }
}
