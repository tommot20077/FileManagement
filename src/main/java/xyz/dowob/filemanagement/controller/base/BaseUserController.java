package xyz.dowob.filemanagement.controller.base;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.RecordLevel;
import xyz.dowob.filemanagement.annotation.RequirePermission;
import xyz.dowob.filemanagement.component.strategy.FileServiceStrategy;
import xyz.dowob.filemanagement.config.properties.SecurityProperties;
import xyz.dowob.filemanagement.customenum.LogLevelEnum;
import xyz.dowob.filemanagement.customenum.PermissionEnum;
import xyz.dowob.filemanagement.customenum.UserInfoTypeEnum;
import xyz.dowob.filemanagement.data.response.ApiResponseDTO;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.service.serviceInterface.UserService;
import xyz.dowob.filemanagement.service.serviceInterface.ValidationService;
import xyz.dowob.filemanagement.unity.ResponseUnity;

import java.util.*;
import java.util.function.Function;

/**
 * 基於 Spring WebFlux 反應式程式設計的已認證使用者控制器抽象基類，提供完整的使用者管理功能。
 *
 * <p>此抽象基類專門處理已通過身份認證的使用者相關操作，提供從基本的帳戶管理
 * 到進階的管理功能的完整解決方案。整合基於註解的權限管理系統，確保所有操作的安全性。</p>
 *
 * <h3>核心功能領域：</h3>
 * <ul>
 *   <li><strong>帳戶管理</strong>：使用者登出、資訊查詢、帳戶狀態管理</li>
 *   <li><strong>用戶搜尋</strong>：支援多種搜尋模式的用戶資訊查詢功能</li>
 *   <li><strong>管理功能</strong>：管理員專用的全系統用戶管理操作</li>
 *   <li><strong>權限控制</strong>：基於註解的細粒度權限驗證機制</li>
 * </ul>
 *
 * <h3>技術架構特色：</h3>
 * <ul>
 *   <li><strong>反應式設計</strong>：全程非阻塞處理，支援高併發場景</li>
 *   <li><strong>註解驅動</strong>：透過 {@link RequirePermission} 實現方法級別權限控制</li>
 *   <li><strong>多環境適配</strong>：支援 Web 和 API 環境的不同處理模式</li>
 *   <li><strong>安全性首考</strong>：整合安全性配置，支援安全 Cookie 和會話管理</li>
 * </ul>
 *
 * <h3>權限管理系統：</h3>
 * <ul>
 *   <li><strong>角色型權限</strong>：支援基於角色的權限控制機制</li>
 *   <li><strong>方法級別保護</strong>：透過註解實現細粒度的操作權限</li>
 *   <li><strong>動態權限檢查</strong>：在運行時根據用戶狀態驗證權限</li>
 *   <li><strong>安全日誌</strong>：所有敏感操作自動記錄安全日誌</li>
 * </ul>
 *
 * <h3>使用者管理功能：</h3>
 * <ul>
 *   <li><strong>身份管理</strong>：使用者登出、會話管理、認證狀態管理</li>
 *   <li><strong>資訊查詢</strong>：使用者個人資訊、公開資料查詢</li>
 *   <li><strong>用戶搜尋</strong>：支援按名稱、ID 等多種搜尋模式</li>
 *   <li><strong>管理操作</strong>：管理員用戶列表、系統監控等功能</li>
 * </ul>
 *
 * <h3>安全性特性：</h3>
 * <ul>
 *   <li><strong>認證要求</strong>：所有方法都要求用戶已通過認證</li>
 *   <li><strong>權限驗證</strong>：敏感操作要求額外的權限檢查</li>
 *   <li><strong>資料保護</strong>：不會洩露敏感的用戶資訊</li>
 *   <li><strong>會話管理</strong>：安全的登出機制和會話失效處理</li>
 * </ul>
 *
 * <h3>使用範例：</h3>
 * <pre>{@code
 * @RestController
 * @RequestMapping("/api/user")
 * public class ApiUserController extends BaseUserController {
 *     
 *     @PostMapping("/logout")
 *     public Mono<ResponseEntity<?>> logoutUser(ServerWebExchange exchange) {
 *         return super.logout(exchange, false); // API 模式
 *     }
 *     
 *     @GetMapping("/profile")
 *     public Mono<ResponseEntity<?>> getProfile(ServerWebExchange exchange) {
 *         return super.getUserInfo(exchange);
 *     }
 * }
 * }</pre>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see ResponseUnity
 * @see RequirePermission
 * @see PermissionEnum
 * @see UserService
 * @see ValidationService
 */
