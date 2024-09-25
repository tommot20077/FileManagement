package xyz.dowob.filemanagement.component.factory;

import org.springframework.stereotype.Component;
import xyz.dowob.filemanagement.component.provider.providerImplement.JwtTokenProviderImpl;
import xyz.dowob.filemanagement.component.provider.providerImplement.PasswordResetTokenProviderImpl;
import xyz.dowob.filemanagement.component.provider.providerInterface.TokenProvider;
import xyz.dowob.filemanagement.customenum.TokenEnum;

import java.util.EnumMap;
import java.util.Map;

/**
 * Token 策略工廠，用於管理 TokenProvider調用的部分
 * 會根據不同的 TokenEnum 返回不同實現類型的憑證處理方法
 * 1. JwtTokenProviderImpl: 用於處理 JWT 憑證 {@link JwtTokenProviderImpl}
 * 2. PasswordResetTokenProviderImpl: 用於處理重置密碼憑證 {@link PasswordResetTokenProviderImpl}
 *
 * @author yuan
 * @program File-Management
 * @ClassName TokenStrategyFactory
 * @description
 * @create 2024-09-20 13:02
 * @Version 1.0
 **/
@Component
public class TokenStrategyFactory {
    /**
     * 工廠管理的憑證策略
     */
    private final Map<TokenEnum, TokenProvider> tokenStrategies;

    /**
     * TokenStrategyFactory 的構造方法，用於初始化 TokenProvider
     * 後續如果需要新增其他 TokenProvider，可以在這裡添加
     *
     * @param jwtTokenProviderImpl           JwtTokenProviderImpl 實現類
     * @param passwordResetTokenProviderImpl PasswordResetTokenProviderImpl 實現類
     */
    public TokenStrategyFactory(JwtTokenProviderImpl jwtTokenProviderImpl, PasswordResetTokenProviderImpl passwordResetTokenProviderImpl) {
        tokenStrategies = new EnumMap<>(TokenEnum.class);
        tokenStrategies.put(TokenEnum.JWT_AUTHORIZATION_TOKEN, jwtTokenProviderImpl);
        tokenStrategies.put(TokenEnum.RESET_PASSWORD_TOKEN, passwordResetTokenProviderImpl);
    }

    /**
     * 根據 TokenEnum 返回對應的 TokenProvider
     *
     * @param tokenEnum TokenEnum
     *
     * @return TokenProvider
     */
    public TokenProvider getTokenProvider(TokenEnum tokenEnum) {
        TokenProvider tokenProvider = tokenStrategies.get(tokenEnum);
        if (tokenProvider == null) {
            throw new IllegalArgumentException("無法找到對應的憑證處理方法");
        }
        return tokenProvider;
    }
}
