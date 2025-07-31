package xyz.dowob.filemanagement.component.provider.providerImplement.cacheprovider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.util.unit.DataSize;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.dowob.filemanagement.component.provider.provider.RedisProvider;
import xyz.dowob.filemanagement.config.properties.CacheProperties;
import xyz.dowob.filemanagement.data.file.po.FluxDataPO;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * StreamCacheProviderImpl 測試類別。
 * 
 * <p>測試 StreamCacheProviderImpl 的串流緩存功能，包括：
 * <ul>
 * <li>構造函數初始化及參數驗證</li>
 * <li>串流資料的分塊存储與獲取</li>
 * <li>DataBuffer 與 FluxDataPO 的轉換處理</li>
 * <li>單一及多個區塊的缅存操作</li>
 * <li>批量操作與緩存失敗後的清理</li>
 * <li>Base64 編解碼及資料完整性驗證</li>
 * <li>空串流與錯誤情況處理</li>
 * </ul>
 * 
 * <p>測試涵蓋 DataBuffer 與 Flux 的非同步處理，包含資料分塊、緩存還原、
 * 及各種邊界情況的正確處理。透過模擬 Redis 操作驗證串流緩存策略的可靠性。
 * 
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StreamCacheProvider 邏輯處理測試")
class StreamCacheProviderImplTest {

    private final String CACHE_PREFIX = "testStreamPrefix:";

    private final int CHUNK_SIZE = 10;

    private final DataSize mockChunkSize = mock(DataSize.class);

    private final Duration DEFAULT_EXPIRE_TIME = Duration.ofSeconds(60);

    private final DefaultDataBufferFactory bufferFactory = new DefaultDataBufferFactory();

    @Mock
    private RedisProvider mockRedisProvider;

    @Mock
    private CacheProperties mockCacheProperties;

    private StreamCacheProviderImpl streamCacheProviderImplUnderTest;


    @BeforeEach
    void setUp() {
        when(mockCacheProperties.getDownloadCachePrefix()).thenReturn(CACHE_PREFIX);
        when(mockCacheProperties.getChunkSize()).thenReturn(mockChunkSize);
        when(mockChunkSize.toBytes()).thenReturn((long) CHUNK_SIZE);
        when(mockCacheProperties.getDownloadCacheExpireTime()).thenReturn(DEFAULT_EXPIRE_TIME);

        streamCacheProviderImplUnderTest = new StreamCacheProviderImpl(mockRedisProvider, mockCacheProperties);
    }


    @Test
    @DisplayName("構造函數初始化 - 成功")
    void constructor_validProperties_initializesSuccessfully() {
        assertThat(streamCacheProviderImplUnderTest.getDefaultExpire()).isEqualTo(DEFAULT_EXPIRE_TIME);
    }


    @Test
    @DisplayName("構造函數初始化 - ChunkSize 為 0 - 拋出 IllegalArgumentException")
    void constructor_chunkSizeZero_throwsIllegalArgumentException() {
        when(mockChunkSize.toBytes()).thenReturn(0L);
        assertThatThrownBy(() -> new StreamCacheProviderImpl(mockRedisProvider, mockCacheProperties))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("下載流緩存塊大小必須大於0");
    }


    @Test
    @DisplayName("構造函數初始化 - ExpireTime 為負數 - 拋出 IllegalArgumentException")
    void constructor_expireTimeNegative_throwsIllegalArgumentException() {
        when(mockCacheProperties.getDownloadCacheExpireTime()).thenReturn(Duration.ofSeconds(-1));
        assertThatThrownBy(() -> new StreamCacheProviderImpl(mockRedisProvider, mockCacheProperties))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("下載流緩存過期時間必須大於0");
    }


    @Test
    @DisplayName("getDefaultExpire - 返回預設過期時間 - 成功")
    void getDefaultExpire_returnsDefaultExpireTime() {
        assertThat(streamCacheProviderImplUnderTest.getDefaultExpire()).isEqualTo(DEFAULT_EXPIRE_TIME);
    }


