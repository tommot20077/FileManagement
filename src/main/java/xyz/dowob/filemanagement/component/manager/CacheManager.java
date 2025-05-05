package xyz.dowob.filemanagement.component.manager;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import xyz.dowob.filemanagement.annotation.CacheProviderType;
import xyz.dowob.filemanagement.annotation.SkipRecord;
import xyz.dowob.filemanagement.component.provider.provider.RedisProvider;
import xyz.dowob.filemanagement.component.provider.providerInterface.CacheProvider;
import xyz.dowob.filemanagement.customenum.CacheProviderEnum;
import xyz.dowob.filemanagement.data.datainterface.FluxContainer;
import xyz.dowob.filemanagement.functionInterface.CacheRule;
import xyz.dowob.filemanagement.unity.LogUnity;

import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 緩存管理器，用於管理緩存提供者 {@link CacheProvider}
 * 這類是對於緩存提供者的管理，並封裝了對於緩存操作的方法，
 * 整個緩存管理器主要處理2件事情：
 * 1. CacheProvider的策略選取，會依照CacheProviderEnum選取對應的CacheProvider
 * 2. 對於緩存操作的封裝，提供了緩存的增刪改查操作
 * 對於緩存操作的具體實現在CacheProvider中，CacheManager只是對其進行了封裝
 * 但如果有特殊需求也可以使用策略模式獲取到CacheProvider進行操作
 *
 * @author yuan
 * @program FileManagement
 * @ClassName CacheManager
 * @create 2025/3/15
 * @Version 1.1
 **/
@Component
@SuppressWarnings("unused")
public class CacheManager {
    /**
     * 緩存提供者的Map，用於存儲不同類型的緩存提供者
     */
    private final EnumMap<CacheProviderEnum, CacheProvider> cacheProviderMap;

    /**
     * Redis 操作提供者
     */
    private final RedisProvider redisProvider;

    /**
     * Redis鎖的前綴
     */
    private static final String LOCK_PREFIX = "cache_lock:";

    /**
     * Redis鎖的過期時間，設定為1分鐘
     */
    private static final Duration DEFAULT_EXPIRE_TIME = Duration.ofMinutes(1);

    /**
     * Redis鎖的值
     */
    private static final String LOCK_VALUE = "locked";

    /**
     * 緩存管理器的構造方法，用於初始化緩存提供者列表
     * 會將緩存提供者列表轉換為EnumMap，方便根據CacheProviderEnum獲取對應的CacheProvider
     *
     * @param cacheProviderList 緩存提供者列表
     * @param redisProvider     Redis 提供者
     */
    public CacheManager(List<CacheProvider> cacheProviderList, RedisProvider redisProvider) {
        this.redisProvider = redisProvider;
        cacheProviderMap = new EnumMap<>(CacheProviderEnum.class);
        for (CacheProvider provider : cacheProviderList) {
            CacheProviderType cacheProviderType = AnnotatedElementUtils.findMergedAnnotation(provider.getClass(), CacheProviderType.class);
            if (cacheProviderType != null) {
                cacheProviderMap.put(cacheProviderType.value(), provider);
            }
        }
    }


    /**
     * 設置緩存提供者，當需要自定義緩存提供者時可以使用此方法
     *
     * @param cacheProviderEnum 緩存提供者的類型
     * @param cacheProvider     緩存提供者
     */
    public void setCacheProvider(CacheProviderEnum cacheProviderEnum, CacheProvider cacheProvider) {
        cacheProviderMap.put(cacheProviderEnum, cacheProvider);
    }


    /**
     * 獲取緩存提供者，根據CacheProviderEnum獲取對應的CacheProvider
     *
     * @param cacheProviderEnum 緩存提供者的類型
     *
     * @return CacheProvider 緩存提供者
     */
    public CacheProvider getCacheProvider(CacheProviderEnum cacheProviderEnum) {
        return cacheProviderMap.get(cacheProviderEnum);
    }


    /**
     * 獲取緩存，根據key獲取緩存數據
     * 回傳值為Mono，用於單個查詢
     *
     * @param key               key
     * @param clazz             類型
     * @param cacheProviderEnum 緩存提供者的類型
     *
     * @return Mono<R>
     */
    public <R> Mono<R> getCacheMono(String key, Class<R> clazz, CacheProviderEnum cacheProviderEnum) {
        return Optional.ofNullable(cacheProviderMap.get(cacheProviderEnum)).map(provider -> provider.get(key, clazz)).orElseGet(Mono::empty);
    }


