package xyz.dowob.filemanagement.repostiory;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.component.manager.JwtAuthenticationManager;
import xyz.dowob.filemanagement.config.properties.SecurityProperties;
import xyz.dowob.filemanagement.customenum.RoleEnum;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.unity.LogUnity;
import xyz.dowob.filemanagement.unity.ResponseUnity;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 基於 WebFlux 的 JWT 安全上下文倉儲實現。
 *
 * <p>實現 ServerSecurityContextRepository 介面，提供非阻塞的 JWT 令牌驗證機制。
 * 支援從多種請求類型（API、WEB、WebSocket）中提取 JWT 令牌，並根據驗證結果
 * 建立對應的安全上下文。採用策略模式處理不同協議的令牌驗證邏輯。
 *
 * <p>令牌提取來源包括：Authorization 標頭、HTTP Cookie、WebSocket 協議標頭。
 * 支援遊客模式，當未提供有效令牌且啟用遊客功能時，自動建立遊客安全上下文。
 * 對於 WebSocket 連線，採用特殊的錯誤處理機制以維持連線穩定性。
 *
 * <p>此實現為無狀態設計，不會持久化安全上下文，所有驗證操作均為即時執行。
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see ServerSecurityContextRepository
 * @see JwtAuthenticationManager
 */
@Component
public class JwtSecurityContextRepository implements ServerSecurityContextRepository, ResponseUnity {
    /**
     * Authorization 標頭中 JWT 令牌的標準前綴。
     * 符合 RFC 6750 Bearer Token 規範。
     */
    private static final String TOKEN_PREFIX = "Bearer ";

    /**
     * 登入端點的 URL 路徑。
     * 此路徑的請求將跳過 JWT 驗證處理。
     */
    private static final String LOGIN_URL = "/guest/login";

    /**
     * WebSocket 子協議標頭名稱。
     * 用於在 WebSocket 握手過程中傳遞 JWT 令牌。
     */
    private static final String WEBSOCKET_SEC_PROTOCOL = "Sec-WebSocket-Protocol";

    /**
     * 遊客使用者實體的靜態實例。
     * 在啟用遊客模式時用於建立匿名安全上下文。
     */
    private static final User GUEST_USER = new User();

    /**
     * JWT 身份驗證管理器，負責令牌的解析與驗證。
     */
    private final JwtAuthenticationManager authenticationManager;

    /**
     * 安全配置屬性，包含 Cookie 名稱和 WebSocket 令牌前綴等設定。
     */
    private final SecurityProperties securityProperties;

    /**
     * 用於從 Cookie 字串中提取 JWT 令牌的正則表達式模式。
     * 根據配置的 Cookie 名稱動態建立。
     */
    private final Pattern jwtCookieNamePattern;


    /**
     * 建構 JWT 安全上下文倉儲實例。
     *
     * <p>初始化過程中會驗證必要的配置參數，包括 JWT Cookie 名稱和
     * WebSocket 令牌前綴的有效性，並建立對應的正則表達式模式。
     *
     * @param authenticationManager JWT 身份驗證管理器
     * @param securityProperties 安全配置屬性
     * @throws IllegalArgumentException 當 Cookie 名稱或 WebSocket 令牌前綴為空時
     */
    public JwtSecurityContextRepository(JwtAuthenticationManager authenticationManager, SecurityProperties securityProperties) {
        this.authenticationManager = authenticationManager;
        this.securityProperties = securityProperties;

        Assert.isTrue(StringUtils.hasText(securityProperties.getCookie().getTokenName()), "JWT Cookie 名稱不能為空");
        this.jwtCookieNamePattern = Pattern.compile(securityProperties.getCookie().getTokenName() + "=([^;]+)");

        Assert.isTrue(StringUtils.hasText(securityProperties.getJwtToken().getWebSocketTokenPrefix()), "WebSocket Token 前綴不能為空");
    }


    /**
     * 初始化遊客使用者實體的預設屬性。
     *
     * <p>設定遊客使用者的基本資訊：ID 為 0、角色為 VISITOR、
     * 儲存限制和已使用空間均為 0。此方法在 Bean 初始化完成後執行。
     */
    @PostConstruct
    public void init() {
        GUEST_USER.setId(0L);
        GUEST_USER.setUsername("Guest");
        GUEST_USER.setPassword("Guest");
        GUEST_USER.setEmail("guest@example.com");
        GUEST_USER.setRole(RoleEnum.VISITOR);
        GUEST_USER.setStorageLimit(0L);
        GUEST_USER.setUsedStorage(0L);
    }


