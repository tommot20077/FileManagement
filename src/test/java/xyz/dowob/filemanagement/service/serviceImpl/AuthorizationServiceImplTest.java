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

/**
 * 授權服務實現測試類別。
 * 
 * 測試 AuthorizationServiceImpl 類別的核心功能，包括使用者身份驗證、CSRF 權杖管理
 * 和各種錯誤處理場景。此測試類別驗證服務層的認證邏輯和安全性實現。
 * 
 * <p>測試涵蓋範圍：
 * <ul>
 * <li>有效使用者名稱和密碼的身份驗證</li>
 * <li>無效認證資訊的錯誤處理</li>
 * <li>CSRF 權杖生成和驗證</li>
 * <li>WebFlux 反應式流程的異常處理</li>
 * </ul>
 * 
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see AuthorizationServiceImpl
 * @see ValidationException
 * @see TokenService
 */
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

    /**
     * 測試前置作業。
     * 
     * 初始化 AuthorizationServiceImpl 實例和所需的模擬依賴項。
     * 設定測試環境，確保每個測試方法都有乾淨的起始狀態。
     */
    @BeforeEach
    void setUp() {
        authorizationServiceImplUnderTest = new AuthorizationServiceImpl(mockUserRepository,
                                                                         mockPasswordEncoder,
                                                                         mockTokenService,
                                                                         mockCsrfTokenRepository
        );
    }


    /**
     * 測試有效認證資訊的身份驗證。
     * 
     * 驗證當提供正確的使用者名稱和密碼時，授權服務能成功產生並回傳 JWT 權杖。
     * 此測試確認正常的登入流程能正確執行。
     * 
     * 前置條件：
     * - 使用者存在於資料庫中
     * - 密碼編碼器能正確驗證密碼
     * 
     * 測試步驟：
     * - 準備有效的認證請求資料
     * - 模擬使用者查詢和密碼驗證
     * - 執行身份驗證
     * 
     * 預期結果：
     * - 成功回傳 JWT 權杖
     * - 無拋出例外
     */
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

    /**
     * 測試使用者不存在時的身份驗證失敗處理。
     * 
     * 驗證當提供的使用者名稱在資料庫中不存在時，授權服務能正確處理錯誤
     * 並拋出適當的 ValidationException。
     * 
     * 前置條件：
     * - 使用者名稱在資料庫中不存在
     * 
     * 測試步驟：
     * - 準備不存在的使用者名稱認證請求
     * - 模擬空的使用者查詢結果
     * - 執行身份驗證
     * 
     * 預期結果：
     * - 拋出 ValidationException 並包含 USERNAME_OR_PASSWORD_ERROR 錯誤碼
     */
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

    /**
     * 測試密碼不正確時的身份驗證失敗處理。
     * 
     * 驗證當提供正確使用者名稱但密碼錯誤時，授權服務能正確識別並
     * 拋出適當的驗證例外。
     * 
     * 前置條件：
     * - 使用者存在於資料庫中
     * - 提供的密碼與儲存的密碼不符
     * 
     * 測試步驟：
     * - 準備錯誤密碼的認證請求
     * - 模擬密碼驗證失敗
     * - 執行身份驗證
     * 
     * 預期結果：
     * - 拋出 ValidationException 並包含 USERNAME_OR_PASSWORD_ERROR 錯誤碼
     */
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


    /**
     * 測試 CSRF 權杖的正常生成功能。
     * 
     * 驗證授權服務能成功生成並回傳有效的 CSRF 權杖，
     * 確保跨站請求偽造防護機制正常運作。
     * 
     * 前置條件：
     * - CSRF 權杖倉庫策略已配置
     * - ServerWebExchange 可用
     * 
     * 測試步驟：
     * - 準備有效的 ServerWebExchange
     * - 模擬 CSRF 權杖生成
     * - 執行權杖生成請求
     * 
     * 預期結果：
     * - 成功回傳有效的 CSRF 權杖
     * - 權杖包含必要的標頭和參數資訊
     */
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
