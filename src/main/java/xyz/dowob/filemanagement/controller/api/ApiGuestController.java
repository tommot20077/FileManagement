package xyz.dowob.filemanagement.controller.api;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.HideSensitive;
import xyz.dowob.filemanagement.annotation.RecordLevel;
import xyz.dowob.filemanagement.annotation.SkipRecord;
import xyz.dowob.filemanagement.config.properties.SecurityProperties;
import xyz.dowob.filemanagement.controller.base.BaseGuestController;
import xyz.dowob.filemanagement.customenum.LogLevelEnum;
import xyz.dowob.filemanagement.data.response.ApiResponseDTO;
import xyz.dowob.filemanagement.data.user.dto.AuthRequestDTO;
import xyz.dowob.filemanagement.data.user.dto.RegisterDTO;
import xyz.dowob.filemanagement.data.user.dto.ResetPasswordDTO;
import xyz.dowob.filemanagement.data.user.dto.UserEmailDTO;
import xyz.dowob.filemanagement.service.serviceInterface.AuthorizationService;
import xyz.dowob.filemanagement.service.serviceInterface.UserService;
import xyz.dowob.filemanagement.service.serviceInterface.ValidationService;

import java.util.HashMap;

/**
 * 基於 Spring WebFlux 反應式編程的訪客身份驗證 RESTful API 控制器實現。
 * 
 * <p>此控制器作為檔案管理系統的公開訪問入口，專門處理未認證使用者的身份驗證相關操作。
 * 採用 WebFlux 反應式程式設計模型，確保在高併發環境下提供非阻塞的身份驗證服務。</p>
 * 
 * <h3>核心功能領域：</h3>
 * <ul>
 *   <li><strong>使用者註冊</strong>：處理新使用者帳戶建立，包含輸入驗證、重複性檢查和密碼安全性驗證</li>
 *   <li><strong>身份認證</strong>：提供安全的登入驗證，支援 JWT 令牌生成和會話管理</li>
 *   <li><strong>密碼管理</strong>：實現安全的密碼重置流程，包含郵件驗證和令牌驗證機制</li>
 *   <li><strong>認證狀態檢查</strong>：驗證使用者當前認證狀態和基本身份資訊</li>
 *   <li><strong>CSRF 防護</strong>：提供跨站請求偽造防護令牌的取得和管理</li>
 * </ul>
 * 
 * <h3>安全性防護機制：</h3>
 * <ul>
 *   <li><strong>限流控制</strong>：整合使用者操作限流機制，防止暴力攻擊和資源濫用</li>
 *   <li><strong>輸入驗證</strong>：所有請求參數均經過嚴格的格式驗證和業務規則檢查</li>
 *   <li><strong>CSRF 防護</strong>：提供完整的跨站請求偽造防護機制</li>
 *   <li><strong>敏感資料保護</strong>：關鍵操作使用 {@code @HideSensitive} 註解防止敏感資訊洩露</li>
 *   <li><strong>密碼安全</strong>：實施密碼強度檢查和歷史密碼重複使用防護</li>
 * </ul>
 * 
 * <h3>反應式程式設計特性：</h3>
 * <ul>
 *   <li>所有端點方法返回 {@link Mono}&lt;{@link ResponseEntity}&gt; 以確保非阻塞處理</li>
 *   <li>透過 {@link ServerWebExchange} 管理請求上下文和回應資料</li>
 *   <li>整合 WebFlux 錯誤處理機制，提供統一的異常回應格式</li>
 *   <li>支援反應式資料庫操作和非同步郵件發送</li>
 * </ul>
 * 
 * <h3>API 端點概覽：</h3>
 * <ul>
 *   <li>{@code POST /api/v1/guest/register} - 使用者註冊</li>
 *   <li>{@code GET /api/v1/guest/checkAuthenticationStatus} - 認證狀態檢查</li>
 *   <li>{@code POST /api/v1/guest/sendResetPasswordMail} - 發送密碼重置郵件</li>
 *   <li>{@code PUT /api/v1/guest/resetPassword} - 執行密碼重置</li>
 *   <li>{@code POST /api/v1/guest/login} - 使用者登入</li>
 *   <li>{@code GET /api/v1/guest/csrf/token} - 取得 CSRF Token</li>
 * </ul>
 * 
 * <p>此控制器繼承自 {@link BaseGuestController}，利用策略模式實現可插拔的認證服務和安全性配置。
 * 所有操作均為公開訪問，但具備完整的安全檢查和防護機制。</p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see BaseGuestController
 * @see AuthorizationService
 * @see UserService
 * @see ValidationService
 * @see SecurityProperties
 */
