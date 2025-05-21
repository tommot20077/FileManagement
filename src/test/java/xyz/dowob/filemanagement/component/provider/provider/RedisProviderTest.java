package xyz.dowob.filemanagement.component.provider.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.dowob.filemanagement.exception.ProcessException;

import java.time.Duration;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedisProvider 邏輯處理測試")
class RedisProviderTest {

    @Mock
    private ReactiveRedisTemplate<String, Object> mockRedisTemplate;

    private RedisProvider redisProviderUnderTest;

    @BeforeEach
    void setUp() {
        redisProviderUnderTest = new RedisProvider(mockRedisTemplate, new ObjectMapper());
    }

    @Test
    @DisplayName("設置值到 Redis - 成功設置並返回")
    void setValue_validInput_completes() {
        String key = "testKey";
        String value = "testValue";
        ReactiveValueOperations<String, Object> valueOps = mock(ReactiveValueOperations.class);
        when(mockRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.set(eq(key), eq(value))).thenReturn(Mono.just(true));

        StepVerifier.create(redisProviderUnderTest.setValue(key, value)).verifyComplete();
    }

    @Test
    @DisplayName("設置 HashMap - 成功設置並返回")
    void setHashMap_validInput_completes() {
        String hashKey = "testHash";
        String innerKey = "testInnerKey";
        String value = "testValue";
        ReactiveHashOperations<String, Object, Object> hashOps = mock(ReactiveHashOperations.class);
        when(mockRedisTemplate.opsForHash()).thenReturn(hashOps);
        when(hashOps.put(eq(hashKey), eq(innerKey), eq(value))).thenReturn(Mono.just(true));

        StepVerifier.create(redisProviderUnderTest.setHashMap(hashKey, innerKey, value)).verifyComplete();
    }

    @Test
    @DisplayName("設置值並帶過期時間到 Redis - 成功設置並返回")
    void setValue_withExpiry_completes() {
        String key = "testKey";
        String value = "testValue";
        Duration expireTime = Duration.ofMinutes(5);
        ReactiveValueOperations<String, Object> valueOps = mock(ReactiveValueOperations.class);
        when(mockRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.set(eq(key), eq(value))).thenReturn(Mono.just(true));
        when(mockRedisTemplate.expire(eq(key), any(Duration.class))).thenReturn(Mono.just(true));

        StepVerifier.create(redisProviderUnderTest.setValue(key, value, expireTime)).verifyComplete();
    }

    @Test
    @DisplayName("獲取分頁響應從 Redis 值 - 成功返回分頁數據")
    void getPagedResponseFromValue_validData_returnsPagedResponse() {
        String key = "testKey";
        ReactiveValueOperations<String, Object> valueOps = mock(ReactiveValueOperations.class);
        Map<String, Object> expectedData = new HashMap<>();
        expectedData.put("data", Arrays.asList("item1", "item2"));
        expectedData.put("totalElements", 2L);

        when(mockRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(key)).thenReturn(Mono.just(expectedData));

        StepVerifier
                .create(redisProviderUnderTest.getPagedResponseFromValue(key, String.class))
                .expectNextMatches(response -> response.getTotalElements() == 2 && response.getData().size() == 2 && response
                        .getData()
                        .contains("item1"))
                .verifyComplete();
    }

    @Test
    @DisplayName("獲取分頁響應從 ZSet - 成功返回分頁列表")
    void getListFromZset_validData_returnsList() {
        String key = "testKey";
        int page = 1;
        List<String> expectedList = Arrays.asList("item1", "item2");
        ReactiveZSetOperations<String, Object> zSetOps = mock(ReactiveZSetOperations.class);
        when(mockRedisTemplate.opsForZSet()).thenReturn(zSetOps);
        when(zSetOps.rangeByScore(eq(key), any(Range.class))).thenReturn(Flux.just(expectedList));

        StepVerifier.create(redisProviderUnderTest.getListFromZset(key, page, String.class)).expectNextSequence(expectedList).verifyComplete();
    }

