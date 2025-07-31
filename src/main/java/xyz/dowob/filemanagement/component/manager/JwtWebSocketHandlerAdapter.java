package xyz.dowob.filemanagement.component.manager;

import jakarta.annotation.Nullable;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.adapter.ReactorNettyWebSocketSession;
import org.springframework.web.reactive.socket.server.support.HandshakeWebSocketService;
import org.springframework.web.reactive.socket.server.upgrade.ReactorNettyRequestUpgradeStrategy;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerResponse;
import reactor.netty.http.server.WebsocketServerSpec;
import xyz.dowob.filemanagement.annotation.HideSensitive;
import xyz.dowob.filemanagement.annotation.RecordLevel;
import xyz.dowob.filemanagement.annotation.SkipRecord;
import xyz.dowob.filemanagement.component.handler.CustomWebSocketSession;
import xyz.dowob.filemanagement.component.handler.FileUploadWebSocketHandler;
import xyz.dowob.filemanagement.component.handler.OnlineFileWebSocketHandler;
import xyz.dowob.filemanagement.component.handler.WebSocketFailHandler;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.config.properties.SecurityProperties;
import xyz.dowob.filemanagement.customenum.LogLevelEnum;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.service.serviceInterface.UserService;
import xyz.dowob.filemanagement.unity.LogUnity;
import xyz.dowob.filemanagement.unity.ResponseUnity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * WebSocket 安全適配器，實現基於 JWT 的響應式 WebSocket 連線安全管理機制。
 *
 * <p>本類是一個高度動態且安全的 WebSocket 連線處理適配器，負責管理 WebSocket 的整個生命週期和安全驗證流程。</p>
 *
 * <p>主要設計特點：</p>
 *
 * <p>1. 響應式安全驗證：
 *    - 繼承 {@link org.springframework.web.reactive.socket.server.support.HandshakeWebSocketService}
 *    - 實現 {@link xyz.dowob.filemanagement.unity.ResponseUnity} 通用響應處理
 *    - 支持基於 JWT 的安全認證與授權</p>
 *
 * <p>2. WebSocket 連線路由：
 *    - 動態路由至特定的 WebSocket 處理器：
 *      - {@link xyz.dowob.filemanagement.component.handler.FileUploadWebSocketHandler}
 *      - {@link xyz.dowob.filemanagement.component.handler.OnlineFileWebSocketHandler}
 *    - 當連線發生錯誤時，由 {@link xyz.dowob.filemanagement.component.handler.WebSocketFailHandler} 處理</p>
 *
 * <p>3. 高級連線管理：
 *    - 支持遊客模式連線
 *    - 動態提取和驗證 WebSocket 連線參數
 *    - 處理複雜的連線異常情況</p>
 *
 * <p>4. 安全性保護：
 *    - 嚴格驗證用戶身份和連線權限
 *    - 支持自定義 WebSocket 協議和負載大小
 *    - 提供細粒度的連線錯誤處理機制</p>
 *
 * <p>本類通過客製化的 WebSocket 升級策略和連線處理邏輯，確保了 WebSocket 連線的安全性、可靠性和可擴展性。</p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@Component
public class JwtWebSocketHandlerAdapter extends HandshakeWebSocketService implements ResponseUnity {
    /**
     * WebSocket 錯誤屬性名稱
     * 當 WebSocket 連線失敗時，將錯誤內容轉換為 JSON 格式，並回傳給客戶端
     */
    private static final String WEBSOCKET_ERROR_ATTRIBUTE = "X-WebSocket-Error";

    /**
     * WebSocket 授權協議請求頭，用於存儲 WebSocket 協議
     * 這個請求頭是由客戶端發送的，服務器會根據這個請求頭來選擇使用哪個 WebSocket 協議
     */
    private static final String WEBSOCKET_SEC_PROTOCOL = "Sec-WebSocket-Protocol";

