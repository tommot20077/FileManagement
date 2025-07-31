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
 * WebSocket 反應式設定類，負責整合 WebSocket 與 WebFlux 的反應式架構。
 * <p>
 * 此設定類建立了一個完整的 WebSocket 應用程式框架，支援即時雙向通訊功能。
 * 主要用於檔案管理系統中的以下場景：
 * <ol>
 *   <li>即時檔案上傳：支援大檔案的分塊上傳與進度回報</li>
 *   <li>線上檔案編輯：實時協作編輯功能，支援多人同時編輯同一檔案</li>
 *   <li>系統狀態通知：即時推送系統狀態變化訊息給用戶</li>
 *   <li>進度監控：即時展示檔案處理進度和狀態變更</li>
 * </ol>
 * <p>
 * 架構特性：
 * <ol>
 *   <li>路由分離：不同功能使用獨立的 WebSocket 端點</li>
 *   <li>JWT 認證整合：全部 WebSocket 連接都必須通過 JWT 認證</li>
 *   <li>反應式設計：與 WebFlux 無縫整合，支援非阻塞 I/O</li>
 *   <li>高優先級：設定最高優先級確保 WebSocket 路由優先處理</li>
 * </ol>
 * <p>
 * 安全性考量：
 * - 所有 WebSocket 連接都需要有效的 JWT 令牌
 * - 使用自訂的 {@link JwtWebSocketHandlerAdapter} 進行認證
 * - 支援連接級別的權限控制
 * - 防止未授權的 WebSocket 連接
 * <p>
 * 此設定確保 WebSocket 服務的高效能、高可用性和高安全性。
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@Configuration
@EnableWebFlux
public class WebSocketConfig {
    /**
     * WebSocket 路徑前綴常數。
     * <p>
     * 為所有 WebSocket 端點提供統一的路徑前綴，便於管理和識別 WebSocket 服務。
     * 此前綴會與具體的功能路徑結合，形成完整的 WebSocket URL。
     */
    private final static String WEBSOCKET_PATH_PREFIX = "/ws";

    /**
     * 檔案上傳 WebSocket 處理器實例。
     * <p>
     * 負責處理檔案上傳相關的 WebSocket 連接和訊息交換，
     * 支援分塊上傳、進度回報、上傳狀態通知等功能。
     */
    private final FileUploadWebSocketHandler fileUploadWebSocketHandler;

    /**
     * 線上檔案編輯 WebSocket 處理器實例。
     * <p>
     * 負責處理線上檔案編輯相關的 WebSocket 連接和訊息交換，
     * 支援即時協作編輯、資料同步、衝突解決等功能。
     */
    private final OnlineFileWebSocketHandler onlineFileWebSocketHandler;

    /**
     * JWT WebSocket 處理器適配器實例。
     * <p>
     * 提供 WebSocket 連接的 JWT 認證功能，確保所有 WebSocket 連接都通過安全驗證。
     * 與 HTTP API 的 JWT 認證保持一致的安全標準。
     */
    private final JwtWebSocketHandlerAdapter jwtWebSocketHandlerAdapter;

    /**
     * 檔案上傳 WebSocket 完整路徑。
     * <p>
     * 由全域 WebSocket 前綴和檔案上傳特定路徑組合而成。
     * 客戶端需要連接到此路徑來進行檔案上傳操作。
     * <p>
     * 路徑組成：{@value WEBSOCKET_PATH_PREFIX} + {@link FileProperties.Upload#getUploadWebSocketPath()}
     */
    private final String fileUploadWebSocketPath;

    /**
     * 線上檔案編輯 WebSocket 完整路徑。
     * <p>
     * 由全域 WebSocket 前綴和線上編輯特定路徑組合而成。
     * 客戶端需要連接到此路徑來進行線上檔案編輯操作。
     * <p>
     * 路徑組成：{@value WEBSOCKET_PATH_PREFIX} + {@link FileProperties.Upload#getEditOnlineFileWebSocketPath()}
     */
    private final String onlineFileEditWebSocketPath;