    /**
     * 獲取緩存，根據key獲取緩存數據
     * 回傳值為Flux，用於查詢集合類緩存
     *
     * @param key               key
     * @param clazz             類型
     * @param cacheProviderEnum 緩存提供者的類型
     *
     * @return Flux<R>
     */
    public <R> Flux<R> getCacheFlux(String key, Class<R> clazz, CacheProviderEnum cacheProviderEnum) {
        return Optional
                .ofNullable(cacheProviderMap.get(cacheProviderEnum))
                .map(provider -> provider.getAsList(key, clazz).flatMapMany(Flux::fromIterable))
                .orElseGet(Flux::empty);
    }


    /**
     * 獲取緩存，根據key獲取緩存數據，此為批量查詢
     * 回傳值為Map，key為查詢的key，value為查詢的結果
     *
     * @param keys              key集合
     * @param clazz             類型
     * @param cacheProviderEnum 緩存提供者的類型
     *
     * @return Flux<R>
     */
    public <R> Mono<Map<String, R>> getCaches(Collection<String> keys, Class<R> clazz, CacheProviderEnum cacheProviderEnum) {
        return Optional.ofNullable(cacheProviderMap.get(cacheProviderEnum)).map(provider -> provider.getAllAsMap(keys, clazz)).orElseGet(Mono::empty);
    }


    /**
     * 獲取緩存，根據key獲取緩存數據，此為批量查詢
     * 回傳值為Map，key為查詢的key，value為查詢的結果
     * 這是查詢列表的方法，返回的是Map，內部是列表
     *
     * @param keys              key集合
     * @param clazz             類型
     * @param cacheProviderEnum 緩存提供者的類型
     *
     * @return Flux<R>
     */
    public <R> Mono<Map<String, List<R>>> getListCaches(Collection<String> keys, Class<R> clazz, CacheProviderEnum cacheProviderEnum) {
        return Optional
                .ofNullable(cacheProviderMap.get(cacheProviderEnum))
                .map(provider -> provider.getAllAsMapList(keys, clazz))
                .orElseGet(Mono::empty);
    }


    /**
     * 獲取緩存，根據key獲取緩存數據，此為批量查詢
     * 將所有查詢結果合併為一個Flux
     *
     * @param keys              key集合
     * @param clazz             類型
     * @param cacheProviderEnum 緩存提供者的類型
     *
     * @return Flux<R>
     */
    public <R> Flux<R> getCachesAsConcat(Collection<String> keys, Class<R> clazz, CacheProviderEnum cacheProviderEnum) {
        return getCaches(keys, clazz, cacheProviderEnum)
                .switchIfEmpty(Mono.just(new HashMap<>()))
                .flatMapMany(map -> Flux.fromIterable(map.values()));
    }


    /**
     * 設置緩存，根據key設置緩存數據
     *
     * @param key               key
     * @param value             存儲value
     * @param cacheProviderEnum 緩存提供者的類型
     *
     * @return Mono<Void>
     */
    public Mono<Void> setCache(String key, Object value, CacheProviderEnum cacheProviderEnum) {
        return setCache(key, value, cacheProviderEnum, null);
    }


    /**
     * 設置緩存，根據key設置緩存數據，並設置過期時間
     *
     * @param key               key
     * @param value             存儲value
     * @param cacheProviderEnum 緩存提供者的類型
     * @param expire            過期時間
     *
     * @return Mono<Void>
     */
    public Mono<Void> setCache(String key, Object value, CacheProviderEnum cacheProviderEnum, Duration expire) {
        return Optional
                .ofNullable(cacheProviderMap.get(cacheProviderEnum))
                .map(provider -> tryAcquireRedisLock(key, cacheProviderEnum).flatMap(acquired -> {
                    if (Boolean.TRUE.equals(acquired)) {
                        return provider
                                .set(key, value, expire)
                                .publishOn(Schedulers.boundedElastic())
                                .doFinally(signal -> releaseRedisLock(key, cacheProviderEnum).subscribe());
                    }
                    return Mono.empty();
                }))
                .orElseGet(Mono::empty);
    }


