package xyz.dowob.filemanagement.controller.exception;

import io.r2dbc.spi.R2dbcException;
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
import xyz.dowob.filemanagement.annotation.RecordLevel;
import xyz.dowob.filemanagement.customenum.LogLevelEnum;
import xyz.dowob.filemanagement.data.response.ApiResponseDTO;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.unity.LogUnity;
import xyz.dowob.filemanagement.unity.ResponseUnity;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * WebFlux 全域異常處理控制器，攔截並統一處理應用程式異常。
 *
 * <p>基於 Spring WebFlux 的反應式異常處理機制，為所有未處理的異常提供統一響應格式。
 * 實現對驗證異常、HTTP 狀態異常、資料庫異常及系統異常的精確捕獲與處理。</p>
 *
 * <p>異常處理涵蓋驗證失敗、資源不存在、方法不支持、媒體類型錯誤、參數轉換失敗、
 * 資料綁定錯誤、JSON 格式錯誤、資料庫操作異常及重試耗盡等情況。所有響應使用統一的
 * {@link ApiResponseDTO} 格式，確保錯誤信息的一致性與安全性。</p>
 *
 * <p>身份驗證與權限異常由 Spring Security 機制獨立處理，不在此控制器範圍內。
 * 敏感系統信息經過過濾處理，避免直接暴露技術實現細節。</p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see ResponseUnity
 * @see ApiResponseDTO
 */
@RestControllerAdvice
@RecordLevel(LogLevelEnum.ERROR)
public class ExceptionController implements ResponseUnity {
    /**
     * 處理權限 AOP 階段的驗證異常，在方法執行前攔截並處理驗證失敗。
     *
     * <p>專門處理權限驗證階段產生的 {@link ValidationException}，此時尚未進入控制器方法，
     * 因此無法使用控制器內建的驗證處理機制。回傳 HTTP 400 狀態與詳細錯誤信息。</p>
     *
     * @param ex 驗證異常，包含錯誤代碼與訊息
     * @param exchange 當前的 Web 交換對象
     * @return 包含驗證錯誤詳情的響應實體
     */
    @RecordLevel(LogLevelEnum.DEBUG)
    @ExceptionHandler({ValidationException.class})
    public Mono<ResponseEntity<?>> handleValidationException(ValidationException ex, ServerWebExchange exchange) {
        ApiResponseDTO<Void> apiResponseDTO = ApiResponseDTO
                .<Void>builder()
                .timestamp(LocalDateTime.now())
                .status(ex.getErrorCode().getCode())
                .path(exchange.getRequest().getURI().getPath())
                .message(ex.getErrorCode().getMessage())
                .data(null)
                .build();

        return createResponseEntity(apiResponseDTO, ex.getErrorCode().getHttpStatus().value());
    }