    @Test
    @DisplayName("設置值到 Redis 時發生錯誤 - 返回錯誤")
    void setValue_whenError_propagatesError() {
        String key = "testKey";
        String value = "testValue";
        ReactiveValueOperations<String, Object> valueOps = mock(ReactiveValueOperations.class);
        when(mockRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.set(eq(key), eq(value))).thenReturn(Mono.error(new RuntimeException("Redis error")));

        StepVerifier
                .create(redisProviderUnderTest.setValue(key, value))
                .expectErrorMatches(error -> error.getMessage().equals("Redis error"))
                .verify();
    }

    @Test
    @DisplayName("設置值到 Redis 帶負數過期時間 - 忽略過期時間設置")
    void setValue_negativeExpiry_setsValueWithoutExpiry() {
        String key = "testKey";
        String value = "testValue";
        Duration negativeExpiry = Duration.ofMinutes(-5);
        ReactiveValueOperations<String, Object> valueOps = mock(ReactiveValueOperations.class);
        when(mockRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.set(eq(key), eq(value))).thenReturn(Mono.just(true));

        StepVerifier.create(redisProviderUnderTest.setValue(key, value, negativeExpiry)).verifyComplete();
    }

    @Test
    @DisplayName("增加 Hash 值 - 成功增加並返回結果")
    void incrementHashMap_validInput_returnsIncrementedValue() {
        String hashKey = "testHash";
        String innerKey = "testInnerKey";
        long delta = 1L;
        ReactiveHashOperations<String, Object, Object> hashOps = mock(ReactiveHashOperations.class);
        when(mockRedisTemplate.opsForHash()).thenReturn(hashOps);
        when(hashOps.increment(eq(hashKey), eq(innerKey), eq(delta))).thenReturn(Mono.just(1L));

        StepVerifier.create(redisProviderUnderTest.incrementHashMap(hashKey, innerKey, delta)).expectNext(1L).verifyComplete();
    }

    @Test
    @DisplayName("檢查分塊狀態 - 返回待處理狀態")
    void isChunkSetPending_returnsTrue() {
        String key = "testChunks";
        int chunkIndex = 1;
        ReactiveSetOperations<String, Object> setOps = mock(ReactiveSetOperations.class);
        when(mockRedisTemplate.opsForSet()).thenReturn(setOps);
        when(setOps.isMember(eq(key), eq(chunkIndex))).thenReturn(Mono.just(true));

        StepVerifier.create(redisProviderUnderTest.isChunkSetPending(key, chunkIndex)).expectNext(true).verifyComplete();
    }

    @Test
    @DisplayName("獲取 HashMap 符合通配符的值 - 成功返回")
    void getHashMapByPattern_returnsMatchingEntries() {
        String hashKey = "testHash";
        String pattern = "test*";
        Map.Entry<Object, Object> entry = Map.entry("testKey", "testValue");
        ReactiveHashOperations<String, Object, Object> hashOps = mock(ReactiveHashOperations.class);
        when(mockRedisTemplate.opsForHash()).thenReturn(hashOps);
        when(hashOps.scan(eq(hashKey), any())).thenReturn(Flux.just(entry));

        StepVerifier
                .create(redisProviderUnderTest.getHashMapByPattern(hashKey, pattern, String.class))
                .expectNextMatches(result -> result.getKey().equals("testKey") && result.getValue().equals("testValue"))
                .verifyComplete();
    }

