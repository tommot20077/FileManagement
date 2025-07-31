package xyz.dowob.filemanagement.component.manager;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.RecordLevel;
import xyz.dowob.filemanagement.component.provider.providerImplement.JwtTokenProviderImpl;
import xyz.dowob.filemanagement.component.strategy.TokenStrategy;
import xyz.dowob.filemanagement.customenum.LogLevelEnum;
import xyz.dowob.filemanagement.customenum.TokenEnum;
import xyz.dowob.filemanagement.exception.ProcessException;
import xyz.dowob.filemanagement.exception.ValidationException;

import java.util.Collections;
import java.util.List;

/**
 * WebFlux 安全認證管理器，實現基於 JWT 的響應式身份認證與授權機制。
 *
 * <p>本類是 Spring Security 響應式認證流程的核心元件，專門處理 JSON Web Token (JWT) 的驗證與授權邏輯。</p>
 *
 * <p>主要設計特點：</p>
 *
 * <p>1. 響應式認證架構：
 *    - 實現 {@link org.springframework.security.authentication.ReactiveAuthenticationManager} 介面
 *    - 採用非阻塞、異步的 JWT 憑證驗證機制
 *    - 支持高併發、低延遲的身份認證處理</p>
 *
 * <p>2. JWT 驗證流程：
 *    - 解析請求頭中的 JWT 憑證
 *    - 驗證憑證的有效性和完整性
 *    - 提取用戶身份標識和角色信息
 *    - 構建 {@link org.springframework.security.authentication.UsernamePasswordAuthenticationToken}</p>
 *
 * <p>3. 安全性保護：
 *    - 使用 {@link xyz.dowob.filemanagement.component.strategy.TokenStrategy} 實現靈活的 Token 提供者策略
 *    - 處理多種驗證異常情況，如憑證過期、無效等
 *    - 拋出具體的異常類型，便於上層系統處理</p>
 *
 * <p>4. 授權策略：
 *    - 根據 JWT 中的角色信息動態生成 {@link org.springframework.security.core.GrantedAuthority}
 *    - 支持基於角色的細粒度權限控制
 *    - 與 Spring Security 的授權機制無縫集成</p>
 *
 * <p>認證失敗時將拋出 {@link xyz.dowob.filemanagement.exception.ValidationException} 或 {@link xyz.dowob.filemanagement.exception.ProcessException}，
 * 確保系統安全性和異常處理的一致性。</p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationManager implements ReactiveAuthenticationManager {
    /**
     * token的策略工廠
     */
    private final TokenStrategy tokenStrategy;


    /**
     * 實現 ReactiveAuthenticationManager 的 authenticate 方法
     *
     * @param authentication 用戶請求頭中的 JWT 憑證
     *
     * @return 當 JWT 憑證驗證成功時，回傳一個 UsernamePasswordAuthenticationToken 對象
     * 用戶的 id 作為 principal，用戶的角色作為 authorities
     * 當 JWT 憑證驗證失敗時，回傳 Mono.empty()
     */
    @Override
    @RecordLevel(LogLevelEnum.DEBUG)
    public Mono<Authentication> authenticate(Authentication authentication) {
        if (authentication == null || authentication.getCredentials() == null) {
            return Mono.error(new ValidationException(ValidationException.ErrorCode.AUTHENTICATION_FAILED));
        }

        return Mono.defer(() -> {
            String token = authentication.getCredentials().toString();
            JwtTokenProviderImpl jwtTokenProvider = (JwtTokenProviderImpl) tokenStrategy.getTokenProvider(TokenEnum.JWT_AUTHORIZATION_TOKEN);

            Mono<Long> userId = jwtTokenProvider.validateToken(token, null);
            return userId.flatMap(id -> jwtTokenProvider.getClaimsFromToken(token).map(claims -> claims.get("role")).map(roles -> {
                List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority((String) roles));
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(id, token, authorities);
                return (Authentication) auth;
            }));
        }).onErrorResume(e -> {
            if (e instanceof ValidationException) {
                return Mono.error(e);
            }
            return Mono.error(new ProcessException(ProcessException.ErrorCode.AUTHENTICATION_ERROR, e));
        });
    }
}