    /**
     * 設置緩存，根據key-value設置緩存數據，此為批量設置
     *
     * @param keyValues         key-value集合
     * @param cacheProviderEnum 緩存提供者的類型
     *
     * @return Mono<Void>
     */
    public Mono<Void> setCaches(Map<String, Object> keyValues, CacheProviderEnum cacheProviderEnum) {
        return setCaches(keyValues, cacheProviderEnum, null);
    }


    /**
     * 設置緩存，根據key-value設置緩存數據，此為批量設置，並設置過期時間
     *
     * @param keyValues         key-value集合
     * @param cacheProviderEnum 緩存提供者的類型
     * @param expire            過期時間
     *
     * @return Mono<Void>
     */
    public Mono<Void> setCaches(Map<String, Object> keyValues, CacheProviderEnum cacheProviderEnum, Duration expire) {
        return Optional
                .ofNullable(cacheProviderMap.get(cacheProviderEnum))
                .map(provider -> tryAcquireRedisLocks(keyValues.keySet(), cacheProviderEnum).flatMap(acquired -> {
                    if (Boolean.TRUE.equals(acquired)) {
                        return provider
                                .setAll(keyValues, expire)
                                .publishOn(Schedulers.boundedElastic())
                                .doFinally(signal -> releaseRedisLock(keyValues.keySet(), cacheProviderEnum).subscribe());
                    }
                    return Mono.empty();
                }))
                .orElseGet(Mono::empty);
    }


    /**
     * 刪除緩存，根據key刪除緩存數據，可以設置是否異步
     *
     * @param key               key
     * @param cacheProviderEnum 緩存提供者的類型
     * @param isAsync           是否異步
     *
     * @return Mono<Void>
     */
    public Mono<Void> deleteCache(String key, CacheProviderEnum cacheProviderEnum, boolean isAsync) {
        return Optional
                .ofNullable(cacheProviderMap.get(cacheProviderEnum))
                .map(provider -> tryAcquireRedisLock(key, cacheProviderEnum).flatMap(acquired -> {
                    if (Boolean.TRUE.equals(acquired)) {
                        return Mono.defer(() -> {
                            Mono<Void> action = provider.delete(key);
                            return isAsync ? action.subscribeOn(Schedulers.boundedElastic()) : action;
                        }).publishOn(Schedulers.boundedElastic()).doFinally(signal -> releaseRedisLock(key, cacheProviderEnum).subscribe());
                    }
                    return Mono.empty();
                }))
                .orElseGet(Mono::empty);
    }


    /**
     * 刪除緩存，根據key刪除緩存數據，此為同步刪除
     *
     * @param key               key
     * @param cacheProviderEnum 緩存提供者的類型
     *
     * @return Mono<Void>
     */
    public Mono<Void> deleteCache(String key, CacheProviderEnum cacheProviderEnum) {
        return deleteCache(key, cacheProviderEnum, false);
    }


    /**
     * 刪除緩存，根據key刪除緩存數據，此為批量刪除，並且可以設置是否異步
     *
     * @param keys              key集合
     * @param cacheProviderEnum 緩存提供者的類型
     * @param isAsync           是否異步
     *
     * @return Mono<Void>
     */
    public Mono<Void> deleteCaches(Collection<String> keys, CacheProviderEnum cacheProviderEnum, boolean isAsync) {
        return Optional
                .ofNullable(cacheProviderMap.get(cacheProviderEnum))
                .map(provider -> tryAcquireRedisLocks(keys, cacheProviderEnum).flatMap(acquired -> {
                    if (Boolean.TRUE.equals(acquired)) {
                        return Mono.defer(() -> {
                            Mono<Void> action = provider.deleteAll(keys);
                            return isAsync ? action.subscribeOn(Schedulers.boundedElastic()) : action;
                        }).publishOn(Schedulers.boundedElastic()).doFinally(signal -> releaseRedisLock(keys, cacheProviderEnum).subscribe());
                    }
                    return Mono.empty();
                }))
                .orElseGet(Mono::empty);

    }


    /**
     * 刪除緩存，根據key刪除緩存數據，此為批量刪除，此為同步刪除
     *
     * @param keys              key集合
     * @param cacheProviderEnum 緩存提供者的類型
     *
     * @return Mono<Void>
     */
    public Mono<Void> deleteCaches(Collection<String> keys, CacheProviderEnum cacheProviderEnum) {
        return deleteCaches(keys, cacheProviderEnum, false);
    }


