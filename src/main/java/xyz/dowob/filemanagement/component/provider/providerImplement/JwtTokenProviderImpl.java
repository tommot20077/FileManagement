package xyz.dowob.filemanagement.component.provider.providerImplement;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import xyz.dowob.filemanagement.annotation.HideSensitive;
import xyz.dowob.filemanagement.annotation.RecordLevel;
import xyz.dowob.filemanagement.annotation.SkipRecord;
import xyz.dowob.filemanagement.component.provider.providerInterface.TokenProvider;
import xyz.dowob.filemanagement.config.properties.SecurityProperties;
import xyz.dowob.filemanagement.customenum.LogLevelEnum;
import xyz.dowob.filemanagement.customenum.RoleEnum;
import xyz.dowob.filemanagement.entity.Token;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.repostiory.TokenRepository;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 此類用於 JWT相關的基礎操作。
 * 繼承 TokenProvider 接口實現包括生成、驗證、刪除 JWT 憑證等功能，
 *
 * @author yuan
 * @program FileManagement
 * @ClassName JwtTokenProviderImpl
 * @description
 * @create 2024-09-23 14:16
 * @Version 1.0
 **/
@Component
@RecordLevel(LogLevelEnum.DEBUG)
@RequiredArgsConstructor
public class JwtTokenProviderImpl implements TokenProvider {
    /**
     * TokenRepository 用於操作 Token 實體的數據庫操作類
     */
    private final TokenRepository tokenRepository;

    /**
     * SecurityProperties 用於操作安全配置的類
     */
    private final SecurityProperties securityProperties;

    /**
     * 用於存儲 JWT 憑證的緩存，key 為 JWT 憑證，value 為 TokenCacheEntity 記錄類 {@link TokenCacheEntity}
     */
    private final ConcurrentHashMap<String, TokenCacheEntity> cacheTokenMap = new ConcurrentHashMap<>();

    /**
     * key 用於生成 JWT 憑證的密鑰
     */
    private SecretKey key;

    /**
     * 初始化方法，用於將 secret 解碼後生成 key
     */
    @PostConstruct
    public void init() {
        byte[] encodedSecret = Base64.getDecoder().decode(securityProperties.getJwtToken().getSecret());
        this.key = Keys.hmacShaKeyFor(encodedSecret);
    }


    /**
     * 根據用戶 ID 生成 JWT 憑證，會根據 Token 實體中的 JWT 憑證版本進行版本管理
     * 其中 subject 為用戶 ID，claim 中包含 JWT 憑證版本
     * 將 JWT 憑證存入 Token 實體中 並一併清除 cacheTokenMap 中的該用戶的 JWT 憑證
     *
     * @param user 用戶實體
     *
     * @return JWT 令牌字符串
     */
    @Override
    @HideSensitive
    public Mono<String> generateToken(User user) {
        if (user == null) {
            return Mono.error(new ValidationException(ValidationException.ErrorCode.USER_NOT_FOUND));
        }

        Mono<Token> tokenMono = tokenRepository.findByUserId(user.getId()).switchIfEmpty(Mono.defer(() -> {
            Token newToken = new Token();
            newToken.setUserId(user.getId());
            return Mono.just(newToken);
        }));

        RoleEnum role = user.getRole();
        Date now = new Date();

        return tokenMono.flatMap(tokenEntity -> {
            String tokenVersion = Token.generateJwtTokenVersion();

            long expirationMs = securityProperties.getJwtToken().getExpiration().toMillis();
            Date expirationDate = new Date(now.getTime() + expirationMs);
            String jwtToken = Jwts
                    .builder()
                    .subject(String.valueOf(user.getId()))
                    .issuedAt(now)
                    .claim("role", role)
                    .claim("username", user.getUsername())
                    .claim("version", tokenVersion)
                    .expiration(expirationDate)
                    .signWith(key)
                    .compact();

            tokenEntity.setJwtTokenVersion(tokenVersion);
            tokenEntity.setJwtTokenExpireTime(LocalDateTime.ofInstant(expirationDate.toInstant(), ZoneId.systemDefault()));

            cacheTokenMap.entrySet().removeIf(entry -> entry.getValue().userId().equals(user.getId()));

            return tokenRepository.save(tokenEntity).then(Mono.just(jwtToken));
        });
    }


    /**
     * 驗證 JWT 憑證，獲取用戶 ID
     * 此方法會根據 JWT 憑證中的用戶 ID 進行驗證，並根據 Token 快取或資料庫實體中的 JWT 憑證版本進行版本管理
     * 主要流程為：
     * 1. 獲取 JWT 憑證中的 Claims
     * 2. 驗證快取中的 JWT 憑證是否有效，若有效則返回用戶 ID
     * 3. 若快取中的 JWT 憑證無效，則根據用戶 ID 查找 Token 實體，並進行版本驗證
     * 驗證成功後返回用戶 ID，否則傳出 JWT_TOKEN_INVALID 錯誤
     *
     * @param token         JWT 憑證
     * @param userIdUseLess 用戶 ID (此參數在此方法中無用)
     *
     * @return 用戶 ID
     */
    @Override
    public Mono<Long> validateToken(String token, Long userIdUseLess) {
        return Mono.defer(() -> {
            TokenCacheEntity cacheEntity = cacheTokenMap.get(token);

            return getClaimsFromToken(token)
                    .flatMap(claims -> validateTokenWithClaims(token, claims, cacheEntity))
                    .retryWhen(Retry.backoff(1, Duration.ofSeconds(3)).filter(e -> !(e instanceof ValidationException)));
        });
    }


