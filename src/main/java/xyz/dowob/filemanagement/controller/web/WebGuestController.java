package xyz.dowob.filemanagement.controller.web;

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
import xyz.dowob.filemanagement.data.user.dto.AuthRequestDTO;
import xyz.dowob.filemanagement.data.user.dto.RegisterDTO;
import xyz.dowob.filemanagement.data.user.dto.ResetPasswordDTO;
import xyz.dowob.filemanagement.data.user.dto.UserEmailDTO;
import xyz.dowob.filemanagement.service.serviceInterface.AuthorizationService;
import xyz.dowob.filemanagement.service.serviceInterface.UserService;
import xyz.dowob.filemanagement.service.serviceInterface.ValidationService;

/**
 * 基於 WebFlux 的訪客 Web 控制器，處理訪客相關的 RESTful API 操作。
 * <p>
 * 提供訪客登入、註冊、密碼重置等身份驗證相關功能。
 * 支援無需登入即可使用的公開功能，如註冊新用戶和密碼重置。
 * <p>
 * 此控制器繼承自 {@link BaseGuestController}，採用反應式編程模式處理所有 HTTP 請求，
 * 確保在高併發身份驗證場景下的性能表現。所有操作均遵循安全設定與限制策略。
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@RestController
@RecordLevel(LogLevelEnum.INFO)
@RequestMapping("/web/v1/guest")
public class WebGuestController extends BaseGuestController {
    /**
     * 訪客 Web 控制器的構造方法。
     *
     * @param authorizationService 授權服務
     * @param userService          用戶服務
     * @param securityProperties   安全屬性設定
     * @param validationService    驗證服務
     */
    protected WebGuestController(AuthorizationService authorizationService, UserService userService, SecurityProperties securityProperties, ValidationService validationService) {
        super(authorizationService, validationService, userService, securityProperties);
    }


    /**
     * 處理訪客註冊請求，創建新的用戶帳號。
     * <p>
     * 驗證用戶提供的註冊資訊，包括用戶名、密碼、電子郵件等，
     * 並在驗證通過後將新用戶儲存到資料庫中。
     * 此操作不需要身份驗證，任何訪客都可以呼叫。
     *
     * @param registerUserDTO 包含用戶名、密碼、電子郵件等註冊資訊的資料傳輸對象，不可為 null
     * @param exchange        伺服器Web交換對象，包含當前HTTP請求的完整上下文資訊
     *
     * @return 包含註冊結果的響應實體 Mono，成功時返回用戶基本資訊，失敗時返回錯誤訊息
     */
    @PostMapping("/register")
    public Mono<ResponseEntity<?>> register(@RequestBody RegisterDTO registerUserDTO, ServerWebExchange exchange) {
        return super.register(registerUserDTO, exchange);
    }

    /**
     * 檢查當前請求的身份驗證狀態。
     * <p>
     * 驗證當前請求是否包含有效的身份驗證令牌，
     * 若已登入則返回用戶基本資訊，未登入則返回未驗證狀態。
     * 此端點用於前端判斷用戶登入狀態，執行頻繁且不記錄日誌。
     *
     * @param exchange 伺服器Web交換對象，包含當前HTTP請求的完整上下文資訊
     *
     * @return 包含身份驗證狀態的響應實體 Mono，已登入時返回用戶資訊，未登入時返回適當的狀態訊息
     */
    @SkipRecord
    @GetMapping("/checkAuthenticationStatus")
    public Mono<ResponseEntity<?>> checkAuthenticationStatus(ServerWebExchange exchange) {
        return super.checkAuthenticationStatus(exchange);
    }

    /**
     * 發送密碼重置郵件給指定的用戶。
     * <p>
     * 根據提供的電子郵件地址查找對應的用戶帳號，
     * 若用戶存在則生成密碼重置令牌並發送重置郵件。
     * 為保護用戶隱私，無論用戶是否存在都會返回相同的成功訊息。
     *
     * @param userMail 包含目標電子郵件地址的資料傳輸對象，不可為 null
     * @param exchange 伺服器Web交換對象，包含當前HTTP請求的完整上下文資訊
     *
     * @return 包含郵件發送結果的響應實體 Mono，始終返回成功訊息以避免洩露用戶存在性
     */
    @PostMapping("/sendResetPasswordMail")
    public Mono<ResponseEntity<?>> sendResetPasswordMail(@RequestBody UserEmailDTO userMail, ServerWebExchange exchange) {
        return super.sendResetPasswordMail(userMail, exchange);
    }


    /**
     * 處理密碼重置請求，完成用戶密碼的變更。
     * <p>
     * 驗證密碼重置令牌的有效性和完整性，
     * 確認令牌未過期且匹配正確的用戶後，
     * 將用戶密碼更新為新提供的密碼並使重置令牌失效。
     *
     * @param resetPasswordDTO 包含重置令牌和新密碼的資料傳輸對象，不可為 null
     * @param exchange         伺服器Web交換對象，包含當前HTTP請求的完整上下文資訊
     *
     * @return 包含密碼重置結果的響應實體 Mono，成功時返回確認訊息，失敗時返回錯誤詳情
     */
    @PutMapping("/resetPassword")
    public Mono<ResponseEntity<?>> resetPassword(@RequestBody ResetPasswordDTO resetPasswordDTO, ServerWebExchange exchange) {
        return super.resetPassword(resetPasswordDTO, exchange);
    }

    /**
     * 處理訪客登入請求，驗證用戶身份並生成訪問令牌。
     * <p>
     * 驗證提供的用戶名和密碼，確認用戶身份有效性，
     * 檢查帳號是否被鎖定或限制，通過驗證後生成JWT訪問令牌。
     * 此方法包含敏感資訊處理，已標記為隱藏敏感資料。
     *
     * @param authRequestDTO 包含用戶名和密碼的登入請求資料，經過驗證註解處理，不可為 null
     * @param exchange       伺服器Web交換對象，包含當前HTTP請求的完整上下文資訊
     *
     * @return 包含登入結果的響應實體 Mono，成功時返回JWT令牌和用戶資訊，失敗時返回錯誤訊息
     */
    @HideSensitive
    @PostMapping("/login")
    public Mono<ResponseEntity<?>> login(@Validated @RequestBody AuthRequestDTO authRequestDTO, ServerWebExchange exchange) {
        return super.login(authRequestDTO, exchange, true);
    }
}
