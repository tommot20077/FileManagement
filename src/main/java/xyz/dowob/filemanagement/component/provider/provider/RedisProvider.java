package xyz.dowob.filemanagement.component.provider.provider;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.HideOverLength;
import xyz.dowob.filemanagement.annotation.SkipRecord;
import xyz.dowob.filemanagement.data.api.PagedResponseDTO;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 此類用於提供 Redis 的操作方法，透過自定義方法操作 RedisTemplate 來對數據進行操作
 * 通過 ReactiveRedisTemplate 來實現非阻塞的操作
 *
 * @author yuan
 * @program FileManagement
 * @ClassName RedisProvider
 * @description
 * @create 2024-09-27 13:53
 * @Version 1.0
 **/
@Component
@SuppressWarnings("unused")
@SkipRecord
public class RedisProvider {
    /**
     * RedisTemplate 用於操作 Redis 的模板，此模板為非阻塞的
     */
    private final ReactiveRedisTemplate<String, Object> redisTemplate;


    /**
     * ObjectMapper 用於對象的序列化和反序列化
     */
    private final ObjectMapper objectMapper;

    /**
     * 隨機緩存過期時間比例
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final Float RANDOM_CACHE_EXPIRE_TIME_RATIO = 0.2f;

    public RedisProvider(ReactiveRedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 將數據存入 Redis，並設置過期時間
     *
     * @param key        鍵
     * @param value      值
     * @param expireTime 過期時間
     *
     * @return 返回 Mono<Void> 對象
     */
    public Mono<Void> setValue(String key, Object value, Duration expireTime) {
        if (expireTime == null || expireTime.isNegative()) {
            return setValue(key, value);
        }
        return redisTemplate.opsForValue().set(key, value).then(setExpire(key, expireTime));
    }

    /**
     * 將數據存入 Redis
     *
     * @param key   鍵
     * @param value 值
     *
     * @return 返回 Mono<Void> 對象
     */
    public Mono<Void> setValue(String key, Object value) {
        return redisTemplate.opsForValue().set(key, value).then();
    }

    /**
     * 根據鍵獲取數據
     *
     * @param key 鍵
     */
    public Mono<Object> getValue(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 根據鍵獲取數據
     *
     * @param key 鍵
     */
    public <T> Mono<T> getValue(String key, Class<T> clazz) {
        return redisTemplate.opsForValue().get(key).cast(clazz);
    }

    /**
     * 根據鍵獲取數據
     *
     * @param key 鍵
     */
    @HideOverLength
    public <T> Mono<PagedResponseDTO<T>> getPagedResponseFromValue(String key, Class<T> clazz) {
        return redisTemplate.opsForValue().get(key).map(obj -> {
            JavaType type = objectMapper.getTypeFactory().constructParametricType(PagedResponseDTO.class, clazz);
            return objectMapper.convertValue(obj, type);
        });
    }

    /**
     * 獲取數據列表
     *
     * @param key 鍵
     *
     * @return 返回 Flux<Object> 對象
     */
    public <T> Flux<T> getValueList(String key, Class<T> clazz) {
        return redisTemplate.opsForValue().get(key).flatMapMany(object -> convertObjectList(object, clazz));
    }


    /**
     * 對數據進行自增操作
     *
     * @param key   鍵
     * @param delta 自增的數值
     *
     * @return 返回 Mono<Void> 對象
     */
    public Mono<Void> incrementDelta(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta).then();
    }

    /**
     * 刪除 Redis 中的數據
     *
     * @param key 鍵
     *
     * @return 返回 Mono<Void> 對象
     */
    public Mono<Void> delete(String key) {
        return redisTemplate.delete(key).then();
    }

    /**
     * 將數據存入 Redis 的 Hash 中，並設置過期時間
     *
     * @param hashKey    Hash 的鍵
     * @param innerKey   Hash 內部的鍵
     * @param value      值
     * @param expireTime 過期時間
     *
     * @return 返回 Mono<Void> 對象
     */
    public Mono<Void> setHashMap(String hashKey, String innerKey, Object value, Duration expireTime) {
        if (expireTime == null || expireTime.isNegative()) {
            return setHashMap(hashKey, innerKey, value);
        }
        return redisTemplate.opsForHash().put(hashKey, innerKey, value).then(setExpire(hashKey, expireTime));
    }