    /**
     * 不支援安全上下文的持久化操作。
     *
     * <p>由於採用無狀態的 JWT 驗證機制，不需要將安全上下文儲存至伺服器端。
     * 所有驗證資訊均透過令牌本身攜帶。
     *
     * @param exchange 伺服器 Web 交換物件
     * @param context 安全上下文
     * @return 永遠回傳 null
     */
    @Override
    public Mono<Void> save(ServerWebExchange exchange, SecurityContext context) {
        return null;
    }


    /**
     * 載入並建立請求對應的安全上下文。
     *
     * <p>根據請求類型採用不同的令牌提取策略：API 請求僅從 Authorization 標頭提取，
     * WEB 和 WebSocket 請求同時支援標頭和 Cookie 提取。WebSocket 請求額外支援
     * 從子協議標頭提取令牌。
     *
     * <p>令牌驗證成功時建立對應的安全上下文，失敗時根據請求類型決定錯誤處理方式：
     * WebSocket 請求建立錯誤驗證物件以維持連線，其他請求型別拋出驗證例外。
     * 當無法取得有效令牌且啟用遊客模式時，自動建立遊客安全上下文。
     *
     * @param exchange 伺服器 Web 交換物件
     * @return 包含安全上下文的 Mono，若無有效驗證則為空
     */
    @Override
    public Mono<SecurityContext> load(ServerWebExchange exchange) {
        if (exchange.getRequest().getPath().value().contains(LOGIN_URL)) {
            return Mono.empty();
        }

        RequestType requestType = RequestType.getRequestType(exchange);
        if (requestType == null) {
            return Mono.error(new ValidationException(ValidationException.ErrorCode.PATH_NOT_FOUND));
        }

        AtomicReference<String> token = new AtomicReference<>();
        String cookie = exchange.getRequest().getHeaders().getFirst(HttpHeaders.COOKIE);
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (requestType == RequestType.WEB_SOCKET) {
            extractJwtFromCookies(cookie).ifPresent(token::set);
            String protocol = exchange.getRequest().getHeaders().getFirst(WEBSOCKET_SEC_PROTOCOL);

            if (token.get() == null && protocol != null && protocol.startsWith(securityProperties.getJwtToken().getWebSocketTokenPrefix())) {
                token.set(protocol.substring(securityProperties.getJwtToken().getWebSocketTokenPrefix().length()));
            }
        } else {
            if (token.get() == null && (requestType == RequestType.WEB || requestType == RequestType.OTHER)) {
                extractJwtFromCookies(cookie).ifPresent(token::set);
            }

            if (token.get() == null && authHeader != null && authHeader.startsWith(TOKEN_PREFIX)) {
                token.set(authHeader.substring(TOKEN_PREFIX.length()));
            }
        }


        if (token.get() != null) {
            Authentication auth = new UsernamePasswordAuthenticationToken(token.get(), token.get());
            return authenticationManager
                    .authenticate(auth)
                    .map(authentication -> (SecurityContext) new SecurityContextImpl(authentication))
                    .onErrorResume(ValidationException.class, e -> Mono.defer(() -> {
                                       LogUnity.info(exchange, "JWT 憑證驗證失敗: %s", e.getMessage());
                                       if (requestType == RequestType.WEB_SOCKET) {
                                           Authentication errorAuthentication = createWebSocketErrorAuthentication(e.getErrorCode());
                                           return Mono.just(new SecurityContextImpl(errorAuthentication));
                                       }
                                       return Mono.error(new ValidationException(ValidationException.ErrorCode.JWT_TOKEN_INVALID));
                                   })
                    );
        } else {
            if (securityProperties.getGuestUser().isEnable()) {
                return createGuestSecurityContext();
            } else {
                ValidationException.ErrorCode errorCode = ValidationException.ErrorCode.UNAUTHORIZED;
                LogUnity.info(exchange, "請求中未包含 JWT Token，且未啟用遊客用戶，請求被拒絕");

                if (requestType == RequestType.WEB_SOCKET) {
                    Authentication errorAuthentication = createWebSocketErrorAuthentication(errorCode);
                    return Mono.just(new SecurityContextImpl(errorAuthentication));
                }
                return Mono.empty();
            }
        }
    }


