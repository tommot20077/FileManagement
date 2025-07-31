package xyz.dowob.filemanagement.component.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.dowob.filemanagement.component.provider.providerImplement.JwtTokenProviderImpl;
import xyz.dowob.filemanagement.component.provider.providerImplement.PasswordResetTokenProviderImpl;
import xyz.dowob.filemanagement.component.provider.providerInterface.TokenProvider;
import xyz.dowob.filemanagement.customenum.TokenEnum;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TokenStrategy 測試類別。
 * 
 * <p>測試 TokenStrategy 的憑證策略選擇功能，包括：
 * <ul>
 * <li>JWT 授權憑證提供者的獲取</li>
 * <li>密碼重設憑證提供者的獲取</li>
 * <li>無效憑證類型的異常處理</li>
 * <li>所有支援憑證類型的完整性驗證</li>
 * <li>憑證提供者的正確映射與選擇</li>
 * </ul>
 * 
 * <p>測試涵蓋策略模式下憑證管理的所有核心功能，包含正常情況、異常處理及系統健墯性。
 * 透過反應式程式測試驗證憑證策略的正確性和依賴注入的可靠性。
 * 
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TokenStrategy 邏輯處理測試")
class TokenStrategyTest {

    @Mock
    private JwtTokenProviderImpl jwtTokenProvider;
    
    @Mock
    private PasswordResetTokenProviderImpl passwordResetTokenProvider;
    
    private TokenStrategy tokenStrategy;

    @BeforeEach
    void setUp() {
        tokenStrategy = new TokenStrategy(jwtTokenProvider, passwordResetTokenProvider);
    }

    @Test
    @DisplayName("測試獲取JWT授權TokenProvider - 返回JwtTokenProviderImpl")
    void getTokenProvider_WhenJwtAuthorizationToken_ReturnsJwtProvider() {
        StepVerifier.create(Mono.fromCallable(() -> tokenStrategy.getTokenProvider(TokenEnum.JWT_AUTHORIZATION_TOKEN)))
                .expectNext(jwtTokenProvider)
                .verifyComplete();
    }

    @Test
    @DisplayName("測試獲取密碼重置TokenProvider - 返回PasswordResetTokenProviderImpl")
    void getTokenProvider_WhenResetPasswordToken_ReturnsPasswordResetProvider() {
        StepVerifier.create(Mono.fromCallable(() -> tokenStrategy.getTokenProvider(TokenEnum.RESET_PASSWORD_TOKEN)))
                .expectNext(passwordResetTokenProvider)
                .verifyComplete();
    }

    @Test
    @DisplayName("測試無效Token類型 - 拋出IllegalArgumentException")
    void getTokenProvider_WhenInvalidTokenType_ThrowsException() {
        StepVerifier.create(Mono.fromCallable(() -> tokenStrategy.getTokenProvider(null)))
                .expectErrorSatisfies(throwable -> {
                    assertTrue(throwable instanceof IllegalArgumentException);
                    assertEquals("無法找到對應的憑證處理方法", throwable.getMessage());
                })
                .verify();
    }

    @Test
    @DisplayName("測試所有支持的Token類型 - 成功獲取TokenProvider")
    void getTokenProvider_AllSupportedTokens() {
        for (TokenEnum tokenType : TokenEnum.values()) {
            assertDoesNotThrow(() -> {
                TokenProvider provider = tokenStrategy.getTokenProvider(tokenType);
                assertNotNull(provider);
            });
        }
    }
}
