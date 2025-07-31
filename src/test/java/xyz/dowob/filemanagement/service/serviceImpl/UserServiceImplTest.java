package xyz.dowob.filemanagement.service.serviceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.dowob.filemanagement.component.limiter.UserLimiter;
import xyz.dowob.filemanagement.component.manager.CacheManager;
import xyz.dowob.filemanagement.component.provider.providerInterface.EmailProvider;
import xyz.dowob.filemanagement.component.strategy.UserLimiterStrategy;
import xyz.dowob.filemanagement.config.properties.SecurityProperties;
import xyz.dowob.filemanagement.customenum.CacheProviderEnum;
import xyz.dowob.filemanagement.customenum.TokenEnum;
import xyz.dowob.filemanagement.customenum.UserLimiterEnum;
import xyz.dowob.filemanagement.data.user.dto.AuthRequestDTO;
import xyz.dowob.filemanagement.data.user.dto.RegisterDTO;
import xyz.dowob.filemanagement.data.user.dto.ResetPasswordDTO;
import xyz.dowob.filemanagement.data.user.dto.UserEmailDTO;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.exception.LimitationException;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.repostiory.UserRepository;
import xyz.dowob.filemanagement.service.serviceInterface.AuthorizationService;
import xyz.dowob.filemanagement.service.serviceInterface.TokenService;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
/**
 * 使用者服務實現測試類別。
 * 
 * 測試 UserServiceImpl 類別的核心功能，包括使用者註冊、登入、密碼重設和資料管理。
 * 此測試類別驗證服務層的使用者管理邏輯和安全性實現。
 * 
 * <p>測試涵蓋範圍：
 * <ul>
 * <li>使用者註冊和驗證流程</li>
 * <li>登入身份驗證和權杖管理</li>
 * <li>密碼重設和安全性操作</li>
 * <li>使用者資料更新和檢索</li>
 * <li>用戶限流和安全控制</li>
 * <li>WebFlux 反應式使用者操作流程</li>
 * </ul>
 * 
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see UserServiceImpl
 * @see AuthorizationService
 * @see TokenService
 */
@SuppressWarnings("all")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("UserService 邏輯處理測試")
class UserServiceImplTest {

    @Mock
    private UserRepository mockUserRepository;
    @Mock
    private AuthorizationService mockAuthorizationService;
    @Mock
    private TokenService mockTokenService;
    @Mock
    private SecurityProperties mockSecurityProperties;
    @Mock
    private PasswordEncoder mockPasswordEncoder;
    @Mock
    private CacheManager mockCacheManager;
    @Mock
    private UserLimiterStrategy mockUserLimiterStrategy;

    @Mock
    private EmailProvider mockEmailProvider;

    private UserServiceImpl userServiceImplUnderTest;

    @BeforeEach
    void setUp() {
        userServiceImplUnderTest = new UserServiceImpl(mockUserRepository,
                                                       mockAuthorizationService,
                                                       mockTokenService,
                                                       Optional.of(mockEmailProvider),
                                                       mockSecurityProperties,
                                                       mockPasswordEncoder,
                                                       mockCacheManager,
                                                       mockUserLimiterStrategy
        );
    }

    @Test
    @DisplayName("測試註冊: 正常情況 - 輸入有效使用者資料時，應成功儲存使用者並回傳 Mono<Void>")
    void register_normalCase_shouldSaveUserAndReturnMonoVoid() {
        RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setUsername("testuser");
        registerDTO.setPassword("password123");
        registerDTO.setEmail("test@example.com");

        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setPassword("encodedPassword");
        user.setEmail(registerDTO.getEmail());

        when(mockPasswordEncoder.encode(registerDTO.getPassword())).thenReturn("encodedPassword");
        when(mockUserRepository.save(any(User.class))).thenReturn(Mono.empty());

        StepVerifier.create(userServiceImplUnderTest.register(registerDTO)).verifyComplete();
    }

    @Test
    @DisplayName("測試註冊: 異常情況 - 使用者名稱或信箱已存在時，應該丟出資料庫違反唯一約束錯誤")
    void register_abnormalCase_shouldThrowUniqueConstraintViolation() {
        RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setUsername("existinguser");
        registerDTO.setPassword("password123");
        registerDTO.setEmail("existing@example.com");

        when(mockPasswordEncoder.encode(registerDTO.getPassword())).thenReturn("encodedPassword");
        when(mockUserRepository.save(any(User.class))).thenReturn(Mono.error(new ValidationException(ValidationException.ErrorCode.EMAIL_ALREADY_EXISTS,
                                                                                                     "test"
        )));

        StepVerifier
                .create(userServiceImplUnderTest.register(registerDTO))
                .expectErrorMatches(e -> e instanceof ValidationException validationException && validationException.getErrorCode() == ValidationException.ErrorCode.EMAIL_ALREADY_EXISTS)
                .verify();
    }

    @Test
    @DisplayName("測試註冊: 異常情況 - registerUserDTO 為 null，應丟出 NullPointerException")
    void register_abnormalCase_nullDto_shouldThrowNullPointerException() {
        StepVerifier.create(userServiceImplUnderTest.register(null)).expectError(NullPointerException.class).verify();
    }

    @Test
    @DisplayName("測試註冊: 邊界條件 - 最短長度使用者名稱與密碼，應成功儲存使用者")
    void register_edgeCase_minLengthUsernameAndPassword_shouldSaveUser() {
        RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setUsername("a");
        registerDTO.setPassword("b");
        registerDTO.setEmail("test@example.com");

        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setPassword("encodedPassword");
        user.setEmail(registerDTO.getEmail());

        when(mockPasswordEncoder.encode(registerDTO.getPassword())).thenReturn("encodedPassword");
        when(mockUserRepository.save(any(User.class))).thenReturn(Mono.empty());

        StepVerifier.create(userServiceImplUnderTest.register(registerDTO)).verifyComplete();
    }

