package xyz.dowob.filemanagement.component.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nonnull;
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
import xyz.dowob.filemanagement.exception.ProcessException;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.unity.LogUnity;
import xyz.dowob.filemanagement.unity.ResponseUnity;

import java.util.Optional;

/**
 * WebSocket 連線失敗情況的專用錯誤處理器，提供優雅的錯誤回應機制。
 *
 * <p>本處理器專門處理 WebSocket 握手成功但後續驗證或初始化失敗的場景。
 * 當正常的 WebSocket 處理器無法完成連線建立時，系統會重導向至此處理器來提供錯誤資訊。
 * 透過標準化的 JSON 格式回傳具體的錯誤原因，並確保連線的正確關閉。</p>
 *
 * <p>支援從會話屬性中讀取錯誤類型，根據不同的失敗原因提供對應的錯誤訊息。
 * 具備完整的錯誤處理機制，即使在 JSON 序列化失敗的情況下也能妥善處理。
 * 自動管理連線的生命週期，確保資源的正確釋放。</p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
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
     * 當WebSocket連接失敗時，將錯誤內容轉換為JSON格式，並回傳給客戶端
     *
     * @param session WebSocketSession 用於處理WebSocket連接的會話
     *
     * @return Mono<Void> 回傳一個Mono對象，表示異步操作的結果
     */
    @Nonnull
    @Override
    @RecordLevel(LogLevelEnum.INFO)
    public Mono<Void> handle(@Nonnull WebSocketSession session) {
        ValidationException.ErrorCode errorCode = Optional
                .ofNullable(session.getAttributes().get(WEBSOCKET_ERROR_ATTRIBUTE))
                .map(errorName -> ValidationException.ErrorCode.fromName(errorName.toString()))
                .orElse(ValidationException.ErrorCode.WEBSOCKET_CONNECTION_ERROR);

        WebSocketResponse<?> response = createWebSocketResponse(WebsocketResponseType.CONNECTION_ERROR, errorCode.getMessage(), null);

        return Mono.defer(() -> {
            try {
                String jsonString = objectMapper.writeValueAsString(response);
                LogUnity.info(session, "用戶WebSocket連接失敗，準備發送錯誤訊息: %s", jsonString);
                return session
                        .send(Mono.just(session.textMessage(jsonString)))
                        .then()
                        .doOnError(e -> LogUnity.error(session, "處理WebSocket連線失敗時，發送訊息時發生錯誤", e));
            } catch (JsonProcessingException e) {
                return Mono.error(new ProcessException(ProcessException.ErrorCode.FORMAT_DATA_TO_JSON_FAILED, e));
            }
        }).doFinally(signalType -> {
            CloseStatus status = (signalType == SignalType.ON_ERROR || signalType == SignalType.CANCEL) ? CloseStatus.SERVER_ERROR : CloseStatus.NORMAL;
            LogUnity.info(session, "WebSocketFailHandler 正在關閉 session，狀態: %s，信號類型: %s", status, signalType);
            session.close(status).subscribeOn(Schedulers.boundedElastic()).subscribe();
        }).then();
    }
}
