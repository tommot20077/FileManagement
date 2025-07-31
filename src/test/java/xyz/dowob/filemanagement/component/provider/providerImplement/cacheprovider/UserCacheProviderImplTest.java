package xyz.dowob.filemanagement.component.provider.providerImplement.cacheprovider;

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
import xyz.dowob.filemanagement.config.properties.CacheProperties;

import java.time.Duration;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * UserCacheProviderImpl 測試類別。
 * 
 * <p>測試 UserCacheProviderImpl 的使用者資訊緩存功能，包括：
 * <ul>
 * <li>構造函數初始化及參數驗證</li>
 * <li>單一使用者資訊緩存的獲取與設定</li>
 * <li>批量使用者資訊緩存的獲取與設定</li>
 * <li>緩存刪除操作（單一及批量）</li>
 * <li>Hash 結構的緩存操作</li>
 * <li>過期時間管理（預設及自定）</li>
 * <li>緩存命中與未命中情況處理</li>
 * </ul>
 * 
 * <p>測試涵蓋 Redis Hash 操作的所有核心功能，包含空鍵值處理、
 * 批量操作及異常情況。透過反應式程式測試確保緩存提供者的正確性和可靠性。
 * 
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserCacheProvider 邏輯處理測試")
class UserCacheProviderImplTest {

    private final String CACHE_PREFIX = "testUserPrefix:";

    private final Duration DEFAULT_EXPIRE_TIME = Duration.ofHours(1);

    @Mock
    private RedisProvider mockRedisProvider;

    @Mock
    private CacheProperties mockCacheProperties;

    private UserCacheProviderImpl userCacheProviderImplUnderTest;


    @BeforeEach
    void setUp() {
        when(mockCacheProperties.getUserInfoCachePrefix()).thenReturn(CACHE_PREFIX);
        when(mockCacheProperties.getUserInfoCacheExpireTime()).thenReturn(DEFAULT_EXPIRE_TIME);
        userCacheProviderImplUnderTest = new UserCacheProviderImpl(mockRedisProvider, mockCacheProperties);
    }

    @Test
    @DisplayName("構造函數初始化 - 成功")
    void constructor_validProperties_initializesSuccessfully() {
        assertThat(userCacheProviderImplUnderTest.getDefaultExpire()).isEqualTo(DEFAULT_EXPIRE_TIME);
    }

    @Test
    @DisplayName("構造函數初始化 - ExpireTime 為零或負數 - 拋出 IllegalArgumentException")
    void constructor_invalidExpireTime_throwsIllegalArgumentException() {
        when(mockCacheProperties.getUserInfoCacheExpireTime()).thenReturn(Duration.ZERO);
        assertThatThrownBy(() -> new UserCacheProviderImpl(mockRedisProvider, mockCacheProperties))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("用戶資訊緩存過期時間必須大於0");

        when(mockCacheProperties.getUserInfoCacheExpireTime()).thenReturn(Duration.ofSeconds(-10));
        assertThatThrownBy(() -> new UserCacheProviderImpl(mockRedisProvider, mockCacheProperties))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("用戶資訊緩存過期時間必須大於0");
    }

    @Test
    @DisplayName("get - 緩存命中 - 返回 Mono<String>")
    void get_cacheHit_returnsMonoWithValue() {
        String key = "user1";
        String expectedValue = "userData1";
        when(mockRedisProvider.getHashMap(CACHE_PREFIX, key, String.class)).thenReturn(Mono.just(expectedValue));

        Mono<String> result = userCacheProviderImplUnderTest.get(key, String.class);

        StepVerifier.create(result).expectNext(expectedValue).verifyComplete();
    }

    @Test
    @DisplayName("get - 緩存未命中 - 返回 Mono.empty")
    void get_cacheMiss_returnsMonoEmpty() {
        String key = "userNonExistent";
        when(mockRedisProvider.getHashMap(CACHE_PREFIX, key, String.class)).thenReturn(Mono.empty());

        Mono<String> result = userCacheProviderImplUnderTest.get(key, String.class);

        StepVerifier.create(result).verifyComplete();
    }

