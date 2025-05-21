package xyz.dowob.filemanagement.unity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.data.response.ApiResponseDTO;
import xyz.dowob.filemanagement.data.response.WebSocketResponse;
import xyz.dowob.filemanagement.exception.LimitationException;
import xyz.dowob.filemanagement.exception.ProcessException;
import xyz.dowob.filemanagement.exception.ValidationException;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 用於定義控制器的接口，為所有控制器的基礎
 * 內部提供通用的方法，用於處理請求的返回
 *
 * @author yuan
 * @program File-Management
 * @ClassName ResponseUnity
 * @description
 * @create 2024-09-17 00:26
 * @Version 1.0
 **/
public interface ResponseUnity {
    /**
     * 用於創建返回ResponseEntity的方法，此為重載方法
     * 默認成功狀態碼為200，失敗狀態碼為400
     *
     * @param apiResponse 返回結果
     *
     * @return ResponseEntity 返回對應的ResponseEntity
     */
    default Mono<ResponseEntity<?>> createResponseEntity(ApiResponseDTO<?> apiResponse) {
        int responseCode = apiResponse.getStatus() == 200 ? 200 : 400;
        return createResponseEntity(apiResponse, responseCode);
    }


    /**
     * 用於創建返回Mono<ResponseEntity>的方法，根據請求的結果創建對應的控制器可以處理的3位數狀態碼
     *
     * @param apiResponse  返回結果
     * @param responseCode 返回狀態碼 (3位數)
     *
     * @return Mono<ResponseEntity> 返回對應的Mono<ResponseEntity>
     */
    default Mono<ResponseEntity<?>> createResponseEntity(ApiResponseDTO<?> apiResponse, Integer responseCode) {
        return Mono.just(ResponseEntity.status(Objects.requireNonNullElse(responseCode, 200)).body(apiResponse));
    }


    /**
     * 用於創建返回ResponseEntity的方法，此為重載方法
     * 根據ApiResponseDTO的狀態碼創建對應的ResponseEntity
     * 同時返回對應的頭部
     *
     * @param apiResponse 返回結果
     * @param headers     返回頭部
     *
     * @return ResponseEntity 返回對應的ResponseEntity
     */
    default Mono<ResponseEntity<?>> createResponseEntity(ApiResponseDTO<?> apiResponse, MultiValueMap<String, String> headers) {
        return Mono.just(new ResponseEntity<>(apiResponse, headers, apiResponse.getStatus()));
    }


    /**
     * 用於創建返回ApiResponseDTO的方法，此為重載方法
     * 適用指定路徑的請求
     *
     * @param status  狀態碼
     * @param message 返回消息
     * @param data    返回數據
     * @param <T>     泛型
     *
     * @return ApiResponseDTO 返回對應的ApiResponseDTO
     */
    default <T> ApiResponseDTO<T> createApiResponse(String path, int status, String message, T data) {
        return new ApiResponseDTO<>(LocalDateTime.now(), status, path, message, data);
    }


