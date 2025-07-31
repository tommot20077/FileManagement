package xyz.dowob.filemanagement.repostiory.ServerCsrfToken;

import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.security.web.server.csrf.DefaultCsrfToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.CsrfRepositoryType;
import xyz.dowob.filemanagement.component.provider.provider.RedisProvider;
import xyz.dowob.filemanagement.config.properties.SecurityProperties;
import xyz.dowob.filemanagement.customenum.CsrfTokenRepositoryEnum;
import xyz.dowob.filemanagement.exception.ValidationException;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * 基於 Redis 的 CSRF Token 存放庫實作，提供分散式和持久化的 Token 管理能力。
 * <p>
 * 此類別繼承自 {@link AbstractServerCsrfTokenRepository}，將 CSRF Token 的儲存
 * 交由 Redis 進行管理。相較於本地記憶體儲存，Redis 存放庫提供更高的安全性、
 * 持久化能力和跨應用程式實例的 Token 共享功能。
 * </p>
 * <p>
 * 適用於分散式系統、多實例部署和需要 Token 持久化的應用場景。
 * 利用 Redis 的過期機制和雜湊表結構進行高效的 Token 管理。
 * </p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see AbstractServerCsrfTokenRepository
 * @see RedisProvider
 * @see CsrfTokenRepositoryEnum
 */
@Component
@CsrfRepositoryType(CsrfTokenRepositoryEnum.REDIS)
public class RedisServerCsrfTokenRepository extends AbstractServerCsrfTokenRepository {
    /**
     * Redis 資料存取提供者。
     * <p>
     * 用於與 Redis 進行互動，提供雜湊表操作、過期管理和資料持久化功能。
     * </p>
     */
    private final RedisProvider redisProvider;


    /**
     * 建構子，初始化 Redis CSRF Token 存放庫。
     * <p>
     * 根據安全性設定初始化基礎屬性，並設定 Redis 提供者用於
     * 後續的 Token 儲存和管理操作。
     * </p>
     *
     * @param securityProperties 安全性設定屬性，包含 CSRF 相關設定
     * @param redisProvider Redis 資料存取提供者
     */
    public RedisServerCsrfTokenRepository(SecurityProperties securityProperties, RedisProvider redisProvider) {
        super(securityProperties);
        this.redisProvider = redisProvider;
    }


    /**
     * 產生新的 CSRF Token 並儲存至 Redis。
     * <p>
     * 使用 UUID 產生唯一的 Token 值，並將其及過期時間儲存至 Redis 雜湊表。
     * Redis 將根據設定的過期時間自動清理過期的 Token。
     * </p>
     *
     * @param exchange 伺服器 Web 交換物件，包含請求上下文資訊
     * @return 新產生的 CSRF Token {@link Mono}
     */
    @Override
    public Mono<CsrfToken> generateToken(ServerWebExchange exchange) {
        String uuid = java.util.UUID.randomUUID().toString();
        CsrfToken csrfToken = new DefaultCsrfToken(csrfTokenHeader, csrfTokenParameter, uuid);
        long expireTime = Instant.now().plus(this.expireTime.toMillis(), ChronoUnit.MILLIS).getEpochSecond();
        return redisProvider.setHashMap(csrfTokenHeader, uuid, expireTime, Duration.ofMinutes(expireTime)).thenReturn(csrfToken);
    }


    /**
     * 儲存 CSRF Token（本實作中為空操作）。
     * <p>
     * 由於本實作在 Token 產生時即已儲存至 Redis，此方法不執行任何操作。
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
     * 從 Redis 載入並驗證 CSRF Token。
     * <p>
     * 從 HTTP 請求標頭中取得 Token 值，並在 Redis 中查詢對應的過期時間。
     * 檢查 Token 是否存在及是否已過期，若無效則拋出相應的驗證例外。
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
        return redisProvider
                .getHashMap(csrfTokenHeader, userToken, Integer.class)
                .switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.INVALID_CSRF_TOKEN)))
                .flatMap(time -> {
                    if (time < Instant.now().getEpochSecond()) {
                        return Mono.error(new ValidationException(ValidationException.ErrorCode.INVALID_CSRF_TOKEN));
                    }
                    return Mono.just(new DefaultCsrfToken(csrfTokenHeader, csrfTokenParameter, userToken));
                });
    }


    /**
     * 刪除指定的 CSRF Token 或清理過期的 Token。
     * <p>
     * 提供靈活的 Token 清理機制：
     * <ul>
     *   <li>當指定 Token 時，從 Redis 中移除該 Token</li>
     *   <li>當 Token 為 null 時，扫描並清理所有過期的 Token</li>
     * </ul>
     * 有助於維護 Redis 的整潔性和效能。
     * </p>
     *
     * @param token 要刪除的 CSRF Token，為 null 時清理過期 Token
     * @return 表示刪除操作完成的 {@link Mono}
     */
    @Override
    public Mono<Void> deleteToken(CsrfToken token) {
        Long expireTime = Instant.now().getEpochSecond();
        if (token != null && token.getToken() != null) {
            return redisProvider.deleteHash(csrfTokenHeader, token.getToken()).then();
        }

        return redisProvider
                .getAllHashMap(csrfTokenHeader, String.class, Long.class)
                .filter(entry -> entry.getValue() < expireTime)
                .map(Map.Entry::getKey)
                .collectList()
                .flatMap(expireTokens -> {
                    if (expireTokens.isEmpty()) {
                        return Mono.empty();
                    }
                    return redisProvider.deleteHash(csrfTokenHeader, expireTokens);
                })
                .then();
    }
}