    @Test
    @DisplayName("getAllAsMap - hashKeys 為 null - 調用 redisProvider.getAllHashMap")
    void getAllAsMap_nullKeys_callsGetAllHashMap() {
        Map.Entry<String, String> entry1 = new AbstractMap.SimpleEntry<>("user1", "data1");
        Map.Entry<String, String> entry2 = new AbstractMap.SimpleEntry<>("user2", "data2");
        when(mockRedisProvider.getAllHashMap(CACHE_PREFIX, String.class, String.class)).thenReturn(Flux.just(entry1, entry2));

        Mono<Map<String, String>> result = userCacheProviderImplUnderTest.getAllAsMap(null, String.class);

        StepVerifier.create(result).assertNext(map -> {
            assertThat(map).hasSize(2).containsEntry("user1", "data1").containsEntry("user2", "data2");
        }).verifyComplete();
        verify(mockRedisProvider).getAllHashMap(CACHE_PREFIX, String.class, String.class);
    }

    @Test
    @DisplayName("getAllAsMap - hashKeys 為空集合 - 調用 redisProvider.getAllHashMap")
    void getAllAsMap_emptyKeys_callsGetAllHashMap() {
        when(mockRedisProvider.getAllHashMap(CACHE_PREFIX, String.class, String.class)).thenReturn(Flux.empty());

        Mono<Map<String, String>> result = userCacheProviderImplUnderTest.getAllAsMap(Collections.emptyList(), String.class);
        StepVerifier.create(result).assertNext(map -> assertThat(map).isEmpty()).verifyComplete();
        verify(mockRedisProvider).getAllHashMap(CACHE_PREFIX, String.class, String.class);
    }

    @Test
    @DisplayName("getAllAsMap - hashKeys 有值 - 調用 redisProvider.getHashMap 多次")
    void getAllAsMap_withKeys_callsGetHashMapForEachKey() {
        String key1 = "user1";
        String value1 = "data1";
        String key2 = "user2";
        String key3 = "user3";
        String value3 = "data3";
        Collection<String> keys = Arrays.asList(key1, key2, key3);

        when(mockRedisProvider.getHashMap(CACHE_PREFIX, key1, String.class)).thenReturn(Mono.just(value1));
        when(mockRedisProvider.getHashMap(CACHE_PREFIX, key2, String.class)).thenReturn(Mono.empty());
        when(mockRedisProvider.getHashMap(CACHE_PREFIX, key3, String.class)).thenReturn(Mono.just(value3));

        Mono<Map<String, String>> result = userCacheProviderImplUnderTest.getAllAsMap(keys, String.class);

        StepVerifier.create(result).assertNext(map -> {
            assertThat(map).hasSize(2).containsEntry(key1, value1).containsEntry(key3, value3).doesNotContainKey(key2);
        }).verifyComplete();
        verify(mockRedisProvider).getHashMap(CACHE_PREFIX, key1, String.class);
        verify(mockRedisProvider).getHashMap(CACHE_PREFIX, key2, String.class);
        verify(mockRedisProvider).getHashMap(CACHE_PREFIX, key3, String.class);
    }

    @Test
    @DisplayName("set - expire 為 null - 使用預設過期時間")
    void set_nullExpire_usesDefaultExpireTime() {
        String key = "userSet1";
        String value = "dataSet1";
        when(mockRedisProvider.setHashMap(CACHE_PREFIX, key, value, DEFAULT_EXPIRE_TIME)).thenReturn(Mono.empty());

        Mono<Void> result = userCacheProviderImplUnderTest.set(key, value, null);
        StepVerifier.create(result).verifyComplete();
        verify(mockRedisProvider).setHashMap(CACHE_PREFIX, key, value, DEFAULT_EXPIRE_TIME);
    }

    @Test
    @DisplayName("set - expire 有值 - 使用提供過期時間")
    void set_withExpire_usesProvidedExpireTime() {
        String key = "userSet2";
        String value = "dataSet2";
        Duration customExpire = Duration.ofMinutes(30);
        when(mockRedisProvider.setHashMap(CACHE_PREFIX, key, value, customExpire)).thenReturn(Mono.empty());

        Mono<Void> result = userCacheProviderImplUnderTest.set(key, value, customExpire);
        StepVerifier.create(result).verifyComplete();
        verify(mockRedisProvider).setHashMap(CACHE_PREFIX, key, value, customExpire);
    }

