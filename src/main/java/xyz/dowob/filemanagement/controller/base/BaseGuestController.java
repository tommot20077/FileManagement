package xyz.dowob.filemanagement.controller.base;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.dto.api.ApiResponseDTO;
import xyz.dowob.filemanagement.dto.user.AuthRequestDTO;
import xyz.dowob.filemanagement.dto.user.RegisterDTO;
import xyz.dowob.filemanagement.dto.user.ResetPasswordDTO;
import xyz.dowob.filemanagement.dto.user.UserEmailDTO;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.service.ServiceInterFace.AuthorizationService;
import xyz.dowob.filemanagement.service.ServiceInterFace.UserService;

import java.util.HashMap;

/**
 * 訪客控制器的基礎類
 * 主要提供訪客控制器的基本方法，並交由子類繼承方法，減少代碼重複
 * 實現BaseController{@link BaseController}
 * 此類會處理請求中發生的ValidationException異常，並回傳對應的錯誤信息
 *
 * @author yuan
 * @program File-Management
 * @ClassName BaseGuestController
 * @description
 * @create 2024-09-17 00:23
 * @Version 1.0
 **/

public abstract class BaseGuestController implements BaseController {
    /**
     * 用戶業務層對象
     */
    @Autowired
    protected UserService userService;

    /**
     * 授權業務層對象
     */
    @Autowired
    protected AuthorizationService authorizationService;

    /**
     * 訪客註冊的請求
     *
     * @param registerUserDTO 註冊用戶的數據傳輸對象
     * @param exchange        請求對象
     *
     * @return Mono<ResponseEntity> 返回註冊結果
     */
    public Mono<ResponseEntity<?>> register(@Validated @RequestBody RegisterDTO registerUserDTO, ServerWebExchange exchange) {
        return userService.register(registerUserDTO).then(Mono.defer(() -> {
            HashMap<String, Object> data = new HashMap<>();
            ApiResponseDTO<?> apiResponse = createResponse(exchange, "註冊成功", data);
            return createResponseEntity(apiResponse);
        }).onErrorResume(ValidationException.class, e -> {
            String errorMessage = String.format("註冊失敗: %s", e.getMessage());
            int errorCode = e.getErrorCode().getCode();
            return createResponseEntity(createResponse(exchange, errorCode, errorMessage, null));
        }));
    }

    /**
     * 訪客登入的請求
     *
     * @param authRequestDTO 登入用戶的數據傳輸對象
     * @param exchange       請求對象
     *
     * @return Mono<ResponseEntity> 返回登入結果
     */
    public Mono<ResponseEntity<?>> login(@Validated @RequestBody AuthRequestDTO authRequestDTO, ServerWebExchange exchange) {
        return userService.login(authRequestDTO, exchange).flatMap(token -> {
            HashMap<String, Object> data = new HashMap<>();
            data.put("JWT 驗證令牌", token);
            ApiResponseDTO<?> apiResponse = createResponse(exchange, "登入成功", data);
            return createResponseEntity(apiResponse);
        }).onErrorResume(ValidationException.class, e -> {
            String errorMessage = String.format("登入失敗: %s", e.getMessage());
            int responseCode = e.getErrorCode().getCode();
            return createResponseEntity(createResponse(exchange, responseCode, errorMessage, null));
        });
    }

    /**
     * 發送重置密碼郵件
     *
     * @param userMail 用戶郵箱
     * @param exchange 請求對象
     *
     * @return Mono<ResponseEntity> 返回發送結果
     */
    public Mono<ResponseEntity<?>> sendResetPasswordMail(@RequestBody @Validated UserEmailDTO userMail, ServerWebExchange exchange) {
        return userService.sendResetPasswordMail(userMail).then(Mono.defer(() -> {
            ApiResponseDTO<?> apiResponse = createResponse(exchange, "重置密碼郵件已發送，請到信箱查收驗證信", null);
            return createResponseEntity(apiResponse);
        })).onErrorResume(ValidationException.class, e -> {
            String errorMessage = String.format("重置密碼郵件發送失敗: %s", e.getMessage());
            int responseCode = e.getErrorCode().getCode();
            return createResponseEntity(createResponse(exchange, responseCode, errorMessage, null));
        });
    }

    /**
     * 重置密碼的請求
     *
     * @param resetPasswordDTO 重置密碼的數據傳輸對象
     * @param exchange         請求對象
     *
     * @return Mono<ResponseEntity> 返回重置密碼結果
     */
    public Mono<ResponseEntity<?>> resetPassword(@Validated @RequestBody ResetPasswordDTO resetPasswordDTO, ServerWebExchange exchange) {
        return userService.resetPassword(resetPasswordDTO).then(Mono.defer(() -> {
            ApiResponseDTO<?> apiResponse = createResponse(exchange, "密碼重置成功", null);
            return createResponseEntity(apiResponse);
        })).onErrorResume(ValidationException.class, e -> {
            String errorMessage = String.format("密碼重置失敗: %s", e.getMessage());
            int responseCode = e.getErrorCode().getCode();
            return createResponseEntity(createResponse(exchange, responseCode, errorMessage, null));
        });
    }
}
