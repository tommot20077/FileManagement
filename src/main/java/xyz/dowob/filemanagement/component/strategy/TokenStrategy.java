package xyz.dowob.filemanagement.component.strategy;

import org.springframework.stereotype.Component;
import xyz.dowob.filemanagement.component.provider.providerImplement.JwtTokenProviderImpl;
import xyz.dowob.filemanagement.component.provider.providerImplement.PasswordResetTokenProviderImpl;
import xyz.dowob.filemanagement.component.provider.providerInterface.TokenProvider;
import xyz.dowob.filemanagement.customenum.TokenEnum;

import java.util.EnumMap;
import java.util.Map;

/**
 * 基於 EnumMap 的令牌策略管理器實現，提供不同令牌類型對應的處理策略選擇機制。
 *
 * <p>此實現使用策略模式動態選擇令牌服務提供者，根據令牌類型枚舉值返回對應的處理策略。
 * 支援 JWT 授權令牌和密碼重置令牌兩種類型，當請求的令牌類型不存在對應策略時拋出 
 * {@link IllegalArgumentException}。令牌策略映射在建構時初始化，使用 EnumMap 
 * 保證類型安全性和查詢效能。</p>
 *
 * <p>新增令牌類型支援需要實作 {@link TokenProvider} 介面並在建構函數中註冊策略映射。
 * 所有策略實例通過依賴注入獲得，確保 Spring 容器管理生命週期。</p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see TokenProvider
 * @see TokenEnum
 */
@Component
public class TokenStrategy {
    /**
     * 令牌類型與對應提供者實現的策略映射表，使用 EnumMap 保證類型安全和效能。
     */
    private final Map<TokenEnum, TokenProvider> tokenStrategies;

    /**
     * 建構令牌策略管理器並初始化策略映射表。
     *
     * <p>通過依賴注入接收令牌提供者實現，並建立令牌類型與提供者的映射關係。
     * 使用 EnumMap 作為底層資料結構，確保類型安全且提供優異的查詢效能。</p>
     *
     * @param jwtTokenProviderImpl           JWT 授權令牌提供者實現，處理 JWT 相關操作
     * @param passwordResetTokenProviderImpl 密碼重置令牌提供者實現，處理密碼重置相關操作
     */
    public TokenStrategy(JwtTokenProviderImpl jwtTokenProviderImpl, PasswordResetTokenProviderImpl passwordResetTokenProviderImpl) {
        tokenStrategies = new EnumMap<>(TokenEnum.class);
        tokenStrategies.put(TokenEnum.JWT_AUTHORIZATION_TOKEN, jwtTokenProviderImpl);
        tokenStrategies.put(TokenEnum.RESET_PASSWORD_TOKEN, passwordResetTokenProviderImpl);
    }


    /**
     * 根據令牌類型枚舉值獲取對應的令牌提供者實現。
     *
     * <p>此方法從策略映射表中查找指定令牌類型對應的提供者實現。如果找不到對應的
     * 提供者，表示系統不支援該令牌類型或配置存在問題，將拋出異常提示錯誤。</p>
     *
     * @param tokenEnum 令牌類型枚舉值，指定要獲取的令牌提供者類型
     * @return 對應的令牌提供者實現，保證非空
     * @throws IllegalArgumentException 當找不到對應的令牌提供者實現時拋出
     */
    public TokenProvider getTokenProvider(TokenEnum tokenEnum) {
        TokenProvider tokenProvider = tokenStrategies.get(tokenEnum);
        if (tokenProvider == null) {
            throw new IllegalArgumentException("無法找到對應的憑證處理方法");
        }
        return tokenProvider;
    }
}
