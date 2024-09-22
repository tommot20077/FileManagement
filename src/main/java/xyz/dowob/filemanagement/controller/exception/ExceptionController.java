package xyz.dowob.filemanagement.controller.exception;

import io.r2dbc.spi.R2dbcException;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.dao.NonTransientDataAccessResourceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.resource.NoResourceFoundException;
import org.springframework.web.server.*;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.data.api.ApiResponseDTO;
import xyz.dowob.filemanagement.unity.ResponseUnity;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 自定義異常處理器，用於處理一些操作所異常
 * 此異常處理器繼承 ResponseUnity，用於統一返回 ResponseEntity
 * 實現BaseController{@link ResponseUnity}
 * 此類不處理驗證身分以及權限的錯誤，將其交由security的exceptionHandling處理
 *
 * @author yuan
 * @program File-Management
 * @ClassName ExceptionController
 * @description
 * @create 2024-09-16 03:47
 * @Version 1.0
 **/
@Log4j2
@RestControllerAdvice
public class ExceptionController implements ResponseUnity {

    /**
     * 處理 404 錯誤，當請求的位置不存在時，返回一個 404 錯誤
     *
     * @param ex       Exception 異常
     * @param exchange ServerWebExchange 服務器 Web的請求
     *
     * @return Mono<ResponseEntity> 回應實體
     */
    @ExceptionHandler({NoResourceFoundException.class, ResponseStatusException.class})
    public Mono<ResponseEntity<?>> handleNotFound(Exception ex, ServerWebExchange exchange) {
        String requestUrl = exchange.getRequest().getURI().getPath();
        log.debug("發生404錯誤: {}, 錯誤的請求位置: {}", ex.getMessage(), requestUrl);

        ApiResponseDTO<Void> apiResponseDTO = ApiResponseDTO
                .<Void>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .path(requestUrl)
                .message("請求的資源不存在")
                .data(null)
                .build();

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
    public Mono<ResponseEntity<?>> handleHttpRequestMethodNotSupportedException(MethodNotAllowedException ex, ServerWebExchange exchange) {
        log.debug("不支持的請求方法: {}", ex.getMessage());
        ApiResponseDTO<Void> apiResponseDTO = ApiResponseDTO
                .<Void>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.METHOD_NOT_ALLOWED.value())
                .path(exchange.getRequest().getURI().getPath())
                .message("不支持的請求方法")
                .data(null)
                .build();

        return createResponseEntity(apiResponseDTO, HttpStatus.METHOD_NOT_ALLOWED.value());
    }


    /**
     * 處理不支持的媒體類型，當請求的媒體類型不正確時，返回一個 415 錯誤
     *
     * @param ex       UnsupportedMediaTypeStatusException 不支持的媒體類型
     * @param exchange ServerWebExchange 服務器 Web的請求
     *
     * @return Mono<ResponseEntity> 回應實體
     */
    @ExceptionHandler(UnsupportedMediaTypeStatusException.class)
    public Mono<ResponseEntity<?>> handleUnsupportedMediaTypeStatusException(UnsupportedMediaTypeStatusException ex, ServerWebExchange exchange) {
        log.debug("不支持的媒體類型: {}", ex.getMessage());
        ApiResponseDTO<Void> apiResponseDTO = ApiResponseDTO
                .<Void>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value())
                .path(exchange.getRequest().getURI().getPath())
                .message("不支持的媒體類型")
                .data(null)
                .build();

