package xyz.dowob.filemanagement.component.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.dowob.filemanagement.customenum.WebsocketResponseType;
import xyz.dowob.filemanagement.data.response.WebSocketResponse;
import xyz.dowob.filemanagement.exception.ProcessException;
import xyz.dowob.filemanagement.exception.ValidationException;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("WebSocketFailHandler 邏輯處理測試")
class WebSocketFailHandlerTest {

    @Mock
    private ObjectMapper mockObjectMapper;

    @Mock
    private WebSocketSession mockSession;

    @Captor
    private ArgumentCaptor<Mono<WebSocketMessage>> messageCaptor;

    @Captor
    private ArgumentCaptor<CloseStatus> closeStatusCaptor;

    @Captor
    private ArgumentCaptor<WebSocketResponse<Object>> webSocketResponseCaptor;

    private WebSocketFailHandler webSocketFailHandlerUnderTest;

    private Map<String, Object> attributes;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        webSocketFailHandlerUnderTest = new WebSocketFailHandler(mockObjectMapper);
        attributes = new HashMap<>();
        when(mockSession.getAttributes()).thenReturn(attributes);
        when(mockSession.close(any(CloseStatus.class))).thenReturn(Mono.empty());
        when(mockSession.send(any())).thenReturn(Mono.empty());