    /**
     * 獲取Mono的緩存，如果緩存不存在則執行source並將結果存入緩存
     * 此為使用CacheProvider內部的默認過期時間
     *
     * @param key               查詢緩存的key，若緩存不存在此值將作為緩存的key
     * @param clazz             回傳的類型
     * @param cacheProviderEnum 緩存提供者的類型
     * @param source            當沒有緩存時執行的方法，方法的回傳值將作為緩存的值
     * @param cacheRules        緩存規則
     * @param <R>               回傳的類型
     *
     * @return Mono<R> 回傳緩存的值或source的回傳值
     */
    public <R> Mono<R> runAndSetCache(String key, Class<R> clazz, CacheProviderEnum cacheProviderEnum, Mono<? extends R> source, List<CacheRule<R>> cacheRules) {
        return runAndSetCache(key, clazz, cacheProviderEnum, source, cacheRules, null);
    }


    /**
     * 獲取Mono的緩存，如果緩存不存在則執行source並將結果存入緩存，並設置過期時間
     *
     * @param key               查詢緩存的key，若緩存不存在此值將作為緩存的key
     * @param clazz             回傳的類型
     * @param cacheProviderEnum 緩存提供者的類型
     * @param source            當沒有緩存時執行的方法，方法的回傳值將作為緩存的值
     * @param cacheRules        緩存規則
     * @param expire            過期時間
     * @param <R>               回傳的類型
     *
     * @return Mono<R> 回傳緩存的值或source的回傳值
     */
    public <R> Mono<R> runAndSetCache(String key, Class<R> clazz, CacheProviderEnum cacheProviderEnum, Mono<? extends R> source, List<CacheRule<R>> cacheRules, Duration expire) {
        return Optional
                .ofNullable(cacheProviderMap.get(cacheProviderEnum))
                .map(provider -> provider.get(key, clazz).switchIfEmpty(Mono.defer(() -> source.doOnNext(value -> {
                    tryAcquireRedisLock(key, cacheProviderEnum)
                            .filter(Boolean.TRUE::equals)
                            .flatMap(acquired -> applyCacheRule(cacheRules, value, expire))
                            .doFinally(signal -> releaseRedisLock(key, cacheProviderEnum).subscribe())
                            .subscribeOn(Schedulers.boundedElastic())
                            .subscribe();
                }))))
                .orElseGet(() -> source.cast(clazz));
    }


    /**
     * 獲取Flux的緩存，此方法為批量查詢並返回一個結果流
     * 如果緩存不存在則執行source並將結果存入緩存
     * 此為使用CacheProvider內部的默認過期時間
     *
     * @param keys              查詢緩存的key集合，若緩存不存在此值將作為緩存的key
     * @param clazz             回傳的類型
     * @param cacheProviderEnum 緩存提供者的類型
     * @param source            當沒有緩存時執行的方法，方法的回傳值將作為緩存的值
     * @param cacheRules        緩存規則
     * @param <R>               回傳的類型
     *
     * @return Mono<R> 回傳緩存的值或source的回傳值
     */
    public <R> Flux<R> runAndSetCache(Collection<String> keys, Class<R> clazz, CacheProviderEnum cacheProviderEnum, Flux<? extends R> source, List<CacheRule<R>> cacheRules) {
        return runAndSetCache(keys, clazz, cacheProviderEnum, source, cacheRules, null);
    }

