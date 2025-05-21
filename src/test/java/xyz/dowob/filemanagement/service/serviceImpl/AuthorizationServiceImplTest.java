package xyz.dowob.filemanagement.service.serviceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.security.web.server.csrf.DefaultCsrfToken;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.dowob.filemanagement.component.strategy.CsrfTokenRepositoryStrategy;
import xyz.dowob.filemanagement.customenum.TokenEnum;
import xyz.dowob.filemanagement.data.user.dto.AuthRequestDTO;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.repostiory.ServerCsrfToken.CustomServerCsrfTokenRepository;
import xyz.dowob.filemanagement.repostiory.UserRepository;
import xyz.dowob.filemanagement.service.serviceInterface.TokenService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@DisplayName("AuthorizationServiceImpl 邏輯處理測試")
@ExtendWith(MockitoExtension.class)
class AuthorizationServiceImplTest {

    @Mock
    private UserRepository mockUserRepository;
    @Mock
    private PasswordEncoder mockPasswordEncoder;
    @Mock
    private TokenService mockTokenService;
    @Mock
    private CsrfTokenRepositoryStrategy mockCsrfTokenRepository;
    @Mock
    private CustomServerCsrfTokenRepository customServerCsrfTokenRepository;
    @Mock
    private ServerWebExchange mockServerWebExchange;
    private AuthorizationServiceImpl authorizationServiceImplUnderTest;

    @BeforeEach
    void setUp() {
        authorizationServiceImplUnderTest = new AuthorizationServiceImpl(mockUserRepository,
                                                                         mockPasswordEncoder,
                                                                         mockTokenService,
                                                                         mockCsrfTokenRepository
        );
    }


    @Test
    @DisplayName("授權成功：帳號與密碼正確 - 回傳 JWT Token - 預期回傳 JWT Token")
    void testAuthenticateWithRequest_ValidCredentials_ReturnsJwtToken() {
        AuthRequestDTO authRequestDTO = new AuthRequestDTO("testuser", "password123");
        User user = new User();
        user.setUsername("testuser");
        user.setPassword("encodedPassword");
        String jwtToken = "jwt.token.value";

        when(mockUserRepository.findByUsername("testuser")).thenReturn(Mono.just(user));
        when(mockPasswordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(mockTokenService.generateToken(user, TokenEnum.JWT_AUTHORIZATION_TOKEN)).thenReturn(Mono.just(jwtToken));

        Mono<String> result = authorizationServiceImplUnderTest.authenticate(authRequestDTO, mockServerWebExchange);

        StepVerifier.create(result).expectNext(jwtToken).verifyComplete();
    }

    @Test
    @DisplayName("授權成功：即使沒有 WebExchange，帳密正確 - 回傳 JWT Token - 預期回傳 JWT Token")
    void testAuthenticateWithRequest_NullRequest_ValidCredentials_ReturnsJwtToken() {
        AuthRequestDTO authRequestDTO = new AuthRequestDTO("testuser", "password123");
        User user = new User();
        user.setUsername("testuser");
        user.setPassword("encodedPassword");
        String jwtToken = "jwt.token.value";

        when(mockUserRepository.findByUsername("testuser")).thenReturn(Mono.just(user));
        when(mockPasswordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(mockTokenService.generateToken(user, TokenEnum.JWT_AUTHORIZATION_TOKEN)).thenReturn(Mono.just(jwtToken));

        Mono<String> result = authorizationServiceImplUnderTest.authenticate(authRequestDTO, null);

        StepVerifier.create(result).expectNext(jwtToken).verifyComplete();
    }

    @Test
    @DisplayName("授權失敗：找不到使用者帳號 - 拋出 ValidationException (USERNAME_OR_PASSWORD_ERROR) - 預期拋出 USERNAME_OR_PASSWORD_ERROR")
    void testAuthenticateWithRequest_UsernameNotFound_ThrowsValidationException() {
        AuthRequestDTO authRequestDTO = new AuthRequestDTO("testuser", "password123");

        when(mockUserRepository.findByUsername("testuser")).thenReturn(Mono.empty());

        Mono<String> result = authorizationServiceImplUnderTest.authenticate(authRequestDTO, mockServerWebExchange);

        StepVerifier
                .create(result)
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.USERNAME_OR_PASSWORD_ERROR)
                .verify();
    }

    @Test
    @DisplayName("授權失敗：密碼錯誤 - 拋出 ValidationException (USERNAME_OR_PASSWORD_ERROR) - 預期拋出 USERNAME_OR_PASSWORD_ERROR")
    void testAuthenticateWithRequest_WrongPassword_ThrowsValidationException() {
        AuthRequestDTO authRequestDTO = new AuthRequestDTO("testuser", "wrongpassword");
        User user = new User();
        user.setUsername("testuser");
        user.setPassword("encodedPassword");

        when(mockUserRepository.findByUsername("testuser")).thenReturn(Mono.just(user));
        when(mockPasswordEncoder.matches("wrongpassword", "encodedPassword")).thenReturn(false);

        Mono<String> result = authorizationServiceImplUnderTest.authenticate(authRequestDTO, mockServerWebExchange);

        StepVerifier
                .create(result)
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.USERNAME_OR_PASSWORD_ERROR)
                .verify();
    }

