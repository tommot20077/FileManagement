package xyz.dowob.filemanagement.controller.base;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.RequirePermission;
import xyz.dowob.filemanagement.component.strategy.FileServiceStrategy;
import xyz.dowob.filemanagement.config.properties.SecurityProperties;
import xyz.dowob.filemanagement.customenum.PermissionEnum;
import xyz.dowob.filemanagement.customenum.UserInfoTypeEnum;
import xyz.dowob.filemanagement.data.api.ApiResponseDTO;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.service.serviceInterface.UserService;
import xyz.dowob.filemanagement.service.serviceInterface.ValidationService;
import xyz.dowob.filemanagement.unity.ResponseUnity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * 用戶控制器的基礎類
 * 此類提供用戶控制器的基本方法，並交由子類繼承方法，減少代碼重複。它實現了 {@link ResponseUnity} 接口。
 * 此類處理請求過程中發生的 ValidationException 異常，並返回對應的錯誤信息。
 * 主要功能包括處理用戶的登出、查詢用戶信息等操作。
 *
 * @author yuan
 * @program File-Management
 * @ClassName BaseUserController
 * @create 2024-09-17 00:23
 * @Version 1.0
 */
@RequiredArgsConstructor
public abstract class BaseUserController implements ResponseUnity {

    /**
     * 文件策略，用於選擇適當的文件服務
     */
    protected final FileServiceStrategy fileServiceStrategy;

    /**
     * 用戶業務層對象，負責處理用戶相關業務邏輯
     */
    protected final UserService userService;

    /**
     * 安全性設定，用於設置安全相關配置，如 Cookie 配置
     */
    protected final SecurityProperties securityProperties;

    protected final ValidationService validationService;

    /**
     * 處理用戶登出的請求
     * 登出用戶並清除 JWT Token，若是 Web 請求，會清除瀏覽器的登錄 Cookie。
     * 如果用戶未認證，將返回未認證的錯誤信息。
     *
     * @param exchange 處理登出請求的 Web 交換對象
     * @param isWeb    是否為 Web 登出請求
     *
     * @return Mono<ResponseEntity < ?>> 返回登出結果，若登出成功，則返回成功消息，若未認證則返回錯誤消息
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
     * 獲取所有用戶信息的請求（管理員使用）
     * 該方法用於管理員查詢所有用戶的信息。
     *
     * @param exchange 請求對象
     *
     * @return Mono<ResponseEntity < ?>> 返回所有用戶的信息，若成功則返回用戶信息列表
     */
    @RequirePermission(PermissionEnum.MANAGE)
    public Mono<ResponseEntity<?>> getAllUserInfo(ServerWebExchange exchange) {
        return handleError(userService.getAll().collectList().flatMap(userList -> {
            ApiResponseDTO<?> responseEntity = createResponse(exchange, "獲取用户信息成功", userList);
            return createResponseEntity(responseEntity);
        }), exchange);
    }


    /**
     * 獲取當前用戶信息的請求
     * 該方法用於查詢當前認證用戶的詳細信息。
     *
     * @param exchange 請求對象
     *
     * @return Mono<ResponseEntity < ?>> 返回當前用戶的詳細信息
     */
    public Mono<ResponseEntity<?>> getUserInfo(ServerWebExchange exchange) {
        return handleError(userService.getUser(exchange).flatMap(user -> {
            ApiResponseDTO<?> responseEntity = createResponse(exchange, "獲取用户信息成功", user);
            return createResponseEntity(responseEntity);
        }), exchange);
    }


    /**
     * 查詢用戶信息的請求
     * 該方法用於查詢指定用戶的詳細信息。
     *
     * @param exchange  請求對象
     * @param userInfos 用戶名
     * @param type      查詢類型
     *
     * @return Mono<ResponseEntity < ?>> 返回指定用戶的詳細信息
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
                    ApiResponseDTO<?> responseEntity = createResponse(exchange, "獲取用户信息成功", result);
                    return createResponseEntity(responseEntity);
                }));
        return handleError(entityMono, exchange);
    }
}
