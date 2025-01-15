package xyz.dowob.filemanagement.controller.base;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.component.strategy.FileStrategy;
import xyz.dowob.filemanagement.dto.api.ApiResponseDTO;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.service.ServiceInterface.UserService;
import xyz.dowob.filemanagement.unity.ResponseUnity;

/**
 * 用戶控制器的基礎類
 * 主要提供用戶控制器的基本方法，並交由子類繼承方法，減少代碼重複
 * 實現BaseController{@link ResponseUnity}
 * 此類會處理請求中發生的ValidationException異常，並回傳對應的錯誤信息
 *
 * @author yuan
 * @program File-Management
 * @ClassName BaseUserController
 * @description
 * @create 2024-09-17 00:23
 * @Version 1.0
 **/
@RequiredArgsConstructor
public abstract class BaseUserController implements ResponseUnity {
    protected final FileStrategy fileStrategy;

    protected final UserService userService;

    /**
     * 用戶登出的請求
     *
     * @param exchange 處理用戶登出的請求
     *
     * @return Mono<ResponseEntity> 返回登出結果
     */
    public Mono<ResponseEntity<?>> logout (ServerWebExchange exchange, boolean isWeb) {
        return userService.getUser(exchange).flatMap(user -> userService.logout(user.getId(), exchange).then(Mono.defer(() -> {
            if (isWeb) {
                ResponseCookie cookie = ResponseCookie
                        .from("jwtToken", "")
                        .httpOnly(true)
                        .secure(true)
                        .maxAge(0)
                        .sameSite("Strict")
                        .build();
                MultiValueMap<String, String> headers = new HttpHeaders();
                headers.add(HttpHeaders.SET_COOKIE, cookie.toString());
                return createResponseEntity(createResponse(exchange, "登出成功", null), headers);
            }
            return createResponseEntity(createResponse(exchange, "登出成功", null));
        }))).switchIfEmpty(createResponseEntity(createResponse(exchange, 401, "未認證", null)));

    }

    /**
     * 獲取所有用戶信息的請求
     *
     * @param exchange 請求對象
     *
     * @return Mono<ResponseEntity> 返回用戶信息
     */
    //todo 改成管理員使用
    public Flux<ResponseEntity<?>> getAllUserInfo (ServerWebExchange exchange) {
        return userService.getAll().flatMap(user -> {
            ApiResponseDTO<?> responseEntity = createResponse(exchange, "獲取用户信息成功", user);
            return createResponseEntity(responseEntity);
        }).onErrorResume(ValidationException.class, e -> {
            String errorMessage = String.format("獲取用户信息失敗: %s", e.getMessage());
            int errorCode = e.getErrorCode().getCode();
            return createResponseEntity(createResponse(exchange, errorCode, errorMessage, null));
        });
    }

    /**
     * 獲取所有用戶信息的請求
     *
     * @param exchange 請求對象
     *
     * @return Mono<ResponseEntity> 返回用戶信息
     */
    // 此方法為示例方法
    public Mono<ResponseEntity<?>> getUserInfo (ServerWebExchange exchange, @RequestParam("userid") Long id) {
        return userService.getById(id).flatMap(user -> {
            ApiResponseDTO<?> responseEntity = createResponse(exchange, "獲取用户信息成功", user);
            return createResponseEntity(responseEntity);
        }).onErrorResume(ValidationException.class, e -> {
            String errorMessage = String.format("獲取用户信息失敗: %s", e.getMessage());
            int errorCode = e.getErrorCode().getCode();
            return createResponseEntity(createResponse(exchange, errorCode, errorMessage, null));
        });
    }
}
