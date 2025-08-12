package xyz.dowob.filemanagement.service.serviceImpl;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.dowob.filemanagement.component.provider.providerImplement.JwtTokenProviderImpl;
import xyz.dowob.filemanagement.component.provider.providerInterface.TokenProvider;
import xyz.dowob.filemanagement.component.strategy.TokenStrategy;
import xyz.dowob.filemanagement.customenum.TokenEnum;
import xyz.dowob.filemanagement.dto.UserInfoDto;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.repostiory.TokenRepository;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * 權杖服務實現測試類別。
 * 
 * 測試 TokenServiceImpl 類別的核心功能，包括權杖產生、驗證、更新和刪除操作。
 * 此測試類別驗證服務層的權杖管理邏輯和 JWT 權杖處理機制。
 * 
 * <p>測試涵蓋範圍：
 * <ul>
 * <li>JWT 權杖的產生和解析</li>
 * <li>權杖的有效性驗證</li>
 * <li>權杖更新和刷新機制</li>
 * <li>權杖的儲存和刪除</li>
 * <li>WebFlux 反應式權杖處理流程</li>
 * </ul>
 * 
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see TokenServiceImpl
 * @see JwtTokenProviderImpl
 * @see TokenRepository
 */
@SuppressWarnings("all")
@ExtendWith(MockitoExtension.class)
@DisplayName("TokenService 邏輯處理測試")
class TokenServiceImplTest {
    @Mock
    private TokenStrategy mockTokenStrategy;

    private TokenServiceImpl tokenServiceImplUnderTest;

    /**
     * 測試前置作業。
     * 
     * 初始化 TokenServiceImpl 實例和所需的模擬依賴項。
     * 設定測試權杖和使用者資料，確保每個測試方法都有乾淨的起始狀態。
     */
    @BeforeEach
    void setUp() {
        tokenServiceImplUnderTest = new TokenServiceImpl(mockTokenStrategy);
    }

    /**
     * 測試有效使用者的 JWT 權杖產生。
     * 
     * 驗證當提供有效的使用者對象時，權杖服務能成功產生並回傳 JWT 權杖。
     * 確保權杖產生流程的正確運作和策略模式的實現。
     * 
     * 前置條件：
     * - 使用者對象非空且有效
     * - 權杖提供者策略正確配置
     * 
     * 測試步驟：
     * - 準備有效的使用者對象
     * - 模擬權杖提供者和權杖產生
     * - 執行權杖產生操作
     * 
     * 預期結果：
     * - 成功回傳有效的 JWT 權杖
     * - 相關服務方法被正確調用
     */
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

    @Test
    @DisplayName("產生 RESET_PASSWORD_TOKEN - 傳入有效 User 應回傳 token")
    void generateToken_validUser_returnsResetPasswordToken() {
        User user = new User();
        String expectedToken = "reset-password-token";
        TokenProvider mockProvider = mock(TokenProvider.class);

        doReturn(mockProvider).when(mockTokenStrategy).getTokenProvider(TokenEnum.RESET_PASSWORD_TOKEN);
        doReturn(Mono.just(expectedToken)).when(mockProvider).generateToken(user);

        StepVerifier
                .create(tokenServiceImplUnderTest.generateToken(user, TokenEnum.RESET_PASSWORD_TOKEN))
                .expectNext(expectedToken)
                .verifyComplete();

        verify(mockProvider).generateToken(user);
        verify(mockTokenStrategy).getTokenProvider(TokenEnum.RESET_PASSWORD_TOKEN);
    }

    @Test
    @DisplayName("驗證 RESET_PASSWORD_TOKEN - 傳入有效 token 應回傳 userId")
    void validateToken_validResetPasswordToken_returnsUserId() {
        String token = "valid-reset-token";
        Long userId = 456L;
        TokenProvider mockProvider = mock(TokenProvider.class);
        when(mockTokenStrategy.getTokenProvider(TokenEnum.RESET_PASSWORD_TOKEN)).thenReturn(mockProvider);
        when(mockProvider.validateToken(token, userId)).thenReturn(Mono.just(userId));

        StepVerifier
                .create(tokenServiceImplUnderTest.validateToken(token, userId, TokenEnum.RESET_PASSWORD_TOKEN))
                .expectNext(userId)
                .verifyComplete();

        verify(mockTokenStrategy, times(1)).getTokenProvider(TokenEnum.RESET_PASSWORD_TOKEN);
        verify(mockProvider, times(1)).validateToken(token, userId);
    }

