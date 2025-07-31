package xyz.dowob.filemanagement.component.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.dowob.filemanagement.entity.User;

import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 自定義 WebSocket 工作階段的單元測試類別。
 *
 * <p>本測試類別全面驗證 CustomWebSocketSession 的功能和使用者關聯機制。</p>
 *
 * <p>測試範圍：
 * 
 *   - WebSocket 工作階段的包裝和擴展功能
 *   - 使用者資訊的關聯和管理
 *   - 屬性設置和獲取機制
 *   - WebSocket 工作階段的代理行為
 * 
 * </p>
 *
 * <p>主要測試方法：
 * 
 *   - 驗證屬性管理功能的正確性
 *   - 測試使用者資訊的關聯機制
 *   - 確認 WebSocket 代理行為的正確性
 *   - 檢驗工作階段生命週期管理
 * 
 * </p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CustomWebSocketSession 邏輯處理測試")
class CustomWebSocketSessionTest {

    @Mock
    private WebSocketSession mockDelegate;

    @Mock
    private User mockUser;

    private CustomWebSocketSession customWebSocketSessionUnderTest;


    @BeforeEach
    void setUp() {
        customWebSocketSessionUnderTest = new CustomWebSocketSession(mockDelegate, mockUser);
    }

    /**
     * 測試屬性設置和獲取的功能。
     *
     * 測試涵蓋的邏輯或場景說明：
     * 驗證 CustomWebSocketSession 能正確設置和獲取屬性值。
     *
     * 前置條件：
     * - 初始化 CustomWebSocketSession 實例
     * - 準備測試用的鍵值對
     *
     * 測試步驟：
     * - 使用 setAttribute 設置屬性
     * - 使用 getAttribute 獲取屬性
     * - 驗證獲取的值與設置的值一致
     *
     * 預期結果：
     * - 成功設置屬性值
     * - 成功獲取先前設置的屬性值
     * - 設置和獲取的值完全一致
     */
    @Test
    @DisplayName("設置和獲取屬性 - 成功設置並獲取屬性")
    void setAndGetAttribute_setsAndGetsAttributeSuccessfully() {
        String key = "testKey";
        String value = "testValue";

        customWebSocketSessionUnderTest.setAttribute(key, value);
        Object retrievedValue = customWebSocketSessionUnderTest.getAttribute(key);
        assertThat(retrievedValue).isEqualTo(value);
    }

    @Test
    @DisplayName("獲取不存在的屬性 - 返回null")
    void getAttribute_nonExistentAttribute_returnsNull() {
        Object retrievedValue = customWebSocketSessionUnderTest.getAttribute("nonExistentKey");
        assertThat(retrievedValue).isNull();
    }

    @Test
    @DisplayName("獲取ID - 成功返回委託的ID")
    void getId_returnsDelegateId() {
        String expectedId = "session123";
        when(mockDelegate.getId()).thenReturn(expectedId);

        String actualId = customWebSocketSessionUnderTest.getId();

        assertThat(actualId).isEqualTo(expectedId);
        verify(mockDelegate).getId();
    }

    @Test
    @DisplayName("獲取握手信息 - 成功返回委託的握手信息")
    void getHandshakeInfo_returnsDelegateHandshakeInfo() {
        HandshakeInfo expectedHandshakeInfo = mock(HandshakeInfo.class);
        when(mockDelegate.getHandshakeInfo()).thenReturn(expectedHandshakeInfo);

        HandshakeInfo actualHandshakeInfo = customWebSocketSessionUnderTest.getHandshakeInfo();

        assertThat(actualHandshakeInfo).isEqualTo(expectedHandshakeInfo);
        verify(mockDelegate).getHandshakeInfo();
    }

    @Test
    @DisplayName("獲取緩衝區工廠 - 成功返回委託的緩衝區工廠")
    void bufferFactory_returnsDelegateBufferFactory() {
        DataBufferFactory expectedBufferFactory = mock(DataBufferFactory.class);
        when(mockDelegate.bufferFactory()).thenReturn(expectedBufferFactory);

        DataBufferFactory actualBufferFactory = customWebSocketSessionUnderTest.bufferFactory();

        assertThat(actualBufferFactory).isEqualTo(expectedBufferFactory);
        verify(mockDelegate).bufferFactory();
    }

    @Test
    @DisplayName("獲取屬性 - 成功返回自定義屬性Map")
    void getAttributes_returnsCustomAttributesMap() {
        String key = "testKey";
        String value = "testValue";
        customWebSocketSessionUnderTest.setAttribute(key, value);

        Map<String, Object> actualAttributes = customWebSocketSessionUnderTest.getAttributes();
        assertThat(actualAttributes).containsEntry(key, value);
    }

    @Test
    @DisplayName("接收消息 - 成功返回委託的接收消息Flux")
    void receive_returnsDelegateReceiveFlux() {
        WebSocketMessage message1 = mock(WebSocketMessage.class);
        WebSocketMessage message2 = mock(WebSocketMessage.class);
        Flux<WebSocketMessage> expectedFlux = Flux.just(message1, message2);
        when(mockDelegate.receive()).thenReturn(expectedFlux);

        Flux<WebSocketMessage> actualFlux = customWebSocketSessionUnderTest.receive();

        StepVerifier.create(actualFlux)
                    .expectNext(message1)
                    .expectNext(message2)
                    .verifyComplete();
        verify(mockDelegate).receive();
    }