@RequiredArgsConstructor
@RecordLevel(LogLevelEnum.INFO)
public abstract class BaseUserController implements ResponseUnity {

    /**
     * 檔案服務策略管理器，提供檔案操作相關的策略選擇和管理能力。
     *
     * <p>在使用者控制器中主要用於處理與使用者相關的檔案操作，
     * 如使用者頭像上傳、個人檔案管理等功能。</p>
     *
     * @see FileServiceStrategy
     */
    protected final FileServiceStrategy fileServiceStrategy;

    /**
     * 使用者業務層服務，在使用者控制器中提供核心的使用者管理功能。
     *
     * <p>在已認證用戶情境下，此服務主要用於：</p>
     * <ul>
     *   <li><strong>用戶認證管理</strong>：當前用戶取得、登出處理等</li>
     *   <li><strong>用戶資訊查詢</strong>：個人資訊、公開資料查詢</li>
     *   <li><strong>用戶搜尋</strong>：多模式用戶搜尋和結果過濾</li>
     *   <li><strong>管理功能</strong>：管理員用戶列表查詢和管理</li>
     * </ul>
     *
     * @see UserService
     */
    protected final UserService userService;

    /**
     * 系統安全性配置屬性，提供認證授權相關的關鍵安全參數。
     *
     * <p>在使用者控制器中主要用於：</p>
     * <ul>
     *   <li><strong>Cookie 管理</strong>：登出時的 Cookie 清理和安全屬性設定</li>
     *   <li><strong>會話管理</strong>：會話的安全策略和過期控制</li>
     *   <li><strong>認證機制</strong>：身份驗證的安全參數和策略</li>
     * </ul>
     *
     * @see SecurityProperties
     */
    protected final SecurityProperties securityProperties;

    /**
     * 資料驗證業務層服務，為使用者控制器提供全方位的輸入驗證能力。
     *
     * <p>在使用者控制器中主要用於：</p>
     * <ul>
     *   <li><strong>輸入驗證</strong>：用戶請求參數的格式和內容檢查</li>
     *   <li><strong>業務規則</strong>：用戶搜尋条件、查詢參數的合理性檢查</li>
     *   <li><strong>安全性檢查</strong>：防止惡意輸入和注入攻擊</li>
     * </ul>
     *
     * @see ValidationService
     */
    protected final ValidationService validationService;