@RestController
@RecordLevel(LogLevelEnum.INFO)
@RequestMapping("/api/v1/guest")
public class ApiGuestController extends BaseGuestController {

    /**
     * 初始化訪客身份驗證 API 控制器，透過 Spring 依賴注入機制設定所需的服務組件。
     * 
     * <p>此建構函數負責整合訪客身份驗證所需的核心服務元件，建立完整的服務層架構。
     * 透過依賴注入確保各服務組件的正確初始化和生命週期管理。</p>
     * 
     * <h4>服務組件說明：</h4>
     * <ul>
     *   <li><strong>授權服務</strong>：負責 JWT 令牌管理、CSRF 防護和使用者認證狀態檢查</li>
     *   <li><strong>使用者服務</strong>：處理使用者註冊、登入、密碼管理等核心業務邏輯</li>
     *   <li><strong>安全屬性配置</strong>：提供系統安全策略、令牌設定和加密參數</li>
     *   <li><strong>驗證服務</strong>：執行輸入參數驗證、格式檢查和業務規則驗證</li>
     * </ul>
     * 
     * <p>建構函數執行時會自動調用父類別建構函數，確保基礎控制器功能的正確初始化。</p>
     *
     * @param authorizationService 授權業務服務，提供身份驗證、JWT 令牌管理和 CSRF 防護功能
     * @param userService 使用者業務服務，負責使用者註冊、登入和帳戶管理相關操作
     * @param securityProperties 系統安全配置屬性，定義安全策略、加密設定和認證參數
     * @param validationService 資料驗證服務，執行請求參數的格式檢查和業務規則驗證
     */
    protected ApiGuestController(AuthorizationService authorizationService, UserService userService, SecurityProperties securityProperties, ValidationService validationService) {
        super(authorizationService, validationService, userService, securityProperties);
    }


    /**
     * 處理新使用者註冊的 RESTful API 端點，提供安全的使用者帳戶建立服務。
     * 
     * <p>此端點接受使用者註冊請求，執行完整的註冊流程驗證和帳戶建立。
     * 採用反應式程式設計模型，確保在高併發環境下的非阻塞處理。</p>
     * 
     * <h4>HTTP 請求規格：</h4>
     * <ul>
     *   <li><strong>方法</strong>：POST</li>
     *   <li><strong>路徑</strong>：{@code /api/v1/guest/register}</li>
     *   <li><strong>內容類型</strong>：application/json</li>
     *   <li><strong>認證要求</strong>：無（公開端點）</li>
     * </ul>
     * 
     * <h4>註冊流程處理：</h4>
     * <ol>
     *   <li>驗證請求參數格式和完整性</li>
     *   <li>檢查使用者名稱和電子郵件的唯一性</li>
     *   <li>驗證密碼強度是否符合安全要求</li>
     *   <li>建立新的使用者帳戶記錄</li>
     *   <li>執行必要的初始化設定</li>
     *   <li>返回註冊結果和狀態資訊</li>
     * </ol>
     * 
     * <h4>安全性控制：</h4>
     * <ul>
     *   <li><strong>輸入驗證</strong>：所有註冊資料均經過嚴格的格式和業務規則驗證</li>
     *   <li><strong>重複性檢查</strong>：防止使用者名稱和電子郵件重複註冊</li>
     *   <li><strong>密碼安全</strong>：強制執行密碼複雜度要求和安全性檢查</li>
     *   <li><strong>限流控制</strong>：整合系統限流機制，防止註冊濫用</li>
     * </ul>
     * 
     * <h4>回應格式：</h4>
     * <ul>
     *   <li><strong>成功</strong>：HTTP 200，包含註冊成功訊息和基本使用者資訊</li>
     *   <li><strong>驗證失敗</strong>：HTTP 400，包含具體的驗證錯誤訊息</li>
     *   <li><strong>衝突錯誤</strong>：HTTP 409，使用者名稱或電子郵件已存在</li>
     *   <li><strong>系統錯誤</strong>：HTTP 500，伺服器內部處理錯誤</li>
     * </ul>
     * 
     * <p>此方法透過調用父類別的 {@link BaseGuestController#register} 方法實現核心業務邏輯，
     * 確保註冊流程的一致性和可靠性。</p>
     *
     * @param registerUserDTO 使用者註冊資料傳輸物件，包含使用者名稱、密碼、電子郵件等必要註冊資訊
     * @param exchange WebFlux 伺服器交換實例，封裝 HTTP 請求和回應的完整上下文資訊
     * @return 反應式回應實體 {@link Mono}&lt;{@link ResponseEntity}&gt;，包裝註冊處理結果和狀態碼
     */
    @PostMapping("/register")
    public Mono<ResponseEntity<?>> register(@RequestBody RegisterDTO registerUserDTO, ServerWebExchange exchange) {
        return super.register(registerUserDTO, exchange);
    }

