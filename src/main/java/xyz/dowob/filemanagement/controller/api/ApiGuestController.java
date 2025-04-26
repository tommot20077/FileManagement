package xyz.dowob.filemanagement.controller.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.HideSensitive;
import xyz.dowob.filemanagement.annotation.RecordLevel;
import xyz.dowob.filemanagement.annotation.SkipRecord;
import xyz.dowob.filemanagement.config.properties.SecurityProperties;
import xyz.dowob.filemanagement.controller.base.BaseGuestController;
import xyz.dowob.filemanagement.customenum.LogLevelEnum;
import xyz.dowob.filemanagement.data.api.ApiResponseDTO;
import xyz.dowob.filemanagement.data.user.dto.AuthRequestDTO;
import xyz.dowob.filemanagement.data.user.dto.RegisterDTO;
import xyz.dowob.filemanagement.data.user.dto.ResetPasswordDTO;
import xyz.dowob.filemanagement.data.user.dto.UserEmailDTO;
import xyz.dowob.filemanagement.service.serviceInterface.AuthorizationService;
import xyz.dowob.filemanagement.service.serviceInterface.UserService;

import java.util.HashMap;

/**
 * 訪客 API 控制器，提供註冊、登入、密碼重置、CSRF Token 獲取等功能。
 *
 * <p>此控制器繼承 {@link BaseGuestController}，覆蓋相關方法，避免代碼重複。</p>
 *
 * @author yuan
 * @version 1.0
 * @since 2024-09-14
 */
@RestController
@RecordLevel(LogLevelEnum.INFO)
@RequestMapping("/api/v1/guest")
public class ApiGuestController extends BaseGuestController {

    /**
     * 構造函數，初始化訪客控制器。
     *
     * @param authorizationService 授權服務
     * @param userService          用戶服務
     * @param securityProperties   安全屬性配置
     */
    protected ApiGuestController(AuthorizationService authorizationService, UserService userService, SecurityProperties securityProperties) {
        super(authorizationService, userService, securityProperties);
    }


    /**
     * 訪客註冊 API
     *
     * @param registerUserDTO 註冊用戶的數據傳輸對象
     * @param exchange        當前請求對象
     * @return Mono<ResponseEntity < ?>> 註冊結果
     */
    @PostMapping("/register")
    public Mono<ResponseEntity<?>> register(@RequestBody RegisterDTO registerUserDTO, ServerWebExchange exchange) {
        return super.register(registerUserDTO, exchange);
    }


    /**
     * 訪客登入 API
     *
     * @param authRequestDTO 登入請求數據
     * @param exchange       當前請求對象
     * @return Mono<ResponseEntity < ?>> 登入結果
     */
    @HideSensitive
    @PostMapping("/login")
    public Mono<ResponseEntity<?>> login(@RequestBody AuthRequestDTO authRequestDTO, ServerWebExchange exchange) {
        return super.login(authRequestDTO, exchange, false);
    }


    /**
     * 訪客請求重置密碼驗證信 API
     *
     * @param userMail 用戶電子郵件
     * @param exchange 當前請求對象
     * @return Mono<ResponseEntity < ?>> 發送驗證信結果
     */
    @PostMapping("/sendResetPasswordMail")
    public Mono<ResponseEntity<?>> sendResetPasswordMail(@RequestBody UserEmailDTO userMail, ServerWebExchange exchange) {
        return super.sendResetPasswordMail(userMail, exchange);
    }


    /**
     * 訪客重置密碼 API
     *
     * @param resetPasswordDTO 重置密碼請求數據
     * @param exchange         當前請求對象
     * @return Mono<ResponseEntity < ?>> 重置密碼結果
     */
    @PutMapping("/resetPassword")
    public Mono<ResponseEntity<?>> resetPassword(@RequestBody ResetPasswordDTO resetPasswordDTO, ServerWebExchange exchange) {
        return super.resetPassword(resetPasswordDTO, exchange);
    }


    /**
     * 獲取 CSRF Token API
     *
     * @param exchange 當前請求對象
     * @return Mono<ResponseEntity < ?>> 返回 CSRF Token
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
            ApiResponseDTO<?> apiResponse = createResponse(exchange, "獲取 CSRF Token 成功", data);
            return createResponseEntity(apiResponse);
        }), exchange);
    }


    /**
     * 確認當前用戶授權狀態，並返回用戶信息
     * 用於檢查當前用戶是否已經授權，並返回用戶的基本信息。
     * 如果用戶未授權，會返回401 Unauthorized錯誤。
     *
     * @param exchange 請求對象，包含請求上下文
     *
     * @return Mono<ResponseEntity < ?>> 返回用戶授權狀態的結果
     */
    @SkipRecord
    @GetMapping("/checkAuthenticationStatus")
    public Mono<ResponseEntity<?>> checkAuthenticationStatus(ServerWebExchange exchange) {
        return super.checkAuthenticationStatus(exchange);
    }
}
