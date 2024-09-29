package xyz.dowob.filemanagement.controller.exception;

import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.resource.NoResourceFoundException;
import org.springframework.web.server.*;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.controller.base.BaseController;
import xyz.dowob.filemanagement.dto.api.ApiResponseDTO;
import xyz.dowob.filemanagement.service.ServiceInterface.UserService;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 自定義異常處理器，用於處理一些操作所異常
 * 此異常處理器繼承 BaseController，用於統一返回 ResponseEntity
 * 實現BaseController{@link BaseController}
 * 此類不處理驗證身分以及權限的錯誤，將其交由security的exceptionHandling處理
 *
 * @author yuan
 * @program File-Management
 * @ClassName WebExceptionController
 * @description
 * @create 2024-09-16 03:47
 * @Version 1.0
 **/
@Log4j2
@RestControllerAdvice
public class WebExceptionController extends BaseController {
    public WebExceptionController(UserService userService) {
        super(userService);
    }

    /**
     * 處理 404 錯誤，當請求的位置不存在時，返回一個 404 錯誤
     *
     * @param ex       Exception 異常
     * @param exchange ServerWebExchange 服務器 Web的請求
     *
     * @return Mono<ResponseEntity> 回應實體
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public Mono<ResponseEntity<?>> handleNotFound(Exception ex, ServerWebExchange exchange) {
        String requestUrl = exchange.getRequest().getURI().getPath();
        log.debug("發生404錯誤: {}, 錯誤的請求位置: {}", ex.getMessage(), requestUrl);

        ApiResponseDTO<Void> apiResponseDTO = new ApiResponseDTO<>(LocalDateTime.now(),
                                                                   HttpStatus.NOT_FOUND.value(),
                                                                   requestUrl,
                                                                   "請求位置不存在",
                                                                   null);

        return createResponseEntity(apiResponseDTO, HttpStatus.NOT_FOUND.value());
    }

    /**
     * 處理錯誤的請求方法，當請求的方法不正確時，返回一個 405 錯誤
     *
     * @param ex       MethodNotAllowedException 不支持的請求方法
     * @param exchange ServerWebExchange 服務器 Web的請求
     *
     * @return Mono<ResponseEntity> 回應實體
     */
    @ExceptionHandler(MethodNotAllowedException.class)
    public Mono<ResponseEntity<?>> handleHttpRequestMethodNotSupportedException(
            MethodNotAllowedException ex, ServerWebExchange exchange) {
        log.debug("不支持的請求方法: {}", ex.getMessage());
        ApiResponseDTO<Void> apiResponseDTO = new ApiResponseDTO<>(LocalDateTime.now(),
                                                                   HttpStatus.METHOD_NOT_ALLOWED.value(),
                                                                   exchange.getRequest().getURI().getPath(),
                                                                   "不支持的請求方法",
                                                                   null);
        return createResponseEntity(apiResponseDTO, HttpStatus.METHOD_NOT_ALLOWED.value());
    }

    @ExceptionHandler(UnsupportedMediaTypeStatusException.class)
    public Mono<ResponseEntity<?>> handleUnsupportedMediaTypeStatusException(
            UnsupportedMediaTypeStatusException ex, ServerWebExchange exchange) {
        log.debug("不支持的媒體類型: {}", ex.getMessage());
        ApiResponseDTO<Void> apiResponseDTO = new ApiResponseDTO<>(LocalDateTime.now(),
                                                                   HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(),
                                                                   exchange.getRequest().getURI().getPath(),
                                                                   "不支持的媒體類型",
                                                                   null);
        return createResponseEntity(apiResponseDTO, HttpStatus.UNSUPPORTED_MEDIA_TYPE.value());
    }