    /**
     * 檢查使用者當前身份認證狀態的 RESTful API 端點，提供即時的認證資訊查詢服務。
     * 
     * <p>此端點用於驗證使用者的當前認證狀態，包括 JWT 令牌有效性、會話狀態和基本使用者資訊。
     * 適用於前端應用程式進行身份驗證狀態檢查和自動登入功能。</p>
     * 
     * <h4>HTTP 請求規格：</h4>
     * <ul>
     *   <li><strong>方法</strong>：GET</li>
     *   <li><strong>路徑</strong>：{@code /api/v1/guest/checkAuthenticationStatus}</li>
     *   <li><strong>內容類型</strong>：無需求體</li>
     *   <li><strong>認證要求</strong>：無（公開端點，但會檢查現有認證）</li>
     * </ul>
     * 
     * <h4>認證檢查流程：</h4>
     * <ol>
     *   <li>檢查請求中的 JWT 令牌或會話 Cookie</li>
     *   <li>驗證令牌的有效性和時效性</li>
     *   <li>提取使用者身份資訊和權限資料</li>
     *   <li>檢查使用者帳戶狀態（啟用、鎖定等）</li>
     *   <li>返回認證狀態和基本使用者資訊</li>
     * </ol>
     * 
     * <h4>回應內容說明：</h4>
     * <ul>
     *   <li><strong>已認證</strong>：返回使用者基本資訊、角色權限和認證狀態</li>
     *   <li><strong>未認證</strong>：返回匿名狀態和相關提示資訊</li>
     *   <li><strong>令牌過期</strong>：返回過期狀態和重新認證指引</li>
     *   <li><strong>帳戶異常</strong>：返回帳戶狀態和相關處理建議</li>
     * </ul>
     * 
     * <h4>安全性考量：</h4>
     * <ul>
     *   <li><strong>無敏感資訊洩露</strong>：只返回必要的公開資訊，避免敏感資料暴露</li>
     *   <li><strong>狀態一致性</strong>：確保回應的認證狀態與實際系統狀態一致</li>
     *   <li><strong>記錄控制</strong>：使用 {@code @SkipRecord} 避免過度記錄頻繁的狀態檢查</li>
     * </ul>
     * 
     * <h4>使用場景：</h4>
     * <ul>
     *   <li>前端應用程式啟動時的認證狀態檢查</li>
     *   <li>頁面重新整理後的使用者資訊恢復</li>
     *   <li>自動登入功能的狀態驗證</li>
     *   <li>權限控制前的身份確認</li>
     * </ul>
     * 
     * <p>此方法標註 {@code @SkipRecord} 以避免頻繁的日誌記錄，透過調用父類別的
     * {@link BaseGuestController#checkAuthenticationStatus} 方法實現核心認證檢查邏輯。</p>
     *
     * @param exchange WebFlux 伺服器交換實例，包含 HTTP 請求上下文、認證資訊和會話狀態
     * @return 反應式回應實體 {@link Mono}&lt;{@link ResponseEntity}&gt;，包裝認證狀態檢查結果
     */
    @SkipRecord
    @GetMapping("/checkAuthenticationStatus")
    public Mono<ResponseEntity<?>> checkAuthenticationStatus(ServerWebExchange exchange) {
        return super.checkAuthenticationStatus(exchange);
    }

