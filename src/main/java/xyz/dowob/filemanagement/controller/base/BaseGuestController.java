package xyz.dowob.filemanagement.controller.base;

import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.RecordLevel;
import xyz.dowob.filemanagement.config.properties.SecurityProperties;
import xyz.dowob.filemanagement.customenum.LogLevelEnum;
import xyz.dowob.filemanagement.data.response.ApiResponseDTO;
import xyz.dowob.filemanagement.data.user.dto.*;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.service.serviceInterface.AuthorizationService;
import xyz.dowob.filemanagement.service.serviceInterface.UserService;
import xyz.dowob.filemanagement.service.serviceInterface.ValidationService;
import xyz.dowob.filemanagement.unity.ResponseUnity;

import java.util.HashMap;

/**
 * 基於 Spring WebFlux 反應式程式設計的訪客控制器抽象基類，提供未認證使用者的核心業務操作。
 *
 * <p>此抽象基類作為檔案管理系統中所有訪客控制器的統一基礎架構，實現了未認證使用者（Guest）
 * 相關的標準化業務邏輯處理。透過反應式程式設計模型，確保高併發環境下的非阻塞處理能力。</p>
 *
 * <h3>核心功能領域：</h3>
 * <ul>
 *   <li><strong>使用者註冊</strong>：新使用者帳戶建立，包含資料驗證和業務規則檢查</li>
 *   <li><strong>身份認證</strong>：使用者登入驗證，支援 JWT 令牌和 Cookie 會話管理</li>
 *   <li><strong>授權狀態檢查</strong>：驗證使用者當前認證狀態和基本資訊查詢</li>
 *   <li><strong>密碼管理</strong>：密碼重置流程，包含郵件驗證和安全性檢查</li>
 * </ul>
 *
 * <h3>設計架構特點：</h3>
 * <ul>
 *   <li><strong>反應式設計</strong>：所有操作返回 {@link Mono} 或 {@link reactor.core.publisher.Flux}，確保非阻塞處理</li>
 *   <li><strong>統一異常處理</strong>：透過 {@link ResponseUnity#handleError} 提供標準化錯誤回應格式</li>
 *   <li><strong>多環境適配</strong>：支援 Web 和 API 環境的不同認證機制（Cookie vs Token）</li>
 *   <li><strong>安全性考量</strong>：整合安全性配置，支援 CSRF 保護和安全 Cookie 設定</li>
 * </ul>
 *
 * <h3>繼承指導原則：</h3>
 * <p>所有具體的訪客控制器實現類別應繼承此基類，並遵循以下設計準則：</p>
 * <ul>
 *   <li>保持方法簽名的一致性，確保 API 介面的標準化</li>
 *   <li>適當重寫基礎方法以實現特定環境的客製化邏輯</li>
 *   <li>遵循反應式程式設計模式，避免阻塞操作</li>
 *   <li>使用標準化的 DTO 物件進行資料傳輸</li>
 * </ul>
 *
 * <h3>安全性特性：</h3>
 * <ul>
 *   <li><strong>輸入驗證</strong>：所有使用者輸入都經過嚴格的格式和業務規則驗證</li>
 *   <li><strong>認證管理</strong>：支援 JWT 令牌認證和安全 Cookie 會話管理</li>
 *   <li><strong>錯誤處理</strong>：敏感資訊不會在錯誤回應中洩露</li>
 *   <li><strong>率限制整合</strong>：與系統限流機制無縫整合</li>
 * </ul>
 *
 * <h3>使用範例：</h3>
 * <pre>{@code
 * @RestController
 * @RequestMapping("/api/guest")
 * public class ApiGuestController extends BaseGuestController {
 *     
 *     public ApiGuestController(AuthorizationService authService, 
 *                               ValidationService validationService,
 *                               UserService userService, 
 *                               SecurityProperties securityProperties) {
 *         super(authService, validationService, userService, securityProperties);
 *     }
 *     
 *     @PostMapping("/register")
 *     public Mono<ResponseEntity<?>> registerUser(@RequestBody RegisterDTO registerDTO, 
 *                                                  ServerWebExchange exchange) {
 *         return super.register(registerDTO, exchange);
 *     }
 * }
 * }</pre>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see ResponseUnity
 * @see AuthorizationService
 * @see UserService
 * @see ValidationService
 * @see SecurityProperties
 * @see xyz.dowob.filemanagement.annotation.RecordLevel
 */
