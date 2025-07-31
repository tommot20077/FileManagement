package xyz.dowob.filemanagement.component.provider.providerImplement.cacheprovider;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import xyz.dowob.filemanagement.annotation.CacheProviderType;
import xyz.dowob.filemanagement.component.provider.provider.RedisProvider;
import xyz.dowob.filemanagement.component.provider.providerInterface.CacheProvider;
import xyz.dowob.filemanagement.config.properties.CacheProperties;
import xyz.dowob.filemanagement.customenum.CacheProviderEnum;
import xyz.dowob.filemanagement.data.file.po.FluxDataPO;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 基於 Redis 的檔案流緩存提供者實現，專為大型檔案流的高效緩存設計。
 * 透過分塊存儲策略將檔案流分割為可配置大小的緩存塊，結合 Base64 編碼確保資料完整性。
 * <p>
 * 核心特性包括原子性緩存操作、自動錯誤恢復機制和記憶體最佳化。當任一緩存塊寫入失敗時，
 * 系統會自動執行清理操作以維護資料一致性。緩存塊大小預設可調整，適用於不同規模的檔案流處理需求。
 * <p>
 * 此實現僅在配置屬性 cache.enable-file-download-stream-cache 為 true 時生效，預設為啟用狀態。
 * 支援 FluxDataPO、Collection 和 Flux 等多種資料類型的緩存操作。
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see CacheProvider
 * @see RedisProvider
 * @see CacheProperties
 * @see FluxDataPO
 */
@Component
@CacheProviderType(CacheProviderEnum.FILE_STREAM_CACHE)
@ConditionalOnProperty(prefix = "cache", name = "enable-file-download-stream-cache", havingValue = "true", matchIfMissing = true)
public class StreamCacheProviderImpl implements CacheProvider {
    /**
     * Redis 操作提供者，用於執行底層的 Redis 緩存操作
     */
    private final RedisProvider redisProvider;

    /**
     * 緩存的預設過期時間，來自配置檔案設定
     */
    private final Duration DEFAULT_EXPIRE_TIME;

    /**
     * 緩存鍵的前綴，用於區分不同類型的緩存資料
     */
    private final String CACHE_PREFIX;

    /**
     * 單個緩存塊的位元組大小，用於分割大型檔案流
     */
    private final int CHUNK_SIZE;


    /**
     * 建構檔案流緩存提供者實例。
     * 初始化緩存配置並驗證設定參數的有效性。
     *
     * @param redisProvider   Redis 操作提供者，不可為 null
     * @param cacheProperties 緩存配置屬性，包含緩存塊大小與過期時間設定
     * @throws IllegalArgumentException 當緩存塊大小小於等於 0 或過期時間為負值時
     */
    public StreamCacheProviderImpl(RedisProvider redisProvider, CacheProperties cacheProperties) {
        Assert.isTrue(cacheProperties.getChunkSize().toBytes() > 0, "下載流緩存塊大小必須大於0");
        Assert.isTrue(cacheProperties.getDownloadCacheExpireTime().isPositive(), "下載流緩存過期時間必須大於0");
        this.redisProvider = redisProvider;
        this.CACHE_PREFIX = cacheProperties.getDownloadCachePrefix();
        this.CHUNK_SIZE = (int) cacheProperties.getChunkSize().toBytes();
        this.DEFAULT_EXPIRE_TIME = cacheProperties.getDownloadCacheExpireTime();
    }


    /**
     * 根據指定鍵值查詢緩存的檔案流資料。
     * 此方法會自動重組分散在多個緩存塊中的資料，按照塊編號順序重新組合，
     * 並將 Base64 編碼的字串解碼轉換回 DataBuffer 流包裝為 FluxDataPO 物件。
     *
     * @param key   緩存鍵值，用於識別特定的檔案流資料，不可為 null
     * @param clazz 期望的回傳類型，必須是 FluxDataPO 的相容類型
     * @param <T>   泛型類型參數，通常為 FluxDataPO 或其子類型
     * @return 包含檔案流資料的 Mono，若緩存不存在則回傳空的 Mono
     */
    @Override
    public <T> Mono<T> get(String key, Class<T> clazz) {
        return redisProvider.getHashMapByPattern(CACHE_PREFIX, key + "_*", String.class).collectList().flatMap(list -> {
            if (list.isEmpty()) {
                return Mono.empty();
            }
            StringBuilder stringBuilder = new StringBuilder();
            list
                    .stream()
                    .sorted(Comparator.comparingInt(o -> Integer.parseInt(o.getKey().substring(o.getKey().lastIndexOf("_") + 1))))
                    .forEach(entry -> stringBuilder.append(entry.getValue()));
            return Mono.just(stringBuilder.toString());
        }).map(base64 -> {
            Flux<DataBuffer> dataBufferFlux = formatBase64ToStream(base64);
            return new FluxDataPO<>(dataBufferFlux);
        }).cast(clazz);
    }