    /**
     * 發送密碼重置驗證電子郵件的 RESTful API 端點，提供安全的密碼重置流程起始服務。
     * 
     * <p>此端點接受使用者的電子郵件地址，產生安全的密碼重置令牌，並透過電子郵件發送重置連結。
     * 採用非同步郵件發送機制，確保使用者請求的即時回應和系統效能。</p>
     * 
     * <h4>HTTP 請求規格：</h4>
     * <ul>
     *   <li><strong>方法</strong>：POST</li>
     *   <li><strong>路徑</strong>：{@code /api/v1/guest/sendResetPasswordMail}</li>
     *   <li><strong>內容類型</strong>：application/json</li>
     *   <li><strong>認證要求</strong>：無（公開端點）</li>
     * </ul>
     * 
     * <h4>密碼重置郵件流程：</h4>
     * <ol>
     *   <li>驗證電子郵件地址格式的正確性</li>
     *   <li>檢查電子郵件地址是否為已註冊使用者</li>
     *   <li>產生具有時效性的密碼重置令牌</li>
     *   <li>建立包含重置連結的電子郵件內容</li>
     *   <li>透過非同步機制發送重置郵件</li>
     *   <li>記錄重置請求的相關資訊</li>
     * </ol>
     * 
     * <h4>安全性防護機制：</h4>
     * <ul>
     *   <li><strong>令牌安全</strong>：使用安全隨機數產生具有時效性的重置令牌</li>
     *   <li><strong>時效控制</strong>：重置令牌具有嚴格的有效期限制</li>
     *   <li><strong>使用者驗證</strong>：只對已註冊的電子郵件地址發送重置郵件</li>
     *   <li><strong>限流保護</strong>：防止同一使用者或 IP 地址的頻繁重置請求</li>
     *   <li><strong>資訊保護</strong>：不洩露使用者帳戶存在與否的資訊</li>
     * </ul>
     * 
     * <h4>郵件內容特性：</h4>
     * <ul>
     *   <li>包含安全的密碼重置連結和令牌資訊</li>
     *   <li>明確標示令牌的有效期限和使用方式</li>
     *   <li>提供安全建議和注意事項</li>
     *   <li>包含系統聯繫資訊以供協助</li>
     * </ul>
     * 
     * <h4>回應處理原則：</h4>
     * <ul>
     *   <li><strong>統一回應</strong>：無論使用者是否存在，均返回相同的成功訊息</li>
     *   <li><strong>隱私保護</strong>：不透露帳戶存在性資訊，防止帳戶列舉攻擊</li>
     *   <li><strong>非同步處理</strong>：郵件發送採用背景處理，避免阻塞使用者請求</li>
     * </ul>
     * 
     * <p>此方法透過調用父類別的 {@link BaseGuestController#sendResetPasswordMail} 方法
     * 實現密碼重置郵件的核心業務邏輯，確保重置流程的安全性和一致性。</p>
     *
     * @param userMail 使用者電子郵件資料傳輸物件，包含需要重置密碼的電子郵件地址
     * @param exchange WebFlux 伺服器交換實例，提供請求上下文、IP 地址和使用者代理資訊
     * @return 反應式回應實體 {@link Mono}&lt;{@link ResponseEntity}&gt;，包裝郵件發送處理結果
     */
    @PostMapping("/sendResetPasswordMail")
    public Mono<ResponseEntity<?>> sendResetPasswordMail(@RequestBody UserEmailDTO userMail, ServerWebExchange exchange) {
        return super.sendResetPasswordMail(userMail, exchange);
    }


