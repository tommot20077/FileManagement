package xyz.dowob.filemanagement.component.provider.providerImplement;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.HideSensitive;
import xyz.dowob.filemanagement.component.provider.providerInterface.TokenProvider;
import xyz.dowob.filemanagement.config.properties.SecurityProperties;
import xyz.dowob.filemanagement.customenum.RoleEnum;
import xyz.dowob.filemanagement.entity.Token;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.repostiory.TokenRepository;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Date;

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
@RequiredArgsConstructor
@Log4j2
public class JwtTokenProviderImpl implements TokenProvider {

    /**
     * TokenRepository 用於操作 Token 實體的數據庫操作類
     */
    private final TokenRepository tokenRepository;

    /**
     *
     */
    private final SecurityProperties securityProperties;


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
     *
     * @param user 用戶實體
     *
     * @return JWT 令牌字符串
     */
    @Override
    @HideSensitive
    public Mono<String> generateToken(User user) {
        Mono<Token> tokenMono = tokenRepository.findByUserId(user.getId()).switchIfEmpty(Mono.defer(() -> {
            Token newToken = new Token();
            newToken.setUserId(user.getId());
            return Mono.just(newToken);
        }));

        RoleEnum role = user.getRole();
        Date now = new Date();

        return tokenMono.flatMap(tokenEntity -> {
            int tokenVersion = tokenEntity.increaseAndGetJwtTokenVersion();

            long expirationMs = (long) securityProperties.getJwtToken().getExpiration() * 1000 * 60;
            Date expirationDate = new Date(now.getTime() + expirationMs);
            String jwtToken = Jwts
                    .builder()
                    .subject(String.valueOf(user.getId()))
                    .issuedAt(now)
                    .claim("role", role)
                    .claim("version", tokenVersion)
                    .expiration(expirationDate)
                    .signWith(key)
                    .compact();

            tokenEntity.setJwtTokenExpireTime(LocalDateTime.ofInstant(expirationDate.toInstant(), ZoneId.systemDefault()));
            return tokenRepository.save(tokenEntity).then(Mono.just(jwtToken));
        });
    }

    /**
     * 驗證 JWT 憑證，獲取用戶 ID
     * 此方法會根據 JWT 憑證中的用戶 ID 進行驗證，並根據 Token 實體中的 JWT 憑證版本進行版本管理
     * 驗證成功後返回用戶 ID，否則傳出 JWT_TOKEN_INVALID 錯誤
     *
     * @param token  JWT 憑證
     * @param userId 用戶 ID (此參數在此方法中無用)
     *
     * @return 用戶 ID
     */
    @Override
    public Mono<Long> validateToken(String token, Long userId) {
        return Mono.defer(() -> {
            try {
                Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
                long subject = Long.parseLong(claims.getSubject());
                return tokenRepository.findByUserId(subject).flatMap(tokenMono -> {
                    if (tokenMono.getJwtTokenVersion() != (int) claims.get("version")) {
                        return Mono.error(new ValidationException(ValidationException.ErrorCode.JWT_TOKEN_INVALID));
                    } else {
                        return Mono.just(subject);
                    }
                });
            } catch (Exception e) {
                return Mono.error(new ValidationException(ValidationException.ErrorCode.JWT_TOKEN_INVALID));
            }
        });
    }

    /**
     * 根據用戶 ID 刪除 JWT 憑證
     * 此方法會根據用戶 ID 查找 Token 實體，並將 JWT 憑證版本設為 0，過期時間設為登出的時間
     * 此時 JWT 在驗證時版本不匹配，並且過期時間在當前時間之前，即 JWT 憑證無效
     *
     * @param userId 用戶 ID
     */
    @Override
    public Mono<Void> revokeToken(Long userId) {
        return tokenRepository.findByUserId(userId).flatMap(tokenMono -> {
            tokenMono.setJwtTokenVersion(0);
            tokenMono.setJwtTokenExpireTime(LocalDateTime.now());
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
}
