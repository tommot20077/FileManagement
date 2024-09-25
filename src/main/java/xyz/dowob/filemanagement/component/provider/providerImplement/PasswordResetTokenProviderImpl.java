package xyz.dowob.filemanagement.component.provider.providerImplement;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.HideSensitive;
import xyz.dowob.filemanagement.component.provider.providerInterface.TokenProvider;
import xyz.dowob.filemanagement.entity.Token;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.repostiory.TokenRepository;

import java.time.LocalDateTime;

/**
 * 重製密碼憑證提供者實現類，用於生成重置密碼憑證
 * 實現了 TokenProvider 接口用於定義 TokenProvider 所需的方法
 *
 * @author yuan
 * @program File-Management
 * @ClassName PasswordResetTokenProviderImpl
 * @description
 * @create 2024-09-20 01:12
 * @Version 1.0
 **/
@Component
@RequiredArgsConstructor
public class PasswordResetTokenProviderImpl implements TokenProvider {
    /**
     * TokenRepository 用於操作 Token 實體的數據庫操作類
     */
    private final TokenRepository tokenRepository;

    /**
     * 驗證碼過期時間，從配置文件中獲取
     * 單位：分鐘
     */
    @Value("${common.security.verificationcode.expiration: 10}")
    private int verificationCodeExpiration;

    /**
     * 生成重置密碼的6位驗證碼
     *
     * @param user 用戶
     *
     * @return 返回生成的驗證碼
     */
    @Override
    @HideSensitive
    public Mono<String> generateToken(User user) {
        String verificationCode = String.valueOf((int) ((Math.random() * 9 + 1) * 100000));
        Mono<Token> tokenMono = tokenRepository.findByUserId(user.getId()).switchIfEmpty(Mono.defer(() -> {
            Token newToken = new Token();
            newToken.setUserId(user.getId());
            return Mono.just(newToken);
        }));

        return tokenMono.flatMap(token -> {
            token.setResetVerificationCode(verificationCode);
            LocalDateTime expireTime = LocalDateTime.now().plusMinutes(verificationCodeExpiration);
            token.setResetVerificationCodeExpireTime(expireTime);
            return tokenRepository.save(token).thenReturn(verificationCode);
        });
    }


    /**
     * 驗證憑證，並返回用戶ID
     * 當憑證無效時，傳出 VERIFICATION_CODE_ERROR 錯誤
     *
     * @param token  憑證
     * @param userId 用戶ID
     *
     * @return 返回用戶ID
     *
     */
    @Override
    public Mono<Long> validateToken(String token, Long userId) {
        return tokenRepository.findByUserId(userId)
                              .switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.VERIFICATION_CODE_ERROR)))
                              .flatMap(tokenEntity -> {
                                  if (tokenEntity.getResetVerificationCode() == null || !tokenEntity.getResetVerificationCode()
                                                                                                    .equals(token)) {
                                      return Mono.error(new ValidationException(ValidationException.ErrorCode.VERIFICATION_CODE_ERROR));
                                  }
                                  if (tokenEntity.getResetVerificationCodeExpireTime() == null || LocalDateTime.now()
                                                                                                               .isAfter(tokenEntity.getResetVerificationCodeExpireTime())) {
                                      return Mono.error(new ValidationException(ValidationException.ErrorCode.VERIFICATION_CODE_ERROR));
                                  }
                                  return Mono.just(userId);
                              });
    }

    /**
     * 根據用戶ID刪除憑證
     * 此方法用於重置密碼憑證，當用戶重置密碼後刪除憑證
     *
     * @param userId 用戶ID
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