    /**
     * 執行使用者密碼重置的 RESTful API 端點，提供安全的密碼更新服務。
     * 
     * <p>此端點接受密碼重置令牌和新密碼，執行完整的密碼重置流程驗證和更新。
     * 採用反應式程式設計模型，確保密碼重置過程的安全性、完整性和非阻塞處理。</p>
     * 
     * <h4>HTTP 請求規格：</h4>
     * <ul>
     *   <li><strong>方法</strong>：PUT</li>
     *   <li><strong>路徑</strong>：{@code /api/v1/guest/resetPassword}</li>
     *   <li><strong>內容類型</strong>：application/json</li>
     *   <li><strong>認證要求</strong>：需要有效的重置令牌（無需使用者認證）</li>
     * </ul>
     * 
     * <h4>密碼重置流程：</h4>
     * <ol>
     *   <li>驗證重置令牌的格式和完整性</li>
     *   <li>檢查令牌的有效性和時效性</li>
     *   <li>確認令牌對應的使用者帳戶狀態</li>
     *   <li>驗證新密碼的安全強度要求</li>
     *   <li>檢查新密碼與歷史密碼的重複性</li>
     *   <li>執行密碼加密和資料庫更新</li>
     *   <li>撤銷相關的重置令牌</li>
     *   <li>記錄密碼重置操作日誌</li>
     * </ol>
     * 
     * <h4>安全性驗證機制：</h4>
     * <ul>
     *   <li><strong>令牌驗證</strong>：嚴格驗證重置令牌的有效性、完整性和時效性</li>
     *   <li><strong>密碼強度檢查</strong>：強制執行密碼複雜度要求和安全性標準</li>
     *   <li><strong>歷史密碼檢查</strong>：防止使用者重複使用近期使用過的密碼</li>
     *   <li><strong>一次性使用</strong>：重置令牌僅可使用一次，使用後立即失效</li>
     *   <li><strong>時效性控制</strong>：重置令牌具有嚴格的有效期限制</li>
     * </ul>
     * 
     * <h4>密碼安全要求：</h4>
     * <ul>
     *   <li>最小長度要求和字元複雜度驗證</li>
     *   <li>包含大小寫字母、數字和特殊字元的組合</li>
     *   <li>禁止使用常見的弱密碼和字典單詞</li>
     *   <li>與使用者資訊（姓名、電子郵件等）的相似度檢查</li>
     * </ul>
     * 
     * <h4>回應格式：</h4>
     * <ul>
     *   <li><strong>成功</strong>：HTTP 200，包含密碼重置成功訊息</li>
     *   <li><strong>令牌無效</strong>：HTTP 400，令牌格式錯誤或已失效</li>
     *   <li><strong>令牌過期</strong>：HTTP 410，令牌已超過有效期限</li>
     *   <li><strong>密碼不符合要求</strong>：HTTP 400，新密碼不符合安全標準</li>
     *   <li><strong>系統錯誤</strong>：HTTP 500，伺服器內部處理錯誤</li>
     * </ul>
     * 
     * <h4>後續安全措施：</h4>
     * <ul>
     *   <li>重置完成後立即撤銷所有相關的重置令牌</li>
     *   <li>可選擇性地撤銷現有的 JWT 認證令牌</li>
     *   <li>發送密碼重置完成通知郵件</li>
     *   <li>記錄安全操作日誌供審計使用</li>
     * </ul>
     * 
     * <p>此方法透過調用父類別的 {@link BaseGuestController#resetPassword} 方法實現
     * 密碼重置的核心業務邏輯，確保重置流程的安全性和一致性。</p>
     *
     * @param resetPasswordDTO 密碼重置資料傳輸物件，包含重置令牌、新密碼和相關驗證資訊
     * @param exchange WebFlux 伺服器交換實例，提供請求上下文、客戶端資訊和操作環境
     * @return 反應式回應實體 {@link Mono}&lt;{@link ResponseEntity}&gt;，包裝密碼重置操作的完整結果
     */
    @PutMapping("/resetPassword")
    public Mono<ResponseEntity<?>> resetPassword(@RequestBody ResetPasswordDTO resetPasswordDTO, ServerWebExchange exchange) {
        return super.resetPassword(resetPasswordDTO, exchange);
    }

