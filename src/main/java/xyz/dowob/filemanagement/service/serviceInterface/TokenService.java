package xyz.dowob.filemanagement.service.serviceInterface;

import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.HideSensitive;
import xyz.dowob.filemanagement.customenum.TokenEnum;
import xyz.dowob.filemanagement.entity.Token;
import xyz.dowob.filemanagement.entity.User;

/**
 * 安全令牌服務核心介面，定義企業級令牌管理和身份驗證機制。
 * <p>
 * 本介面提供完整的令牌生命週期管理功能，從生成、驗證到撤銷的全流程安全控制。
 * 繼承自 {@link CrudService} 提供基礎的 CRUD 操作，並擴展專業的令牌安全功能。
 * 採用反應式編程模式確保高效能和可擴展性，特別適用於需要高安全性和高併發的應用場景。
 * <p>
 * <strong>核心安全特性：</strong>
 * <ul>
 *   <li><strong>多類型令牌：</strong>支援 JWT、密碼重設、驗證碼等多種令牌類型</li>
 *   <li><strong>加密安全：</strong>使用業界標準的加密演算法和數位簽章</li>
 *   <li><strong>時效控制：</strong>精確的令牌過期時間管理和自動清理</li>
 *   <li><strong>撤銷機制：</strong>即時令牌撤銷和黑名單管理</li>
 *   <li><strong>審計追蹤：</strong>完整的令牌使用日誌和安全事件記錄</li>
 * </ul>
 * <p>
 * <strong>支援的令牌類型：</strong>
 * <ul>
 *   <li><strong>JWT 令牌：</strong>用於使用者會話管理和 API 存取控制</li>
 *   <li><strong>重設密碼令牌：</strong>安全的密碼重設流程專用令牌</li>
 *   <li><strong>驗證令牌：</strong>雙因素驗證和身份確認令牌</li>
 *   <li><strong>API 金鑰：</strong>第三方整合和服務間通訊令牌</li>
 *   <li><strong>一次性令牌：</strong>單次使用的高安全級別令牌</li>
 * </ul>
 * <p>
 * <strong>安全防護機制：</strong>
 * <ul>
 *   <li><strong>防重放攻擊：</strong>令牌唯一性驗證和時間戳檢查</li>
 *   <li><strong>防篡改保護：</strong>數位簽章驗證和完整性檢查</li>
 *   <li><strong>權限控制：</strong>細粒度的權限範圍控制</li>
 *   <li><strong>異常檢測：</strong>異常使用模式和潛在威脅識別</li>
 *   <li><strong>安全日誌：</strong>敏感操作自動標記 {@code @HideSensitive}</li>
 * </ul>
 * <p>
 * <strong>令牌生命週期管理：</strong>
 * <ol>
 *   <li><strong>生成階段：</strong>
 *     <ul>
 *       <li>驗證使用者身份和權限</li>
 *       <li>設定適當的過期時間和範圍</li>
 *       <li>生成加密簽章和唯一識別碼</li>
 *       <li>持久化令牌資訊到安全儲存</li>
 *     </ul>
 *   </li>
 *   <li><strong>驗證階段：</strong>
 *     <ul>
 *       <li>檢查令牌格式和簽章完整性</li>
 *       <li>驗證過期時間和有效性</li>
 *       <li>確認使用者歸屬和權限範圍</li>
 *       <li>檢查撤銷狀態和黑名單</li>
 *     </ul>
 *   </li>
 *   <li><strong>撤銷階段：</strong>
 *     <ul>
 *       <li>立即標記令牌為無效狀態</li>
 *       <li>更新黑名單和撤銷記錄</li>
 *       <li>清理相關的會話資訊</li>
 *       <li>記錄安全事件和審計日誌</li>
 *     </ul>
 *   </li>
 * </ol>
 * <p>
 * <strong>效能最佳化：</strong>
 * <ul>
 *   <li><strong>非阻塞 I/O：</strong>反應式編程模式提升併發處理能力</li>
 *   <li><strong>快取機制：</strong>熱點令牌快取減少資料庫查詢</li>
 *   <li><strong>批量處理：</strong>批量驗證和撤銷操作最佳化</li>
 *   <li><strong>懶載入：</strong>按需載入令牌詳細資訊</li>
 * </ul>
 * <p>
 * <strong>完整使用範例：</strong>
 * <pre>{@code
 * // 注入服務
 * @Autowired
 * private TokenService tokenService;
 * 
 * // 使用者登入 - 生成 JWT 令牌
 * public Mono<String> loginUser(User user) {
 *     return tokenService.generateToken(user, TokenEnum.JWT)
 *         .doOnSuccess(token -> log.info("使用者 {} 登入成功", user.getUsername()))
 *         .doOnError(error -> log.error("令牌生成失敗: {}", error.getMessage()));
 * }
 * 
 * // API 請求驗證
 * public Mono<Long> authenticateRequest(String authHeader, Long expectedUserId) {
 *     String token = extractTokenFromHeader(authHeader);
 *     return tokenService.validateToken(token, expectedUserId, TokenEnum.JWT)
 *         .doOnSuccess(userId -> log.debug("令牌驗證成功，使用者ID: {}", userId))
 *         .onErrorMap(JwtAuthenticationException.class, 
 *             ex -> new UnauthorizedException("令牌驗證失敗: " + ex.getMessage()));
 * }
 * 
 * // 密碼重設流程
 * public Mono<String> initiatePasswordReset(User user) {
 *     return tokenService.generateToken(user, TokenEnum.PASSWORD_RESET)
 *         .flatMap(resetToken -> emailService.sendPasswordResetEmail(user.getEmail(), resetToken))
 *         .doOnSuccess(token -> log.info("密碼重設令牌已發送給使用者: {}", user.getUsername()));
 * }
 * 
 * // 使用者登出 - 撤銷令牌
 * public Mono<Void> logoutUser(Long userId) {
 *     return tokenService.revokeToken(userId, TokenEnum.JWT)
 *         .doOnSuccess(v -> log.info("使用者 {} 已登出", userId))
 *         .onErrorResume(error -> {
 *             log.warn("令牌撤銷失敗: {}", error.getMessage());
 *             return Mono.empty(); // 即使撤銷失敗也允許登出
 *         });
 * }
 * 
 * // 批量撤銷（安全事件響應）
 * public Mono<Void> revokeAllUserTokens(Long userId) {
 *     return Flux.fromArray(TokenEnum.values())
 *         .flatMap(tokenType -> tokenService.revokeToken(userId, tokenType)
 *             .onErrorResume(error -> {
 *                 log.warn("撤銷 {} 令牌失敗: {}", tokenType, error.getMessage());
 *                 return Mono.empty();
 *             }))
 *         .then()
 *         .doOnSuccess(v -> log.info("已撤銷使用者 {} 的所有令牌", userId));
 * }
 * 
 * // 安全中介軟體
 * @Component
 * public class TokenAuthenticationFilter implements WebFilter {
 *     @Override
 *     public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
 *         return extractToken(exchange)
 *             .flatMap(token -> tokenService.validateToken(token, null, TokenEnum.JWT))
 *             .flatMap(userId -> setAuthenticationContext(exchange, userId))
 *             .then(chain.filter(exchange))
 *             .onErrorResume(this::handleAuthenticationError);
 *     }
 * }
 * }</pre>
 * <p>
 * <strong>安全最佳實踐：</strong>
 * <ul>
 *   <li>所有令牌操作都應標記 {@code @HideSensitive} 避免敏感資訊洩露</li>
 *   <li>實作類別應使用強加密演算法（如 RS256, ES256）</li>
 *   <li>定期輪替簽章金鑰和更新安全設定</li>
 *   <li>實現令牌撤銷的即時廣播機制</li>
 *   <li>建立完整的安全監控和異常檢測</li>
 * </ul>
 * <p>
 * <strong>錯誤處理策略：</strong>
 * <ul>
 *   <li><strong>驗證失敗：</strong>拋出 {@code JwtAuthenticationException}</li>
 *   <li><strong>資料無效：</strong>拋出 {@code ValidationException}</li>
 *   <li><strong>權限不足：</strong>拋出 {@code SecurityException}</li>
 *   <li><strong>系統錯誤：</strong>拋出 {@code ProcessException}</li>
 * </ul>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see CrudService
 * @see Token
 * @see TokenEnum
 * @see xyz.dowob.filemanagement.annotation.HideSensitive
 * @see xyz.dowob.filemanagement.exception.JwtAuthenticationException
 */
