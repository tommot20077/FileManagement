package xyz.dowob.filemanagement.functionInterface;

import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * 定義快取操作規則的函數式介面。此介面採用響應式程式設計模式，
 * 提供將指定值存入快取系統並設定過期時間的統一方法。
 * <p>
 * 實現此介面的規則應支援任意類型的資料快取，並回傳表示操作完成狀態的 Mono。
 * 快取操作為非阻塞式，適用於高並發環境下的資料快取需求。
 * </p>
 * <p>
 * 典型使用場景包括用戶資料快取、檔案元資料快取和計算結果快取等。
 * </p>
 *
 * @param <T> 需要快取的資料型別
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see Duration
 * @see Mono
 */
@FunctionalInterface
public interface CacheRule<T> {
    /**
     * 應用快取規則，將指定的值存入快取系統。
     * <p>
     * 此方法以響應式方式執行快取操作，支援各種快取後端實現。
     * 操作完成後回傳空的 Mono，表示快取操作的完成狀態。
     * 過期時間使用 Duration.ZERO 表示立即過期，null 值的處理取決於具體實現。
     * </p>
     *
     * @param value  需要快取的資料物件
     * @param expire 快取的過期時間
     * @return 表示快取操作完成的 Mono，成功完成時為空值
     */
    Mono<Void> apply(T value, Duration expire);
}