    @Test
    @DisplayName("增加 Hash 值並設置過期時間 - 成功增加並返回")
    void incrementHashMap_withExpiry_returnsIncrementedValue() {
        String hashKey = "testHash";
        String innerKey = "testInnerKey";
        long delta = 1L;
        Duration expireTime = Duration.ofMinutes(5);
        ReactiveHashOperations<String, Object, Object> hashOps = mock(ReactiveHashOperations.class);
        when(mockRedisTemplate.opsForHash()).thenReturn(hashOps);
        when(hashOps.increment(eq(hashKey), eq(innerKey), eq(delta))).thenReturn(Mono.just(1L));
        when(mockRedisTemplate.expire(eq(hashKey), any(Duration.class))).thenReturn(Mono.just(true));

        StepVerifier.create(redisProviderUnderTest.incrementHashMap(hashKey, innerKey, delta, expireTime)).expectNext(1L).verifyComplete();
    }

    @Test
    @DisplayName("依照通配符刪除 - 無匹配鍵時完成")
    void deleteByPattern_noMatches_completes() {
        String pattern = "test*";
        when(mockRedisTemplate.keys(pattern)).thenReturn(Flux.empty());
        when(mockRedisTemplate.delete(any(String[].class))).thenReturn(Mono.just(0L));

        StepVerifier.create(redisProviderUnderTest.deleteByPattern(pattern)).verifyComplete();
    }

    @Test
    @DisplayName("獲取 HashMap 時型別轉換失敗 - 返回錯誤")
    void getHashMap_typeCastError_returnsError() {
        String hashKey = "testHash";
        String innerKey = "testInnerKey";
        ReactiveHashOperations<String, Object, Object> hashOps = mock(ReactiveHashOperations.class);
        when(mockRedisTemplate.opsForHash()).thenReturn(hashOps);
        when(hashOps.get(eq(hashKey), eq(innerKey))).thenReturn(Mono.just(123));
        StepVerifier.create(redisProviderUnderTest.getHashMap(hashKey, innerKey, String.class)).expectError(ClassCastException.class).verify();
    }

    @Test
    @DisplayName("刪除多個鍵值但部分不存在 - 成功刪除並返回")
    void deleteValue_partialKeys_completes() {
        Collection<String> keys = Arrays.asList("key1", "key2", "nonexistent");
        when(mockRedisTemplate.delete(any(String[].class))).thenReturn(Mono.just(2L));

        StepVerifier.create(redisProviderUnderTest.deleteValue(keys)).verifyComplete();
    }

    @Test
    @DisplayName("執行事務時發生錯誤 - 返回錯誤")
    void setValueIfAbsent_transactionError_returnsError() {
        String key = "testKey";
        String value = "testValue";
        ReactiveValueOperations<String, Object> valueOps = mock(ReactiveValueOperations.class);
        when(mockRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(eq(key), eq(value))).thenReturn(Mono.error(new RuntimeException("Transaction failed")));

        StepVerifier
                .create(redisProviderUnderTest.setValueIfAbsent(key, value))
                .expectErrorMatches(error -> error.getMessage().equals("Transaction failed"))
                .verify();
    }

    @Test
    @DisplayName("設置 Set 並帶過期時間 - 成功設置並返回")
    void setSet_withExpiry_completes() {
        String key = "testSet";
        String value = "testValue";
        Duration expireTime = Duration.ofMinutes(5);
        ReactiveSetOperations<String, Object> setOps = mock(ReactiveSetOperations.class);
        when(mockRedisTemplate.opsForSet()).thenReturn(setOps);
        when(setOps.add(eq(key), eq(value))).thenReturn(Mono.just(1L));
        when(mockRedisTemplate.expire(eq(key), any(Duration.class))).thenReturn(Mono.just(true));

        StepVerifier.create(redisProviderUnderTest.setSet(key, value, expireTime)).verifyComplete();
    }