@RecordLevel(LogLevelEnum.INFO)
public abstract class BaseGuestController implements ResponseUnity {
    /**
     * 授權業務層服務，負責處理系統內使用者認證和授權的核心業務邏輯。
     *
     * <p>此服務提供完整的認證授權功能，包括：</p>
     * <ul>
     *   <li>使用者身份驗證和權限檢查</li>
     *   <li>JWT 令牌的生成、驗證和管理</li>
     *   <li>角色型權限控制（RBAC）的實現</li>
     *   <li>會話狀態管理和安全性檢查</li>
     * </ul>
     *
     * <p>在訪客控制器中主要用於登入後的認證狀態確認和權限驗證。</p>
     *
     * @see AuthorizationService
     */
    protected final AuthorizationService authorizationService;

    /**
     * 驗證業務層服務，提供全方位的資料格式驗證和業務規則檢查機制。
     *
     * <p>此服務負責確保系統輸入資料的正確性和安全性，覆蓋以下驗證領域：</p>
     * <ul>
     *   <li><strong>格式驗證</strong>：郵件格式、密碼強度、使用者名稱規範等</li>
     *   <li><strong>業務規則驗證</strong>：重複性檢查、資料完整性約束等</li>
     *   <li><strong>安全性驗證</strong>：防止 SQL 注入、XSS 攻擊等安全性檢查</li>
     *   <li><strong>DTO 物件驗證</strong>：完整的資料傳輸物件格式和內容驗證</li>
     * </ul>
     *
     * <p>在訪客控制器中用於註冊、登入、密碼重置等關鍵流程的資料驗證。</p>
     *
     * @see ValidationService
     */
    protected final ValidationService validationService;

    /**
     * 使用者業務層服務，實現檔案管理系統中使用者生命週期的完整業務邏輯。
     *
     * <p>此服務是使用者管理的核心元件，提供從註冊到日常操作的完整功能支援：</p>
     * <ul>
     *   <li><strong>帳戶管理</strong>：使用者註冊、資料更新、帳戶狀態管理</li>
     *   <li><strong>認證服務</strong>：登入驗證、JWT 令牌生成、會話管理</li>
     *   <li><strong>密碼服務</strong>：密碼加密、重置流程、安全性檢查</li>
     *   <li><strong>郵件服務</strong>：註冊確認、密碼重置、系統通知郵件發送</li>
     *   <li><strong>使用者資訊查詢</strong>：基本資料檢索、權限資訊獲取</li>
     * </ul>
     *
     * <p>整合資料持久層和快取層，提供高效能的使用者資料存取能力。</p>
     *
     * @see UserService
     */
    protected final UserService userService;

    /**
     * 系統安全設定屬性配置，集中管理認證授權機制的關鍵參數。
     *
     * <p>此配置物件包含檔案管理系統安全性相關的所有重要設定，確保系統安全性的
     * 一致性和可配置性：</p>
     * <ul>
     *   <li><strong>JWT 設定</strong>：令牌過期時間、簽名金鑰、演算法配置</li>
     *   <li><strong>Cookie 安全</strong>：HttpOnly、Secure、SameSite 等安全屬性</li>
     *   <li><strong>CSRF 保護</strong>：跨站請求偽造防護機制配置</li>
     *   <li><strong>會話管理</strong>：會話過期時間、並行會話限制</li>
     *   <li><strong>密碼策略</strong>：密碼複雜度要求、加密演算法設定</li>
     * </ul>
     *
     * <p>在訪客控制器中主要用於登入時的 Cookie 設定和安全性參數應用。</p>
     *
     * @see SecurityProperties
     */
    protected final SecurityProperties securityProperties;