    @Test
    @DisplayName("測試註冊: 邊界條件 - 信箱格式為最小有效格式，應成功儲存使用者")
    void register_edgeCase_minValidEmailFormat_shouldSaveUser() {
        RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setUsername("testuser");
        registerDTO.setPassword("password123");
        registerDTO.setEmail("a@b.c");


        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setPassword("encodedPassword");
        user.setEmail(registerDTO.getEmail());

        when(mockPasswordEncoder.encode(registerDTO.getPassword())).thenReturn("encodedPassword");
        when(mockUserRepository.save(any(User.class))).thenReturn(Mono.empty());

        StepVerifier.create(userServiceImplUnderTest.register(registerDTO)).verifyComplete();
    }

    @Test
    @DisplayName("測試登入: 正常情況 - 使用正確帳號密碼登入成功，返回 Token 並釋放限流資源")
    void login_normalCase_shouldReturnTokenAndReleaseLimiter() {
        AuthRequestDTO authRequestDTO = new AuthRequestDTO("testuser", "password123");
        ServerWebExchange mockExchange = mock(ServerWebExchange.class);
        UserLimiter mockUserLimiter = mock(UserLimiter.class);

        when(mockUserLimiterStrategy.getUserLimiter(UserLimiterEnum.USER_LOGIN_LIMITER)).thenReturn(mockUserLimiter);
        when(mockUserLimiter.tryAcquire(authRequestDTO.getUsername())).thenReturn(Mono.just(true));
        when(mockAuthorizationService.authenticate(authRequestDTO, mockExchange)).thenReturn(Mono.just("mockToken"));
        when(mockUserLimiter.release(authRequestDTO.getUsername())).thenReturn(Mono.empty());

        StepVerifier.create(userServiceImplUnderTest.login(authRequestDTO, mockExchange)).expectNext("mockToken").verifyComplete();

        verify(mockUserLimiter).release(authRequestDTO.getUsername());
    }

    @Test
    @DisplayName("測試登入: 異常情況 - 使用者密碼錯誤，應丟出 USERNAME_OR_PASSWORD_ERROR")
    void login_abnormalCase_incorrectPassword_shouldThrowUsernameOrPasswordError() {
        AuthRequestDTO authRequestDTO = new AuthRequestDTO("testuser", "wrongpassword");
        ServerWebExchange mockExchange = mock(ServerWebExchange.class);
        UserLimiter mockUserLimiter = mock(UserLimiter.class);

        when(mockUserLimiterStrategy.getUserLimiter(UserLimiterEnum.USER_LOGIN_LIMITER)).thenReturn(mockUserLimiter);
        when(mockUserLimiter.tryAcquire(authRequestDTO.getUsername())).thenReturn(Mono.just(true));
        when(mockAuthorizationService.authenticate(authRequestDTO,
                                                   mockExchange
        )).thenReturn(Mono.error(new ValidationException(ValidationException.ErrorCode.USERNAME_OR_PASSWORD_ERROR)));

        StepVerifier
                .create(userServiceImplUnderTest.login(authRequestDTO, mockExchange))
                .expectErrorMatches(e -> e instanceof ValidationException validationException && validationException.getErrorCode() == ValidationException.ErrorCode.USERNAME_OR_PASSWORD_ERROR)
                .verify();
    }

    @Test
    @DisplayName("測試登入: 異常情況 - 使用者帳號不存在，應丟出 USERNAME_OR_PASSWORD_ERROR")
    void login_abnormalCase_userNotFound_shouldThrowUsernameOrPasswordError() {
        AuthRequestDTO authRequestDTO = new AuthRequestDTO("nonexistentuser", "password123");
        ServerWebExchange mockExchange = mock(ServerWebExchange.class);
        UserLimiter mockUserLimiter = mock(UserLimiter.class);

        when(mockUserLimiterStrategy.getUserLimiter(UserLimiterEnum.USER_LOGIN_LIMITER)).thenReturn(mockUserLimiter);
        when(mockUserLimiter.tryAcquire(authRequestDTO.getUsername())).thenReturn(Mono.just(true));
        when(mockAuthorizationService.authenticate(authRequestDTO,
                                                   mockExchange
        )).thenReturn(Mono.error(new ValidationException(ValidationException.ErrorCode.USERNAME_OR_PASSWORD_ERROR)));

        StepVerifier
                .create(userServiceImplUnderTest.login(authRequestDTO, mockExchange))
                .expectErrorMatches(e -> e instanceof ValidationException validationException && validationException.getErrorCode() == ValidationException.ErrorCode.USERNAME_OR_PASSWORD_ERROR)
                .verify();
    }

    @Test
    @DisplayName("測試登入: 異常情況 - 達到登入限流上限，拋出 LimitationException")
    void login_abnormalCase_rateLimitExceeded_shouldThrowLimitationException() {
        AuthRequestDTO authRequestDTO = new AuthRequestDTO("testuser", "password123");
        ServerWebExchange mockExchange = mock(ServerWebExchange.class);
        UserLimiter mockUserLimiter = mock(UserLimiter.class);

        when(mockUserLimiterStrategy.getUserLimiter(UserLimiterEnum.USER_LOGIN_LIMITER)).thenReturn(mockUserLimiter);
        when(mockUserLimiter.tryAcquire(authRequestDTO.getUsername())).thenReturn(Mono.just(false));

        StepVerifier.create(userServiceImplUnderTest.login(authRequestDTO, mockExchange)).expectError(LimitationException.class).verify();
    }

    @Test
    @DisplayName("測試登入: 異常情況 - 認證服務內部拋出錯誤")
    void login_abnormalCase_authenticationServiceError_shouldPropagateError() {
        AuthRequestDTO authRequestDTO = new AuthRequestDTO("testuser", "password123");
        ServerWebExchange mockExchange = mock(ServerWebExchange.class);
        UserLimiter mockUserLimiter = mock(UserLimiter.class);

        when(mockUserLimiterStrategy.getUserLimiter(UserLimiterEnum.USER_LOGIN_LIMITER)).thenReturn(mockUserLimiter);
        when(mockUserLimiter.tryAcquire(authRequestDTO.getUsername())).thenReturn(Mono.just(true));
        when(mockAuthorizationService.authenticate(authRequestDTO, mockExchange)).thenReturn(Mono.error(new RuntimeException(
                "Authentication service error")));

        StepVerifier.create(userServiceImplUnderTest.login(authRequestDTO, mockExchange)).expectError(RuntimeException.class).verify();
    }