    /**
     * 將數據存入 Redis 的 Hash 中
     *
     * @param hashKey  Hash 的鍵
     * @param innerKey Hash 內部的鍵
     * @param value    值
     *
     * @return 返回 Mono<Void> 對象
     */
    public Mono<Void> setHashMap(String hashKey, String innerKey, Object value) {
        return redisTemplate.opsForHash().put(hashKey, innerKey, value).then();
    }


    /**
     * 將數據存入 Redis 的 Hash 中
     *
     * @param hashKey Hash 的鍵
     * @param value   Hash 內部的鍵和值
     *
     * @return 返回 Mono<Void> 對象
     */
    public Mono<Void> setHashMapAll(String hashKey, Map<String, Object> value, Duration expireTime) {
        if (expireTime == null || expireTime.isNegative()) {
            return setHashMapAll(hashKey, value);
        }
        return redisTemplate.opsForHash().putAll(hashKey, value).then(setExpire(hashKey, expireTime));
    }

    /**
     * 將數據存入 Redis 的 Hash 中，此為批量設定
     *
     * @param hashKey Hash 的鍵
     * @param value   Hash 內部的鍵和值
     *
     * @return 返回 Mono<Void> 對象
     */
    public Mono<Void> setHashMapAll(String hashKey, Map<String, Object> value) {
        return redisTemplate.opsForHash().putAll(hashKey, value).then();
    }

    /**
     * 根據 Hash 的鍵和內部的鍵獲取數據
     *
     * @param hashKey  Hash 的鍵
     * @param innerKey Hash 內部的鍵
     *
     * @return 返回 Mono<Object> 對象
     */
    public Mono<Object> getHashMap(String hashKey, String innerKey) {
        return redisTemplate.opsForHash().get(hashKey, innerKey);
    }

    /**
     * 根據 Hash 的鍵和內部的鍵獲取數據
     *
     * @param hashKey  Hash 的鍵
     * @param innerKey Hash 內部的鍵
     *
     * @return 返回 Mono<Object> 對象
     */
    public <T> Mono<T> getHashMap(String hashKey, String innerKey, Class<T> clazz) {
        return redisTemplate.opsForHash().get(hashKey, innerKey).cast(clazz);
    }


    /**
     * 根據 Hash 的鍵和內部的鍵獲取數據，此適用於列表形式
     *
     * @param hashKey   Hash 的鍵
     * @param innerKeys Hash 內部的鍵的集合
     *
     * @return 返回 Mono<Object> 對象
     */
    public <T> Flux<T> getHashMapList(String hashKey, List<String> innerKeys, Class<T> clazz) {
        List<Object> innerKeyList = innerKeys.stream().map(innerKey -> (Object) innerKey).toList();
        return redisTemplate.opsForHash().multiGet(hashKey, innerKeyList).flatMapMany(list -> {
            List<Object> filteredList = list.stream().filter(Objects::nonNull).toList();

            if (filteredList.isEmpty()) {
                return Flux.empty();
            }

            if (clazz.isInstance(filteredList.getFirst())) {
                return Flux.fromIterable(filteredList).cast(clazz);
            }
            return Flux.fromIterable(filteredList).map(object -> objectMapper.convertValue(object, clazz));
        });
    }

    /**
     * 獲取指定Key 中的所有數據
     *
     * @param hashKey    Hash 的鍵
     * @param KeyClass   Key 的類型
     * @param ValueClass Value 的類型
     *
     * @return 返回 Flux<Map.Entry<Object, Object>> 對象
     */
    public <K, V> Flux<Map.Entry<K, V>> getAllHashMap(String hashKey, Class<K> KeyClass, Class<V> ValueClass) {
        return redisTemplate.opsForHash().entries(hashKey).map(entry -> {
            Object key = entry.getKey();
            Object value = entry.getValue();
            if (KeyClass.isInstance(key) && ValueClass.isInstance(value)) {
                return Map.entry(KeyClass.cast(key), ValueClass.cast(value));
            }
            return Map.entry(objectMapper.convertValue(key, KeyClass), objectMapper.convertValue(value, ValueClass));
        });
    }

