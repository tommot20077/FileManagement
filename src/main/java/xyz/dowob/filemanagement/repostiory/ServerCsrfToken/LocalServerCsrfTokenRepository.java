package xyz.dowob.filemanagement.repostiory.ServerCsrfToken;

import jakarta.annotation.PreDestroy;
import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.security.web.server.csrf.DefaultCsrfToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.CsrfRepositoryType;
import xyz.dowob.filemanagement.config.properties.SecurityProperties;
import xyz.dowob.filemanagement.customenum.CsrfTokenRepositoryEnum;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.unity.CacheConcurrentHashMap;

/**
 * 基於本地記憶體的 CSRF Token 存放庫實作，使用高效能的併發雜湊表進行 Token 管理。
 * <p>
 * 此類別繼承自 {@link AbstractServerCsrfTokenRepository}，實作了 CSRF Token 的
 * 本地記憶體儲存策略。使用 {@link CacheConcurrentHashMap} 作為底層儲存結構，
 * 提供高效能的 Token 操作和自動過期清理功能。
 * </p>
 * <p>
 * 相較於基於外部儲存的實作，本地儲存具有更高的存取效能和更低的延遲，
 * 但無法提供跨應用程式實例的 Token 共享和持久化保存能力。
 * 適用於單機部署或不需要 Token 持久化的應用場景。
 * </p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see AbstractServerCsrfTokenRepository
 * @see CacheConcurrentHashMap
 * @see CsrfTokenRepositoryEnum
 */
@Component
@CsrfRepositoryType(CsrfTokenRepositoryEnum.LOCAL)
public class LocalServerCsrfTokenRepository extends AbstractServerCsrfTokenRepository {
    /**
     * 儲存 CSRF Token 的併發安全雜湊表。
     * <p>
     * 使用 {@link CacheConcurrentHashMap} 提供高效能的 Token 儲存和自動過期功能，
     * 支援併發存取和記憶體管理。
     * </p>
     */
    private final CacheConcurrentHashMap<String, CsrfToken> csrfTokenMap;


    /**
     * 建構子，初始化本地 CSRF Token 存放庫。
     * <p>
     * 根據安全性設定初始化基礎屬性，並建立具有自動過期功能的
     * 併發雜湊表用於 Token 儲存。
     * </p>
     *
     * @param securityProperties 安全性設定屬性，包含 CSRF 相關設定
     */
    public LocalServerCsrfTokenRepository(SecurityProperties securityProperties) {
        super(securityProperties);
        this.csrfTokenMap = new CacheConcurrentHashMap<>(64, super.expireTime, false);
        this.csrfTokenMap.setTag("Csrf 本地憑證緩存表");
    }


    /**
     * 產生新的 CSRF Token 並儲存至本地快取。
     * <p>
     * 使用 UUID 產生唯一的 Token 值，並立即儲存至本地記憶體中，
     * Token 將在設定的過期時間後自動失效。
     * </p>
     *
     * @param exchange 伺服器 Web 交換物件，包含請求上下文資訊
     * @return 新產生的 CSRF Token {@link Mono}
     */
    @Override
    public Mono<CsrfToken> generateToken(ServerWebExchange exchange) {
        String uuid = java.util.UUID.randomUUID().toString();
        CsrfToken csrfToken = new DefaultCsrfToken(csrfTokenHeader, csrfTokenParameter, uuid);
        csrfTokenMap.set(uuid, csrfToken);
        return Mono.just(csrfToken);
    }


    /**
     * 儲存 CSRF Token（本實作中為空操作）。
     * <p>
     * 由於本實作在 Token 產生時即已儲存，此方法不執行任何操作。
     * 符合 Spring Security 的 Repository 介面契約要求。
     * </p>
     *
     * @param exchange 伺服器 Web 交換物件
     * @param token 要儲存的 CSRF Token
     * @return 空的 {@link Mono}
     */
    @Override
    public Mono<Void> saveToken(ServerWebExchange exchange, CsrfToken token) {
        return Mono.empty();
    }


    /**
     * 從本地快取載入並驗證 CSRF Token。
     * <p>
     * 從 HTTP 請求標頭中取得 Token 值，並在本地快取中查詢對應的 Token。
     * 若 Token 不存在或已過期，將拋出相應的驗證例外。
     * </p>
     *
     * @param exchange 伺服器 Web 交換物件，包含 HTTP 請求資訊
     * @return 載入的 CSRF Token {@link Mono}
     */
    @Override
    public Mono<CsrfToken> loadToken(ServerWebExchange exchange) {
        String userToken = exchange.getRequest().getHeaders().getFirst(csrfTokenHeader);
        if (userToken == null) {
            return Mono.error(new ValidationException(ValidationException.ErrorCode.MISSING_CSRF_TOKEN));
        }

        CsrfToken csrfToken = csrfTokenMap.get(userToken);
        if (csrfToken != null) {
            return Mono.just(csrfToken);
        }
        return Mono.error(new ValidationException(ValidationException.ErrorCode.INVALID_CSRF_TOKEN));
    }


    /**
     * 刪除指定的 CSRF Token 或清理過期的 Token。
     * <p>
     * 提供靈活的 Token 清理機制：
     * <ul>
     *   <li>當指定 Token 時，從快取中移除該 Token</li>
     *   <li>當 Token 為 null 時，執行過期 Token 的清理作業</li>
     * </ul>
     * 有助於維護快取的整潔性和記憶體使用效率。
     * </p>
     *
     * @param token 要刪除的 CSRF Token，為 null 時清理過期 Token
     * @return 表示刪除操作完成的 {@link Mono}
     */
    @Override
    public Mono<Void> deleteToken(CsrfToken token) {
        return Mono.fromRunnable(() -> {
            if (token != null) {
                csrfTokenMap.remove(token.getToken());
                return;
            }
            csrfTokenMap.getCleanupTask().run();
        });
    }


    /**
     * 銷毀 CSRF Token 存放庫並釋放相關資源。
     * <p>
     * 在應用程式關閉時自動呼叫，負責清理本地快取和相關資源，
     * 確保系統正常關閉時不會發生資源洩漏。
     * </p>
     */
    @PreDestroy
    public void destroy() {
        csrfTokenMap.destroy();
    }
}