    @Test
    @DisplayName("撤銷 RESET_PASSWORD_TOKEN - 傳入有效 userId 應正常完成")
    void revokeToken_validUserId_completesSuccessfullyForResetPasswordToken() {
        Long userId = 789L;
        TokenProvider mockProvider = mock(TokenProvider.class);
        when(mockTokenStrategy.getTokenProvider(TokenEnum.RESET_PASSWORD_TOKEN)).thenReturn(mockProvider);
        when(mockProvider.revokeToken(userId)).thenReturn(Mono.empty());

        StepVerifier.create(tokenServiceImplUnderTest.revokeToken(userId, TokenEnum.RESET_PASSWORD_TOKEN))
                .verifyComplete();

        verify(mockTokenStrategy, times(1)).getTokenProvider(TokenEnum.RESET_PASSWORD_TOKEN);
        verify(mockProvider, times(1)).revokeToken(userId);
    }

    @Test
    @DisplayName("撤銷 Token - 使用無效的 TokenEnum 應拋出 IllegalArgumentException")
    void revokeToken_invalidTokenEnum_propagatesException() {
        Long userId = 123L;
        when(mockTokenStrategy.getTokenProvider(TokenEnum.JWT_AUTHORIZATION_TOKEN)).thenThrow(new IllegalArgumentException("無法找到對應的憑證處理方法"));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                                                          () -> tokenServiceImplUnderTest.revokeToken(userId, TokenEnum.JWT_AUTHORIZATION_TOKEN)
        );

