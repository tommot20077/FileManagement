package xyz.dowob.filemanagement.component.handler;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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
 * 基於裝飾器模式的客製化 WebSocket 會話實現，擴展標準 WebSocketSession 功能並提供使用者上下文管理。
 *
 * <p>此類別採用裝飾器設計模式包裝原始 WebSocketSession，在保持完整 WebSocket 通訊能力的同時，
 * 額外提供使用者身份綁定和動態屬性管理功能。每個會話實例關聯特定使用者實體，
 * 便於後續的身份驗證、權限控制和狀態追蹤。</p>
 *
 * <p>內建屬性儲存機制支援在 WebSocket 連線生命週期內動態儲存和檢索任意鍵值對資料，
 * 適用於需要維護連線狀態或使用者上下文的即時通訊場景。所有操作都遵循非阻塞模式，
 * 與 Spring WebFlux 反應式程式設計模型完全相容。</p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see WebSocketSession
 * @see User
 */
@SkipRecord
public class CustomWebSocketSession implements WebSocketSession {
    /**
     * 被包裝的原始 WebSocketSession 實例，委派所有標準 WebSocket 操作至此實例。
     */
    private final WebSocketSession delegate;

    /**
     * 與此 WebSocket 會話關聯的使用者實體，用於身份識別和權限驗證。可為 null 表示匿名連線。
     */
    @Getter
    private final User user;

    /**
     * 自訂屬性儲存容器，用於在 WebSocket 會話生命週期內保存任意鍵值對資料。
     * 此容器獨立於原始 WebSocketSession 的屬性系統，提供額外的狀態管理能力。
     */
    private final Map<String, Object> attributes = new HashMap<>();

    /**
     * 建構客製化 WebSocket 會話實例。
     *
     * @param delegate 被包裝的原始 WebSocketSession 實例，不可為 null
     * @param user     關聯的使用者實體，可為 null 表示匿名連線
     */
    public CustomWebSocketSession(WebSocketSession delegate,@Nullable User user) {
        this.delegate = delegate;
        this.user = user;
    }