    /**
     * 查詢指定鍵值的緩存資料並以列表形式回傳。
     * 重組緩存塊後將資料流解碼為 DataBuffer，再轉換為指定類型的元素列表。
     * 適用於需要將檔案流緩存轉換為多個離散物件的場景。
     *
     * @param key   緩存鍵值，不可為 null
     * @param clazz 列表元素的類型，必須與緩存內容相容
     * @param <T>   泛型類型參數，代表列表元素類型
     * @return 包含緩存資料列表的 Mono，若緩存不存在則回傳空列表
     */
    @Override
    public <T> Mono<List<T>> getAsList(String key, Class<T> clazz) {
        return redisProvider.getHashMapByPattern(CACHE_PREFIX, key + "_*", String.class).collectList().flatMap(list -> {
            if (list.isEmpty()) {
                return Mono.just(Collections.emptyList());
            }
            StringBuilder stringBuilder = new StringBuilder();
            list
                    .stream()
                    .sorted(Comparator.comparingInt(o -> Integer.parseInt(o.getKey().substring(o.getKey().lastIndexOf("_") + 1))))
                    .forEach(entry -> stringBuilder.append(entry.getValue()));
            return Mono.just(stringBuilder.toString()).flatMapMany(base64 -> formatBase64ToStream(base64).cast(clazz)).collectList();
        });
    }


    /**
     * 批量查詢多個緩存鍵對應的資料列表。
     * 針對每個鍵值並行執行查詢操作，並將結果整合為鍵值與資料列表的映射。
     * 適用於需要同時讀取多個檔案流緩存的批次處理場景。
     *
     * @param keys  要查詢的緩存鍵值集合，不可為 null 且不可包含 null 元素
     * @param clazz 列表元素的類型，必須與緩存內容相容
     * @param <T>   泛型類型參數，代表列表元素類型
     * @return 包含鍵值與對應資料列表映射的 Mono，若某鍵值無緩存則對應空列表
     */
    @Override
    public <T> Mono<Map<String, List<T>>> getAllAsMapList(Collection<String> keys, Class<T> clazz) {
        HashMap<String, List<T>> map = new HashMap<>();
        return Flux.fromIterable(keys).flatMap(key -> getAsList(key, clazz).doOnNext(list -> map.put(key, list))).then(Mono.just(map));
    }


    /**
     * 設定檔案流緩存，採用分塊存儲策略以處理大型檔案。
     * 將資料流轉換為 Base64 編碼並按配置的塊大小分割為多個緩存塊進行原子性存儲。
     * 若任一緩存塊寫入失敗，會自動觸發清理機制刪除所有相關緩存塊以確保資料一致性。
     * 支援多種資料類型的自動轉換和處理。
     *
     * @param key    緩存鍵值，不可為 null 或空字串
     * @param value  要緩存的資料，支援 FluxDataPO、Collection 或 Flux&lt;DataBuffer&gt; 類型
     * @param expire 緩存過期時間，若為 null 則使用預設過期時間
     * @return 完成設定操作的 Mono&lt;Void&gt;
     * @throws UnsupportedOperationException 當資料類型不受支援時拋出
     */
    @Override
    public Mono<Void> set(String key, Object value, Duration expire) {
        Duration chooseTime = Objects.requireNonNullElse(expire, DEFAULT_EXPIRE_TIME);

        Flux<DataBuffer> dataBufferFlux;
        if (value instanceof FluxDataPO<?> dataPO) {
            FluxDataPO<DataBuffer> dataBufferPO = new FluxDataPO<>();
            dataBufferFlux = dataBufferPO.formatAndSet(dataPO.getTFlux(), DataBuffer.class);
        } else if (value instanceof Collection<?> c) {
            dataBufferFlux = Flux.fromIterable(c).cast(DataBuffer.class);
        } else {
            dataBufferFlux = ((Flux<?>) value).flatMap(o -> {
                if (o instanceof DataBuffer) {
                    return Flux.just((DataBuffer) o);
                }
                return Flux.error(new UnsupportedOperationException("不支持的操作類型: " + o.getClass().getName()));
            });
        }
        return formatStreamToBase64(dataBufferFlux).flatMapMany(base64 -> {
            int totalChunks = (int) Math.ceil((double) base64.length() / CHUNK_SIZE);
            AtomicBoolean errorOccurred = new AtomicBoolean(false);

            return Flux.range(0, totalChunks).flatMap(i -> {
                if (errorOccurred.get()) {
                    return Mono.empty();
                }

                String chunkKey = key + "_" + (i + 1);
                int start = i * CHUNK_SIZE;
                int end = Math.min(start + CHUNK_SIZE, base64.length());
                String chunkData = base64.substring(start, end);

                return redisProvider.setHashMap(CACHE_PREFIX, chunkKey, chunkData, chooseTime).onErrorResume(e -> {
                    if (errorOccurred.compareAndSet(false, true)) {
                        return deleteUnCompletedCache(key, totalChunks);
                    }
                    return Mono.empty();
                });
            });
        }).then();
    }