    /**
     * 處理資源不存在異常，回傳 HTTP 404 狀態。
     *
     * <p>捕獲 {@link NoResourceFoundException} 與 {@link ResponseStatusException}，
     * 處理請求路徑不存在或資源無法找到的情況。</p>
     *
     * @param ex 資源不存在異常
     * @param exchange 當前的 Web 交換對象
     * @return 包含 404 錯誤信息的響應實體
     */
    @RecordLevel(LogLevelEnum.DEBUG)
    @ExceptionHandler({NoResourceFoundException.class, ResponseStatusException.class})
    public Mono<ResponseEntity<?>> handleNotFound(Exception ex, ServerWebExchange exchange) {
        String requestUrl = exchange.getRequest().getURI().getPath();
        LogUnity.debug(exchange, "發生404錯誤: %s, 錯誤的請求位置: %s", ex.getMessage(), requestUrl);

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
     * 處理不支持的 HTTP 方法異常，回傳 HTTP 405 狀態。
     *
     * <p>當請求使用的 HTTP 方法（GET、POST、PUT、DELETE 等）不被目標端點支持時觸發。</p>
     *
     * @param ex HTTP 方法不支持異常
     * @param exchange 當前的 Web 交換對象
     * @return 包含方法不支持錯誤信息的響應實體
     */
    @RecordLevel(LogLevelEnum.DEBUG)
    @ExceptionHandler(MethodNotAllowedException.class)
    public Mono<ResponseEntity<?>> handleHttpRequestMethodNotSupportedException(MethodNotAllowedException ex, ServerWebExchange exchange) {
        LogUnity.debug(exchange, "不支持的請求方法: %s", ex.getMessage());
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
     * 處理不支持的媒體類型異常，回傳 HTTP 415 狀態。
     *
     * <p>當請求的 Content-Type 頭部指定的媒體類型不被端點接受時觸發，
     * 例如端點期望 JSON 但收到 XML 格式。</p>
     *
     * @param ex 媒體類型不支持異常
     * @param exchange 當前的 Web 交換對象
     * @return 包含媒體類型錯誤信息的響應實體
     */
    @RecordLevel(LogLevelEnum.DEBUG)
    @ExceptionHandler(UnsupportedMediaTypeStatusException.class)
    public Mono<ResponseEntity<?>> handleUnsupportedMediaTypeStatusException(UnsupportedMediaTypeStatusException ex, ServerWebExchange exchange) {
        LogUnity.debug(exchange, "不支持的媒體類型: %s", ex.getMessage());
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
     * 處理參數類型轉換失敗異常，回傳 HTTP 400 狀態與詳細轉換錯誤信息。
     *
     * <p>當請求參數無法轉換為目標類型時觸發，例如字串無法轉換為數字。
     * 使用正則表達式解析異常信息，提取來源類型、目標類型與錯誤值，
     * 生成友好的錯誤提示信息。</p>
     *
     * @param ex 類型轉換失敗異常
     * @param exchange 當前的 Web 交換對象
     * @return 包含轉換錯誤詳情的響應實體
     */
    @RecordLevel(LogLevelEnum.DEBUG)
    @ExceptionHandler(ConversionFailedException.class)
    public Mono<ResponseEntity<?>> handleConversionFailException(ConversionFailedException ex, ServerWebExchange exchange) {
        LogUnity.debug(exchange, "轉換類型時發生錯誤: %s", ex.getMessage());
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
     * 處理資料綁定與驗證異常，回傳 HTTP 400 狀態與字段級錯誤詳情。
     *
     * <p>處理 {@code @Validated} 與 {@code @Valid} 注解觸發的驗證失敗，以及缺少必要請求參數的情況。
     * 對於綁定異常，收集所有字段錯誤並構建詳細錯誤映射；對於缺少參數異常，
     * 解析異常信息提取缺少的參數名稱。</p>
     *
     * @param ex 資料綁定異常或缺少請求值異常
     * @param exchange 當前的 Web 交換對象
     * @return 包含驗證錯誤詳情的響應實體
     */
    @RecordLevel(LogLevelEnum.DEBUG)
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

        LogUnity.debug(exchange, "資料驗證失敗，錯誤原因: %s", errors);
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
     * 處理伺服器 Web 輸入異常，回傳 HTTP 400 狀態。
     *
     * <p>當請求體無法被正確解析時觸發，常見於 JSON 格式錯誤、
     * 請求體為空或包含無效字符等情況。</p>
     *
     * @param ex 伺服器 Web 輸入異常
     * @param exchange 當前的 Web 交換對象
     * @return 包含輸入格式錯誤信息的響應實體
     */
    @RecordLevel(LogLevelEnum.DEBUG)
    @ExceptionHandler(ServerWebInputException.class)
    public Mono<ResponseEntity<?>> handleInvalidJsonException(ServerWebInputException ex, ServerWebExchange exchange) {
        LogUnity.debug(exchange, "用戶輸入的JSON 格式錯誤，錯誤: %s", ex.getMessage());

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
     * 處理不支持的操作異常，回傳 HTTP 400 狀態。
     *
     * <p>當執行的操作不被當前實現支持時觸發，例如呼叫未實現的方法或
     * 在不可修改的集合上執行修改操作。</p>
     *
     * @param ex 不支持的操作異常
     * @param exchange 當前的 Web 交換對象
     * @return 包含操作不支持錯誤信息的響應實體
     */
    @RecordLevel(LogLevelEnum.DEBUG)
    @ExceptionHandler(UnsupportedOperationException.class)
    public Mono<ResponseEntity<?>> handleUnsupportedOperationException(UnsupportedOperationException ex, ServerWebExchange exchange) {
        LogUnity.debug(exchange, "不支持的操作: %s", ex.getMessage());

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
     * 處理 R2DBC 資料庫操作異常，回傳 HTTP 500 狀態。
     *
     * <p>處理反應式資料庫操作失敗，可能由連接問題、SQL 語法錯誤、
     * 操作頻率過高或資料庫資源耗盡等原因引起。錯誤響應包含請求 ID
     * 以便問題追蹤與除錯。</p>
     *
     * @param ex R2DBC 資料庫異常
     * @param exchange 當前的 Web 交換對象
     * @return 包含資料庫錯誤信息與請求 ID 的響應實體
     */
    @RecordLevel(LogLevelEnum.ERROR)
    @ExceptionHandler(R2dbcException.class)
    public Mono<ResponseEntity<?>> handleR2dbcException(R2dbcException ex, ServerWebExchange exchange) {
        LogUnity.error(exchange, "R2dbc 錯誤: ", ex);

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
     * 處理非暫時性資料存取資源異常，回傳 HTTP 500 狀態。
     *
     * <p>處理資料庫連接池耗盡、資料庫服務不可用或持久性配置錯誤等
     * 無法通過重試解決的資料存取問題。提供請求 ID 以便系統管理員追蹤問題。</p>
     *
     * @param ex 非暫時性資料存取資源異常
     * @param exchange 當前的 Web 交換對象
     * @return 包含資料存取錯誤信息與請求 ID 的響應實體
     */
    @RecordLevel(LogLevelEnum.ERROR)
    @ExceptionHandler(NonTransientDataAccessResourceException.class)
    private Mono<ResponseEntity<?>> handleDatabaseException(Throwable ex, ServerWebExchange exchange) {
        LogUnity.error(exchange, "資料庫操作錯誤: ", ex);

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
     * 處理所有未被特定處理器捕獲的異常，提供最終異常處理保障。
     *
     * <p>對特殊類型異常進行分類處理，目前識別重試耗盡異常並提供專門處理，
     * 其餘未知異常統一處理為伺服器內部錯誤。確保所有異常都能得到適當響應，
     * 避免系統產生未處理的異常。</p>
     *
     * @param ex 任意類型的異常
     * @param exchange 當前的 Web 交換對象
     * @return 根據異常類型決定的響應實體
     */
    @RecordLevel(LogLevelEnum.ERROR)
    @ExceptionHandler(Throwable.class)
    public Mono<ResponseEntity<?>> handleException(Throwable ex, ServerWebExchange exchange) {
        if (Exceptions.isRetryExhausted(ex)) {
            return handleRetryExhaustedException(ex, exchange);
        }
        return handleUnknownException(ex, exchange);
    }


    /**
     * 處理重試耗盡異常，回傳 HTTP 429 狀態。
     *
     * <p>當反應式操作重試次數達到上限仍失敗時觸發，通常表示系統過載
     * 或下游服務不可用。回傳限速錯誤狀態提示客戶端稍後重試。</p>
     *
     * @param ex 重試耗盡異常
     * @param exchange 當前的 Web 交換對象
     * @return 包含操作限制錯誤信息的響應實體
     */
    private Mono<ResponseEntity<?>> handleRetryExhaustedException(Throwable ex, ServerWebExchange exchange) {
        LogUnity.error(exchange, "重試次數過多，錯誤: ", ex);

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
     * 處理未知異常，回傳 HTTP 500 狀態。
     *
     * <p>處理所有無法歸類的系統異常，記錄完整錯誤信息並生成請求 ID
     * 以便後續問題追蹤。確保敏感系統信息不會洩露給客戶端。</p>
     *
     * @param ex 未知異常
     * @param exchange 當前的 Web 交換對象
     * @return 包含通用錯誤信息與請求 ID 的響應實體
     */
    private Mono<ResponseEntity<?>> handleUnknownException(Throwable ex, ServerWebExchange exchange) {
        String requestId = exchange.getAttribute("requestId");
        LogUnity.error(exchange, "請求ID: %s 發生未知錯誤: ", ex, requestId);

        ApiResponseDTO<Void> apiResponseDTO = ApiResponseDTO
                .<Void>builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .path(exchange.getRequest().getURI().getPath())
                .message("伺服器內部處理錯誤，請聯繫管理員並附上此ID: " + requestId)
                .data(null)
                .build();

        return createResponseEntity(apiResponseDTO, HttpStatus.INTERNAL_SERVER_ERROR.value());
    }
}