    @Test
    @DisplayName("setAll - expire 為 null - 使用預設過期時間")
    void setAll_nullExpire_usesDefaultExpireTime() {
        Map<String, Object> keyValues = Map.of("userA", "dataA", "userB", "dataB");
        when(mockRedisProvider.setHashMapAll(CACHE_PREFIX, keyValues, DEFAULT_EXPIRE_TIME)).thenReturn(Mono.empty());

        Mono<Void> result = userCacheProviderImplUnderTest.setAll(keyValues, null);
        StepVerifier.create(result).verifyComplete();
        verify(mockRedisProvider).setHashMapAll(CACHE_PREFIX, keyValues, DEFAULT_EXPIRE_TIME);
    }

    @Test
    @DisplayName("setAll - expire 有值 - 使用提供過期時間")
    void setAll_withExpire_usesProvidedExpireTime() {
        Map<String, Object> keyValues = Map.of("userC", "dataC");
        Duration customExpire = Duration.ofMinutes(5);
        when(mockRedisProvider.setHashMapAll(CACHE_PREFIX, keyValues, customExpire)).thenReturn(Mono.empty());

        Mono<Void> result = userCacheProviderImplUnderTest.setAll(keyValues, customExpire);
        StepVerifier.create(result).verifyComplete();
        verify(mockRedisProvider).setHashMapAll(CACHE_PREFIX, keyValues, customExpire);
    }

    @Test
    @DisplayName("delete - 刪除單一鍵 - 調用 redisProvider.deleteHash")
    void delete_singleKey_callsDeleteHashWithKey() {
        String key = "userDelete1";
        when(mockRedisProvider.deleteHash(CACHE_PREFIX, key)).thenReturn(Mono.empty());

        Mono<Void> result = userCacheProviderImplUnderTest.delete(key);
        StepVerifier.create(result).verifyComplete();
        verify(mockRedisProvider).deleteHash(CACHE_PREFIX, key);
    }

    @Test
    @DisplayName("deleteAll - hashKeys 為 null - 調用 redisProvider.deleteHash(prefix)")
    void deleteAll_nullKeys_callsDeleteHashWithPrefixOnly() {
        when(mockRedisProvider.deleteHash(CACHE_PREFIX)).thenReturn(Mono.empty());

        Mono<Void> result = userCacheProviderImplUnderTest.deleteAll(null);
        StepVerifier.create(result).verifyComplete();
        verify(mockRedisProvider).deleteHash(CACHE_PREFIX);
    }

    @Test
    @DisplayName("deleteAll - hashKeys 為空集合 - 調用 redisProvider.deleteHash(prefix)")
    void deleteAll_emptyKeys_callsDeleteHashWithPrefixOnly() {
        when(mockRedisProvider.deleteHash(CACHE_PREFIX)).thenReturn(Mono.empty());

        Mono<Void> result = userCacheProviderImplUnderTest.deleteAll(Collections.emptyList());
        StepVerifier.create(result).verifyComplete();
        verify(mockRedisProvider).deleteHash(CACHE_PREFIX);
    }

    @Test
    @DisplayName("deleteAll - hashKeys 有值 - 調用 redisProvider.deleteHash(prefix, keys)")
    void deleteAll_withKeys_callsDeleteHashWithPrefixAndKeys() {
        List<String> keys = Arrays.asList("userDelA", "userDelB");
        when(mockRedisProvider.deleteHash(CACHE_PREFIX, keys)).thenReturn(Mono.empty());

        Mono<Void> result = userCacheProviderImplUnderTest.deleteAll(keys);
        StepVerifier.create(result).verifyComplete();
        verify(mockRedisProvider).deleteHash(CACHE_PREFIX, keys);
    }

    @Test
    @DisplayName("getDefaultExpire - 返回預設過期時間 - 成功")
    void getDefaultExpire_returnsDefaultExpireTime() {
        assertThat(userCacheProviderImplUnderTest.getDefaultExpire()).isEqualTo(DEFAULT_EXPIRE_TIME);
    }
}
