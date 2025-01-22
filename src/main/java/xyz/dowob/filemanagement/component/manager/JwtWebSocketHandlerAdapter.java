package xyz.dowob.filemanagement.component.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.Nullable;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.adapter.AbstractWebSocketSession;
import org.springframework.web.reactive.socket.adapter.ReactorNettyWebSocketSession;
import org.springframework.web.reactive.socket.server.support.HandshakeWebSocketService;
import org.springframework.web.reactive.socket.server.upgrade.ReactorNettyRequestUpgradeStrategy;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.WebsocketServerSpec;
import xyz.dowob.filemanagement.component.handler.CustomWebSocketSession;
import xyz.dowob.filemanagement.component.handler.FileUploadWebSocketHandler;
import xyz.dowob.filemanagement.component.provider.providerImplement.JwtTokenProviderImpl;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.data.api.ApiResponseDTO;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.unity.ResponseUnity;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
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

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * JwtWebSocketHandlerAdapter 構造方法
     *
     * @param jwtTokenProvider           JwtTokenProviderImpl 用於 JWT 憑證相關操作的實現類
     * @param fileProperties             FileProperties 用於操作文件上傳相關配置的類
     * @param fileUploadWebSocketHandler FileUploadWebSocketHandler 用於處理文件上傳的 WebSocketHandler
     */
    public JwtWebSocketHandlerAdapter(JwtTokenProviderImpl jwtTokenProvider, FileProperties fileProperties, FileUploadWebSocketHandler fileUploadWebSocketHandler) {
        super(createUpgradeStrategy(fileProperties.getUpload().getPayloadLength()));
        this.jwtTokenProvider = jwtTokenProvider;
        this.fileProperties = fileProperties;
        this.fileUploadWebSocketHandler = fileUploadWebSocketHandler;
        this.objectMapper.registerModule(new JavaTimeModule());
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
                    @NonNull WebSocketHandler handler,
                    @Nullable String subProtocol, @NonNull Supplier<HandshakeInfo> handshakeInfoFactory) {
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
    // todo 憑證錯誤回傳回應
    public Mono<Void> handleRequest(@NonNull ServerWebExchange exchange, @NonNull WebSocketHandler wsHandler) {
        return Mono.defer(() -> {
            List<String> protocols = exchange.getRequest().getHeaders().get("Sec-WebSocket-Protocol");
            if (protocols != null && !protocols.isEmpty()) {
                Optional<String> protocolOption = protocols.stream().filter(protocol -> protocol.startsWith("jwt.")).findFirst();
                if (protocolOption.isPresent()) {
                    String protocol = protocolOption.get();
                    String token = protocol.substring(4);
                    return jwtTokenProvider.validateToken(token, null).flatMap(userId -> {
                        exchange.getAttributes().put("userId", userId);
                        exchange.getResponse().getHeaders().add("Sec-WebSocket-Protocol", protocol);
                        return super.handleRequest(exchange, session -> {
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
                                                                                                      fileProperties
                                                                                                              .getUpload()
                                                                                                              .getPayloadLength() * 1024 * 1024,
                                                                                                      userId.toString()
                                    );
                                    return fileUploadWebSocketHandler.handle(customSession);
                                } catch (Exception e) {
                                    log.error("Error: ", e);
                                    return Mono.error(e);
                                }
                            }
                            return fileUploadWebSocketHandler.handle(session);
                        });
                    });
                }
            }
            return Mono.error(new ValidationException(ValidationException.ErrorCode.AUTHENTICATION_FAILED));
        }).onErrorResume(ValidationException.class, e -> {
            try {
                ApiResponseDTO<?> apiResponse = createResponse(exchange, e.getErrorCode().getCode(), e.getMessage(), null);
                byte[] responseBytes = objectMapper.writeValueAsBytes(apiResponse);

                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

                return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(responseBytes)));
            } catch (Exception jsonException) {
                log.error("序列化結果時發生錯誤: ", jsonException);
                return Mono.error(jsonException);
            }
        });
    }
}
//todo 前端頁面顯示、檔案下載功能、用戶檔案的歷程記錄、用戶個人檔案的管理、檔案預覽、檔案分享、檔案容量上限