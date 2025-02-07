package xyz.dowob.filemanagement.component.provider.provider;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.HideOverLength;
import xyz.dowob.filemanagement.data.api.PagedResponseDTO;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

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
public class RedisProvider {
    /**
     * RedisTemplate 用於操作 Redis 的模板，此模板為非阻塞的
     */
    private final ReactiveRedisTemplate<String, Object> redisTemplate;

    private final ObjectMapper objectMapper;

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
     * @param unit       過期時間的單位
     *
     * @return 返回 Mono<Void> 對象
     */
    public Mono<Void> setValue(String key, Object value, long expireTime, ChronoUnit unit) {
        if (expireTime <= 0) {
            return setValue(key, value);
        }
        return redisTemplate.opsForValue().set(key, value).then(redisTemplate.expire(key, Duration.of(expireTime, unit))).then();

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

    public <T> Mono<T> getValue(String key, Class<T> clazz) {
        return redisTemplate.opsForValue().get(key).cast(clazz);
    }

    @HideOverLength
    public <T> Mono<PagedResponseDTO<T>> getPagedResponseFromValue(String key, Class<T> clazz) {
        return redisTemplate.opsForValue().get(key).map(obj -> {
            JavaType type = objectMapper.getTypeFactory().constructParametricType(PagedResponseDTO.class, clazz);
            return objectMapper.convertValue(obj, type);
        });
    }

    public <T> Flux<T> getValueList(String key, Class<T> clazz) {
        return redisTemplate.opsForValue().get(key).flatMapMany(object -> convertObjectList(object, clazz));
    }

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
     * @param unit       過期時間的單位
     *
     * @return 返回 Mono<Void> 對象
     */
    public Mono<Void> setHashMap(String hashKey, String innerKey, Object value, long expireTime, ChronoUnit unit) {
        if (expireTime <= 0) {
            return setHashMap(hashKey, innerKey, value);
        }
        return redisTemplate
                .opsForHash().put(hashKey, innerKey, value).then(redisTemplate.expire(hashKey, Duration.of(expireTime, unit)))
                .then();
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
     * @param unit       過期時間的單位
     *
     * @return 返回 Mono<Void> 對象
     */
    public Mono<Object> incrementHashMap(String hashKey, String innerKey, long delta, long expireTime, ChronoUnit unit) {
        return redisTemplate
                .opsForHash()
                .increment(hashKey, innerKey, delta)
                .flatMap(incrementResult -> redisTemplate.expire(hashKey, Duration.of(expireTime, unit)).thenReturn(incrementResult));
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
     * 刪除 Hash 中的數據
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
     * 刪除 Hash 中的數據
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
     * @param key   鍵
     * @param value 值
     *
     * @return 返回 Mono<Void> 對象
     */
    public Mono<Void> setSet(String key, Object value, long expireTime, ChronoUnit unit) {
        if (expireTime <= 0) {
            return setSet(key, value);
        }
        return redisTemplate.opsForSet().add(key, value).then(redisTemplate.expire(key, Duration.of(expireTime, unit))).then();
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

    public Mono<Void> setList(String key, Object value, long expireTime, ChronoUnit unit) {
        if (expireTime <= 0) {
            return setList(key, value);
        }
        return redisTemplate.opsForList().rightPush(key, value).then(redisTemplate.expire(key, Duration.of(expireTime, unit))).then();
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
        return redisTemplate.opsForList().range(key, 0, -1);
    }

    public Flux<Object> getList(String key, long start, long end) {
        return redisTemplate.opsForList().range(key, start, end);
    }

    public <T> Flux<T> getList(String key, Class<T> clazz) {
        return redisTemplate.opsForList().range(key, 0, -1).flatMap(object -> convertObjectList(object, clazz));
    }

    /**
     * 將數據存入 Redis 的 List 中，並設置過期時間
     *
     * @param key        鍵
     * @param value      值
     * @param isLeft     是否從左邊插入
     * @param expireTime 過期時間
     * @param unit       過期時間的單位
     *
     * @return 返回 Mono<Void> 對象
     */
    public Mono<Void> insertList(String key, Object value, Boolean isLeft, long expireTime, ChronoUnit unit) {
        if (expireTime <= 0) {
            return insertList(key, value, isLeft);
        }
        return insertList(key, value, isLeft).then(redisTemplate.expire(key, Duration.of(expireTime, unit))).then();
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
     * 新增數據到 Redis 的 Zset 中，並設置過期時間
     *
     * @param key        鍵
     * @param value      值
     * @param score      序號
     * @param expireTime 過期時間
     * @param unit       過期時間的單位
     *
     * @return 返回 Mono<Void> 對象
     */
    public Mono<Void> setZset(String key, Object value, double score, long expireTime, ChronoUnit unit) {
        if (expireTime <= 0) {
            return setZset(key, value, score);
        }
        return setZset(key, value, score).then(redisTemplate.expire(key, Duration.of(expireTime, unit))).then();
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

    public Mono<Void> deleteZset(String key) {
        return redisTemplate.opsForZSet().removeRange(key, Range.unbounded()).then();
    }

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
}
