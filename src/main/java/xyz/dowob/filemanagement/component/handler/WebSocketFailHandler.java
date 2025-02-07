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
import xyz.dowob.filemanagement.data.api.ApiResponseDTO;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.unity.ResponseUnity;

/**
 * @author yuan
 * @program FileManagement
 * @ClassName FailWebSocketHandler
 * @create 2025/2/5
 * @Version 1.0
 **/
@Component
@RequiredArgsConstructor
public class WebSocketFailHandler implements WebSocketHandler, ResponseUnity {
    private final ObjectMapper objectMapper;

    @NotNull
    @Override
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

    private ValidationException.ErrorCode getErrorCode(@Nullable String errorMessage) {
        if (errorMessage == null) {
            return ValidationException.ErrorCode.WEBSOCKET_CONNECTION_ERROR;
        }
        return ValidationException.ErrorCode.valueOf(errorMessage);
    }
}