    /**
     * 處理使用者身份認證登入的 RESTful API 端點，提供安全的使用者身份驗證服務。
     * 
     * <p>此端點接受使用者登入憑證，執行完整的身份驗證流程，並產生相應的認證令牌。
     * 採用反應式程式設計模型，確保認證過程的安全性和非阻塞處理。</p>
     * 
     * <h4>HTTP 請求規格：</h4>
     * <ul>
     *   <li><strong>方法</strong>：POST</li>
     *   <li><strong>路徑</strong>：{@code /api/v1/guest/login}</li>
     *   <li><strong>內容類型</strong>：application/json</li>
     *   <li><strong>認證要求</strong>：無（公開端點，但需提供有效憑證）</li>
     * </ul>
     * 
     * <h4>身份認證流程：</h4>
     * <ol>
     *   <li>驗證輸入參數的格式和完整性（透過 {@code @Validated} 註解）</li>
     *   <li>檢查使用者帳戶的存在性和狀態</li>
     *   <li>執行密碼驗證和安全性檢查</li>
     *   <li>檢查帳戶是否被鎖定或停用</li>
     *   <li>產生 JWT 認證令牌和相關會話資訊</li>
     *   <li>記錄登入事件和安全審計資訊</li>
     *   <li>返回認證結果和使用者基本資訊</li>
     * </ol>
     * 
     * <h4>安全性防護機制：</h4>
     * <ul>
     *   <li><strong>參數驗證</strong>：使用 {@code @Validated} 確保輸入參數的格式正確性</li>
     *   <li><strong>敏感資料保護</strong>：使用 {@code @HideSensitive} 防止登入憑證在日誌中洩露</li>
     *   <li><strong>暴力攻擊防護</strong>：整合登入限流機制，防止密碼暴力破解</li>
     *   <li><strong>帳戶安全檢查</strong>：驗證帳戶狀態，包括是否啟用、鎖定或暫停</li>
     *   <li><strong>令牌安全</strong>：產生具有時效性和安全性的 JWT 認證令牌</li>
     * </ul>
     * 
     * <h4>令牌管理特性：</h4>
     * <ul>
     *   <li>JWT 令牌包含使用者身份、角色和權限資訊</li>
     *   <li>令牌具有可配置的有效期限和自動續期機制</li>
     *   <li>支援令牌撤銷和黑名單管理</li>
     *   <li>整合安全的令牌儲存和傳輸機制</li>
     * </ul>
     * 
     * <h4>回應格式：</h4>
     * <ul>
     *   <li><strong>成功</strong>：HTTP 200，包含 JWT 令牌、使用者資訊和權限資料</li>
     *   <li><strong>憑證無效</strong>：HTTP 401，使用者名稱或密碼錯誤</li>
     *   <li><strong>帳戶鎖定</strong>：HTTP 423，帳戶因安全原因被暫時鎖定</li>
     *   <li><strong>帳戶停用</strong>：HTTP 403，帳戶已被管理員停用</li>
     *   <li><strong>驗證失敗</strong>：HTTP 400，請求參數格式錯誤</li>
     * </ul>
     * 
     * <h4>登入限制和保護：</h4>
     * <ul>
     *   <li>限制單一 IP 地址的登入嘗試頻率</li>
     *   <li>限制單一帳戶的連續失敗登入次數</li>
     *   <li>自動帳戶鎖定和解鎖機制</li>
     *   <li>異常登入行為的檢測和通知</li>
     * </ul>
     * 
     * <p>此方法透過調用父類別的 {@link BaseGuestController#login} 方法實現登入認證的核心邏輯，
     * 第三個參數 {@code false} 表示此為 API 模式登入，不使用 Cookie 會話管理。</p>
     *
     * @param authRequestDTO 身份認證請求資料傳輸物件，包含使用者名稱、密碼和相關認證資訊
     * @param exchange WebFlux 伺服器交換實例，提供請求上下文、客戶端資訊和會話管理
     * @return 反應式回應實體 {@link Mono}&lt;{@link ResponseEntity}&gt;，包裝認證結果和令牌資訊
     */
    @HideSensitive
    @PostMapping("/login")
    public Mono<ResponseEntity<?>> login(@Validated @RequestBody AuthRequestDTO authRequestDTO, ServerWebExchange exchange) {
        return super.login(authRequestDTO, exchange, false);
    }

