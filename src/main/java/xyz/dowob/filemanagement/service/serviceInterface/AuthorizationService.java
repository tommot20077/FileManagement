package xyz.dowob.filemanagement.service.serviceInterface;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;
import xyz.dowob.filemanagement.data.user.dto.AuthRequestDTO;
import xyz.dowob.filemanagement.entity.User;

/**
 * 有關授權的業務邏輯接口
 * 定義實現類需要實現的方法
 *
 * @author yuan
 * @program File-Management
 * @ClassName AuthorizationService
 * @description
 * @create 2024-09-16 03:00
 * @Version 1.0
 **/
public interface AuthorizationService {
    /**
     * 認證用戶並返回 JWT
     *
     * @param authRequest 包含用戶名和密碼的請求
     *
     * @return 包含 JWT 的響應
     */
    Mono<String> authenticate(AuthRequestDTO authRequest, ServerWebExchange request);


    /**
     * 接口默認方法，根據用戶請求對象和用戶對象進行授權
     * 將用戶名稱、用戶ID和授權對象存入ServerWebExchange中
     *
     * @param request 請求對象
     * @param user    用戶對象
     */
    default Mono<Void> setAuthorization(ServerWebExchange request, User user) {
        if (user == null) {
            return Mono.empty();
        }
        Authentication authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        Context securityContext = ReactiveSecurityContextHolder.withSecurityContext(Mono.just(new SecurityContextImpl(authentication)));
        request.getAttributes().put(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
        request.getAttributes().put("username", user.getUsername());
        request.getAttributes().put("userId", user.getId());
        request.getAttributes().put("role", user.getRole());
        return Mono.empty();
    }

    /**
     * 接口默認方法，根據請求對象獲取CSRF憑證
     *
     * @param request 請求對象
     *
     * @return CSRF憑證
     */
    default Mono<CsrfToken> getCSRFToken(ServerWebExchange request) {
        return request.getSession().mapNotNull(webSession -> webSession.getAttribute("csrfToken"));
    }
}
