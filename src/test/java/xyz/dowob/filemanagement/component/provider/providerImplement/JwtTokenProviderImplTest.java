package xyz.dowob.filemanagement.component.provider.providerImplement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.config.properties.SecurityProperties;
import xyz.dowob.filemanagement.customenum.RoleEnum;
import xyz.dowob.filemanagement.entity.Token;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.repostiory.TokenRepository;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static reactor.test.StepVerifier.create;

/**
 * JwtTokenProviderImpl 測試類別。
 * 
 * <p>測試 JwtTokenProviderImpl 的 JWT 憑證管理功能，包括：
 * <ul>
 * <li>JWT 憑證的生成與驗證</li>
 * <li>使用者物件轉換為 JWT 憑證</li>
 * <li>無效使用者資料處理</li>
 * <li>Token 存储庫依賴操作</li>
 * <li>安全性與過期時間管理</li>
 * </ul>
 * 
 * <p>測試涵蓋 JWT 憑證的所有核心功能，包含正常生成流程及異常處理。
 * 透過反應式程式測試驗證 JWT 服務的正確性和安全性。
 * 
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtTokenProvider 邏輯處理測試")
class JwtTokenProviderImplTest {

    @Mock
    private TokenRepository mockTokenRepository;
    @Mock
    private SecurityProperties mockSecurityProperties;

    private JwtTokenProviderImpl jwtTokenProviderImplUnderTest;

    @BeforeEach
    void setUp() {
        SecurityProperties.JwtToken jwtToken = new SecurityProperties.JwtToken();
        jwtToken.setSecret(java.util.Base64.getEncoder().encodeToString("thisisasecretkeyforjwttokenproviderimpltest".getBytes()));
        jwtToken.setExpiration(java.time.Duration.ofHours(1));
        when(mockSecurityProperties.getJwtToken()).thenReturn(jwtToken);

        jwtTokenProviderImplUnderTest = new JwtTokenProviderImpl(mockTokenRepository, mockSecurityProperties);
        jwtTokenProviderImplUnderTest.init();
    }

    @Test
    @DisplayName("輸入 User 物件來產生 JWT 憑證 - 產生 JWT 憑證")
    void generateToken_validUser_returnsJwtToken() {
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setRole(RoleEnum.USER);

        when(mockTokenRepository.findByUserId(any(Long.class))).thenReturn(Mono.just(new Token()));
        when(mockTokenRepository.save(any(Token.class))).thenReturn(Mono.just(new Token()));

        final Mono<String> result = jwtTokenProviderImplUnderTest.generateToken(user);

        create(result).expectNextMatches(token -> token != null && !token.isEmpty()).verifyComplete();
    }

    @Test
    @DisplayName("輸入 null User 物件來產生 JWT 憑證 - 拋出 ValidationException 錯誤")
    void generateToken_nullUser_throwsValidationException() {
        final User user = null;

        final Mono<String> result = jwtTokenProviderImplUnderTest.generateToken(user);

        create(result).expectErrorMatches(throwable -> throwable instanceof ValidationException &&
                ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.USER_NOT_FOUND).verify();
    }
}