    @Test
    @DisplayName("get - 緩存存在且單一區塊 - 返回 FluxDataPO<DataBuffer>")
    void get_cacheExistsSingleChunk_returnsFluxDataPO() {
        String key = "testKey1";
        String originalData = "TestData";
        String base64Data = toBase64(originalData);

        Map.Entry<String, String> chunk1 = new AbstractMap.SimpleEntry<>(key + "_1", base64Data);
        when(mockRedisProvider.getHashMapByPattern(CACHE_PREFIX, key + "_*", String.class)).thenReturn(Flux.just(chunk1));

        Mono<FluxDataPO<DataBuffer>> result = streamCacheProviderImplUnderTest.get(key, (Class<FluxDataPO<DataBuffer>>) (Class<?>) FluxDataPO.class);

        StepVerifier.create(result).assertNext(fluxDataPO -> {
            assertThat(fluxDataPO).isNotNull();
            StepVerifier.create(fluxDataBufferToString(fluxDataPO.getTFlux())).expectNext(originalData).verifyComplete();
        }).verifyComplete();
    }


    private String toBase64(String input) {
        if (input == null) {
            return null;
        }
        return Base64.getEncoder().encodeToString(input.getBytes(StandardCharsets.UTF_8));
    }


    private Mono<String> fluxDataBufferToString(Flux<DataBuffer> flux) {
        return flux.map(db -> {
            byte[] bytes = new byte[db.readableByteCount()];
            db.read(bytes);
            DataBufferUtils.release(db);
            return new String(bytes, StandardCharsets.UTF_8);
        }).collect(Collectors.joining());
    }


    @Test
    @DisplayName("get - 緩存存在且多個區塊 - 返回 FluxDataPO<DataBuffer>")
    void get_cacheExistsMultipleChunks_returnsFluxDataPO() {
        String key = "testKeyMulti";
        String originalData = "ThisIsLongerTestData";
        String base64OriginalData = toBase64(originalData);

        String chunk1Data = base64OriginalData.substring(0, CHUNK_SIZE);
        String chunk2Data = base64OriginalData.substring(CHUNK_SIZE, Math.min(CHUNK_SIZE * 2, base64OriginalData.length()));
        String chunk3Data = "";
        if (base64OriginalData.length() > CHUNK_SIZE * 2) {
            chunk3Data = base64OriginalData.substring(CHUNK_SIZE * 2);
        }

        Map.Entry<String, String> entry1 = new AbstractMap.SimpleEntry<>(key + "_1", chunk1Data);
        Map.Entry<String, String> entry2 = new AbstractMap.SimpleEntry<>(key + "_2", chunk2Data);

        List<Map.Entry<String, String>> entries = new ArrayList<>();
        entries.add(entry1);
        entries.add(entry2);
        if (!chunk3Data.isEmpty()) {
            Map.Entry<String, String> entry3 = new AbstractMap.SimpleEntry<>(key + "_3", chunk3Data);
            entries.add(entry3);
        }

        List<Map.Entry<String, String>> shuffledEntries = new ArrayList<>(entries);
        Collections.shuffle(shuffledEntries);

        when(mockRedisProvider.getHashMapByPattern(CACHE_PREFIX, key + "_*", String.class)).thenReturn(Flux.fromIterable(shuffledEntries));

        Mono<FluxDataPO<DataBuffer>> result = streamCacheProviderImplUnderTest.get(key, (Class<FluxDataPO<DataBuffer>>) (Class<?>) FluxDataPO.class);

        StepVerifier.create(result).assertNext(fluxDataPO -> {
            assertThat(fluxDataPO).isNotNull();
            StepVerifier.create(fluxDataBufferToString(fluxDataPO.getTFlux())).expectNext(originalData).verifyComplete();
        }).verifyComplete();
    }


    @Test
    @DisplayName("get - 緩存不存在 - 返回 Mono.empty")
    void get_cacheDoesNotExist_returnsMonoEmpty() {
        String key = "nonExistentKey";
        when(mockRedisProvider.getHashMapByPattern(CACHE_PREFIX, key + "_*", String.class)).thenReturn(Flux.empty());

        Mono<FluxDataPO<DataBuffer>> result = streamCacheProviderImplUnderTest.get(key, (Class<FluxDataPO<DataBuffer>>) (Class<?>) FluxDataPO.class);

        StepVerifier.create(result).verifyComplete();
    }