    /**
     * 處理資料驗證錯誤，此錯誤是由 @Validated 或 @Valid 注解引起的
     * 返回的錯誤信息是一個 Map，其中 key 是錯誤的字段名，value 是錯誤的原因
     *
     * @param ex       WebExchangeBindException或MissingRequestValueException 資料驗證錯誤
     * @param exchange ServerWebExchange 服務器 Web的請求
     *
     * @return Mono<ResponseEntity> 回應實體
     */
    @ExceptionHandler({WebExchangeBindException.class, MissingRequestValueException.class})
    public Mono<ResponseEntity<?>> handleValidationExceptions(Exception ex, ServerWebExchange exchange) {
        Map<String, String> errors = new HashMap<>();
        StringBuilder errorMessageBuilder = new StringBuilder();

        if (ex instanceof WebExchangeBindException bindException) {
            bindException.getBindingResult().getAllErrors().forEach((error) -> {
                String fieldName = ((FieldError) error).getField();
                String errorMessage = error.getDefaultMessage();
                errors.put(fieldName, errorMessage);
                errorMessageBuilder.append(errorMessage).append("、 ");
            });
            if (!errorMessageBuilder.isEmpty()) {
                errorMessageBuilder.setLength(errorMessageBuilder.length() - 2);
            }
        } else if (ex instanceof MissingRequestValueException missingRequestValueException) {
            String[] missingParams = Objects.requireNonNull(missingRequestValueException.getReason()).split(" ");
            if (missingParams.length > 4) {
                errorMessageBuilder.append("缺少請求參數:").append(missingParams[3]);
            }
        }

        log.debug("資料驗證失敗，錯誤原因：{}", errors);

        ApiResponseDTO<Void> apiResponseDTO = new ApiResponseDTO<>(LocalDateTime.now(),
                                                                   HttpStatus.BAD_REQUEST.value(),
                                                                   exchange.getRequest().getURI().getPath(),
                                                                   String.format("資料驗證失敗，錯誤原因：[%s]", errorMessageBuilder),
                                                                   null);
        return createResponseEntity(apiResponseDTO, HttpStatus.BAD_REQUEST.value());
    }

    /**
     * 處理請求格式錯誤
     *
     * @param ex       ServerWebInputException 傳入訊息無法讀取錯誤
     * @param exchange ServerWebExchange 服務器 Web的請求
     *
     * @return Mono<ResponseEntity> 回應實體
     */
    @ExceptionHandler(ServerWebInputException.class)
    public Mono<ResponseEntity<?>> handleInvalidJsonException(ServerWebInputException ex, ServerWebExchange exchange) {
        log.debug("JSON 格式錯誤，錯誤原因：{}", ex.getMessage());

        ApiResponseDTO<Void> apiResponseDTO = new ApiResponseDTO<>(LocalDateTime.now(),
                                                                   HttpStatus.BAD_REQUEST.value(),
                                                                   exchange.getRequest().getURI().getPath(),
                                                                   "請求的 JSON 格式無效",
                                                                   null);

        return createResponseEntity(apiResponseDTO, HttpStatus.BAD_REQUEST.value());
    }

    /**
     * 處理未知錯誤，當發生未知錯誤時，返回一個 500 錯誤
     *
     * @param ex       Throwable 異常
     * @param exchange ServerWebExchange 服務器 Web的請求
     *
     * @return Mono<ResponseEntity> 回應實體
     */
    @ExceptionHandler(Throwable.class)
    public Mono<ResponseEntity<?>> handleException(Throwable ex, ServerWebExchange exchange) {
        log.error("錯誤類型: {}", ex.getClass().getName());
        log.error("發生未知錯誤: {}", ex.getMessage());
        log.error("錯誤起因: ", ex.getCause());
        log.error("錯誤堆棧: ", ex);
        ApiResponseDTO<Void> apiResponseDTO = new ApiResponseDTO<>(LocalDateTime.now(),
                                                                   HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                                                   exchange.getRequest().getURI().getPath(),
                                                                   "伺服器內部處理錯誤",
                                                                   null);

        return createResponseEntity(apiResponseDTO, HttpStatus.INTERNAL_SERVER_ERROR.value());
    }
}