    /**
     * 建構訪客控制器基礎實例，初始化所有必要的業務服務和配置元件。
     *
     * <p>此建構函式透過依賴注入模式初始化訪客控制器運作所需的所有核心服務，
     * 確保子類別能夠直接使用完整的業務處理能力。</p>
     *
     * <p><strong>初始化元件包括：</strong></p>
     * <ul>
     *   <li>授權服務：提供認證和權限管理功能</li>
     *   <li>驗證服務：提供資料格式和業務規則驗證</li>
     *   <li>使用者服務：提供使用者生命週期管理功能</li>
     *   <li>安全配置：提供系統安全性參數和策略</li>
     * </ul>
     *
     * <p><strong>設計模式說明：</strong></p>
     * <p>採用建構函式注入確保所有依賴關係在物件建立時完成初始化，
     * 遵循不可變性原則，提高系統的穩定性和可測試性。</p>
     *
     * @param authorizationService 授權業務層服務實例，提供認證授權功能
     * @param validationService 驗證業務層服務實例，提供資料驗證功能
     * @param userService 使用者業務層服務實例，提供使用者管理功能
     * @param securityProperties 系統安全設定屬性實例，提供安全性配置
     *
     * @see AuthorizationService
     * @see ValidationService
     * @see UserService
     * @see SecurityProperties
     */
    protected BaseGuestController(AuthorizationService authorizationService, ValidationService validationService, UserService userService, SecurityProperties securityProperties) {
        this.authorizationService = authorizationService;
        this.validationService = validationService;
        this.userService = userService;
        this.securityProperties = securityProperties;
    }


    /**
     * 處理訪客使用者註冊請求，實現新使用者帳戶的完整建立流程。
     *
     * <p>此方法提供安全、完整的使用者註冊處理機制，包含多層次的資料驗證、
     * 業務規則檢查和帳戶建立流程。整個過程採用反應式程式設計，確保高併發環境下的穩定性。</p>
     *
     * <h3>處理流程：</h3>
     * <ol>
     *   <li><strong>資料驗證</strong>：透過 {@link ValidationService} 驗證註冊資料格式和業務規則</li>
     *   <li><strong>重複性檢查</strong>：檢查使用者名稱和郵件地址的唯一性</li>
     *   <li><strong>帳戶建立</strong>：透過 {@link UserService} 執行實際的使用者帳戶建立</li>
     *   <li><strong>回應生成</strong>：建立標準化的成功回應，狀態碼 201 Created</li>
     * </ol>
     *
     * <h3>安全性特性：</h3>
     * <ul>
     *   <li><strong>密碼加密</strong>：使用者密碼經過安全雜湊處理後儲存</li>
     *   <li><strong>輸入驗證</strong>：防止惡意輸入和注入攻擊</li>
     *   <li><strong>業務規則檢查</strong>：確保註冊資料符合系統要求</li>
     *   <li><strong>異常處理</strong>：統一的錯誤處理機制，不洩露敏感資訊</li>
     * </ul>
     *
     * <h3>回應格式：</h3>
     * <p><strong>成功回應（201 Created）：</strong></p>
     * <pre>{@code
     * {
     *   "status": 201,
     *   "message": "註冊成功",
     *   "data": {},
     *   "timestamp": "2024-01-01T12:00:00Z"
     * }
     * }</pre>
     *
     * <p><strong>錯誤回應範例：</strong></p>
     * <ul>
     *   <li><strong>400 Bad Request</strong>：資料格式錯誤或業務規則不符</li>
     *   <li><strong>409 Conflict</strong>：使用者名稱或郵件地址已存在</li>
     *   <li><strong>500 Internal Server Error</strong>：系統內部錯誤</li>
     * </ul>
     *
     * @param registerUserDTO 註冊使用者的資料傳輸物件，包含使用者名稱、密碼、郵件等必要資訊
     * @param exchange WebFlux 伺服器請求交換物件，包含請求內容和回應設定
     *
     * @return Mono<ResponseEntity<?>> 包含註冊結果的反應式回應物件
     *
     * @see RegisterDTO
     * @see ValidationService#validateRegisterDTO(RegisterDTO)
     * @see UserService#register(RegisterDTO)
     * @see ResponseUnity#handleError(Mono, ServerWebExchange)
     */
    public Mono<ResponseEntity<?>> register(RegisterDTO registerUserDTO, ServerWebExchange exchange) {
        return handleError(validationService.validateRegisterDTO(registerUserDTO).then(userService.register(registerUserDTO).then(Mono.defer(() -> {
                               HashMap<String, Object> data = new HashMap<>();
                               ApiResponseDTO<?> apiResponse = createApiResponse(exchange, 201, "註冊成功", data);
                               return createResponseEntity(apiResponse, 201);
                           }))), exchange
        );
    }


