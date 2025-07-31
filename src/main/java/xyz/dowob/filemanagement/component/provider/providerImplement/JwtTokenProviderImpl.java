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
 * 基於 JWT 的令牌管理反應式實現。
 *
 * <p>此實現提供完整的 JWT 令牌生命週期管理，包括生成、驗證、撤銷等操作。
 * 使用 HMAC-SHA 簽章技術確保令牌安全性，支援令牌版本控制以實現強制登出功能。
 * 令牌包含使用者基本資訊和角色權限，並透過內建快取機制提升驗證效能。</p>
 *
 * <p>實現採用非阻塞反應式設計，適用於高併發環境下的令牌操作。
 * 快取機制使用 ConcurrentHashMap 確保線程安全，並提供自動過期清理功能。</p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@Component
@RecordLevel(LogLevelEnum.DEBUG)
@RequiredArgsConstructor
public class JwtTokenProviderImpl implements TokenProvider {
    /**
     * 令牌資料庫操作介面，負責令牌實體的持久化和查詢操作。
     */
    private final TokenRepository tokenRepository;

    /**
     * 系統安全配置屬性，提供 JWT 令牌相關的安全參數設定。
     */
    private final SecurityProperties securityProperties;

    /**
     * 令牌快取映射表，用於暫存令牌驗證資訊以提升效能。
     *
     * <p>使用 ConcurrentHashMap 確保多線程環境下的快取安全性。</p>
     */
    private final ConcurrentHashMap<String, TokenCacheEntity> cacheTokenMap = new ConcurrentHashMap<>();

    /**
     * JWT 令牌簽章密鑰，用於令牌的生成和驗證操作。
     */
    private SecretKey key;


    /**
     * 初始化 JWT 簽章密鑰。
     *
     * <p>從安全配置中讀取 Base64 編碼的密鑰字串，解碼後生成 HMAC-SHA 簽章密鑰。
     * 此方法在 Spring 容器初始化後自動執行。</p>
     */
    @PostConstruct
    public void init() {
        byte[] encodedSecret = Base64.getDecoder().decode(securityProperties.getJwtToken().getSecret());
        this.key = Keys.hmacShaKeyFor(encodedSecret);
    }


    /**
     * 為指定用戶生成 JWT 令牌。
     *
     * <p>生成過程包括驗證用戶有效性、查詢或建立令牌實體、設定令牌聲明（包含用戶 ID、角色、
     * 用戶名稱和版本號）、儲存令牌實體並清除相關快取。令牌包含過期時間並使用 HMAC-SHA 簽章。</p>
     *
     * @param user 用戶實體，不可為 null
     * @return 包含生成的 JWT 令牌字串的 Mono
     */
    @Override
    @HideSensitive
    public Mono<String> generateToken(User user) {
        if (user == null) {
            return Mono.error(new ValidationException(ValidationException.ErrorCode.USER_NOT_FOUND, "空的用戶實體"));
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
     * 驗證 JWT 令牌並回傳用戶 ID。
     *
     * <p>驗證過程包括檢查快取、解析令牌聲明、驗證令牌版本和有效期。
     * 若快取失效則查詢資料庫進行驗證，並更新快取。包含重試機制處理暫時性錯誤。</p>
     *
     * @param token 待驗證的 JWT 令牌字串
     * @param userIdUseLess 相容性參數，此實現中不使用
     * @return 包含用戶 ID 的 Mono，驗證失敗時回傳錯誤
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
     * 撤銷指定用戶的所有有效令牌。
     *
     * <p>撤銷過程包括查詢用戶令牌實體、清除相關快取、更新令牌版本號並清空過期時間。
     * 版本號更新後，所有使用舊版本的令牌將自動失效。</p>
     *
     * @param userId 需要撤銷令牌的用戶 ID
     * @return 完成撤銷操作的 Mono
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
     * 解析 JWT 令牌並提取聲明內容。
     *
     * <p>使用配置的 HMAC-SHA 密鑰驗證令牌簽章並解析其中的聲明內容。
     * 解析失敗時回傳驗證異常。</p>
     *
     * @param token 待解析的 JWT 令牌字串
     * @return 包含令牌聲明的 Mono，解析失敗時回傳錯誤
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
     * 驗證令牌聲明並進行版本控制檢查。
     *
     * <p>從聲明中提取用戶 ID 和版本號，優先檢查快取有效性。
     * 若快取無效則查詢資料庫驗證令牌版本，並更新快取。</p>
     *
     * @param token 令牌字串
     * @param claims 令牌聲明內容
     * @param cacheEntity 快取實體，可為 null
     * @return 包含用戶 ID 的 Mono，驗證失敗時回傳錯誤
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
     * 檢查快取中的令牌是否有效。
     *
     * <p>驗證快取實體存在性、過期時間和版本號。若過期時間為 null 表示令牌已被撤銷，
     * 若已過期則清除快取，版本不符則回傳無效。</p>
     *
     * @param cacheEntity 快取實體，可為 null
     * @param version 令牌版本號
     * @param token 令牌字串，用於清除過期快取
     * @return true 表示快取有效，false 表示需查詢資料庫
     * @throws ValidationException 當令牌已被撤銷時拋出
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
     * 更新令牌快取記錄。
     *
     * <p>將令牌實體的版本號、用戶 ID 和過期時間存入快取映射表。</p>
     *
     * @param token 令牌字串作為快取鍵
     * @param tokenEntity 令牌資料庫實體
     * @param userId 用戶 ID
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
     * 取得令牌快取映射表。
     *
     * <p>回傳內部快取映射表的參考，主要用於測試和監控目的。</p>
     *
     * @return 令牌快取映射表
     */
    @HideSensitive
    @SkipRecord
    public ConcurrentHashMap<String, TokenCacheEntity> getCacheTokenMap() {
        return cacheTokenMap;
    }


    /**
     * 令牌快取實體記錄，包含令牌驗證所需的關鍵資訊。
     *
     * <p>此記錄包含令牌版本號、擁有者用戶 ID 和過期時間，
     * 用於快速驗證令牌有效性而無需查詢資料庫。</p>
     *
     * @param version 令牌版本號
     * @param userId 令牌擁有者用戶 ID  
     * @param expireTime 令牌過期時間，null 表示已撤銷
     */
    public record TokenCacheEntity(String version, Long userId, Date expireTime) {
    }
}
