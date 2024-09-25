package xyz.dowob.filemanagement.service.ServiceInterFace;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.dto.user.AuthRequestDTO;
import xyz.dowob.filemanagement.entity.User;

import java.util.Map;

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
     * 接口默認方法，根據用戶請求對象和用戶對象進行Session授權
     * 將用戶名稱、用戶ID和授權對象存入Session中
     *
     * @param request 請求對象
     * @param user    用戶對象
     */
    default Mono<Void> setSessionAuthorization(ServerWebExchange request, User user) {
        return request.getSession().doOnNext(webSession -> {
            Authentication authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);

            Map<String, Object> attributes = webSession.getAttributes();
            attributes.put(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, SecurityContextHolder.getContext());
            attributes.put("username", user.getUsername());
            attributes.put("userId", user.getId());
        }).then();
    }
}