    /**
     * 用於創建返回ApiResponseDTO的方法，此為重載方法
     * 適用WebSocket請求
     *
     * @param session WebSocketSession
     * @param status  狀態碼
     * @param message 返回消息
     * @param data    返回數據
     * @param <T>     泛型
     *
     * @return ApiResponseDTO 返回對應的ApiResponseDTO
     */
    default <T> ApiResponseDTO<T> createApiResponse(WebSocketSession session, int status, String message, T data) {
        return new ApiResponseDTO<>(LocalDateTime.now(), status, session.getHandshakeInfo().getUri().getPath(), message, data);
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
    default <T> ApiResponseDTO<T> createApiResponse(ServerWebExchange request, String message, T data) {
        return new ApiResponseDTO<>(LocalDateTime.now(), 200, request.getRequest().getURI().getPath(), message, data);
    }


    /**
     * 用於創建返回ApiResponseDTO的方法，此為重載方法，默認狀態碼為200，適用指定路徑的請求
     *
     * @param message 返回消息
     * @param data    返回數據
     * @param <T>     泛型
     *
     * @return ApiResponseDTO 返回對應的ApiResponseDTO
     */
    default <T> ApiResponseDTO<T> createApiResponse(String path, String message, T data) {
        return new ApiResponseDTO<>(LocalDateTime.now(), 200, path, message, data);
    }


    /**
     * 用於創建返回WebSocketResponse的方法
     *
     * @param <T>     訊息類型
     * @param message 返回消息
     * @param data    返回數據
     *
     * @return ApiResponseDTO 返回對應的WebSocketResponse
     */
    default <T> WebSocketResponse<T> createWebSocketResponse(T type, String message, Object data) {
        return new WebSocketResponse<>(LocalDateTime.now(), type, message, data);
    }


    /**
     * 用於創建返回WebSocketResponse的方法，此為重載方法，無傳輸資料，適用WebSocket請求
     *
     * @param <T>     訊息類型
     * @param message 返回消息
     *
     * @return ApiResponseDTO 返回對應的WebSocketResponse
     */
    default <T> WebSocketResponse<T> createWebSocketResponse(T type, String message) {
        return new WebSocketResponse<>(LocalDateTime.now(), type, message, null);
    }


    /**
     * 統一處理錯誤
     *
     * @param operation 要執行的操作
     * @param exchange  請求對象
     *
     * @return 處理後的 ResponseEntity
     */
    default Mono<ResponseEntity<?>> handleError(Mono<ResponseEntity<?>> operation, ServerWebExchange exchange) {
        return operation.onErrorResume(ValidationException.class, validationException -> {
                                           String errorMessage = String.format("處理失敗: %s", validationException.getMessage());
                                           ApiResponseDTO<?> apiResponse = createApiResponse(exchange, validationException.getErrorCode().getCode(), errorMessage, null);
                                           return createResponseEntity(apiResponse, validationException.getErrorCode().getHttpStatus().value());
                                       }
        ).onErrorResume(LimitationException.class, limitationException -> {
                            String errorMessage = String.format("限制錯誤: %s", limitationException.getMessage());
                            ApiResponseDTO<?> apiResponse = createApiResponse(exchange, limitationException.getErrorCode().getCode(), errorMessage, null);
                            return createResponseEntity(apiResponse, limitationException.getErrorCode().getHttpStatus().value());
                        }
        );
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
    default <T> ApiResponseDTO<T> createApiResponse(ServerWebExchange request, int status, String message, T data) {
        return new ApiResponseDTO<>(LocalDateTime.now(), status, request.getRequest().getURI().getPath(), message, data);
    }


    /**
     * 將自定義的 ApiResponseDTO 轉換為 JSON 格式的響應消息並寫入響應
     * 此方法用於處理 LimitationException 錯誤
     *
     * @param exchange     請求交換對象
     * @param objectMapper 用於將 ApiResponseDTO 轉換為 JSON 的 ObjectMapper
     * @param error        錯誤信息
     * @param errorMessage 錯誤消息
     *
     * @return Mono<Void>
     */
    default Mono<Void> sendErrorResponse(ServerWebExchange exchange, ObjectMapper objectMapper, LimitationException.ErrorCode error, String errorMessage) {
        return sendErrorResponse(exchange, objectMapper, errorMessage, error.getCode(), error.getHttpStatus());
    }

    /**
     * 給定一個錯誤的狀態碼、錯誤消息和請求對象將其轉換成自定義的 ApiResponseDTO
     * 並將 JSON 格式的響應消息並寫入請求交換對象
     *
     * @param exchange     請求交換對象
     * @param objectMapper 用於將 ApiResponseDTO 轉換為 JSON 的 ObjectMapper
     * @param errorMessage 錯誤消息
     * @param errorCode    錯誤碼
     * @param httpStatus   HTTP 狀態碼
     *
     * @return Mono<Void>
     */
    default Mono<Void> sendErrorResponse(ServerWebExchange exchange, ObjectMapper objectMapper, String errorMessage, int errorCode, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        if (response.isCommitted()) {
            LogUnity.warn(exchange, "響應已提交，無法再次設置狀態碼或頭部，錯誤碼: %s, 訊息: %s", errorCode, errorMessage);
            return Mono.empty();
        }

        try {
            ApiResponseDTO<?> apiResponseDTO = ApiResponseDTO
                    .builder()
                    .timestamp(LocalDateTime.now())
                    .path(exchange.getRequest().getPath().value())
                    .message(errorMessage)
                    .status(errorCode)
                    .build();

            response.getHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            response.setStatusCode(httpStatus);

            Mono<DataBuffer> responseBody = Mono.just(exchange.getResponse().bufferFactory().wrap(objectMapper.writeValueAsBytes(apiResponseDTO)));
            return response.writeWith(responseBody);
        } catch (JsonProcessingException ex) {
            LogUnity.error(exchange, "資料轉換 JSON 格式失敗", ex);
            return Mono.error(new ProcessException(ProcessException.ErrorCode.FORMAT_DATA_TO_JSON_FAILED, ex));
        }
    }

    /**
     * 將自定義的 ApiResponseDTO 轉換為 JSON 格式的響應消息並寫入響應
     * 此為重寫方法，用於處理 ValidationException
     *
     * @param exchange     請求交換對象
     * @param objectMapper 用於將 ApiResponseDTO 轉換為 JSON 的 ObjectMapper
     * @param error        錯誤信息
     * @param args         錯誤消息的參數
     *
     * @return Mono<Void>
     */
    default Mono<Void> sendErrorResponse(ServerWebExchange exchange, ObjectMapper objectMapper, ValidationException.ErrorCode error, Object... args) {
        return sendErrorResponse(exchange, objectMapper, String.format(error.getMessage(), args), error.getCode(), error.getHttpStatus());
    }
}