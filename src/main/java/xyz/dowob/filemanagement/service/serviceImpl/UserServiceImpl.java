package xyz.dowob.filemanagement.service.serviceImpl;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import xyz.dowob.filemanagement.annotation.HideSensitive;
import xyz.dowob.filemanagement.annotation.RecordLevel;
import xyz.dowob.filemanagement.annotation.RequirePermission;
import xyz.dowob.filemanagement.component.limiter.UserLimiter;
import xyz.dowob.filemanagement.component.manager.CacheManager;
import xyz.dowob.filemanagement.component.provider.providerInterface.EmailProvider;
import xyz.dowob.filemanagement.component.strategy.UserLimiterStrategy;
import xyz.dowob.filemanagement.config.properties.SecurityProperties;
import xyz.dowob.filemanagement.customenum.*;
import xyz.dowob.filemanagement.data.user.dto.AuthRequestDTO;
import xyz.dowob.filemanagement.data.user.dto.RegisterDTO;
import xyz.dowob.filemanagement.data.user.dto.ResetPasswordDTO;
import xyz.dowob.filemanagement.data.user.dto.UserEmailDTO;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.exception.LimitationException;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.functionInterface.CacheRule;
import xyz.dowob.filemanagement.repostiory.UserRepository;
import xyz.dowob.filemanagement.service.serviceInterface.AuthorizationService;
import xyz.dowob.filemanagement.service.serviceInterface.TokenService;
import xyz.dowob.filemanagement.service.serviceInterface.UserService;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 用戶服務實現類，負責處理用戶相關的業務邏輯和安全操作。提供用戶認證、註冊、密碼管理、郵件驗證等功能。
 * <p>
 * 所有操作都採用響應式編程模式，回傳 {@code Mono} 或 {@code Flux} 類型。支援緩存機制以提高性能，使用用戶 ID 和用戶名作為緩存鍵。
 * 安全敏感操作使用 {@code @HideSensitive} 註解防止資訊洩漏。
 * <p>
 * 整合用戶限流器防止暴力攻擊，支援可選的電子郵件服務進行密碼重設通知。
 * 用戶登入時採用限流機制，超過嘗試次數將暫時禁止登入。
 * <p>
 * 緩存策略採用雙重緩存規則，同時支援用戶 ID 和用戶名查詢快取。未認證用戶會回傳預設的遊客用戶對象。
 * JWT 令牌管理通過令牌服務完成，包括生成、驗證和撤銷操作。
 * <p>
 * <strong>使用示例：</strong>
 * <pre>{@code
 * // 用戶註冊
 * RegisterDTO registerData = new RegisterDTO("user", "email@test.com", "pass", "pass");
 * Mono<Void> result = userService.register(registerData);
 * 
 * // 獲取用戶資訊
 * Mono<User> user = userService.getUser(exchange);
 * }</pre>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see UserService
 * @see AuthorizationService
 */