    /**
     * 建構 WebSocket 設定實例，初始化所有必要的 WebSocket 組件和路徑。
     * <p>
     * 透過依賴注入接收所有必要的 WebSocket 處理器和設定屬性，
     * 同時驗證設定的有效性並構建完整的 WebSocket 路徑。
     * <p>
     * 初始化過程：
     * <ol>
     *   <li>驗證檔案上傳路徑是否為空</li>
     *   <li>驗證線上編輯路徑是否為空</li>
     *   <li>構建完整的 WebSocket URL 路徑</li>
     *   <li>儲存所有必要的組件參考</li>
     * </ol>
     * <p>
     * 驗證範例：
     * - 上傳路徑：/ws + /file/upload = /ws/file/upload
     * - 編輯路徑：/ws + /file/editing = /ws/file/editing
     *
     * @param fileUploadWebSocketHandler 檔案上傳 WebSocket 處理器，負責處理檔案上傳相關的 WebSocket 事件
     * @param onlineFileWebSocketHandler 線上檔案 WebSocket 處理器，負責處理線上編輯相關的 WebSocket 事件
     * @param jwtWebSocketHandlerAdapter JWT WebSocket 處理器適配器，提供 WebSocket 連接的認證功能
     * @param fileProperties             檔案系統設定屬性，包含 WebSocket 路徑設定資訊
     * @throws IllegalArgumentException 當 WebSocket 路徑設定為空或無效時拋出
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
     * 建立 WebSocket 路由映射，將不同的 WebSocket 路徑與相應的處理器進行綁定。
     * <p>
     * 此方法建立了一個簡單而高效的 URL 路由系統，將不同的 WebSocket 端點
     * 與其專用的處理器關聯起來。每個處理器都專門負責一種特定的功能。
     * <p>
     * 路由映射表：
     * - {@link #fileUploadWebSocketPath} → {@link #fileUploadWebSocketHandler}
     * - {@link #onlineFileEditWebSocketPath} → {@link #onlineFileWebSocketHandler}
     * <p>
     * 特性設定：
     * <ol>
     *   <li>最高優先級：設定為 {@link Ordered#HIGHEST_PRECEDENCE} 確保 WebSocket 路由優先處理</li>
     *   <li>精確匹配：使用精確的路徑匹配，避免路由衝突</li>
     *   <li>分離關伸：不同功能使用獨立的處理器，保持代碼清晰</li>
     *   <li>容易擴展：新增 WebSocket 功能時只需新增映射關係</li>
     * </ol>
     * <p>
     * 效能優化：
     * - 使用 HashMap 提供 O(1) 的路由查找速度
     * - 預先建立所有路由映射，運行期無額外開銷
     * - 最高優先級設定確保 WebSocket 快速路由
     *
     * @return 設定完成的 WebSocket 路由映射器
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
     * 建立 WebSocket 處理器適配器，整合 JWT 認證與 WebSocket 連接管理。
     * <p>
     * 此適配器將 Spring WebFlux 的 WebSocket 支援與自訂的 JWT 認證機制進行整合，
     * 確保所有 WebSocket 連接都通過安全驗證。
     * <p>
     * 功能特性：
     * <ol>
     *   <li>JWT 認證整合：在 WebSocket 握手時驗證 JWT 令牌</li>
     *   <li>連接管理：管理 WebSocket 連接的建立、維持和關閉</li>
     *   <li>安全控制：拒絕未授權的 WebSocket 連接嘗試</li>
     *   <li>錯誤處理：統一處理 WebSocket 認證失敗和連接錯誤</li>
     * </ol>
     * <p>
     * 認證流程：
     * <ol>
     *   <li>客戶端發起 WebSocket 連接</li>
     *   <li>適配器提取並驗證 JWT 令牌</li>
     *   <li>驗證成功後建立 WebSocket 連接</li>
     *   <li>驗證失敗則拒絕連接並回傳錯誤</li>
     * </ol>
     * <p>
     * 此適配器確保 WebSocket 服務與 HTTP API 保持一致的安全標準。
     *
     * @return 設定完成的 WebSocket 處理器適配器
     */
    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter(jwtWebSocketHandlerAdapter);
    }
}
