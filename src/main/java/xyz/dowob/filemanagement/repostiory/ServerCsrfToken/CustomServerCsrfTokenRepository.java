package xyz.dowob.filemanagement.repostiory.ServerCsrfToken;

import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.security.web.server.csrf.ServerCsrfTokenRepository;
import reactor.core.publisher.Mono;

/**
 * 自定義 CsrfTokenRepository，繼承自 ServerCsrfTokenRepository
 * 除了 ServerCsrfTokenRepository 的方法外，還提供了清理憑證的方法
 *
 * @author yuan
 * @program FileManagement
 * @ClassName CustomServerCsrfTokenRepository
 * @create 2025/3/6
 * @Version 1.0
 **/

public interface CustomServerCsrfTokenRepository extends ServerCsrfTokenRepository {
    /**
     * 清理憑證
     */
    Mono<Void> deleteToken(CsrfToken token);
}