    @Test
    @DisplayName("操作 List - 插入和獲取操作成功")
    void list_operations_complete() {
        String key = "testList";
        String value = "testValue";
        ReactiveListOperations<String, Object> listOps = mock(ReactiveListOperations.class);
        when(mockRedisTemplate.opsForList()).thenReturn(listOps);
        when(listOps.rightPush(eq(key), eq(value))).thenReturn(Mono.just(1L));
        when(listOps.leftPush(eq(key), eq(value))).thenReturn(Mono.just(1L));
        when(listOps.range(eq(key), eq(0L), eq(-1L))).thenReturn(Flux.just(value));

        StepVerifier.create(redisProviderUnderTest.setList(key, value)).verifyComplete();

        StepVerifier.create(redisProviderUnderTest.insertList(key, value, true)).verifyComplete();

        StepVerifier.create(redisProviderUnderTest.getList(key)).expectNext(value).verifyComplete();
    }

    @Test
    @DisplayName("操作 Hash - 批量設置和查詢操作成功")
    void hashMapAll_operations_complete() {
        String hashKey = "testHash";
        Map<String, Object> values = Map.of("key1", "value1", "key2", "value2");
        ReactiveHashOperations<String, Object, Object> hashOps = mock(ReactiveHashOperations.class);
        when(mockRedisTemplate.opsForHash()).thenReturn(hashOps);
        when(hashOps.putAll(eq(hashKey), eq(values))).thenReturn(Mono.empty());
        when(hashOps.values(hashKey)).thenReturn(Flux.fromIterable(values.values()));

        StepVerifier.create(redisProviderUnderTest.setHashMapAll(hashKey, values)).verifyComplete();

        StepVerifier.create(redisProviderUnderTest.getHashMapAll(hashKey)).expectNextCount(2).verifyComplete();
    }

    @Test
    @DisplayName("獲取指定範圍的 List 內容 - 成功返回")
    void getListContent_validRange_returnsContent() {
        String key = "testList";
        long start = 0;
        long end = 1;
        List<String> expectedValues = Arrays.asList("value1", "value2");
        ReactiveListOperations<String, Object> listOps = mock(ReactiveListOperations.class);
        when(mockRedisTemplate.opsForList()).thenReturn(listOps);
        when(listOps.range(eq(key), eq(start), eq(end))).thenReturn(Flux.fromIterable(expectedValues));

        StepVerifier.create(redisProviderUnderTest.getListContent(key, start, end, String.class)).expectNextSequence(expectedValues).verifyComplete();
    }

    @Test
    @DisplayName("獲取 List 內容為空 - 返回空 Flux")
    void getListContent_emptyRange_returnsEmptyFlux() {
        String key = "testList";
        long start = 0;
        long end = 1;
        ReactiveListOperations<String, Object> listOps = mock(ReactiveListOperations.class);
        when(mockRedisTemplate.opsForList()).thenReturn(listOps);
        when(listOps.range(eq(key), eq(start), eq(end))).thenReturn(Flux.empty());

        StepVerifier.create(redisProviderUnderTest.getListContent(key, start, end, String.class)).verifyComplete();
    }

    @Test
    @DisplayName("刪除 List 中的指定值 - 成功刪除")
    void deleteList_specificValue_completes() {
        String key = "testList";
        String value = "testValue";
        ReactiveListOperations<String, Object> listOps = mock(ReactiveListOperations.class);
        when(mockRedisTemplate.opsForList()).thenReturn(listOps);
        when(listOps.remove(eq(key), eq(1L), eq(value))).thenReturn(Mono.just(1L));

        StepVerifier.create(redisProviderUnderTest.deleteList(key, value)).verifyComplete();
    }

    @Test
    @DisplayName("設置 ZSet 並帶過期時間 - 成功設置並返回")
    void setZset_withExpiry_completes() {
        String key = "testZSet";
        String value = "testValue";
        double score = 1.0;
        Duration expireTime = Duration.ofMinutes(5);
        ReactiveZSetOperations<String, Object> zSetOps = mock(ReactiveZSetOperations.class);
        when(mockRedisTemplate.opsForZSet()).thenReturn(zSetOps);
        when(zSetOps.add(eq(key), eq(value), eq(score))).thenReturn(Mono.just(true));
        when(mockRedisTemplate.expire(eq(key), any(Duration.class))).thenReturn(Mono.just(true));

        StepVerifier.create(redisProviderUnderTest.setZset(key, value, score, expireTime)).verifyComplete();
    }

