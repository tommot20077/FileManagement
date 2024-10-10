package xyz.dowob.filemanagement.component.handler;

import lombok.Getter;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.adapter.ReactorNettyWebSocketSession;

/**
 * @author yuan
 * @program FileManagement
 * @ClassName CustomWebSocketSession
 * @description
 * @create 2024-10-08 22:46
 * @Version 1.0
 **/
@Getter
public class CustomWebSocketSession extends ReactorNettyWebSocketSession {
    private final String userId;


    public CustomWebSocketSession(WebSocketConnection delegate, HandshakeInfo handshakeInfo, NettyDataBufferFactory bufferFactory, int maxFramePayloadLength, String userId) {
        super(delegate.getInbound(), delegate.getOutbound(), handshakeInfo, bufferFactory, maxFramePayloadLength);
        this.userId = userId;
    }

    @Override
    @NonNull
    public String getId() {
        return userId;
    }
}