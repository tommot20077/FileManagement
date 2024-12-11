package xyz.dowob.filemanagement.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import xyz.dowob.filemanagement.component.handler.FileUploadWebSocketHandler;
import xyz.dowob.filemanagement.component.manager.JwtWebSocketHandlerAdapter;

import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket 配置類，用於配置 WebSocket 相關的配置
 *
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
    /**
     * 文件上傳 WebSocket 處理器
     */
    private final FileUploadWebSocketHandler fileUploadWebSocketHandler;

    /**
     * JWT WebSocket 處理器適配器
     */
    private final JwtWebSocketHandlerAdapter jwtWebSocketHandlerAdapter;


    /**
     * 配置 WebSocket 映射，將 WebSocket 請求映射到對應的處理器
     *
     * @return HandlerMapping
     */
    @Bean
    public HandlerMapping webSocketMapping() {
        final Map<String, WebSocketHandler> map = new HashMap<>();
        map.put("/ws/file/upload", fileUploadWebSocketHandler);

        final SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setOrder(Ordered.HIGHEST_PRECEDENCE);
        mapping.setUrlMap(map);
        return mapping;
    }

    /**
     * 配置 WebSocket 處理器適配器
     *
     * @return WebSocketHandlerAdapter
     */
    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter(jwtWebSocketHandlerAdapter);
    }
}
