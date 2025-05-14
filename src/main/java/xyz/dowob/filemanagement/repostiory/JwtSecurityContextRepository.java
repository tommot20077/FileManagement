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
 * Spring Security 上下文存儲庫，用於保存和加載 SecurityContext
 * 這裡主要用於 JWT 的驗證，從請求中獲取 JWT Token 進行驗證
 *
 * @author yuan
 * @program FileManagement
 * @ClassName SecurityContextRepository
 * @description
 * @create 2024-09-25 00:44
 * @Version 1.0
 **/
@Component
public class JwtSecurityContextRepository implements ServerSecurityContextRepository, ResponseUnity {
    /**
     * JWT 憑證的前綴
     */
    private static final String TOKEN_PREFIX = "Bearer ";

    /**
     * 登錄請求的 URL
     */
    private static final String LOGIN_URL = "/guest/login";

    /**
     * WebSocket 授權協議請求頭，用於存儲 WebSocket 協議
     */
    private static final String WEBSOCKET_SEC_PROTOCOL = "Sec-WebSocket-Protocol";

    /**
     * 遊客用戶對象
     */
    private static final User GUEST_USER = new User();

    /**
     * JWT 驗證管理器
     */
    private final JwtAuthenticationManager authenticationManager;

    /**
     * 安全配置屬性
     */
    private final SecurityProperties securityProperties;

    /**
     * JWT 憑證的正則表達式
     */
    private final Pattern jwtCookieNamePattern;


    /**
     * JWT 憑證處理庫的構造函數
     * 會檢查安全配置屬性中的 JWT Cookie 名稱和 WebSocket Token 前綴是否為空
     *
     * @param authenticationManager JWT 驗證管理器
     * @param securityProperties    安全配置屬性
     */
    public JwtSecurityContextRepository(JwtAuthenticationManager authenticationManager, SecurityProperties securityProperties) {
        this.authenticationManager = authenticationManager;
        this.securityProperties = securityProperties;

        Assert.isTrue(StringUtils.hasText(securityProperties.getCookie().getTokenName()), "JWT Cookie 名稱不能為空");
        this.jwtCookieNamePattern = Pattern.compile(securityProperties.getCookie().getTokenName() + "=([^;]+)");

        Assert.isTrue(StringUtils.hasText(securityProperties.getJwtToken().getWebSocketTokenPrefix()), "WebSocket Token 前綴不能為空");
    }


    /**
     * 初始化方法，設置遊客用戶對象的屬性
     * 遊客用戶對象的 ID 為 0，角色為 VISITOR，存儲限制和已使用存儲空間均為 0
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
     * 此方法不支援，不需要保存 SecurityContext
     *
     * @param exchange 請求
     * @param context  安全上下文
     *
     * @return Mono<Void>
     */
    @Override
    public Mono<Void> save(ServerWebExchange exchange, SecurityContext context) {
        return null;
    }


    /**
     * 從請求中獲取 JWT Token，若存在則進行驗證
     * 分成 API 、WEB 和 WEBSOCKET 三種請求類型
     * 其中 API請求只會從請求頭中獲取 JWT Token
     * WEB 以及 WEBSOCKET 請求則會從請求頭和 Cookie 中獲取 JWT Token
     * 驗證成功則返回 SecurityContext，驗證失敗則返回 Mono.error()
     * 如無法獲取 JWT Token，則會檢查是否啟用遊客用戶
     * 若啟用則返回遊客用戶的 SecurityContext，否則返回 Mono.empty()，設置為 AnonymousAuthenticationToken
     *
     * @param exchange 請求
     *
     * @return Mono<SecurityContext> 安全上下文
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
     * 從 Cookie 中提取 JWT Token
     *
     * @param cookieValue Cookie
     *
     * @return JWT Token
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
     * 設置為遊客身分的用戶對象
     *
     * @return Mono<User> 返回遊客身分的用戶對象
     */
    private Mono<SecurityContext> createGuestSecurityContext() {
        List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(RoleEnum.VISITOR.name()));
        Authentication guestAuth = new UsernamePasswordAuthenticationToken(GUEST_USER.getId(), null, authorities);
        return Mono.just(new SecurityContextImpl(guestAuth));
    }


    /**
     * 創建 WebSocket 錯誤的身份驗證對象
     *
     * @param errorCode 錯誤代碼
     *
     * @return Authentication 返回身份驗證對象
     */
    private Authentication createWebSocketErrorAuthentication(ValidationException.ErrorCode errorCode) {
        Object principal = "ERROR_WEBSOCKET_AUTH_" + errorCode.name();

        UsernamePasswordAuthenticationToken errorToken = new UsernamePasswordAuthenticationToken(principal, null);
        errorToken.setDetails(errorCode);
        return errorToken;
    }


    /**
     * 請求類型枚舉類，用於定義請求的類型
     * 主要用於區分各種類型的請求，使用不同的方式來獲取 JWT Token
     */
    @Getter
    @RequiredArgsConstructor
    enum RequestType {
        /**
         * API 請求，此為默認請求類型，從請求頭中獲取 JWT Token
         */
        API("API協議"),

        /**
         * WEB 請求，從請求頭和 Cookie 中獲取 JWT Token
         */
        WEB("WEB協議"),

        /**
         * WebSocket 請求，此處不做驗證，交由 ${@link xyz.dowob.filemanagement.component.manager.JwtWebSocketHandlerAdapter} 處理
         */
        WEB_SOCKET("WebSocket協議"),

        /**
         * 其他請求類型，此類為功能性請求，包含 /docs、/actuator 等請求
         */
        OTHER("其他協議");

        /**
         * 請求類型名稱
         */
        private final String name;

        /**
         * 獲取請求類型
         *
         * @param exchange 請求
         *
         * @return 請求類型
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