    /**
     * 從 Cookie 字串中提取 JWT 令牌。
     *
     * <p>使用預編譯的正則表達式模式匹配配置的 Cookie 名稱，
     * 並提取對應的令牌值。
     *
     * @param cookieValue 完整的 Cookie 字串
     * @return 包含 JWT 令牌的 Optional，若未找到則為空
     */
    private Optional<String> extractJwtFromCookies(String cookieValue) {
        if (cookieValue == null) {
            return Optional.empty();
        }

        Matcher matcher = jwtCookieNamePattern.matcher(cookieValue);
        if (matcher.find()) {
            return Optional.of(matcher.group(1));
        }
        return Optional.empty();
    }

    /**
     * 建立 WebSocket 連線的錯誤身份驗證物件。
     *
     * <p>當 WebSocket 令牌驗證失敗時，建立特殊的驗證物件以維持連線狀態，
     * 避免連線中斷。錯誤資訊包含在驗證物件的詳細資料中。
     *
     * @param errorCode 驗證失敗的錯誤代碼
     * @return 包含錯誤資訊的身份驗證物件
     */
    private Authentication createWebSocketErrorAuthentication(ValidationException.ErrorCode errorCode) {
        Object principal = "ERROR_WEBSOCKET_AUTH_" + errorCode.name();

        UsernamePasswordAuthenticationToken errorToken = new UsernamePasswordAuthenticationToken(principal, null);
        errorToken.setDetails(errorCode);
        return errorToken;
    }

    /**
     * 建立遊客使用者的安全上下文。
     *
     * <p>為遊客使用者建立具有 VISITOR 角色權限的安全上下文，
     * 用於支援匿名存取受限功能的場景。
     *
     * @return 包含遊客安全上下文的 Mono
     */
    private Mono<SecurityContext> createGuestSecurityContext() {
        List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(RoleEnum.VISITOR.name()));
        Authentication guestAuth = new UsernamePasswordAuthenticationToken(GUEST_USER.getId(), null, authorities);
        return Mono.just(new SecurityContextImpl(guestAuth));
    }


    /**
     * 請求類型枚舉，定義不同協議的令牌提取策略。
     *
     * <p>根據請求路徑和標頭特徵識別請求類型，並採用對應的
     * JWT 令牌提取和驗證邏輯。
     */
    @Getter
    @RequiredArgsConstructor
    enum RequestType {
        /**
         * API 請求類型，僅從 Authorization 標頭提取令牌。
         */
        API("API協議"),

        /**
         * WEB 請求類型，支援從標頭和 Cookie 提取令牌。
         */
        WEB("WEB協議"),

        /**
         * WebSocket 請求類型，支援從 Cookie 和子協議標頭提取令牌。
         */
        WEB_SOCKET("WebSocket協議"),

        /**
         * 其他請求類型，包含文件和監控端點等功能性請求。
         */
        OTHER("其他協議");

        /**
         * 請求類型的顯示名稱。
         */
        private final String name;

        /**
         * 根據請求特徵識別請求類型。
         *
         * <p>依序檢查請求路徑前綴和標頭資訊，判斷請求所屬的協議類型。
         * 支援 WEB、WebSocket、文件/監控端點和 API 請求的自動識別。
         *
         * @param exchange 伺服器 Web 交換物件
         * @return 對應的請求類型，若無法識別則回傳 null
         */
        public static RequestType getRequestType(ServerWebExchange exchange) {
            String path = exchange.getRequest().getPath().value();
            HttpHeaders headers = exchange.getRequest().getHeaders();

            if (path.startsWith("/web")) {
                return WEB;
            }

            boolean isWebSocket = headers.containsKey("Upgrade") && "websocket".equalsIgnoreCase(headers.getFirst("Upgrade"));
            if (path.startsWith("/ws") && isWebSocket) {
                return WEB_SOCKET;
            }

            boolean isOther = path.startsWith("/docs") || path.startsWith("/actuator");
            if (isOther) {
                return OTHER;
            }

            return path.startsWith("/api") ? API : null;
        }
    }
}