    /**
     * 處理訪客使用者登入請求，實現多環境適配的身份認證機制。
     *
     * <p>此方法提供靈活且安全的使用者登入處理，支援不同環境下的認證模式。
     * 系統會根據環境類型自動選擇適當的認證令牌傳遞方式，確保最佳的安全性和使用體驗。</p>
     *
     * <h3>登入處理流程：</h3>
     * <ol>
     *   <li><strong>憑證驗證</strong>：透過 {@link ValidationService} 驗證登入資料格式</li>
     *   <li><strong>身份認證</strong>：透過 {@link UserService} 驗證使用者憑證的正確性</li>
     *   <li><strong>令牌生成</strong>：成功認證後生成 JWT 存取令牌</li>
     *   <li><strong>回應處理</strong>：根據環境類型設定適當的令牌傳遞方式</li>
     * </ol>
     *
     * <h3>多環境支援：</h3>
     * <ul>
     *   <li><strong>Web 環境（isWeb=true）</strong>：
     *     <ul>
     *       <li>JWT 令牌儲存為 HttpOnly Cookie</li>
     *       <li>套用 Secure、SameSite 等安全屬性</li>
     *       <li>自動設定 Cookie 過期時間</li>
     *       <li>支援 CSRF 保護機制</li>
     *     </ul>
     *   </li>
     *   <li><strong>API 環境（isWeb=false）</strong>：
     *     <ul>
     *       <li>JWT 令牌在回應 body 中返回</li>
     *       <li>適合行動應用程式和第三方整合</li>
     *       <li>支援 Bearer Token 認證模式</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * <h3>安全性特性：</h3>
     * <ul>
     *   <li><strong>密碼保護</strong>：使用安全雜湊演算法驗證密碼</li>
     *   <li><strong>令牌安全</strong>：JWT 令牌包含過期時間和數位簽章</li>
     *   <li><strong>Cookie 安全</strong>：Web 環境下套用完整的 Cookie 安全策略</li>
     *   <li><strong>會話管理</strong>：支援會話追蹤和並行登入控制</li>
     * </ul>
     *
     * <h3>回應格式：</h3>
     * <p><strong>成功回應（200 OK）：</strong></p>
     * <pre>{@code
     * {
     *   "status": 200,
     *   "message": "登入成功",
     *   "data": {
     *     "jwtToken": "eyJhbGciOiJIUzI1NiIs..."
     *   },
     *   "timestamp": "2024-01-01T12:00:00Z"
     * }
     * }</pre>
     *
     * <p><strong>錯誤回應範例：</strong></p>
     * <ul>
     *   <li><strong>400 Bad Request</strong>：登入資料格式錯誤</li>
     *   <li><strong>401 Unauthorized</strong>：使用者名稱或密碼錯誤</li>
     *   <li><strong>423 Locked</strong>：帳戶被鎖定或暫停</li>
     * </ul>
     *
     * @param authRequestDTO 登入認證的資料傳輸物件，包含使用者名稱和密碼
     * @param exchange WebFlux 伺服器請求交換物件，包含請求內容和回應設定
     * @param isWeb 環境類型標識，{@code true} 表示 Web 環境，{@code false} 表示 API 環境
     *
     * @return Mono<ResponseEntity<?>> 包含登入結果和認證令牌的反應式回應物件
     *
     * @see AuthRequestDTO
     * @see AuthResponseDTO
     * @see ValidationService#validateAuthRequestDTO(AuthRequestDTO)
     * @see UserService#login(AuthRequestDTO, ServerWebExchange)
     * @see SecurityProperties.Cookie
     * @see SecurityProperties.JwtToken
     */
    public Mono<ResponseEntity<?>> login(AuthRequestDTO authRequestDTO, ServerWebExchange exchange, boolean isWeb) {
        Mono<ResponseEntity<?>> action = validationService
                .validateAuthRequestDTO(authRequestDTO)
                .then(userService.login(authRequestDTO, exchange).flatMap(token -> {
                    if (isWeb) {
                        ResponseCookie cookie = ResponseCookie
                                .from(securityProperties.getCookie().getTokenName(), token)
                                .httpOnly(securityProperties.getCookie().isHttpOnly())
                                .secure(securityProperties.getCookie().isSecure())
                                .maxAge(securityProperties.getJwtToken().getExpiration().toSeconds())
                                .sameSite(securityProperties.getCookie().getSameSite())
                                .path("/")
                                .build();
                        exchange.getResponse().addCookie(cookie);
                    }
                    AuthResponseDTO authResponse = new AuthResponseDTO();
                    authResponse.setJwtToken(token);
                    ApiResponseDTO<?> apiResponse = createApiResponse(exchange, "登入成功", authResponse);
                    return createResponseEntity(apiResponse);
                }));
        return handleError(action, exchange);
    }


