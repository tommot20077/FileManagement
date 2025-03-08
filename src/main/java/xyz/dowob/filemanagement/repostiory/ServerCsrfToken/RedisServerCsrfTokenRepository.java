package xyz.dowob.filemanagement.repostiory.ServerCsrfToken;

import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.security.web.server.csrf.DefaultCsrfToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.CsrfRepositoryType;
import xyz.dowob.filemanagement.component.provider.provider.RedisProvider;
import xyz.dowob.filemanagement.config.properties.SecurityProperties;
import xyz.dowob.filemanagement.customenum.CsrfTokenRepositoryEnum;
import xyz.dowob.filemanagement.exception.ValidationException;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * Redis CSRF Token 存儲庫，繼承 AbstractServerCsrfTokenRepository
 * 這類將CSRF 憑證管理交由 Redis 進行，在一般情況下，Redis 會比本地存儲更加安全
 * 並可以進行持久化存儲，適合用於分布式系統
 *
 * @author yuan
 * @program FileManagement
 * @ClassName RedisServerCsrfTokenRepository
 * @create 2025/3/2
 * @Version 1.0
 **/
@Component
@CsrfRepositoryType(CsrfTokenRepositoryEnum.REDIS)
public class RedisServerCsrfTokenRepository extends AbstractServerCsrfTokenRepository {
    /**
     * Redis 提供者
     */
    private final RedisProvider redisProvider;

    /**
     * 初始化屬性
     */
    public RedisServerCsrfTokenRepository(SecurityProperties securityProperties, RedisProvider redisProvider) {
        super(securityProperties);
        this.redisProvider = redisProvider;
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
                .getHashMap(CSRF_TOKEN_HEADER, userToken, Integer.class)
                .switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.INVALID_CSRF_TOKEN)))
                .flatMap(time -> {
                    if (time < Instant.now().getEpochSecond()) {
                        return Mono.error(new ValidationException(ValidationException.ErrorCode.INVALID_CSRF_TOKEN));
                    }
                    return Mono.just(new DefaultCsrfToken(CSRF_TOKEN_HEADER, CSRF_TOKEN_PARAMETER, userToken));
                });
    }

    /**
     * 清理憑證
     */
    @Override
    public Mono<Void> deleteToken(CsrfToken token) {
        Long expireTime = Instant.now().getEpochSecond();
        if (token != null) {
            return redisProvider.deleteHash(CSRF_TOKEN_HEADER, token.getToken());
        }

        return redisProvider
                .getAllHashMap(CSRF_TOKEN_HEADER, String.class, Long.class)
                .filter(entry -> entry.getValue() < expireTime)
                .map(Map.Entry::getKey)
                .collectList()
                .flatMap(expireTokens -> redisProvider.deleteHash(CSRF_TOKEN_HEADER, expireTokens));
    }
}