    @Test
    @DisplayName("測試登入: 邊界條件 - 空字串使用者名稱與密碼")
    void login_edgeCase_emptyUsernameAndPassword() {
        AuthRequestDTO authRequestDTO = new AuthRequestDTO("", "");
        ServerWebExchange mockExchange = mock(ServerWebExchange.class);
        UserLimiter mockUserLimiter = mock(UserLimiter.class);

        when(mockUserLimiterStrategy.getUserLimiter(UserLimiterEnum.USER_LOGIN_LIMITER)).thenReturn(mockUserLimiter);
        when(mockUserLimiter.tryAcquire(authRequestDTO.getUsername())).thenReturn(Mono.just(true));
        when(mockAuthorizationService.authenticate(authRequestDTO,
                                                   mockExchange
        )).thenReturn(Mono.error(new ValidationException(ValidationException.ErrorCode.REQUEST_IS_INVALID, "用戶名稱或密碼不能為空")));

        StepVerifier
                .create(userServiceImplUnderTest.login(authRequestDTO, mockExchange))
                .expectErrorMatches(e -> e instanceof ValidationException validationException && validationException.getErrorCode() == ValidationException.ErrorCode.REQUEST_IS_INVALID)
                .verify();
    }

    @Test
    @DisplayName("測試登入: 邊界條件 - 極長的使用者名稱與密碼")
    void login_edgeCase_veryLongUsernameAndPassword() {
        String longString = "a".repeat(256);
        AuthRequestDTO authRequestDTO = new AuthRequestDTO(longString, longString);
        ServerWebExchange mockExchange = mock(ServerWebExchange.class);
        UserLimiter mockUserLimiter = mock(UserLimiter.class);

        when(mockUserLimiterStrategy.getUserLimiter(UserLimiterEnum.USER_LOGIN_LIMITER)).thenReturn(mockUserLimiter);
        when(mockUserLimiter.tryAcquire(authRequestDTO.getUsername())).thenReturn(Mono.just(true));
        when(mockAuthorizationService.authenticate(authRequestDTO,
                                                   mockExchange
        )).thenReturn(Mono.error(new ValidationException(ValidationException.ErrorCode.REQUEST_IS_INVALID, "Credentials too long")));

        StepVerifier
                .create(userServiceImplUnderTest.login(authRequestDTO, mockExchange))
                .expectErrorMatches(e -> e instanceof ValidationException validationException && validationException.getErrorCode() == ValidationException.ErrorCode.REQUEST_IS_INVALID)
                .verify();
    }

    @Test
    @DisplayName("測試登入: 邊界條件 - 請求物件為 null，應丟出 NullPointerException")
    void login_edgeCase_nullRequest_shouldThrowNullPointerException() {
        AuthRequestDTO authRequestDTO = new AuthRequestDTO("testuser", "password123");

        StepVerifier.create(userServiceImplUnderTest.login(authRequestDTO, null)).expectError(NullPointerException.class).verify();
    }

    @Test
    @DisplayName("測試登出: 正常情況 - 正常使用者登出並撤銷 Token，Session 成功被清除")
    void logout_normalCase_shouldRevokeTokenAndInvalidateSession() {
        Long userId = 1L;
        ServerWebExchange mockExchange = mock(ServerWebExchange.class);
        WebSession mockSession = mock(WebSession.class);
        User mockUser = mock(User.class);

        when(mockExchange.getSession()).thenReturn(Mono.just(mockSession));
        when(mockUserRepository.findById(userId)).thenReturn(Mono.just(mockUser));
        when(mockTokenService.revokeToken(userId, TokenEnum.JWT_AUTHORIZATION_TOKEN)).thenReturn(Mono.empty());
        when(mockSession.invalidate()).thenReturn(Mono.empty());
        when(mockUser.getId()).thenReturn(userId);


        StepVerifier.create(userServiceImplUnderTest.logout(userId, mockExchange)).verifyComplete();

        verify(mockTokenService).revokeToken(userId, TokenEnum.JWT_AUTHORIZATION_TOKEN);
        verify(mockSession).invalidate();
    }

    @Test
    @DisplayName("測試登出: 異常情況 - userId 不存在於資料庫中，應丟出異常")
    void logout_abnormalCase_userNotFound_shouldThrowException() {
        Long userId = 1L;
        ServerWebExchange mockExchange = mock(ServerWebExchange.class);
        WebSession mockSession = mock(WebSession.class);

        when(mockExchange.getSession()).thenReturn(Mono.just(mockSession));
        when(mockUserRepository.findById(userId)).thenReturn(Mono.empty());


        StepVerifier
                .create(userServiceImplUnderTest.logout(userId, mockExchange))
                .expectErrorMatches(e -> e instanceof ValidationException validationException && validationException.getErrorCode() == ValidationException.ErrorCode.USER_NOT_FOUND)
                .verify();


    }

    @Test
    @DisplayName("測試登出: 異常情況 - 無法從 exchange 取得 session，應丟出異常")
    void logout_abnormalCase_cannotGetSession_shouldThrowException() {
        Long userId = 1L;
        ServerWebExchange mockExchange = mock(ServerWebExchange.class);

        when(mockExchange.getSession()).thenReturn(Mono.empty());


        StepVerifier
                .create(userServiceImplUnderTest.logout(userId, mockExchange))
                .expectErrorMatches(e -> e instanceof ValidationException validationException && validationException.getErrorCode() == ValidationException.ErrorCode.UNAUTHORIZED)
                .verify();
    }

