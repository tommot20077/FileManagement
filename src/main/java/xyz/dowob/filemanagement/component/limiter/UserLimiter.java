package xyz.dowob.filemanagement.component.limiter;

import reactor.core.publisher.Mono;

/**
 * 響應式用戶限流器接口，提供非阻塞的請求限制與控制機制。
 * 
 * <p>本介面定義了標準的用戶限流操作，支援基於不同策略的流量控制實現。
 * 主要用於防止用戶過度使用系統資源，確保系統穩定性和服務品質。</p>
 * 
 * <p>限流器的核心操作包括許可取得和釋放，採用響應式設計模式，
 * 所有操作回傳 {@link reactor.core.publisher.Mono} 以支援非阻塞處理。</p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */

public interface UserLimiter {
    /**
     * 嘗試取得限流許可，驗證用戶是否可執行受限制的操作。
     * 
     * <p>根據限流策略檢查指定用戶的請求是否允許通過。
     * 若用戶請求頻率或數量超過設定限制，則拒絕許可。</p>
     *
     * @param key 用戶識別值，可為用戶ID、用戶名或其他唯一標識
     * @return 響應式布林值，true表示許可通過，false表示已達限制
     */
    Mono<Boolean> tryAcquire(Object key);

    /**
     * 釋放限流許可，清理或重置用戶的限流狀態。
     * 
     * <p>當用戶完成受限制的操作或需要重設限流計數器時調用。
     * 具體行為依據限流器實現策略而定。</p>
     *
     * @param key 用戶識別值，與 tryAcquire 使用相同的標識
     * @return 完成信號，表示釋放操作已執行
     */
    Mono<Void> release(Object key);
}
