package xyz.dowob.filemanagement.repostiory;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.entity.Token;

import java.time.LocalDateTime;

/**
 * 憑證資料存取層介面，提供 JWT 憑證的響應式資料庫操作功能。
 * <p>
 * 此介面繼承自 Spring Data R2DBC 的 {@link ReactiveCrudRepository}，
 * 專門用於管理用戶 JWT 憑證的生命週期，包括憑證的建立、查詢、更新和清理。
 * 採用非阻塞的響應式程式設計模式，確保高並發情況下的效能表現。
 * </p>
 * <p>
 * 主要功能包括：
 * <ul>
 *   <li>基本的 CRUD 操作（繼承自父介面）</li>
 *   <li>根據用戶 ID 查詢對應的憑證記錄</li>
 *   <li>查詢並清理過期的 JWT 憑證</li>
 *   <li>支援憑證續簽和撤銷操作</li>
 * </ul>
 * </p>
 * <p>
 * 使用範例：
 * <pre>{@code
 * @Autowired
 * private TokenRepository tokenRepository;
 * 
 * // 查詢用戶憑證
 * Mono<Token> userToken = tokenRepository.findByUserId(userId);
 * 
 * // 清理過期憑證（定時任務使用）
 * Flux<Token> expiredTokens = tokenRepository.findAllByJwtTokenExpireTimeIsBefore(LocalDateTime.now());
 * }</pre>
 * </p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see Token
 * @see ReactiveCrudRepository
 */
@Repository
public interface TokenRepository extends ReactiveCrudRepository<Token, Long> {
    /**
     * 根據用戶 ID 查詢對應的 JWT 憑證記錄。
     * <p>
     * 此方法用於查詢特定用戶的現有憑證，通常用於憑證驗證、
     * 憑證續簽和用戶登出等場景。每個用戶在系統中通常只會有一個有效的憑證記錄。
     * </p>
     *
     * @param userId 用戶的唯一識別碼，不得為 null 或負數
     * @return 包含用戶憑證的 {@link Mono}，如果找不到則為空
     */
    Mono<Token> findByUserId(long userId);

    /**
     * 查詢所有在指定時間之前過期的 JWT 憑證。
     * <p>
     * 此方法主要用於系統的定時清理任務，定期清除已過期的憑證記錄
     * 以節省儲存空間並維護系統安全性。透過比較憑證的過期時間與指定時間點，
     * 找出所有需要清理的過期憑證。
     * </p>
     *
     * @param expireTime 過期時間的判斷基準點，早於此時間的憑證將被視為過期
     * @return 包含所有過期憑證的 {@link Flux}，可能為空流
     */
    Flux<Token> findAllByJwtTokenExpireTimeIsBefore(LocalDateTime expireTime);
}
