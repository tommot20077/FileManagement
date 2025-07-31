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
import xyz.dowob.filemanagement.data.file.dto.UserFileListDTO;

import java.time.Duration;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * FileListCacheProviderImpl 測試類別。
 * 
 * <p>測試 FileListCacheProviderImpl 的檔案清單緩存功能，包括：
 * <ul>
 * <li>構造函數初始化及參數驗證</li>
 * <li>單一清單緩存的獲取與設定</li>
 * <li>批量清單緩存的獲取與設定</li>
 * <li>緩存刪除操作（單一及批量）</li>
 * <li>不同資料類型的轉換與處理</li>
 * <li>過期時間管理與驗證</li>
 * <li>錯誤情況與異常處理</li>
 * </ul>
 * 
 * <p>測試涵蓋 Redis 簿列操作的所有核心功能，驗證緩存提供者的正確性和健壯性。
 * 透過反應式程式測試確保異步操作的正確性和緩存策略的有效性。
 * 
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FileListCacheProvider 邏輯處理測試")
class FileListCacheProviderImplTest {

    private final Duration defaultExpireTime = Duration.ofMinutes(30);
    private final Duration customExpireTime = Duration.ofHours(1);
    @Mock
    private RedisProvider mockRedisProvider;
    @Mock
    private CacheProperties mockCacheProperties;
    private FileListCacheProviderImpl fileListCacheProviderImplUnderTest;

    @BeforeEach
    void setUp() {
        when(mockCacheProperties.getFileListCacheExpireTime()).thenReturn(defaultExpireTime);
        fileListCacheProviderImplUnderTest = new FileListCacheProviderImpl(mockRedisProvider, mockCacheProperties);
    }

    @Test
    @DisplayName("建構子 - 過期時間為正數 - 成功創建")
    void constructor_positiveExpireTime_createsSuccessfully() {
        assertEquals(defaultExpireTime, fileListCacheProviderImplUnderTest.getDefaultExpire());
    }

    @Test
    @DisplayName("建構子 - 過期時間為零 - 拋出 IllegalArgumentException")
    void constructor_zeroExpireTime_throwsIllegalArgumentException() {
        when(mockCacheProperties.getFileListCacheExpireTime()).thenReturn(Duration.ZERO);
        assertThrows(IllegalArgumentException.class, () -> new FileListCacheProviderImpl(mockRedisProvider, mockCacheProperties));
    }

    @Test
    @DisplayName("建構子 - 過期時間為負數 - 拋出 IllegalArgumentException")
    void constructor_negativeExpireTime_throwsIllegalArgumentException() {
        when(mockCacheProperties.getFileListCacheExpireTime()).thenReturn(Duration.ofMinutes(-10));
        assertThrows(IllegalArgumentException.class, () -> new FileListCacheProviderImpl(mockRedisProvider, mockCacheProperties));
    }


    @Test
    @DisplayName("getAsList - 獲取存在的緩存列表 - 成功返回列表")
    void getAsList_existingCache_returnsList() {
        String key = "testKey";
        UserFileListDTO dto1 = new UserFileListDTO();
        UserFileListDTO dto2 = new UserFileListDTO();
        List<UserFileListDTO> expectedList = Arrays.asList(dto1, dto2);
        when(mockRedisProvider.getList(eq(key), eq(UserFileListDTO.class))).thenReturn(Flux.fromIterable(expectedList));

        StepVerifier.create(fileListCacheProviderImplUnderTest.getAsList(key, UserFileListDTO.class)).expectNext(expectedList).verifyComplete();

        verify(mockRedisProvider).getList(eq(key), eq(UserFileListDTO.class));
    }

    @Test
    @DisplayName("getAsList - 獲取不存在的緩存列表 - 返回空列表")
    void getAsList_nonExistingCache_returnsEmptyList() {
        String key = "nonExistingKey";
        when(mockRedisProvider.getList(eq(key), eq(UserFileListDTO.class))).thenReturn(Flux.empty());

        StepVerifier
                .create(fileListCacheProviderImplUnderTest.getAsList(key, UserFileListDTO.class))
                .expectNext(Collections.emptyList())
                .verifyComplete();
    }

