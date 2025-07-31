package xyz.dowob.filemanagement.exception;

import lombok.Getter;
import org.springframework.security.core.AuthenticationException;

/**
 * JSON Web Token認證異常類別，專門處理JWT憑證驗證過程中的各種失敗情況。
 * <p>
 * 此異常類別繼承自 Spring Security 的{@link AuthenticationException}，專門處理
 * JWT憑證的安全驗證失敗情況。當JWT憑證無效、過期、格式錯誤、簽章驗證失敗或
 * 其他安全違反時，系統會拋出此異常並觸發相應的安全保護機制。
 * </p>
 * <p>
 * 在Spring WebFlux反應式架構中，此異常透過{@code Mono.error()}進行非阻塞錯誤傳播，
 * 與反應式安全流水線完美整合。異常處理時會自動記錄安全事件，
 * 包括失敗的JWT識別、用戶資訊、失敗原因等，供安全審計和威脅情報分析使用。
 * </p>
 * <p>
 * 常見的JWT驗證失敗場景包括：
 * <ul>
 *   <li>JWT憑證已過期（exp claim驗證失敗）</li>
 *   <li>JWT簽章驗證失敗（秘鑰不匹配或簽章算法錯誤）</li>
 *   <li>JWT格式不正確（Base64解碼失敗或JSON結構錯誤）</li>
 *   <li>JWT版本不匹配（憑證已被撤銷或更新）</li>
 *   <li>JWT缺少必要的聲明（sub、iat、exp等關鍵claims）</li>
 *   <li>JWT發行者驗證失敗（iss claim不匹配）</li>
 *   <li>JWT受眾驗證失敗（aud claim不匹配）</li>
 * </ul>
 * </p>
 * <p>
 * 使用範例：
 * <pre>{@code
 * // 在反應式環境中的JWT驗證
 * return jwtTokenProvider.validateToken(token)
 *     .switchIfEmpty(Mono.error(new JwtAuthenticationException("JWT憑證已過期")))
 *     .onErrorMap(SignatureException.class, 
 *         ex -> new JwtAuthenticationException("JWT簽章驗證失敗"));
 * }</pre>
 * </p>
 * <p>
 * 此異常會被Spring Security的反應式認證機制自動捕獲，並轉換為HTTP 401 Unauthorized回應，
 * 要求用戶重新進行身份驗證。系統會根據安全策略自動應用適當的安全措施，
 * 如率限、IP封鎖或適應性認證等。
 * </p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see AuthenticationException
 * @see reactor.core.publisher.Mono#error(Throwable)
 * @see org.springframework.security.web.server.authentication.ServerAuthenticationFailureHandler
 * @see org.springframework.security.oauth2.jwt.JwtException
 */
@Getter
public class JwtAuthenticationException extends AuthenticationException {
    /**
     * 建構JWT認證異常實例，包含詳細的安全失敗資訊。
     * <p>
     * 建立一個包含詳細錯誤訊息的JWT認證異常，此訊息將被記錄到安全日誌中，
     * 並會觸發相應的安全監控和威脅情報收集機制。錯誤訊息會根據安全策略
     * 進行適當的敏感資訊過濾，確保不洩露系統內部安全詳情。
     * </p>
     * <p>
     * 在Spring WebFlux反應式環境中，此異常會被認證過濾器捕獲並轉換為
     * 相應的HTTP 401回應，同時觸發安全事件記錄和後續安全措施。
     * </p>
     *
     * @param msg 描述JWT認證失敗原因的詳細訊息，將被記錄到安全日誌中供後續分析
     * @see AuthenticationException#AuthenticationException(String)
     * @see reactor.core.publisher.Mono#error(Throwable)
     * @see org.springframework.security.web.server.ServerHttpSecurity.OAuth2ResourceServerSpec
     */
    public JwtAuthenticationException(String msg) {
        super(msg);
    }
}