    @Test
    @DisplayName("授權失敗：資料庫錯誤 - 拋出 RuntimeException - 預期拋出 RuntimeException")
    void testAuthenticateWithRequest_RepositoryError_PropagatesException() {
        AuthRequestDTO authRequestDTO = new AuthRequestDTO("testuser", "password123");

        when(mockUserRepository.findByUsername("testuser")).thenReturn(Mono.error(new RuntimeException("Database error")));

        Mono<String> result = authorizationServiceImplUnderTest.authenticate(authRequestDTO, mockServerWebExchange);

        StepVerifier
                .create(result)
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException && throwable.getMessage().equals("Database error"))
                .verify();
    }

    @Test
    @DisplayName("授權失敗：帳號為空字串 - 拋出 ValidationException (USERNAME_OR_PASSWORD_ERROR) - 預期拋出 USERNAME_OR_PASSWORD_ERROR")
    void testAuthenticateWithRequest_EmptyUsername_ThrowsValidationException() {
        AuthRequestDTO authRequestDTO = new AuthRequestDTO("", "password123");

        when(mockUserRepository.findByUsername("")).thenReturn(Mono.empty());

        Mono<String> result = authorizationServiceImplUnderTest.authenticate(authRequestDTO, mockServerWebExchange);

        StepVerifier
                .create(result)
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.USERNAME_OR_PASSWORD_ERROR)
                .verify();
    }

    @Test
    @DisplayName("授權失敗：密碼為空字串 - 拋出 ValidationException (USERNAME_OR_PASSWORD_ERROR) - 預期拋出 USERNAME_OR_PASSWORD_ERROR")
    void testAuthenticateWithRequest_EmptyPassword_ThrowsValidationException() {
        AuthRequestDTO authRequestDTO = new AuthRequestDTO("testuser", "");
        User user = new User();
        user.setUsername("testuser");
        user.setPassword("encodedPassword");

        when(mockUserRepository.findByUsername("testuser")).thenReturn(Mono.just(user));
        when(mockPasswordEncoder.matches("", "encodedPassword")).thenReturn(false);

        Mono<String> result = authorizationServiceImplUnderTest.authenticate(authRequestDTO, mockServerWebExchange);

        StepVerifier
                .create(result)
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.USERNAME_OR_PASSWORD_ERROR)
                .verify();
    }


    @Test
    @DisplayName("授權成功（無 WebExchange）：帳密正確 - 回傳 JWT Token")
    void testAuthenticateWithoutRequest_ValidCredentials_ReturnsJwtToken() {
        AuthRequestDTO authRequestDTO = new AuthRequestDTO("testuser", "password123");

        User user = new User();
        user.setUsername("testuser");
        user.setPassword("encodedPassword");
        String jwtToken = "jwt.token.value";

        when(mockUserRepository.findByUsername("testuser")).thenReturn(Mono.just(user));
        when(mockPasswordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(mockTokenService.generateToken(user, TokenEnum.JWT_AUTHORIZATION_TOKEN)).thenReturn(Mono.just(jwtToken));

        Mono<String> result = authorizationServiceImplUnderTest.authenticate(authRequestDTO);

        StepVerifier.create(result).expectNext(jwtToken).verifyComplete();
    }

    @Test
    @DisplayName("授權失敗（無 WebExchange）：找不到帳號 - 拋出 ValidationException (USERNAME_OR_PASSWORD_ERROR)")
    void testAuthenticateWithoutRequest_UsernameNotFound_ThrowsValidationException() {
        AuthRequestDTO authRequestDTO = new AuthRequestDTO("testuser", "password123");

        when(mockUserRepository.findByUsername("testuser")).thenReturn(Mono.empty());

        Mono<String> result = authorizationServiceImplUnderTest.authenticate(authRequestDTO);

        StepVerifier
                .create(result)
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.USERNAME_OR_PASSWORD_ERROR)
                .verify();
    }

    @Test
    @DisplayName("授權失敗（無 WebExchange）：密碼錯誤 - 拋出 ValidationException (USERNAME_OR_PASSWORD_ERROR)")
    void testAuthenticateWithoutRequest_WrongPassword_ThrowsValidationException() {
        AuthRequestDTO authRequestDTO = new AuthRequestDTO("testuser", "wrongpassword");

        User user = new User();
        user.setUsername("testuser");
        user.setPassword("encodedPassword");

        when(mockUserRepository.findByUsername("testuser")).thenReturn(Mono.just(user));
        when(mockPasswordEncoder.matches("wrongpassword", "encodedPassword")).thenReturn(false);

        Mono<String> result = authorizationServiceImplUnderTest.authenticate(authRequestDTO);

        StepVerifier
                .create(result)
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.USERNAME_OR_PASSWORD_ERROR)
                .verify();
    }


    @Test
    @DisplayName("CSRF Token：正常請求 - 回傳 CSRF Token")
    void testGetCSRFToken_ValidRequest_ReturnsCsrfToken() {
        CsrfToken csrfToken = new DefaultCsrfToken("csrfTokenHeader", "csrfTokenParameter", "csrf-token-value");
        when(mockCsrfTokenRepository.getCsrfTokenRepository()).thenReturn(customServerCsrfTokenRepository);
        when(mockCsrfTokenRepository.getCsrfTokenRepository().generateToken(mockServerWebExchange)).thenReturn(Mono.just(csrfToken));

        Mono<CsrfToken> result = authorizationServiceImplUnderTest.getCSRFToken(mockServerWebExchange);

        StepVerifier.create(result).expectNext(csrfToken).verifyComplete();
    }

    @Test
    @DisplayName("CSRF Token：即使請求為 null - 回傳 CSRF Token")
    void testGetCSRFToken_NullRequest_ThrowsException() {
        CsrfToken csrfToken = new DefaultCsrfToken("csrfTokenHeader", "csrfTokenParameter", "csrf-token-value");
        when(mockCsrfTokenRepository.getCsrfTokenRepository()).thenReturn(customServerCsrfTokenRepository);
        when(mockCsrfTokenRepository.getCsrfTokenRepository().generateToken(any())).thenReturn(Mono.just(csrfToken));
        Mono<CsrfToken> result = authorizationServiceImplUnderTest.getCSRFToken(null);

        StepVerifier.create(result).expectNext(csrfToken).verifyComplete();
    }
}