    /**
     * 對 Hash 中的數據進行自增操作
     *
     * @param hashKey  Hash 的鍵
     * @param innerKey Hash 內部的鍵
     * @param delta    自增的數值
     *
     * @return 返回 Mono<Void> 對象
     */
    public Mono<Void> incrementHashMap(String hashKey, String innerKey, long delta) {
        return redisTemplate.opsForHash().increment(hashKey, innerKey, delta).then();
    }

    /**
     * 對 Hash 中的數據進行自增操作，並設置過期時間
     *
     * @param hashKey    Hash 的鍵
     * @param innerKey   Hash 內部的鍵
     * @param delta      自增的數值
     * @param expireTime 過期時間
     *
     * @return 返回 Mono<Void> 對象
     */
    public Mono<Object> incrementHashMap(String hashKey, String innerKey, long delta, Duration expireTime) {
        return redisTemplate
                .opsForHash()
                .increment(hashKey, innerKey, delta).flatMap(incrementResult -> setExpire(hashKey, expireTime).thenReturn(incrementResult));
    }

    /**
     * 獲取 HashMap 中的查詢Key的所有數據
     *
     * @param hashKey Hash 的鍵
     *
     * @return 返回 Flux<Object> 對象
     */
    public Flux<Object> getHashMapAll(String hashKey) {
        return redisTemplate.opsForHash().values(hashKey);
    }

    /**
     * 獲取 HashMap 中的查詢Key的所有數據
     *
     * @param hashKey Hash 的鍵
     *
     * @return 返回 Flux<Object> 對象
     */
    public <T> Flux<T> getHashMapAll(String hashKey, Class<T> clazz) {
        return redisTemplate.opsForHash().values(hashKey).cast(clazz);
    }

    /**
     * 依照通配符獲取 HashMap 中的數據
     * 其中，pattern 為通配符，clazz 為數據的類型
     *
     * @param hashKey Hash 的鍵
     * @param pattern 通配符
     * @param clazz   類型
     * @param <T>     泛型
     *
     * @return 返回 Flux<Map.Entry<String, T>> 對象，其中 Key 為 Hash 的內部鍵，Value 為 Hash 的內部值
     */
    public <T> Flux<Map.Entry<String, T>> getHashMapByPattern(String hashKey, String pattern, Class<T> clazz) {
        ScanOptions patternOptions = ScanOptions.scanOptions().match(pattern).build();
        return redisTemplate.opsForHash().scan(hashKey, patternOptions).flatMap(entry -> {
            Object key = entry.getKey();
            Object value = entry.getValue();
            if (key == null || value == null) {
                return Flux.empty();
            }
            return Flux.just(Map.entry(key.toString(), objectMapper.convertValue(value, clazz)));
        });
    }

    /**
     * 刪除 Hash 中指定外部Key中內部Key的數據
     *
     * @param key      Hash 的鍵
     * @param innerKey Hash 內部的鍵
     *
     * @return 返回 Mono<Void> 對象
     */
    public Mono<Void> deleteHash(String key, String innerKey) {
        return redisTemplate.opsForHash().remove(key, innerKey).then();
    }

    /**
     * 刪除 Hash 中指定外部Key中內部Key的數據，此為批量刪除
     * 當 innerKey 為空時，則不進行操作
     *
     * @param key      Hash 的鍵
     * @param innerKey Hash 內部的鍵的列表
     *
     * @return 返回 Mono<Void> 對象
     */
    public Mono<Void> deleteHash(String key, List<String> innerKey) {
        if (innerKey.isEmpty()) {
            return Mono.empty();
        }
        return redisTemplate.opsForHash().remove(key, innerKey.toArray()).then();
    }

