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
    private final ReactiveRedisTemplate<String, Object> ObjectRedisTemplate;

    public Mono<Void> setValue(String key, Object value) {
        return ObjectRedisTemplate.opsForValue().set(key, value).then();
    }

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

    public Mono<Void> incrementDelta(String key, long delta) {
        return ObjectRedisTemplate.opsForValue().increment(key, delta).then();
    }

    public Mono<Object> getValue(String key) {
        return ObjectRedisTemplate.opsForValue().get(key);
    }

    public Mono<Void> delete(String key) {
        return ObjectRedisTemplate.delete(key).then();
    }


    public Mono<Void> setHashMap(String hashKey, String innerKey, Object value) {
        return ObjectRedisTemplate.opsForHash().put(hashKey, innerKey, value).then();
    }

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

    public Mono<Void> incrementHashMap(String hashKey, String innerKey, long delta) {
        return ObjectRedisTemplate.opsForHash().increment(hashKey, innerKey, delta).then();
    }

    public Mono<Object> incrementHashMap(String hashKey, String innerKey, long delta, long expireTime, ChronoUnit unit) {
        return ObjectRedisTemplate
                .opsForHash()
                .increment(hashKey, innerKey, delta)
                .flatMap(incrementResult -> ObjectRedisTemplate.expire(hashKey, Duration.of(expireTime, unit)).thenReturn(incrementResult));
    }
    public Mono<Object> getHashMap(String hashKey, String innerKey) {
        return ObjectRedisTemplate.opsForHash().get(hashKey, innerKey);
    }

    public Flux<Object> getHashMapAll(String hashKey) {
        return ObjectRedisTemplate.opsForHash().values(hashKey);
    }

    public Mono<Void> deleteHash(String key, String innerKey) {
        return ObjectRedisTemplate.opsForHash().remove(key, innerKey).then();
    }

    public Mono<Void> deleteHash(String key) {
        return ObjectRedisTemplate.opsForHash().delete(key).then();
    }

    public Mono<Void> setSet(String key, Object value) {
        return ObjectRedisTemplate.opsForSet().add(key, value).then();
    }

    public Flux<Object> getSet(String key) {
        return ObjectRedisTemplate.opsForSet().members(key);
    }

    public Mono<Void> setList(String key, Object value) {
        return ObjectRedisTemplate.opsForList().rightPush(key, value).then();
    }

    public Mono<Void> deleteSet(String key, Object value) {
        return ObjectRedisTemplate.opsForSet().remove(key, value).then();
    }

    public Mono<Void> deleteSet(String key) {
        return ObjectRedisTemplate.opsForSet().delete(key).then();
    }

    public Mono<Void> insertList(String key, Object value, Boolean isLeft) {
        if (isLeft) {
            return ObjectRedisTemplate.opsForList().leftPush(key, value).then();
        } else {
            return ObjectRedisTemplate.opsForList().rightPush(key, value).then();
        }
    }

    public Mono<Void> insertList(String key, Object value, Boolean isLeft, long expireTime, ChronoUnit unit) {
        if (expireTime <= 0) {
            return insertList(key, value, isLeft);
        }
        return insertList(key, value, isLeft).then(ObjectRedisTemplate.expire(key, Duration.of(expireTime, unit))).then();
    }

    public Flux<Object> getList(String key) {
        return ObjectRedisTemplate.opsForList().range(key, 0, -1);
    }

    public Mono<Void> deleteList(String key, Object value) {
        return ObjectRedisTemplate.opsForList().remove(key, 1, value).then();
    }

    public Mono<Void> setZset(String key, Object value, double score) {
        return ObjectRedisTemplate.opsForZSet().add(key, value, score).then();
    }

    public Mono<Void> setZset(String key, Object value, double score, long expireTime, ChronoUnit unit) {
        if (expireTime <= 0) {
            return setZset(key, value, score);
        }
        return setZset(key, value, score).then(ObjectRedisTemplate.expire(key, Duration.of(expireTime, unit))).then();
    }

    public Flux<Object> getZset(String key) {
        return ObjectRedisTemplate.opsForZSet().range(key, Range.from(Range.Bound.inclusive(0L)).to(Range.Bound.unbounded()));
    }

    public Flux<Object> getZset(String key, long start, long end) {
        return ObjectRedisTemplate.opsForZSet().range(key, Range.from(Range.Bound.inclusive(start)).to(Range.Bound.inclusive(end)));
    }

    public Mono<Void> deleteZset(String key, Object value) {
        return ObjectRedisTemplate.opsForZSet().remove(key, value).then();
    }


    public Mono<Void> deleteByPattern(String pattern) {
        return Mono.empty();
    }

    public Mono<?> formatObject(Class<?> clazz, Mono<Object> objectMono) {
        return objectMono.cast(clazz);
    }

    public Flux<?> formatObject(Class<?> clazz, Flux<Object> objectsFlux) {
        return objectsFlux.cast(clazz);
    }


    public Mono<Void> generateChunkSet(String key, int totalChunks) {
        return Flux.range(1, totalChunks).flatMap(index -> setSet(key, index)).then();
    }
}