    /**
     * 批量設定多個緩存項目。
     * 對每個鍵值對並行執行個別的分塊緩存設定操作，確保批次處理的效率。
     * 每個項目的緩存策略與單一設定操作相同，包括分塊存儲和錯誤恢復機制。
     *
     * @param keyValues 包含鍵值與對應資料的映射，不可為 null
     * @param expire    所有緩存項目的統一過期時間，若為 null 則使用預設過期時間
     * @return 完成所有設定操作的 Mono&lt;Void&gt;
     */
    @Override
    public Mono<Void> setAll(Map<String, Object> keyValues, Duration expire) {
        return keyValues.entrySet().stream().map(entry -> set(entry.getKey(), entry.getValue(), expire)).reduce(Mono::then).orElse(Mono.empty());
    }


    /**
     * 取得緩存的預設過期時間。
     * 此時間設定來自於系統配置，用於所有未明確指定過期時間的緩存操作。
     *
     * @return 預設過期時間設定，保證為正值
     */
    @Override
    public Duration getDefaultExpire() {
        return DEFAULT_EXPIRE_TIME;
    }


    /**
     * 將 DataBuffer 流轉換為 Base64 編碼字串。
     * 依序讀取所有 DataBuffer 的位元組資料，合併為完整的位元組陣列後進行 Base64 編碼。
     * 此方法會自動釋放 DataBuffer 資源以防止記憶體洩漏。
     *
     * @param dataBufferFlux 要轉換的資料流，不可為 null
     * @return 包含 Base64 編碼字串的 Mono
     * @throws RuntimeException 當寫入位元組陣列時發生 I/O 錯誤時包裝並拋出
     */
    private Mono<String> formatStreamToBase64(Flux<DataBuffer> dataBufferFlux) {
        return dataBufferFlux.map(dataBuffer -> {
            byte[] bytes = new byte[dataBuffer.readableByteCount()];
            dataBuffer.read(bytes);
            DataBufferUtils.release(dataBuffer);
            return bytes;
        }).collectList().map(byteList -> {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(byteList.size());
            byteList.forEach(bytes -> {
                try {
                    outputStream.write(bytes);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        });
    }


    /**
     * 清理未完成的緩存塊以維護資料一致性。
     * 當緩存寫入過程中發生錯誤時，自動刪除所有相關的緩存塊以避免部分寫入造成的資料不完整。
     * 採用指數退避重試策略（最多重試 3 次，初始間隔 1 分鐘）確保清理操作的可靠性。
     *
     * @param key         緩存的基礎鍵值，用於構建要刪除的緩存塊鍵名
     * @param totalChunks 需要清理的緩存塊總數，必須為正整數
     * @return 完成清理操作的 Mono&lt;Void&gt;
     */
    private Mono<Void> deleteUnCompletedCache(String key, int totalChunks) {
        List<String> keysToDelete = new ArrayList<>();
        for (int i = 1; i <= totalChunks; i++) {
            keysToDelete.add(key + "_" + i);
        }
        return redisProvider.deleteHash(CACHE_PREFIX, keysToDelete).retryWhen(Retry.backoff(3, Duration.ofMinutes(1))).then();
    }


    /**
     * 將 Base64 編碼字串轉換為 DataBuffer 流。
     * 使用標準 Base64 解碼器解碼字串，並使用預設的 DataBufferFactory 包裝為 DataBuffer。
     * 轉換後的 DataBuffer 可直接用於 WebFlux 響應式流處理。
     *
     * @param base64 要轉換的 Base64 編碼字串，不可為 null 或無效的 Base64 格式
     * @return 包含解碼資料的 DataBuffer 流
     * @throws IllegalArgumentException 當 Base64 字串格式無效時由解碼器拋出
     */
    private Flux<DataBuffer> formatBase64ToStream(String base64) {
        byte[] bytes = Base64.getDecoder().decode(base64);
        return Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(bytes));
    }
}
