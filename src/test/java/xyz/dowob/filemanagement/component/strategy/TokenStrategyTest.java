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