    @Test
    @DisplayName("getAsList - 緩存存在且單一區塊 - 返回 List<DataBuffer>")
    void getAsList_cacheExistsSingleChunk_returnsListOfDataBuffer() {
        String key = "listKey1";
        String originalData = "ListData";
        String base64Data = toBase64(originalData);

        Map.Entry<String, String> chunk1 = new AbstractMap.SimpleEntry<>(key + "_1", base64Data);
        when(mockRedisProvider.getHashMapByPattern(CACHE_PREFIX, key + "_*", String.class)).thenReturn(Flux.just(chunk1));

        Mono<List<DataBuffer>> result = streamCacheProviderImplUnderTest.getAsList(key, DataBuffer.class);

        StepVerifier.create(result).assertNext(list -> {
            assertThat(list).hasSize(1);
            StepVerifier.create(fluxDataBufferToString(Flux.fromIterable(list))).expectNext(originalData).verifyComplete();
        }).verifyComplete();
    }


    @Test
    @DisplayName("getAsList - 緩存不存在 - 返回空列表的 Mono")
    void getAsList_cacheDoesNotExist_returnsMonoEmptyList() {
        String key = "nonExistentListKey";
        when(mockRedisProvider.getHashMapByPattern(CACHE_PREFIX, key + "_*", String.class)).thenReturn(Flux.empty());

        Mono<List<DataBuffer>> result = streamCacheProviderImplUnderTest.getAsList(key, DataBuffer.class);

        StepVerifier.create(result).expectNextMatches(List::isEmpty).verifyComplete();
    }


    @Test
    @DisplayName("getAllAsMapList - 多個鍵，部分存在緩存 - 返回 Map<String, List<DataBuffer>>")
    void getAllAsMapList_multipleKeysSomeExist_returnsMapOfLists() {
        String key1 = "mapListKey1";
        String originalData1 = "DataOne";
        String base64Data1 = toBase64(originalData1);
        Map.Entry<String, String> chunk1_1 = new AbstractMap.SimpleEntry<>(key1 + "_1", base64Data1);

        String key2 = "mapListKey2";
        String key3 = "mapListKey3";
        String originalData3 = "DataThree";
        String base64Data3 = toBase64(originalData3);
        Map.Entry<String, String> chunk3_1 = new AbstractMap.SimpleEntry<>(key3 + "_1", base64Data3);

        when(mockRedisProvider.getHashMapByPattern(CACHE_PREFIX, key1 + "_*", String.class)).thenReturn(Flux.just(chunk1_1));
        when(mockRedisProvider.getHashMapByPattern(CACHE_PREFIX, key2 + "_*", String.class)).thenReturn(Flux.empty());
        when(mockRedisProvider.getHashMapByPattern(CACHE_PREFIX, key3 + "_*", String.class)).thenReturn(Flux.just(chunk3_1));

        Collection<String> keys = Arrays.asList(key1, key2, key3);
        Mono<Map<String, List<DataBuffer>>> result = streamCacheProviderImplUnderTest.getAllAsMapList(keys, DataBuffer.class);

        StepVerifier.create(result).assertNext(map -> {
            assertThat(map).hasSize(3);
            StepVerifier.create(fluxDataBufferToString(Flux.fromIterable(map.get(key1)))).expectNext(originalData1).verifyComplete();
            assertThat(map.get(key2)).isEmpty();
            StepVerifier.create(fluxDataBufferToString(Flux.fromIterable(map.get(key3)))).expectNext(originalData3).verifyComplete();
        }).verifyComplete();
    }


