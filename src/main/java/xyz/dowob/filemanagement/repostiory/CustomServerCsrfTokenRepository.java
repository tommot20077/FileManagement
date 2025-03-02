package xyz.dowob.filemanagement.repostiory;

import jakarta.annotation.PostConstruct;
import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.security.web.server.csrf.DefaultCsrfToken;
import org.springframework.security.web.server.csrf.ServerCsrfTokenRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.component.manager.CronTaskManager;
import xyz.dowob.filemanagement.component.provider.provider.RedisProvider;
import xyz.dowob.filemanagement.config.properties.SecurityProperties;
import xyz.dowob.filemanagement.exception.ValidationException;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * 自定義的 CSRF Token 存儲庫，用於CSRF 的相關操作
 * 主要實現 ServerCsrfTokenRepository 接口
 * 用於生成、保存、加載 CSRF Token
 * 這裡省略了保存 Token 的操作，因為我們在生成 Token 的時候就已經保存，所以這裡只需要生成和加載即可
 * 這裡的 Token 是保存在 Redis 中的，並且設置了過期時間，而還有使用{@link CronTaskManager}定時清理過期的 Token
 * 這邊的屬性都可以在配置文件中配置 {@link SecurityProperties}
 *
 * @author yuan
 * @program FileManagement
 * @ClassName CustomServerCsrfTokenRepository
 * @create 2025/3/2
 * @Version 1.0
 **/
@Component

public class CustomServerCsrfTokenRepository implements ServerCsrfTokenRepository {
    /**
     * Redis 提供者
     */
    private final RedisProvider redisProvider;
    /**
     * 安全相關設定
     */
    private final SecurityProperties securityProperties;
    /**
     * CSRF Token 的 Header 名稱
     */
    private String CSRF_TOKEN_HEADER;
    /**
     * CSRF Token 的參數名稱
     */
    private String CSRF_TOKEN_PARAMETER;
    /**
     * CSRF Token 過期時間
     */
    private Long EXPIRE_TIME;

    public CustomServerCsrfTokenRepository(RedisProvider redisProvider, SecurityProperties securityProperties) {
        this.redisProvider = redisProvider;
        this.securityProperties = securityProperties;
    }

    @PostConstruct
    public void init() {
        CSRF_TOKEN_HEADER = securityProperties.getCsrf().getHeaderName();
        CSRF_TOKEN_PARAMETER = securityProperties.getCsrf().getParameterName();
        EXPIRE_TIME = securityProperties.getCsrf().getExpiration();
    }


    /**
     * 生成 CSRF Token，並保存到 Redis 中
     *
     * @param exchange 伺服器 Web 交換對象
     *
     * @return 生成的 CSRF Token
     */
    @Override
    public Mono<CsrfToken> generateToken(ServerWebExchange exchange) {
        String uuid = java.util.UUID.randomUUID().toString();
        CsrfToken csrfToken = new DefaultCsrfToken(CSRF_TOKEN_HEADER, CSRF_TOKEN_PARAMETER, uuid);
        long expireTime = Instant.now().plus(EXPIRE_TIME, ChronoUnit.MINUTES).getEpochSecond();
        return redisProvider.setHashMap(CSRF_TOKEN_HEADER, uuid, expireTime, Duration.ofMinutes(expireTime)).thenReturn(csrfToken);
    }

    /**
     * 保存 CSRF Token，這裡不做任何操作
     *
     * @param exchange 伺服器 Web 交換對象
     * @param token    CSRF Token
     *
     * @return 空 Mono
     */
    @Override
    public Mono<Void> saveToken(ServerWebExchange exchange, CsrfToken token) {
        return Mono.empty();
    }

    /**
     * 加載 CSRF Token，並檢查是否合法
     * 如果 Token 不合法，則返回驗證錯誤
     *
     * @param exchange 伺服器 Web 交換對象
     *
     * @return 加載的 CSRF Token
     */
    @Override
    public Mono<CsrfToken> loadToken(ServerWebExchange exchange) {
        String userToken = exchange.getRequest().getHeaders().getFirst(CSRF_TOKEN_HEADER);
        if (userToken == null) {
            return Mono.error(new ValidationException(ValidationException.ErrorCode.MISSING_CSRF_TOKEN));
        }
        return redisProvider
                .getHashMap(CSRF_TOKEN_HEADER, userToken)
                .switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.INVALID_CSRF_TOKEN)))
                .map(time -> new DefaultCsrfToken(CSRF_TOKEN_HEADER, CSRF_TOKEN_PARAMETER, userToken));
    }
}