    /**
     * 處理使用者登出請求，實現安全的會話終止和資源清理功能。
     *
     * <p>此方法實現完整的用戶登出流程，包含伺服端會話終止和客戶端資源清理。
     * 支援多環境適配，根據請求來源選擇適當的登出處理方式。</p>
     *
     * <h3>登出處理流程：</h3>
     * <ol>
     *   <li><strong>用戶認證</strong>：驗證當前請求的用戶認證狀態</li>
     *   <li><strong>會話終止</strong>：透過 UserService 終止伺服端的用戶會話</li>
     *   <li><strong>客戶端清理</strong>：清除客戶端的認證令牌和 Cookie</li>
     *   <li><strong>回應生成</strong>：產生適當的登出確認回應</li>
     * </ol>
     *
     * <h3>多環境支援：</h3>
     * <ul>
     *   <li><strong>Web 環境（isWeb=true）</strong>：
     *     <ul>
     *       <li>清除瀏覽器中的 HttpOnly Cookie</li>
     *       <li>設定 Cookie 的 maxAge 為 0，強制過期</li>
     *       <li>保持原有的安全屬性（Secure、SameSite）</li>
     *     </ul>
     *   </li>
     *   <li><strong>API 環境（isWeb=false）</strong>：
     *     <ul>
     *       <li>僅處理伺服端會話終止</li>
     *       <li>客戶端需自行清除儲存的 JWT 令牌</li>
     *       <li>適合由前端應用程式和第三方系統使用</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * <h3>安全性特性：</h3>
     * <ul>
     *   <li><strong>會話安全</strong>：強制終止伺服端會話，防止會話劫持</li>
     *   <li><strong>Cookie 安全</strong>：Web 環境下完整清除認證 Cookie</li>
     *   <li><strong>資源清理</strong>：清理所有與用戶會話相關的伺服端資源</li>
     *   <li><strong>審計日誌</strong>：記錄登出操作的完整日誌資訊</li>
     * </ul>
     *
     * <h3>回應行為：</h3>
     * <ul>
     *   <li><strong>認證用戶</strong>：返回登出成功确認訊息</li>
     *   <li><strong>未認證狀態</strong>：返回 401 狀態碼和異常訊息</li>
     * </ul>
     *
     * <h3>回應格式：</h3>
     * <p><strong>登出成功（200 OK）：</strong></p>
     * <pre>{@code
     * {
     *   "status": 200,
     *   "message": "登出成功",
     *   "data": null,
     *   "timestamp": "2024-01-01T12:00:00Z"
     * }
     * }</pre>
     *
     * <p><strong>未認證（401 Unauthorized）：</strong></p>
     * <pre>{@code
     * {
     *   "status": 401,
     *   "message": "未認證",
     *   "data": null,
     *   "timestamp": "2024-01-01T12:00:00Z"
     * }
     * }</pre>
     *
     * @param exchange WebFlux 伺服器請求交換物件，包含用戶資訊和請求內容
     * @param isWeb 環境類型標識，{@code true} 表示 Web 環境，{@code false} 表示 API 環境
     *
     * @return Mono<ResponseEntity<?>> 包含登出結果的反應式回應物件
     *
     * @see UserService#getUser(ServerWebExchange)
     * @see UserService#logout(Long, ServerWebExchange)
     * @see SecurityProperties.Cookie
     */
    public Mono<ResponseEntity<?>> logout(ServerWebExchange exchange, boolean isWeb) {
        return userService.getUser(exchange).flatMap(user -> userService.logout(user.getId(), exchange).then(Mono.defer(() -> {
            if (isWeb) {
                ResponseCookie cookie = ResponseCookie
                        .from(securityProperties.getCookie().getTokenName(), "")
                        .httpOnly(securityProperties.getCookie().isHttpOnly())
                        .secure(securityProperties.getCookie().isSecure())
                        .maxAge(0)
                        .sameSite(securityProperties.getCookie().getSameSite())
                        .path("/")
                        .build();
                exchange.getResponse().addCookie(cookie);
            }
            return createResponseEntity(createApiResponse(exchange, "登出成功", null));
        }))).switchIfEmpty(createResponseEntity(createApiResponse(exchange, 401, "未認證", null)));
    }


    /**
     * 處理管理員的全系統用戶資訊查詢請求，提供完整的用戶列表。
     *
     * <p>此方法提供管理員級別的系統管理功能，可以查詢系統中所有用戶的詳細資訊。
     * 這是一個高權限操作，需要管理員權限且會被系統記錄在高級別的安全日誌中。</p>
     *
     * <h3>權限要求：</h3>
     * <ul>
     *   <li><strong>管理員權限</strong>：要求 {@link PermissionEnum#MANAGE} 權限</li>
     *   <li><strong>認證要求</strong>：必須為已認證且具有有效會話的管理員</li>
     *   <li><strong>日誌級別</strong>：此操作會被記錄為警告級別日誌</li>
     * </ul>
     *
     * <h3>用戶資訊內容：</h3>
     * <p>返回的用戶列表包含以下資訊：</p>
     * <ul>
     *   <li>用戶 ID 和用戶名稱</li>
     *   <li>郵件地址和認證狀態</li>
     *   <li>用戶角色和權限資訊</li>
     *   <li>帳戶建立時間和最後登入時間</li>
     *   <li>帳戶狀態（啟用/停用/鎖定）</li>
     * </ul>
     *
     * <h3>安全性考量：</h3>
     * <ul>
     *   <li><strong>數據過濾</strong>：不會返回敏感的用戶資訊（如密碼雜湊）</li>
     *   <li><strong>存取記錄</strong>：所有查詢操作都會被記錄在安全日誌中</li>
     *   <li><strong>權限驗證</strong>：在方法級別進行安全檢查</li>
     * </ul>
     *
     * <h3>回應格式：</h3>
     * <p><strong>查詢成功（200 OK）：</strong></p>
     * <pre>{@code
     * {
     *   "status": 200,
     *   "message": "獲取用戶信息成功",
     *   "data": [
     *     {
     *       "id": 1,
     *       "username": "admin",
     *       "email": "admin@example.com",
     *       "role": "ADMIN",
     *       "isEnabled": true,
     *       "createdAt": "2024-01-01T00:00:00Z",
     *       "lastLogin": "2024-01-01T12:00:00Z"
     *     }
     *   ],
     *   "timestamp": "2024-01-01T12:00:00Z"
     * }
     * }</pre>
     *
     * @param exchange WebFlux 伺服器請求交換物件，包含管理員資訊和請求內容
     *
     * @return Mono<ResponseEntity<?>> 包含所有用戶資訊列表的反應式回應物件
     *
     * @see RequirePermission
     * @see PermissionEnum#MANAGE
     * @see RecordLevel
     * @see LogLevelEnum#WARN
     * @see UserService#getAll()
     */
    @RecordLevel(LogLevelEnum.WARN)
    @RequirePermission(PermissionEnum.MANAGE)
    public Mono<ResponseEntity<?>> getAllUserInfo(ServerWebExchange exchange) {
        return handleError(userService.getAll().collectList().flatMap(userList -> {
            ApiResponseDTO<?> responseEntity = createApiResponse(exchange, "獲取用户信息成功", userList);
            return createResponseEntity(responseEntity);
        }), exchange);
    }