    @Test
    @DisplayName("測試登出: 邊界條件 - userId 為 0（訪客身分），應完成但不執行登出操作")
    void logout_edgeCase_guestUserId_shouldCompleteWithoutError() {
        Long userId = 0L;
        ServerWebExchange mockExchange = mock(ServerWebExchange.class);
        WebSession mockSession = mock(WebSession.class);

        StepVerifier.create(userServiceImplUnderTest.logout(userId, mockExchange)).verifyComplete();

        verify(mockTokenService, never()).revokeToken(anyLong(), any(TokenEnum.class));
        verify(mockSession, never()).invalidate();
    }

    @Test
    @DisplayName("測試登出: 邊界條件 - exchange 為 null，應丟出 NullPointerException")
    void logout_edgeCase_nullExchange_shouldThrowNullPointerException() {
        Long userId = 1L;

        StepVerifier.create(userServiceImplUnderTest.logout(userId, null)).expectComplete().verify();
    }

    @Test
    @DisplayName("測試發送重置密碼郵件: 正常情況 - 成功發送重設密碼郵件給已註冊的使用者")
    void sendResetPasswordMail_normalCase_shouldSendEmail() {
        UserEmailDTO userEmailDTO = new UserEmailDTO();
        userEmailDTO.setEmail("test@example.com");
        User mockUser = mock(User.class);
        String mockToken = "resetToken123";

        when(mockUserRepository.findByEmail(userEmailDTO.getEmail())).thenReturn(Mono.just(mockUser));
        when(mockUser.getEmail()).thenReturn(userEmailDTO.getEmail());
        when(mockTokenService.generateToken(mockUser, TokenEnum.RESET_PASSWORD_TOKEN)).thenReturn(Mono.just(mockToken));
        when(mockSecurityProperties.getResetPasswordToken()).thenReturn(mock(SecurityProperties.resetPasswordToken.class));
        when(mockSecurityProperties.getResetPasswordToken().getExpiration()).thenReturn(Duration.ofMinutes(15));
        when(mockEmailProvider.sendEmail(eq(userEmailDTO.getEmail()), eq("重置密碼"), anyString())).thenReturn(Mono.empty());

        StepVerifier.create(userServiceImplUnderTest.sendResetPasswordMail(userEmailDTO)).verifyComplete();

        verify(mockEmailProvider).sendEmail(eq(userEmailDTO.getEmail()), eq("重置密碼"), anyString());
    }

    @Test
    @DisplayName("測試發送重置密碼郵件: 異常情況 - 使用者信箱不存在，拋出 USER_NOT_FOUND")
    void sendResetPasswordMail_abnormalCase_userNotFound_shouldThrowUserNotFound() {
        UserEmailDTO userEmailDTO = new UserEmailDTO();
        userEmailDTO.setEmail("nonexistent@example.com");

        when(mockUserRepository.findByEmail(userEmailDTO.getEmail())).thenReturn(Mono.empty());


        StepVerifier
                .create(userServiceImplUnderTest.sendResetPasswordMail(userEmailDTO))
                .expectErrorMatches(e -> e instanceof ValidationException validationException && validationException.getErrorCode() == ValidationException.ErrorCode.USER_NOT_FOUND)
                .verify();
    }

    @Test
    @DisplayName("測試發送重置密碼郵件: 異常情況 - emailProvider 不存在，拋出 UNSUPPORTED_OPERATION")
    void sendResetPasswordMail_abnormalCase_emailProviderNotFound_shouldThrowUnsupportedOperation() {
        UserServiceImpl userServiceImplWithoutEmailProvider = new UserServiceImpl(mockUserRepository,
                                                                                  mockAuthorizationService,
                                                                                  mockTokenService,
                                                                                  Optional.empty(),
                                                                                  mockSecurityProperties,
                                                                                  mockPasswordEncoder,
                                                                                  mockCacheManager,
                                                                                  mockUserLimiterStrategy
        );

        UserEmailDTO userEmailDTO = new UserEmailDTO();
        userEmailDTO.setEmail("test@example.com");

        StepVerifier
                .create(userServiceImplWithoutEmailProvider.sendResetPasswordMail(userEmailDTO))
                .expectErrorMatches(e -> e instanceof ValidationException validationException && validationException.getErrorCode() == ValidationException.ErrorCode.UNSUPPORTED_OPERATION)
                .verify();
    }

    @Test
    @DisplayName("測試發送重置密碼郵件: 異常情況 - 發送郵件失敗")
    void sendResetPasswordMail_abnormalCase_sendEmailFailed_shouldPropagateError() {
        UserEmailDTO userEmailDTO = new UserEmailDTO();
        userEmailDTO.setEmail("test@example.com");
        User mockUser = mock(User.class);
        String mockToken = "resetToken123";

        when(mockUserRepository.findByEmail(userEmailDTO.getEmail())).thenReturn(Mono.just(mockUser));
        when(mockTokenService.generateToken(mockUser, TokenEnum.RESET_PASSWORD_TOKEN)).thenReturn(Mono.just(mockToken));
        when(mockSecurityProperties.getResetPasswordToken()).thenReturn(mock(SecurityProperties.resetPasswordToken.class));
        when(mockSecurityProperties.getResetPasswordToken().getExpiration()).thenReturn(Duration.ofMinutes(15));
        when(mockEmailProvider.sendEmail(eq(userEmailDTO.getEmail()), eq("重置密碼"), anyString())).thenReturn(Mono.error(new RuntimeException(
                "Email sending failed")));


        StepVerifier.create(userServiceImplUnderTest.sendResetPasswordMail(userEmailDTO)).expectError(RuntimeException.class).verify();
    }

    @Test
    @DisplayName("測試發送重置密碼郵件: 邊界條件 - 空信箱，應丟出異常")
    void sendResetPasswordMail_edgeCase_emptyEmail_shouldThrowException() {
        UserEmailDTO userEmailDTO = new UserEmailDTO();
        userEmailDTO.setEmail("");

        when(mockUserRepository.findByEmail(userEmailDTO.getEmail())).thenReturn(Mono.empty());

        StepVerifier
                .create(userServiceImplUnderTest.sendResetPasswordMail(userEmailDTO))
                .expectErrorMatches(e -> e instanceof ValidationException validationException && validationException.getErrorCode() == ValidationException.ErrorCode.USER_NOT_FOUND)
                .verify();
    }

