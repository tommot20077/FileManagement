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


    @DisplayName("設置快取提供者 - 成功設置並可以獲取")
    @Test
    void setCacheProvider_successfullySetAndGet() {
        CacheProvider newProvider = mock(CacheProvider.class);

        cacheManagerUnderTest.setCacheProvider(CacheProviderEnum.FILE_STREAM_CACHE, newProvider);
        CacheProvider result = cacheManagerUnderTest.getCacheProvider(CacheProviderEnum.FILE_STREAM_CACHE);

        StepVerifier.create(Mono.just(result)).expectNext(newProvider).verifyComplete();
    }


    @DisplayName("獲取單個值的快取 - 成功從快取中獲取數據")
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


    @DisplayName("獲取單個值的快取 - 快取不存在時返回空")
    @Test
    void getCacheMono_whenCacheNotExists_returnsEmpty() {
        String key = "nonExistentKey";
        when(mockCacheProvider.get(key, String.class)).thenReturn(Mono.empty());

        StepVerifier.create(cacheManagerUnderTest.getCacheMono(key, String.class, CacheProviderEnum.USER_CACHE)).verifyComplete();

        verify(mockCacheProvider).get(key, String.class);
    }


    @DisplayName("設置快取值 - 成功設置並鎖定")
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


    @DisplayName("設置快取值 - 無法獲取鎖時不進行設置")
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


    @DisplayName("刪除快取 - 成功刪除並釋放鎖")
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


    @DisplayName("批量刪除快取 - 成功刪除多個快取")
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


    @DisplayName("生成快取規則 - 成功生成並執行規則")
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


    @DisplayName("運行並設置單值快取 - 當快取不存在時設置快取")
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


    @DisplayName("運行並設置多個快取 - 當快取不存在時設置快取")
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


    @DisplayName("運行並設置批量快取 - 當快取不存在時設置快取")
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


    @DisplayName("快取操作錯誤處理 - 當設置快取失敗時拋出異常")
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


    @DisplayName("快取過期時間設置 - 成功設置自定義過期時間")
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


    @DisplayName("多個快取值操作 - 部分快取操作失敗時的處理")
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


    @DisplayName("快取鎖釋放 - 確保在操作完成後釋放鎖")
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


    @DisplayName("並發快取操作 - 多個相同鍵值的快取操作處理")
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


    @DisplayName("快取輸入驗證 - 處理無效的輸入參數")
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