    /**
     * 刪除 Hash 指定Key中的所有數據
     *
     * @param key Hash 的鍵
     *
     * @return 返回 Mono<Void> 對象
     */
    public Mono<Void> deleteHash(String key) {
        return redisTemplate.opsForHash().delete(key).then();
    }

    /**
     * 將數據存入 Redis 的 Set 中
     *
     * @param key        鍵
     * @param value      值
     * @param expireTime 過期時間
     *
     * @return 返回 Mono<Void> 對象
     */
    public Mono<Void> setSet(String key, Object value, Duration expireTime) {
        if (expireTime == null || expireTime.isNegative()) {
            return setSet(key, value);
        }
        return redisTemplate.opsForSet().add(key, value).then(setExpire(key, expireTime));
    }

    /**
     * 將數據存入 Redis 的 Set 中
     *
     * @param key   鍵
     * @param value 值
     *
     * @return 返回 Mono<Void> 對象
     */
    public Mono<Void> setSet(String key, Object value) {
        return redisTemplate.opsForSet().add(key, value).then();
    }

    /**
     * 取得 Set 中的數據
     *
     * @param key 鍵
     *
     * @return 返回 Mono<Void> 對象
     */
    public Flux<Object> getSet(String key) {
        return redisTemplate.opsForSet().members(key);
    }

    /**
     * 取得 Set 中的數據
     *
     * @param key 鍵
     *
     * @return 返回 Mono<Void> 對象
     */
    public <T> Flux<T> getSet(String key, Class<T> clazz) {
        return redisTemplate.opsForSet().members(key).flatMap(object -> convertObjectList(object, clazz));
    }

    /**
     * 根據需要刪除的數值，刪除 Set 中的數據
     *
     * @param key   鍵
     * @param value 值
     *
     * @return 返回 Mono<Void> 對象
     */
    public Mono<Void> deleteSet(String key, Object value) {
        return redisTemplate.opsForSet().remove(key, value).then();
    }

    /**
     * 刪除 Set 中的數據
     *
     * @param key 鍵
     *
     * @return 返回 Mono<Void> 對象
     */
    public Mono<Void> deleteSet(String key) {
        return redisTemplate.opsForSet().delete(key).then();
    }

    /**
     * 將數據存入 Redis 的 List 中，並設置過期時間
     *
     * @param key        鍵
     * @param value      值
     * @param expireTime 過期時間
     *
     * @return 返回 Mono<Void> 對象
     */
    public Mono<Void> setList(String key, Object value, Duration expireTime) {
        if (expireTime == null || expireTime.isNegative()) {
            return setList(key, value);
        }
        return redisTemplate.opsForList().rightPush(key, value).then(setExpire(key, expireTime));
    }

    /**
     * 插入額外的數據到 Set 中
     *
     * @param key   鍵
     * @param value 值
     *
     * @return 返回 Mono<Void> 對象
     */
    public Mono<Void> setList(String key, Object value) {
        return redisTemplate.opsForList().rightPush(key, value).then();
    }

    /**
     * 獲取 List 中的數據
     *
     * @param key 鍵
     *
     * @return 返回 Flux<Object> 對象
     */
    public Flux<Object> getList(String key) {
        return getList(key, 0, -1);
    }

    /**
     * 獲取 List 中的數據
     *
     * @param key   鍵
     * @param start 起始序號
     * @param end   結束序號
     *
     * @return 返回 Flux<Object> 對象
     */
    public Flux<Object> getList(String key, long start, long end) {
        return redisTemplate.opsForList().range(key, start, end);
    }

    /**
     * 獲取 List 中的數據
     *
     * @param key 鍵
     *
     * @return 返回 Flux<Object> 對象
     */
    public <T> Flux<T> getList(String key, Class<T> clazz) {
        return getList(key).cast(clazz);
    }