public interface TokenService extends CrudService<Token, Long> {
    /**
     * 根據指定的令牌類型為使用者產生安全令牌，提供高度客製化的身份識別機制。實現複雜且安全的令牌生成流程，支援不同類型的令牌應用場景：JWT（JSON Web Token）用於會話管理、密碼重設令牌用於安全地重置密碼、多因素驗證令牌。
     * <p>
     * 令牌生成過程將驗證使用者有效性、根據令牌類型設置適當的有效期、產生加密簽名、將令牌資訊持久化到資料庫。成功時回傳令牌，失敗時傳播錯誤訊息，當使用者資料無效時拋出 ValidationException。
     * <p>
     * 範例用法：
     * <pre>
     * Mono<String> jwtToken = tokenService.generateToken(user, TokenEnum.JWT);
     * Mono<String> resetToken = tokenService.generateToken(user, TokenEnum.PASSWORD_RESET);
     * </pre>
     * 
     * @param user 要產生令牌的目標使用者實體對象，包含使用者識別資訊和權限資料
     * @param tokenType 指定令牌類型，決定令牌的用途、有效期限與安全級別
     * @return {@link Mono} 包含產生的令牌字串的響應式單值流
     * @see TokenEnum
     * @see reactor.core.publisher.Mono
     */
    @HideSensitive
    Mono<String> generateToken(User user, TokenEnum tokenType);

