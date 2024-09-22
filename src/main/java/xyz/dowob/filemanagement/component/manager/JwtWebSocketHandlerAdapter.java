package xyz.dowob.filemanagement.component.manager;

import jakarta.annotation.Nullable;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.adapter.AbstractWebSocketSession;
import org.springframework.web.reactive.socket.adapter.ReactorNettyWebSocketSession;
import org.springframework.web.reactive.socket.server.support.HandshakeWebSocketService;
import org.springframework.web.reactive.socket.server.upgrade.ReactorNettyRequestUpgradeStrategy;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.WebsocketServerSpec;
import xyz.dowob.filemanagement.component.handler.CustomWebSocketSession;
import xyz.dowob.filemanagement.component.handler.FileUploadWebSocketHandler;
import xyz.dowob.filemanagement.component.handler.WebSocketFailHandler;
import xyz.dowob.filemanagement.component.provider.providerImplement.JwtTokenProviderImpl;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.unity.ResponseUnity;

import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Supplier;

/**
 * 用於處理 WebSocket 請求的處理器，繼承自 HandshakeWebSocketService，實現 ResponseUnity 接口
 * 此類用於處理 WebSocket 請求，當請求中包含 JWT 憑證時，進行驗證，並將用戶ID存入 ServerWebExchange 的屬性中
 *
 * @author yuan
 * @program FileManagement
 * @ClassName JwtWebSocketHandlerAdapter
 * @description
 * @create 2024-10-07 00:51
 * @Version 1.0
 **/
@Component
@Log4j2
public class JwtWebSocketHandlerAdapter extends HandshakeWebSocketService implements ResponseUnity {
    /**
     * JwtTokenProviderImpl 用於 JWT 憑證相關操作的實現類
     */
    private final JwtTokenProviderImpl jwtTokenProvider;

    /**
     * FileProperties 用於操作文件上傳相關配置的類
     */
    private final FileProperties fileProperties;

    /**
     * FileUploadWebSocketHandler 用於處理文件上傳的 WebSocketHandler
     */
    private final FileUploadWebSocketHandler fileUploadWebSocketHandler;

    /**
     * WebSocketFailHandler 用於處理 WebSocket 連接失敗的處理器
     */
    private final WebSocketFailHandler webSocketFailHandler;

    /**
     * JwtWebSocketHandlerAdapter 構造方法
     *
     * @param jwtTokenProvider           JwtTokenProviderImpl 用於 JWT 憑證相關操作的實現類
     * @param fileProperties             FileProperties 用於操作文件上傳相關配置的類
     * @param fileUploadWebSocketHandler FileUploadWebSocketHandler 用於處理文件上傳的 WebSocketHandler
     */
    public JwtWebSocketHandlerAdapter(JwtTokenProviderImpl jwtTokenProvider, FileProperties fileProperties, FileUploadWebSocketHandler fileUploadWebSocketHandler, WebSocketFailHandler webSocketFailHandler) {
        super(createUpgradeStrategy(fileProperties.getUpload().getPayloadLength()));
        this.jwtTokenProvider = jwtTokenProvider;
        this.fileProperties = fileProperties;
        this.fileUploadWebSocketHandler = fileUploadWebSocketHandler;
        this.webSocketFailHandler = webSocketFailHandler;
        super.setSessionAttributePredicate(attributeKey -> attributeKey.startsWith("X-WebSocket-Error"));
    }


    /**
     * 創建 WebSocket 請求處理策略
     * 這裡主要是設置最大帧载荷长度，用於限制文件上傳的大小
     * 這裡的最大帧载荷长度是在配置文件中配置的，單位是 MB，所以這裡需要乘以 1024 * 1024
     * 這裡還重寫了 upgrade 方法，用於處理 Sec-WebSocket-Protocol 請求頭中的協議
     *
     * @param maxFramePayloadLength 最大帧载荷长度
     *
     * @return ReactorNettyRequestUpgradeStrategy 返回 WebSocket 升級策略
     */
    private static ReactorNettyRequestUpgradeStrategy createUpgradeStrategy(int maxFramePayloadLength) {
        WebsocketServerSpec.Builder builder = WebsocketServerSpec.builder().maxFramePayloadLength(maxFramePayloadLength * 1024 * 1024);
        return new ReactorNettyRequestUpgradeStrategy(builder) {
            @Override
            @NonNull
            public Mono<Void> upgrade(
                    @NonNull ServerWebExchange exchange,
                    @NonNull WebSocketHandler handler, @Nullable String subProtocol, @NonNull Supplier<HandshakeInfo> handshakeInfoFactory) {
                List<String> protocols = exchange.getRequest().getHeaders().get("Sec-WebSocket-Protocol");
                if (protocols != null && !protocols.isEmpty()) {
                    return super.upgrade(exchange, handler, protocols.getFirst(), handshakeInfoFactory);
                }
                return super.upgrade(exchange, handler, null, handshakeInfoFactory);
            }
        };
    }