    /**
     * 獲取 List 中的數據
     *
     * @param key 鍵
     *
     * @return 返回 Mono<Void> 對象
     */
    public <T> Flux<T> getListContent(String key, long start, long end, Class<T> clazz) {
        return redisTemplate.opsForList().range(key, start, end).flatMap(object -> convertObjectList(object, clazz)).switchIfEmpty(Flux.empty());
    }

    /**
     * 將數據存入 Redis 的 List 中，並設置過期時間
     *
     * @param key        鍵
     * @param value      值
     * @param isLeft     是否從左邊插入
     * @param expireTime 過期時間
     *
     * @return 返回 Mono<Void> 對象
     */
    public Mono<Void> insertList(String key, Object value, Boolean isLeft, Duration expireTime) {
        if (expireTime == null || expireTime.isNegative()) {
            return insertList(key, value, isLeft);
        }
        return insertList(key, value, isLeft).then(setExpire(key, expireTime));
    }

    /**
     * 將數據存入 Redis 的 List 中
     *
     * @param key    鍵
     * @param value  值
     * @param isLeft 是否從左邊插入
     *
     * @return 返回 Mono<Void> 對象
     */
    public Mono<Void> insertList(String key, Object value, Boolean isLeft) {
        if (isLeft) {
            return redisTemplate.opsForList().leftPush(key, value).then();
        } else {
            return redisTemplate.opsForList().rightPush(key, value).then();
        }
    }

    /**
     * 根據目標數值，刪除 List 中的數據
     *
     * @param key   鍵
     * @param value 值
     *
     * @return 返回 Mono<Void> 對象
     */
    public Mono<Void> deleteList(String key, Object value) {
        return redisTemplate.opsForList().remove(key, 1, value).then();
    }


    /**
     * 刪除 List 中的數據
     *
     * @param key 鍵
     *
     * @return 返回 Mono<Void> 對象
     */
    public Mono<Void> deleteList(String key) {
        return redisTemplate.opsForList().delete(key).then();
    }


    /**
     * 新增數據到 Redis 的 Zset 中，並設置過期時間
     *
     * @param key        鍵
     * @param value      值
     * @param score      序號
     * @param expireTime 過期時間
     *
     * @return 返回 Mono<Void> 對象
     */
    public Mono<Void> setZset(String key, Object value, double score, Duration expireTime) {
        if (expireTime == null || expireTime.isNegative()) {
            return setZset(key, value, score);
        }
        return setZset(key, value, score).then(setExpire(key, expireTime));
    }

    /**
     * 新增數據到 Redis 的 Zset 中
     *
     * @param key   鍵
     * @param value 值
     * @param score 序號
     *
     * @return 返回 Mono<Void> 對象
     */
    public Mono<Void> setZset(String key, Object value, double score) {
        return redisTemplate.opsForZSet().add(key, value, score).then();
    }

    /**
     * 獲取 Zset 中的數據
     *
     * @param key 鍵
     *
     * @return 返回 Flux<Object> 對象
     */
    public Flux<Object> getZset(String key) {
        return redisTemplate.opsForZSet().range(key, Range.from(Range.Bound.inclusive(0L)).to(Range.Bound.unbounded()));
    }

    /**
     * 獲取 Zset 中的數據
     *
     * @param key   鍵
     * @param start 起始序號
     * @param end   結束序號
     *
     * @return 返回 Flux<Object> 對象
     */
    public Flux<Object> getZset(String key, long start, long end) {
        return redisTemplate.opsForZSet().range(key, Range.from(Range.Bound.inclusive(start)).to(Range.Bound.inclusive(end)));
    }

    /**
     * 獲取 Zset 中的數據
     *
     * @param key 鍵
     *
     * @return 返回 Mono<Object> 對象
     */
    @HideOverLength
    public <T> Mono<PagedResponseDTO<T>> getPagedResponseFromZset(String key, int page, Class<T> clazz) {
        return redisTemplate.opsForZSet().rangeByScore(key, Range.just((double) page)).next().flatMap(obj -> {
            if (obj == null) {
                return Mono.empty();
            }

            JavaType type = objectMapper.getTypeFactory().constructParametricType(PagedResponseDTO.class, clazz);
            PagedResponseDTO<T> pagedResponseDTO = objectMapper.convertValue(obj, type);
            return Mono.just(pagedResponseDTO);
        });
    }

