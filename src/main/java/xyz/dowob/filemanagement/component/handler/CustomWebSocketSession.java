package xyz.dowob.filemanagement.component.handler;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.SkipRecord;
import xyz.dowob.filemanagement.entity.User;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 自定義WebSocket Session，實現WebSocketSession接口
 * 透過裝飾器模式來擴展WebSocketSession的功能，除了原生的WebSocketSession功能外，
 * 另外加入了用戶信息和自定義屬性的功能
 *
 * @author yuan
 * @program FileManagement
 * @ClassName CustomWebSocketSession
 * @description
 * @create 2024-10-08 22:46
 * @Version 1.0
 **/
@SkipRecord
public class CustomWebSocketSession implements WebSocketSession {
    /**
     * 用戶連接的WebSocketSession
     */
    private final WebSocketSession delegate;

    /**
     * 用戶
     */
    @Getter
    private final User user;

    /**
     * 自定義屬性
     */
    private final Map<String, Object> attributes = new HashMap<>();

    /**
     * 自定義WebSocketSession的構造函數
     *
     * @param delegate WebSocketSession
     * @param user     用戶ID
     */
    public CustomWebSocketSession(WebSocketSession delegate,@Nullable User user) {
        this.delegate = delegate;
        this.user = user;
    }


    /**
     * 設定自定義屬性的值
     *
     * @param key   屬性鍵
     * @param value 屬性值
     */
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }


    /**
     * 獲取自定義屬性
     *
     * @param key 屬性鍵
     *
     * @return 屬性值
     */
    public Object getAttribute(String key) {
        return attributes.get(key);
    }


    /**
     * 重寫toString方法，返回自定義的WebSocketSession信息
     *
     * @return 自定義的WebSocketSession信息
     */
    @Override
    public String toString() {
        return "CustomWebSocketSession{id=" + getId() + ", user=" + user + ", attributes=" + attributes + '}';
    }


    /**
     * 獲取 Session ID
     *
     * @return Session ID
     */
    @NotNull
    @Override
    public String getId() {
        return delegate.getId();
    }


    /**
     * 獲取握手信息
     *
     * @return 握手信息
     */
    @NotNull
    @Override
    public HandshakeInfo getHandshakeInfo() {
        return delegate.getHandshakeInfo();
    }


    /**
     * 獲取緩衝區工廠
     *
     * @return 緩衝區工廠
     */
    @NotNull
    @Override
    public DataBufferFactory bufferFactory() {
        return delegate.bufferFactory();
    }


    /**
     * 獲取所有自定義的屬性
     *
     * @return 自定義屬性Map
     */
    @NotNull
    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }


    /**
     * 獲取WebSocket消息的請求
     *
     * @return WebSocket消息請求
     */
    @NotNull
    @Override
    public Flux<WebSocketMessage> receive() {
        return delegate.receive();
    }


    /**
     * 發送WebSocket消息
     *
     * @param messages WebSocket消息
     *
     * @return Mono<Void>
     */
    @NotNull
    @Override
    public Mono<Void> send(@NotNull Publisher<WebSocketMessage> messages) {
        return delegate.send(messages);
    }


    /**
     * 檢查WebSocket連接是否開啟
     *
     * @return 當開啟時返回true，否則返回false
     */
    @Override
    public boolean isOpen() {
        return delegate.isOpen();
    }


    /**
     * 關閉WebSocket連接，並傳遞關閉狀態
     *
     * @param status 關閉狀態
     *
     * @return Mono<Void>
     */
    @NotNull
    @Override
    public Mono<Void> close(@NotNull CloseStatus status) {
        return delegate.close(status);
    }


    /**
     * 獲取關閉狀態
     *
     * @return 關閉狀態
     */
    @NotNull
    @Override
    public Mono<CloseStatus> closeStatus() {
        return delegate.closeStatus();
    }


    /**
     * 轉換文本消息成為WebSocket消息
     *
     * @param payload 文本消息的字符串
     *
     * @return WebSocket消息
     */
    @NotNull
    @Override
    public WebSocketMessage textMessage(@NotNull String payload) {
        return delegate.textMessage(payload);
    }


    /**
     * 轉換二進制消息成為WebSocket消息
     *
     * @param payloadFactory 二進制消息的緩衝區工廠
     *
     * @return WebSocket消息
     */
    @NotNull
    @Override
    public WebSocketMessage binaryMessage(@NotNull Function<DataBufferFactory, DataBuffer> payloadFactory) {
        return delegate.binaryMessage(payloadFactory);
    }


    /**
     * 轉換Ping消息成為WebSocket消息
     *
     * @param payloadFactory Ping消息的緩衝區工廠
     *
     * @return WebSocket消息
     */
    @NotNull
    @Override
    public WebSocketMessage pingMessage(@NotNull Function<DataBufferFactory, DataBuffer> payloadFactory) {
        return delegate.pingMessage(payloadFactory);
    }


    /**
     * 轉換Pong消息成為WebSocket消息
     *
     * @param payloadFactory Pong消息的緩衝區工廠
     *
     * @return WebSocket消息
     */
    @NotNull
    @Override
    public WebSocketMessage pongMessage(@NotNull Function<DataBufferFactory, DataBuffer> payloadFactory) {
        return delegate.pongMessage(payloadFactory);
    }
}