package xyz.dowob.filemanagement.component.manager;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.server.RequestUpgradeStrategy;
import org.springframework.web.reactive.socket.server.support.HandshakeWebSocketService;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.component.provider.providerImplement.JwtTokenProviderImpl;
import xyz.dowob.filemanagement.dto.api.ApiResponseDTO;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.unity.ResponseUnity;

import java.util.List;
import java.util.Optional;

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

    public JwtWebSocketHandlerAdapter(RequestUpgradeStrategy requestUpgradeStrategy, JwtTokenProviderImpl jwtTokenProvider) {
        super(requestUpgradeStrategy);
        this.jwtTokenProvider = jwtTokenProvider;
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
                        return wsHandler.handle((WebSocketSession) exchange);
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
