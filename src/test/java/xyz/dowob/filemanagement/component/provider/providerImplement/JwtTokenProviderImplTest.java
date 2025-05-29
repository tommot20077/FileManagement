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
