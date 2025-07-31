package xyz.dowob.filemanagement.repostiory.ServerCsrfToken;

import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.security.web.server.csrf.ServerCsrfTokenRepository;
import reactor.core.publisher.Mono;

/**
 * 自定義的 CSRF Token 存放庫介面，擴展 Spring Security 的標準功能。
 * <p>
 * 此介面繼承自 Spring Security WebFlux 的 {@link ServerCsrfTokenRepository}，
 * 在標準的 CSRF Token 管理功能基礎上，額外提供了 Token 清理功能。
 * 支援響應式程式設計模式，確保非阻塞的安全性操作。
 * </p>
 * <p>
 * 除了基本的 Token 產生、儲存和載入功能外，還提供主動清理過期或無效 Token 的能力，
 * 有助於維護系統安全性和記憶體效能。
 * </p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see ServerCsrfTokenRepository
 * @see CsrfToken
 */

public interface CustomServerCsrfTokenRepository extends ServerCsrfTokenRepository {
    /**
     * 刪除指定的 CSRF Token 或清理過期的 Token。
     * <p>
     * 此方法提供靈活的 Token 清理功能：
     * <ul>
     *   <li>當傳入特定 Token 時，刪除該 Token</li>
     *   <li>當傳入 null 時，清理所有過期的 Token</li>
     * </ul>
     * 有助於維護 Token 儲存的整潔性和系統安全性。
     * </p>
     *
     * @param token 要刪除的 CSRF Token，為 null 時則清理過期 Token
     * @return 表示刪除操作完成的 {@link Mono}
     */
    Mono<Void> deleteToken(CsrfToken token);
}
