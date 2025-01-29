package xyz.dowob.filemanagement.service.ServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.security.web.server.csrf.ServerCsrfTokenRepository;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.customenum.RoleEnum;
import xyz.dowob.filemanagement.customenum.TokenEnum;
import xyz.dowob.filemanagement.data.user.dto.AuthRequestDTO;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.repostiory.UserRepository;
import xyz.dowob.filemanagement.service.ServiceInterface.TokenService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceImplTest {

    @Mock
    private UserRepository mockUserRepository;
    @Mock
    private PasswordEncoder mockPasswordEncoder;
    @Mock
    private TokenService mockTokenService;
    @Mock
    private ServerCsrfTokenRepository mockCsrfTokenRepository;

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
    void testTokenAuthorize() {
        // Setup
        final ServerWebExchange request = null;
        when(mockTokenService.validateToken("jwtToken", 0L, TokenEnum.JWT_AUTHORIZATION_TOKEN)).thenReturn(Mono.just(0L));

        // Configure UserRepository.findById(...).
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);
        final Mono<User> userMono = Mono.just(user);
        when(mockUserRepository.findById(any(Mono.class))).thenReturn(userMono);

        // Run the test
        final Mono<Void> result = authorizationServiceImplUnderTest.tokenAuthorize("jwtToken", request);

        // Verify the results
    }

    @Test
    void testTokenAuthorize_TokenServiceReturnsNull() {
        // Setup
        final ServerWebExchange request = null;
        when(mockTokenService.validateToken("jwtToken", 0L, TokenEnum.JWT_AUTHORIZATION_TOKEN)).thenReturn(Mono.empty());

        // Configure UserRepository.findById(...).
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);
        final Mono<User> userMono = Mono.just(user);
        when(mockUserRepository.findById(any(Mono.class))).thenReturn(userMono);

        // Run the test
        final Mono<Void> result = authorizationServiceImplUnderTest.tokenAuthorize("jwtToken", request);

        // Verify the results
    }

    @Test
    void testTokenAuthorize_TokenServiceReturnsError() {
        // Setup
        final ServerWebExchange request = null;
        when(mockTokenService.validateToken("jwtToken", 0L, TokenEnum.JWT_AUTHORIZATION_TOKEN)).thenReturn(Mono.error(new Exception(
                "message")));

        // Configure UserRepository.findById(...).
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);
        final Mono<User> userMono = Mono.just(user);
        when(mockUserRepository.findById(any(Mono.class))).thenReturn(userMono);

        // Run the test
        final Mono<Void> result = authorizationServiceImplUnderTest.tokenAuthorize("jwtToken", request);

        // Verify the results
    }

    @Test
    void testTokenAuthorize_UserRepositoryReturnsNoItem() {
        // Setup
        final ServerWebExchange request = null;
        when(mockTokenService.validateToken("jwtToken", 0L, TokenEnum.JWT_AUTHORIZATION_TOKEN)).thenReturn(Mono.just(0L));
        when(mockUserRepository.findById(any(Mono.class))).thenReturn(Mono.empty());

        // Run the test
        final Mono<Void> result = authorizationServiceImplUnderTest.tokenAuthorize("jwtToken", request);

        // Verify the results
    }

    @Test
    void testTokenAuthorize_UserRepositoryReturnsError() {
        // Setup
        final ServerWebExchange request = null;
        when(mockTokenService.validateToken("jwtToken", 0L, TokenEnum.JWT_AUTHORIZATION_TOKEN)).thenReturn(Mono.just(0L));

        // Configure UserRepository.findById(...).
        final Mono<User> userMono = Mono.error(new Exception("message"));
        when(mockUserRepository.findById(any(Mono.class))).thenReturn(userMono);

        // Run the test
        final Mono<Void> result = authorizationServiceImplUnderTest.tokenAuthorize("jwtToken", request);

        // Verify the results
    }

    @Test
    void testAuthenticate1() {
        // Setup
        final AuthRequestDTO authRequestDTO = new AuthRequestDTO("username", "password");
        final ServerWebExchange request = null;

        // Configure UserRepository.findByUsername(...).
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);
        final Mono<User> userMono = Mono.just(user);
        when(mockUserRepository.findByUsername("username")).thenReturn(userMono);

        when(mockPasswordEncoder.matches("password", "password")).thenReturn(false);

        // Run the test
        final Mono<String> result = authorizationServiceImplUnderTest.authenticate(authRequestDTO, request);

        // Verify the results
    }

    @Test
    void testAuthenticate1_UserRepositoryReturnsNoItem() {
        // Setup
        final AuthRequestDTO authRequestDTO = new AuthRequestDTO("username", "password");
        final ServerWebExchange request = null;
        when(mockUserRepository.findByUsername("username")).thenReturn(Mono.empty());
        when(mockPasswordEncoder.matches("password", "password")).thenReturn(false);

        // Run the test
        final Mono<String> result = authorizationServiceImplUnderTest.authenticate(authRequestDTO, request);

        // Verify the results
    }

    @Test
    void testAuthenticate1_UserRepositoryReturnsError() {
        // Setup
        final AuthRequestDTO authRequestDTO = new AuthRequestDTO("username", "password");
        final ServerWebExchange request = null;

        // Configure UserRepository.findByUsername(...).
        final Mono<User> userMono = Mono.error(new Exception("message"));
        when(mockUserRepository.findByUsername("username")).thenReturn(userMono);

        when(mockPasswordEncoder.matches("password", "password")).thenReturn(false);

        // Run the test
        final Mono<String> result = authorizationServiceImplUnderTest.authenticate(authRequestDTO, request);

        // Verify the results
    }

    @Test
    void testAuthenticate1_PasswordEncoderReturnsTrue() {
        // Setup
        final AuthRequestDTO authRequestDTO = new AuthRequestDTO("username", "password");
        final ServerWebExchange request = null;

        // Configure UserRepository.findByUsername(...).
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);
        final Mono<User> userMono = Mono.just(user);
        when(mockUserRepository.findByUsername("username")).thenReturn(userMono);

        when(mockPasswordEncoder.matches("password", "password")).thenReturn(true);

        // Configure TokenService.generateToken(...).
        final User user1 = new User();
        user1.setId(0L);
        user1.setUsername("username");
        user1.setPassword("password");
        user1.setEmail("email");
        user1.setRole(RoleEnum.ADMIN);
        when(mockTokenService.generateToken(user1, TokenEnum.JWT_AUTHORIZATION_TOKEN)).thenReturn(Mono.just("value"));

        // Run the test
        final Mono<String> result = authorizationServiceImplUnderTest.authenticate(authRequestDTO, request);

        // Verify the results
    }

    @Test
    void testAuthenticate1_TokenServiceReturnsNoItem() {
        // Setup
        final AuthRequestDTO authRequestDTO = new AuthRequestDTO("username", "password");
        final ServerWebExchange request = null;

        // Configure UserRepository.findByUsername(...).
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);
        final Mono<User> userMono = Mono.just(user);
        when(mockUserRepository.findByUsername("username")).thenReturn(userMono);

        when(mockPasswordEncoder.matches("password", "password")).thenReturn(true);

        // Configure TokenService.generateToken(...).
        final User user1 = new User();
        user1.setId(0L);
        user1.setUsername("username");
        user1.setPassword("password");
        user1.setEmail("email");
        user1.setRole(RoleEnum.ADMIN);
        when(mockTokenService.generateToken(user1, TokenEnum.JWT_AUTHORIZATION_TOKEN)).thenReturn(Mono.empty());

        // Run the test
        final Mono<String> result = authorizationServiceImplUnderTest.authenticate(authRequestDTO, request);

        // Verify the results
    }

    @Test
    void testAuthenticate1_TokenServiceReturnsError() {
        // Setup
        final AuthRequestDTO authRequestDTO = new AuthRequestDTO("username", "password");
        final ServerWebExchange request = null;

        // Configure UserRepository.findByUsername(...).
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);
        final Mono<User> userMono = Mono.just(user);
        when(mockUserRepository.findByUsername("username")).thenReturn(userMono);

        when(mockPasswordEncoder.matches("password", "password")).thenReturn(true);

        // Configure TokenService.generateToken(...).
        final User user1 = new User();
        user1.setId(0L);
        user1.setUsername("username");
        user1.setPassword("password");
        user1.setEmail("email");
        user1.setRole(RoleEnum.ADMIN);
        when(mockTokenService.generateToken(user1, TokenEnum.JWT_AUTHORIZATION_TOKEN)).thenReturn(Mono.error(new Exception("message")));

        // Run the test
        final Mono<String> result = authorizationServiceImplUnderTest.authenticate(authRequestDTO, request);

        // Verify the results
    }

    @Test
    void testAuthenticate2() {
        // Setup
        final AuthRequestDTO authRequestDTO = new AuthRequestDTO("username", "password");

        // Configure UserRepository.findByUsername(...).
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);
        final Mono<User> userMono = Mono.just(user);
        when(mockUserRepository.findByUsername("username")).thenReturn(userMono);

        when(mockPasswordEncoder.matches("password", "password")).thenReturn(false);

        // Run the test
        final Mono<String> result = authorizationServiceImplUnderTest.authenticate(authRequestDTO);

        // Verify the results
    }

    @Test
    void testAuthenticate2_UserRepositoryReturnsNoItem() {
        // Setup
        final AuthRequestDTO authRequestDTO = new AuthRequestDTO("username", "password");
        when(mockUserRepository.findByUsername("username")).thenReturn(Mono.empty());
        when(mockPasswordEncoder.matches("password", "password")).thenReturn(false);

        // Run the test
        final Mono<String> result = authorizationServiceImplUnderTest.authenticate(authRequestDTO);

        // Verify the results
    }

    @Test
    void testAuthenticate2_UserRepositoryReturnsError() {
        // Setup
        final AuthRequestDTO authRequestDTO = new AuthRequestDTO("username", "password");

        // Configure UserRepository.findByUsername(...).
        final Mono<User> userMono = Mono.error(new Exception("message"));
        when(mockUserRepository.findByUsername("username")).thenReturn(userMono);

        when(mockPasswordEncoder.matches("password", "password")).thenReturn(false);

        // Run the test
        final Mono<String> result = authorizationServiceImplUnderTest.authenticate(authRequestDTO);

        // Verify the results
    }

    @Test
    void testAuthenticate2_PasswordEncoderReturnsTrue() {
        // Setup
        final AuthRequestDTO authRequestDTO = new AuthRequestDTO("username", "password");

        // Configure UserRepository.findByUsername(...).
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);
        final Mono<User> userMono = Mono.just(user);
        when(mockUserRepository.findByUsername("username")).thenReturn(userMono);

        when(mockPasswordEncoder.matches("password", "password")).thenReturn(true);

        // Configure TokenService.generateToken(...).
        final User user1 = new User();
        user1.setId(0L);
        user1.setUsername("username");
        user1.setPassword("password");
        user1.setEmail("email");
        user1.setRole(RoleEnum.ADMIN);
        when(mockTokenService.generateToken(user1, TokenEnum.JWT_AUTHORIZATION_TOKEN)).thenReturn(Mono.just("value"));

        // Run the test
        final Mono<String> result = authorizationServiceImplUnderTest.authenticate(authRequestDTO);

        // Verify the results
    }

    @Test
    void testAuthenticate2_TokenServiceReturnsNoItem() {
        // Setup
        final AuthRequestDTO authRequestDTO = new AuthRequestDTO("username", "password");

        // Configure UserRepository.findByUsername(...).
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);
        final Mono<User> userMono = Mono.just(user);
        when(mockUserRepository.findByUsername("username")).thenReturn(userMono);

        when(mockPasswordEncoder.matches("password", "password")).thenReturn(true);

        // Configure TokenService.generateToken(...).
        final User user1 = new User();
        user1.setId(0L);
        user1.setUsername("username");
        user1.setPassword("password");
        user1.setEmail("email");
        user1.setRole(RoleEnum.ADMIN);
        when(mockTokenService.generateToken(user1, TokenEnum.JWT_AUTHORIZATION_TOKEN)).thenReturn(Mono.empty());

        // Run the test
        final Mono<String> result = authorizationServiceImplUnderTest.authenticate(authRequestDTO);

        // Verify the results
    }

    @Test
    void testAuthenticate2_TokenServiceReturnsError() {
        // Setup
        final AuthRequestDTO authRequestDTO = new AuthRequestDTO("username", "password");

        // Configure UserRepository.findByUsername(...).
        final User user = new User();
        user.setId(0L);
        user.setUsername("username");
        user.setPassword("password");
        user.setEmail("email");
        user.setRole(RoleEnum.ADMIN);
        final Mono<User> userMono = Mono.just(user);
        when(mockUserRepository.findByUsername("username")).thenReturn(userMono);

        when(mockPasswordEncoder.matches("password", "password")).thenReturn(true);

        // Configure TokenService.generateToken(...).
        final User user1 = new User();
        user1.setId(0L);
        user1.setUsername("username");
        user1.setPassword("password");
        user1.setEmail("email");
        user1.setRole(RoleEnum.ADMIN);
        when(mockTokenService.generateToken(user1, TokenEnum.JWT_AUTHORIZATION_TOKEN)).thenReturn(Mono.error(new Exception("message")));

        // Run the test
        final Mono<String> result = authorizationServiceImplUnderTest.authenticate(authRequestDTO);

        // Verify the results
    }

    @Test
    void testGetCSRFToken() {
        // Setup
        final ServerWebExchange request = null;
        when(mockCsrfTokenRepository.generateToken(any(ServerWebExchange.class))).thenReturn(Mono.just(null));

        // Run the test
        final Mono<CsrfToken> result = authorizationServiceImplUnderTest.getCSRFToken(request);

        // Verify the results
    }

    @Test
    void testGetCSRFToken_ServerCsrfTokenRepositoryReturnsNoItem() {
        // Setup
        final ServerWebExchange request = null;
        when(mockCsrfTokenRepository.generateToken(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // Run the test
        final Mono<CsrfToken> result = authorizationServiceImplUnderTest.getCSRFToken(request);

        // Verify the results
    }

    @Test
    void testGetCSRFToken_ServerCsrfTokenRepositoryReturnsError() {
        // Setup
        final ServerWebExchange request = null;
        when(mockCsrfTokenRepository.generateToken(any(ServerWebExchange.class))).thenReturn(Mono.error(new Exception("message")));

        // Run the test
        final Mono<CsrfToken> result = authorizationServiceImplUnderTest.getCSRFToken(request);

        // Verify the results
    }
}