    /**
     * 處理當前使用者的個人資訊查詢請求，返回詳細的用戶資料。
     *
     * <p>此方法提供已認證用戶查詢自身詳細資訊的功能，包含個人資料和帳戶狀態。
     * 這是一個基本的用戶服務功能，用於支援個人資料管理和資料介面顯示。</p>
     *
     * <h3>查詢權限：</h3>
     * <ul>
     *   <li><strong>自身資訊</strong>：用戶只能查詢自身的資訊</li>
     *   <li><strong>認證要求</strong>：必須為已認證且具有有效會話</li>
     *   <li><strong>安全策略</strong>：不會返回敏感的安全資訊</li>
     * </ul>
     *
     * <h3>返回資訊內容：</h3>
     * <ul>
     *   <li><strong>基本資訊</strong>：用戶 ID、用戶名稱、郵件地址</li>
     *   <li><strong>角色資訊</strong>：用戶角色和權限級別</li>
     *   <li><strong>帳戶狀態</strong>：啟用狀態、驗證狀態</li>
     *   <li><strong>時間資訊</strong>：帳戶建立時間、最後更新時間</li>
     * </ul>
     *
     * <h3>安全性特性：</h3>
     * <ul>
     *   <li><strong>資料過濾</strong>：不包含密碼雜湊、JWT 秘鑰等敏感資訊</li>
     *   <li><strong>身份驗證</strong>：仅在用戶通過身份驗證後才可存取</li>
     *   <li><strong>會話檢查</strong>：驗證當前會話的有效性</li>
     * </ul>
     *
     * <h3>使用場景：</h3>
     * <ul>
     *   <li><strong>個人資料頁面</strong>：顯示用戶的資料和設定</li>
     *   <li><strong>資料編輯表單</strong>：填入當前用戶資訊作為預設值</li>
     *   <li><strong>權限檢查</strong>：根據用戶角色控制介面功能</li>
     *   <li><strong>資料同步</strong>：與其他系統同步用戶資訊</li>
     * </ul>
     *
     * <h3>回應格式：</h3>
     * <p><strong>查詢成功（200 OK）：</strong></p>
     * <pre>{@code
     * {
     *   "status": 200,
     *   "message": "獲取用戶信息成功",
     *   "data": {
     *     "id": 12345,
     *     "username": "testuser",
     *     "email": "test@example.com",
     *     "role": "USER",
     *     "isEnabled": true,
     *     "isEmailVerified": true,
     *     "createdAt": "2024-01-01T00:00:00Z",
     *     "updatedAt": "2024-01-01T12:00:00Z"
     *   },
     *   "timestamp": "2024-01-01T12:00:00Z"
     * }
     * }</pre>
     *
     * @param exchange WebFlux 伺服器請求交換物件，包含用戶資訊和請求內容
     *
     * @return Mono<ResponseEntity<?>> 包含當前用戶詳細資訊的反應式回應物件
     *
     * @see UserService#getUser(ServerWebExchange)
     */
    public Mono<ResponseEntity<?>> getUserInfo(ServerWebExchange exchange) {
        return handleError(userService.getUser(exchange).flatMap(user -> {
            ApiResponseDTO<?> responseEntity = createApiResponse(exchange, "獲取用户信息成功", user);
            return createResponseEntity(responseEntity);
        }), exchange);
    }


