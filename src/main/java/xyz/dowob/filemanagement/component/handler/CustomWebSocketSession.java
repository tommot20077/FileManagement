package xyz.dowob.filemanagement.component.handler;

import lombok.Getter;
import lombok.NonNull;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.adapter.ReactorNettyWebSocketSession;

/**
 * 自定義WebSocket Session，繼承自ReactorNettyWebSocketSession
 * 原有的ReactorNettyWebSocketSession中並沒有提供用戶ID的屬性，所以這裡自定義一個屬性用於存儲用戶ID
 *
 * @author yuan
 * @program FileManagement
 * @ClassName CustomWebSocketSession
 * @description
 * @create 2024-10-08 22:46
 * @Version 1.0
 **/
@Getter
public class CustomWebSocketSession extends ReactorNettyWebSocketSession {
    /**
     * 用戶ID
     */
    private final String userId;


    /**
     * 自定義WebSocket Session構造方法
     *
     * @param delegate              WebSocket連接
     * @param handshakeInfo         握手信息
     * @param bufferFactory         數據緩衝工廠
     * @param maxFramePayloadLength 最大幀載荷長度
     * @param userId                用戶ID
     */
    public CustomWebSocketSession(WebSocketConnection delegate, HandshakeInfo handshakeInfo, NettyDataBufferFactory bufferFactory, int maxFramePayloadLength, String userId) {
        super(delegate.getInbound(), delegate.getOutbound(), handshakeInfo, bufferFactory, maxFramePayloadLength);
        this.userId = userId;
    }


    /**
     * 獲取Session ID
     *
     * @return Session ID
     */
    @Override
    @NonNull
    public String getId() {
        return super.getId();
    }
}