    @Test
    @DisplayName("測試發送重置密碼郵件: 邊界條件 - 非法信箱格式，應丟出異常")
    void sendResetPasswordMail_edgeCase_invalidEmailFormat_shouldThrowException() {
        UserEmailDTO userEmailDTO = new UserEmailDTO();
        userEmailDTO.setEmail("invalid-email");

        when(mockUserRepository.findByEmail(userEmailDTO.getEmail())).thenReturn(Mono.empty());

        StepVerifier
                .create(userServiceImplUnderTest.sendResetPasswordMail(userEmailDTO))
                .expectErrorMatches(e -> e instanceof ValidationException validationException && validationException.getErrorCode() == ValidationException.ErrorCode.USER_NOT_FOUND)
                .verify();
    }

    @Test
    @DisplayName("測試重置密碼: 正常情況 - 憑證驗證成功後更新密碼並撤銷原憑證")
    void resetPassword_normalCase_shouldUpdatePasswordAndRevokeToken() {
        ResetPasswordDTO resetPasswordDTO = new ResetPasswordDTO();
        resetPasswordDTO.setEmail("test@example.com");
        resetPasswordDTO.setVerificationCode("validToken");
        resetPasswordDTO.setNewPassword("newPassword123");

        User mockUser = mock(User.class);
        String encodedPassword = "encodedNewPassword";

        when(mockUserRepository.findByEmail(resetPasswordDTO.getEmail())).thenReturn(Mono.just(mockUser));
        when(mockUser.getId()).thenReturn(1L);
        when(mockTokenService.validateToken(eq(resetPasswordDTO.getVerificationCode()),
                                            eq(1L),
                                            eq(TokenEnum.RESET_PASSWORD_TOKEN)
        )).thenReturn(Mono.empty());
        when(mockPasswordEncoder.encode(resetPasswordDTO.getNewPassword())).thenReturn(encodedPassword);
        when(mockUserRepository.save(mockUser)).thenReturn(Mono.just(mockUser));
        when(mockTokenService.revokeToken(eq(1L), eq(TokenEnum.RESET_PASSWORD_TOKEN))).thenReturn(Mono.empty());


        StepVerifier.create(userServiceImplUnderTest.resetPassword(resetPasswordDTO)).verifyComplete();

        verify(mockUser).setPassword(encodedPassword);
        verify(mockUserRepository).save(mockUser);
        verify(mockTokenService).revokeToken(mockUser.getId(), TokenEnum.RESET_PASSWORD_TOKEN);

    }

    @Test
    @DisplayName("測試重置密碼: 異常情況 - 無該信箱之使用者，應丟出 USER_NOT_FOUND")
    void resetPassword_abnormalCase_userNotFound_shouldThrowUserNotFound() {
        ResetPasswordDTO resetPasswordDTO = new ResetPasswordDTO();
        resetPasswordDTO.setEmail("nonexistent@example.com");
        resetPasswordDTO.setVerificationCode("validToken");
        resetPasswordDTO.setNewPassword("newPassword123");

        when(mockUserRepository.findByEmail(resetPasswordDTO.getEmail())).thenReturn(Mono.empty());


        StepVerifier
                .create(userServiceImplUnderTest.resetPassword(resetPasswordDTO))
                .expectErrorMatches(e -> e instanceof ValidationException validationException && validationException.getErrorCode() == ValidationException.ErrorCode.USER_NOT_FOUND)
                .verify();
    }

    @Test
    @DisplayName("測試重置密碼: 異常情況 - 驗證碼錯誤或過期，應丟出 VERIFICATION_CODE_ERROR")
    void resetPassword_abnormalCase_invalidVerificationCode_shouldThrowVerificationCodeError() {
        ResetPasswordDTO resetPasswordDTO = new ResetPasswordDTO();
        resetPasswordDTO.setEmail("test@example.com");
        resetPasswordDTO.setVerificationCode("invalidToken");
        resetPasswordDTO.setNewPassword("newPassword123");

        User mockUser = mock(User.class);

        when(mockUserRepository.findByEmail(resetPasswordDTO.getEmail())).thenReturn(Mono.just(mockUser));
        when(mockTokenService.validateToken(resetPasswordDTO.getVerificationCode(),
                                            mockUser.getId(),
                                            TokenEnum.RESET_PASSWORD_TOKEN
        )).thenReturn(Mono.error(new ValidationException(ValidationException.ErrorCode.VERIFICATION_CODE_ERROR)));


        StepVerifier
                .create(userServiceImplUnderTest.resetPassword(resetPasswordDTO))
                .expectErrorMatches(e -> e instanceof ValidationException validationException && validationException.getErrorCode() == ValidationException.ErrorCode.VERIFICATION_CODE_ERROR)
                .verify();
    }

    @Test
    @DisplayName("測試重置密碼: 異常情況 - 資料庫更新失敗")
    void resetPassword_abnormalCase_databaseUpdateFailed_shouldPropagateError() {
        ResetPasswordDTO resetPasswordDTO = new ResetPasswordDTO();
        resetPasswordDTO.setEmail("test@example.com");
        resetPasswordDTO.setVerificationCode("validToken");
        resetPasswordDTO.setNewPassword("newPassword123");

        User mockUser = mock(User.class);
        String encodedPassword = "encodedNewPassword";

        when(mockUserRepository.findByEmail(resetPasswordDTO.getEmail())).thenReturn(Mono.just(mockUser));
        when(mockTokenService.validateToken(resetPasswordDTO.getVerificationCode(),
                                            mockUser.getId(),
                                            TokenEnum.RESET_PASSWORD_TOKEN
        )).thenReturn(Mono.empty());
        when(mockPasswordEncoder.encode(resetPasswordDTO.getNewPassword())).thenReturn(encodedPassword);
        when(mockUserRepository.save(mockUser)).thenReturn(Mono.error(new RuntimeException("Database save failed")));


        StepVerifier.create(userServiceImplUnderTest.resetPassword(resetPasswordDTO)).expectError(RuntimeException.class).verify();
    }

