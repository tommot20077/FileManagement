package xyz.dowob.filemanagement.service.serviceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.dowob.filemanagement.component.provider.providerInterface.TokenProvider;
import xyz.dowob.filemanagement.component.strategy.TokenStrategy;
import xyz.dowob.filemanagement.customenum.TokenEnum;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.exception.ValidationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@SuppressWarnings("all")
@ExtendWith(MockitoExtension.class)
@DisplayName("TokenService 邏輯處理測試")
class TokenServiceImplTest {
    @Mock
    private TokenStrategy mockTokenStrategy;

    private TokenServiceImpl tokenServiceImplUnderTest;

    @BeforeEach
    void setUp() {
        tokenServiceImplUnderTest = new TokenServiceImpl(mockTokenStrategy);
    }

    @Test
    @DisplayName("產生 JWT Token - 傳入有效 User 應回傳 token")
    void generateToken_validUser_returnsToken() {
        User user = new User();
        String expectedToken = "valid-token";
        TokenProvider mockProvider = mock(TokenProvider.class);

        doReturn(mockProvider).when(mockTokenStrategy).getTokenProvider(TokenEnum.JWT_AUTHORIZATION_TOKEN);
        doReturn(Mono.just(expectedToken)).when(mockProvider).generateToken(user);

        StepVerifier
                .create(tokenServiceImplUnderTest.generateToken(user, TokenEnum.JWT_AUTHORIZATION_TOKEN))
                .expectNext(expectedToken)
                .verifyComplete();

        verify(mockProvider).generateToken(user);
        verify(mockTokenStrategy).getTokenProvider(TokenEnum.JWT_AUTHORIZATION_TOKEN);
    }

    @Test
    @DisplayName("產生 JWT Token - 傳入 null User 應拋出 USER_NOT_FOUND ValidationException")
    void generateToken_nullUser_propagatesValidationException() {
        TokenProvider mockProvider = mock(TokenProvider.class);
        when(mockTokenStrategy.getTokenProvider(TokenEnum.JWT_AUTHORIZATION_TOKEN)).thenReturn(mockProvider);
        when(mockProvider.generateToken(null)).thenReturn(Mono.error(new ValidationException(ValidationException.ErrorCode.USER_NOT_FOUND, "test")));

        StepVerifier
                .create(tokenServiceImplUnderTest.generateToken(null, TokenEnum.JWT_AUTHORIZATION_TOKEN))
                .expectErrorMatches(e -> e instanceof ValidationException validationException && validationException.getErrorCode() == ValidationException.ErrorCode.USER_NOT_FOUND)
                .verify();

        verify(mockTokenStrategy, times(1)).getTokenProvider(TokenEnum.JWT_AUTHORIZATION_TOKEN);
        verify(mockProvider, times(1)).generateToken(null);
    }

    @Test
    @DisplayName("產生 JWT Token - 使用無效的 TokenEnum 應立即拋出 IllegalArgumentException")
    void generateToken_invalidTokenEnum_throwsImmediately() {
        User user = new User();
        when(mockTokenStrategy.getTokenProvider(TokenEnum.JWT_AUTHORIZATION_TOKEN)).thenThrow(new IllegalArgumentException("無法找到對應的憑證處理方法"));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                                                          () -> tokenServiceImplUnderTest.generateToken(user, TokenEnum.JWT_AUTHORIZATION_TOKEN)
        );

