package xyz.dowob.filemanagement.service.serviceImpl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.HideSensitive;
import xyz.dowob.filemanagement.annotation.RecordLevel;
import xyz.dowob.filemanagement.component.strategy.CsrfTokenRepositoryStrategy;
import xyz.dowob.filemanagement.customenum.LogLevelEnum;
import xyz.dowob.filemanagement.customenum.TokenEnum;
import xyz.dowob.filemanagement.data.user.dto.AuthRequestDTO;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.repostiory.UserRepository;
import xyz.dowob.filemanagement.service.serviceInterface.AuthorizationService;
import xyz.dowob.filemanagement.service.serviceInterface.TokenService;

/**
 * 授權服務實現，負責用戶認證和安全驗證。提供用戶身份驗證、JWT 令牌生成和 CSRF 令牌管理功能。
 * <p>
 * 所有操作都採用響應式編程模式，回傳 {@code Mono} 類型。認證流程包括查詢用戶資料庫、BCrypt 密碼驗證和生成 JWT 令牌。
 * 認證失敗時通過 {@code Mono.error()} 傳播 {@link ValidationException}，而不是拋出傳統異常。
 * <p>
 * 支持多種 CSRF 令牌存儲實現（如記憶體快取、Redis 等），透過策略模式動態選擇存儲後端。
 * 敏感操作使用 {@code @HideSensitive} 註解防止密碼等資訊被記錄到日誌中。
 * <p>
 * <strong>使用示例：</strong>
 * <pre>{@code
 * // 用戶認證
 * AuthRequestDTO request = new AuthRequestDTO("username", "password");
 * Mono<String> token = authService.authenticate(request, exchange);
 * 
 * // 獲取 CSRF 令牌
 * Mono<CsrfToken> csrfToken = authService.getCSRFToken(exchange);
 * }</pre>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see AuthorizationService
 * @see ValidationException
 */
@Service
@RequiredArgsConstructor
public class AuthorizationServiceImpl implements AuthorizationService {
    /**
     * 用戶資料庫存取介面，用於查詢用戶資訊
     */
    private final UserRepository userRepository;

    /**
     * BCrypt 密碼編碼器，用於密碼比對驗證
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * JWT 令牌服務，負責令牌生成和驗證
     */
    private final TokenService tokenService;

    /**
     * CSRF 令牌存儲庫策略，支援多種存儲實現
     */
    private final CsrfTokenRepositoryStrategy csrfTokenRepository;

    /**
     * 執行用戶認證，不處理伺服器端會話管理。
     * <p>
     * 此方法為 {@link #authenticate(AuthRequestDTO, ServerWebExchange)} 的簡化版本，
     * 將 {@code request} 參數設為 {@code null}。適用於不需要伺服器端會話管理的認證場景，
     * 如 API 令牌產生、批次處理認證等情況。
     * <p>
     * 認證流程與完整版本相同：查詢用戶資料庫、BCrypt 密碼驗證、JWT 令牌生成。
     * 但不執行任何與請求上下文相關的額外安全檢查。
     * <p>
     * <strong>使用示例：</strong>
     * <pre>{@code
     * AuthRequestDTO auth = new AuthRequestDTO("username", "password");
     * Mono<String> token = authenticate(auth)
     *     .doOnSuccess(jwt -> log.info("認證成功，令牌已生成"))
     *     .doOnError(ValidationException.class, error -> 
     *         log.warn("認證失敗: {}", error.getMessage()));
     * }</pre>
     *
     * @param authRequestDTO 認證請求資料，包含用戶名和密碼，不可為 {@code null}
     * @return 包含 JWT 授權令牌的 {@code Mono}，認證失敗時傳播 {@code ValidationException}
     */
    @RecordLevel(LogLevelEnum.INFO)
    public Mono<String> authenticate(AuthRequestDTO authRequestDTO) {
        return authenticate(authRequestDTO, null);
    }

    /**
     * 執行用戶認證並生成 JWT 授權令牌。
     * <p>
     * 認證流程依序執行：根據用戶名查詢用戶資料、使用 BCrypt 比對密碼、驗證成功時生成 JWT 令牌、驗證失敗時傳播 {@code ValidationException}。
     * <p>
     * 此方法使用 {@code @HideSensitive} 註解防止密碼等敏感資訊被記錄到日誌中。
     * 支持伺服器端會話管理，可根據請求上下文進行額外的安全驗證。
     * <p>
     * <strong>使用示例：</strong>
     * <pre>{@code
     * AuthRequestDTO request = new AuthRequestDTO("user123", "password");
     * 
     * authenticate(request, exchange)
     *     .doOnSuccess(token -> log.info("認證成功"))
     *     .doOnError(error -> log.error("認證失敗: {}", error.getMessage()))
     *     .subscribe();
     * }</pre>
     *
     * @param authRequestDTO 認證請求資料，包含用戶名和密碼，不可為 {@code null}
     * @param request 伺服器端交換上下文，可為 {@code null}
     * @return 包含 JWT 令牌字串的 {@code Mono}，認證失敗時傳播 {@code ValidationException}
     */
    @Override
    @HideSensitive
    @RecordLevel(LogLevelEnum.INFO)
    public Mono<String> authenticate(AuthRequestDTO authRequestDTO, ServerWebExchange request) {
        return userRepository
                .findByUsername(authRequestDTO.getUsername())
                .switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.USERNAME_OR_PASSWORD_ERROR)))
                .flatMap(user -> {
                    if (passwordEncoder.matches(authRequestDTO.getPassword(), user.getPassword())) {
                        return tokenService.generateToken(user, TokenEnum.JWT_AUTHORIZATION_TOKEN);
                    }
                    return Mono.error(new ValidationException(ValidationException.ErrorCode.USERNAME_OR_PASSWORD_ERROR));
                });
    }


    /**
     * 生成 CSRF 令牌以防止跨站請求偽造攻擊。
     * <p>
     * 此方法委託給設定的 CSRF 令牌存儲庫策略生成令牌，支援多種存儲實現（如記憶體快取、Redis 等）。
     * 令牌的存儲和檢索機制由策略模式決定，確保在不同部署環境下的靈活性。
     * <p>
     * 生成的令牌包含唯一識別值和標頭名稱，客戶端需將令牌添加到後續請求的標頭中以通過 CSRF 驗證。
     * <p>
     * <strong>使用示例：</strong>
     * <pre>{@code
     * getCSRFToken(exchange)
     *     .doOnNext(token -> {
     *         String tokenValue = token.getToken();
     *         String headerName = token.getHeaderName();
     *         // 將令牌添加到回應標頭
     *     })
     *     .subscribe();
     * }</pre>
     *
     * @param request 伺服器端交換上下文，不可為 {@code null}
     * @return 包含 CSRF 令牌的 {@code Mono}
     */
    @Override
    @RecordLevel(LogLevelEnum.DEBUG)
    public Mono<CsrfToken> getCSRFToken(ServerWebExchange request) {
        return csrfTokenRepository.getCsrfTokenRepository().generateToken(request);
    }
}