        assertEquals("無法找到對應的憑證處理方法", exception.getMessage());
        verify(mockTokenStrategy).getTokenProvider(TokenEnum.JWT_AUTHORIZATION_TOKEN);
    }

    @Test
    @DisplayName("create - 應回傳空的 Mono")
    void create_returnsEmptyMono() {
        StepVerifier
                .create(tokenServiceImplUnderTest.create())
                .verifyComplete();
    }

    @Test
    @DisplayName("getById - 傳入任何 ID 應回傳空的 Mono")
    void getById_anyId_returnsEmptyMono() {
        StepVerifier
                .create(tokenServiceImplUnderTest.getById(1L))
                .verifyComplete();
    }

    @Test
    @DisplayName("getAll - 應回傳空的 Flux")
    void getAll_returnsEmptyFlux() {
        StepVerifier
                .create(tokenServiceImplUnderTest.getAll())
                .verifyComplete();
    }

    @Test
    @DisplayName("getAllByParams - 傳入任何參數應回傳空的 Flux")
    void getAllByParams_anyParams_returnsEmptyFlux() {
        StepVerifier
                .create(tokenServiceImplUnderTest.getAllByParams("type", "param1", "param2"))
                .verifyComplete();
    }

    @Test
    @DisplayName("update - 傳入任何 Token 應回傳空的 Mono")
    void update_anyToken_returnsEmptyMono() {
        StepVerifier
                .create(tokenServiceImplUnderTest.update(new xyz.dowob.filemanagement.entity.Token()))
                .verifyComplete();
    }

    @Test
    @DisplayName("delete - 傳入任何 Token 應回傳空的 Mono")
    void delete_anyToken_returnsEmptyMono() {
        StepVerifier
                .create(tokenServiceImplUnderTest.delete(new xyz.dowob.filemanagement.entity.Token()))
                .verifyComplete();
    }

    @Test
    @DisplayName("從 Token 提取 UserId - 傳入有效 JWT Token 應回傳 userId")
    void extractUserInfoFromToken_validJwtToken_returnsUserId() {
        String jwtToken = "valid-jwt-token";
        Long expectedUserId = 123L;
        JwtTokenProviderImpl mockJwtProvider = mock(JwtTokenProviderImpl.class);

        // 建立模擬的Claims對象
        Claims mockClaims = mock(Claims.class);
        Date expiration = new Date(System.currentTimeMillis() + 3600000);
        when(mockClaims.getSubject()).thenReturn("123");
        when(mockClaims.get("username", String.class)).thenReturn("testUser");
        when(mockClaims.get("role", String.class)).thenReturn("USER");
        when(mockClaims.getExpiration()).thenReturn(expiration);
        when(mockClaims.get("version", String.class)).thenReturn("1.0");

        UserInfoDto expectedUserInfoDto = UserInfoDto.builder()
                                             .userId(expectedUserId)
                                             .username("testUser")
                                             .role("USER")
                                             .tokenExpiry(expiration)
                                             .tokenVersion("1.0")
                                             .build();

        when(mockTokenStrategy.getTokenProvider(TokenEnum.JWT_AUTHORIZATION_TOKEN)).thenReturn(mockJwtProvider);
        when(mockJwtProvider.getClaimsFromToken(jwtToken)).thenReturn(Mono.just(mockClaims));

        StepVerifier
                .create(tokenServiceImplUnderTest.extractUserInfoFromToken(jwtToken, TokenEnum.JWT_AUTHORIZATION_TOKEN))
                .expectNext(expectedUserInfoDto)
                .verifyComplete();

        verify(mockTokenStrategy).getTokenProvider(TokenEnum.JWT_AUTHORIZATION_TOKEN);
        verify(mockJwtProvider).getClaimsFromToken(jwtToken);
    }

    @Test
    @DisplayName("從 Token 提取 UserId - 傳入無效 Token 應拋出 JWT_TOKEN_INVALID ValidationException")
    void extractUserInfoFromToken_invalidToken_throwsValidationException() {
        String invalidToken = "invalid-token";
        JwtTokenProviderImpl mockJwtProvider = mock(JwtTokenProviderImpl.class);

        when(mockTokenStrategy.getTokenProvider(TokenEnum.JWT_AUTHORIZATION_TOKEN)).thenReturn(mockJwtProvider);
        when(mockJwtProvider.getClaimsFromToken(invalidToken))
                .thenReturn(Mono.error(new ValidationException(ValidationException.ErrorCode.JWT_TOKEN_INVALID)));

        StepVerifier
                .create(tokenServiceImplUnderTest.extractUserInfoFromToken(invalidToken, TokenEnum.JWT_AUTHORIZATION_TOKEN))
                .expectErrorMatches(e -> e instanceof ValidationException validationException 
                    && validationException.getErrorCode() == ValidationException.ErrorCode.JWT_TOKEN_INVALID)
                .verify();

        verify(mockTokenStrategy).getTokenProvider(TokenEnum.JWT_AUTHORIZATION_TOKEN);
        verify(mockJwtProvider).getClaimsFromToken(invalidToken);
    }

    @Test
    @DisplayName("從 Token 提取 UserId - 傳入 null Token 應拋出 JWT_TOKEN_INVALID ValidationException")
    void extractUserInfoFromToken_nullToken_throwsValidationException() {
        JwtTokenProviderImpl mockJwtProvider = mock(JwtTokenProviderImpl.class);

        when(mockTokenStrategy.getTokenProvider(TokenEnum.JWT_AUTHORIZATION_TOKEN)).thenReturn(mockJwtProvider);
        when(mockJwtProvider.getClaimsFromToken(null))
                .thenReturn(Mono.error(new ValidationException(ValidationException.ErrorCode.JWT_TOKEN_INVALID)));

        StepVerifier
                .create(tokenServiceImplUnderTest.extractUserInfoFromToken(null, TokenEnum.JWT_AUTHORIZATION_TOKEN))
                .expectErrorMatches(e -> e instanceof ValidationException validationException 
                    && validationException.getErrorCode() == ValidationException.ErrorCode.JWT_TOKEN_INVALID)
                .verify();

        verify(mockTokenStrategy).getTokenProvider(TokenEnum.JWT_AUTHORIZATION_TOKEN);
        verify(mockJwtProvider).getClaimsFromToken(null);
    }
}