    /**
     * 檢查當前使用者的認證授權狀態，並返回詳細的使用者資訊。
     *
     * <p>此方法用於驗證當前請求是否包含有效的認證資訊，並在成功驗證後返回
     * 使用者的基本資料。主要用於前端應用程式的認證狀態確認和使用者資訊顯示。</p>
     *
     * <h3>驗證處理流程：</h3>
     * <ol>
     *   <li><strong>令牌提取</strong>：從請求標頭或 Cookie 中提取 JWT 令牌</li>
     *   <li><strong>令牌驗證</strong>：驗證 JWT 令牌的有效性和完整性</li>
     *   <li><strong>使用者查詢</strong>：根據令牌資訊查詢使用者詳細資料</li>
     *   <li><strong>資訊組裝</strong>：組裝並返回標準化的使用者資訊回應</li>
     * </ol>
     *
     * <h3>返回的使用者資訊：</h3>
     * <ul>
     *   <li><strong>userId</strong>：使用者唯一識別碼</li>
     *   <li><strong>userName</strong>：使用者名稱</li>
     *   <li><strong>userMail</strong>：使用者郵件地址</li>
     *   <li><strong>userRole</strong>：使用者角色權限</li>
     *   <li><strong>isAuthenticated</strong>：認證狀態標識</li>
     * </ul>
     *
     * <h3>使用場景：</h3>
     * <ul>
     *   <li><strong>頁面初始化</strong>：前端應用程式載入時的認證狀態檢查</li>
     *   <li><strong>會話驗證</strong>：定期檢查使用者會話的有效性</li>
     *   <li><strong>權限確認</strong>：在執行需要認證的操作前進行確認</li>
     *   <li><strong>使用者資訊顯示</strong>：獲取當前登入使用者的基本資料</li>
     * </ul>
     *
     * <h3>回應格式：</h3>
     * <p><strong>認證成功（200 OK）：</strong></p>
     * <pre>{@code
     * {
     *   "status": 200,
     *   "message": "用戶已授權",
     *   "data": {
     *     "user": {
     *       "userId": 12345,
     *       "userName": "testuser",
     *       "userMail": "test@example.com",
     *       "userRole": "USER"
     *     },
     *     "isAuthenticated": true
     *   },
     *   "timestamp": "2024-01-01T12:00:00Z"
     * }
     * }</pre>
     *
     * <p><strong>認證失敗（401 Unauthorized）：</strong></p>
     * <pre>{@code
     * {
     *   "status": 401,
     *   "message": "用戶未授權",
     *   "data": null,
     *   "timestamp": "2024-01-01T12:00:00Z"
     * }
     * }</pre>
     *
     * @param exchange WebFlux 伺服器請求交換物件，包含認證資訊和請求內容
     *
     * @return Mono<ResponseEntity<?>> 包含使用者認證狀態和資訊的反應式回應物件
     *
     * @see UserService#getUser(ServerWebExchange)
     * @see ValidationException.ErrorCode#UNAUTHORIZED
     */
    public Mono<ResponseEntity<?>> checkAuthenticationStatus(ServerWebExchange exchange) {
        return userService
                .getUser(exchange)
                .flatMap(user -> {
                    HashMap<String, Object> data = new HashMap<>();
                    HashMap<String, Object> userMap = new HashMap<>();
                    userMap.put("userId", user.getId());
                    userMap.put("userName", user.getUsername());
                    userMap.put("userMail", user.getEmail());
                    userMap.put("userRole", user.getRole());
                    data.put("user", userMap);
                    data.put("isAuthenticated", true);
                    ApiResponseDTO<?> apiResponse = createApiResponse(exchange, "用戶已授權", data);
                    return createResponseEntity(apiResponse);
                })
                .switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.UNAUTHORIZED)))
                .onErrorResume(ValidationException.class, e -> {
                                   ApiResponseDTO<?> apiResponse = createApiResponse(exchange, 401, "用戶未授權", null);
                                   return createResponseEntity(apiResponse);
                               }
                );
    }


    /**
     * 處理密碼重置郵件發送請求，啟動安全的密碼重置流程。
     *
     * <p>此方法實現安全且使用者友善的密碼重置機制，透過郵件驗證確保
     * 只有合法的帳戶擁有者才能進行密碼重置操作。整個流程符合現代安全最佳實踐。</p>
     *
     * <h3>密碼重置流程：</h3>
     * <ol>
     *   <li><strong>郵件驗證</strong>：驗證提供的郵件地址格式和有效性</li>
     *   <li><strong>帳戶檢查</strong>：確認該郵件地址對應的使用者帳戶存在</li>
     *   <li><strong>令牌生成</strong>：產生安全的密碼重置令牌，包含時效性限制</li>
     *   <li><strong>郵件發送</strong>：向使用者郵箱發送包含重置連結的通知郵件</li>
     *   <li><strong>回應確認</strong>：返回郵件發送成功的確認訊息</li>
     * </ol>
     *
     * <h3>安全性特性：</h3>
     * <ul>
     *   <li><strong>令牌時效性</strong>：重置令牌具有有限的有效期間（通常 15-30 分鐘）</li>
     *   <li><strong>一次性使用</strong>：每個重置令牌只能使用一次</li>
     *   <li><strong>資訊保護</strong>：不會洩露帳戶是否存在的資訊</li>
     *   <li><strong>郵件安全</strong>：重置連結包含加密的驗證資訊</li>
     * </ul>
     *
     * <h3>郵件內容特性：</h3>
     * <ul>
     *   <li><strong>專業格式</strong>：使用系統設計的郵件範本</li>
     *   <li><strong>安全連結</strong>：包含唯一的重置令牌和驗證資訊</li>
     *   <li><strong>使用說明</strong>：清楚的操作指引和注意事項</li>
     *   <li><strong>有效期限</strong>：明確標示連結的有效時間</li>
     * </ul>
     *
     * <h3>回應行為：</h3>
     * <p><strong>成功回應（200 OK）：</strong></p>
     * <p>無論帳戶是否存在，系統都會返回相同的成功訊息，
     * 這是為了防止帳戶枚舉攻擊，保護使用者隱私。</p>
     *
     * <pre>{@code
     * {
     *   "status": 200,
     *   "message": "重置密碼郵件已發送，請到信箱查收驗證信",
     *   "data": null,
     *   "timestamp": "2024-01-01T12:00:00Z"
     * }
     * }</pre>
     *
     * <p><strong>錯誤回應範例：</strong></p>
     * <ul>
     *   <li><strong>400 Bad Request</strong>：郵件地址格式錯誤</li>
     *   <li><strong>429 Too Many Requests</strong>：請求過於頻繁</li>
     *   <li><strong>500 Internal Server Error</strong>：郵件服務暫時不可用</li>
     * </ul>
     *
     * @param userEmailDTO 包含使用者郵件地址的資料傳輸物件
     * @param exchange WebFlux 伺服器請求交換物件，包含請求內容和回應設定
     *
     * @return Mono<ResponseEntity<?>> 包含郵件發送結果的反應式回應物件
     *
     * @see UserEmailDTO
     * @see ValidationService#validateNotNull(Object)
     * @see UserService#sendResetPasswordMail(UserEmailDTO)
     * @see ResponseUnity#handleError(Mono, ServerWebExchange)
     */
    public Mono<ResponseEntity<?>> sendResetPasswordMail(UserEmailDTO userEmailDTO, ServerWebExchange exchange) {
        return handleError(validationService
                                   .validateNotNull(userEmailDTO)
                                   .then(userService.sendResetPasswordMail(userEmailDTO).then(Mono.defer(() -> {
                                       ApiResponseDTO<?> apiResponse = createApiResponse(exchange, "重置密碼郵件已發送，請到信箱查收驗證信", null);
                                       return createResponseEntity(apiResponse);
                                   }))), exchange
        );
    }


    /**
     * 處理密碼重置執行請求，完成使用者密碼的安全更新操作。
     *
     * <p>此方法是密碼重置流程的最終步驟，負責驗證重置令牌的有效性並執行
     * 實際的密碼更新操作。整個過程採用多重安全驗證，確保只有合法使用者
     * 才能成功重置密碼。</p>
     *
     * <h3>密碼重置執行流程：</h3>
     * <ol>
     *   <li><strong>資料驗證</strong>：驗證重置請求資料的完整性和格式正確性</li>
     *   <li><strong>令牌驗證</strong>：檢查重置令牌的有效性、時效性和真實性</li>
     *   <li><strong>密碼驗證</strong>：確認新密碼符合系統安全要求</li>
     *   <li><strong>密碼更新</strong>：執行安全的密碼雜湊和資料庫更新</li>
     *   <li><strong>會話清理</strong>：清除相關的重置令牌和舊會話資訊</li>
     * </ol>
     *
     * <h3>安全性驗證：</h3>
     * <ul>
     *   <li><strong>令牌驗證</strong>：
     *     <ul>
     *       <li>檢查令牌是否為系統簽發</li>
     *       <li>驗證令牌是否在有效期限內</li>
     *       <li>確認令牌是否已被使用過</li>
     *       <li>驗證令牌與使用者的關聯性</li>
     *     </ul>
     *   </li>
     *   <li><strong>密碼安全</strong>：
     *     <ul>
     *       <li>密碼複雜度要求檢查</li>
     *       <li>與舊密碼的差異性驗證</li>
     *       <li>使用安全雜湊演算法加密</li>
     *       <li>密碼歷史記錄檢查（如果啟用）</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * <h3>重置成功後的操作：</h3>
     * <ul>
     *   <li><strong>令牌失效</strong>：使所有相關的重置令牌立即失效</li>
     *   <li><strong>會話清除</strong>：清除使用者的所有現有登入會話</li>
     *   <li><strong>安全日誌</strong>：記錄密碼重置操作的安全日誌</li>
     *   <li><strong>通知郵件</strong>：可選發送密碼重置成功的通知郵件</li>
     * </ul>
     *
     * <h3>回應格式：</h3>
     * <p><strong>重置成功（200 OK）：</strong></p>
     * <pre>{@code
     * {
     *   "status": 200,
     *   "message": "密碼重置成功",
     *   "data": null,
     *   "timestamp": "2024-01-01T12:00:00Z"
     * }
     * }</pre>
     *
     * <p><strong>錯誤回應範例：</strong></p>
     * <ul>
     *   <li><strong>400 Bad Request</strong>：重置資料格式錯誤或密碼不符合要求</li>
     *   <li><strong>401 Unauthorized</strong>：重置令牌無效或已過期</li>
     *   <li><strong>404 Not Found</strong>：重置令牌對應的使用者不存在</li>
     *   <li><strong>410 Gone</strong>：重置令牌已被使用過</li>
     * </ul>
     *
     * @param resetPasswordDTO 包含重置令牌和新密碼的資料傳輸物件
     * @param exchange WebFlux 伺服器請求交換物件，包含請求內容和回應設定
     *
     * @return Mono<ResponseEntity<?>> 包含密碼重置結果的反應式回應物件
     *
     * @see ResetPasswordDTO
     * @see ValidationService#validateResetPasswordDTO(ResetPasswordDTO)
     * @see UserService#resetPassword(ResetPasswordDTO)
     * @see ResponseUnity#handleError(Mono, ServerWebExchange)
     */
    public Mono<ResponseEntity<?>> resetPassword(ResetPasswordDTO resetPasswordDTO, ServerWebExchange exchange) {
        return handleError(validationService
                                   .validateResetPasswordDTO(resetPasswordDTO)
                                   .then(userService.resetPassword(resetPasswordDTO).then(Mono.defer(() -> {
                                       ApiResponseDTO<?> apiResponse = createApiResponse(exchange, "密碼重置成功", null);
                                       return createResponseEntity(apiResponse);
                                   }))), exchange
        );
    }
}