    @Test
    @DisplayName("getAsList - RedisProvider 發生錯誤 - 返回錯誤")
    void getAsList_redisProviderError_returnsError() {
        String key = "errorKey";
        RuntimeException exception = new RuntimeException("Redis error");
        when(mockRedisProvider.getList(eq(key), eq(UserFileListDTO.class))).thenReturn(Flux.error(exception));

        StepVerifier
                .create(fileListCacheProviderImplUnderTest.getAsList(key, UserFileListDTO.class))
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException && "Redis error".equals(throwable.getMessage()))
                .verify();
    }

    @Test
    @DisplayName("getAllAsMapList - 獲取多個存在的緩存列表 - 成功返回 Map")
    void getAllAsMapList_existingCaches_returnsMap() {
        String key1 = "key1";
        UserFileListDTO dto1 = new UserFileListDTO();
        List<UserFileListDTO> list1 = Collections.singletonList(dto1);

        String key2 = "key2";
        UserFileListDTO dto2a = new UserFileListDTO();
        UserFileListDTO dto2b = new UserFileListDTO();
        List<UserFileListDTO> list2 = Arrays.asList(dto2a, dto2b);

        Collection<String> keys = Arrays.asList(key1, key2);
        Map<String, List<UserFileListDTO>> expectedMap = new HashMap<>();
        expectedMap.put(key1, list1);
        expectedMap.put(key2, list2);

        when(mockRedisProvider.getList(eq(key1), eq(UserFileListDTO.class))).thenReturn(Flux.fromIterable(list1));
        when(mockRedisProvider.getList(eq(key2), eq(UserFileListDTO.class))).thenReturn(Flux.fromIterable(list2));

        StepVerifier
                .create(fileListCacheProviderImplUnderTest.getAllAsMapList(keys, UserFileListDTO.class))
                .expectNextMatches(map -> map.size() == 2 && map.get(key1).equals(list1) && map.get(key2).equals(list2))
                .verifyComplete();
    }

    @Test
    @DisplayName("getAllAsMapList - 部分緩存不存在 - 返回包含空列表的 Map")
    void getAllAsMapList_partialNonExistingCache_returnsMapWithEmptyLists() {
        String key1 = "key1";
        UserFileListDTO dto1 = new UserFileListDTO();
        List<UserFileListDTO> list1 = Collections.singletonList(dto1);

        String key2 = "key2";
        Collection<String> keys = Arrays.asList(key1, key2);

        when(mockRedisProvider.getList(eq(key1), eq(UserFileListDTO.class))).thenReturn(Flux.fromIterable(list1));
        when(mockRedisProvider.getList(eq(key2), eq(UserFileListDTO.class))).thenReturn(Flux.empty());

        StepVerifier
                .create(fileListCacheProviderImplUnderTest.getAllAsMapList(keys, UserFileListDTO.class))
                .expectNextMatches(map -> map.size() == 2 && map.get(key1).equals(list1) && map.get(key2).isEmpty())
                .verifyComplete();
    }

    @Test
    @DisplayName("getAllAsMapList - 輸入空鍵集合 - 返回空 Map")
    void getAllAsMapList_emptyKeySet_returnsEmptyMap() {
        Collection<String> keys = Collections.emptyList();
        StepVerifier
                .create(fileListCacheProviderImplUnderTest.getAllAsMapList(keys, UserFileListDTO.class))
                .expectNextMatches(Map::isEmpty)
                .verifyComplete();
        verify(mockRedisProvider, never()).getList(anyString(), any());
    }

    @Test
    @DisplayName("getAllAsMapList - RedisProvider 發生錯誤 - 返回錯誤")
    void getAllAsMapList_redisError_returnsError() {
        String key1 = "key1";
        String key2 = "errorKey";
        Collection<String> keys = Arrays.asList(key1, key2);
        RuntimeException exception = new RuntimeException("Redis error on getAll");

        when(mockRedisProvider.getList(eq(key1), eq(UserFileListDTO.class))).thenReturn(Flux.just(new UserFileListDTO()));
        when(mockRedisProvider.getList(eq(key2), eq(UserFileListDTO.class))).thenReturn(Flux.error(exception));


        StepVerifier
                .create(fileListCacheProviderImplUnderTest.getAllAsMapList(keys, UserFileListDTO.class))
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException && "Redis error on getAll".equals(throwable.getMessage()))
                .verify();
    }


    @Test
    @DisplayName("set - 設置 Collection 類型緩存 - 成功")
    void set_collectionValue_completes() {
        String key = "setKeyColl";
        List<UserFileListDTO> value = Arrays.asList(new UserFileListDTO(), new UserFileListDTO());
        when(mockRedisProvider.insertList(eq(key), any(UserFileListDTO.class), eq(false), eq(defaultExpireTime))).thenReturn(Mono.empty());

        StepVerifier.create(fileListCacheProviderImplUnderTest.set(key, value, null)).verifyComplete();

        verify(mockRedisProvider, times(2)).insertList(eq(key), any(UserFileListDTO.class), eq(false), eq(defaultExpireTime));
    }

    @Test
    @DisplayName("set - 設置 UserFileListDTO 類型緩存 - 成功")
    void set_userFileListDTOValue_completes() {
        String key = "setKeyDTO";
        UserFileListDTO value = new UserFileListDTO();
        when(mockRedisProvider.insertList(eq(key), eq(value), eq(false), eq(defaultExpireTime))).thenReturn(Mono.empty());

        StepVerifier.create(fileListCacheProviderImplUnderTest.set(key, value, null)).verifyComplete();

        verify(mockRedisProvider).insertList(eq(key), eq(value), eq(false), eq(defaultExpireTime));
    }

    @Test
    @DisplayName("set - 設置緩存並指定過期時間 - 成功")
    void set_withCustomExpireTime_completes() {
        String key = "setKeyCustomExpire";
        UserFileListDTO value = new UserFileListDTO();
        when(mockRedisProvider.insertList(eq(key), eq(value), eq(false), eq(customExpireTime))).thenReturn(Mono.empty());

        StepVerifier.create(fileListCacheProviderImplUnderTest.set(key, value, customExpireTime)).verifyComplete();

        verify(mockRedisProvider).insertList(eq(key), eq(value), eq(false), eq(customExpireTime));
    }

    @Test
    @DisplayName("set - 設置不支持的類型緩存 - 拋出 UnsupportedOperationException")
    void set_unsupportedType_returnsError() {
        String key = "setKeyUnsupported";
        String value = "Unsupported String";
        StepVerifier.create(fileListCacheProviderImplUnderTest.set(key, value, null)).expectError(UnsupportedOperationException.class).verify();

        verify(mockRedisProvider, never()).insertList(anyString(), any(), anyBoolean(), any(Duration.class));
    }

    @Test
    @DisplayName("set - RedisProvider 插入時發生錯誤 - 返回錯誤")
    void set_redisProviderError_returnsError() {
        String key = "setKeyError";
        UserFileListDTO value = new UserFileListDTO();
        RuntimeException exception = new RuntimeException("Redis insert error");
        when(mockRedisProvider.insertList(eq(key), eq(value), eq(false), eq(defaultExpireTime))).thenReturn(Mono.error(exception));

        StepVerifier
                .create(fileListCacheProviderImplUnderTest.set(key, value, null))
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException && "Redis insert error".equals(throwable.getMessage()))
                .verify();
    }

    @Test
    @DisplayName("set - 設置空 Collection - 成功且不調用 RedisProvider")
    void set_emptyCollection_completesWithoutRedisCall() {
        String key = "setEmptyColl";
        List<UserFileListDTO> value = Collections.emptyList();

        StepVerifier.create(fileListCacheProviderImplUnderTest.set(key, value, null)).verifyComplete();

        verify(mockRedisProvider, never()).insertList(anyString(), any(), anyBoolean(), any(Duration.class));
    }


    @Test
    @DisplayName("setAll - 批量設置緩存 - 成功")
    void setAll_validMap_completes() {
        Map<String, Object> keyValues = new HashMap<>();
        keyValues.put("key1DTO", new UserFileListDTO());
        keyValues.put("key2Coll", Collections.singletonList(new UserFileListDTO()));

        when(mockRedisProvider.insertList(anyString(), any(UserFileListDTO.class), eq(false), eq(defaultExpireTime))).thenReturn(Mono.empty());

        StepVerifier.create(fileListCacheProviderImplUnderTest.setAll(keyValues, null)).verifyComplete();

        verify(mockRedisProvider, times(1)).insertList(eq("key1DTO"), any(UserFileListDTO.class), eq(false), eq(defaultExpireTime));
        verify(mockRedisProvider, times(1)).insertList(eq("key2Coll"), any(UserFileListDTO.class), eq(false), eq(defaultExpireTime));
    }

    @Test
    @DisplayName("setAll - 批量設置緩存並指定過期時間 - 成功")
    void setAll_withCustomExpireTime_completes() {
        Map<String, Object> keyValues = new HashMap<>();
        keyValues.put("keyCustom", new UserFileListDTO());

        when(mockRedisProvider.insertList(eq("keyCustom"), any(UserFileListDTO.class), eq(false), eq(customExpireTime))).thenReturn(Mono.empty());

        StepVerifier.create(fileListCacheProviderImplUnderTest.setAll(keyValues, customExpireTime)).verifyComplete();

        verify(mockRedisProvider).insertList(eq("keyCustom"), any(UserFileListDTO.class), eq(false), eq(customExpireTime));
    }

    @Test
    @DisplayName("setAll - 批量設置包含不支持的類型 - 拋出 UnsupportedOperationException")
    void setAll_unsupportedTypeInMap_returnsError() {
        Map<String, Object> keyValues = new LinkedHashMap<>();
        keyValues.put("keyValid", new UserFileListDTO());
        keyValues.put("keyInvalid", "Unsupported String");

        when(mockRedisProvider.insertList(eq("keyValid"), any(UserFileListDTO.class), eq(false), eq(defaultExpireTime))).thenReturn(Mono.empty());

        StepVerifier.create(fileListCacheProviderImplUnderTest.setAll(keyValues, null)).expectError(UnsupportedOperationException.class).verify();
        verify(mockRedisProvider, times(1)).insertList(eq("keyValid"), any(UserFileListDTO.class), eq(false), eq(defaultExpireTime));
    }

    @Test
    @DisplayName("setAll - RedisProvider 插入時發生錯誤 - 返回錯誤")
    void setAll_redisProviderError_returnsError() {
        Map<String, Object> keyValues = new HashMap<>();
        keyValues.put("keyError", new UserFileListDTO());
        RuntimeException exception = new RuntimeException("Redis batch insert error");

        when(mockRedisProvider.insertList(eq("keyError"), any(UserFileListDTO.class), eq(false), eq(defaultExpireTime))).thenReturn(Mono.error(
                exception));

        StepVerifier
                .create(fileListCacheProviderImplUnderTest.setAll(keyValues, null))
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException && "Redis batch insert error".equals(throwable.getMessage()))
                .verify();
    }

    @Test
    @DisplayName("setAll - 批量設置空 Map - 成功且不調用 RedisProvider")
    void setAll_emptyMap_completesWithoutRedisCall() {
        Map<String, Object> keyValues = Collections.emptyMap();

        StepVerifier.create(fileListCacheProviderImplUnderTest.setAll(keyValues, null)).verifyComplete();

        verify(mockRedisProvider, never()).insertList(anyString(), any(), anyBoolean(), any(Duration.class));
    }


    @Test
    @DisplayName("delete - 刪除單個緩存鍵 - 成功")
    void delete_existingKey_completes() {
        String key = "deleteKey";
        when(mockRedisProvider.deleteList(eq(key))).thenReturn(Mono.empty());

        StepVerifier.create(fileListCacheProviderImplUnderTest.delete(key)).verifyComplete();

        verify(mockRedisProvider).deleteList(eq(key));
    }

    @Test
    @DisplayName("delete - RedisProvider 刪除時發生錯誤 - 返回錯誤")
    void delete_redisProviderError_returnsError() {
        String key = "deleteKeyError";
        RuntimeException exception = new RuntimeException("Redis delete error");
        when(mockRedisProvider.deleteList(eq(key))).thenReturn(Mono.error(exception));

        StepVerifier
                .create(fileListCacheProviderImplUnderTest.delete(key))
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException && "Redis delete error".equals(throwable.getMessage()))
                .verify();
    }


    @Test
    @DisplayName("deleteAll - 批量刪除緩存鍵 - 成功")
    void deleteAll_multipleKeys_completes() {
        Collection<String> keys = Arrays.asList("delKey1", "delKey2");
        when(mockRedisProvider.deleteList(anyString())).thenReturn(Mono.empty());

        StepVerifier.create(fileListCacheProviderImplUnderTest.deleteAll(keys)).verifyComplete();

        verify(mockRedisProvider).deleteList(eq("delKey1"));
        verify(mockRedisProvider).deleteList(eq("delKey2"));
    }

    @Test
    @DisplayName("deleteAll - RedisProvider 刪除部分鍵時發生錯誤 - 返回錯誤")
    void deleteAll_redisProviderErrorOnOneKey_returnsError() {
        Collection<String> keys = Arrays.asList("delKeyOK", "delKeyFail");
        RuntimeException exception = new RuntimeException("Redis batch delete error");
        when(mockRedisProvider.deleteList(eq("delKeyOK"))).thenReturn(Mono.empty());
        when(mockRedisProvider.deleteList(eq("delKeyFail"))).thenReturn(Mono.error(exception));

        StepVerifier
                .create(fileListCacheProviderImplUnderTest.deleteAll(keys))
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException && "Redis batch delete error".equals(throwable.getMessage()))
                .verify();

        verify(mockRedisProvider).deleteList(eq("delKeyOK"));
        verify(mockRedisProvider).deleteList(eq("delKeyFail"));
    }

    @Test
    @DisplayName("deleteAll - 批量刪除空鍵集合 - 成功且不調用 RedisProvider")
    void deleteAll_emptyKeySet_completesWithoutRedisCall() {
        Collection<String> keys = Collections.emptyList();

        StepVerifier.create(fileListCacheProviderImplUnderTest.deleteAll(keys)).verifyComplete();

        verify(mockRedisProvider, never()).deleteList(anyString());
    }


    @Test
    @DisplayName("getDefaultExpire - 獲取默認過期時間 - 成功返回")
    void getDefaultExpire_returnsDefaultDuration() {
        assertEquals(defaultExpireTime, fileListCacheProviderImplUnderTest.getDefaultExpire());
    }


    @Test
    @DisplayName("設置檔案列表緩存 - 成功設置並返回 (已由 set_xxx 測試覆蓋)")
    void setCache_validInput_completes() {
        String key = "setCacheKey";
        UserFileListDTO value = new UserFileListDTO();
        when(mockRedisProvider.insertList(eq(key), eq(value), eq(false), eq(defaultExpireTime))).thenReturn(Mono.empty());
        StepVerifier.create(fileListCacheProviderImplUnderTest.set(key, value, null)).verifyComplete();
    }

    @Test
    @DisplayName("獲取檔案列表緩存 - 成功返回緩存數據 (已由 getAsList_existingCache_returnsList 測試覆蓋)")
    void getCache_existingCache_returnsFileList() {
        String key = "getCacheKey";
        UserFileListDTO dto1 = new UserFileListDTO();
        List<UserFileListDTO> expectedList = Collections.singletonList(dto1);
        when(mockRedisProvider.getList(eq(key), eq(UserFileListDTO.class))).thenReturn(Flux.fromIterable(expectedList));
        StepVerifier.create(fileListCacheProviderImplUnderTest.getAsList(key, UserFileListDTO.class)).expectNext(expectedList).verifyComplete();
    }

    @Test
    @DisplayName("獲取不存在的緩存 - 返回空 Mono (應為空 List, 已由 getAsList_nonExistingCache_returnsEmptyList 測試覆蓋)")
    void getCache_nonExistingCache_returnsEmptyMono() {
        String key = "getNonExistingCacheKey";
        when(mockRedisProvider.getList(eq(key), eq(UserFileListDTO.class))).thenReturn(Flux.empty());
        StepVerifier
                .create(fileListCacheProviderImplUnderTest.getAsList(key, UserFileListDTO.class))
                .expectNext(Collections.emptyList())
                .verifyComplete();
    }

    @Test
    @DisplayName("設置緩存時發生錯誤 - 返回錯誤 (已由 set_redisProviderError_returnsError 測試覆蓋)")
    void setCache_error_returnsError() {
        String key = "setCacheErrorKey";
        UserFileListDTO value = new UserFileListDTO();
        RuntimeException exception = new RuntimeException("Set cache error");
        when(mockRedisProvider.insertList(eq(key), eq(value), eq(false), eq(defaultExpireTime))).thenReturn(Mono.error(exception));
        StepVerifier.create(fileListCacheProviderImplUnderTest.set(key, value, null)).expectError(RuntimeException.class).verify();
    }

    @Test
    @DisplayName("使用無效的用戶 ID - 返回空 Mono (此 Provider 不直接處理用戶 ID, 而是鍵, 已由 getAsList_nonExistingCache_returnsEmptyList 測試覆蓋)")
    void getCache_invalidUserId_returnsEmptyMono() {
        String invalidKey = "invalidUserKey";
        when(mockRedisProvider.getList(eq(invalidKey), eq(UserFileListDTO.class))).thenReturn(Flux.empty());
        StepVerifier
                .create(fileListCacheProviderImplUnderTest.getAsList(invalidKey, UserFileListDTO.class))
                .expectNext(Collections.emptyList())
                .verifyComplete();
    }
}
