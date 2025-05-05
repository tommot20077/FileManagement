package xyz.dowob.filemanagement.repostiory.ServerCsrfToken;

import jakarta.annotation.PreDestroy;
import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.security.web.server.csrf.DefaultCsrfToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.CsrfRepositoryType;
import xyz.dowob.filemanagement.config.properties.SecurityProperties;
import xyz.dowob.filemanagement.customenum.CsrfTokenRepositoryEnum;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.unity.CacheConcurrentHashMap;

/**
 * 本地 CsrfTokenRepository，使用 ConcurrentHashMap 保存 CsrfToken
 * 這個類別是 CsrfTokenRepository 的實現類型
 * 這個類別繼承了 AbstractServerCsrfTokenRepository 類別
 * 此類因為沒有使用到外部儲存，擁有相較其他 CsrfTokenRepository 類別更高的效率
 * 但也無法實現持久化保存 CsrfToken
 *
 * @author yuan
 * @program FileManagement
 * @ClassName LocalCsrfTokenRepository
 * @create 2025/3/6
 * @Version 1.0
 **/
@Component
@CsrfRepositoryType(CsrfTokenRepositoryEnum.LOCAL)
public class LocalServerCsrfTokenRepository extends AbstractServerCsrfTokenRepository {
    /**
     * 保存 CsrfToken 的 Map
     */
    private final CacheConcurrentHashMap<String, CsrfToken> csrfTokenMap;


    /**
     * 初始化屬性
     */
    public LocalServerCsrfTokenRepository(SecurityProperties securityProperties) {
        super(securityProperties);
        this.csrfTokenMap = new CacheConcurrentHashMap<>(64, super.expireTime, false);
    }


    /**
     * 生成 CsrfToken，並保存到 Map 中
     *
     * @param exchange 伺服器 Web 交換對象
     *
     * @return 生成的 CsrfToken
     */
    @Override
    public Mono<CsrfToken> generateToken(ServerWebExchange exchange) {
        String uuid = java.util.UUID.randomUUID().toString();
        CsrfToken csrfToken = new DefaultCsrfToken(csrfTokenHeader, csrfTokenParameter, uuid);
        csrfTokenMap.set(uuid, csrfToken);
        return Mono.just(csrfToken);
    }


    /**
     * 保存 CsrfToken，這裡不做任何操作
     *
     * @param exchange ServerWebExchange
     * @param token    CsrfToken
     *
     * @return Mono<Void>
     */
    @Override
    public Mono<Void> saveToken(ServerWebExchange exchange, CsrfToken token) {
        return Mono.empty();
    }


    /**
     * 加載 CsrfToken，並檢查是否合法
     * 如果 Token 不合法，則返回驗證錯誤
     *
     * @param exchange 伺服器 Web 交換對象
     *
     * @return 加載的 CSRF Token
     */
    @Override
    public Mono<CsrfToken> loadToken(ServerWebExchange exchange) {
        String userToken = exchange.getRequest().getHeaders().getFirst(csrfTokenHeader);
        if (userToken == null) {
            return Mono.error(new ValidationException(ValidationException.ErrorCode.MISSING_CSRF_TOKEN));
        }

        CsrfToken csrfToken = csrfTokenMap.get(userToken);
        if (csrfToken != null) {
            return Mono.just(csrfToken);
        }
        return Mono.error(new ValidationException(ValidationException.ErrorCode.INVALID_CSRF_TOKEN));
    }


    /**
     * 刪除憑證
     * 當傳入的 token 不為空時，則刪除指定的憑證
     * 否則，則刪除所有過期的憑證
     *
     * @param token CsrfToken 憑證，如果為空，則刪除過期的憑證
     */
    @Override
    public Mono<Void> deleteToken(CsrfToken token) {
        return Mono.fromRunnable(() -> {
            if (token != null) {
                csrfTokenMap.remove(token.getToken());
                return;
            }
            csrfTokenMap.getCleanupTask().run();
        });
    }


    /**
     * 銷毀 CSRF Token 存儲庫
     * 在應用程序關閉時調用
     */
    @PreDestroy
    public void destroy() {
        csrfTokenMap.destroy();
    }
}
