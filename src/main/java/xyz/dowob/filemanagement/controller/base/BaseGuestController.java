package xyz.dowob.filemanagement.controller.base;

import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.RecordLevel;
import xyz.dowob.filemanagement.config.properties.SecurityProperties;
import xyz.dowob.filemanagement.customenum.LogLevelEnum;
import xyz.dowob.filemanagement.data.api.ApiResponseDTO;
import xyz.dowob.filemanagement.data.user.dto.*;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.service.serviceInterface.AuthorizationService;
import xyz.dowob.filemanagement.service.serviceInterface.UserService;
import xyz.dowob.filemanagement.unity.ResponseUnity;

import java.util.HashMap;

/**
 * 訪客控制器的基礎類
 * 本類提供了訪客相關操作的基礎功能，如用戶註冊、登入、重置密碼等。
 * 這些方法可以被子類繼承並進行具體的業務邏輯實現，減少代碼重複。
 * 主要功能包括處理用戶註冊、登入、確認授權狀態、發送重置密碼郵件等操作，並處理相應的異常。
 * 本類會處理請求中發生的ValidationException異常，並回傳對應的錯誤信息。
 *
 * @author yuan
 * @program File-Management
 * @ClassName BaseGuestController
 * @description 基礎的訪客控制器，提供了用戶授權和操作的公共方法
 * @create 2024-09-17 00:23
 * @Version 1.0
 **/
@RecordLevel(LogLevelEnum.INFO)
public abstract class BaseGuestController implements ResponseUnity {
    /**
     * 授權業務層對象
     * 用於處理授權相關的業務邏輯，例如驗證用戶權限等
     */
    protected final AuthorizationService authorizationService;

    /**
     * 用戶業務層對象
     * 用於處理與用戶相關的業務邏輯，例如註冊、登入、密碼重置等
     */
    protected final UserService userService;

    /**
     * 安全屬性
     * 用於存取安全設置，例如JWT過期時間、Cookie屬性等
     */
    protected final SecurityProperties securityProperties;

    /**
     * 建構子，初始化授權服務、用戶服務和安全配置
     *
     * @param authorizationService 授權業務層對象
     * @param userService          用戶業務層對象
     * @param securityProperties   安全屬性配置
     */
    protected BaseGuestController(AuthorizationService authorizationService, UserService userService, SecurityProperties securityProperties) {
        this.authorizationService = authorizationService;
        this.userService = userService;
        this.securityProperties = securityProperties;
    }


    /**
     * 訪客註冊的請求
     * 當用戶進行註冊時，系統將接收用戶數據並創建新用戶。
     * 成功註冊後返回註冊成功的結果。
     *
     * @param registerUserDTO 註冊用戶的數據傳輸對象
     * @param exchange        請求對象，包含請求相關的信息
     *
     * @return Mono<ResponseEntity < ?>> 返回註冊結果的Mono對象，封裝了響應數據
     */
    public Mono<ResponseEntity<?>> register(RegisterDTO registerUserDTO, ServerWebExchange exchange) {
        return handleError(userService.register(registerUserDTO).then(Mono.defer(() -> {
            HashMap<String, Object> data = new HashMap<>();
            ApiResponseDTO<?> apiResponse = createResponse(exchange, 201, "註冊成功", data);
            return createResponseEntity(apiResponse, 201);
        })), exchange);
    }


    /**
     * 訪客登入的請求
     * 用戶登入時，系統驗證其憑證並發送JWT令牌（若Web環境則存儲為Cookie）。
     * 登入成功後返回登入結果。
     *
     * @param authRequestDTO 登入用戶的數據傳輸對象
     * @param exchange       請求對象
     * @param isWeb          是否為Web環境，決定是否設置HTTPOnly、Secure等Cookie屬性
     *
     * @return Mono<ResponseEntity < ?>> 返回登入結果的Mono對象
     */
    public Mono<ResponseEntity<?>> login(AuthRequestDTO authRequestDTO, ServerWebExchange exchange, boolean isWeb) {
        return handleError(userService.login(authRequestDTO, exchange).flatMap(token -> {
            if (isWeb) {
                ResponseCookie cookie = ResponseCookie
                        .from("jwtToken", token)
                        .httpOnly(securityProperties.getCookie().isHttpOnly())
                        .secure(securityProperties.getCookie().isSecure())
                        .maxAge(securityProperties.getJwtToken().getExpiration().toSeconds())
                        .sameSite(securityProperties.getCookie().getSameSite())
                        .path("/")
                        .build();
                exchange.getResponse().addCookie(cookie);
            }
            ApiResponseDTO<?> apiResponse = createResponse(exchange, "登入成功", new AuthResponseDTO(token));
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
                    ApiResponseDTO<?> apiResponse = createResponse(exchange, "用戶已授權", data);
                    return createResponseEntity(apiResponse);
                })
                .switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.UNAUTHORIZED)))
                .onErrorResume(ValidationException.class, e -> {
                    ApiResponseDTO<?> apiResponse = createResponse(exchange, 401, "用戶未授權", null);
                    return createResponseEntity(apiResponse);
                });
    }


    /**
     * 發送重置密碼郵件
     * 用戶請求重置密碼時，系統將發送包含重置鏈接的郵件至用戶郵箱。
     *
     * @param userMail 用戶郵箱數據傳輸對象
     * @param exchange 請求對象
     *
     * @return Mono<ResponseEntity < ?>> 返回發送結果
     */
    public Mono<ResponseEntity<?>> sendResetPasswordMail(UserEmailDTO userMail, ServerWebExchange exchange) {
        return handleError(userService.sendResetPasswordMail(userMail).then(Mono.defer(() -> {
            ApiResponseDTO<?> apiResponse = createResponse(exchange, "重置密碼郵件已發送，請到信箱查收驗證信", null);
            return createResponseEntity(apiResponse);
        })), exchange);
    }


    /**
     * 重置密碼的請求
     * 用戶提供重置密碼的驗證信息後，系統將更新用戶的密碼。
     *
     * @param resetPasswordDTO 重置密碼的數據傳輸對象
     * @param exchange         請求對象
     *
     * @return Mono<ResponseEntity < ?>> 返回重置密碼結果
     */
    public Mono<ResponseEntity<?>> resetPassword(ResetPasswordDTO resetPasswordDTO, ServerWebExchange exchange) {
        return handleError(userService.resetPassword(resetPasswordDTO).then(Mono.defer(() -> {
            ApiResponseDTO<?> apiResponse = createResponse(exchange, "密碼重置成功", null);
            return createResponseEntity(apiResponse);
        })), exchange);
    }
}