    /**
     * WebSocket 路徑前綴
     * 用於設定 WebSocket 的路徑前綴，所有的 WebSocket 路徑都會以這個前綴開頭
     */
    private static final String WEBSOCKET_PATH_PREFIX = "/ws";

    /**
     * 用戶處理服務邏輯
     */
    private final UserService userService;

    /**
     * 安全設定屬性
     */
    private final SecurityProperties securityProperties;

    /**
     * 檔案上傳 WebSocket 處理器
     */
    private final FileUploadWebSocketHandler fileUploadWebSocketHandler;

    /**
     * 線上檔案 WebSocket 處理器
     */
    private final OnlineFileWebSocketHandler onlineFileWebSocketHandler;

    /**
     * WebSocket 連線失敗處理器
     */
    private final WebSocketFailHandler webSocketFailHandler;

    /**
     * 檔案上傳 WebSocket 路徑
     * 由 {@link FileProperties#getGlobal().getWebSocketPathPrefix()} 和 {@link FileProperties#getUpload().getUploadWebSocketPath()} 組成
     */
    private final String fileUploadWebSocketPath;

    /**
     * 在線檔案編輯 WebSocket 路徑
     * 由 {@link FileProperties#getGlobal().getWebSocketPathPrefix()} 和 {@link FileProperties#getUpload().getEditOnlineFileWebSocketPath()} 組成
     */
    private final String onlineFileEditWebSocketPath;

    /**
     * WebSocketHandlerAdapter 的構造函數
     * 將 WebSocketHandlerAdapter 的請求升級策略設定為 {@link ReactorNettyRequestUpgradeStrategy}
     * 並設定 WebSocket 錯誤屬性名稱
     * 檢查安全設定屬性中的 JWT Token 前綴、JWT Cookie 名稱和 WebSocket 路徑前綴是否為空
     * 並將 WebSocket 路徑前綴、檔案上傳 WebSocket 路徑和在線檔案編輯 WebSocket 路徑設定為對應的屬性
     *
     * @param userService                用戶服務
     * @param fileProperties             檔案設定屬性
     * @param securityProperties         安全設定屬性
     * @param fileUploadWebSocketHandler 檔案上傳 WebSocket 處理器
     * @param webSocketFailHandler       WebSocket 連線失敗處理器
     * @param onlineFileWebSocketHandler 在線檔案 WebSocket 處理器
     */
    public JwtWebSocketHandlerAdapter(UserService userService, FileProperties fileProperties, SecurityProperties securityProperties, FileUploadWebSocketHandler fileUploadWebSocketHandler, WebSocketFailHandler webSocketFailHandler, OnlineFileWebSocketHandler onlineFileWebSocketHandler) {
        super(createUpgradeStrategy(fileProperties.getUpload().getPayloadLength()));
        this.userService = userService;
        this.fileUploadWebSocketHandler = fileUploadWebSocketHandler;
        this.onlineFileWebSocketHandler = onlineFileWebSocketHandler;
        this.webSocketFailHandler = webSocketFailHandler;
        this.securityProperties = securityProperties;
        super.setSessionAttributePredicate(attributeKey -> attributeKey.startsWith(WEBSOCKET_ERROR_ATTRIBUTE));

        Assert.isTrue(StringUtils.hasText(securityProperties.getJwtToken().getWebSocketTokenPrefix()), "WebSocket Token 前綴不能為空");
        Assert.isTrue(StringUtils.hasText(securityProperties.getCookie().getTokenName()), "JWT Cookie 名稱不能為空");

        this.fileUploadWebSocketPath = WEBSOCKET_PATH_PREFIX + fileProperties.getUpload().getUploadWebSocketPath();
        this.onlineFileEditWebSocketPath = WEBSOCKET_PATH_PREFIX + fileProperties.getUpload().getEditOnlineFileWebSocketPath();
    }


