package xyz.dowob.filemanagement.component.manager;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import xyz.dowob.filemanagement.annotation.CacheProviderType;
import xyz.dowob.filemanagement.annotation.SkipRecord;
import xyz.dowob.filemanagement.component.provider.providerInterface.CacheProvider;
import xyz.dowob.filemanagement.customenum.CacheProviderEnum;
import xyz.dowob.filemanagement.functionInterface.CacheRule;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

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
@SkipRecord
public class CacheManager {
    /**
     * 緩存提供者的Map，用於存儲不同類型的緩存提供者
     */
    private final EnumMap<CacheProviderEnum, CacheProvider> cacheProviderMap;

    /**
     * 緩存鎖Map，用於存儲緩存的鎖
     */
    private static final Map<String, ReentrantLock> lockMap = new ConcurrentHashMap<>();

    /**
     * 緩存管理器的構造方法，用於初始化緩存提供者列表
     * 會將緩存提供者列表轉換為EnumMap，方便根據CacheProviderEnum獲取對應的CacheProvider
     *
     * @param cacheProviderList 緩存提供者列表
     */
    public CacheManager(List<CacheProvider> cacheProviderList) {
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
     * @return Mono<T>
     */
    public <T> Mono<T> getCacheMono(String key, Class<T> clazz, CacheProviderEnum cacheProviderEnum) {
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
     * @return Flux<T>
     */
    public <T> Flux<T> getCacheFlux(String key, Class<T> clazz, CacheProviderEnum cacheProviderEnum) {
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
     * @return Flux<T>
     */
    public <T> Mono<Map<String, T>> getCaches(Collection<String> keys, Class<T> clazz, CacheProviderEnum cacheProviderEnum) {
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
     * @return Flux<T>
     */
    public <T> Mono<Map<String, List<T>>> getListCaches(Collection<String> keys, Class<T> clazz, CacheProviderEnum cacheProviderEnum) {
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
     * @return Flux<T>
     */
    public <T> Flux<T> getCachesAsConcat(Collection<String> keys, Class<T> clazz, CacheProviderEnum cacheProviderEnum) {
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
        return Optional.ofNullable(cacheProviderMap.get(cacheProviderEnum)).map(provider -> {
            WriteLock.tryLock(key, cacheProviderEnum, k -> provider.set(key, value, expire).subscribeOn(Schedulers.boundedElastic()).subscribe());
            return Mono.empty();
        }).orElseGet(Mono::empty).then();
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
        return Optional.ofNullable(cacheProviderMap.get(cacheProviderEnum)).map(provider -> {
            WriteLock.tryLock(keyValues.keySet(),
                              cacheProviderEnum,
                              k -> provider.setAll(keyValues, expire).subscribeOn(Schedulers.boundedElastic()).subscribe()
            );
            return Mono.empty();
        }).orElseGet(Mono::empty).then();
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
        return Optional.ofNullable(cacheProviderMap.get(cacheProviderEnum)).map(provider -> {
            WriteLock.tryLock(key, cacheProviderEnum, k -> {
                Mono<Void> action = provider.delete(key);
                return isAsync ? action.subscribeOn(Schedulers.boundedElastic()).subscribe() : action;
            });
            return Mono.empty();
        }).orElseGet(Mono::empty).then();
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
        return Optional.ofNullable(cacheProviderMap.get(cacheProviderEnum)).map(provider -> {
            WriteLock.tryLock(keys, cacheProviderEnum, k -> {
                Mono<Void> action = provider.deleteAll(keys);
                return isAsync ? action.subscribeOn(Schedulers.boundedElastic()).subscribe() : action;
            });
            return Mono.empty();
        }).orElseGet(Mono::empty).then();
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
     * @param <T>               回傳的類型
     *
     * @return Mono<T> 回傳緩存的值或source的回傳值
     */
    public <T> Mono<T> runAndSetCache(String key, Class<T> clazz, CacheProviderEnum cacheProviderEnum, Mono<? extends T> source, List<CacheRule<T>> cacheRules) {
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
     * @param <T>               回傳的類型
     *
     * @return Mono<T> 回傳緩存的值或source的回傳值
     */
    public <T> Mono<T> runAndSetCache(String key, Class<T> clazz, CacheProviderEnum cacheProviderEnum, Mono<? extends T> source, List<CacheRule<T>> cacheRules, Duration expire) {
        return Optional
                .ofNullable(cacheProviderMap.get(cacheProviderEnum))
                .map(provider -> provider.get(key, clazz).switchIfEmpty(source.doOnNext(value -> {
                    WriteLock.tryLock(key, cacheProviderEnum, k -> applyCacheRule(cacheRules, value, expire));
                })))
                .orElseGet(() -> source.cast(clazz));
    }

    /**
     * 獲取Flux的緩存，如果緩存不存在則執行source並將結果存入緩存
     *
     * @param key               查詢緩存的ke，若緩存不存在此值將作為緩存的key
     * @param clazz             回傳的類型
     * @param cacheProviderEnum 緩存提供者的類型
     * @param source            當沒有緩存時執行的方法，方法的回傳值將作為緩存的值
     * @param cacheRules        緩存規則
     * @param <T>               回傳的類型
     *
     * @return Flux<T> 回傳緩存的值或source的回傳值
     */
    public <T> Flux<T> runAndSetCache(String key, Class<T> clazz, CacheProviderEnum cacheProviderEnum, Flux<? extends T> source, List<CacheRule<T>> cacheRules) {
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
     * @param <T>               回傳的類型
     *
     * @return Flux<T> 回傳緩存的值或source的回傳值
     */
    public <T> Flux<T> runAndSetCache(String key, Class<T> clazz, CacheProviderEnum cacheProviderEnum, Flux<? extends T> source, List<CacheRule<T>> cacheRules, Duration expire) {
        return Optional
                .ofNullable(cacheProviderMap.get(cacheProviderEnum))
                .map(provider -> provider.getAsList(key, clazz).flatMapMany(Flux::fromIterable).switchIfEmpty(source.doOnNext(value -> {
                    WriteLock.tryLock(key, cacheProviderEnum, k -> applyCacheRule(cacheRules, value, expire));
                })))
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
     * @param <T>               回傳的類型
     *
     * @return Mono<T> 回傳緩存的值或source的回傳值
     */
    public <T> Flux<T> runAndSetCache(Collection<String> keys, Class<T> clazz, CacheProviderEnum cacheProviderEnum, Flux<? extends T> source, List<CacheRule<T>> cacheRules) {
        return runAndSetCache(keys, clazz, cacheProviderEnum, source, cacheRules, null);
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
     * @param <T>               回傳的類型
     *
     * @return Mono<T> 回傳緩存的值或source的回傳值
     */
    public <T> Flux<T> runAndSetCache(Collection<String> keys, Class<T> clazz, CacheProviderEnum cacheProviderEnum, Flux<? extends T> source, List<CacheRule<T>> cacheRules, Duration expire) {
        return Optional.ofNullable(cacheProviderMap.get(cacheProviderEnum)).map(provider -> provider.getAllAsMap(keys, clazz).flatMapMany(map -> {
            if (map.values().size() == keys.size()) {
                return Flux.fromIterable(map.values());
            }
            return source.doOnNext(value -> {
                WriteLock.tryLock(keys, cacheProviderEnum, k -> applyCacheRule(cacheRules, value, expire));
            });
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
     * @param <T>               回傳的類型
     *
     * @return Mono<T> 回傳緩存的值或source的回傳值
     */
    public <T> Mono<Map<String, T>> runAndSetCaches(Collection<String> keys, Class<T> clazz, CacheProviderEnum cacheProviderEnum, Function<Collection<String>, Mono<Map<String, T>>> source, List<CacheRule<T>> cacheRules, Duration expire) {
        return Optional
                .ofNullable(cacheProviderMap.get(cacheProviderEnum))
                .map(provider -> provider.getAllAsMap(keys, clazz).switchIfEmpty(source.apply(keys).doOnNext(resultMap -> {
                    resultMap.forEach((key, value) -> {
                        WriteLock.tryLock(key, cacheProviderEnum, k -> applyCacheRule(cacheRules, value, expire));
                    });
                })))
                .orElseGet(() -> source.apply(keys));
    }


    /**
     * 生成緩存規則，用於將緩存規則封裝成CacheRule，並且指定回傳值的某項屬性作為key
     *
     * @param keyExtractor      key提取器
     * @param cacheProviderEnum 緩存提供者的類型
     * @param <T>               回傳的類型
     *
     * @return CacheRule<T> 緩存規則
     */
    @SkipRecord
    public <T> CacheRule<T> generateCacheRule(Function<T, ?> keyExtractor, CacheProviderEnum cacheProviderEnum) {
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
     * @param <T>               回傳的類型
     *
     * @return CacheRule<T> 緩存規則
     */
    @SkipRecord
    public <T> CacheRule<T> generateCacheRule(String key, CacheProviderEnum cacheProviderEnum) {
        return (value, expire) -> Optional
                .ofNullable(cacheProviderMap.get(cacheProviderEnum))
                .map(provider -> provider.set(key, value, expire))
                .orElseGet(Mono::empty);
    }

    /**
     * 應用緩存規則，將緩存規則應用到需要緩存的值上
     *
     * @param cacheRules 緩存規則
     * @param value      需要緩存的值
     * @param expire     過期時間
     * @param <T>        回傳的類型
     */
    private <T> Void applyCacheRule(List<CacheRule<T>> cacheRules, T value, Duration expire) {
        Mono.when(cacheRules.stream().map(rule -> rule.apply(value, expire)).toList()).subscribeOn(Schedulers.boundedElastic()).subscribe();
        return null;
    }

    /**
     * 緩存鎖，用於對緩存進行加鎖操作
     */
    @SkipRecord
    private static class WriteLock {
        /**
         * 生成鎖的key並且嘗試執行操作
         *
         * @param key               鍵
         * @param cacheProviderEnum 緩存提供者的類型
         * @param source            操作
         */
        private static void tryLock(String key, CacheProviderEnum cacheProviderEnum, Function<?, ?> source) {
            String lockKey = cacheProviderEnum.name() + ":" + key;
            doAction(lockKey, cacheProviderEnum, source);
        }

        /**
         * 生成鎖的key並且嘗試執行操作
         *
         * @param keys              鍵
         * @param cacheProviderEnum 緩存提供者的類型
         * @param source            操作
         */
        private static void tryLock(Collection<String> keys, CacheProviderEnum cacheProviderEnum, Function<?, ?> source) {
            StringBuilder keyBuilder = new StringBuilder();
            keys.forEach(keyBuilder::append);
            String lockKey = cacheProviderEnum.name() + ":" + keyBuilder;
            doAction(lockKey, cacheProviderEnum, source);
        }

        /**
         * 嘗試執行操作，如果獲取到鎖則執行操作，否則不執行
         * 並且在操作完成後釋放鎖並且從鎖Map中移除
         *
         * @param lockKey           鎖的key
         * @param cacheProviderEnum 緩存提供者的類型
         * @param source            操作
         */
        private static void doAction(String lockKey, CacheProviderEnum cacheProviderEnum, Function<?, ?> source) {
            ReentrantLock lock = lockMap.computeIfAbsent(lockKey, k -> new ReentrantLock());
            try {
                if (lock.tryLock()) {
                    source.apply(null);
                }
            } finally {
                lock.unlock();
                lockMap.remove(lockKey);
            }
        }
    }
}