        when(mockSession.textMessage(anyString())).thenAnswer(invocation -> {
            WebSocketMessage mockMessage = mock(WebSocketMessage.class);
            when(mockMessage.getPayloadAsText()).thenReturn(invocation.getArgument(0));
            return mockMessage;
        });
    }

    @Test
    @DisplayName("處理帶有特定錯誤屬性的 Session - 發送對應錯誤訊息並正常關閉連線")
    void handle_withWebSocketErrorAttribute_sendsErrorResponseAndCloses() throws JsonProcessingException {
        ValidationException.ErrorCode expectedErrorCode = ValidationException.ErrorCode.USERNAME_OR_PASSWORD_ERROR;
        attributes.put("X-WebSocket-Error", expectedErrorCode.name());
        String expectedJson = "{\"type\":\"CONNECTION_ERROR\",\"message\":\"" + expectedErrorCode.getMessage() + "\",\"data\":null}";

        when(mockObjectMapper.writeValueAsString(any(WebSocketResponse.class))).thenReturn(expectedJson);

        Mono<Void> result = webSocketFailHandlerUnderTest.handle(mockSession);

        StepVerifier.create(result)
                .verifyComplete();

        verify(mockObjectMapper).writeValueAsString(webSocketResponseCaptor.capture());
        WebSocketResponse<Object> capturedResponse = webSocketResponseCaptor.getValue();
        assertThat(capturedResponse.getType()).isEqualTo(WebsocketResponseType.CONNECTION_ERROR);
        assertThat(capturedResponse.getMessage()).isEqualTo(expectedErrorCode.getMessage());
        assertThat(capturedResponse.getData()).isNull();
        assertThat(capturedResponse.getTimestamp()).isNotNull();


        verify(mockSession).send(messageCaptor.capture());
        verify(mockSession).close(closeStatusCaptor.capture());

        StepVerifier.create(messageCaptor.getValue())
                .expectNextMatches(message -> message.getPayloadAsText().equals(expectedJson))
                .verifyComplete();

        assertThat(closeStatusCaptor.getValue()).isEqualTo(CloseStatus.NORMAL);
    }

    @Test
    @DisplayName("處理未帶有錯誤屬性的 Session - 發送預設錯誤訊息並正常關閉連線")
    void handle_withoutWebSocketErrorAttribute_sendsDefaultErrorResponseAndCloses() throws JsonProcessingException {
        ValidationException.ErrorCode defaultErrorCode = ValidationException.ErrorCode.WEBSOCKET_CONNECTION_ERROR;
        String expectedJson = "{\"type\":\"CONNECTION_ERROR\",\"message\":\"" + defaultErrorCode.getMessage() + "\",\"data\":null}";

        when(mockObjectMapper.writeValueAsString(any(WebSocketResponse.class))).thenReturn(expectedJson);

        Mono<Void> result = webSocketFailHandlerUnderTest.handle(mockSession);

        StepVerifier.create(result)
                .verifyComplete();

        verify(mockObjectMapper).writeValueAsString(webSocketResponseCaptor.capture());
        WebSocketResponse<Object> capturedResponse = webSocketResponseCaptor.getValue();
        assertThat(capturedResponse.getType()).isEqualTo(WebsocketResponseType.CONNECTION_ERROR);
        assertThat(capturedResponse.getMessage()).isEqualTo(defaultErrorCode.getMessage());
        assertThat(capturedResponse.getData()).isNull();
        assertThat(capturedResponse.getTimestamp()).isNotNull();

        verify(mockSession).send(messageCaptor.capture());
        verify(mockSession).close(closeStatusCaptor.capture());

        StepVerifier.create(messageCaptor.getValue())
                .expectNextMatches(message -> message.getPayloadAsText().equals(expectedJson))
                .verifyComplete();

        assertThat(closeStatusCaptor.getValue()).isEqualTo(CloseStatus.NORMAL);
    }

    @Test
    @DisplayName("處理時 ObjectMapper 拋出例外 - 記錄錯誤並以伺服器錯誤關閉連線")
    void handle_objectMapperThrowsException_logsErrorAndCloses() throws JsonProcessingException {
        JsonProcessingException exception = new JsonProcessingException("Serialization failed") {};
        when(mockObjectMapper.writeValueAsString(any(WebSocketResponse.class))).thenThrow(exception);
        Mono<Void> result = webSocketFailHandlerUnderTest.handle(mockSession);

        StepVerifier.create(result)
                    .verifyErrorMatches(e -> e instanceof ProcessException processException && processException.getErrorCode() == ProcessException.ErrorCode.FORMAT_DATA_TO_JSON_FAILED);

        verify(mockSession, never()).send(any());
        verify(mockSession).close(closeStatusCaptor.capture());
        assertThat(closeStatusCaptor.getValue()).isEqualTo(CloseStatus.SERVER_ERROR);
    }

    @Test
    @DisplayName("處理時 session.send 拋出例外 - 記錄錯誤並以伺服器錯誤關閉連線")
    void handle_sessionSendThrowsException_logsErrorAndCloses() throws JsonProcessingException {
        ValidationException.ErrorCode defaultErrorCode = ValidationException.ErrorCode.WEBSOCKET_CONNECTION_ERROR;
        String expectedJson = "{\"type\":\"CONNECTION_ERROR\",\"message\":\"" + defaultErrorCode.getMessage() + "\",\"data\":null}";
        RuntimeException sendException = new RuntimeException("Send failed");

        when(mockObjectMapper.writeValueAsString(any(WebSocketResponse.class))).thenReturn(expectedJson);
        when(mockSession.send(any())).thenReturn(Mono.error(sendException));

        Mono<Void> result = webSocketFailHandlerUnderTest.handle(mockSession);

        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        verify(mockObjectMapper).writeValueAsString(webSocketResponseCaptor.capture());
        WebSocketResponse<Object> capturedResponse = webSocketResponseCaptor.getValue();
        assertThat(capturedResponse.getType()).isEqualTo(WebsocketResponseType.CONNECTION_ERROR);
        assertThat(capturedResponse.getMessage()).isEqualTo(defaultErrorCode.getMessage());
        assertThat(capturedResponse.getData()).isNull();
        assertThat(capturedResponse.getTimestamp()).isNotNull();

        verify(mockSession).send(any());
        verify(mockSession).close(closeStatusCaptor.capture());
        assertThat(closeStatusCaptor.getValue()).isEqualTo(CloseStatus.SERVER_ERROR);
    }

    @Test
    @DisplayName("處理時 X-WebSocket-Error 屬性值為無效錯誤碼名稱 - 發送預設錯誤訊息並正常關閉連線")
    void handle_withInvalidWebSocketErrorAttributeName_sendsDefaultErrorResponseAndCloses() throws JsonProcessingException {
        attributes.put("X-WebSocket-Error", "INVALID_ERROR_CODE_NAME_XYZ");
        ValidationException.ErrorCode expectedErrorCode = ValidationException.ErrorCode.WEBSOCKET_CONNECTION_ERROR;
        String expectedJson = "{\"type\":\"CONNECTION_ERROR\",\"message\":\"" + expectedErrorCode.getMessage() + "\",\"data\":null}";

        when(mockObjectMapper.writeValueAsString(any(WebSocketResponse.class))).thenReturn(expectedJson);

        Mono<Void> result = webSocketFailHandlerUnderTest.handle(mockSession);
        StepVerifier.create(result)
                .verifyComplete();

        verify(mockObjectMapper).writeValueAsString(webSocketResponseCaptor.capture());
        WebSocketResponse<Object> capturedResponse = webSocketResponseCaptor.getValue();
        assertThat(capturedResponse.getType()).isEqualTo(WebsocketResponseType.CONNECTION_ERROR);
        assertThat(capturedResponse.getMessage()).isEqualTo(expectedErrorCode.getMessage());
        assertThat(capturedResponse.getData()).isNull();
        assertThat(capturedResponse.getTimestamp()).isNotNull();

        verify(mockSession).send(messageCaptor.capture());
        StepVerifier.create(messageCaptor.getValue())
                .expectNextMatches(message -> message.getPayloadAsText().equals(expectedJson))
                .verifyComplete();

        verify(mockSession).close(closeStatusCaptor.capture());
        assertThat(closeStatusCaptor.getValue()).isEqualTo(CloseStatus.NORMAL);
    }

    @Test
    @DisplayName("處理時 session.textMessage 拋出例外 - 記錄錯誤並以伺服器錯誤關閉連線")
    void handle_sessionTextMessageThrowsException_logsErrorAndClosesWithServerError() throws JsonProcessingException {
        ValidationException.ErrorCode defaultErrorCode = ValidationException.ErrorCode.WEBSOCKET_CONNECTION_ERROR;
        String expectedJson = "{\"type\":\"CONNECTION_ERROR\",\"message\":\"" + defaultErrorCode.getMessage() + "\",\"data\":null}";
        RuntimeException textMessageException = new IllegalArgumentException("Invalid text message content");

        when(mockObjectMapper.writeValueAsString(any(WebSocketResponse.class))).thenReturn(expectedJson);
        when(mockSession.textMessage(expectedJson)).thenThrow(textMessageException);


        Mono<Void> result = webSocketFailHandlerUnderTest.handle(mockSession);

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof IllegalArgumentException &&
                                                 throwable.getMessage().equals("Invalid text message content"))
                .verify();

        verify(mockObjectMapper).writeValueAsString(webSocketResponseCaptor.capture());
        WebSocketResponse<Object> capturedResponse = webSocketResponseCaptor.getValue();
        assertThat(capturedResponse.getType()).isEqualTo(WebsocketResponseType.CONNECTION_ERROR);
        assertThat(capturedResponse.getMessage()).isEqualTo(defaultErrorCode.getMessage());

        verify(mockSession, never()).send(any(Mono.class));


        verify(mockSession).close(closeStatusCaptor.capture());
        assertThat(closeStatusCaptor.getValue()).isEqualTo(CloseStatus.SERVER_ERROR);
    }
}