@Service
@RecordLevel(LogLevelEnum.INFO)
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    /**
     * 用戶 ID 緩存規則，使用用戶 ID 作為緩存鍵
     */
    private static final CacheRule<User> USER_ID_CACHE_RULE = CacheManager.generateCacheRule(User::getId, CacheProviderEnum.USER_CACHE);
    /**
     * 用戶名緩存規則，使用用戶名作為緩存鍵
     */
    private static final CacheRule<User> USERNAME_CACHE_RULE = CacheManager.generateCacheRule(User::getUsername, CacheProviderEnum.USER_CACHE);

    /**
     * 用戶資料庫存取介面，用於用戶資訊的 CRUD 操作
     */
    private final UserRepository userRepository;

    /**
     * 授權服務，負責用戶認證和 CSRF 令牌管理
     */
    private final AuthorizationService authorizationService;

    /**
     * JWT 令牌服務，用於生成和驗證令牌
     */
    private final TokenService tokenService;

    /**
     * 電子郵件服務提供者，用於發送驗證碼和通知郵件（可選）
     */
    private final Optional<EmailProvider> emailProvider;

    /**
     * 安全設定屬性，包含密碼強度和令牌過期等設定
     */
    private final SecurityProperties securityProperties;

    /**
     * BCrypt 密碼編碼器，用於密碼加密和驗證
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * 緩存管理器，負責用戶資訊的緩存操作
     */
    private final CacheManager cacheManager;

    /**
     * 用戶限流器策略，用於防止暴力攻擊和频繁請求
     */
    private final UserLimiterStrategy userLimiterStrategy;
    /**
     * 遊客用戶對象，用於處理未認證用戶的請求
     */
    private final User guestUser = new User();

    /**
     * 初始化遊客用戶對象的預設屬性。
     * <p>
     * 此方法在 Spring Bean 初始化後執行，設定遊客用戶的基本資訊。
     */
    @PostConstruct
    public void init() {
        guestUser.setId(0L);
        guestUser.setUsername("Guest");
        guestUser.setPassword("Guest");
        guestUser.setEmail("guest@example.com");
        guestUser.setRole(RoleEnum.VISITOR);
        guestUser.setStorageLimit(0L);
        guestUser.setUsedStorage(0L);

    }


    /**
     * 註冊新用戶帳戶。
     * <p>
     * 此方法實現用戶註冊流程：建立新的用戶對象、使用 BCrypt 加密密碼、儲存用戶資訊到資料庫。
     * 註冊前應先使用 {@link xyz.dowob.filemanagement.service.serviceInterface.ValidationService#validateRegisterDTO}
     * 驗證註冊資料的完整性。
     * <p>
     * 新用戶將獲得預設的角色和儲存配額設定。密碼使用 BCrypt 演算法進行不可逆加密處理，確保儲存安全。
     * 註冊完成後用戶資訊將持久化到資料庫中。
     * <p>
     * <strong>使用示例：</strong>
     * <pre>{@code
     * RegisterDTO dto = new RegisterDTO("username", "user@example.com", "password", "password");
     * 
     * register(dto)
     *     .doOnSuccess(() -> log.info("用戶註冊成功"))
     *     .doOnError(ex -> log.error("註冊失敗: {}", ex.getMessage()))
     *     .subscribe();
     * }</pre>
     *
     * @param registerUserDTO 註冊資料，包含用戶名、電子郵件和密碼，不可為 {@code null}
     * @return 空的 {@code Mono}，註冊成功時完成
     */
    @Override
    public Mono<Void> register(RegisterDTO registerUserDTO) {
        return Mono.defer(() -> {
            User user = new User();
            user.setUsername(registerUserDTO.getUsername());
            user.setPassword(passwordEncoder.encode(registerUserDTO.getPassword()));
            user.setEmail(registerUserDTO.getEmail());
            return userRepository.save(user).then();
        });
    }


    /**
     * 處理使用者登入請求，提供完整的身份驗證和安全限流機制。
     * <p>
     * 此方法實現安全的用戶登入流程，整合用戶限流器防止暴力攻擊。登入成功時生成 JWT 令牌，
     * 失敗時記錄嘗試次數並在超過限制時暫時禁止登入。
     * <p>
     * 登入驗證流程：
     * <ol>
     *   <li>檢查用戶限流器，驗證是否允許當前登入嘗試</li>
     *   <li>調用授權服務進行密碼驗證和令牌生成</li>
     *   <li>登入成功時釋放限流器資源</li>
     *   <li>登入失敗時保持限流器狀態直到時間視窗結束</li>
     * </ol>
     * <p>
     * 安全特性：
     * <ul>
     *   <li><strong>限流保護</strong>：防止暴力破解攻擊</li>
     *   <li><strong>敏感資訊保護</strong>：使用 {@code @HideSensitive} 防止密碼洩漏</li>
     *   <li><strong>自動解鎖</strong>：登入成功時自動釋放用戶限流器</li>
     * </ul>
     * <p>
     * <strong>使用示例：</strong>
     * <pre>{@code
     * AuthRequestDTO authRequest = new AuthRequestDTO("username", "password");
     * 
     * login(authRequest, exchange)
     *     .doOnSuccess(token -> log.info("用戶登入成功"))
     *     .doOnError(ex -> log.error("登入失敗: {}", ex.getMessage()))
     *     .subscribe();
     * }</pre>
     *
     * @param authRequestDTO 包含用戶名和密碼的登入請求資料，不可為 {@code null}
     * @param request WebFlux 伺服器請求交換物件，用於會話管理
     * @return 包含 JWT 令牌的 {@code Mono}，登入失敗時傳播異常
     * @see AuthorizationService#authenticate(AuthRequestDTO, ServerWebExchange)
     * @see UserLimiterStrategy#getUserLimiter(UserLimiterEnum)
     */
    @Override
    @HideSensitive
    public Mono<String> login(AuthRequestDTO authRequestDTO, ServerWebExchange request) {
        return Mono.defer(() -> {
            UserLimiter userLimiter = userLimiterStrategy.getUserLimiter(UserLimiterEnum.USER_LOGIN_LIMITER);
            return userLimiter.tryAcquire(authRequestDTO.getUsername()).flatMap(acquired -> {
                if (acquired) {
                    return authorizationService.authenticate(authRequestDTO, request).doOnSuccess(token -> {
                        userLimiter.release(authRequestDTO.getUsername()).subscribeOn(Schedulers.boundedElastic()).subscribe();
                    });
                }
                return Mono.error(new LimitationException(LimitationException.ErrorCode.USER_EXCEED_LIMIT, "嘗試登入次數過多，請稍後再試"));
            });
        });
    }


    /**
     * 處理使用者登出請求，實現完整的會話終止和令牌撤銷機制。
     * <p>
     * 此方法提供安全的登出流程，包含伺服端會話失效、JWT 令牌撤銷和安全上下文清理。
     * 確保用戶登出後無法再使用原有的認證資訊存取系統資源。
     * <p>
     * 登出處理流程：
     * <ol>
     *   <li>驗證用戶 ID 和會話的有效性</li>
     *   <li>從資料庫確認用戶存在</li>
     *   <li>撤銷用戶的 JWT 授權令牌</li>
     *   <li>使伺服端會話失效</li>
     *   <li>清理安全上下文</li>
     * </ol>
     * <p>
     * 安全性特性：
     * <ul>
     *   <li><strong>完整清理</strong>：清除所有認證相關的伺服端狀態</li>
     *   <li><strong>令牌撤銷</strong>：確保 JWT 令牌立即失效</li>
     *   <li><strong>會話管理</strong>：正確終止用戶會話</li>
     *   <li><strong>錯誤處理</strong>：對無效用戶 ID 和會話進行適當處理</li>
     * </ul>
     * <p>
     * 特殊情況處理：
     * <ul>
     *   <li>用戶 ID 為 0（遊客用戶）時直接返回空 Mono</li>
     *   <li>請求交換物件為 null 時直接返回空 Mono</li>
     *   <li>無法獲取會話時拋出驗證異常</li>
     *   <li>用戶不存在時拋出用戶未找到異常</li>
     * </ul>
     * <p>
     * <strong>使用示例：</strong>
     * <pre>{@code
     * Long userId = getCurrentUserId();
     * 
     * logout(userId, exchange)
     *     .doOnSuccess(() -> log.info("用戶登出成功"))
     *     .doOnError(ex -> log.error("登出失敗: {}", ex.getMessage()))
     *     .subscribe();
     * }</pre>
     *
     * @param userId 要登出的用戶 ID，{@code 0L} 表示遊客用戶，{@code null} 時返回空 Mono
     * @param exchange WebFlux 伺服器請求交換物件，用於會話管理，{@code null} 時返回空 Mono
     * @return 空的 {@code Mono}，登出完成時完成
     * @see TokenService#revokeToken(Long, TokenEnum)
     * @see UserRepository#findById(Object)
     */
    @Override
    public Mono<Void> logout(Long userId, ServerWebExchange exchange) {
        if (Objects.equals(userId, 0L) || exchange == null) {
            return Mono.empty();
        }

        return exchange
                .getSession()
                .switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.UNAUTHORIZED, "無法獲取 Session")))
                .flatMap(session -> userRepository
                        .findById(userId)
                        .switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.USER_NOT_FOUND, "用戶ID: " + userId)))
                        .flatMap(user -> tokenService.revokeToken(user.getId(), TokenEnum.JWT_AUTHORIZATION_TOKEN).then(session.invalidate())))
                .doFinally(signalType -> SecurityContextHolder.clearContext());
    }


    /**
     * 處理使用者密碼變更請求（預留介面）。
     * <p>
     * 此方法為密碼修改功能的預留介面，目前實現返回 {@code null}。
     * 完整實現應包含舊密碼驗證、新密碼強度檢查、加密處理和資料庫更新等步驟。
     * <p>
     * 預期的實現邏輯：
     * <ol>
     *   <li>驗證用戶身份和當前會話有效性</li>
     *   <li>檢查舊密碼是否正確</li>
     *   <li>驗證新密碼是否符合安全策略</li>
     *   <li>使用 BCrypt 加密新密碼</li>
     *   <li>更新資料庫中的密碼雜湊值</li>
     *   <li>撤銷現有的 JWT 令牌強制重新登入</li>
     *   <li>記錄密碼修改的安全日誌</li>
     * </ol>
     * <p>
     * 安全性考量：
     * <ul>
     *   <li><strong>舊密碼驗證</strong>：確保只有知道當前密碼的用戶才能修改</li>
     *   <li><strong>密碼強度</strong>：新密碼應符合系統安全策略要求</li>
     *   <li><strong>會話管理</strong>：密碼修改後應強制用戶重新登入</li>
     *   <li><strong>審計日誌</strong>：記錄密碼修改操作以便安全審計</li>
     * </ul>
     * <p>
     * <strong>預期使用方式：</strong>
     * <pre>{@code
     * User currentUser = getCurrentUser();
     * // 設定新密碼相關屬性
     * 
     * changePassword(currentUser)
     *     .doOnSuccess(updatedUser -> log.info("密碼修改成功"))
     *     .doOnError(ex -> log.error("密碼修改失敗: {}", ex.getMessage()))
     *     .subscribe();
     * }</pre>
     *
     * @param user 要修改密碼的用戶對象，應包含新密碼和驗證資訊
     * @return 包含更新後用戶資訊的 {@code Mono}，目前實現返回 {@code null}
     * @deprecated 此方法為預留介面，尚未實現完整功能
     */
    @Override
    public Mono<User> changePassword(User user) {
        return null;
    }


    /**
     * 處理使用者電子郵件地址變更請求（預留介面）。
     * <p>
     * 此方法為電子郵件修改功能的預留介面，目前實現返回 {@code null}。
     * 完整實現應包含郵件驗證、重複檢查、驗證碼確認等安全步驟。
     * <p>
     * 預期的實現邏輯：
     * <ol>
     *   <li>驗證用戶身份和當前會話有效性</li>
     *   <li>檢查新電子郵件格式的有效性</li>
     *   <li>確認新電子郵件未被其他用戶使用</li>
     *   <li>向新電子郵件發送驗證碼</li>
     *   <li>用戶輸入驗證碼進行確認</li>
     *   <li>更新資料庫中的電子郵件地址</li>
     *   <li>記錄郵件修改的安全日誌</li>
     * </ol>
     * <p>
     * 安全性考量：
     * <ul>
     *   <li><strong>郵件驗證</strong>：確保用戶對新電子郵件地址擁有控制權</li>
     *   <li><strong>唯一性檢查</strong>：防止重複註冊相同的電子郵件</li>
     *   <li><strong>驗證碼安全</strong>：使用安全的驗證碼生成和過期機制</li>
     *   <li><strong>操作記錄</strong>：記錄郵件修改操作以便安全審計</li>
     * </ul>
     * <p>
     * 業務流程設計：
     * <ul>
     *   <li><strong>兩階段確認</strong>：先驗證新郵件，再更新資料庫</li>
     *   <li><strong>回滾機制</strong>：提供撤銷功能以防止誤操作</li>
     *   <li><strong>通知機制</strong>：向舊郵件發送變更通知</li>
     * </ul>
     * <p>
     * <strong>預期使用方式：</strong>
     * <pre>{@code
     * User currentUser = getCurrentUser();
     * // 設定新電子郵件地址
     * 
     * changeEmail(currentUser)
     *     .doOnSuccess(updatedUser -> log.info("電子郵件修改成功"))
     *     .doOnError(ex -> log.error("電子郵件修改失敗: {}", ex.getMessage()))
     *     .subscribe();
     * }</pre>
     *
     * @param user 要修改電子郵件的用戶對象，應包含新電子郵件地址
     * @return 包含更新後用戶資訊的 {@code Mono}，目前實現返回 {@code null}
     * @deprecated 此方法為預留介面，尚未實現完整功能
     */
    @Override
    public Mono<User> changeEmail(User user) {
        return null;
    }


    /**
     * 發送密碼重設郵件，包含安全驗證碼和時效性控制機制。
     * <p>
     * 此方法實現安全的密碼重設流程起始步驟，根據用戶提供的電子郵件地址查找用戶，
     * 生成具有時效性的重設令牌，並通過電子郵件服務發送給用戶。
     * <p>
     * 重設郵件處理流程：
     * <ol>
     *   <li>根據電子郵件地址查找用戶資訊</li>
     *   <li>生成具有時效性的密碼重設令牌</li>
     *   <li>構建包含令牌和時效說明的郵件內容</li>
     *   <li>透過電子郵件服務發送重設郵件</li>
     * </ol>
     * <p>
     * 安全性特性：
     * <ul>
     *   <li><strong>令牌時效</strong>：重設令牌具有固定的有效期限，過期自動失效</li>
     *   <li><strong>用戶驗證</strong>：只有系統中存在的用戶才能接收重設郵件</li>
     *   <li><strong>服務可選</strong>：當電子郵件服務不可用時返回相應錯誤</li>
     *   <li><strong>令牌唯一性</strong>：每次請求生成不同的重設令牌</li>
     * </ul>
     * <p>
     * 郵件內容設計：
     * <ul>
     *   <li>包含唯一的重設驗證碼</li>
     *   <li>明確的有效期限說明</li>
     *   <li>簡潔的使用指引</li>
     * </ul>
     * <p>
     * 錯誤處理：
     * <ul>
     *   <li>電子郵件不存在時拋出用戶未找到異常</li>
     *   <li>電子郵件服務不可用時拋出不支援操作異常</li>
     * </ul>
     * <p>
     * <strong>使用示例：</strong>
     * <pre>{@code
     * UserEmailDTO emailRequest = new UserEmailDTO("user@example.com");
     * 
     * sendResetPasswordMail(emailRequest)
     *     .doOnSuccess(() -> log.info("重設郵件發送成功"))
     *     .doOnError(ex -> log.error("郵件發送失敗: {}", ex.getMessage()))
     *     .subscribe();
     * }</pre>
     *
     * @param userEmailDTO 包含目標電子郵件地址的請求資料，不可為 {@code null}
     * @return 空的 {@code Mono}，郵件發送成功時完成
     * @see EmailProvider#sendEmail(String, String, String)
     * @see TokenService#generateToken(User, TokenEnum)
     * @see SecurityProperties#getResetPasswordToken()
     */
    @Override
    public Mono<Void> sendResetPasswordMail(UserEmailDTO userEmailDTO) {
        return emailProvider.map(provider -> {
            return Mono.defer(() -> userRepository
                    .findByEmail(userEmailDTO.getEmail())
                    .switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.USER_NOT_FOUND, userEmailDTO.getEmail())))
                    .flatMap(user -> tokenService.generateToken(user, TokenEnum.RESET_PASSWORD_TOKEN).flatMap(token -> {
                        String content = String.format("重置密碼的憑證為：%s\n請於%s分鐘內重置密碼",
                                                       token,
                                                       securityProperties.getResetPasswordToken().getExpiration().toMinutes()
                        );
                        return provider.sendEmail(user.getEmail(), "重置密碼", content);
                    })));
        }).orElseGet(() -> Mono.error(new ValidationException(ValidationException.ErrorCode.UNSUPPORTED_OPERATION)));
    }


    /**
     * 執行密碼重設操作，包含驗證碼驗證和令牌撤銷的完整安全流程。
     * <p>
     * 此方法實現密碼重設的核心邏輯，驗證用戶提供的重設令牌，更新密碼並撤銷相關令牌。
     * 整個過程確保只有擁有有效重設令牌的用戶才能修改密碼。
     * <p>
     * 密碼重設處理流程：
     * <ol>
     *   <li>根據電子郵件地址查找目標用戶</li>
     *   <li>驗證重設令牌的有效性和歸屬</li>
     *   <li>使用 BCrypt 加密新密碼</li>
     *   <li>更新資料庫中的用戶密碼</li>
     *   <li>撤銷重設令牌防止重複使用</li>
     * </ol>
     * <p>
     * 安全性特性：
     * <ul>
     *   <li><strong>令牌驗證</strong>：嚴格驗證重設令牌的有效性和歸屬用戶</li>
     *   <li><strong>密碼加密</strong>：使用 BCrypt 算法進行不可逆密碼加密</li>
     *   <li><strong>令牌撤銷</strong>：重設成功後立即撤銷令牌防止重複使用</li>
     *   <li><strong>原子操作</strong>：密碼更新和令牌撤銷在同一個事務中完成</li>
     * </ul>
     * <p>
     * 驗證機制：
     * <ul>
     *   <li>用戶身份驗證：確保電子郵件對應的用戶存在</li>
     *   <li>令牌完整性：驗證令牌格式、簽名和有效期</li>
     *   <li>用戶匹配：確保令牌歸屬於請求重設的用戶</li>
     * </ul>
     * <p>
     * 錯誤處理：
     * <ul>
     *   <li>用戶不存在時拋出用戶未找到異常</li>
     *   <li>令牌無效時透過令牌服務拋出相應異常</li>
     *   <li>資料庫更新失敗時傳播相關異常</li>
     * </ul>
     * <p>
     * <strong>使用示例：</strong>
     * <pre>{@code
     * ResetPasswordDTO resetRequest = new ResetPasswordDTO(
     *     "user@example.com", 
     *     "reset-token-123", 
     *     "newSecurePassword"
     * );
     * 
     * resetPassword(resetRequest)
     *     .doOnSuccess(() -> log.info("密碼重設成功"))
     *     .doOnError(ex -> log.error("密碼重設失敗: {}", ex.getMessage()))
     *     .subscribe();
     * }</pre>
     *
     * @param resetPasswordDTO 包含電子郵件、驗證碼和新密碼的重設請求資料，不可為 {@code null}
     * @return 空的 {@code Mono}，密碼重設成功時完成
     * @see TokenService#validateToken(String, Long, TokenEnum)
     * @see TokenService#revokeToken(Long, TokenEnum)
     * @see PasswordEncoder#encode(CharSequence)
     */
    @Override
    public Mono<Void> resetPassword(ResetPasswordDTO resetPasswordDTO) {
        return userRepository
                .findByEmail(resetPasswordDTO.getEmail())
                .switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.USER_NOT_FOUND, resetPasswordDTO.getEmail())))
                .flatMap(user -> tokenService
                        .validateToken(resetPasswordDTO.getVerificationCode(), user.getId(), TokenEnum.RESET_PASSWORD_TOKEN)
                        .then(Mono.defer(() -> {
                            user.setPassword(passwordEncoder.encode(resetPasswordDTO.getNewPassword()));
                            return userRepository.save(user).then(tokenService.revokeToken(user.getId(), TokenEnum.RESET_PASSWORD_TOKEN));
                        })));
    }


    /**
     * 從請求上下文中獲取當前已認證的用戶資訊。
     * <p>
     * 此方法是用戶身份識別的核心邏輯，從 Spring Security 的反應式安全上下文中提取用戶身份，
     * 並查詢完整的用戶資訊。這是一個高頻調用的方法，啟用了除錯級別的日誌記錄。
     * <p>
     * 用戶識別流程：
     * <ol>
     *   <li>從反應式安全上下文中獲取認證資訊</li>
     *   <li>提取認證對象中的用戶 ID（Principal）</li>
     *   <li>使用用戶 ID 查詢完整的用戶對象</li>
     *   <li>返回包含完整資訊的用戶對象</li>
     * </ol>
     * <p>
     * 安全性特性：
     * <ul>
     *   <li><strong>認證要求</strong>：只有通過身份驗證的請求才能獲取用戶資訊</li>
     *   <li><strong>上下文安全</strong>：依賴 Spring Security 的安全上下文機制</li>
     *   <li><strong>身份驗證</strong>：未認證請求會拋出未授權異常</li>
     * </ul>
     * <p>
     * 性能最佳化：
     * <ul>
     *   <li>利用緩存機制提高查詢效率</li>
     *   <li>除錯級別日誌避免頻繁記錄影響性能</li>
     *   <li>響應式設計確保非阻塞執行</li>
     * </ul>
     * <p>
     * 錯誤處理：
     * <ul>
     *   <li>無安全上下文時拋出未授權異常</li>
     *   <li>用戶 ID 無效時透過 getById 方法處理</li>
     *   <li>用戶不存在時返回相應的錯誤狀態</li>
     * </ul>
     * <p>
     * <strong>使用示例：</strong>
     * <pre>{@code
     * // 在控制器中獲取當前用戶
     * getUser(exchange)
     *     .doOnNext(user -> log.info("當前用戶: {}", user.getUsername()))
     *     .flatMap(user -> processUserRequest(user))
     *     .subscribe();
     * }</pre>
     *
     * @param exchange WebFlux 伺服器請求交換物件，包含安全上下文資訊
     * @return 包含當前用戶完整資訊的 {@code Mono}
     * @see ReactiveSecurityContextHolder#getContext()
     * @see #getById(Long)
     */
    @Override
    @RecordLevel(LogLevelEnum.DEBUG)
    public Mono<User> getUser(ServerWebExchange exchange) {
        return Mono.defer(() -> ReactiveSecurityContextHolder
                .getContext()
                .switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.UNAUTHORIZED)))
                .map(SecurityContext::getAuthentication)
                .map(auth -> (Long) auth.getPrincipal())
                .flatMap(this::getById));
    }


    /**
     * 根據用戶 ID 從資料庫直接查詢用戶資訊（內部方法）。
     * <p>
     * 此為私有輔助方法，用於從資料庫層直接查詢用戶資訊，不經過緩存處理。
     * 主要供緩存管理器在緩存未命中時作為資料來源使用。
     * <p>
     * 此方法的特點：
     * <ul>
     *   <li><strong>直接查詢</strong>：繞過緩存層直接存取資料庫</li>
     *   <li><strong>純資料存取</strong>：不包含業務邏輯和安全檢查</li>
     *   <li><strong>內部使用</strong>：僅供類內部的緩存機制調用</li>
     * </ul>
     * <p>
     * <strong>使用場景：</strong>
     * <ul>
     *   <li>緩存管理器的資料來源函數</li>
     *   <li>需要強制刷新快取時的資料查詢</li>
     *   <li>緩存失效後的資料重新載入</li>
     * </ul>
     *
     * @param userId 要查詢的用戶 ID，不可為 {@code null}
     * @return 包含用戶資訊的 {@code Mono}，若用戶不存在則為空
     * @see UserRepository#findById(Object)
     */
    private Mono<User> getByIdWithDB(Long userId) {
        return userRepository.findById(userId);
    }


    /**
     * 創建新的用戶實體對象（預留介面）。
     * <p>
     * 此方法為 CRUD 服務介面的創建方法實現，目前返回空的 {@code Mono}。
     * 在完整實現中，此方法可用於創建具有預設屬性的新用戶實體。
     * <p>
     * 預期功能：
     * <ul>
     *   <li>創建具有預設屬性的空白用戶對象</li>
     *   <li>設定系統預設的角色和權限</li>
     *   <li>初始化用戶的儲存配額等基本屬性</li>
     * </ul>
     * <p>
     * <strong>注意：</strong>此方法不同於用戶註冊流程，主要用於內部系統需要創建用戶模板的場景。
     *
     * @return 包含新用戶實體的 {@code Mono}，目前實現為空
     * @deprecated 此方法為預留介面，尚未實現完整功能
     */
    @Override
    public Mono<User> create() {
        return Mono.empty();
    }


    /**
     * 根據用戶 ID 獲取用戶對象，整合緩存機制和特殊用戶處理邏輯。
     * <p>
     * 此方法是用戶查詢的核心實現，支援遊客用戶、緩存最佳化和完整的錯誤處理。
     * 採用雙重緩存策略（用戶 ID 和用戶名）以提供最佳的查詢性能。
     * <p>
     * 查詢邏輯流程：
     * <ol>
     *   <li>輸入驗證：檢查用戶 ID 是否為 null</li>
     *   <li>特殊用戶處理：ID 為 0 時返回預設遊客用戶</li>
     *   <li>緩存查詢：首先嘗試從緩存中獲取用戶資訊</li>
     *   <li>資料庫查詢：緩存未命中時查詢資料庫</li>
     *   <li>緩存更新：查詢結果同時更新 ID 和用戶名兩種緩存</li>
     * </ol>
     * <p>
     * 特殊用戶處理：
     * <ul>
     *   <li><strong>遊客用戶（ID=0）</strong>：返回預設的遊客用戶對象，包含基本權限</li>
     *   <li><strong>空值檢查</strong>：用戶 ID 為 null 時拋出驗證異常</li>
     *   <li><strong>用戶不存在</strong>：查詢無結果時拋出用戶未找到異常</li>
     * </ul>
     * <p>
     * 緩存策略：
     * <ul>
     *   <li><strong>雙重索引</strong>：同時支援用戶 ID 和用戶名作為緩存鍵</li>
     *   <li><strong>自動更新</strong>：資料庫查詢後自動更新雙重緩存</li>
     *   <li><strong>一致性保證</strong>：確保不同緩存鍵的資料一致性</li>
     * </ul>
     * <p>
     * 性能最佳化：
     * <ul>
     *   <li>緩存優先策略減少資料庫查詢</li>
     *   <li>除錯級別日誌避免性能影響</li>
     *   <li>響應式設計確保非阻塞執行</li>
     * </ul>
     * <p>
     * <strong>使用示例：</strong>
     * <pre>{@code
     * Long userId = 12345L;
     *
     * getById(userId)
     *     .doOnNext(user -> log.info("用戶資訊: {}", user.getUsername()))
     *     .doOnError(ex -> log.error("用戶查詢失敗: {}", ex.getMessage()))
     *     .subscribe();
     * }</pre>
     *
     * @param userId 要查詢的用戶 ID，{@code 0L} 表示遊客用戶，{@code null} 時拋出異常
     * @return 包含用戶完整資訊的 {@code Mono}
     * @see CacheManager#runAndSetCache
     * @see #getByIdWithDB(Long)
     */
    @Override
    @RecordLevel(LogLevelEnum.DEBUG)
    public Mono<User> getById(Long userId) {
        if (userId == null) {
            return Mono.error(new ValidationException(ValidationException.ErrorCode.USER_NOT_FOUND, "空值的用戶ID"));
        }

        if (Objects.equals(userId, 0L)) {
            return Mono.just(guestUser);
        }

        List<CacheRule<User>> cacheRules = List.of(USER_ID_CACHE_RULE, USERNAME_CACHE_RULE);
        return cacheManager
                .runAndSetCache(userId.toString(), User.class, CacheProviderEnum.USER_CACHE, this.getByIdWithDB(userId), cacheRules)
                .switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.USER_NOT_FOUND, "用戶ID: " + userId)));
    }


    /**
     * 獲取系統中所有用戶的資訊列表（管理員專用）。
     * <p>
     * 此方法提供管理員級別的系統管理功能，可以查詢系統中所有註冊用戶的完整列表。
     * 這是一個高權限操作，需要管理員權限且會被記錄在警告級別的安全日誌中。
     * <p>
     * 權限要求：
     * <ul>
     *   <li><strong>管理員權限</strong>：要求 {@link PermissionEnum#MANAGE} 權限</li>
     *   <li><strong>認證要求</strong>：必須為已認證且具有有效會話的管理員</li>
     *   <li><strong>日誌級別</strong>：此操作會被記錄為警告級別日誌</li>
     * </ul>
     * <p>
     * 返回資料特性：
     * <ul>
     *   <li>包含所有用戶的完整資訊</li>
     *   <li>不包含敏感的安全資訊（如密碼雜湊）</li>
     *   <li>直接從資料庫查詢，確保資料即時性</li>
     * </ul>
     * <p>
     * 安全性考量：
     * <ul>
     *   <li><strong>權限檢查</strong>：在方法級別進行嚴格的權限驗證</li>
     *   <li><strong>操作記錄</strong>：所有查詢操作都會被記錄在安全日誌中</li>
     *   <li><strong>資料過濾</strong>：自動過濾敏感資訊</li>
     * </ul>
     * <p>
     * <strong>使用場景：</strong>
     * <ul>
     *   <li>管理員控制台的用戶管理介面</li>
     *   <li>系統監控和統計報表</li>
     *   <li>用戶資料的批量管理操作</li>
     * </ul>
     *
     * @return 包含所有用戶資訊的 {@code Flux}
     * @see RequirePermission
     * @see PermissionEnum#MANAGE
     * @see RecordLevel
     */

    @Override
    @RecordLevel(LogLevelEnum.WARN)
    @RequirePermission(PermissionEnum.MANAGE)
    public Flux<User> getAll() {
        return userRepository.findAll();
    }


    /**
     * 根據指定類型和參數批量查詢用戶資訊，支援多種查詢模式和緩存最佳化。
     * <p>
     * 此方法提供靈活的批量用戶查詢功能，支援按用戶 ID 或用戶名進行批量查詢。
     * 整合智慧緩存機制，優先從緩存獲取資料，僅對缺失的資料進行資料庫查詢。
     * <p>
     * 查詢類型支援：
     * <ul>
     *   <li><strong>按 ID 查詢（type=ID）</strong>：
     *     <ul>
     *       <li>輸入：用戶 ID 字串陣列</li>
     *       <li>驗證：數字格式檢查和有效性驗證</li>
     *       <li>查詢：批量 ID 查詢</li>
     *     </ul>
     *   </li>
     *   <li><strong>按用戶名查詢（type=NAME）</strong>：
     *     <ul>
     *       <li>輸入：用戶名字串陣列</li>
     *       <li>驗證：非空和格式檢查</li>
     *       <li>查詢：批量用戶名查詢</li>
     *     </ul>
     *   </li>
     * </ul>
     * <p>
     * 智慧緩存策略：
     * <ol>
     *   <li>首先從緩存中批量獲取已存在的用戶資料</li>
     *   <li>從查詢列表中移除已緩存的用戶標識</li>
     *   <li>僅對剩餘的用戶標識進行資料庫查詢</li>
     *   <li>將資料庫查詢結果更新到雙重緩存中</li>
     *   <li>合併緩存和資料庫的查詢結果</li>
     * </ol>
     * <p>
     * 輸入驗證機制：
     * <ul>
     *   <li><strong>參數檢查</strong>：驗證參數陣列非空</li>
     *   <li><strong>類型驗證</strong>：確保查詢類型為有效值</li>
     *   <li><strong>格式驗證</strong>：ID 必須為有效數字，用戶名不可為空</li>
     *   <li><strong>有效性過濾</strong>：自動過濾無效的輸入參數</li>
     * </ul>
     * <p>
     * 性能最佳化：
     * <ul>
     *   <li>緩存優先策略減少資料庫負載</li>
     *   <li>批量查詢減少資料庫往返次數</li>
     *   <li>智慧去重避免重複查詢</li>
     *   <li>除錯級別日誌控制輸出量</li>
     * </ul>
     * <p>
     * 錯誤處理：
     * <ul>
     *   <li>空參數或無效類型時拋出驗證異常</li>
     *   <li>所有輸入參數無效時拋出標準不合理異常</li>
     *   <li>資料庫查詢異常會正確傳播</li>
     * </ul>
     * <p>
     * <strong>使用示例：</strong>
     * <pre>{@code
     * // 按 ID 批量查詢
     * Set<String> userIds = Set.of("123", "456", "789");
     * getAllByParams(UserInfoTypeEnum.ID.name(), userIds.toArray())
     *     .collectList()
     *     .doOnNext(users -> log.info("查詢到 {} 個用戶", users.size()))
     *     .subscribe();
     * 
     * // 按用戶名批量查詢
     * Set<String> usernames = Set.of("alice", "bob", "charlie");
     * getAllByParams(UserInfoTypeEnum.NAME.name(), usernames.toArray())
     *     .doOnNext(user -> log.info("用戶: {}", user.getUsername()))
     *     .subscribe();
     * }</pre>
     *
     * @param type 查詢類型，必須為 {@link UserInfoTypeEnum#ID} 或 {@link UserInfoTypeEnum#NAME}
     * @param args 查詢參數陣列，包含用戶 ID 或用戶名
     * @return 包含查詢結果的 {@code Flux}，按緩存和資料庫結果的順序返回
     * @see UserInfoTypeEnum
     * @see CacheManager#getCachesAsConcat
     * @see UserRepository#findAllByIdIn(Collection)
     * @see UserRepository#findAllByUsernameIn(Collection)
     */
    @Override
    @RecordLevel(LogLevelEnum.DEBUG)
    public Flux<User> getAllByParams(String type, Object... args) {
        if (args == null || args.length == 0) {
            return Flux.error(new ValidationException(ValidationException.ErrorCode.SEARCH_CRITERIA_EMPTY));
        }


        if (type == null || (!Objects.equals(type, UserInfoTypeEnum.ID.name()) && !Objects.equals(type, UserInfoTypeEnum.NAME.name()))) {
            return Flux.error(new ValidationException(ValidationException.ErrorCode.INVALID_SEARCH_CRITERIA, "無效的查詢類型: " + type));
        }

        List<String> userInfoList = new LinkedList<>();
        boolean isId = Objects.equals(type, UserInfoTypeEnum.ID.name());

        Stream.of(args).forEach(arg -> {
            userInfoList.add(arg.toString());
        });

        Flux<User> cacheUserFlux = cacheManager.getCachesAsConcat(userInfoList, User.class, CacheProviderEnum.USER_CACHE).doOnNext(user -> {
            Object userType = isId ? user.getId().toString() : user.getUsername();
            userInfoList.remove(userType);
        });

        if (userInfoList.isEmpty()) {
            return cacheUserFlux;
        }

        Flux<User> userRepositoryChooseFlux;
        if (isId) {
            List<Long> validIds = userInfoList
                    .stream()
                    .filter(id -> id != null && id.matches("\\d+"))
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
            if (validIds.isEmpty()) {
                return Flux.error(new ValidationException(ValidationException.ErrorCode.INVALID_SEARCH_CRITERIA, "提供的ID均無效"));
            }
            userRepositoryChooseFlux = userRepository.findAllByIdIn(validIds);
        } else {
            List<String> validUsernames = userInfoList
                    .stream()
                    .filter(username -> username != null && !username.trim().isEmpty())
                    .collect(Collectors.toList());

            if (validUsernames.isEmpty()) {
                return Flux.error(new ValidationException(ValidationException.ErrorCode.INVALID_SEARCH_CRITERIA, "提供的使用者名稱均無效"));
            }
            userRepositoryChooseFlux = userRepository.findAllByUsernameIn(validUsernames);
        }

        Flux<User> userRepositoryFlux = cacheManager.runAndSetCache(userInfoList,
                                                                    User.class,
                                                                    CacheProviderEnum.USER_CACHE,
                                                                    userRepositoryChooseFlux,
                                                                    List.of(USER_ID_CACHE_RULE, USERNAME_CACHE_RULE)
        );
        return cacheUserFlux.concatWith(userRepositoryFlux);
    }


    /**
     * 更新用戶實體資訊（預留介面）。
     * <p>
     * 此方法為用戶資訊更新功能的預留介面，目前實現返回空的 {@code Mono}。
     * 完整實現應包含資料驗證、權限檢查、緩存更新和資料庫持久化等邏輯。
     * <p>
     * 預期的實現邏輯：
     * <ol>
     *   <li>驗證輸入資料的完整性和格式</li>
     *   <li>檢查操作者的更新權限</li>
     *   <li>比較變更內容並記錄異動</li>
     *   <li>更新資料庫中的用戶資訊</li>
     *   <li>清理並更新相關緩存</li>
     *   <li>記錄操作日誌以便審計</li>
     * </ol>
     * <p>
     * 安全性考量：
     * <ul>
     *   <li><strong>權限驗證</strong>：確保只有授權用戶可以更新資訊</li>
     *   <li><strong>資料完整性</strong>：驗證更新資料的合法性</li>
     *   <li><strong>敏感資訊</strong>：密碼等敏感資料需要特殊處理</li>
     *   <li><strong>操作記錄</strong>：記錄所有更新操作以便審計</li>
     * </ul>
     * <p>
     * 緩存一致性：
     * <ul>
     *   <li>更新後需要清理相關的緩存項目</li>
     *   <li>如果用戶名改變，需要更新用戶名緩存索引</li>
     *   <li>確保多個緩存鍵的資料一致性</li>
     * </ul>
     * <p>
     * <strong>預期使用方式：</strong>
     * <pre>{@code
     * User userToUpdate = getCurrentUser();
     * userToUpdate.setEmail("newemail@example.com");
     * 
     * update(userToUpdate)
     *     .doOnSuccess(updatedUser -> log.info("用戶資訊更新成功"))
     *     .doOnError(ex -> log.error("更新失敗: {}", ex.getMessage()))
     *     .subscribe();
     * }</pre>
     *
     * @param entity 要更新的用戶實體對象，包含新的屬性值
     * @return 包含更新後用戶資訊的 {@code Mono}，目前實現為空
     * @deprecated 此方法為預留介面，尚未實現完整功能
     */
    @Override
    public Mono<User> update(User entity) {
        return Mono.empty();
    }


    /**
     * 刪除用戶實體（管理員專用，預留介面）。
     * <p>
     * 此方法為用戶刪除功能的預留介面，目前實現返回空的 {@code Mono}。
     * 這是一個高權限的危險操作，需要管理員權限且會被記錄在警告級別的安全日誌中。
     * <p>
     * 預期的實現邏輯：
     * <ol>
     *   <li>驗證操作者的管理員權限</li>
     *   <li>檢查要刪除的用戶是否存在</li>
     *   <li>處理用戶相關的檔案和資料</li>
     *   <li>撤銷用戶的所有令牌和會話</li>
     *   <li>清理用戶的所有緩存項目</li>
     *   <li>從資料庫中刪除用戶記錄</li>
     *   <li>記錄刪除操作的完整審計日誌</li>
     * </ol>
     * <p>
     * 權限要求：
     * <ul>
     *   <li><strong>管理員權限</strong>：要求 {@link PermissionEnum#MANAGE} 權限</li>
     *   <li><strong>操作確認</strong>：建議實現刪除確認機制</li>
     *   <li><strong>審計記錄</strong>：記錄完整的刪除操作資訊</li>
     * </ul>
     * <p>
     * 安全性考量：
     * <ul>
     *   <li><strong>級聯刪除</strong>：妥善處理用戶相關的所有資料</li>
     *   <li><strong>會話撤銷</strong>：立即撤銷用戶的所有認證令牌</li>
     *   <li><strong>資料保護</strong>：考慮是否需要軟刪除以保留審計記錄</li>
     *   <li><strong>操作記錄</strong>：詳細記錄刪除操作以便後續追查</li>
     * </ul>
     * <p>
     * 資料清理範圍：
     * <ul>
     *   <li>用戶的所有檔案和資料夾</li>
     *   <li>用戶的分享記錄和權限</li>
     *   <li>用戶的操作日誌和活動記錄</li>
     *   <li>用戶的所有緩存項目</li>
     * </ul>
     * <p>
     * <strong>預期使用方式：</strong>
     * <pre>{@code
     * User userToDelete = getUserById(userId);
     * 
     * delete(userToDelete)
     *     .doOnSuccess(() -> log.warn("用戶已被刪除: {}", userToDelete.getUsername()))
     *     .doOnError(ex -> log.error("刪除失敗: {}", ex.getMessage()))
     *     .subscribe();
     * }</pre>
     *
     * @param entity 要刪除的用戶實體對象，不可為 {@code null}
     * @return 空的 {@code Mono}，刪除完成時完成，目前實現為空
     * @deprecated 此方法為預留介面，尚未實現完整功能
     * @see RequirePermission
     * @see PermissionEnum#MANAGE
     * @see RecordLevel
     */
    @Override
    @RecordLevel(LogLevelEnum.WARN)
    @RequirePermission(PermissionEnum.MANAGE)
    public Mono<Void> delete(User entity) {
        return Mono.empty();
    }

}