        assertEquals("無法找到對應的憑證處理方法", exception.getMessage());
        verify(mockTokenStrategy).getTokenProvider(TokenEnum.JWT_AUTHORIZATION_TOKEN);
    }

    @Test
    @DisplayName("驗證 Token - 傳入有效 token 應回傳 userId")
    void validateToken_validToken_returnsUserId() {
        String token = "valid-token";
        Long userId = 123L;
        TokenProvider mockProvider = mock(TokenProvider.class);
        when(mockTokenStrategy.getTokenProvider(TokenEnum.JWT_AUTHORIZATION_TOKEN)).thenReturn(mockProvider);
        when(mockProvider.validateToken(token, null)).thenReturn(Mono.just(userId));

        StepVerifier
                .create(tokenServiceImplUnderTest.validateToken(token, null, TokenEnum.JWT_AUTHORIZATION_TOKEN))
                .expectNext(userId)
                .verifyComplete();

        verify(mockTokenStrategy, times(1)).getTokenProvider(TokenEnum.JWT_AUTHORIZATION_TOKEN);
        verify(mockProvider, times(1)).validateToken(token, null);
    }

    @Test
    @DisplayName("驗證 Token - 傳入 null token 應拋出 JWT_TOKEN_INVALID ValidationException")
    void validateToken_nullToken_propagatesValidationException() {
        TokenProvider mockProvider = mock(TokenProvider.class);
        when(mockTokenStrategy.getTokenProvider(TokenEnum.JWT_AUTHORIZATION_TOKEN)).thenReturn(mockProvider);
        when(mockProvider.validateToken(null, null)).thenReturn(Mono.error(new ValidationException(ValidationException.ErrorCode.JWT_TOKEN_INVALID)));

        StepVerifier
                .create(tokenServiceImplUnderTest.validateToken(null, null, TokenEnum.JWT_AUTHORIZATION_TOKEN))
                .expectErrorMatches(e -> e instanceof ValidationException validationException && validationException.getErrorCode() == ValidationException.ErrorCode.JWT_TOKEN_INVALID)
                .verify();

        verify(mockTokenStrategy, times(1)).getTokenProvider(TokenEnum.JWT_AUTHORIZATION_TOKEN);
        verify(mockProvider, times(1)).validateToken(null, null);
    }

    @Test
    @DisplayName("驗證 Token - 使用無效的 TokenEnum 應拋出 IllegalArgumentException")
    void validateToken_invalidTokenEnum_propagatesException() {
        String token = "valid-token";
        when(mockTokenStrategy.getTokenProvider(TokenEnum.JWT_AUTHORIZATION_TOKEN)).thenThrow(new IllegalArgumentException("無法找到對應的憑證處理方法"));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                                                          () -> tokenServiceImplUnderTest.validateToken(token,
                                                                                                        null,
                                                                                                        TokenEnum.JWT_AUTHORIZATION_TOKEN
                                                          )
        );

        assertEquals("無法找到對應的憑證處理方法", exception.getMessage());
        verify(mockTokenStrategy).getTokenProvider(TokenEnum.JWT_AUTHORIZATION_TOKEN);
    }

    @Test
    @DisplayName("撤銷 Token - 傳入有效 userId 應正常完成")
    void revokeToken_validUserId_completesSuccessfully() {
        Long userId = 123L;
        TokenProvider mockProvider = mock(TokenProvider.class);
        when(mockTokenStrategy.getTokenProvider(TokenEnum.JWT_AUTHORIZATION_TOKEN)).thenReturn(mockProvider);
        when(mockProvider.revokeToken(userId)).thenReturn(Mono.empty());


        StepVerifier.create(tokenServiceImplUnderTest.revokeToken(userId, TokenEnum.JWT_AUTHORIZATION_TOKEN)).verifyComplete();

        verify(mockTokenStrategy, times(1)).getTokenProvider(TokenEnum.JWT_AUTHORIZATION_TOKEN);
        verify(mockProvider, times(1)).revokeToken(userId);
    }

    @Test
    @DisplayName("撤銷 Token - 傳入 null userId 應拋出 USER_NOT_FOUND ValidationException")
    void revokeToken_nullUserId_propagatesValidationException() {
        TokenProvider mockProvider = mock(TokenProvider.class);
        when(mockTokenStrategy.getTokenProvider(TokenEnum.JWT_AUTHORIZATION_TOKEN)).thenReturn(mockProvider);
        when(mockProvider.revokeToken(null)).thenReturn(Mono.error(new ValidationException(ValidationException.ErrorCode.USER_NOT_FOUND, "test")));


        StepVerifier
                .create(tokenServiceImplUnderTest.revokeToken(null, TokenEnum.JWT_AUTHORIZATION_TOKEN))
                .expectErrorMatches(e -> e instanceof ValidationException validationException && validationException.getErrorCode() == ValidationException.ErrorCode.USER_NOT_FOUND)
                .verify();

        verify(mockTokenStrategy, times(1)).getTokenProvider(TokenEnum.JWT_AUTHORIZATION_TOKEN);
        verify(mockProvider, times(1)).revokeToken(null);
    }
}