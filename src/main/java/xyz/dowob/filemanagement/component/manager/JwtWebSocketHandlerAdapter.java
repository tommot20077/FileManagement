package xyz.dowob.filemanagement.component.manager;

import jakarta.annotation.Nullable;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.ResponseEntity;
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
import xyz.dowob.filemanagement.dto.api.ApiResponseDTO;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.unity.ResponseUnity;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
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
    private final JwtTokenProviderImpl jwtTokenProvider;

    private final FileProperties fileProperties;

    private final FileUploadWebSocketHandler fileUploadWebSocketHandler;

    public JwtWebSocketHandlerAdapter(
            JwtTokenProviderImpl jwtTokenProvider, FileProperties fileProperties, FileUploadWebSocketHandler fileUploadWebSocketHandler) {
        super(createUpgradeStrategy(fileProperties.getUpload().getMaxFramePayloadLength()));
        this.jwtTokenProvider = jwtTokenProvider;
        this.fileProperties = fileProperties;
        this.fileUploadWebSocketHandler = fileUploadWebSocketHandler;
    }

    private static ReactorNettyRequestUpgradeStrategy createUpgradeStrategy(int maxFramePayloadLength) {
        WebsocketServerSpec.Builder builder = WebsocketServerSpec.builder().maxFramePayloadLength(maxFramePayloadLength * 1024 * 1024);
        return new ReactorNettyRequestUpgradeStrategy(builder) {
            @Override
            @NonNull
            public Mono<Void> upgrade(
                    @NonNull ServerWebExchange exchange,
                    @NonNull WebSocketHandler handler,
                    @Nullable String subProtocol,
                    @NonNull Supplier<HandshakeInfo> handshakeInfoFactory) {
                List<String> protocols = exchange.getRequest().getHeaders().get("Sec-WebSocket-Protocol");
                if (protocols != null && !protocols.isEmpty()) {
                    return super.upgrade(exchange, handler, protocols.getFirst(), handshakeInfoFactory);
                }
                return super.upgrade(exchange, handler, null, handshakeInfoFactory);
            }
        };
    }

    @Override
    @NonNull
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
                                    log.info("nettySession: {}", nettySession);
                                    NettyDataBufferFactory bufferFactory = (NettyDataBufferFactory) exchange.getResponse().bufferFactory();
                                    Method getDelegateMethod = AbstractWebSocketSession.class.getDeclaredMethod("getDelegate");
                                    getDelegateMethod.setAccessible(true);
                                    ReactorNettyWebSocketSession.WebSocketConnection delegate =
                                            (ReactorNettyWebSocketSession.WebSocketConnection) getDelegateMethod.invoke(
                                            nettySession);
                                    CustomWebSocketSession customSession = new CustomWebSocketSession(delegate,
                                                                                                      nettySession.getHandshakeInfo(),
                                                                                                      bufferFactory,
                                                                                                      fileProperties
                                                                                                              .getUpload()
                                                                                                              .getMaxFramePayloadLength() * 1024 * 1024,
                                                                                                      userId.toString());
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
            ApiResponseDTO<?> apiResponse = createResponse(exchange, e.getErrorCode().getCode(), e.getMessage(), null);
            Mono<ResponseEntity<?>> responseEntity = createResponseEntity(apiResponse);
            return exchange
                    .getResponse()
                    .writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(responseEntity.toString().getBytes())));
        });
    }

}