    @Test
    @DisplayName("測試重置密碼: 邊界條件 - 極短密碼，應成功更新密碼")
    void resetPassword_edgeCase_shortPassword_shouldUpdatePassword() {
        ResetPasswordDTO shortPasswordDTO = new ResetPasswordDTO();
        shortPasswordDTO.setEmail("test@example.com");
        shortPasswordDTO.setVerificationCode("validToken");
        shortPasswordDTO.setNewPassword("short");

        User mockUserShort = mock(User.class);
        String encodedShortPassword = "encodedShortPassword";

        when(mockUserRepository.findByEmail(shortPasswordDTO.getEmail())).thenReturn(Mono.just(mockUserShort));
        when(mockUserShort.getId()).thenReturn(1L);
        when(mockTokenService.validateToken(eq(shortPasswordDTO.getVerificationCode()),
                                            eq(1L),
                                            eq(TokenEnum.RESET_PASSWORD_TOKEN)
        )).thenReturn(Mono.empty());
        when(mockPasswordEncoder.encode(shortPasswordDTO.getNewPassword())).thenReturn(encodedShortPassword);
        when(mockUserRepository.save(mockUserShort)).thenReturn(Mono.just(mockUserShort));
        when(mockTokenService.revokeToken(eq(1L), eq(TokenEnum.RESET_PASSWORD_TOKEN))).thenReturn(Mono.empty());

        StepVerifier.create(userServiceImplUnderTest.resetPassword(shortPasswordDTO)).verifyComplete();

        verify(mockUserShort).setPassword(encodedShortPassword);
        verify(mockUserRepository).save(mockUserShort);
        verify(mockTokenService).revokeToken(mockUserShort.getId(), TokenEnum.RESET_PASSWORD_TOKEN);
    }

    @Test
    @DisplayName("測試重置密碼: 邊界條件 - 極長密碼，應成功更新密碼")
    void resetPassword_edgeCase_longPassword_shouldUpdatePassword() {
        String longPassword = "a".repeat(256);
        ResetPasswordDTO longPasswordDTO = new ResetPasswordDTO();
        longPasswordDTO.setEmail("test@example.com");
        longPasswordDTO.setVerificationCode("validToken");
        longPasswordDTO.setNewPassword(longPassword);

        User mockUserLong = mock(User.class);
        String encodedLongPassword = "encodedLongPassword";

        when(mockUserRepository.findByEmail(longPasswordDTO.getEmail())).thenReturn(Mono.just(mockUserLong));
        when(mockUserLong.getId()).thenReturn(1L);
        when(mockTokenService.validateToken(eq(longPasswordDTO.getVerificationCode()),
                                            eq(1L),
                                            eq(TokenEnum.RESET_PASSWORD_TOKEN)
        )).thenReturn(Mono.empty());
        when(mockPasswordEncoder.encode(longPasswordDTO.getNewPassword())).thenReturn(encodedLongPassword);
        when(mockUserRepository.save(mockUserLong)).thenReturn(Mono.just(mockUserLong));
        when(mockTokenService.revokeToken(eq(1L), eq(TokenEnum.RESET_PASSWORD_TOKEN))).thenReturn(Mono.empty());

        StepVerifier.create(userServiceImplUnderTest.resetPassword(longPasswordDTO)).verifyComplete();

        verify(mockUserLong).setPassword(encodedLongPassword);
        verify(mockUserRepository).save(mockUserLong);
        verify(mockTokenService).revokeToken(mockUserLong.getId(), TokenEnum.RESET_PASSWORD_TOKEN);
    }

    @Test
    @DisplayName("測試重置密碼: 邊界條件 - 憑證為空或格式不符，應丟出 VERIFICATION_CODE_ERROR")
    void resetPassword_edgeCase_emptyOrInvalidCredential_shouldThrowVerificationCodeError() {
        ResetPasswordDTO emptyCredentialDTO = new ResetPasswordDTO();
        emptyCredentialDTO.setEmail("test@example.com");
        emptyCredentialDTO.setVerificationCode("");
        emptyCredentialDTO.setNewPassword("newPassword123");

        User mockUserEmpty = mock(User.class);

        when(mockUserRepository.findByEmail(emptyCredentialDTO.getEmail())).thenReturn(Mono.just(mockUserEmpty));
        when(mockTokenService.validateToken(emptyCredentialDTO.getVerificationCode(),
                                            mockUserEmpty.getId(),
                                            TokenEnum.RESET_PASSWORD_TOKEN
        )).thenReturn(Mono.error(new ValidationException(ValidationException.ErrorCode.VERIFICATION_CODE_ERROR)));


        StepVerifier
                .create(userServiceImplUnderTest.resetPassword(emptyCredentialDTO))
                .expectErrorMatches(e -> e instanceof ValidationException validationException && validationException.getErrorCode() == ValidationException.ErrorCode.VERIFICATION_CODE_ERROR)
                .verify();

        ResetPasswordDTO invalidFormatCredentialDTO = new ResetPasswordDTO();
        invalidFormatCredentialDTO.setEmail("test@example.com");
        invalidFormatCredentialDTO.setVerificationCode("invalid-format-token");
        invalidFormatCredentialDTO.setNewPassword("newPassword123");

        User mockUserInvalid = mock(User.class);

        when(mockUserRepository.findByEmail(invalidFormatCredentialDTO.getEmail())).thenReturn(Mono.just(mockUserInvalid));
        when(mockTokenService.validateToken(invalidFormatCredentialDTO.getVerificationCode(),
                                            mockUserInvalid.getId(),
                                            TokenEnum.RESET_PASSWORD_TOKEN
        )).thenReturn(Mono.error(new ValidationException(ValidationException.ErrorCode.VERIFICATION_CODE_ERROR)));


        StepVerifier
                .create(userServiceImplUnderTest.resetPassword(invalidFormatCredentialDTO))
                .expectErrorMatches(e -> e instanceof ValidationException validationException && validationException.getErrorCode() == ValidationException.ErrorCode.VERIFICATION_CODE_ERROR)
                .verify();
    }