        return createResponseEntity(apiResponseDTO, HttpStatus.UNSUPPORTED_MEDIA_TYPE.value());
    }


    /**
     * 處理參數轉換錯誤，當請求參數無法轉換時，返回一個 400 錯誤，並提示錯誤的參數
     *
     * @param ex       ConversionFailedException 轉換類型時發生錯誤
     * @param exchange ServerWebExchange 服務器 Web的請求
     *
     * @return Mono<ResponseEntity> 回應實體
     */
    @ExceptionHandler(ConversionFailedException.class)
    public Mono<ResponseEntity<?>> handleConversionFailException(ConversionFailedException ex, ServerWebExchange exchange) {
        log.debug("轉換類型時發生錯誤: {}", ex.getMessage());
        String errorMessage;
        Pattern pattern = Pattern.compile("Failed to convert from type \\[(.*?)\\] to type \\[(.*?)\\] for value \\[(.*?)\\]");
        Matcher matcher = pattern.matcher(ex.getMessage());
        if (matcher.find()) {
            errorMessage = String.format("轉換類型時發生問題，從類型[%s]轉換到類型[%s]時發生錯誤，請檢查請求參數: %s",
                                         matcher.group(1),
                                         matcher.group(2),
                                         matcher.group(3)
            );
        } else {
            errorMessage = "轉換類型時發生問題，請檢查請求參數";
        }

        ApiResponseDTO<Void> apiResponseDTO = ApiResponseDTO
                .<Void>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .path(exchange.getRequest().getURI().getPath())
                .message(errorMessage)
                .data(null)
                .build();

        return createResponseEntity(apiResponseDTO, HttpStatus.BAD_REQUEST.value());
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
        ApiResponseDTO<Void> apiResponseDTO = ApiResponseDTO
                .<Void>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .path(exchange.getRequest().getURI().getPath())
                .message(String.format("資料驗證失敗，錯誤原因：[%s]", errorMessageBuilder))
                .data(null)
                .build();

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
        log.debug("用戶輸入的JSON 格式錯誤，錯誤: {}", ex.getMessage());

        ApiResponseDTO<Void> apiResponseDTO = ApiResponseDTO
                .<Void>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .path(exchange.getRequest().getURI().getPath())
                .message("JSON 格式錯誤")
                .data(null)
                .build();

        return createResponseEntity(apiResponseDTO, HttpStatus.BAD_REQUEST.value());
    }


    /**
     * 處理不支持的操作
     *
     * @param ex       UnsupportedOperationException 不支持的操作
     * @param exchange ServerWebExchange 服務器 Web的請求
     *
     * @return Mono<ResponseEntity> 回應實體
     */
    @ExceptionHandler(UnsupportedOperationException.class)
    public Mono<ResponseEntity<?>> handleUnsupportedOperationException(UnsupportedOperationException ex, ServerWebExchange exchange) {
        log.debug("不支持的操作: {}", ex.getMessage());
        ApiResponseDTO<Void> apiResponseDTO = ApiResponseDTO
                .<Void>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .path(exchange.getRequest().getURI().getPath())
                .message("不支持的操作")
                .data(null)
                .build();

        return createResponseEntity(apiResponseDTO, HttpStatus.BAD_REQUEST.value());
    }


    /**
     * 處理 R2dbc資料庫操作錯誤
     * 發生此異常可能是因為操作過於頻繁，導致資料庫操作失敗
     *
     * @param ex       R2dbcException R2dbc 錯誤
     * @param exchange ServerWebExchange 服務器 Web的請求
     *
     * @return Mono<ResponseEntity> 回應實體
     */
    @ExceptionHandler(R2dbcException.class)
    public Mono<ResponseEntity<?>> handleR2dbcException(R2dbcException ex, ServerWebExchange exchange) {
        log.error("R2dbc 錯誤: ", ex);
        ApiResponseDTO<Void> response = ApiResponseDTO
                .<Void>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .path(exchange.getRequest().getURI().getPath())
                .message("超出操作限制，請稍後再試，若問題持續請聯繫管理員並附上此ID: " + exchange.getAttribute("requestId"))
                .data(null)
                .build();

        return Mono.just(ResponseEntity.status(500).body(response));
    }


    /**
     * 處理資料庫操作錯誤
     * 發生此異常可能是因為操作過於頻繁，導致資料庫操作失敗
     *
     * @param ex       NonTransientDataAccessResourceException 資料庫操作錯誤
     * @param exchange ServerWebExchange 服務器 Web的請求
     *
     * @return Mono<ResponseEntity> 回應實體
     */
    @ExceptionHandler(NonTransientDataAccessResourceException.class)
    private Mono<ResponseEntity<?>> handleDatabaseException(Throwable ex, ServerWebExchange exchange) {
        log.error("資料庫操作錯誤: ", ex);
        ApiResponseDTO<Void> response = ApiResponseDTO
                .<Void>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .path(exchange.getRequest().getURI().getPath())
                .message("操作失敗，請稍後再試，若問題持續請聯繫管理員並附上此ID: " + exchange.getAttribute("requestId"))
                .data(null)
                .build();

        return Mono.just(ResponseEntity.status(500).body(response));
    }


    /**
     * 處理其他異常，部分異常為該錯誤類的內部類，無法直接捕獲
     * 此處對該異常進行過濾並進行處理，當都無法處理時，返回未知錯誤
     * 此類目前過濾
     * 1. 重試次數過多的異常 {@link Exceptions#isRetryExhausted(Throwable)}
     *
     * @param ex       Throwable 異常
     * @param exchange ServerWebExchange 服務器 Web的請求
     *
     * @return Mono<ResponseEntity> 回應實體
     */
    @ExceptionHandler(Throwable.class)
    public Mono<ResponseEntity<?>> handleException(Throwable ex, ServerWebExchange exchange) {
        if (Exceptions.isRetryExhausted(ex)) {
            return handleRetryExhaustedException(ex, exchange);
        }
        return handleUnknownException(ex, exchange);
    }


    /**
     * 處理重試次數過多的異常
     *
     * @param ex       重試次數過多的異常
     * @param exchange 服務器 Web的請求
     *
     * @return Mono<ResponseEntity> 回應實體
     */
    private Mono<ResponseEntity<?>> handleRetryExhaustedException(Throwable ex, ServerWebExchange exchange) {
        log.debug("重試次數過多，錯誤起因: ", ex.getCause());
        ApiResponseDTO<Void> response = ApiResponseDTO
                .<Void>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .path(exchange.getRequest().getURI().getPath())
                .message("超出操作限制，請稍後再試，若問題持續請聯繫管理員並附上此ID: " + exchange.getAttribute("requestId"))
                .data(null)
                .build();

        return Mono.just(ResponseEntity.status(429).body(response));
    }


    /**
     * 處理未知異常
     *
     * @param ex       Throwable 異常
     * @param exchange ServerWebExchange 服務器 Web的請求
     *
     * @return Mono<ResponseEntity> 回應實體
     */
    private Mono<ResponseEntity<?>> handleUnknownException(Throwable ex, ServerWebExchange exchange) {
        String requestId = exchange.getAttribute("requestId");
        log.error("請求ID: {} 發生未知錯誤", requestId, ex);
        ApiResponseDTO<Void> apiResponseDTO = ApiResponseDTO
                .<Void>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .path(exchange.getRequest().getURI().getPath()).message("伺服器內部處理錯誤，請聯繫管理員並附上此ID: " + requestId)
                .data(null)
                .build();

        return createResponseEntity(apiResponseDTO, HttpStatus.INTERNAL_SERVER_ERROR.value());
    }
}