    @Test
    @DisplayName("set - 值為 FluxDataPO<DataBuffer> 單一區塊 - 成功寫入 Redis")
    void set_valueFluxDataPOSingleChunk_writesToRedis() {
        String key = "setKey1";
        String originalData = "SetData";
        Flux<DataBuffer> dataFlux = stringToFluxDataBuffer(originalData);
        FluxDataPO<DataBuffer> fluxPO = createFluxDataPO(dataFlux);
        Duration expire = Duration.ofMinutes(5);

        when(mockRedisProvider.setHashMap(eq(CACHE_PREFIX), anyString(), any(), eq(expire))).thenReturn(Mono.empty());

        Mono<Void> result = streamCacheProviderImplUnderTest.set(key, fluxPO, expire);
        StepVerifier.create(result).verifyComplete();
        verify(mockRedisProvider, times(2)).setHashMap(eq(CACHE_PREFIX), anyString(), any(), eq(expire));
    }


    private Flux<DataBuffer> stringToFluxDataBuffer(String s) {
        if (s == null || s.isEmpty()) {
            return Flux.empty();
        }
        return Flux.just(stringToDataBuffer(s));
    }


    private <T> FluxDataPO<T> createFluxDataPO(Flux<T> flux) {
        FluxDataPO<T> po = new FluxDataPO<>();
        po.setTFlux(flux);
        return po;
    }


    private DataBuffer stringToDataBuffer(String s) {
        return bufferFactory.wrap(s.getBytes(StandardCharsets.UTF_8));
    }


