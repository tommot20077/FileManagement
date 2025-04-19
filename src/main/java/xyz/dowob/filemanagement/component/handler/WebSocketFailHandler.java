package xyz.dowob.filemanagement.component.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.RecordLevel;
import xyz.dowob.filemanagement.annotation.SkipRecord;
import xyz.dowob.filemanagement.customenum.LogLevelEnum;
import xyz.dowob.filemanagement.data.api.ApiResponseDTO;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.unity.ResponseUnity;

/**
 * 用於處理WebSocket連接失敗的處理器，當WebSocket連接失敗時將無法使用原本的連線返回錯誤內容
 * 因此需要使用這個處理器來處理連線失敗的情況，透過一個默認的WebSocket連線來返回錯誤內容
 * 這個處理器會將錯誤內容轉換為JSON格式，並返回給客戶端
 * 此類實現了WebSocketHandler接口，並重寫了handle方法，將內容寫入並回傳
 * 以及ResponseUnity接口，內部提供通用的回應處理方法
 *
 * @author yuan
 * @program FileManagement
 * @ClassName FailWebSocketHandler
 * @create 2025/2/5
 * @Version 1.0
 **/
@Component
@RequiredArgsConstructor
public class WebSocketFailHandler implements WebSocketHandler, ResponseUnity {
    /**
     * ObjectMapper 用於將對象轉換為JSON格式的工具
     */
    private final ObjectMapper objectMapper;

    /**
     * handle方法用於處理WebSocket連接失敗的情況
     * 當WebSocket連接失敗時，將錯誤內容轉換為JSON格式，並返回給客戶端
     *
     * @param session WebSocketSession 用於處理WebSocket連接的會話
     *
     * @return Mono<Void> 返回一個Mono對象，表示異步操作的結果
     */
    @NotNull
    @Override
    @RecordLevel(LogLevelEnum.DEBUG)
    public Mono<Void> handle(@NotNull WebSocketSession session) {
        return Mono.using(() -> session, webSocketSession -> {
            String errorMessage = webSocketSession.getAttributes().get("X-WebSocket-Error").toString();
            ValidationException.ErrorCode errorCode = getErrorCode(errorMessage);
            ApiResponseDTO<Object> response = createResponse(webSocketSession, errorCode.getCode(), errorCode.getMessage(), null);

            return Mono
                    .fromCallable(() -> objectMapper.writeValueAsString(response))
                    .flatMap(jsonString -> webSocketSession.send(Flux.just(webSocketSession.textMessage(jsonString))));
        }, WebSocketSession::close, true).onErrorResume(e -> Mono.error(new RuntimeException("WebSocket處理連線時發生錯誤")));
    }


    /**
     * 獲取錯誤代碼，利用錯誤訊息來獲取對應的錯誤代碼
     * 當錯誤訊息為null時，返回預設的WebSocket連線錯誤代碼
     *
     * @param errorMessage 錯誤訊息
     *
     * @return ValidationException.ErrorCode 錯誤代碼
     */
    @SkipRecord
    private ValidationException.ErrorCode getErrorCode(@Nullable String errorMessage) {
        if (errorMessage == null) {
            return ValidationException.ErrorCode.WEBSOCKET_CONNECTION_ERROR;
        }
        return ValidationException.ErrorCode.valueOf(errorMessage);
    }
}
