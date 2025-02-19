package xyz.dowob.filemanagement.service.serviceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.component.provider.providerInterface.EmailProvider;
import xyz.dowob.filemanagement.config.properties.SecurityProperties;
import xyz.dowob.filemanagement.customenum.RoleEnum;
import xyz.dowob.filemanagement.customenum.TokenEnum;
import xyz.dowob.filemanagement.data.user.dto.AuthRequestDTO;
import xyz.dowob.filemanagement.data.user.dto.RegisterDTO;
import xyz.dowob.filemanagement.data.user.dto.ResetPasswordDTO;
import xyz.dowob.filemanagement.data.user.dto.UserEmailDTO;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.repostiory.UserRepository;
import xyz.dowob.filemanagement.service.serviceInterface.AuthorizationService;
import xyz.dowob.filemanagement.service.serviceInterface.TokenService;
import xyz.dowob.filemanagement.service.serviceInterface.ValidationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository mockUserRepository;
    @Mock
    private ValidationService mockValidationService;
    @Mock
    private AuthorizationService mockAuthorizationService;
    @Mock
    private TokenService mockTokenService;
    @Mock
    private EmailProvider mockEmailProvider;
    @Mock
    private SecurityProperties mockSecurityProperties;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserServiceImpl userServiceImplUnderTest;

    @BeforeEach
    void setUp() {
        userServiceImplUnderTest = new UserServiceImpl(mockUserRepository,
                                                       mockValidationService,
                                                       mockAuthorizationService,
                                                       mockTokenService,
                                                       mockEmailProvider, mockSecurityProperties, passwordEncoder
        );
    }

    @Test
    void testRegister() {
        // Setup
        final RegisterDTO registerUserDTO = new RegisterDTO();
        registerUserDTO.setUsername("username");
        registerUserDTO.setPassword("newPassword");
        registerUserDTO.setConfirmPassword("confirmPassword");
        registerUserDTO.setEmail("email");

        when(mockValidationService.validateRegisterDTO(any(RegisterDTO.class))).thenReturn(Mono.empty());

        // Configure UserRepository.save(...).
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("newPassword");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);
        final Mono<User> userMono = Mono.just(user);
        final User entity = new User();
        entity.setId(0L);
        entity.setUsername("username");
        entity.setPassword("newPassword");
        entity.setEmail("email");
        entity.setRole(RoleEnum.ADMIN);
        when(mockUserRepository.save(entity)).thenReturn(userMono);

        // Run the test
        final Mono<Void> result = userServiceImplUnderTest.register(registerUserDTO);

        // Verify the results
    }

    @Test
    void testRegister_ValidationServiceReturnsError() {
        // Setup
        final RegisterDTO registerUserDTO = new RegisterDTO();
        registerUserDTO.setUsername("username");
        registerUserDTO.setPassword("newPassword");
        registerUserDTO.setConfirmPassword("confirmPassword");
        registerUserDTO.setEmail("email");

        when(mockValidationService.validateRegisterDTO(any(RegisterDTO.class))).thenReturn(Mono.error(new Exception("message")));

        // Configure UserRepository.save(...).
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("newPassword");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);
        final Mono<User> userMono = Mono.just(user);
        final User entity = new User();
        entity.setId(0L);
        entity.setUsername("username");
        entity.setPassword("newPassword");
        entity.setEmail("email");
        entity.setRole(RoleEnum.ADMIN);
        when(mockUserRepository.save(entity)).thenReturn(userMono);

        // Run the test
        final Mono<Void> result = userServiceImplUnderTest.register(registerUserDTO);

        // Verify the results
    }

    @Test
    void testRegister_UserRepositoryReturnsNoItem() {
        // Setup
        final RegisterDTO registerUserDTO = new RegisterDTO();
        registerUserDTO.setUsername("username");
        registerUserDTO.setPassword("newPassword");
        registerUserDTO.setConfirmPassword("confirmPassword");
        registerUserDTO.setEmail("email");

        when(mockValidationService.validateRegisterDTO(any(RegisterDTO.class))).thenReturn(Mono.empty());

        // Configure UserRepository.save(...).
        final User entity = new User();
        entity.setId(0L);
        entity.setUsername("username");
        entity.setPassword("newPassword");
        entity.setEmail("email");
        entity.setRole(RoleEnum.ADMIN);
        when(mockUserRepository.save(entity)).thenReturn(Mono.empty());

        // Run the test
        final Mono<Void> result = userServiceImplUnderTest.register(registerUserDTO);

        // Verify the results
    }

    @Test
    void testRegister_UserRepositoryReturnsError() {
        // Setup
        final RegisterDTO registerUserDTO = new RegisterDTO();
        registerUserDTO.setUsername("username");
        registerUserDTO.setPassword("newPassword");
        registerUserDTO.setConfirmPassword("confirmPassword");
        registerUserDTO.setEmail("email");

        when(mockValidationService.validateRegisterDTO(any(RegisterDTO.class))).thenReturn(Mono.empty());

        // Configure UserRepository.save(...).
        final Mono<User> userMono = Mono.error(new Exception("message"));
        final User entity = new User();
        entity.setId(0L);
        entity.setUsername("username");
        entity.setPassword("newPassword");
        entity.setEmail("email");
        entity.setRole(RoleEnum.ADMIN);
        when(mockUserRepository.save(entity)).thenReturn(userMono);

        // Run the test
        final Mono<Void> result = userServiceImplUnderTest.register(registerUserDTO);

        // Verify the results
    }

    @Test
    void testLogin() {
        // Setup
        final AuthRequestDTO authRequestDTO = new AuthRequestDTO("username", "password");
        final ServerWebExchange request = null;
        when(mockValidationService.validateNotNull(new AuthRequestDTO("username", "password"))).thenReturn(Mono.empty());
        when(mockAuthorizationService.authenticate(eq(new AuthRequestDTO("username", "password")),
                                                   any(ServerWebExchange.class)
        )).thenReturn(Mono.just("value"));

        // Run the test
        final Mono<String> result = userServiceImplUnderTest.login(authRequestDTO, request);

        // Verify the results
    }

    @Test
    void testLogin_ValidationServiceReturnsError() {
        // Setup
        final AuthRequestDTO authRequestDTO = new AuthRequestDTO("username", "password");
        final ServerWebExchange request = null;
        when(mockValidationService.validateNotNull(new AuthRequestDTO("username", "password"))).thenReturn(Mono.error(new Exception(
                "message")));
        when(mockAuthorizationService.authenticate(eq(new AuthRequestDTO("username", "password")),
                                                   any(ServerWebExchange.class)
        )).thenReturn(Mono.just("value"));

        // Run the test
        final Mono<String> result = userServiceImplUnderTest.login(authRequestDTO, request);

        // Verify the results
    }

    @Test
    void testLogin_AuthorizationServiceReturnsNoItem() {
        // Setup
        final AuthRequestDTO authRequestDTO = new AuthRequestDTO("username", "password");
        final ServerWebExchange request = null;
        when(mockValidationService.validateNotNull(new AuthRequestDTO("username", "password"))).thenReturn(Mono.empty());
        when(mockAuthorizationService.authenticate(eq(new AuthRequestDTO("username", "password")),
                                                   any(ServerWebExchange.class)
        )).thenReturn(Mono.empty());

        // Run the test
        final Mono<String> result = userServiceImplUnderTest.login(authRequestDTO, request);

        // Verify the results
    }

    @Test
    void testLogin_AuthorizationServiceReturnsError() {
        // Setup
        final AuthRequestDTO authRequestDTO = new AuthRequestDTO("username", "password");
        final ServerWebExchange request = null;
        when(mockValidationService.validateNotNull(new AuthRequestDTO("username", "password"))).thenReturn(Mono.empty());
        when(mockAuthorizationService.authenticate(eq(new AuthRequestDTO("username", "password")),
                                                   any(ServerWebExchange.class)
        )).thenReturn(Mono.error(new Exception("message")));

        // Run the test
        final Mono<String> result = userServiceImplUnderTest.login(authRequestDTO, request);

        // Verify the results
    }

    @Test
    void testLogout() {
        // Setup
        final ServerWebExchange exchange = null;

        // Configure UserRepository.findById(...).
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("newPassword");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);
        final Mono<User> userMono = Mono.just(user);
        when(mockUserRepository.findById(0L)).thenReturn(userMono);

        when(mockTokenService.revokeToken(0L, TokenEnum.JWT_AUTHORIZATION_TOKEN)).thenReturn(Mono.empty());

        // Run the test
        final Mono<Void> result = userServiceImplUnderTest.logout(0L, exchange);

        // Verify the results
    }

    @Test
    void testLogout_UserRepositoryReturnsNoItem() {
        // Setup
        final ServerWebExchange exchange = null;
        when(mockUserRepository.findById(0L)).thenReturn(Mono.empty());
        when(mockTokenService.revokeToken(0L, TokenEnum.JWT_AUTHORIZATION_TOKEN)).thenReturn(Mono.empty());

        // Run the test
        final Mono<Void> result = userServiceImplUnderTest.logout(0L, exchange);

        // Verify the results
    }

    @Test
    void testLogout_UserRepositoryReturnsError() {
        // Setup
        final ServerWebExchange exchange = null;

        // Configure UserRepository.findById(...).
        final Mono<User> userMono = Mono.error(new Exception("message"));
        when(mockUserRepository.findById(0L)).thenReturn(userMono);

        when(mockTokenService.revokeToken(0L, TokenEnum.JWT_AUTHORIZATION_TOKEN)).thenReturn(Mono.empty());

        // Run the test
        final Mono<Void> result = userServiceImplUnderTest.logout(0L, exchange);

        // Verify the results
    }

    @Test
    void testLogout_TokenServiceReturnsError() {
        // Setup
        final ServerWebExchange exchange = null;

        // Configure UserRepository.findById(...).
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("newPassword");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);
        final Mono<User> userMono = Mono.just(user);
        when(mockUserRepository.findById(0L)).thenReturn(userMono);

        when(mockTokenService.revokeToken(0L, TokenEnum.JWT_AUTHORIZATION_TOKEN)).thenReturn(Mono.error(new Exception("message")));

        // Run the test
        final Mono<Void> result = userServiceImplUnderTest.logout(0L, exchange);

        // Verify the results
    }

    @Test
    void testChangePassword() {
        assertThat(userServiceImplUnderTest.changePassword(new User())).isNull();
    }

    @Test
    void testChangeEmail() {
        assertThat(userServiceImplUnderTest.changeEmail(new User())).isNull();
    }

    @Test
    void testSendResetPasswordMail() {
        // Setup
        final UserEmailDTO userEmailDTO = new UserEmailDTO();
        userEmailDTO.setEmail("email");

        // Configure ValidationService.validateNotNull(...).
        final UserEmailDTO dto = new UserEmailDTO();
        dto.setEmail("email");
        when(mockValidationService.validateNotNull(dto)).thenReturn(Mono.empty());

        // Configure UserRepository.findByEmail(...).
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("newPassword");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);
        final Mono<User> userMono = Mono.just(user);
        when(mockUserRepository.findByEmail("email")).thenReturn(userMono);

        // Configure TokenService.generateToken(...).
        final User user1 = new User();
        user1.setId(0L);
        user1.setUsername("username");
        user1.setPassword("newPassword");
        user1.setEmail("email");
        user1.setRole(RoleEnum.ADMIN);
        when(mockTokenService.generateToken(user1, TokenEnum.RESET_PASSWORD_TOKEN)).thenReturn(Mono.just("value"));

        // Configure SecurityProperties.getResetPasswordToken(...).
        final SecurityProperties.resetPasswordToken resetPasswordToken = new SecurityProperties.resetPasswordToken();
        resetPasswordToken.setLength(0);
        resetPasswordToken.setExpiration(0);
        when(mockSecurityProperties.getResetPasswordToken()).thenReturn(resetPasswordToken);

        when(mockEmailProvider.sendEmail("email", "重置密碼", "content")).thenReturn(Mono.empty());

        // Run the test
        final Mono<Void> result = userServiceImplUnderTest.sendResetPasswordMail(userEmailDTO);

        // Verify the results
    }

    @Test
    void testSendResetPasswordMail_ValidationServiceReturnsError() {
        // Setup
        final UserEmailDTO userEmailDTO = new UserEmailDTO();
        userEmailDTO.setEmail("email");

        // Configure ValidationService.validateNotNull(...).
        final UserEmailDTO dto = new UserEmailDTO();
        dto.setEmail("email");
        when(mockValidationService.validateNotNull(dto)).thenReturn(Mono.error(new Exception("message")));

        // Configure UserRepository.findByEmail(...).
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("newPassword");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);
        final Mono<User> userMono = Mono.just(user);
        when(mockUserRepository.findByEmail("email")).thenReturn(userMono);

        // Configure TokenService.generateToken(...).
        final User user1 = new User();
        user1.setId(0L);
        user1.setUsername("username");
        user1.setPassword("newPassword");
        user1.setEmail("email");
        user1.setRole(RoleEnum.ADMIN);
        when(mockTokenService.generateToken(user1, TokenEnum.RESET_PASSWORD_TOKEN)).thenReturn(Mono.just("value"));

        // Configure SecurityProperties.getResetPasswordToken(...).
        final SecurityProperties.resetPasswordToken resetPasswordToken = new SecurityProperties.resetPasswordToken();
        resetPasswordToken.setLength(0);
        resetPasswordToken.setExpiration(0);
        when(mockSecurityProperties.getResetPasswordToken()).thenReturn(resetPasswordToken);

        when(mockEmailProvider.sendEmail("email", "重置密碼", "content")).thenReturn(Mono.empty());

        // Run the test
        final Mono<Void> result = userServiceImplUnderTest.sendResetPasswordMail(userEmailDTO);

        // Verify the results
    }

    @Test
    void testSendResetPasswordMail_UserRepositoryReturnsNoItem() {
        // Setup
        final UserEmailDTO userEmailDTO = new UserEmailDTO();
        userEmailDTO.setEmail("email");

        // Configure ValidationService.validateNotNull(...).
        final UserEmailDTO dto = new UserEmailDTO();
        dto.setEmail("email");
        when(mockValidationService.validateNotNull(dto)).thenReturn(Mono.empty());

        when(mockUserRepository.findByEmail("email")).thenReturn(Mono.empty());

        // Configure TokenService.generateToken(...).
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("newPassword");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);
        when(mockTokenService.generateToken(user, TokenEnum.RESET_PASSWORD_TOKEN)).thenReturn(Mono.just("value"));

        // Configure SecurityProperties.getResetPasswordToken(...).
        final SecurityProperties.resetPasswordToken resetPasswordToken = new SecurityProperties.resetPasswordToken();
        resetPasswordToken.setLength(0);
        resetPasswordToken.setExpiration(0);
        when(mockSecurityProperties.getResetPasswordToken()).thenReturn(resetPasswordToken);

        when(mockEmailProvider.sendEmail("email", "重置密碼", "content")).thenReturn(Mono.empty());

        // Run the test
        final Mono<Void> result = userServiceImplUnderTest.sendResetPasswordMail(userEmailDTO);

        // Verify the results
    }

    @Test
    void testSendResetPasswordMail_UserRepositoryReturnsError() {
        // Setup
        final UserEmailDTO userEmailDTO = new UserEmailDTO();
        userEmailDTO.setEmail("email");

        // Configure ValidationService.validateNotNull(...).
        final UserEmailDTO dto = new UserEmailDTO();
        dto.setEmail("email");
        when(mockValidationService.validateNotNull(dto)).thenReturn(Mono.empty());

        // Configure UserRepository.findByEmail(...).
        final Mono<User> userMono = Mono.error(new Exception("message"));
        when(mockUserRepository.findByEmail("email")).thenReturn(userMono);

        // Configure TokenService.generateToken(...).
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("newPassword");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);
        when(mockTokenService.generateToken(user, TokenEnum.RESET_PASSWORD_TOKEN)).thenReturn(Mono.just("value"));

        // Configure SecurityProperties.getResetPasswordToken(...).
        final SecurityProperties.resetPasswordToken resetPasswordToken = new SecurityProperties.resetPasswordToken();
        resetPasswordToken.setLength(0);
        resetPasswordToken.setExpiration(0);
        when(mockSecurityProperties.getResetPasswordToken()).thenReturn(resetPasswordToken);

        when(mockEmailProvider.sendEmail("email", "重置密碼", "content")).thenReturn(Mono.empty());

        // Run the test
        final Mono<Void> result = userServiceImplUnderTest.sendResetPasswordMail(userEmailDTO);

        // Verify the results
    }

    @Test
    void testSendResetPasswordMail_TokenServiceReturnsNoItem() {
        // Setup
        final UserEmailDTO userEmailDTO = new UserEmailDTO();
        userEmailDTO.setEmail("email");

        // Configure ValidationService.validateNotNull(...).
        final UserEmailDTO dto = new UserEmailDTO();
        dto.setEmail("email");
        when(mockValidationService.validateNotNull(dto)).thenReturn(Mono.empty());

        // Configure UserRepository.findByEmail(...).
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("newPassword");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);
        final Mono<User> userMono = Mono.just(user);
        when(mockUserRepository.findByEmail("email")).thenReturn(userMono);

        // Configure TokenService.generateToken(...).
        final User user1 = new User();
        user1.setId(0L);
        user1.setUsername("username");
        user1.setPassword("newPassword");
        user1.setEmail("email");
        user1.setRole(RoleEnum.ADMIN);
        when(mockTokenService.generateToken(user1, TokenEnum.RESET_PASSWORD_TOKEN)).thenReturn(Mono.empty());

        // Configure SecurityProperties.getResetPasswordToken(...).
        final SecurityProperties.resetPasswordToken resetPasswordToken = new SecurityProperties.resetPasswordToken();
        resetPasswordToken.setLength(0);
        resetPasswordToken.setExpiration(0);
        when(mockSecurityProperties.getResetPasswordToken()).thenReturn(resetPasswordToken);

        when(mockEmailProvider.sendEmail("email", "重置密碼", "content")).thenReturn(Mono.empty());

        // Run the test
        final Mono<Void> result = userServiceImplUnderTest.sendResetPasswordMail(userEmailDTO);

        // Verify the results
    }

    @Test
    void testSendResetPasswordMail_TokenServiceReturnsError() {
        // Setup
        final UserEmailDTO userEmailDTO = new UserEmailDTO();
        userEmailDTO.setEmail("email");

        // Configure ValidationService.validateNotNull(...).
        final UserEmailDTO dto = new UserEmailDTO();
        dto.setEmail("email");
        when(mockValidationService.validateNotNull(dto)).thenReturn(Mono.empty());

        // Configure UserRepository.findByEmail(...).
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("newPassword");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);
        final Mono<User> userMono = Mono.just(user);
        when(mockUserRepository.findByEmail("email")).thenReturn(userMono);

        // Configure TokenService.generateToken(...).
        final User user1 = new User();
        user1.setId(0L);
        user1.setUsername("username");
        user1.setPassword("newPassword");
        user1.setEmail("email");
        user1.setRole(RoleEnum.ADMIN);
        when(mockTokenService.generateToken(user1, TokenEnum.RESET_PASSWORD_TOKEN)).thenReturn(Mono.error(new Exception("message")));

        // Configure SecurityProperties.getResetPasswordToken(...).
        final SecurityProperties.resetPasswordToken resetPasswordToken = new SecurityProperties.resetPasswordToken();
        resetPasswordToken.setLength(0);
        resetPasswordToken.setExpiration(0);
        when(mockSecurityProperties.getResetPasswordToken()).thenReturn(resetPasswordToken);

        when(mockEmailProvider.sendEmail("email", "重置密碼", "content")).thenReturn(Mono.empty());

        // Run the test
        final Mono<Void> result = userServiceImplUnderTest.sendResetPasswordMail(userEmailDTO);

        // Verify the results
    }

    @Test
    void testSendResetPasswordMail_EmailProviderReturnsError() {
        // Setup
        final UserEmailDTO userEmailDTO = new UserEmailDTO();
        userEmailDTO.setEmail("email");

        // Configure ValidationService.validateNotNull(...).
        final UserEmailDTO dto = new UserEmailDTO();
        dto.setEmail("email");
        when(mockValidationService.validateNotNull(dto)).thenReturn(Mono.empty());

        // Configure UserRepository.findByEmail(...).
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("newPassword");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);
        final Mono<User> userMono = Mono.just(user);
        when(mockUserRepository.findByEmail("email")).thenReturn(userMono);

        // Configure TokenService.generateToken(...).
        final User user1 = new User();
        user1.setId(0L);
        user1.setUsername("username");
        user1.setPassword("newPassword");
        user1.setEmail("email");
        user1.setRole(RoleEnum.ADMIN);
        when(mockTokenService.generateToken(user1, TokenEnum.RESET_PASSWORD_TOKEN)).thenReturn(Mono.just("value"));

        // Configure SecurityProperties.getResetPasswordToken(...).
        final SecurityProperties.resetPasswordToken resetPasswordToken = new SecurityProperties.resetPasswordToken();
        resetPasswordToken.setLength(0);
        resetPasswordToken.setExpiration(0);
        when(mockSecurityProperties.getResetPasswordToken()).thenReturn(resetPasswordToken);

        when(mockEmailProvider.sendEmail("email", "重置密碼", "content")).thenReturn(Mono.error(new Exception("message")));

        // Run the test
        final Mono<Void> result = userServiceImplUnderTest.sendResetPasswordMail(userEmailDTO);

        // Verify the results
    }

    @Test
    void testResetPassword() {
        // Setup
        final ResetPasswordDTO resetPasswordDTO = new ResetPasswordDTO();
        resetPasswordDTO.setEmail("email");
        resetPasswordDTO.setVerificationCode("verificationCode");
        resetPasswordDTO.setNewPassword("newPassword");
        resetPasswordDTO.setConfirmPassword("confirmPassword");

        when(mockValidationService.validateResetPasswordDTO(any(ResetPasswordDTO.class))).thenReturn(Mono.empty());

        // Configure UserRepository.findByEmail(...).
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("newPassword");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);
        final Mono<User> userMono = Mono.just(user);
        when(mockUserRepository.findByEmail("email")).thenReturn(userMono);

        when(mockTokenService.validateToken("verificationCode", 0L, TokenEnum.RESET_PASSWORD_TOKEN)).thenReturn(Mono.just(0L));

        // Configure UserRepository.save(...).
        final User user1 = new User();
        user1.setId(0L);
        user1.setUsername("username");
        user1.setPassword("newPassword");
        user1.setEmail("email");
        user1.setRole(RoleEnum.ADMIN);
        final Mono<User> userMono1 = Mono.just(user1);
        final User entity = new User();
        entity.setId(0L);
        entity.setUsername("username");
        entity.setPassword("newPassword");
        entity.setEmail("email");
        entity.setRole(RoleEnum.ADMIN);
        when(mockUserRepository.save(entity)).thenReturn(userMono1);

        when(mockTokenService.revokeToken(0L, TokenEnum.RESET_PASSWORD_TOKEN)).thenReturn(Mono.empty());

        // Run the test
        final Mono<Void> result = userServiceImplUnderTest.resetPassword(resetPasswordDTO);

        // Verify the results
    }

    @Test
    void testResetPassword_ValidationServiceReturnsError() {
        // Setup
        final ResetPasswordDTO resetPasswordDTO = new ResetPasswordDTO();
        resetPasswordDTO.setEmail("email");
        resetPasswordDTO.setVerificationCode("verificationCode");
        resetPasswordDTO.setNewPassword("newPassword");
        resetPasswordDTO.setConfirmPassword("confirmPassword");

        when(mockValidationService.validateResetPasswordDTO(any(ResetPasswordDTO.class))).thenReturn(Mono.error(new Exception("message")));

        // Configure UserRepository.findByEmail(...).
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("newPassword");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);
        final Mono<User> userMono = Mono.just(user);
        when(mockUserRepository.findByEmail("email")).thenReturn(userMono);

        when(mockTokenService.validateToken("verificationCode", 0L, TokenEnum.RESET_PASSWORD_TOKEN)).thenReturn(Mono.just(0L));

        // Configure UserRepository.save(...).
        final User user1 = new User();
        user1.setId(0L);
        user1.setUsername("username");
        user1.setPassword("newPassword");
        user1.setEmail("email");
        user1.setRole(RoleEnum.ADMIN);
        final Mono<User> userMono1 = Mono.just(user1);
        final User entity = new User();
        entity.setId(0L);
        entity.setUsername("username");
        entity.setPassword("newPassword");
        entity.setEmail("email");
        entity.setRole(RoleEnum.ADMIN);
        when(mockUserRepository.save(entity)).thenReturn(userMono1);

        when(mockTokenService.revokeToken(0L, TokenEnum.RESET_PASSWORD_TOKEN)).thenReturn(Mono.empty());

        // Run the test
        final Mono<Void> result = userServiceImplUnderTest.resetPassword(resetPasswordDTO);

        // Verify the results
    }

    @Test
    void testResetPassword_UserRepositoryFindByEmailReturnsNoItem() {
        // Setup
        final ResetPasswordDTO resetPasswordDTO = new ResetPasswordDTO();
        resetPasswordDTO.setEmail("email");
        resetPasswordDTO.setVerificationCode("verificationCode");
        resetPasswordDTO.setNewPassword("newPassword");
        resetPasswordDTO.setConfirmPassword("confirmPassword");

        when(mockValidationService.validateResetPasswordDTO(any(ResetPasswordDTO.class))).thenReturn(Mono.empty());
        when(mockUserRepository.findByEmail("email")).thenReturn(Mono.empty());
        when(mockTokenService.validateToken("verificationCode", 0L, TokenEnum.RESET_PASSWORD_TOKEN)).thenReturn(Mono.just(0L));

        // Configure UserRepository.save(...).
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("newPassword");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);
        final Mono<User> userMono = Mono.just(user);
        final User entity = new User();
        entity.setId(0L);
        entity.setUsername("username");
        entity.setPassword("newPassword");
        entity.setEmail("email");
        entity.setRole(RoleEnum.ADMIN);
        when(mockUserRepository.save(entity)).thenReturn(userMono);

        when(mockTokenService.revokeToken(0L, TokenEnum.RESET_PASSWORD_TOKEN)).thenReturn(Mono.empty());

        // Run the test
        final Mono<Void> result = userServiceImplUnderTest.resetPassword(resetPasswordDTO);

        // Verify the results
    }

    @Test
    void testResetPassword_UserRepositoryFindByEmailReturnsError() {
        // Setup
        final ResetPasswordDTO resetPasswordDTO = new ResetPasswordDTO();
        resetPasswordDTO.setEmail("email");
        resetPasswordDTO.setVerificationCode("verificationCode");
        resetPasswordDTO.setNewPassword("newPassword");
        resetPasswordDTO.setConfirmPassword("confirmPassword");

        when(mockValidationService.validateResetPasswordDTO(any(ResetPasswordDTO.class))).thenReturn(Mono.empty());

        // Configure UserRepository.findByEmail(...).
        final Mono<User> userMono = Mono.error(new Exception("message"));
        when(mockUserRepository.findByEmail("email")).thenReturn(userMono);

        when(mockTokenService.validateToken("verificationCode", 0L, TokenEnum.RESET_PASSWORD_TOKEN)).thenReturn(Mono.just(0L));

        // Configure UserRepository.save(...).
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("newPassword");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);
        final Mono<User> userMono1 = Mono.just(user);
        final User entity = new User();
        entity.setId(0L);
        entity.setUsername("username");
        entity.setPassword("newPassword");
        entity.setEmail("email");
        entity.setRole(RoleEnum.ADMIN);
        when(mockUserRepository.save(entity)).thenReturn(userMono1);

        when(mockTokenService.revokeToken(0L, TokenEnum.RESET_PASSWORD_TOKEN)).thenReturn(Mono.empty());

        // Run the test
        final Mono<Void> result = userServiceImplUnderTest.resetPassword(resetPasswordDTO);

        // Verify the results
    }

    @Test
    void testResetPassword_TokenServiceValidateTokenReturnsNoItem() {
        // Setup
        final ResetPasswordDTO resetPasswordDTO = new ResetPasswordDTO();
        resetPasswordDTO.setEmail("email");
        resetPasswordDTO.setVerificationCode("verificationCode");
        resetPasswordDTO.setNewPassword("newPassword");
        resetPasswordDTO.setConfirmPassword("confirmPassword");

        when(mockValidationService.validateResetPasswordDTO(any(ResetPasswordDTO.class))).thenReturn(Mono.empty());

        // Configure UserRepository.findByEmail(...).
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("newPassword");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);
        final Mono<User> userMono = Mono.just(user);
        when(mockUserRepository.findByEmail("email")).thenReturn(userMono);

        when(mockTokenService.validateToken("verificationCode", 0L, TokenEnum.RESET_PASSWORD_TOKEN)).thenReturn(Mono.empty());

        // Configure UserRepository.save(...).
        final User user1 = new User();
        user1.setId(0L);
        user1.setUsername("username");
        user1.setPassword("newPassword");
        user1.setEmail("email");
        user1.setRole(RoleEnum.ADMIN);
        final Mono<User> userMono1 = Mono.just(user1);
        final User entity = new User();
        entity.setId(0L);
        entity.setUsername("username");
        entity.setPassword("newPassword");
        entity.setEmail("email");
        entity.setRole(RoleEnum.ADMIN);
        when(mockUserRepository.save(entity)).thenReturn(userMono1);

        when(mockTokenService.revokeToken(0L, TokenEnum.RESET_PASSWORD_TOKEN)).thenReturn(Mono.empty());

        // Run the test
        final Mono<Void> result = userServiceImplUnderTest.resetPassword(resetPasswordDTO);

        // Verify the results
    }

    @Test
    void testResetPassword_TokenServiceValidateTokenReturnsError() {
        // Setup
        final ResetPasswordDTO resetPasswordDTO = new ResetPasswordDTO();
        resetPasswordDTO.setEmail("email");
        resetPasswordDTO.setVerificationCode("verificationCode");
        resetPasswordDTO.setNewPassword("newPassword");
        resetPasswordDTO.setConfirmPassword("confirmPassword");

        when(mockValidationService.validateResetPasswordDTO(any(ResetPasswordDTO.class))).thenReturn(Mono.empty());

        // Configure UserRepository.findByEmail(...).
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("newPassword");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);
        final Mono<User> userMono = Mono.just(user);
        when(mockUserRepository.findByEmail("email")).thenReturn(userMono);

        when(mockTokenService.validateToken("verificationCode", 0L, TokenEnum.RESET_PASSWORD_TOKEN)).thenReturn(Mono.error(new Exception(
                "message")));

        // Configure UserRepository.save(...).
        final User user1 = new User();
        user1.setId(0L);
        user1.setUsername("username");
        user1.setPassword("newPassword");
        user1.setEmail("email");
        user1.setRole(RoleEnum.ADMIN);
        final Mono<User> userMono1 = Mono.just(user1);
        final User entity = new User();
        entity.setId(0L);
        entity.setUsername("username");
        entity.setPassword("newPassword");
        entity.setEmail("email");
        entity.setRole(RoleEnum.ADMIN);
        when(mockUserRepository.save(entity)).thenReturn(userMono1);

        when(mockTokenService.revokeToken(0L, TokenEnum.RESET_PASSWORD_TOKEN)).thenReturn(Mono.empty());

        // Run the test
        final Mono<Void> result = userServiceImplUnderTest.resetPassword(resetPasswordDTO);

        // Verify the results
    }

    @Test
    void testResetPassword_UserRepositorySaveReturnsNoItem() {
        // Setup
        final ResetPasswordDTO resetPasswordDTO = new ResetPasswordDTO();
        resetPasswordDTO.setEmail("email");
        resetPasswordDTO.setVerificationCode("verificationCode");
        resetPasswordDTO.setNewPassword("newPassword");
        resetPasswordDTO.setConfirmPassword("confirmPassword");

        when(mockValidationService.validateResetPasswordDTO(any(ResetPasswordDTO.class))).thenReturn(Mono.empty());

        // Configure UserRepository.findByEmail(...).
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("newPassword");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);
        final Mono<User> userMono = Mono.just(user);
        when(mockUserRepository.findByEmail("email")).thenReturn(userMono);

        when(mockTokenService.validateToken("verificationCode", 0L, TokenEnum.RESET_PASSWORD_TOKEN)).thenReturn(Mono.just(0L));

        // Configure UserRepository.save(...).
        final User entity = new User();
        entity.setId(0L);
        entity.setUsername("username");
        entity.setPassword("newPassword");
        entity.setEmail("email");
        entity.setRole(RoleEnum.ADMIN);
        when(mockUserRepository.save(entity)).thenReturn(Mono.empty());

        when(mockTokenService.revokeToken(0L, TokenEnum.RESET_PASSWORD_TOKEN)).thenReturn(Mono.empty());

        // Run the test
        final Mono<Void> result = userServiceImplUnderTest.resetPassword(resetPasswordDTO);

        // Verify the results
    }

    @Test
    void testResetPassword_UserRepositorySaveReturnsError() {
        // Setup
        final ResetPasswordDTO resetPasswordDTO = new ResetPasswordDTO();
        resetPasswordDTO.setEmail("email");
        resetPasswordDTO.setVerificationCode("verificationCode");
        resetPasswordDTO.setNewPassword("newPassword");
        resetPasswordDTO.setConfirmPassword("confirmPassword");

        when(mockValidationService.validateResetPasswordDTO(any(ResetPasswordDTO.class))).thenReturn(Mono.empty());

        // Configure UserRepository.findByEmail(...).
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("newPassword");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);
        final Mono<User> userMono = Mono.just(user);
        when(mockUserRepository.findByEmail("email")).thenReturn(userMono);

        when(mockTokenService.validateToken("verificationCode", 0L, TokenEnum.RESET_PASSWORD_TOKEN)).thenReturn(Mono.just(0L));

        // Configure UserRepository.save(...).
        final Mono<User> userMono1 = Mono.error(new Exception("message"));
        final User entity = new User();
        entity.setId(0L);
        entity.setUsername("username");
        entity.setPassword("newPassword");
        entity.setEmail("email");
        entity.setRole(RoleEnum.ADMIN);
        when(mockUserRepository.save(entity)).thenReturn(userMono1);

        when(mockTokenService.revokeToken(0L, TokenEnum.RESET_PASSWORD_TOKEN)).thenReturn(Mono.empty());

        // Run the test
        final Mono<Void> result = userServiceImplUnderTest.resetPassword(resetPasswordDTO);

        // Verify the results
    }

    @Test
    void testResetPassword_TokenServiceRevokeTokenReturnsError() {
        // Setup
        final ResetPasswordDTO resetPasswordDTO = new ResetPasswordDTO();
        resetPasswordDTO.setEmail("email");
        resetPasswordDTO.setVerificationCode("verificationCode");
        resetPasswordDTO.setNewPassword("newPassword");
        resetPasswordDTO.setConfirmPassword("confirmPassword");

        when(mockValidationService.validateResetPasswordDTO(any(ResetPasswordDTO.class))).thenReturn(Mono.empty());

        // Configure UserRepository.findByEmail(...).
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("newPassword");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);
        final Mono<User> userMono = Mono.just(user);
        when(mockUserRepository.findByEmail("email")).thenReturn(userMono);

        when(mockTokenService.validateToken("verificationCode", 0L, TokenEnum.RESET_PASSWORD_TOKEN)).thenReturn(Mono.just(0L));

        // Configure UserRepository.save(...).
        final User user1 = new User();
        user1.setId(0L);
        user1.setUsername("username");
        user1.setPassword("newPassword");
        user1.setEmail("email");
        user1.setRole(RoleEnum.ADMIN);
        final Mono<User> userMono1 = Mono.just(user1);
        final User entity = new User();
        entity.setId(0L);
        entity.setUsername("username");
        entity.setPassword("newPassword");
        entity.setEmail("email");
        entity.setRole(RoleEnum.ADMIN);
        when(mockUserRepository.save(entity)).thenReturn(userMono1);

        when(mockTokenService.revokeToken(0L, TokenEnum.RESET_PASSWORD_TOKEN)).thenReturn(Mono.error(new Exception("message")));

        // Run the test
        final Mono<Void> result = userServiceImplUnderTest.resetPassword(resetPasswordDTO);

        // Verify the results
    }

    @Test
    void testGetUser() {
        // Setup
        final ServerWebExchange exchange = null;

        // Configure UserRepository.findById(...).
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("newPassword");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);
        final Mono<User> userMono = Mono.just(user);
        when(mockUserRepository.findById(0L)).thenReturn(userMono);

        // Run the test
        final Mono<User> result = userServiceImplUnderTest.getUser(exchange);

        // Verify the results
    }

    @Test
    void testGetUser_UserRepositoryReturnsNoItem() {
        // Setup
        final ServerWebExchange exchange = null;
        when(mockUserRepository.findById(0L)).thenReturn(Mono.empty());

        // Run the test
        final Mono<User> result = userServiceImplUnderTest.getUser(exchange);

        // Verify the results
    }

    @Test
    void testGetUser_UserRepositoryReturnsError() {
        // Setup
        final ServerWebExchange exchange = null;

        // Configure UserRepository.findById(...).
        final Mono<User> userMono = Mono.error(new Exception("message"));
        when(mockUserRepository.findById(0L)).thenReturn(userMono);

        // Run the test
        final Mono<User> result = userServiceImplUnderTest.getUser(exchange);

        // Verify the results
    }

    @Test
    void testCreate() {
        assertThat(userServiceImplUnderTest.create()).isNull();
    }

    @Test
    void testGetById() {
        // Setup
        // Configure UserRepository.findById(...).
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("newPassword");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);
        final Mono<User> userMono = Mono.just(user);
        when(mockUserRepository.findById(0L)).thenReturn(userMono);

        // Run the test
        final Mono<User> result = userServiceImplUnderTest.getById(0L);

        // Verify the results
    }

    @Test
    void testGetById_UserRepositoryReturnsNoItem() {
        // Setup
        when(mockUserRepository.findById(0L)).thenReturn(Mono.empty());

        // Run the test
        final Mono<User> result = userServiceImplUnderTest.getById(0L);

        // Verify the results
    }

    @Test
    void testGetById_UserRepositoryReturnsError() {
        // Setup
        // Configure UserRepository.findById(...).
        final Mono<User> userMono = Mono.error(new Exception("message"));
        when(mockUserRepository.findById(0L)).thenReturn(userMono);

        // Run the test
        final Mono<User> result = userServiceImplUnderTest.getById(0L);

        // Verify the results
    }

    @Test
    void testGetAll() {
        // Setup
        // Configure UserRepository.findAll(...).
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("newPassword");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);
        final Flux<User> userFlux = Flux.just(user);
        when(mockUserRepository.findAll()).thenReturn(userFlux);

        // Run the test
        final Flux<User> result = userServiceImplUnderTest.getAll();

        // Verify the results
    }

    @Test
    void testGetAll_UserRepositoryReturnsNoItem() {
        // Setup
        when(mockUserRepository.findAll()).thenReturn(Flux.empty());

        // Run the test
        final Flux<User> result = userServiceImplUnderTest.getAll();

        // Verify the results
    }

    @Test
    void testGetAll_UserRepositoryReturnsError() {
        // Setup
        // Configure UserRepository.findAll(...).
        final Flux<User> userFlux = Flux.error(new Exception("message"));
        when(mockUserRepository.findAll()).thenReturn(userFlux);

        // Run the test
        final Flux<User> result = userServiceImplUnderTest.getAll();

        // Verify the results
    }

    @Test
    void testUpdate() {
        assertThat(userServiceImplUnderTest.update(new User())).isNull();
    }

    @Test
    void testDelete() {
        assertThat(userServiceImplUnderTest.delete(new User())).isNull();
    }
}
