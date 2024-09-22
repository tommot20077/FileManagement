package xyz.dowob.filemanagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import xyz.dowob.filemanagement.component.handler.FileUploadWebSocketHandler;
import xyz.dowob.filemanagement.component.handler.OnlineFileWebSocketHandler;
import xyz.dowob.filemanagement.component.manager.JwtWebSocketHandlerAdapter;
import xyz.dowob.filemanagement.config.properties.FileProperties;

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
public class WebSocketConfig {
    /**
     * WebSocket 路徑前綴
     * 為所有 WebSocket 路徑添加前綴
     */
    private final static String WEBSOCKET_PATH_PREFIX = "/ws";

    /**
     * 文件上傳 WebSocket 處理器
     */
    private final FileUploadWebSocketHandler fileUploadWebSocketHandler;

    /**
     * 在線文件 WebSocket 處理器
     */
    private final OnlineFileWebSocketHandler onlineFileWebSocketHandler;

    /**
     * JWT WebSocket 處理器適配器
     */
    private final JwtWebSocketHandlerAdapter jwtWebSocketHandlerAdapter;

    /**
     * 檔案上傳 WebSocket 路徑
     * 由 {@link FileProperties#getGlobal().getWebSocketPathPrefix()} 和 {@link FileProperties#getUpload().getUploadWebSocketPath()} 組成
     */
    private final String fileUploadWebSocketPath;

    /**
     * 在線文件編輯 WebSocket 路徑
     * 由 {@link FileProperties#getGlobal().getWebSocketPathPrefix()} 和 {@link FileProperties#getUpload().getEditOnlineFileWebSocketPath()} 組成
     */
    private final String onlineFileEditWebSocketPath;

    /**
     * WebSocketConfig 的構造函數
     *
     * @param fileUploadWebSocketHandler 文件上傳 WebSocket 處理器
     * @param onlineFileWebSocketHandler 在線文件 WebSocket 處理器
     * @param jwtWebSocketHandlerAdapter JWT WebSocket 處理器適配器
     * @param fileProperties             文件配置屬性
     */
    public WebSocketConfig(FileUploadWebSocketHandler fileUploadWebSocketHandler, OnlineFileWebSocketHandler onlineFileWebSocketHandler, JwtWebSocketHandlerAdapter jwtWebSocketHandlerAdapter, FileProperties fileProperties) {
        Assert.isTrue(StringUtils.hasText(fileProperties.getUpload().getUploadWebSocketPath()), "WebSocket 上傳路徑不能為空");
        Assert.isTrue(StringUtils.hasText(fileProperties.getUpload().getEditOnlineFileWebSocketPath()), "WebSocket 編輯路徑不能為空");

        this.fileUploadWebSocketHandler = fileUploadWebSocketHandler;
        this.onlineFileWebSocketHandler = onlineFileWebSocketHandler;
        this.jwtWebSocketHandlerAdapter = jwtWebSocketHandlerAdapter;
        this.fileUploadWebSocketPath = WEBSOCKET_PATH_PREFIX + fileProperties.getUpload().getUploadWebSocketPath();
        this.onlineFileEditWebSocketPath = WEBSOCKET_PATH_PREFIX + fileProperties.getUpload().getEditOnlineFileWebSocketPath();
    }


    /**
     * 配置 WebSocket 映射，將 WebSocket 請求映射到對應的處理器
     *
     * @return HandlerMapping 映射處理對象
     */
    @Bean
    public HandlerMapping webSocketMapping() {
        final Map<String, WebSocketHandler> map = new HashMap<>();
        map.put(fileUploadWebSocketPath, fileUploadWebSocketHandler);
        map.put(onlineFileEditWebSocketPath, onlineFileWebSocketHandler);

        final SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setOrder(Ordered.HIGHEST_PRECEDENCE);
        mapping.setUrlMap(map);
        return mapping;
    }


    /**
     * 配置 WebSocket 處理器適配器
     *
     * @return WebSocketHandlerAdapter WebSocket 處理器適配器
     */
    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter(jwtWebSocketHandlerAdapter);
    }
}