    /**
     * 重寫 handleRequest 方法，用於處理 WebSocket 請求，當請求中包含 JWT 憑證時，進行驗證，並將用戶ID存入 ServerWebExchange 的屬性中
     *
     * @param exchange  ServerWebExchange 用於處理請求的交換器
     * @param wsHandler WebSocketHandler 用於處理 WebSocket 請求的處理器
     *
     * @return Mono<Void> 返回一個 Mono 對象
     */
    @Override
    @NonNull
    public Mono<Void> handleRequest(@NonNull ServerWebExchange exchange, @NonNull WebSocketHandler wsHandler) {
        return Mono.defer(() -> {
            String token = extractTokenFromProtocol(exchange);
            if (token == null) {
                return failWithError(exchange, ValidationException.ErrorCode.WEBSOCKET_PROTOCOL_ERROR);
            }

            return jwtTokenProvider
                    .validateToken(token, null)
                    .flatMap(userId -> super.handleRequest(exchange, session -> handleWebSocketSession(exchange, session, userId)))
                    .onErrorResume(ValidationException.class, e -> failWithError(exchange, e.getErrorCode()));
        });
    }


    /**
     * 從 WebSocket 請求標頭中提取 JWT Token
     *
     * @param exchange ServerWebExchange 用於處理請求的交換器
     */
    private String extractTokenFromProtocol(ServerWebExchange exchange) {
        List<String> protocols = exchange.getRequest().getHeaders().get("Sec-WebSocket-Protocol");
        if (protocols == null || protocols.isEmpty()) {
            return null;
        }
        return protocols.stream().filter(protocol -> protocol.startsWith("jwt.")).map(protocol -> protocol.substring(4)).findFirst().orElse(null);
    }


    /**
     * 失敗時設置錯誤標頭，並使用 {@link WebSocketFailHandler} 處理請求
     *
     * @param exchange  ServerWebExchange 用於處理請求的交換器
     * @param errorCode 錯誤代碼
     */
    private Mono<Void> failWithError(ServerWebExchange exchange, ValidationException.ErrorCode errorCode) {
        return exchange.getSession().flatMap(session -> {
            session.getAttributes().put("X-WebSocket-Error", errorCode.name());
            return super.handleRequest(exchange, webSocketFailHandler);
        });
    }


    /**
     * 轉換 WebSocketSession，並交給 {@link FileUploadWebSocketHandler} 處理
     *
     * @param exchange ServerWebExchange 用於處理請求的交換器
     * @param session  WebSocketSession 用於處理 WebSocket 請求的處理器
     * @param userId   用戶ID
     */
    private Mono<Void> handleWebSocketSession(ServerWebExchange exchange, WebSocketSession session, Long userId) {
        if (session instanceof ReactorNettyWebSocketSession nettySession) {
            try {
                NettyDataBufferFactory bufferFactory = (NettyDataBufferFactory) exchange.getResponse().bufferFactory();
                Method getDelegateMethod = AbstractWebSocketSession.class.getDeclaredMethod("getDelegate");
                getDelegateMethod.setAccessible(true);
                ReactorNettyWebSocketSession.WebSocketConnection delegate = (ReactorNettyWebSocketSession.WebSocketConnection) getDelegateMethod.invoke(
                        nettySession);
                CustomWebSocketSession customSession = new CustomWebSocketSession(delegate,
                                                                                  nettySession.getHandshakeInfo(),
                                                                                  bufferFactory,
                                                                                  fileProperties.getUpload().getPayloadLength() * 1024 * 1024,
                                                                                  userId.toString()
                );
                return fileUploadWebSocketHandler.handle(customSession);
            } catch (Exception e) {
                return Mono.error(e);
            }
        }
        return fileUploadWebSocketHandler.handle(session);
    }

}