    /**
     * 設定自訂屬性值，將指定的鍵值對儲存至內建屬性容器。
     * 若鍵已存在則覆蓋原有值，支援 null 值儲存。
     *
     * @param key   屬性鍵，不可為 null
     * @param value 屬性值，可為 null
     */
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }


    /**
     * 檢索指定鍵對應的自訂屬性值。
     *
     * @param key 屬性鍵，不可為 null
     * @return 對應的屬性值，若鍵不存在則回傳 null
     */
    public Object getAttribute(String key) {
        return attributes.get(key);
    }


    /**
     * 回傳包含會話 ID、關聯使用者和自訂屬性的字串表示。
     *
     * @return 格式化的會話資訊字串，包含關鍵識別資訊
     */
    @Override
    public String toString() {
        return "CustomWebSocketSession{id=" + getId() + ", user=" + user + ", attributes=" + attributes + '}';
    }


    /**
     * 獲取 WebSocket 會話的唯一識別碼。
     *
     * @return 會話唯一識別碼，永不為 null
     */
    @Nonnull
    @Override
    public String getId() {
        return delegate.getId();
    }


    /**
     * 獲取 WebSocket 握手階段的詳細資訊，包含 HTTP 標頭、查詢參數和連線元資料。
     *
     * @return 握手資訊物件，包含連線建立時的所有相關資料
     */
    @Nonnull
    @Override
    public HandshakeInfo getHandshakeInfo() {
        return delegate.getHandshakeInfo();
    }


    /**
     * 獲取用於建立資料緩衝區的工廠實例，支援高效能的記憶體管理和資料傳輸。
     *
     * @return 資料緩衝區工廠實例，用於建立 DataBuffer 物件
     */
    @Nonnull
    @Override
    public DataBufferFactory bufferFactory() {
        return delegate.bufferFactory();
    }


    /**
     * 獲取包含所有自訂屬性的 Map 集合。回傳的是內建屬性容器的直接參考，
     * 允許外部程式碼直接操作屬性集合。
     *
     * @return 自訂屬性的 Map 集合，包含所有已設定的鍵值對
     */
    @Nonnull
    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }


    /**
     * 獲取接收 WebSocket 訊息的反應式串流。此方法回傳一個 Flux，
     * 可用於訂閱並處理從客戶端傳入的所有 WebSocket 訊息。
     *
     * @return 包含所有接收訊息的 Flux 串流
     */
    @Nonnull
    @Override
    public Flux<WebSocketMessage> receive() {
        return delegate.receive();
    }


    /**
     * 發送 WebSocket 訊息至連線的客戶端。接受一個 Publisher，
     * 支援批次發送多個訊息，所有訊息將按序傳輸。
     *
     * @param messages 要發送的 WebSocket 訊息發佈者
     * @return 表示發送完成的 Mono，完成時表示所有訊息已成功傳輸
     */
    @Nonnull
    @Override
    public Mono<Void> send(@Nonnull Publisher<WebSocketMessage> messages) {
        return delegate.send(messages);
    }


    /**
     * 檢查 WebSocket 連線的當前狀態。
     *
     * @return 若連線處於開啟狀態回傳 true，若已關閉或異常則回傳 false
     */
    @Override
    public boolean isOpen() {
        return delegate.isOpen();
    }


    /**
     * 主動關閉 WebSocket 連線並指定關閉狀態碼。關閉操作為非阻塞式，
     * 回傳的 Mono 完成時表示關閉程序已執行。
     *
     * @param status 關閉狀態碼，包含關閉原因和相關資訊
     * @return 表示關閉操作完成的 Mono
     */
    @Nonnull
    @Override
    public Mono<Void> close(@Nonnull CloseStatus status) {
        return delegate.close(status);
    }


    /**
     * 獲取 WebSocket 連線的關閉狀態資訊。若連線仍處於開啟狀態，
     * 回傳的 Mono 將在連線關閉時發出對應的關閉狀態。
     *
     * @return 包含關閉狀態資訊的 Mono，在連線關閉時完成
     */
    @Nonnull
    @Override
    public Mono<CloseStatus> closeStatus() {
        return delegate.closeStatus();
    }


    /**
     * 建立包含指定文字內容的 WebSocket 文字訊息。
     *
     * @param payload 要封裝的文字內容，不可為 null
     * @return 包含指定文字的 WebSocket 訊息物件
     */
    @Nonnull
    @Override
    public WebSocketMessage textMessage(@Nonnull String payload) {
        return delegate.textMessage(payload);
    }


    /**
     * 建立包含二進制資料的 WebSocket 訊息。使用提供的工廠函數生成資料緩衝區，
     * 支援高效能的二進制資料傳輸。
     *
     * @param payloadFactory 用於生成二進制資料緩衝區的工廠函數
     * @return 包含二進制資料的 WebSocket 訊息物件
     */
    @Nonnull
    @Override
    public WebSocketMessage binaryMessage(@Nonnull Function<DataBufferFactory, DataBuffer> payloadFactory) {
        return delegate.binaryMessage(payloadFactory);
    }


    /**
     * 建立 WebSocket Ping 控制訊息，用於連線保活和延遲測試。
     * Ping 訊息通常由接收端以 Pong 訊息回應。
     *
     * @param payloadFactory 用於生成 Ping 資料內容的工廠函數
     * @return WebSocket Ping 控制訊息物件
     */
    @Nonnull
    @Override
    public WebSocketMessage pingMessage(@Nonnull Function<DataBufferFactory, DataBuffer> payloadFactory) {
        return delegate.pingMessage(payloadFactory);
    }


    /**
     * 建立 WebSocket Pong 控制訊息，通常用於回應 Ping 訊息。
     * Pong 訊息也可主動發送以進行單向的連線狀態通知。
     *
     * @param payloadFactory 用於生成 Pong 資料內容的工廠函數
     * @return WebSocket Pong 控制訊息物件
     */
    @Nonnull
    @Override
    public WebSocketMessage pongMessage(@Nonnull Function<DataBufferFactory, DataBuffer> payloadFactory) {
        return delegate.pongMessage(payloadFactory);
    }
}