    @Test
    @DisplayName("刪除指定範圍的 ZSet 值 - 成功刪除")
    void deleteZset_range_completes() {
        String key = "testZSet";
        long start = 0;
        long end = 1;
        ReactiveZSetOperations<String, Object> zSetOps = mock(ReactiveZSetOperations.class);
        when(mockRedisTemplate.opsForZSet()).thenReturn(zSetOps);
        when(zSetOps.removeRange(eq(key), any(Range.class))).thenReturn(Mono.just(2L));

        StepVerifier.create(redisProviderUnderTest.deleteZset(key, start, end)).verifyComplete();
    }

    @Test
    @DisplayName("獲取分頁響應從 ZSet - 成功返回分頁對象")
    void getPagedResponseFromZset_validData_returnsPagedResponse() {
        String key = "testZSet";
        int page = 1;
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("data", Arrays.asList("item1", "item2"));
        responseData.put("totalElements", 2L);
        ReactiveZSetOperations<String, Object> zSetOps = mock(ReactiveZSetOperations.class);
        when(mockRedisTemplate.opsForZSet()).thenReturn(zSetOps);
        when(zSetOps.rangeByScore(eq(key), any(Range.class))).thenReturn(Flux.just(responseData));

        StepVerifier
                .create(redisProviderUnderTest.getPagedResponseFromZset(key, page, String.class))
                .expectNextMatches(response -> response.getData().size() == 2 && response.getData().contains("item1"))
                .verifyComplete();
    }

    @Test
    @DisplayName("刪除 Set 值 - 成功刪除")
    void deleteSet_specificValue_completes() {
        String key = "testSet";
        String value = "testValue";
        ReactiveSetOperations<String, Object> setOps = mock(ReactiveSetOperations.class);
        when(mockRedisTemplate.opsForSet()).thenReturn(setOps);
        when(setOps.remove(eq(key), eq(value))).thenReturn(Mono.just(1L));

        StepVerifier.create(redisProviderUnderTest.deleteSet(key, value)).verifyComplete();
    }

    @Test
    @DisplayName("獲取 Set 值並轉換類型 - 成功轉換並返回")
    void getSet_withTypeConversion_returnsConvertedValues() {
        String key = "testSet";
        List<Object> numbers = Arrays.asList(1, 2, 3);
        ReactiveSetOperations<String, Object> setOps = mock(ReactiveSetOperations.class);
        when(mockRedisTemplate.opsForSet()).thenReturn(setOps);
        when(setOps.members(key)).thenReturn(Flux.fromIterable(numbers));

        StepVerifier.create(redisProviderUnderTest.getSet(key, Integer.class)).expectNext(1, 2, 3).verifyComplete();
    }

    @Test
    @DisplayName("獲取 Set 值時類型轉換失敗 - 返回轉換錯誤")
    void getSet_invalidTypeConversion_returnsProcessException() {
        String key = "testSet";
        List<Object> invalidData = Arrays.asList("not a number", "invalid");
        ReactiveSetOperations<String, Object> setOps = mock(ReactiveSetOperations.class);
        when(mockRedisTemplate.opsForSet()).thenReturn(setOps);
        when(setOps.members(key)).thenReturn(Flux.fromIterable(invalidData));

        StepVerifier
                .create(redisProviderUnderTest.getSet(key, Integer.class))
                .expectErrorMatches(e -> e instanceof ProcessException processException && processException.getErrorCode() == ProcessException.ErrorCode.CONVERT_JSON_TO_TARGET_FAILED)
                .verify();
    }