    @Test
    @DisplayName("發送消息 - 成功調用委託的發送方法")
    void send_callsDelegateSend() {
        Publisher<WebSocketMessage> messages = Flux.empty();
        when(mockDelegate.send(any(Publisher.class))).thenReturn(Mono.empty());

        Mono<Void> result = customWebSocketSessionUnderTest.send(messages);

        StepVerifier.create(result).verifyComplete();
        verify(mockDelegate).send(messages);
    }

    @Test
    @DisplayName("檢查是否開啟 - 成功返回委託的開啟狀態")
    void isOpen_returnsDelegateIsOpen() {
        when(mockDelegate.isOpen()).thenReturn(true);

        boolean actualIsOpen = customWebSocketSessionUnderTest.isOpen();

        assertThat(actualIsOpen).isTrue();
        verify(mockDelegate).isOpen();
    }

    @Test
    @DisplayName("關閉會話 - 成功調用委託的關閉方法")
    void close_callsDelegateClose() {
        CloseStatus status = CloseStatus.NORMAL;
        when(mockDelegate.close(status)).thenReturn(Mono.empty());

        Mono<Void> result = customWebSocketSessionUnderTest.close(status);

        StepVerifier.create(result).verifyComplete();
        verify(mockDelegate).close(status);
    }

    @Test
    @DisplayName("獲取關閉狀態 - 成功返回委託的關閉狀態")
    void closeStatus_returnsDelegateCloseStatus() {
        CloseStatus expectedStatus = CloseStatus.NORMAL;
        when(mockDelegate.closeStatus()).thenReturn(Mono.just(expectedStatus));

        Mono<CloseStatus> actualStatusMono = customWebSocketSessionUnderTest.closeStatus();

        StepVerifier.create(actualStatusMono)
                    .expectNext(expectedStatus)
                    .verifyComplete();
        verify(mockDelegate).closeStatus();
    }

    @Test
    @DisplayName("創建文本消息 - 成功返回委託的文本消息")
    void textMessage_returnsDelegateTextMessage() {
        String payload = "testPayload";
        WebSocketMessage expectedMessage = mock(WebSocketMessage.class);
        when(mockDelegate.textMessage(payload)).thenReturn(expectedMessage);

        WebSocketMessage actualMessage = customWebSocketSessionUnderTest.textMessage(payload);

        assertThat(actualMessage).isEqualTo(expectedMessage);
        verify(mockDelegate).textMessage(payload);
    }

    @Test
    @DisplayName("創建二進制消息 - 成功返回委託的二進制消息")
    void binaryMessage_returnsDelegateBinaryMessage() {
        Function<DataBufferFactory, DataBuffer> payloadFactory = dataBufferFactory -> mock(DataBuffer.class);
        WebSocketMessage expectedMessage = mock(WebSocketMessage.class);
        when(mockDelegate.binaryMessage(any(Function.class))).thenReturn(expectedMessage);

        WebSocketMessage actualMessage = customWebSocketSessionUnderTest.binaryMessage(payloadFactory);

        assertThat(actualMessage).isEqualTo(expectedMessage);
        verify(mockDelegate).binaryMessage(payloadFactory);
    }

    @Test
    @DisplayName("創建Ping消息 - 成功返回委託的Ping消息")
    void pingMessage_returnsDelegatePingMessage() {
        Function<DataBufferFactory, DataBuffer> payloadFactory = dataBufferFactory -> mock(DataBuffer.class);
        WebSocketMessage expectedMessage = mock(WebSocketMessage.class);
        when(mockDelegate.pingMessage(any(Function.class))).thenReturn(expectedMessage);

        WebSocketMessage actualMessage = customWebSocketSessionUnderTest.pingMessage(payloadFactory);

        assertThat(actualMessage).isEqualTo(expectedMessage);
        verify(mockDelegate).pingMessage(payloadFactory);
    }

    @Test
    @DisplayName("創建Pong消息 - 成功返回委託的Pong消息")
    void pongMessage_returnsDelegatePongMessage() {
        Function<DataBufferFactory, DataBuffer> payloadFactory = dataBufferFactory -> mock(DataBuffer.class);
        WebSocketMessage expectedMessage = mock(WebSocketMessage.class);
        when(mockDelegate.pongMessage(any(Function.class))).thenReturn(expectedMessage);

        WebSocketMessage actualMessage = customWebSocketSessionUnderTest.pongMessage(payloadFactory);

        assertThat(actualMessage).isEqualTo(expectedMessage);
        verify(mockDelegate).pongMessage(payloadFactory);
    }

    @Test
    @DisplayName("toString方法 - 成功返回自定義的toString結果")
    void toString_returnsCustomToString() {
        String sessionId = "testSessionId";
        when(mockDelegate.getId()).thenReturn(sessionId);
        customWebSocketSessionUnderTest.setAttribute("attr", "value");

        String actualString = customWebSocketSessionUnderTest.toString();

        assertThat(actualString).contains("CustomWebSocketSession{id=" + sessionId);
        assertThat(actualString).contains("user=" + mockUser.toString());
        assertThat(actualString).contains("attributes={attr=value}");
        verify(mockDelegate).getId();
    }
}