    /**
     * 處理多模式用戶搜尋請求，支援按名稱或 ID 進行批量用戶查詢。
     *
     * <p>此方法提供靈活的用戶搜尋功能，支援多種搜尋模式和批量查詢。
     * 適用於用戶選擇器、協作者查找、聯絡人清單等場景，提供高效的用戶資訊查詢服務。</p>
     *
     * <h3>搜尋模式支援：</h3>
     * <ul>
     *   <li><strong>按名稱搜尋（type=NAME）</strong>：
     *     <ul>
     *       <li>輸入：用戶名稱集合</li>
     *       <li>輸出：用戶名稱 → 用戶 ID 的映射</li>
     *       <li>適用：已知用戶名稱，需要獲取 ID</li>
     *     </ul>
     *   </li>
     *   <li><strong>按 ID 搜尋（其他類型）</strong>：
     *     <ul>
     *       <li>輸入：用戶 ID 字串集合</li>
     *       <li>輸出：用戶 ID → 用戶名稱的映射</li>
     *       <li>適用：已知用戶 ID，需要獲取名稱</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * <h3>批量查詢特性：</h3>
     * <ul>
     *   <li><strong>高效性能</strong>：一次請求處理多個用戶查詢</li>
     *   <li><strong>結果分類</strong>：明確區分找到和未找到的用戶</li>
     *   <li><strong>部分失敗容忍</strong>：部分用戶不存在不影響其他結果</li>
     * </ul>
     *
     * <h3>輸入驗證：</h3>
     * <ul>
     *   <li><strong>數量限制</strong>：限制單次查詢的用戶數量防止濫用</li>
     *   <li><strong>格式檢查</strong>：驗證輸入字串的格式和內容</li>
     *   <li><strong>安全過濾</strong>：防止惡意輸入和注入攻擊</li>
     * </ul>
     *
     * <h3>回應結構：</h3>
     * <p>回應包含两部分資料：</p>
     * <ul>
     *   <li><strong>foundUser</strong>：成功找到的用戶映射</li>
     *   <li><strong>notFoundUser</strong>：未找到的用戶標識集合</li>
     * </ul>
     *
     * <h3>常見使用場景：</h3>
     * <ul>
     *   <li><strong>用戶選擇器</strong>：在介面中實現用戶選擇功能</li>
     *   <li><strong>協作者搜尋</strong>：搜尋可以協作編輯的用戶</li>
     *   <li><strong>聯絡人清單</strong>：查找聯絡人的詳細資訊</li>
     *   <li><strong>權限分配</strong>：為資源分配存取權限時搜尋用戶</li>
     * </ul>
     *
     * <h3>回應格式：</h3>
     * <p><strong>搜尋成功（200 OK）：</strong></p>
     * <pre>{@code
     * {
     *   "status": 200,
     *   "message": "獲取用戶信息成功",
     *   "data": {
     *     "foundUser": {
     *       "testuser1": "12345",
     *       "testuser2": "12346"
     *     },
     *     "notFoundUser": ["nonexistent"]
     *   },
     *   "timestamp": "2024-01-01T12:00:00Z"
     * }
     * }</pre>
     *
     * @param exchange WebFlux 伺服器請求交換物件，包含請求內容和用戶資訊
     * @param userInfos 要搜尋的用戶標識集合（用戶名稱或 ID）
     * @param type 搜尋類型，{@link UserInfoTypeEnum#NAME} 或其他類型
     *
     * @return Mono<ResponseEntity<?>> 包含搜尋結果的反應式回應物件
     *
     * @see UserInfoTypeEnum
     * @see ValidationService#validateUserSearchList(Collection) 
     * @see UserService#getAllByParams(String, Object[])
     */
    public Mono<ResponseEntity<?>> searchUserInfo(ServerWebExchange exchange, Set<String> userInfos, String type) {
        Function<? super User, ? extends String> key = type.equals(UserInfoTypeEnum.NAME.name()) ? User::getUsername : user -> user
                .getId()
                .toString();
        Function<? super User, ? extends String> value = type.equals(UserInfoTypeEnum.NAME.name()) ? user -> user
                .getId()
                .toString() : User::getUsername;
        Mono<ResponseEntity<?>> entityMono = validationService
                .validateUserSearchList(userInfos)
                .then(userService.getAllByParams(type, userInfos.toArray()).collectMap(key, value).flatMap(userMap -> {
                    Set<String> inValidUser = new HashSet<>(userInfos);
                    inValidUser.removeAll(userMap.keySet());

                    Map<String, Object> result = new HashMap<>();
                    result.put("foundUser", userMap);
                    result.put("notFoundUser", inValidUser);
                    ApiResponseDTO<?> responseEntity = createApiResponse(exchange, "獲取用户信息成功", result);
                    return createResponseEntity(responseEntity);
                }));
        return handleError(entityMono, exchange);
    }
}
