package xyz.dowob.filemanagement.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.RequestUpgradeStrategy;
import org.springframework.web.reactive.socket.server.WebSocketService;
import org.springframework.web.reactive.socket.server.support.HandshakeWebSocketService;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import org.springframework.web.reactive.socket.server.upgrade.ReactorNettyRequestUpgradeStrategy;
import reactor.netty.http.server.WebsocketServerSpec;
import xyz.dowob.filemanagement.component.handler.FileUploadWebSocketHandler;
import xyz.dowob.filemanagement.component.manager.JwtWebSocketHandlerAdapter;
import xyz.dowob.filemanagement.component.provider.providerImplement.JwtTokenProviderImpl;
import xyz.dowob.filemanagement.config.properties.FileProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * @author yuan
 * @program FileManagement
 * @ClassName WebSocketConfig
 * @description
 * @create 2024-10-04 22:30
 * @Version 1.0
 **/
@Configuration
@EnableWebFlux
@RequiredArgsConstructor
public class WebSocketConfig {
    private final FileUploadWebSocketHandler fileUploadWebSocketHandler;

    private final FileProperties fileProperties;

    private final JwtTokenProviderImpl jwtTokenProvider;

    @Bean
    public HandlerMapping webSocketMapping() {
        final Map<String, WebSocketHandler> map = new HashMap<>();
        map.put("/ws/file/upload", fileUploadWebSocketHandler);

        final SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setOrder(Ordered.HIGHEST_PRECEDENCE);
        mapping.setUrlMap(map);
        return mapping;
    }

    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter(webSocketService());
    }

    @Bean
    public RequestUpgradeStrategy requestUpgradeStrategy() {
        WebsocketServerSpec.Builder builder = WebsocketServerSpec.builder();
        builder.maxFramePayloadLength(fileProperties.getUpload().getMaxFramePayloadLength() * 1024 * 1024);
        return new ReactorNettyRequestUpgradeStrategy(builder);
    }

    @Bean
    public WebSocketService webSocketService() {
        return new JwtWebSocketHandlerAdapter(requestUpgradeStrategy(), jwtTokenProvider);
    }

}
