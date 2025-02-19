package xyz.dowob.filemanagement.controller.base;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.component.strategy.FileServiceStrategy;
import xyz.dowob.filemanagement.config.properties.SecurityProperties;
import xyz.dowob.filemanagement.data.api.ApiResponseDTO;
import xyz.dowob.filemanagement.service.serviceInterface.UserService;
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
    protected final FileServiceStrategy fileServiceStrategy;

    protected final UserService userService;

    protected final SecurityProperties securityProperties;

    /**
     * 用戶登出的請求
     *
     * @param exchange 處理用戶登出的請求
     *
     * @return Mono<ResponseEntity> 返回登出結果
     */
    public Mono<ResponseEntity<?>> logout(ServerWebExchange exchange, boolean isWeb) {
        return userService.getUser(exchange).flatMap(user -> userService.logout(user.getId(), exchange).then(Mono.defer(() -> {
            if (isWeb) {
                ResponseCookie cookie = ResponseCookie
                        .from("jwtToken", "")
                        .httpOnly(securityProperties.getCookie().isHttpOnly())
                        .secure(securityProperties.getCookie().isSecure())
                        .maxAge(0)
                        .sameSite(securityProperties.getCookie().getSameSite())
                        .path("/")
                        .build();
                exchange.getResponse().addCookie(cookie);
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
    public Mono<ResponseEntity<?>> getAllUserInfo(ServerWebExchange exchange) {
        return handleError(userService.getAll().collectList().flatMap(userList -> {
            ApiResponseDTO<?> responseEntity = createResponse(exchange, "獲取用户信息成功", userList);
            return createResponseEntity(responseEntity);
        }), exchange);
    }

    /**
     * 獲取所有用戶信息的請求
     *
     * @param exchange 請求對象
     *
     * @return Mono<ResponseEntity> 返回用戶信息
     */
    // 此方法為管理員方法
    public Mono<ResponseEntity<?>> getUserInfo(ServerWebExchange exchange) {
        return handleError(userService.getUser(exchange).flatMap(user -> {
            ApiResponseDTO<?> responseEntity = createResponse(exchange, "獲取用户信息成功", user);
            return createResponseEntity(responseEntity);
        }), exchange);
    }
}
