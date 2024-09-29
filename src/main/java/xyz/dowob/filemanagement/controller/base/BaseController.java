package xyz.dowob.filemanagement.controller.base;

/**
 * @author yuan
 * @program FileManagement
 * @ClassName BaseController
 * @description
 * @create 2024-09-23 19:44
 * @Version 1.0
 **/

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.component.strategy.FileStrategy;
import xyz.dowob.filemanagement.dto.api.ApiResponseDTO;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.service.ServiceInterface.UserService;

import java.time.LocalDateTime;

/**
 * 用於定義控制器的接口，為所有控制器的基礎
 * 內部提供通用的方法，用於處理請求的返回
 *
 * @author yuan
 * @program File-Management
 * @ClassName BaseController
 * @description
 * @create 2024-09-17 00:26
 * @Version 1.0
 **/
@RequiredArgsConstructor
public abstract class BaseController {
    protected final UserService userService;


    /**
     * 用於創建返回Mono<ResponseEntity>的方法，根據請求的結果創建對應的控制器可以處理的3位數狀態碼
     *
     * @param apiResponse  返回結果
     * @param responseCode 返回狀態碼 (3位數)
     *
     * @return Mono<ResponseEntity> 返回對應的Mono<ResponseEntity>
     */
    protected Mono<ResponseEntity<?>> createResponseEntity(ApiResponseDTO<?> apiResponse, int responseCode) {
        return Mono.just(ResponseEntity.status(responseCode).body(apiResponse));
    }

    /**
     * 用於創建返回ResponseEntity的方法，此為重載方法
     * 默認成功狀態碼為200，失敗狀態碼為400
     *
     * @param apiResponse 返回結果
     *
     * @return ResponseEntity 返回對應的ResponseEntity
     */
    protected Mono<ResponseEntity<?>> createResponseEntity(ApiResponseDTO<?> apiResponse) {
        int responseCode = apiResponse.getStatus() == 200 ? 200 : 400;
        return createResponseEntity(apiResponse, responseCode);
    }

    /**
     * 用於創建返回ApiResponseDTO的方法，根據請求的結果創建對應的ApiResponseDTO
     *
     * @param request 請求對象
     * @param status  狀態碼
     * @param message 返回消息
     * @param data    返回數據
     * @param <T>     泛型
     *
     * @return ApiResponseDTO 返回對應的ApiResponseDTO
     */
    protected <T> ApiResponseDTO<T> createResponse(ServerWebExchange request, int status, String message, T data) {
        return new ApiResponseDTO<>(LocalDateTime.now(), status, request.getRequest().getURI().getPath(), message, data);
    }

    /**
     * 用於創建返回ApiResponseDTO的方法，此為重載方法，默認狀態碼為200
     *
     * @param request 請求對象
     * @param message 返回消息
     * @param data    返回數據
     * @param <T>     泛型
     *
     * @return ApiResponseDTO 返回對應的ApiResponseDTO
     */
    protected <T> ApiResponseDTO<T> createResponse(ServerWebExchange request, String message, T data) {
        return new ApiResponseDTO<>(LocalDateTime.now(), 200, request.getRequest().getURI().getPath(), message, data);
    }


    /**
     * 用於獲取用戶的方法，根據請求對象獲取用戶對象
     * 先從Session中獲取用戶ID，如果Session中沒有則從SecurityContext中獲取
     * 當其中一個獲取到用戶ID時，則根據用戶ID獲取用戶對象
     * 如果都沒有獲取到用戶ID，則返回空
     *
     * @param exchange 請求對象
     *
     * @return Mono<User> 返回用戶對象
     */
    protected Mono<User> getUser(ServerWebExchange exchange) {
        final Object[] userId = new Object[1];
        return exchange.getSession().flatMap(webSession -> {
            userId[0] = webSession.getAttributes().get("userId");
            if (userId[0] == null) {
                return ReactiveSecurityContextHolder.getContext().map(SecurityContext::getAuthentication).flatMap(authentication -> {
                    if (authentication != null && authentication.isAuthenticated()) {
                        userId[0] = Long.valueOf(authentication.getPrincipal().toString());
                        return userService.getById((Long) userId[0]);
                    }
                    return Mono.empty();
                });
            }
            return userService.getById((Long) userId[0]);
        }).switchIfEmpty(Mono.empty());
    }

}