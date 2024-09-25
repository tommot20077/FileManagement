package xyz.dowob.filemanagement.component.manager;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.component.factory.TokenStrategyFactory;
import xyz.dowob.filemanagement.component.provider.providerImplement.JwtTokenProviderImpl;
import xyz.dowob.filemanagement.customenum.TokenEnum;

import java.util.Collections;
import java.util.List;

/**
 * 此類用於管理 JWT 憑證的驗證
 * 主要應用在Web Security中，用於驗證用戶請求頭中的 JWT 憑證並返回驗證結果
 * 繼承 ReactiveAuthenticationManager 並實現 authenticate 方法
 *
 * @author yuan
 * @program FileManagement
 * @ClassName JwtAuthenticationManager
 * @description
 * @create 2024-09-25 00:30
 * @Version 1.0
 **/
@Component
@RequiredArgsConstructor
public class JwtAuthenticationManager implements ReactiveAuthenticationManager {
    /**
     * token的策略工廠
     */
    private final TokenStrategyFactory tokenStrategyFactory;

    /**
     * @param authentication 用戶請求頭中的 JWT 憑證
     *
     * @return 當 JWT 憑證驗證成功時，返回一個 UsernamePasswordAuthenticationToken 對象
     * 用戶的 id 作為 principal，用戶的角色作為 authorities
     * 當 JWT 憑證驗證失敗時，返回 Mono.empty()
     */
    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        String token = authentication.getCredentials().toString();
        JwtTokenProviderImpl jwtTokenProvider =
                (JwtTokenProviderImpl) tokenStrategyFactory.getTokenProvider(TokenEnum.JWT_AUTHORIZATION_TOKEN);
        try {
            Mono<Long> userId = jwtTokenProvider.validateToken(token, null);
            return userId.flatMap(id -> jwtTokenProvider.getClaimsFromToken(token).map(claims -> claims.get("role")).map(roles -> {
                List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority((String) roles));
                return new UsernamePasswordAuthenticationToken(id, null, authorities);
            }));
        } catch (Exception e) {
            return Mono.empty();
        }
    }
}