    /**
     * 創建 WebSocket 升級策略
     * 使用 {@link WebsocketServerSpec} 來設定 WebSocket 的最大幀負載長度
     * 同時檢查請求的 WebSocket 協議是否存在，當前請求的 WebSocket 協議不為空時，則使用請求的 WebSocket 協議
     * 若為空則使用默認的 WebSocket 協議
     *
     * @param maxFramePayloadLength 最大幀負載長度
     *
     * @return ReactorNettyRequestUpgradeStrategy 升級策略
     */
    @HideSensitive
    private static ReactorNettyRequestUpgradeStrategy createUpgradeStrategy(DataSize maxFramePayloadLength) {
        WebsocketServerSpec.Builder builder = WebsocketServerSpec.builder().maxFramePayloadLength((int) maxFramePayloadLength.toBytes());
        return new ReactorNettyRequestUpgradeStrategy(builder) {
            @Override
            @NonNull
            public Mono<Void> upgrade(
                    @NonNull ServerWebExchange exchange,
                    @NonNull WebSocketHandler handler, @Nullable String subProtocol, @NonNull Supplier<HandshakeInfo> handshakeInfoFactory) {
                List<String> protocols = exchange.getRequest().getHeaders().get(WEBSOCKET_SEC_PROTOCOL);
                String selectedProtocol = (protocols != null && !protocols.isEmpty()) ? protocols.getFirst() : null;

                ServerHttpResponse response = exchange.getResponse();
                if (response.isCommitted()) {
                    LogUnity.info(exchange, "WebSocket 升級請求失敗，響應已提交");
                    return Mono.error(new ValidationException(ValidationException.ErrorCode.WEBSOCKET_CONNECTION_ERROR));
                }

                HttpServerResponse reactorResponse = org.springframework.http.server.reactive.ServerHttpResponseDecorator.getNativeResponse(response);
                HandshakeInfo handshakeInfo = handshakeInfoFactory.get();
                NettyDataBufferFactory bufferFactory = (NettyDataBufferFactory) response.bufferFactory();
                WebsocketServerSpec.Builder builder = WebsocketServerSpec.builder().maxFramePayloadLength((int) maxFramePayloadLength.toBytes());
                WebsocketServerSpec spec = (selectedProtocol != null) ? builder.protocols(selectedProtocol).build() : builder.build();


                return Mono.defer(() -> {
                    return reactorResponse.sendWebsocket((in, out) -> {
                                                             ReactorNettyWebSocketSession session = new ReactorNettyWebSocketSession(in,
                                                                                                                                     out,
                                                                                                                                     handshakeInfo,
                                                                                                                                     bufferFactory,
                                                                                                                                     spec.maxFramePayloadLength()
                                                             );
                                                             return handler.handle(session);
                                                         }, spec
                    );
                });
            }
        };
    }


