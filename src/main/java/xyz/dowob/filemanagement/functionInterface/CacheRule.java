package xyz.dowob.filemanagement.functionInterface;

import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * 緩存規則函數式接口，用於定義緩存規則
 * 指定一個檔案類型 <T> 並給予一個過期時間
 * 交由後續的緩存處理器進行處理
 *
 * @author yuan
 * @program FileManagement
 * @ClassName CacheRule
 * @create 2025/3/15
 * @Version 1.0
 **/
@FunctionalInterface
public interface CacheRule<T> {
    Mono<Void> apply(T value, Duration expire);
}