    /**
     * 獲取跨站請求偽造（CSRF）防護令牌的 RESTful API 端點，提供安全的表單防護服務。
     * 
     * <p>此端點負責產生和返回 CSRF 防護令牌，用於防止跨站請求偽造攻擊。
     * 前端應用程式可透過此端點獲取有效的 CSRF 令牌，並在後續的狀態變更請求中包含此令牌。</p>
     * 
     * <h4>HTTP 請求規格：</h4>
     * <ul>
     *   <li><strong>方法</strong>：GET</li>
     *   <li><strong>路徑</strong>：{@code /api/v1/guest/csrf/token}</li>
     *   <li><strong>內容類型</strong>：無需求體</li>
     *   <li><strong>認證要求</strong>：無（公開端點）</li>
     * </ul>
     * 
     * <h4>CSRF 防護機制：</h4>
     * <ol>
     *   <li>透過 {@link AuthorizationService#getCSRFToken} 產生安全的 CSRF 令牌</li>
     *   <li>令牌包含隨機數值，具有會話綁定特性</li>
     *   <li>令牌與使用者會話狀態和請求來源進行關聯</li>
     *   <li>提供令牌的多種使用方式（Header 和 Parameter 模式）</li>
     * </ol>
     * 
     * <h4>回應資料結構：</h4>
     * <ul>
     *   <li><strong>token</strong>：實際的 CSRF 令牌字串，用於請求驗證</li>
     *   <li><strong>headerName</strong>：HTTP Header 中使用的令牌欄位名稱（例如：X-CSRF-TOKEN）</li>
     *   <li><strong>parameterName</strong>：HTTP 參數中使用的令牌欄位名稱（例如：_csrf）</li>
     * </ul>
     * 
     * <h4>安全性特性：</h4>
     * <ul>
     *   <li><strong>會話綁定</strong>：令牌與特定使用者會話關聯，無法跨會話使用</li>
     *   <li><strong>時效性控制</strong>：令牌具有適當的有效期限制</li>
     *   <li><strong>隨機性保證</strong>：使用安全隨機數產生器確保令牌的不可預測性</li>
     *   <li><strong>雙重驗證</strong>：支援 Header 和 Parameter 兩種令牌傳遞方式</li>
     *   <li><strong>敏感資料保護</strong>：使用 {@code @HideSensitive} 註解防止令牌在日誌中洩露</li>
     * </ul>
     * 
     * <h4>使用指導：</h4>
     * <ul>
     *   <li><strong>前端整合</strong>：前端應用程式應在頁面載入時獲取 CSRF 令牌</li>
     *   <li><strong>請求包含</strong>：所有狀態變更請求（POST、PUT、DELETE）都應包含此令牌</li>
     *   <li><strong>令牌更新</strong>：建議定期更新令牌以維持安全性</li>
     *   <li><strong>錯誤處理</strong>：令牌無效時應重新獲取新的令牌</li>
     * </ul>
     * 
     * <h4>反應式處理特性：</h4>
     * <ul>
     *   <li>使用反應式操作鏈 {@code flatMap} 進行非阻塞的令牌產生和回應構建</li>
     *   <li>透過 {@code handleError} 方法提供統一的錯誤處理機制</li>
     *   <li>確保在高併發環境下的穩定效能表現</li>
     * </ul>
     * 
     * <h4>日誌記錄等級：</h4>
     * <p>此方法使用 {@code @RecordLevel(LogLevelEnum.DEBUG)} 註解，將日誌記錄等級設定為 DEBUG，
     * 避免在一般運行環境中產生過多的令牌獲取日誌記錄。</p>
     * 
     * <h4>回應格式：</h4>
     * <ul>
     *   <li><strong>成功</strong>：HTTP 200，包含完整的 CSRF 令牌資訊</li>
     *   <li><strong>系統錯誤</strong>：HTTP 500，令牌產生過程中的內部錯誤</li>
     * </ul>
     * 
     * <p>此方法透過呼叫 {@link AuthorizationService#getCSRFToken} 服務方法獲取 CSRF 令牌，
     * 並使用統一的回應格式包裝令牌資訊返回給客戶端。</p>
     *
     * @param exchange WebFlux 伺服器交換實例，包含 HTTP 請求上下文、會話資訊和客戶端環境
     * @return 反應式回應實體 {@link Mono}&lt;{@link ResponseEntity}&gt;，包裝 CSRF 令牌資訊和使用指導
     */
    @HideSensitive
    @GetMapping("/csrf/token")
    @RecordLevel(LogLevelEnum.DEBUG)
    public Mono<ResponseEntity<?>> getCSRFToken(ServerWebExchange exchange) {
        return handleError(authorizationService.getCSRFToken(exchange).flatMap(csrfToken -> {
            HashMap<String, Object> data = new HashMap<>();
            data.put("token", csrfToken.getToken());
            data.put("headerName", csrfToken.getHeaderName());
            data.put("parameterName", csrfToken.getParameterName());
            ApiResponseDTO<?> apiResponse = createApiResponse(exchange, "獲取 CSRF Token 成功", data);
            return createResponseEntity(apiResponse);
        }), exchange);
    }
}
