package xyz.dowob.filemanagement.component.provider.providerImplement.cacheprovider;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.CacheProviderType;
import xyz.dowob.filemanagement.annotation.HideOverLength;
import xyz.dowob.filemanagement.component.provider.provider.AbstractRedisCacheProvider;
import xyz.dowob.filemanagement.component.provider.provider.RedisProvider;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.customenum.CacheProviderEnum;
import xyz.dowob.filemanagement.data.file.po.DataBufferPO;

import java.time.Duration;
import java.util.Base64;
import java.util.Collection;
import java.util.Map;

/**
 * 文件流緩存提供者實現類，用於提供文件流緩存的操作
 * 對於每一次請求都需要從GridFS中獲取文件流，這樣會對服務器造成壓力，因此提供對於檔案的數據流進行緩存
 * 透過繼承AbstractRedisCacheProvider，實現了CacheProvider接口，提供了緩存操作的具體實現
 * 此類透過檔案設定enable-file-stream-cache來判斷是否啟用文件流緩存，當開啟時此類才會生效，默認開啟 {@link FileProperties}
 * 文件流緩存的key前綴以及緩存的默認過期時間來自於檔案設定
 *
 * @author yuan
 * @program FileManagement
 * @ClassName StreamCacheProviderImpl
 * @create 2025/3/15
 * @Version 1.0
 **/
@Component
@CacheProviderType(CacheProviderEnum.FILE_STREAM_CACHE)
@ConditionalOnProperty(prefix = "file", name = "global.enable-file-stream-cache", havingValue = "true", matchIfMissing = true)
public class StreamCacheProviderImpl extends AbstractRedisCacheProvider {
    /**
     * 文件流緩存提供者實現類的構造方法
     *
     * @param redisProvider Redis操作提供者
     */
    public StreamCacheProviderImpl(RedisProvider redisProvider, FileProperties fileProperties) {
        super(redisProvider);
        super.setCACHE_PREFIX(fileProperties.getDownload().getDownloadCachePrefix());
        super.setDEFAULT_EXPIRE_TIME(fileProperties.getDownload().getDownloadCacheExpireTime());
    }

    /**
     * 根據key獲取緩存數據
     *
     * @param hashKey key
     * @param clazz   類型
     *
     * @return Mono<T>
     */
    @HideOverLength
    @Override
    public <T> Mono<T> get(String hashKey, Class<T> clazz) {
        return super.getRedisProvider().getHashMap(super.getCACHE_PREFIX(), hashKey, String.class).mapNotNull(base64 -> {
            if (clazz.isAssignableFrom(DataBufferPO.class)) {
                DataBufferPO dataBufferPO = new DataBufferPO();
                dataBufferPO.setDataBufferFlux(formatBase64ToStream(base64));
                return dataBufferPO;
            }
            return null;
        }).cast(clazz);
    }


    /**
     * 根據key獲取緩存數據，此為批量查詢
     *
     * @param hashKeys key集合
     * @param clazz    類型
     *
     * @return Flux<T>
     */
    @HideOverLength
    @Override
    public <T> Flux<T> getAll(Collection<String> hashKeys, Class<T> clazz) {
        return Flux.error(new UnsupportedOperationException("檔案流緩存不支持批量查詢"));
    }


    /**
     * 設定緩存數據
     *
     * @param hashKey 查詢key
     * @param value   存儲value
     * @param expire  過期時間
     *
     * @return Mono<Void>
     */
    @Override
    public Mono<Void> set(String hashKey, Object value, Duration... expire) {
        boolean hasExpire = expire != null && expire.length > 0;
        if (value instanceof DataBufferPO dataBufferPO) {
            return formatStreamToBase64(dataBufferPO.getDataBufferFlux()).flatMap(base64 -> {
                if (hasExpire) {
                    return super.getRedisProvider().setHashMap(super.getCACHE_PREFIX(), hashKey, base64, expire[0]);
                }
                return super
                        .getRedisProvider()
                        .setHashMap(super.getCACHE_PREFIX(), hashKey, base64, Duration.ofMinutes(super.getDEFAULT_EXPIRE_TIME()));
            }).then();
        }
        return Mono.empty();
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
    public Mono<Void> setAll(Map<String, Object> keyValues, Duration... expire) {
        return Mono.error(new UnsupportedOperationException("檔案流緩存不支持批量設定"));
    }


    /**
     * 轉換流為base64格式的字符串
     *
     * @param dataBufferFlux 數據流
     *
     * @return Mono<String>
     */
    @HideOverLength
    private Mono<String> formatStreamToBase64(Flux<DataBuffer> dataBufferFlux) {
        return dataBufferFlux.map(dataBuffer -> {
            byte[] bytes = new byte[dataBuffer.readableByteCount()];
            dataBuffer.read(bytes);
            DataBufferUtils.release(dataBuffer);
            return bytes;
        }).collectList().map(byteList -> {
            byte[] allBytes = byteList.stream().reduce(new byte[0], (a, b) -> {
                byte[] result = new byte[a.length + b.length];
                System.arraycopy(a, 0, result, 0, a.length);
                System.arraycopy(b, 0, result, a.length, b.length);
                return result;
            });
            return Base64.getEncoder().encodeToString(allBytes);
        });
    }

    /**
     * 將base64格式的字符串轉換為流
     *
     * @param base64 base64字符串
     *
     * @return Flux<DataBuffer>
     */
    @HideOverLength
    private Flux<DataBuffer> formatBase64ToStream(String base64) {
        byte[] bytes = Base64.getDecoder().decode(base64);
        return Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(bytes));
    }

}
