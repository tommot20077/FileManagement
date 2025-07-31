package xyz.dowob.filemanagement.component.provider.providerImplement;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.HideSensitive;
import xyz.dowob.filemanagement.annotation.RecordLevel;
import xyz.dowob.filemanagement.component.provider.providerInterface.TokenProvider;
import xyz.dowob.filemanagement.config.properties.SecurityProperties;
import xyz.dowob.filemanagement.customenum.LogLevelEnum;
import xyz.dowob.filemanagement.entity.Token;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.repostiory.TokenRepository;

import java.time.LocalDateTime;

/**
 * 密碼重置驗證令牌提供者，從反應式的觀點管理密碼重置流程。
 *
 * <p>此類別實現了 {@link xyz.dowob.filemanagement.component.provider.providerInterface.TokenProvider} 介面，
 * 提供安全且反應式的密碼重置機制。</p>
 *
 * <p>重要特性：
 * <ul>
 *   <li>使用並時性令牌生成機制</li>
 *   <li>重置密碼驗證碼可設定</li>
 *   <li>對令牌設定超時時間</li>
 *   <li>令牌與用戶 ID 的安全鑑權</li>
 * </ul>
 * </p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@Component
@RecordLevel(LogLevelEnum.DEBUG)
@RequiredArgsConstructor
public class PasswordResetTokenProviderImpl implements TokenProvider {
    /**
     * 令牌儲存庫操作維護，專責管理重置密碼令牌的持久化和查詢作業。
     *
     * @see xyz.dowob.filemanagement.repostiory.TokenRepository
     */
    private final TokenRepository tokenRepository;

    /**
     * 全域安全設定屬性，管理密碼重置令牌的安全設定。
     *
     * <p>主要設定項目：
     * <ul>
     *   <li>重置密碼驗證碼的位數長度</li>
     *   <li>重置密碼驗證碼的有效期限</li>
     * </ul>
     * </p>
     *
     * @see xyz.dowob.filemanagement.config.properties.SecurityProperties
     */
    private final SecurityProperties securityProperties;

    /**
     * 生成密碼重置的安全驗證碼，使用第5層反應式設計。
     *
     * <p>生成流程：
     * <ol>
     *   <li>根據設定生成指定位數的驗證碼</li>
     *   <li>將驗證碼儲存至用戶令牌實體</li>
     *   <li>設定驗證碼超時時間</li>
     *   <li>儲存令牌至資料庫</li>
     * </ol>
     * </p>
     *
     * @param user 用戶實體
     * @return {@link reactor.core.publisher.Mono<String>} 非同步生成的驗證碼
     */
    @Override
    @HideSensitive
    public Mono<String> generateToken(User user) {
        double verificationCodeInit = Math.pow(10, securityProperties.getResetPasswordToken().getLength() - 1);
        String verificationCode = String.valueOf((int) ((Math.random() * 9 + 1) * verificationCodeInit));
        Mono<Token> tokenMono = tokenRepository.findByUserId(user.getId()).switchIfEmpty(Mono.defer(() -> {
            Token newToken = new Token();
            newToken.setJwtTokenVersion(Token.generateJwtTokenVersion());
            newToken.setUserId(user.getId());
            return Mono.just(newToken);
        }));

        return tokenMono.flatMap(token -> {
            token.setResetVerificationCode(verificationCode);
            LocalDateTime expireTime = LocalDateTime.now().plusMinutes(securityProperties.getResetPasswordToken().getExpiration().toMinutes());
            token.setResetVerificationCodeExpireTime(expireTime);
            return tokenRepository.save(token).thenReturn(verificationCode);
        });
    }


    /**
     * 非同步驗證密碼重置驗證碼的有效性。
     *
     * <p>驗證流程：
     * <ol>
     *   <li>檢查用戶儲存的驗證碼實體</li>
     *   <li>檢查驗證碼是否匹配</li>
     *   <li>檢查驗證碼是否過期</li>
     * </ol>
     * </p>
     *
     * @param token 待驗證的驗證碼
     * @param userId 用戶 ID
     * @return {@link reactor.core.publisher.Mono<Long>} 非同步回傳的用戶 ID
     */
    @Override
    public Mono<Long> validateToken(String token, Long userId) {
        return tokenRepository
                .findByUserId(userId)
                .switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.VERIFICATION_CODE_ERROR)))
                .flatMap(tokenEntity -> {
                    if (tokenEntity.getResetVerificationCode() == null || !tokenEntity.getResetVerificationCode().equals(token)) {
                        return Mono.error(new ValidationException(ValidationException.ErrorCode.VERIFICATION_CODE_ERROR));
                    }
                    if (tokenEntity.getResetVerificationCodeExpireTime() == null || LocalDateTime
                            .now()
                            .isAfter(tokenEntity.getResetVerificationCodeExpireTime())) {
                        return Mono.error(new ValidationException(ValidationException.ErrorCode.VERIFICATION_CODE_ERROR));
                    }
                    return Mono.just(userId);
                });
    }


    /**
     * 失效密碼重置驗證碼，確保安全令牌的生命週期。
     *
     * <p>失效流程：
     * <ol>
     *   <li>查詢用戶的驗證碼實體</li>
     *   <li>清除驗證碼</li>
     *   <li>將驗證碼超時時間設為立即</li>
     * </ol>
     * </p>
     *
     * @param userId 用戶 ID
     * @return {@link reactor.core.publisher.Mono<Void>} 非同步失效作業
     */
    @Override
    public Mono<Void> revokeToken(Long userId) {
        return tokenRepository.findByUserId(userId).flatMap(tokenMono -> {
            tokenMono.setResetVerificationCode(null);
            tokenMono.setResetVerificationCodeExpireTime(LocalDateTime.now());
            return tokenRepository.save(tokenMono);
        }).then();
    }
}
