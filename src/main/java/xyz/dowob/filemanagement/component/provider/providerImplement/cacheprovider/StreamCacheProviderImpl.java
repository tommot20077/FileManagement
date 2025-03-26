package xyz.dowob.filemanagement.component.provider.providerImplement.cacheprovider;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.CacheProviderType;
import xyz.dowob.filemanagement.annotation.SkipRecord;
import xyz.dowob.filemanagement.component.provider.provider.RedisProvider;
import xyz.dowob.filemanagement.component.provider.providerInterface.CacheProvider;
import xyz.dowob.filemanagement.config.properties.CacheProperties;
import xyz.dowob.filemanagement.customenum.CacheProviderEnum;
import xyz.dowob.filemanagement.data.file.po.FluxDataPO;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.*;

/**
 * 文件流緩存提供者實現類，用於提供文件流緩存的操作
 * 對於每一次請求都需要從GridFS中獲取文件流，這樣會對服務器造成壓力，因此提供對於檔案的數據流進行緩存
 * 透過繼承AbstractRedisCacheProvider，實現了CacheProvider接口，提供了緩存操作的具體實現
 * 此類透過緩存設定enable-file-stream-cache來判斷是否啟用文件流緩存，當開啟時此類才會生效，默認開啟 {@link CacheProperties}
 * 文件流緩存的key前綴以及緩存的默認過期時間來自於檔案設定
 *
 * @author yuan
 * @program FileManagement
 * @ClassName StreamCacheProviderImpl
 * @create 2025/3/15
 * @Version 1.0
 **/
@Component
@SkipRecord
@CacheProviderType(CacheProviderEnum.FILE_STREAM_CACHE)
@ConditionalOnProperty(prefix = "cache", name = "enable-file-download-stream-cache", havingValue = "true", matchIfMissing = true)
public class StreamCacheProviderImpl implements CacheProvider {
    /**
     * Redis操作提供者
     */
    private final RedisProvider redisProvider;

    /**
     * 默認過期時間
     */
    private final Duration DEFAULT_EXPIRE_TIME;

    /**
     * 緩存前綴
     */
    private final String CACHE_PREFIX;

    /**
     * 單個緩存塊的大小
     */
    private final int CHUNK_SIZE;

    /**
     * 文件流緩存提供者實現類的構造方法
     *
     * @param redisProvider Redis操作提供者
     */
    public StreamCacheProviderImpl(RedisProvider redisProvider, CacheProperties cacheProperties) {
        this.redisProvider = redisProvider;
        this.DEFAULT_EXPIRE_TIME = Duration.ofMinutes(cacheProperties.getDownloadCacheExpireTime());
        this.CACHE_PREFIX = cacheProperties.getDownloadCachePrefix();
        this.CHUNK_SIZE = cacheProperties.getChunkSize();
    }


    /**
     * 查詢緩存值
     *
     * @param key   查詢key
     * @param clazz 值的類型
     * @param <T>   泛型類型
     *
     * @return Mono<T>
     */
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
     * 查詢緩存集合 (返回一個 List)
     *
     * @param key   緩存鍵集合
     * @param clazz 值的類型
     * @param <T>   泛型類型
     *
     * @return Mono<List < T>>
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
     * 批量查詢緩存列表 (返回一個 Map，內部為列表)
     *
     * @param keys  緩存鍵集合
     * @param clazz 值的類型
     * @param <T>   泛型類型
     *
     * @return Mono<Map < String, List < T>>>
     */
    @Override
    public <T> Mono<Map<String, List<T>>> getAllAsMapList(Collection<String> keys, Class<T> clazz) {
        HashMap<String, List<T>> map = new HashMap<>();
        return Flux.fromIterable(keys).flatMap(key -> getAsList(key, clazz).doOnNext(list -> map.put(key, list))).then(Mono.just(map));
    }


    /**
     * 設定單個緩存值
     *
     * @param key    查詢key
     * @param value  存儲value
     * @param expire 過期時間
     *
     * @return Mono<Void>
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

            List<Mono<Void>> saveOperations = new ArrayList<>();
            int totalChunks = (int) Math.ceil((double) base64.length() / CHUNK_SIZE);

            for (int i = 0; i < totalChunks; i++) {
                String chunkKey = key + "_" + (i + 1);
                int start = i * CHUNK_SIZE;
                int end = Math.min(start + CHUNK_SIZE, base64.length());

                String chunkData = base64.substring(start, end);

                saveOperations.add(redisProvider.setHashMap(CACHE_PREFIX, chunkKey, chunkData, chooseTime));
            }
            return Flux.merge(saveOperations);
        }).then();
    }


    /**
     * 設定緩存數據，此為批量設定
     *
     * @param keyValues key-keyValues 集合
     * @param expire    過期時間
     *
     * @return Mono<Void>
     */
    @Override
    public Mono<Void> setAll(Map<String, Object> keyValues, Duration expire) {
        return keyValues.entrySet().stream().map(entry -> set(entry.getKey(), entry.getValue(), expire)).reduce(Mono::then).orElse(Mono.empty());
    }

    /**
     * 獲取默認過期時間
     *
     * @return 默認過期時間
     */
    @Override
    @SkipRecord
    public Duration getDefaultExpire() {
        return DEFAULT_EXPIRE_TIME;
    }


    /**
     * 轉換流為base64格式的字符串
     *
     * @param dataBufferFlux 數據流
     *
     * @return Mono<String>
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
     * 將base64格式的字符串轉換為流
     *
     * @param base64 base64字符串
     *
     * @return Flux<DataBuffer>
     */
    private Flux<DataBuffer> formatBase64ToStream(String base64) {
        byte[] bytes = Base64.getDecoder().decode(base64);
        return Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(bytes));
    }

}
