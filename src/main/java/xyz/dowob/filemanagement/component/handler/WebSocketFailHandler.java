package xyz.dowob.filemanagement.component.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;
import reactor.core.scheduler.Schedulers;
import xyz.dowob.filemanagement.annotation.RecordLevel;
import xyz.dowob.filemanagement.customenum.LogLevelEnum;
import xyz.dowob.filemanagement.customenum.WebsocketResponseType;
import xyz.dowob.filemanagement.data.response.WebSocketResponse;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.unity.LogUnity;
import xyz.dowob.filemanagement.unity.ResponseUnity;

import java.util.Optional;

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
     * Websocket連接失敗時的錯誤屬性名稱
     */
    private static final String WEBSOCKET_ERROR_ATTRIBUTE = "X-WebSocket-Error";

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
    @RecordLevel(LogLevelEnum.INFO)
    public Mono<Void> handle(@NotNull WebSocketSession session) {
        ValidationException.ErrorCode errorCode = Optional
                .ofNullable(session.getAttributes().get(WEBSOCKET_ERROR_ATTRIBUTE))
                .map(errorName -> ValidationException.ErrorCode.fromName(errorName.toString()))
                .orElse(ValidationException.ErrorCode.WEBSOCKET_CONNECTION_ERROR);

        WebSocketResponse<?> response = createWebSocketResponse(WebsocketResponseType.CONNECTION_ERROR, errorCode.getMessage(), null);

        return Mono
                .fromCallable(() -> objectMapper.writeValueAsString(response))
                .doOnNext(s -> LogUnity.info(session, "用戶WebSocket連接失敗，關閉連線，session: %s", session))
                .flatMap(jsonString -> session.send(Mono.just(session.textMessage(jsonString))).then().onErrorResume(e -> {
                    LogUnity.error(session, "處理WebSocket連線失敗時發生意外的錯誤", e);
                    return Mono.empty();
                }))
                .doFinally(signalType -> {
                    CloseStatus status = (signalType == SignalType.ON_ERROR || signalType == SignalType.CANCEL) ? CloseStatus.SERVER_ERROR : CloseStatus.NORMAL;
                    session.close(status).subscribeOn(Schedulers.boundedElastic()).subscribe();
                });
    }
}