    /**
     * 驗證指定令牌的有效性、真實性和合法性，確認令牌是否可安全使用。提供全面的令牌驗證機制，包括多層次的安全檢查：驗證令牌簽名的正確性、檢查令牌是否在有效期限內、確認令牌是否屬於指定的使用者、查驗令牌是否已被撤銷或無效。
     * <p>
     * 驗證流程將執行嚴格的安全檢查，包括解析令牌內容、驗證簽名、檢查時效性、比對使用者身份、查詢令牌狀態。驗證成功時回傳使用者 ID，失敗時傳播錯誤訊息，當令牌驗證失敗時拋出 JwtAuthenticationException。
     * <p>
     * 範例用法：
     * <pre>
     * Mono<Long> userId = tokenService.validateToken(jwtToken, expectedUserId, TokenEnum.JWT);
     * </pre>
     * 
     * @param token 待驗證的令牌字串，包含使用者身份和權限資訊
     * @param userId 預期的使用者唯一識別碼，用於驗證令牌歸屬
     * @param tokenType 令牌類型，決定驗證的具體規則和有效期限
     * @return {@link Mono} 包含驗證後的使用者 ID 的響應式單值流
     * @throws xyz.dowob.filemanagement.exception.JwtAuthenticationException 當令牌驗證失敗時拋出
     * @see TokenEnum
     * @see reactor.core.publisher.Mono
     */
    Mono<Long> validateToken(String token, Long userId, TokenEnum tokenType);

    /**
     * 撤銷指定使用者的特定類型令牌，使其立即失效並防止進一步使用。提供安全的令牌撤銷機制，適用於多種安全敏感場景：使用者主動登出、密碼變更、檢測到可疑安全事件、強制使用者重新驗證。
     * <p>
     * 撤銷處理將將指定類型令牌標記為無效、從資料庫中刪除或禁用相關令牌、確保被撤銷的令牌無法通過後續驗證、記錄令牌撤銷日誌。成功時完成撤銷操作，失敗時傳播錯誤訊息，當用戶 ID 無效時拋出 ValidationException。
     * <p>
     * 範例用法：
     * <pre>
     * // 登出時撤銷 JWT 令牌
     * Mono<Void> revocationResult = tokenService.revokeToken(userId, TokenEnum.JWT);
     * </pre>
     * 
     * @param userId 要撤銷令牌的使用者唯一識別碼
     * @param tokenType 要撤銷的令牌類型，決定撤銷的具體範圍和方式
     * @return {@link Mono} 空的響應式單值流
     * @see TokenEnum
     * @see reactor.core.publisher.Mono
     */
    Mono<Void> revokeToken(Long userId, TokenEnum tokenType);
}