    /**
     * 獲取 Zset 中的數據
     *
     * @param key   鍵
     * @param page  頁數
     * @param clazz 類型
     *
     * @return 返回 Flux<T> 對象
     */
    @HideOverLength
    public <T> Flux<T> getListFromZset(String key, int page, Class<T> clazz) {
        return redisTemplate.opsForZSet().rangeByScore(key, Range.just((double) page)).next().flatMapMany(object -> convertObjectList(object, clazz));
    }


    /**
     * 根據目標數值，刪除 Zset 中的數據
     *
     * @param key   鍵
     * @param value 值
     *
     * @return 返回 Mono<Void> 對象
     */
    public Mono<Void> deleteZset(String key, Object... value) {
        return redisTemplate.opsForZSet().remove(key, value).then();
    }

    /**
     * 刪除 Zset 中的數據
     *
     * @param key 鍵
     *
     * @return 返回 Mono<Void> 對象
     */
    public Mono<Void> deleteZset(String key) {
        return redisTemplate.opsForZSet().removeRange(key, Range.unbounded()).then();
    }

    /**
     * 根據目標數值，刪除 Zset 中的數據
     *
     * @param key   鍵
     * @param start 起始序號
     * @param end   結束序號
     *
     * @return 返回 Mono<Void> 對象
     */
    public Mono<Void> deleteZset(String key, long start, long end) {
        return redisTemplate.opsForZSet().removeRange(key, Range.from(Range.Bound.inclusive(start)).to(Range.Bound.inclusive(end))).then();
    }

    /**
     * 生成一個分塊集合，用於標記分塊的完成情況
     *
     * @param key         鍵
     * @param totalChunks 分塊數量
     *
     * @return 返回 Mono<Void> 對象
     */
    public Mono<Void> generateChunkSet(String key, int totalChunks) {
        return Flux.range(1, totalChunks).flatMap(index -> setSet(key, index)).then();
    }

    /**
     * 依照通配符刪除 Redis 中的數據
     *
     * @param pattern 通配符
     *
     * @return 返回 Mono<Void> 對象
     */
    public Mono<Void> deleteByPattern(String pattern) {
        return redisTemplate.keys(pattern).collectList().flatMap(keys -> {
            keys.forEach(redisTemplate::delete);
            return Mono.empty();
        });
    }

    /**
     * 確認分塊是否尚未完成
     *
     * @param key        鍵
     * @param chunkIndex 分塊序號
     *
     * @return 返回 Mono<Boolean> 對象
     */
    public Mono<Boolean> isChunkSetPending(String key, int chunkIndex) {
        return redisTemplate.opsForSet().isMember(key, chunkIndex);
    }

    /**
     * 設定過期時間，避免緩存雪崩的情況，會在原有的過期時間上增加隨機的過期時間
     *
     * @param key        鍵
     * @param expireTime 過期時間
     *
     * @return 返回 Mono<Void> 對象
     */
    private Mono<Void> setExpire(String key, Duration expireTime) {
        Duration randomExpireTime = Duration.ofSeconds(Math.round(expireTime.getSeconds() * RANDOM_CACHE_EXPIRE_TIME_RATIO));
        return redisTemplate.expire(key, randomExpireTime).then();
    }


    /**
     * 轉換數據為指定類型的列表
     *
     * @param objects 數據
     * @param clazz   類型
     * @param <T>     泛型
     *
     * @return 返回 Flux<T> 對象
     */
    private <T> Flux<T> convertObjectList(Object objects, Class<T> clazz) {
        if (!(objects instanceof List<?> list)) {
            return Flux.empty();
        }
        List<T> finalList = list.stream().map(object -> {
            if (clazz.isInstance(object)) {
                return clazz.cast(object);
            }
            return objectMapper.convertValue(object, clazz);
        }).toList();
        return Flux.fromIterable(finalList);
    }
}
