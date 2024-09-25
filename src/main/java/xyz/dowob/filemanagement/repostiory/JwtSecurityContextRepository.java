package xyz.dowob.filemanagement.repostiory;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.component.manager.JwtAuthenticationManager;

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
     * JWT 驗證管理器
     */
    private final JwtAuthenticationManager authenticationManager;

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
     * 驗證成功則返回 SecurityContext
     *
     * @param exchange 請求
     *
     * @return Mono<SecurityContext>
     */
    @Override
    public Mono<SecurityContext> load(ServerWebExchange exchange) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith(TOKEN_PREFIX)) {
            String authToken = authHeader.substring(TOKEN_PREFIX.length());
            Authentication auth = new UsernamePasswordAuthenticationToken(authToken, authToken);
            return authenticationManager.authenticate(auth).map(SecurityContextImpl::new);
        } else {
            return Mono.empty();
        }
    }
}
