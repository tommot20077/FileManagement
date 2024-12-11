package xyz.dowob.filemanagement.component.provider.providerImpl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

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
@RequiredArgsConstructor
public class RedisProvider {
    /**
     * RedisTemplate 用於操作 Redis 的模板，此模板為非阻塞的
     */
    private final ReactiveRedisTemplate<String, Object> ObjectRedisTemplate;

    /**
     * 將數據存入 Redis
     *
     * @param key   鍵
     * @param value 值
     *
     * @return 返回 Mono<Void> 對象
     */
    public Mono<Void> setValue(String key, Object value) {
        return ObjectRedisTemplate.opsForValue().set(key, value).then();
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
        return ObjectRedisTemplate
                .opsForValue()
                .set(key, value)
                .then(ObjectRedisTemplate.expire(key, Duration.of(expireTime, unit)))
                .then();

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
        return ObjectRedisTemplate.opsForValue().increment(key, delta).then();
    }

    /**
     * 根據鍵獲取數據
     *
     * @param key 鍵
     */
    public Mono<Object> getValue(String key) {
        return ObjectRedisTemplate.opsForValue().get(key);
    }

    /**
     * 刪除 Redis 中的數據
     *
     * @param key 鍵
     *
     * @return 返回 Mono<Void> 對象
     */
    public Mono<Void> delete(String key) {
        return ObjectRedisTemplate.delete(key).then();
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
        return ObjectRedisTemplate.opsForHash().put(hashKey, innerKey, value).then();
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
        return ObjectRedisTemplate
                .opsForHash()
                .put(hashKey, innerKey, value)
                .then(ObjectRedisTemplate.expire(hashKey, Duration.of(expireTime, unit)))
                .then();
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
        return ObjectRedisTemplate.opsForHash().increment(hashKey, innerKey, delta).then();
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
        return ObjectRedisTemplate
                .opsForHash()
                .increment(hashKey, innerKey, delta)
                .flatMap(incrementResult -> ObjectRedisTemplate.expire(hashKey, Duration.of(expireTime, unit)).thenReturn(incrementResult));
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
        return ObjectRedisTemplate.opsForHash().get(hashKey, innerKey);
    }

    /**
     * 獲取 HashMap 中的查詢Key的所有數據
     *
     * @param hashKey Hash 的鍵
     *
     * @return 返回 Flux<Object> 對象
     */
    public Flux<Object> getHashMapAll(String hashKey) {
        return ObjectRedisTemplate.opsForHash().values(hashKey);
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
        return ObjectRedisTemplate.opsForHash().remove(key, innerKey).then();
    }

    /**
     * 刪除 Hash 中的數據
     *
     * @param key Hash 的鍵
     *
     * @return 返回 Mono<Void> 對象
     */
    public Mono<Void> deleteHash(String key) {
        return ObjectRedisTemplate.opsForHash().delete(key).then();
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
        return ObjectRedisTemplate.opsForSet().add(key, value).then();
    }

    /**
     * 取得 Set 中的數據
     *
     * @param key 鍵
     *
     * @return 返回 Mono<Void> 對象
     */
    public Flux<Object> getSet(String key) {
        return ObjectRedisTemplate.opsForSet().members(key);
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
        return ObjectRedisTemplate.opsForList().rightPush(key, value).then();
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
        return ObjectRedisTemplate.opsForSet().remove(key, value).then();
    }

    /**
     * 刪除 Set 中的數據
     *
     * @param key 鍵
     *
     * @return 返回 Mono<Void> 對象
     */
    public Mono<Void> deleteSet(String key) {
        return ObjectRedisTemplate.opsForSet().delete(key).then();
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
            return ObjectRedisTemplate.opsForList().leftPush(key, value).then();
        } else {
            return ObjectRedisTemplate.opsForList().rightPush(key, value).then();
        }
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
        return insertList(key, value, isLeft).then(ObjectRedisTemplate.expire(key, Duration.of(expireTime, unit))).then();
    }

    /**
     * 獲取 List 中的數據
     *
     * @param key 鍵
     *
     * @return 返回 Flux<Object> 對象
     */
    public Flux<Object> getList(String key) {
        return ObjectRedisTemplate.opsForList().range(key, 0, -1);
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
        return ObjectRedisTemplate.opsForList().remove(key, 1, value).then();
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
        return ObjectRedisTemplate.opsForZSet().add(key, value, score).then();
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
        return setZset(key, value, score).then(ObjectRedisTemplate.expire(key, Duration.of(expireTime, unit))).then();
    }

    /**
     * 獲取 Zset 中的數據
     *
     * @param key 鍵
     *
     * @return 返回 Flux<Object> 對象
     */
    public Flux<Object> getZset(String key) {
        return ObjectRedisTemplate.opsForZSet().range(key, Range.from(Range.Bound.inclusive(0L)).to(Range.Bound.unbounded()));
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
        return ObjectRedisTemplate.opsForZSet().range(key, Range.from(Range.Bound.inclusive(start)).to(Range.Bound.inclusive(end)));
    }

    /**
     * 根據目標數值，刪除 Zset 中的數據
     *
     * @param key   鍵
     * @param value 值
     *
     * @return 返回 Mono<Void> 對象
     */
    public Mono<Void> deleteZset(String key, Object value) {
        return ObjectRedisTemplate.opsForZSet().remove(key, value).then();
    }

    /**
     * 依照通配符刪除 Redis 中的數據
     *
     * @param pattern 通配符
     *
     * @return 返回 Mono<Void> 對象
     */
    @Deprecated
    public Mono<Void> deleteByPattern(String pattern) {
        return Mono.empty();
    }

    /**
     * 格式化數據，將單一數據轉換為指定類型
     *
     * @param clazz      類型
     * @param objectMono 數據流
     *
     * @return 返回 Mono<?> 對象
     */
    public Mono<?> formatObject(Class<?> clazz, Mono<Object> objectMono) {
        return objectMono.cast(clazz);
    }

    /**
     * 格式化數據，將數據流中的數據轉換為指定類型
     *
     * @param clazz       類型
     * @param objectsFlux 數據流
     *
     * @return 返回 Flux<?> 對象
     */
    public Flux<?> formatObject(Class<?> clazz, Flux<Object> objectsFlux) {
        return objectsFlux.cast(clazz);
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
     * 確認分塊是否尚未完成
     *
     * @param key        鍵
     * @param chunkIndex 分塊序號
     *
     * @return 返回 Mono<Boolean> 對象
     */
    public Mono<Boolean> isChunkSetPending(String key, int chunkIndex) {
        return ObjectRedisTemplate.opsForSet().isMember(key, chunkIndex);
    }
}