    @Test
    @DisplayName("自增數值並設置過期時間 - 成功增加並返回")
    void incrementDelta_withExpiry_returnsIncrementedValue() {
        String key = "testKey";
        long delta = 1L;
        Duration expireTime = Duration.ofMinutes(5);
        ReactiveValueOperations<String, Object> valueOps = mock(ReactiveValueOperations.class);
        when(mockRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(eq(key), eq(delta))).thenReturn(Mono.just(1L));
        when(mockRedisTemplate.expire(eq(key), any(Duration.class))).thenReturn(Mono.just(true));

        StepVerifier.create(redisProviderUnderTest.incrementDelta(key, delta, expireTime)).expectNext(1L).verifyComplete();
    }

    @Test
    @DisplayName("僅在鍵不存在時設置值並帶過期時間 - 成功設置並返回")
    void setValueIfAbsent_withExpiry_returnsTrue() {
        String key = "testKey";
        String value = "testValue";
        Duration expireTime = Duration.ofMinutes(5);
        ReactiveValueOperations<String, Object> valueOps = mock(ReactiveValueOperations.class);
        when(mockRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(eq(key), eq(value))).thenReturn(Mono.just(true));
        when(mockRedisTemplate.expire(eq(key), any(Duration.class))).thenReturn(Mono.just(true));

        StepVerifier.create(redisProviderUnderTest.setValueIfAbsent(key, value, expireTime)).expectNext(true).verifyComplete();
    }

    @Test
    @DisplayName("使用無效的通配符模式 - 返回空 Flux")
    void deleteByPattern_invalidPattern_returnsEmpty() {
        String pattern = "[invalid";
        when(mockRedisTemplate.keys(pattern)).thenReturn(Flux.empty());
        when(mockRedisTemplate.delete(any(String[].class))).thenReturn(Mono.just(0L));

        StepVerifier.create(redisProviderUnderTest.deleteByPattern(pattern)).verifyComplete();
    }

    @Test
    @DisplayName("使用空的通配符模式 - 返回操作錯誤")
    void deleteByPattern_invalidPattern_returnsError() {
        String pattern = null;
        when(mockRedisTemplate.keys(pattern)).thenReturn(Flux.error(new IllegalArgumentException("Pattern must not be null")));

        StepVerifier
                .create(redisProviderUnderTest.deleteByPattern(pattern))
                .verifyErrorMatches(e -> e instanceof IllegalArgumentException && e.getMessage().equals("Pattern must not be null"));
    }


    @Test
    @DisplayName("獲取 List 內容超出範圍 - 返回空 Flux")
    void getListContent_outOfRange_returnsEmptyFlux() {
        String key = "testList";
        long start = 100;
        long end = 200;
        ReactiveListOperations<String, Object> listOps = mock(ReactiveListOperations.class);
        when(mockRedisTemplate.opsForList()).thenReturn(listOps);
        when(listOps.range(eq(key), eq(start), eq(end))).thenReturn(Flux.empty());

        StepVerifier.create(redisProviderUnderTest.getListContent(key, start, end, String.class)).verifyComplete();
    }

    @Test
    @DisplayName("分頁參數無效 - 返回空的分頁響應")
    void getPagedResponseFromZset_invalidPage_returnsEmpty() {
        String key = "testZSet";
        int invalidPage = -1;
        ReactiveZSetOperations<String, Object> zSetOps = mock(ReactiveZSetOperations.class);
        when(mockRedisTemplate.opsForZSet()).thenReturn(zSetOps);
        when(zSetOps.rangeByScore(eq(key), any(Range.class))).thenReturn(Flux.empty());

        StepVerifier.create(redisProviderUnderTest.getPagedResponseFromZset(key, invalidPage, String.class)).expectComplete().verify();
    }