    @Test
    @DisplayName("set - 值為 Collection<DataBuffer> 多個區塊 - 成功寫入 Redis")
    void set_valueCollectionDataBufferMultipleChunks_writesToRedis() {
        String key = "setKeyMulti";
        String originalData = "ThisIsLongerSetDataContent";
        DataBuffer db1 = stringToDataBuffer(originalData.substring(0, 8));
        DataBuffer db2 = stringToDataBuffer(originalData.substring(8, 15));
        DataBuffer db3 = stringToDataBuffer(originalData.substring(15));
        Collection<DataBuffer> dataCollection = Arrays.asList(db1, db2, db3);
        Duration expire = null;

        String expectedBase64 = toBase64(originalData);
        int numChunks = (int) Math.ceil((double) expectedBase64.length() / CHUNK_SIZE);

        for (int i = 0; i < numChunks; i++) {
            String chunkKey = key + "_" + (i + 1);
            int start = i * CHUNK_SIZE;
            int end = Math.min(start + CHUNK_SIZE, expectedBase64.length());
            String chunkData = expectedBase64.substring(start, end);
            when(mockRedisProvider.setHashMap(CACHE_PREFIX, chunkKey, chunkData, DEFAULT_EXPIRE_TIME)).thenReturn(Mono.empty());
        }

        Mono<Void> result = streamCacheProviderImplUnderTest.set(key, dataCollection, expire);
        StepVerifier.create(result).verifyComplete();

        for (int i = 0; i < numChunks; i++) {
            String chunkKey = key + "_" + (i + 1);
            int start = i * CHUNK_SIZE;
            int end = Math.min(start + CHUNK_SIZE, expectedBase64.length());
            String chunkData = expectedBase64.substring(start, end);
            verify(mockRedisProvider).setHashMap(CACHE_PREFIX, chunkKey, chunkData, DEFAULT_EXPIRE_TIME);
        }
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    @DisplayName("set - Redis 寫入部分區塊失敗 - 觸發 deleteUnCompletedCache")
    void set_redisWriteFailsForOneChunk_triggersDelete() {
        String key = "setErrorKey";
        String originalData = "ErrorDataContentIsLongEnough";
        Flux<DataBuffer> dataFlux = stringToFluxDataBuffer(originalData);
        Duration expire = Duration.ofMinutes(10);

        String expectedBase64 = toBase64(originalData);
        int totalChunks = (int) Math.ceil((double) expectedBase64.length() / CHUNK_SIZE);

        for (int i = 0; i < totalChunks; i++) {
            String chunkKey = key + "_" + (i + 1);
            int start = i * CHUNK_SIZE;
            int end = Math.min(start + CHUNK_SIZE, expectedBase64.length());
            String chunkData = expectedBase64.substring(start, end);

            if (i == 1) {
                when(mockRedisProvider.setHashMap(CACHE_PREFIX, chunkKey, chunkData, expire)).thenReturn(Mono.error(new RuntimeException(
                        "Redis write failed for chunk " + (i + 1))));
            } else {
                when(mockRedisProvider.setHashMap(CACHE_PREFIX, chunkKey, chunkData, expire)).thenReturn(Mono.empty());
            }
        }

        List<String> keysToDelete = IntStream.rangeClosed(1, totalChunks).mapToObj(j -> key + "_" + j).collect(Collectors.toList());
        when(mockRedisProvider.deleteHash(CACHE_PREFIX, keysToDelete)).thenReturn(Mono.empty());

        Mono<Void> result = streamCacheProviderImplUnderTest.set(key, dataFlux, expire);
        StepVerifier.create(result).verifyComplete();

        verify(mockRedisProvider).setHashMap(CACHE_PREFIX, key + "_1", expectedBase64.substring(0, CHUNK_SIZE), expire);
        verify(mockRedisProvider).setHashMap(CACHE_PREFIX,
                                             key + "_2",
                                             expectedBase64.substring(CHUNK_SIZE, Math.min(CHUNK_SIZE * 2, expectedBase64.length())),
                                             expire
        );
        verify(mockRedisProvider).deleteHash(CACHE_PREFIX, keysToDelete);

        if (totalChunks > 2) {
            String chunkKey3 = key + "_3";
            String chunkData3 = expectedBase64.substring(CHUNK_SIZE * 2, Math.min(CHUNK_SIZE * 3, expectedBase64.length()));
            verify(mockRedisProvider, never()).setHashMap(CACHE_PREFIX, chunkKey3, chunkData3, expire);
        }
    }

    @Test
    @DisplayName("set - 值為不支援的 Flux 類型 - 拋出 UnsupportedOperationException")
    void set_valueUnsupportedFluxType_throwsException() {
        String key = "unsupportedTypeKey";
        Flux<String> unsupportedFlux = Flux.just("test");
        Duration expire = Duration.ofMinutes(5);

        Mono<Void> result = streamCacheProviderImplUnderTest.set(key, unsupportedFlux, expire);
        StepVerifier.create(result).expectError(UnsupportedOperationException.class).verify();
    }

    @Test
    @DisplayName("set - 空的 FluxDataBuffer - 不執行 Redis 寫入")
    void set_emptyFluxDataBuffer_doesNotWriteToRedis() {
        String key = "emptyFluxKey";
        Flux<DataBuffer> emptyFlux = Flux.empty();
        Duration expire = Duration.ofMinutes(5);

        Mono<Void> result = streamCacheProviderImplUnderTest.set(key, emptyFlux, expire);
        StepVerifier.create(result).verifyComplete();
        verify(mockRedisProvider, never()).setHashMap(anyString(), anyString(), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("setAll - 多個鍵值對 - 成功調用 set 方法")
    void setAll_multipleKeyValues_callsSetForEach() {
        String key1 = "setAllKey1";
        String originalData1 = "DataOne";
        Flux<DataBuffer> flux1 = stringToFluxDataBuffer(originalData1);

        String key2 = "setAllKey2";
        String originalData2 = "DataTwoLonger";
        Flux<DataBuffer> flux2 = stringToFluxDataBuffer(originalData2);

        Map<String, Object> keyValues = new LinkedHashMap<>();
        keyValues.put(key1, flux1);
        keyValues.put(key2, flux2);
        Duration expire = Duration.ofMinutes(30);

        doAnswer(invocation -> Mono.empty()).when(mockRedisProvider).setHashMap(eq(CACHE_PREFIX), anyString(), anyString(), eq(expire));

        Mono<Void> result = streamCacheProviderImplUnderTest.setAll(keyValues, expire);
        StepVerifier.create(result).verifyComplete();
    }

    @Test
    @DisplayName("setAll - 空 Map - 不執行任何操作並成功返回")
    void setAll_emptyMap_completesSuccessfullyWithoutAction() {
        Map<String, Object> keyValues = Collections.emptyMap();
        Duration expire = Duration.ofMinutes(5);

        Mono<Void> result = streamCacheProviderImplUnderTest.setAll(keyValues, expire);
        StepVerifier.create(result).verifyComplete();

        verifyNoInteractions(mockRedisProvider);
    }
}