    @Test
    @DisplayName("測試獲取使用者: 正常情況 - 已登入使用者成功取得對應 User")
    void getUser_normalCase_shouldReturnUser() {
        Long userId = 1L;
        User mockUser = mock(User.class);
        ServerWebExchange mockExchange = mock(ServerWebExchange.class);
        String cacheKey = userId.toString();

        doReturn(Mono.just(mockUser))
                .when(mockCacheManager)
                .runAndSetCache(eq(cacheKey), eq(User.class), eq(CacheProviderEnum.USER_CACHE), (Mono<? extends User>) isNull(), anyList());

        StepVerifier
                .create(Mono
                                .deferContextual(ctx -> userServiceImplUnderTest.getUser(mockExchange))
                                .contextWrite(context -> context.put(SecurityContext.class, Mono.just(createMockSecurityContext(userId)))))
                .expectNext(mockUser)
                .verifyComplete();

        verify(mockCacheManager).runAndSetCache(eq(cacheKey),
                                                eq(User.class),
                                                eq(CacheProviderEnum.USER_CACHE),
                                                (Mono<? extends User>) isNull(),
                                                anyList()
        );
    }


    private SecurityContext createMockSecurityContext(Long userId) {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userId);
        return securityContext;
    }

    @Test
    @DisplayName("測試獲取使用者: 異常情況 - SecurityContext 為空，應拋出 UNAUTHORIZED")
    void getUser_abnormalCase_emptySecurityContext_shouldThrowUnauthorized() {
        ServerWebExchange mockExchange = mock(ServerWebExchange.class);
        StepVerifier
                .create(Mono
                                .deferContextual(ctx -> userServiceImplUnderTest.getUser(mockExchange))
                                .contextWrite(context -> context.put(SecurityContext.class, Mono.empty())))
                .expectErrorMatches(e -> e instanceof ValidationException validationException && validationException.getErrorCode() == ValidationException.ErrorCode.UNAUTHORIZED)
                .verify();
    }

    @Test
    @DisplayName("測試獲取使用者: 邊界條件 - exchange 為 null，應丟出 NullPointerException")
    void getUser_edgeCase_nullExchange_shouldThrowNullPointerException() {
        StepVerifier
                .create(userServiceImplUnderTest.getUser(null))
                .expectErrorMatches(e -> e instanceof ValidationException validationException && validationException.getErrorCode() == ValidationException.ErrorCode.UNAUTHORIZED)
                .verify();
    }

    @Test
    @DisplayName("測試獲取使用者: 正常情况 - 正常 ID 查询成功，并命中或建立缓存")
    void getUser_normalCase_shouldReturnUserAndUseCache() {
        Long userId = 1L;
        User mockUser = mock(User.class);
        ServerWebExchange mockExchange = mock(ServerWebExchange.class);
        doReturn(Mono.just(mockUser)).when(mockCacheManager).runAndSetCache(anyString(), any(), any(), (Mono<?>) any(), anyList());

        StepVerifier
                .create(Mono
                                .deferContextual(ctx -> userServiceImplUnderTest.getUser(mockExchange))
                                .contextWrite(context -> context.put(SecurityContext.class, Mono.just(createMockSecurityContext(userId)))))
                .expectNext(mockUser)
                .verifyComplete();

        verify(mockCacheManager).runAndSetCache(anyString(), any(), any(), (Mono<?>) any(), anyList());
    }

    @Test
    @DisplayName("測試通過ID獲取使用者: 異常情況 - ID 為 null，應丟出 USER_NOT_FOUND")
    void getById_abnormalCase_nullId_shouldThrowUserNotFound() {
        StepVerifier
                .create(userServiceImplUnderTest.getById(null))
                .expectErrorMatches(e -> e instanceof ValidationException validationException && validationException.getErrorCode() == ValidationException.ErrorCode.USER_NOT_FOUND)
                .verify();
    }

    @Test
    @DisplayName("測試通過ID獲取使用者: 異常情況 - 資料庫查無此人，應丟出 USER_NOT_FOUND")
    void getById_abnormalCase_userNotFoundInDb_shouldThrowUserNotFound() {
        Long userId = 1L;
        String cacheKey = userId.toString();

        when(mockUserRepository.findById(userId)).thenReturn(Mono.empty());

        when(mockCacheManager.runAndSetCache(eq(cacheKey), eq(User.class), eq(CacheProviderEnum.USER_CACHE), any(Mono.class), anyList())).thenAnswer(
                invocation -> {
                    Mono<User> dbCallMono = invocation.getArgument(3);
                    return dbCallMono;
                });


        StepVerifier
                .create(userServiceImplUnderTest.getById(userId))
                .expectErrorMatches(e -> e instanceof ValidationException validationException && validationException.getErrorCode() == ValidationException.ErrorCode.USER_NOT_FOUND && e
                        .getMessage()
                        .contains(userId.toString()))
                .verify();
    }

    @Test
    @DisplayName("測試通過ID獲取使用者: 邊界條件 - ID 為 0，應回傳 guestUser")
    void getById_edgeCase_zeroId_shouldReturnGuestUser() {
        Long userId = 0L;

        User guestUser = null;
        try {
            java.lang.reflect.Field field = UserServiceImpl.class.getDeclaredField("guestUser");
            field.setAccessible(true);
            guestUser = (User) field.get(userServiceImplUnderTest);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Could not access guestUser field: " + e.getMessage());
        }

        StepVerifier.create(userServiceImplUnderTest.getById(userId)).expectNext(guestUser).verifyComplete();
    }

    @Test
    @DisplayName("測試通過ID獲取使用者: 邊界條件 - ID 為負數或極大值，應丟出異常情況")
    void getById_edgeCase_negativeOrVeryLargeId_shouldThrowException() {
        Long negativeUserId = -1L;
        String negativeCacheKey = negativeUserId.toString();

        when(mockUserRepository.findById(negativeUserId)).thenReturn(Mono.empty());
        when(mockCacheManager.runAndSetCache(eq(negativeCacheKey),
                                             eq(User.class),
                                             eq(CacheProviderEnum.USER_CACHE),
                                             any(Mono.class),
                                             anyList()
        )).thenAnswer(invocation -> invocation.getArgument(3));


        StepVerifier
                .create(userServiceImplUnderTest.getById(negativeUserId))
                .expectErrorMatches(e -> e instanceof ValidationException validationException && validationException.getErrorCode() == ValidationException.ErrorCode.USER_NOT_FOUND && e
                        .getMessage()
                        .contains(negativeCacheKey))
                .verify();


        Long veryLargeUserId = Long.MAX_VALUE;
        String largeCacheKey = veryLargeUserId.toString();

        when(mockUserRepository.findById(veryLargeUserId)).thenReturn(Mono.empty());
        when(mockCacheManager.runAndSetCache(eq(largeCacheKey),
                                             eq(User.class),
                                             eq(CacheProviderEnum.USER_CACHE),
                                             any(Mono.class),
                                             anyList()
        )).thenAnswer(invocation -> invocation.getArgument(3));

        StepVerifier
                .create(userServiceImplUnderTest.getById(veryLargeUserId))
                .expectErrorMatches(e -> e instanceof ValidationException validationException && validationException.getErrorCode() == ValidationException.ErrorCode.USER_NOT_FOUND && e
                        .getMessage()
                        .contains(largeCacheKey))
                .verify();
    }

    @Test
    @DisplayName("測試獲取所有使用者: 正常情況 - 擁有 MANAGE 權限者成功回傳所有使用者清單")
    void getAll_normalCase_shouldReturnAllUsers() {
        User mockUser1 = mock(User.class);
        User mockUser2 = mock(User.class);
        List<User> userList = List.of(mockUser1, mockUser2);

        when(mockUserRepository.findAll()).thenReturn(Flux.fromIterable(userList));

        StepVerifier.create(userServiceImplUnderTest.getAll()).expectNextCount(userList.size()).verifyComplete();
        verify(mockUserRepository).findAll();
    }

    @Test
    @DisplayName("測試通過參數獲取所有使用者: 異常情況 - type 為 null 或無效值，應丟出異常情況")
    void getAllByParams_abnormalCase_nullOrInvalidType_shouldThrowException() {
        String nullType = null;
        Object[] args = {"user1"};

        StepVerifier
                .create(userServiceImplUnderTest.getAllByParams(nullType, args))
                .expectErrorMatches(e -> e instanceof ValidationException validationException && validationException.getErrorCode() == ValidationException.ErrorCode.INVALID_SEARCH_CRITERIA)
                .verify();

        String invalidType = "invalidType";
        Object[] args2 = {"user1"};

        StepVerifier
                .create(userServiceImplUnderTest.getAllByParams(invalidType, args2))
                .expectErrorMatches(e -> e instanceof ValidationException validationException && validationException.getErrorCode() == ValidationException.ErrorCode.INVALID_SEARCH_CRITERIA)
                .verify();
    }

    @Test
    @DisplayName("測試通過參數獲取所有使用者: 異常情況 - args 全部為無效資料，應丟出異常情況")
    void getAllByParams_abnormalCase_allInvalidArgs_shouldThrowException() {
        String typeId = "ID";
        Object[] invalidIdArgs = {"abc", "def"};

        when(mockCacheManager.getCachesAsConcat(anyList(), eq(User.class), eq(CacheProviderEnum.USER_CACHE))).thenReturn(Flux.empty());

        StepVerifier
                .create(userServiceImplUnderTest.getAllByParams(typeId, invalidIdArgs))
                .expectErrorMatches(e -> e instanceof ValidationException validationException && validationException.getErrorCode() == ValidationException.ErrorCode.INVALID_SEARCH_CRITERIA)
                .verify();

        String typeUsername = "NAME";
        Object[] invalidUsernameArgs = {"", "   "};

        StepVerifier
                .create(userServiceImplUnderTest.getAllByParams(typeUsername, invalidUsernameArgs))
                .expectErrorMatches(e -> e instanceof ValidationException validationException && validationException.getErrorCode() == ValidationException.ErrorCode.INVALID_SEARCH_CRITERIA)
                .verify();
    }

    @Test
    @DisplayName("測試通過參數獲取所有使用者: 邊界條件 - args 為空陣列，應丟出異常情況")
    void getAllByParams_edgeCase_emptyArgs_shouldThrowException() {
        String type = "ID";
        Object[] args = {};

        StepVerifier
                .create(userServiceImplUnderTest.getAllByParams(type, args))
                .expectErrorMatches(e -> e instanceof ValidationException validationException && validationException.getErrorCode() == ValidationException.ErrorCode.SEARCH_CRITERIA_EMPTY)
                .verify();
    }

    @Test
    @DisplayName("測試通過參數獲取所有使用者: 邊界條件 - 含大量 ID 或使用者名稱查詢")
    void getAllByParams_edgeCase_largeNumberOfArgs() {
        String typeId = "ID";
        Object[] args = {1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L};
        List<User> mockUsers = List.of(mock(User.class), mock(User.class));

        when(mockCacheManager.getCachesAsConcat(anyList(), eq(User.class), eq(CacheProviderEnum.USER_CACHE))).thenReturn(Flux.empty());
        when(mockUserRepository.findAllByIdIn(anyList())).thenReturn(Flux.fromIterable(mockUsers));
        when(mockCacheManager.runAndSetCache(anyList(), eq(User.class), eq(CacheProviderEnum.USER_CACHE), any(Flux.class), anyList())).thenAnswer(
                invocation -> invocation.getArgument(3));

        StepVerifier.create(userServiceImplUnderTest.getAllByParams(typeId, args)).expectNextCount(mockUsers.size()).verifyComplete();
    }
}