    @Test
    @DisplayName("空列表操作 - 成功處理空列表")
    void list_emptyOperations_completes() {
        String key = "emptyList";
        ReactiveListOperations<String, Object> listOps = mock(ReactiveListOperations.class);
        when(mockRedisTemplate.opsForList()).thenReturn(listOps);
        when(listOps.range(eq(key), eq(0L), eq(-1L))).thenReturn(Flux.empty());
        when(listOps.size(key)).thenReturn(Mono.just(0L));

        StepVerifier.create(redisProviderUnderTest.getList(key)).verifyComplete();

        StepVerifier.create(listOps.size(key)).expectNext(0L).verifyComplete();
    }

    @Test
    @DisplayName("自訂類型序列化和反序列化 - 成功轉換")
    void customType_serializationAndDeserialization_succeeds() {
        String key = "testCustomType";
        Map<String, Object> customObject = Map.of("field1", "value1", "field2", 123);
        ReactiveValueOperations<String, Object> valueOps = mock(ReactiveValueOperations.class);
        when(mockRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.set(eq(key), eq(customObject))).thenReturn(Mono.just(true));
        when(valueOps.get(key)).thenReturn(Mono.just(customObject));

        StepVerifier.create(redisProviderUnderTest.setValue(key, customObject)).verifyComplete();

        StepVerifier
                .create(redisProviderUnderTest.getValue(key, Map.class))
                .expectNextMatches(result -> result.get("field1").equals("value1") && result.get("field2").equals(123))
                .verifyComplete();
    }