    /**
     * 根據用戶 ID 刪除 JWT 憑證
     * 此方法會根據用戶 ID 查找 Token 實體，並將 JWT 憑證版本將被重新設定，過期時間設為空
     * 此時 JWT 在驗證時版本不匹配，並且過期時間在當前時間之前，即 JWT 憑證無效
     * 並且會將 cacheTokenMap 中的對應關係刪除
     *
     * @param userId 用戶 ID
     */
    @Override
    public Mono<Void> revokeToken(Long userId) {
        return tokenRepository.findByUserId(userId).flatMap(tokenMono -> {
            cacheTokenMap.entrySet().removeIf(entry -> entry.getValue().userId().equals(userId));

            tokenMono.setJwtTokenVersion(Token.generateJwtTokenVersion());
            tokenMono.setJwtTokenExpireTime(null);

            return tokenRepository.save(tokenMono);
        }).then();
    }


    /**
     * 根據 JWT 憑證獲取 JWT 憑證中的 Claims
     * 當 JWT 憑證無效時，傳出 JWT_TOKEN_INVALID 錯誤
     *
     * @param token JWT 憑證
     *
     * @return JWT 憑證中的 Claims
     */
    public Mono<Claims> getClaimsFromToken(String token) {
        return Mono.defer(() -> {
            try {
                return Mono.just(Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload());
            } catch (Exception e) {
                return Mono.error(new ValidationException(ValidationException.ErrorCode.JWT_TOKEN_INVALID));
            }
        });
    }


    /**
     * 驗證 JWT 憑證中的 Claims，並根據快取中的 JWT 憑證版本進行版本管理
     * 當 JWT 憑證版本不匹配或 JWT 憑證過期時，傳出 JWT_TOKEN_INVALID 錯誤
     * 驗證成功後返回用戶 ID
     *
     * @param token       JWT 憑證
     * @param claims      JWT 憑證中的 Claims
     * @param cacheEntity 快取中的 JWT 憑證記錄
     *
     * @return 用戶 ID
     */
    private Mono<Long> validateTokenWithClaims(String token, Claims claims, TokenCacheEntity cacheEntity) {
        Long userId = Long.parseLong(claims.getSubject());
        String version = claims.get("version", String.class);

        try {
            if (isValidCache(cacheEntity, version, token)) {
                return Mono.just(userId);
            }
        } catch (ValidationException e) {
            return Mono.error(e);
        }

        return tokenRepository.findByUserId(userId).flatMap(tokenEntity -> {
            updateCache(token, tokenEntity, userId);
            if (!tokenEntity.getJwtTokenVersion().equals(version)) {
                return Mono.error(new ValidationException(ValidationException.ErrorCode.JWT_TOKEN_INVALID));
            }
            return Mono.just(userId);
        }).switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.JWT_TOKEN_INVALID)));
    }


    /**
     * 驗證快取中的 JWT 憑證是否有效
     * 分成幾種情況：
     * 1. 快取中無 JWT 憑證，返回 false
     * 2. JWT 憑證過期，刪除快取中的 JWT 憑證，返回 false
     * 3. JWT 憑證有效但版本不匹配，返回 false
     * 4. JWT 憑證有效且版本匹配，返回 true
     * 5. JWT 憑證的過期時間為空，傳出 JWT_TOKEN_INVALID 錯誤(此情況為登出後的 JWT 憑證)
     *
     * @param cacheEntity 快取中的 JWT 憑證記錄
     * @param version     JWT 憑證版本
     * @param token       JWT 憑證
     *
     * @return 是否有效
     *
     * @throws ValidationException 驗證失敗時傳出 JWT_TOKEN_INVALID 錯誤
     */
    @SkipRecord
    private boolean isValidCache(TokenCacheEntity cacheEntity, String version, String token) throws ValidationException {
        if (cacheEntity == null) {
            return false;
        }

        if (cacheEntity.expireTime() == null) {
            throw new ValidationException(ValidationException.ErrorCode.JWT_TOKEN_INVALID);
        }

        if (cacheEntity.expireTime().before(new Date())) {
            cacheTokenMap.remove(token);
            return false;
        }
        return cacheEntity.version().equals(version);
    }


    /**
     * 更新快取中的 JWT 憑證記錄
     *
     * @param token       JWT 憑證
     * @param tokenEntity Token 實體
     * @param userId      用戶 ID
     */
    @SkipRecord
    private void updateCache(String token, Token tokenEntity, Long userId) {
        Date expireTime = tokenEntity.getJwtTokenExpireTime() != null ? Date.from(tokenEntity
                                                                                          .getJwtTokenExpireTime()
                                                                                          .atZone(ZoneId.systemDefault())
                                                                                          .toInstant()) : null;
        cacheTokenMap.put(token, new TokenCacheEntity(tokenEntity.getJwtTokenVersion(), userId, expireTime));
    }


    /**
     * 獲取快取中的 JWT 憑證記錄
     *
     * @return 快取中的 JWT 憑證記錄
     */
    @HideSensitive
    @SkipRecord
    public ConcurrentHashMap<String, TokenCacheEntity> getCacheTokenMap() {
        return cacheTokenMap;
    }


    /**
     * TokenCacheEntity 用於記錄 JWT 憑證的版本、用戶 ID 和過期時間
     */
    public record TokenCacheEntity(String version, Long userId, Date expireTime) {
    }
}