    /**
     * 應用緩存規則，將緩存規則應用到需要緩存的值上
     *
     * @param cacheRules 緩存規則
     * @param value      需要緩存的值
     * @param expire     過期時間
     * @param <R>        回傳的類型
     */
    private <R> Mono<Void> applyCacheRule(List<CacheRule<R>> cacheRules, R value, Duration expire) {
        if (value instanceof FluxContainer fluxContainer) {
            return fluxContainer
                    .getFlux()
                    .then()
                    .doOnSuccess(v -> cacheRules.forEach(rule -> rule.apply(value, expire).subscribeOn(Schedulers.boundedElastic()).subscribe()))
                    .subscribeOn(Schedulers.boundedElastic());
        }
        if (value instanceof Flux) {
            return ((Flux<?>) value)
                    .then()
                    .doOnSuccess(v -> cacheRules.forEach(rule -> rule.apply(value, expire).subscribeOn(Schedulers.boundedElastic()).subscribe()))
                    .subscribeOn(Schedulers.boundedElastic());
        }
        return Mono.when(cacheRules.stream().map(rule -> rule.apply(value, expire)).toList()).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 獲取Flux的緩存，此方法為批量查詢並返回一個結果流
     * 如果緩存不存在則執行source並將結果存入緩存，並設置過期時間
     *
     * @param keys              查詢緩存的key集合，若緩存不存在此值將作為緩存的key
     * @param clazz             回傳的類型
     * @param cacheProviderEnum 緩存提供者的類型
     * @param source            當沒有緩存時執行的方法，方法的回傳值將作為緩存的值
     * @param cacheRules        緩存規則
     * @param expire            過期時間
     * @param <R>               回傳的類型
     *
     * @return Mono<R> 回傳緩存的值或source的回傳值
     */
    public <R> Flux<R> runAndSetCache(Collection<String> keys, Class<R> clazz, CacheProviderEnum cacheProviderEnum, Flux<? extends R> source, List<CacheRule<R>> cacheRules, Duration expire) {
        return Optional.ofNullable(cacheProviderMap.get(cacheProviderEnum)).map(provider -> provider.getAllAsMap(keys, clazz).flatMapMany(map -> {
            if (map.values().size() == keys.size()) {
                return Flux.fromIterable(map.values());
            }
            return Flux.defer(() -> source.collectList().flatMapMany(valueList -> {
                return tryAcquireRedisLocks(keys, cacheProviderEnum).flatMapMany(acquired -> {
                    if (Boolean.TRUE.equals(acquired)) {
                        return Flux
                                .fromIterable(valueList)
                                .doOnNext(value -> applyCacheRule(cacheRules, value, expire)
                                        .doFinally(signal -> releaseRedisLock(keys, cacheProviderEnum).subscribe())
                                        .subscribeOn(Schedulers.boundedElastic())
                                        .subscribe());
                    }
                    return Flux.fromIterable(valueList);
                });
            }));
        })).orElseGet(() -> source.cast(clazz));
    }

    /**
     * 獲取Mono的Map緩存，如果緩存不存在則執行source並將結果存入緩存
     * 此為使用CacheProvider內部的默認過期時間
     * 適用於批量查詢，會將結果依照key存入Map
     *
     * @param keys              查詢緩存的key集合，若緩存不存在此值將作為緩存的key
     * @param clazz             回傳的類型
     * @param cacheProviderEnum 緩存提供者的類型
     * @param source            當沒有緩存時執行的方法，方法的回傳值將作為緩存的值
     * @param cacheRules        緩存規則
     * @param <R>               回傳的類型
     *
     * @return Mono<R> 回傳緩存的值或source的回傳值
     */
    public <R> Mono<Map<String, R>> runAndSetCaches(Collection<String> keys, Class<R> clazz, CacheProviderEnum cacheProviderEnum, Function<Collection<String>, Mono<Map<String, R>>> source, List<CacheRule<R>> cacheRules) {
        return runAndSetCaches(keys, clazz, cacheProviderEnum, source, cacheRules, null);
    }

    /**
     * 獲取Mono的Map緩存，如果緩存不存在則執行source並將結果存入緩存
     * 適用於批量查詢，會將結果依照key存入Map
     *
     * @param keys              查詢緩存的key集合，若緩存不存在此值將作為緩存的key
     * @param clazz             回傳的類型
     * @param cacheProviderEnum 緩存提供者的類型
     * @param source            當沒有緩存時執行的方法，方法的回傳值將作為緩存的值
     * @param cacheRules        緩存規則
     * @param <R>               回傳的類型
     *
     * @return Mono<R> 回傳緩存的值或source的回傳值
     */
    public <R> Mono<Map<String, R>> runAndSetCaches(Collection<String> keys, Class<R> clazz, CacheProviderEnum cacheProviderEnum, Function<Collection<String>, Mono<Map<String, R>>> source, List<CacheRule<R>> cacheRules, Duration expire) {
        return Optional
                .ofNullable(cacheProviderMap.get(cacheProviderEnum))
                .map(provider -> provider.getAllAsMap(keys, clazz).switchIfEmpty(Mono.defer(() -> source.apply(keys).doOnNext(resultMap -> {
                    tryAcquireRedisLocks(keys, cacheProviderEnum).flatMap(acquired -> {
                        if (Boolean.TRUE.equals(acquired)) {
                            return Mono.when(resultMap.entrySet().stream().map(entry -> {
                                String key = entry.getKey();
                                R value = entry.getValue();
                                return applyCacheRule(cacheRules, value, expire).doFinally(signal -> releaseRedisLock(key, cacheProviderEnum)
                                        .subscribeOn(Schedulers.boundedElastic())
                                        .subscribe());
                            }).toList());
                        }
                        return Mono.empty();
                    }).subscribeOn(Schedulers.boundedElastic()).subscribe();
                }))))
                .orElseGet(() -> source.apply(keys));
    }

    /**
     * 獲取Flux的緩存，如果緩存不存在則執行source並將結果存入緩存
     *
     * @param key               查詢緩存的ke，若緩存不存在此值將作為緩存的key
     * @param clazz             回傳的類型
     * @param cacheProviderEnum 緩存提供者的類型
     * @param source            當沒有緩存時執行的方法，方法的回傳值將作為緩存的值
     * @param cacheRules        緩存規則
     * @param <R>               回傳的類型
     *
     * @return Flux<R> 回傳緩存的值或source的回傳值
     */
    public <R> Flux<R> runAndSetCache(String key, Class<R> clazz, CacheProviderEnum cacheProviderEnum, Flux<? extends R> source, List<CacheRule<R>> cacheRules) {
        return runAndSetCache(key, clazz, cacheProviderEnum, source, cacheRules, null);
    }

    /**
     * 獲取Flux的緩存，如果緩存不存在則執行source並將結果存入緩存，並設置過期時間
     *
     * @param key               查詢緩存的key集合，若緩存不存在此值將作為緩存的key
     * @param clazz             回傳的類型
     * @param cacheProviderEnum 緩存提供者的類型
     * @param source            當沒有緩存時執行的方法，方法的回傳值將作為緩存的值
     * @param cacheRules        緩存規則
     * @param expire            過期時間
     * @param <R>               回傳的類型
     *
     * @return Flux<R> 回傳緩存的值或source的回傳值
     */
    public <R> Flux<R> runAndSetCache(String key, Class<R> clazz, CacheProviderEnum cacheProviderEnum, Flux<? extends R> source, List<CacheRule<R>> cacheRules, Duration expire) {
        return Optional.ofNullable(cacheProviderMap.get(cacheProviderEnum)).map(provider -> {
            return provider
                    .getAsList(key, clazz)
                    .flatMapMany(Flux::fromIterable)
                    .switchIfEmpty(Flux.defer(() -> source.collectList().flatMapMany(valueList -> {
                        return tryAcquireRedisLock(key, cacheProviderEnum).flatMapMany(acquired -> {
                            if (Boolean.TRUE.equals(acquired)) {
                                return Flux
                                        .fromIterable(valueList)
                                        .doOnNext(value -> applyCacheRule(cacheRules, value, expire)
                                                .doFinally(signal -> releaseRedisLock(key, cacheProviderEnum).subscribe())
                                                .subscribeOn(Schedulers.boundedElastic())
                                                .subscribe());
                            }
                            return Flux.fromIterable(valueList);
                        });
                    })));
        }).orElseGet(() -> source.cast(clazz));
    }

    /**
     * 生成緩存規則，用於將緩存規則封裝成CacheRule，並且指定回傳值的某項屬性作為key
     *
     * @param keyExtractor      key提取器
     * @param cacheProviderEnum 緩存提供者的類型
     * @param <R>               回傳的類型
     *
     * @return CacheRule<R> 緩存規則
     */
    @SkipRecord
    public <R> CacheRule<R> generateCacheRule(Function<R, ?> keyExtractor, CacheProviderEnum cacheProviderEnum) {
        return (value, expire) -> {
            String key = keyExtractor.apply(value).toString();
            return Optional
                    .ofNullable(cacheProviderMap.get(cacheProviderEnum))
                    .map(provider -> provider.set(key, value, expire))
                    .orElseGet(Mono::empty);
        };
    }

    /**
     * 生成緩存規則，用於將緩存規則封裝成CacheRule，並且指定key
     *
     * @param key               key
     * @param cacheProviderEnum 緩存提供者的類型
     * @param <R>               回傳的類型
     *
     * @return CacheRule<R> 緩存規則
     */
    @SkipRecord
    public <R> CacheRule<R> generateCacheRule(String key, CacheProviderEnum cacheProviderEnum) {
        return (value, expire) -> Optional
                .ofNullable(cacheProviderMap.get(cacheProviderEnum))
                .map(provider -> provider.set(key, value, expire))
                .orElseGet(Mono::empty);
    }


    /**
     * 嘗試獲取單個 Redis 鎖
     *
     * @param key               業務 key (不含 prefix)
     * @param cacheProviderEnum 用於生成完整 lock key
     *
     * @return Mono<Boolean> 如果成功獲取鎖，則返回 true，否則返回 false
     */
    private Mono<Boolean> tryAcquireRedisLock(String key, CacheProviderEnum cacheProviderEnum) {
        String lockKey = LOCK_PREFIX + cacheProviderEnum.name() + ":" + key;
        return redisProvider.setValueIfAbsent(lockKey, LOCK_VALUE, CacheManager.DEFAULT_EXPIRE_TIME).onErrorResume(e -> {
            LogUnity.warn("無法獲取 Redis 鎖: %s", e, lockKey);
            return Mono.just(false);
        });
    }


    /**
     * 嘗試獲取多個 Redis 鎖
     * 依次嘗試獲取所有鎖，如果中途失敗，則回滾釋放已獲取的鎖。
     *
     * @param keys              業務 key 集合 (不含 prefix)
     * @param cacheProviderEnum 用於生成完整 lock key
     *
     * @return Mono<Boolean> 如果成功獲取所有鎖，則返回 true，否則返回 false
     */
    private Mono<Boolean> tryAcquireRedisLocks(Collection<String> keys, CacheProviderEnum cacheProviderEnum) {
        if (keys == null || keys.isEmpty()) {
            return Mono.just(true);
        }
        List<String> lockKeyList = keys
                .stream()
                .map(key -> LOCK_PREFIX + cacheProviderEnum.name() + ":" + key)
                .sorted()
                .distinct()
                .collect(Collectors.toList());

        List<String> acquiredLocks = new ArrayList<>();

        return Flux
                .fromIterable(lockKeyList)
                .concatMap(lockKey -> redisProvider.setValueIfAbsent(lockKey, LOCK_VALUE, CacheManager.DEFAULT_EXPIRE_TIME).doOnSuccess(acquired -> {
                    if (Boolean.TRUE.equals(acquired)) {
                        acquiredLocks.add(lockKey);
                    }
                }).onErrorResume(e -> {
                    LogUnity.warn("無法獲取 Redis 鎖: %s", e, lockKey);
                    return Mono.just(false);
                }))
                .all(acquired -> acquired)
                .flatMap(allAcquired -> {
                    if (Boolean.TRUE.equals(allAcquired)) {
                        return Mono.just(true);
                    } else {
                        LogUnity.debug("無法獲取全部所需要的鎖 [%s], 釋放已經獲取的鎖 : [%s]", keys, acquiredLocks);
                        return releaseRedisLocksInternal(acquiredLocks).thenReturn(false);
                    }
                });
    }


    /**
     * 釋放 Redis 鎖
     *
     * @param key               業務 key (不含 prefix)
     * @param cacheProviderEnum 用於生成完整 lock key
     *
     * @return Mono<Void>
     */
    private Mono<Void> releaseRedisLock(String key, CacheProviderEnum cacheProviderEnum) {
        return releaseRedisLock(Collections.singletonList(key), cacheProviderEnum);
    }


    /**
     * 釋放多個 Redis 鎖
     *
     * @param keys              業務 key 集合
     * @param cacheProviderEnum 用於生成完整 lock key
     *
     * @return Mono<Long>
     */
    private Mono<Void> releaseRedisLock(Collection<String> keys, CacheProviderEnum cacheProviderEnum) {
        if (keys == null || keys.isEmpty()) {
            return Mono.empty();
        }
        List<String> lockKeyList = keys
                .stream()
                .map(key -> LOCK_PREFIX + cacheProviderEnum.name() + ":" + key)
                .distinct()
                .collect(Collectors.toList());
        return releaseRedisLocksInternal(lockKeyList);
    }


    /**
     * 釋放多個 Redis 鎖
     *
     * @param lockKeys 完整的 lock key 集合 (包含 prefix)
     *
     * @return Mono<Long> 返回成功刪除的 key 的數量
     */
    private Mono<Void> releaseRedisLocksInternal(Collection<String> lockKeys) {
        if (lockKeys == null || lockKeys.isEmpty()) {
            return Mono.empty();
        }
        return redisProvider.deleteValue(lockKeys).onErrorResume(e -> {
            LogUnity.warn("無法釋放 Redis 鎖: %s", e, lockKeys);
            return Mono.empty();
        });
    }
}
