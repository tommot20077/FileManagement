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
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.component.manager.JwtAuthenticationManager;
import xyz.dowob.filemanagement.config.properties.SecurityProperties;
import xyz.dowob.filemanagement.customenum.RoleEnum;
import xyz.dowob.filemanagement.entity.User;

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
@RequiredArgsConstructor
public class JwtSecurityContextRepository implements ServerSecurityContextRepository {
    /**
     * JWT 憑證的前綴
     */
    private static final String TOKEN_PREFIX = "Bearer ";

    /**
     * JWT 憑證的正則表達式
     */
    private static final Pattern JWT_PATTERN = Pattern.compile("jwtToken=([^;]+)");

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
     * 分成 API 和 WEB 兩種請求類型
     * 其中 API請求只會從請求頭中獲取 JWT Token，WEB請求則會從請求頭和 Cookie 中獲取 JWT Token
     * 避免 CSRF 攻擊
     * 驗證成功則返回 SecurityContext
     * 驗證失敗則返回 Mono.empty()
     *
     * @param exchange 請求
     *
     * @return Mono<SecurityContext>
     */
    @Override
    public Mono<SecurityContext> load(ServerWebExchange exchange) {
        AtomicReference<String> token = new AtomicReference<>();
        RequestType requestType = RequestType.getRequestType(exchange);

        String cookie = exchange.getRequest().getHeaders().getFirst(HttpHeaders.COOKIE);
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (requestType == RequestType.WEB) {
            extractJwtFromHeader(cookie).ifPresent(token::set);
        }

        if (token.get() == null && authHeader != null && authHeader.startsWith(TOKEN_PREFIX)) {
            token.set(authHeader.substring(TOKEN_PREFIX.length()));
        }

        if (token.get() != null) {
            Authentication auth = new UsernamePasswordAuthenticationToken(token.get(), token.get());
            return authenticationManager
                    .authenticate(auth)
                    .map(authentication -> (SecurityContext) new SecurityContextImpl(authentication))
                    .switchIfEmpty(Mono.defer(() -> {
                        if (securityProperties.getGuestUser().isEnable()) {
                            return createGuestSecurityContext();
                        }
                        return Mono.empty();
                    }));
        } else if (securityProperties.getGuestUser().isEnable()) {
            return createGuestSecurityContext();
        }
        return Mono.empty();
    }


    /**
     * 從 Cookie 中提取 JWT Token
     *
     * @param cookieHeader Cookie
     *
     * @return JWT Token
     */
    private Optional<String> extractJwtFromHeader(String cookieHeader) {
        if (cookieHeader == null) {
            return Optional.empty();
        }

        Matcher matcher = JWT_PATTERN.matcher(cookieHeader);
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
        WEB("WEB協議");

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
            if (path.startsWith("/web")) {
                return WEB;
            }
            return API;
        }

    }
}