    @Test
    @DisplayName("併發多重操作 - 所有操作成功完成")
    void multipleOperations_concurrent_allComplete() {
        String key = "testMultiOp";
        ReactiveValueOperations<String, Object> valueOps = mock(ReactiveValueOperations.class);
        ReactiveSetOperations<String, Object> setOps = mock(ReactiveSetOperations.class);
        when(mockRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(mockRedisTemplate.opsForSet()).thenReturn(setOps);
        when(valueOps.increment(eq(key), eq(1L))).thenReturn(Mono.just(1L));
        when(setOps.add(eq(key), any())).thenReturn(Mono.just(1L));

        StepVerifier
                .create(Flux.merge(redisProviderUnderTest.incrementDelta(key, 1L), redisProviderUnderTest.setSet(key, "value")))
                .expectNext(1L)
                .verifyComplete();
    }


    @Test
    @DisplayName("設置過期時間後檢查鍵是否過期 - 過期後返回空")
    void keyExpiration_afterExpiry_returnsEmpty() {
        String key = "testExpiry";
        String value = "testValue";
        Duration expireTime = Duration.ofMinutes(5);
        ReactiveValueOperations<String, Object> valueOps = mock(ReactiveValueOperations.class);

        when(mockRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.set(eq(key), eq(value))).thenReturn(Mono.just(true));
        doReturn(Mono.just(true)).when(mockRedisTemplate).expire(eq(key), any(Duration.class));
        when(valueOps.get(key)).thenReturn(Mono.empty());

        StepVerifier.create(redisProviderUnderTest.setValue(key, value, expireTime)).verifyComplete();

        StepVerifier.create(redisProviderUnderTest.getValue(key, String.class)).verifyComplete();
    }

    @Test
    @DisplayName("批量刪除操作 - 成功刪除所有鍵")
    void batchDelete_multipleKeys_completes() {
        List<String> keys = Arrays.asList("key1", "key2", "key3", "key4");
        when(mockRedisTemplate.delete(any(String[].class))).thenReturn(Mono.just((long) keys.size()));

        StepVerifier.create(redisProviderUnderTest.deleteValue(keys)).verifyComplete();
    }

    @Test
    @DisplayName("網絡超時處理 - 返回錯誤")
    void networkTimeout_returnsError() {
        String key = "testTimeout";
        ReactiveValueOperations<String, Object> valueOps = mock(ReactiveValueOperations.class);
        when(mockRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(key)).thenReturn(Mono.error(new RuntimeException("Connection timeout")));

        StepVerifier
                .create(redisProviderUnderTest.getValue(key, String.class))
                .expectErrorMatches(error -> error.getMessage().equals("Connection timeout"))
                .verify();
    }

    @Test
    @DisplayName("鍵名格式無效 - 返回錯誤")
    void invalidKeyFormat_returnsError() {
        String invalidKey = " ";
        String value = "testValue";
        ReactiveValueOperations<String, Object> valueOps = mock(ReactiveValueOperations.class);
        when(mockRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.set(eq(invalidKey), eq(value))).thenReturn(Mono.error(new IllegalArgumentException("Invalid key format")));

        StepVerifier.create(redisProviderUnderTest.setValue(invalidKey, value)).expectError(IllegalArgumentException.class).verify();
    }

    @Test
    @DisplayName("設置過期時間為 0 - 直接返回成功")
    void setValue_zeroDurationExpiry_completes() {
        String key = "testKey";
        String value = "testValue";
        Duration zeroExpiry = Duration.ZERO;
        ReactiveValueOperations<String, Object> valueOps = mock(ReactiveValueOperations.class);
        when(mockRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.set(eq(key), eq(value))).thenReturn(Mono.just(true));
        when(mockRedisTemplate.expire(eq(key), any(Duration.class))).thenReturn(Mono.just(true));

        StepVerifier.create(redisProviderUnderTest.setValue(key, value, zeroExpiry)).verifyComplete();
    }

    @Test
    @DisplayName("設置極大過期時間 - 成功設置並返回")
    void setValue_maxDurationExpiry_completes() {
        String key = "testKey";
        String value = "testValue";
        Duration maxExpiry = Duration.ofDays(365 * 100);
        ReactiveValueOperations<String, Object> valueOps = mock(ReactiveValueOperations.class);
        when(mockRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.set(eq(key), eq(value))).thenReturn(Mono.just(true));
        when(mockRedisTemplate.expire(eq(key), any(Duration.class))).thenReturn(Mono.just(true));

        StepVerifier.create(redisProviderUnderTest.setValue(key, value, maxExpiry)).verifyComplete();
    }

    @Test
    @DisplayName("Redis 連線被拒絕 - 返回連線錯誤")
    void connection_refused_returnsError() {
        String key = "testKey";
        ReactiveValueOperations<String, Object> valueOps = mock(ReactiveValueOperations.class);
        when(mockRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(key)).thenReturn(Mono.error(new RuntimeException("Connection refused")));

        StepVerifier
                .create(redisProviderUnderTest.getValue(key, String.class))
                .expectErrorMatches(error -> error.getMessage().equals("Connection refused"))
                .verify();
    }

    @Test
    @DisplayName("Redis 操作超時重試 - 返回超時錯誤")
    void operation_timeoutWithRetry_returnsError() {
        String key = "testKey";
        ReactiveValueOperations<String, Object> valueOps = mock(ReactiveValueOperations.class);
        when(mockRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(key))
                .thenReturn(Mono.error(new RuntimeException("Operation timed out")))
                .thenReturn(Mono.error(new RuntimeException("Operation timed out")))
                .thenReturn(Mono.error(new RuntimeException("Operation timed out")));

        StepVerifier
                .create(redisProviderUnderTest.getValue(key, String.class))
                .expectErrorMatches(error -> error.getMessage().equals("Operation timed out"))
                .verify();
    }

    @Test
    @DisplayName("生成分塊集合失敗 - 返回錯誤")
    void generateChunkSet_error_returnsError() {
        String key = "testChunks";
        int totalChunks = 3;
        ReactiveSetOperations<String, Object> setOps = mock(ReactiveSetOperations.class);
        when(mockRedisTemplate.opsForSet()).thenReturn(setOps);
        when(setOps.add(eq(key), any())).thenReturn(Mono.error(new RuntimeException("Redis error")));

        StepVerifier
                .create(redisProviderUnderTest.generateChunkSet(key, totalChunks))
                .expectErrorMatches(error -> error.getMessage().equals("Redis error"))
                .verify();
    }

}