    /**
     * 處理 WebSocket 連線請求
     * 當請求中沒有 JWT Token 時，則使用 {@link #switchGuestHandler(ServerWebExchange)} 方法處理請求
     * 否則使用 {@link #handleWebSocketRequest(ServerWebExchange, User)} 方法處理請求
     * 若其中發生錯誤或是找不到用戶，則使用 {@link #failWithError} 方法處理請求
     *
     * @param exchange  請求
     * @param wsHandler WebSocketHandler 用於處理 WebSocket 請求的處理器
     *
     * @return Mono<Void>
     */
    @Override
    @NonNull
    @RecordLevel(LogLevelEnum.DEBUG)
    public Mono<Void> handleRequest(@NonNull ServerWebExchange exchange, @NonNull WebSocketHandler wsHandler) {
        Mono<Authentication> authMono = ReactiveSecurityContextHolder.getContext().map(SecurityContext::getAuthentication);
        return authMono.switchIfEmpty(Mono.defer(() -> switchGuestHandler(exchange).then(Mono.empty()))).flatMap(authentication -> {
            if (authentication != null && authentication.getDetails() instanceof ValidationException.ErrorCode errorCode) {
                return this.failWithError(exchange, new ValidationException(errorCode));
            }

            if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof Long userId) {
                return userService
                        .getById(userId)
                        .flatMap(user -> handleWebSocketRequest(exchange, user))
                        .onErrorResume(e -> failWithError(exchange, e));
            }
            return this.failWithError(exchange, new ValidationException(ValidationException.ErrorCode.UNAUTHORIZED));
        }).onErrorResume(e -> failWithError(exchange, e));
    }


    /**
     * 處理遊客請求
     * 當安全設定屬性中的遊客用戶啟用時，則使用 {@link UserService#getById)}} 方法獲取遊客用戶
     * 並將請求轉發給 {@link #handleWebSocketRequest(ServerWebExchange, User)} 方法處理
     * 否則將交由 {@link #failWithError} 方法處理錯誤
     *
     * @param exchange 用於處理請求的交換器
     *
     * @return Mono<Void>
     */
    private Mono<Void> switchGuestHandler(ServerWebExchange exchange) {
        if (securityProperties.getGuestUser().isEnable()) {
            return userService.getById(0L).flatMap(user -> handleWebSocketRequest(exchange, user));
        }
        return failWithError(exchange, new ValidationException(ValidationException.ErrorCode.UNAUTHORIZED));
    }


    /**
     * 處理失敗的請求，此為重載方法，表示尚未取得用戶的請求
     *
     * @param exchange  ServerWebExchange 用於處理請求的交換器
     * @param throwable 當前請求的異常
     */
    private Mono<Void> failWithError(ServerWebExchange exchange, Throwable throwable) {
        return failWithError(exchange, null, throwable);
    }


    /**
     * 處理 WebSocket 連線請求
     * 當從 {@link #getConnectionInfo(ServerWebExchange)} 獲取到的連接處理訊息為空時，則回傳錯誤
     * 否則將請求轉發給後續的 WebSocketHandler 處理器進行處理
     *
     * @param exchange ServerWebExchange 用於處理請求的交換器
     * @param user     用戶
     *
     * @return Mono<Void>
     */
    private Mono<Void> handleWebSocketRequest(ServerWebExchange exchange, User user) {
        ConnectionInfo info = getConnectionInfo(exchange);
        if (info == null) {
            return failWithError(exchange, user,new ValidationException(ValidationException.ErrorCode.PATH_NOT_FOUND));
        }
        return super.handleRequest(exchange, session -> handleWebSocketSession(session, user, info));
    }


    /**
     * 獲取請求的連接處理訊息
     * 會依照請求的路徑來獲取對應的連接處理訊息並將所使用的處理器設定
     * 並解析請求中的檔案 ID 將其與 Exchange 中的請求 ID 和用戶 IP 放入 {@link ConnectionInfo#attributes} 中
     * 當請求的路徑不符合預設的 WebSocket 路徑時或者請求的檔案 ID 無效時，則回傳錯誤
     *
     * @param exchange 用於處理請求的交換器
     *
     * @return ConnectionInfo 連接處理訊息
     */
    private ConnectionInfo getConnectionInfo(ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().value();
        if (path.equals(fileUploadWebSocketPath)) {
            return new ConnectionInfo(fileUploadWebSocketHandler);
        } else if (path.equals(onlineFileEditWebSocketPath)) {
            Long fileId = extractFileId(exchange);
            if (fileId == null) {
                return null;
            }
            ConnectionInfo connectionInfo = new ConnectionInfo(onlineFileWebSocketHandler);
            connectionInfo.setAttribute("fileId", fileId);
            connectionInfo.setAttribute("clientIp", exchange.getAttributes().get("clientIp"));
            connectionInfo.setAttribute("requestId", exchange.getAttributes().get("requestId"));
            return connectionInfo;
        }
        return null;
    }


    /**
     * 轉換 WebSocketSession，並交給 {@link FileUploadWebSocketHandler} 處理
     *
     * @param session        WebSocketSession 用於處理 WebSocket 請求的處理器
     * @param user           用戶
     * @param connectionInfo 連接處理訊息
     *
     * @return Mono<Void>
     */
    @RecordLevel(LogLevelEnum.DEBUG)
    private Mono<Void> handleWebSocketSession(WebSocketSession session, User user, ConnectionInfo connectionInfo) {
        CustomWebSocketSession customSession = new CustomWebSocketSession(session, user);
        connectionInfo.getAttributes().forEach(customSession::setAttribute);
        return connectionInfo.getHandler().handle(customSession);
    }


    /**
     * 失敗時設定錯誤標頭，並使用 {@link WebSocketFailHandler} 處理請求
     *
     * @param exchange  ServerWebExchange 用於處理請求的交換器
     * @param throwable 當前請求的異常
     */
    private Mono<Void> failWithError(ServerWebExchange exchange, User user, Throwable throwable) {
        if (exchange.getResponse().isCommitted()) {
            LogUnity.info(exchange, "響應已提交，無法處理錯誤: %s", throwable.getMessage());
            return Mono.empty();
        }

        String errorCodeName;
        if (throwable instanceof ValidationException validationException) {
            LogUnity.info(exchange, "用戶 WebSocket 連線請求錯誤: %s ", validationException.getMessage());
            errorCodeName = validationException.getErrorCode().name();
        } else {
            LogUnity.error(exchange, "用戶 WebSocket 連線發生非預期的錯誤: ", throwable);
            errorCodeName = ValidationException.ErrorCode.WEBSOCKET_CONNECTION_ERROR.name();
        }

        ConnectionInfo connectionInfo = new ConnectionInfo(webSocketFailHandler);
        connectionInfo.setAttribute(WEBSOCKET_ERROR_ATTRIBUTE, errorCodeName);
        return super
                .handleRequest(exchange, session -> handleWebSocketSession(session, user, connectionInfo))
                .onErrorResume(upgradeOrHandlerError -> {
                    LogUnity.error(exchange, "處理連線失敗的 WebSocket 協議時，升級發生未預期錯誤", upgradeOrHandlerError);
                    return Mono.empty();
                });
    }


    /**
     * 從請求中提取檔案 ID
     * 當請求的檔案 ID 為空或不符合數字格式時，則回傳錯誤
     *
     * @param exchange 用於處理請求的交換器
     *
     * @return Long 檔案 ID
     */
    private Long extractFileId(ServerWebExchange exchange) {
        Map<String, String> query = exchange.getRequest().getQueryParams().toSingleValueMap();
        String fileId = query.get("fileId");
        if (fileId != null && fileId.matches("\\d+")) {
            return Long.parseLong(fileId);
        }
        return null;
    }

    /**
     * 連接處理訊息
     * 用於存儲 WebSocketHandler 和請求的屬性
     * 包含 {@link WebSocketHandler} 和 {@link HashMap} 屬性
     */
    @RequiredArgsConstructor
    private static class ConnectionInfo {
        /**
         * WebSocketHandler 用於處理 WebSocket 請求的處理器
         */
        private final WebSocketHandler handler;


        /**
         * 請求的屬性
         * 包含請求的檔案 ID 和用戶請求的相關參數
         */
        private final HashMap<String, Object> attributes = new HashMap<>();


        /**
         * 獲取請求的 WebSocketHandler
         *
         * @return WebSocketHandler 用於處理 WebSocket 請求的處理器
         */
        @SkipRecord
        public WebSocketHandler getHandler() {
            return handler;
        }


        /**
         * 設定請求的屬性
         *
         * @param key   屬性鍵
         * @param value 屬性值
         */
        @SkipRecord
        public void setAttribute(String key, Object value) {
            attributes.put(key, value);
        }


        /**
         * 獲取請求的屬性
         *
         * @param key 屬性鍵
         *
         * @return 屬性值
         */
        @SkipRecord
        public Object getAttribute(String key) {
            return attributes.get(key);
        }


        /**
         * 獲取請求的所有屬性
         *
         * @return 請求的所有屬性
         */
        @SkipRecord
        public Map<String, Object> getAttributes() {
            return attributes;
        }
    }
}