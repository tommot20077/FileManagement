package xyz.dowob.filemanagement.component.manager;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.dowob.filemanagement.component.provider.providerImplement.JwtTokenProviderImpl;
import xyz.dowob.filemanagement.component.strategy.TokenStrategy;
import xyz.dowob.filemanagement.customenum.TokenEnum;
import xyz.dowob.filemanagement.exception.ProcessException;
import xyz.dowob.filemanagement.exception.ValidationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationManager 邏輯處理測試")
class JwtAuthenticationManagerTest {

    @Mock
    private TokenStrategy mockTokenStrategy;

    @Mock
    private JwtTokenProviderImpl mockJwtTokenProvider;

    private JwtAuthenticationManager jwtAuthenticationManagerUnderTest;


    @BeforeEach
    void setUp() {
        jwtAuthenticationManagerUnderTest = new JwtAuthenticationManager(mockTokenStrategy);
    }


    private void setupTokenProvider(boolean lenient) {
        if (lenient) {
            org.mockito.Mockito
                    .lenient()
                    .when(mockTokenStrategy.getTokenProvider(TokenEnum.JWT_AUTHORIZATION_TOKEN))
                    .thenReturn(mockJwtTokenProvider);
        } else {
            when(mockTokenStrategy.getTokenProvider(TokenEnum.JWT_AUTHORIZATION_TOKEN)).thenReturn(mockJwtTokenProvider);
        }
    }


    @Test
    @DisplayName("驗證有效的JWT令牌 - 返回帶有用戶ID和權限的驗證對象")
    void authenticate_validToken_returnsAuthenticationWithUserIdAndAuthorities() {
        setupTokenProvider();
        String token = "valid.jwt.token";
        Long userId = 123L;
        String role = "ROLE_USER";
        Authentication authentication = new TestingAuthenticationToken(null, token);
        Claims claims = Jwts.claims().add("role", role).build();

        when(mockJwtTokenProvider.validateToken(eq(token), any())).thenReturn(Mono.just(userId));
        when(mockJwtTokenProvider.getClaimsFromToken(token)).thenReturn(Mono.just(claims));

        StepVerifier.create(jwtAuthenticationManagerUnderTest.authenticate(authentication)).assertNext(result -> {
            assertThat(result).isInstanceOf(UsernamePasswordAuthenticationToken.class);
            assertThat(result.getPrincipal()).isEqualTo(userId);
            assertThat(result.getCredentials()).isEqualTo(token);
            assertThat(result.getAuthorities().iterator().next()).isEqualTo(new SimpleGrantedAuthority(role));
        }).verifyComplete();
    }


    private void setupTokenProvider() {
        when(mockTokenStrategy.getTokenProvider(TokenEnum.JWT_AUTHORIZATION_TOKEN)).thenReturn(mockJwtTokenProvider);
    }


    @Test
    @DisplayName("驗證無效的JWT令牌 - 返回空的Mono")
    void authenticate_invalidToken_returnsEmptyMono() {
        setupTokenProvider();
        String token = "invalid.jwt.token";
        Authentication authentication = new TestingAuthenticationToken(null, token);

        when(mockJwtTokenProvider.validateToken(eq(token), any())).thenReturn(Mono.empty());

        StepVerifier.create(jwtAuthenticationManagerUnderTest.authenticate(authentication)).expectNextCount(0).verifyComplete();
    }


    @Test
    @DisplayName("驗證令牌時發生異常 - 返回 ProcessException")
    void authenticate_tokenValidationThrowsException_returnsProperError() {
        setupTokenProvider();
        String token = "invalid.jwt.token";
        Authentication authentication = new TestingAuthenticationToken(null, token);

        when(mockJwtTokenProvider.validateToken(eq(token), any())).thenReturn(Mono.error(new RuntimeException("Token validation failed")));

        StepVerifier
                .create(jwtAuthenticationManagerUnderTest.authenticate(authentication))
                .verifyErrorMatches(e -> e instanceof ProcessException processException && processException.getErrorCode() == ProcessException.ErrorCode.AUTHENTICATION_ERROR);
    }


    @Test
    @DisplayName("解析令牌聲明時發生異常 - 返回 ProcessException")
    void authenticate_claimsExtractionThrowsException_returnsProperError() {
        setupTokenProvider();
        String token = "valid.jwt.token";
        Long userId = 123L;
        Authentication authentication = new TestingAuthenticationToken(null, token);

        when(mockJwtTokenProvider.validateToken(eq(token), any())).thenReturn(Mono.just(userId));
        when(mockJwtTokenProvider.getClaimsFromToken(token)).thenReturn(Mono.error(new RuntimeException("Claims extraction failed")));

        StepVerifier
                .create(jwtAuthenticationManagerUnderTest.authenticate(authentication))
                .verifyErrorMatches(e -> e instanceof ProcessException processException && processException.getErrorCode() == ProcessException.ErrorCode.AUTHENTICATION_ERROR);
    }


    @Test
    @DisplayName("JWT令牌中缺少role聲明 - 返回ProcessException")
    void authenticate_tokenWithoutRoleClaim_returnsProcessException() {
        setupTokenProvider();
        String token = "valid.jwt.token";
        Long userId = 123L;
        Authentication authentication = new TestingAuthenticationToken(null, token);
        Claims claims = Jwts.claims().build();

        when(mockJwtTokenProvider.validateToken(eq(token), any())).thenReturn(Mono.just(userId));
        when(mockJwtTokenProvider.getClaimsFromToken(token)).thenReturn(Mono.just(claims));

        StepVerifier
                .create(jwtAuthenticationManagerUnderTest.authenticate(authentication))
                .verifyErrorMatches(e -> e instanceof ProcessException processException && processException.getErrorCode() == ProcessException.ErrorCode.AUTHENTICATION_ERROR);
    }


    @Test
    @DisplayName("JWT令牌中的role不是字符串類型 - 返回 ProcessException")
    void authenticate_tokenWithInvalidRoleType_returnsProcessException() {
        setupTokenProvider();
        String token = "valid.jwt.token";
        Long userId = 123L;
        Authentication authentication = new TestingAuthenticationToken(null, token);
        Claims claims = Jwts.claims().add("role", 123).build();

        when(mockJwtTokenProvider.validateToken(eq(token), any())).thenReturn(Mono.just(userId));
        when(mockJwtTokenProvider.getClaimsFromToken(token)).thenReturn(Mono.just(claims));

        StepVerifier
                .create(jwtAuthenticationManagerUnderTest.authenticate(authentication))
                .verifyErrorMatches(e -> e instanceof ProcessException processException && processException.getErrorCode() == ProcessException.ErrorCode.AUTHENTICATION_ERROR);
    }


    @Test
    @DisplayName("驗證憑證為空 - 返回空的Mono")
    void authenticate_nullCredentials_returnsEmptyMono() {
        Authentication authentication = new TestingAuthenticationToken(null, null);

        StepVerifier.create(jwtAuthenticationManagerUnderTest.authenticate(authentication)).expectNextCount(0).verifyError(ValidationException.class);
    }


    @Test
    @DisplayName("驗證成功但無法獲取令牌聲明 - 返回空的Mono")
    void authenticate_validTokenButEmptyClaims_returnsEmptyMono() {
        setupTokenProvider();
        String token = "valid.jwt.token";
        Long userId = 123L;
        Authentication authentication = new TestingAuthenticationToken(null, token);

        when(mockJwtTokenProvider.validateToken(eq(token), any())).thenReturn(Mono.just(userId));
        when(mockJwtTokenProvider.getClaimsFromToken(token)).thenReturn(Mono.empty());

        StepVerifier.create(jwtAuthenticationManagerUnderTest.authenticate(authentication)).expectNextCount(0).verifyComplete();
    }


    @Test
    @DisplayName("JWT令牌中的role為空字符串 - 返回 ProcessException")
    void authenticate_tokenWithEmptyRole_returnsProcessException() {
        setupTokenProvider();
        String token = "valid.jwt.token";
        Long userId = 123L;
        Authentication authentication = new TestingAuthenticationToken(null, token);
        Claims claims = Jwts.claims().add("role", "").build();

        when(mockJwtTokenProvider.validateToken(eq(token), any())).thenReturn(Mono.just(userId));
        when(mockJwtTokenProvider.getClaimsFromToken(token)).thenReturn(Mono.just(claims));

        StepVerifier
                .create(jwtAuthenticationManagerUnderTest.authenticate(authentication))
                .verifyErrorMatches(e -> e instanceof ProcessException processException && processException.getErrorCode() == ProcessException.ErrorCode.AUTHENTICATION_ERROR);
    }


    @Test
    @DisplayName("JWT令牌中的role只包含空格 - 返回 ProcessException")
    void authenticate_tokenWithWhitespaceRole_returnsProcessException() {
        setupTokenProvider();
        String token = "valid.jwt.token";
        Long userId = 123L;
        Authentication authentication = new TestingAuthenticationToken(null, token);
        Claims claims = Jwts.claims().add("role", "   ").build();

        when(mockJwtTokenProvider.validateToken(eq(token), any())).thenReturn(Mono.just(userId));
        when(mockJwtTokenProvider.getClaimsFromToken(token)).thenReturn(Mono.just(claims));

        StepVerifier
                .create(jwtAuthenticationManagerUnderTest.authenticate(authentication))
                .verifyErrorMatches(e -> e instanceof ProcessException processException && processException.getErrorCode() == ProcessException.ErrorCode.AUTHENTICATION_ERROR);
    }


    @Test
    @DisplayName("無效的TokenEnum類型 - 返回 ProcessException")
    void authenticate_invalidTokenProvider_returnsProcessException() {
        String token = "valid.jwt.token";
        Authentication authentication = new TestingAuthenticationToken(null, token);
        when(mockTokenStrategy.getTokenProvider(TokenEnum.JWT_AUTHORIZATION_TOKEN)).thenReturn(null);

        StepVerifier
                .create(jwtAuthenticationManagerUnderTest.authenticate(authentication))
                .verifyErrorMatches(e -> e instanceof ProcessException processException && processException.getErrorCode() == ProcessException.ErrorCode.AUTHENTICATION_ERROR);
    }


    @Test
    @DisplayName("用戶ID不是Long類型 - 返回 ProcessException")
    void authenticate_invalidUserIdType_returnsProcessException() {
        setupTokenProvider();
        String token = "valid.jwt.token";
        Authentication authentication = new TestingAuthenticationToken(null, token);
        when(mockJwtTokenProvider.validateToken(eq(token), any())).thenAnswer(invocation -> Mono.just(new Object()));

        StepVerifier
                .create(jwtAuthenticationManagerUnderTest.authenticate(authentication))
                .verifyErrorMatches(e -> e instanceof ProcessException processException && processException.getErrorCode() == ProcessException.ErrorCode.AUTHENTICATION_ERROR);
    }

    /*
    @Override
    @RecordLevel(LogLevelEnum.DEBUG)
    public Mono<Authentication> authenticate(Authentication authentication) {
        if (authentication.getCredentials() == null) {
            return Mono.empty();
        }
        String token = authentication.getCredentials().toString();
        TokenProvider provider = tokenStrategy.getTokenProvider(TokenEnum.JWT_AUTHORIZATION_TOKEN);
        if (!(provider instanceof JwtTokenProviderImpl jwtTokenProvider)) {
            return Mono.empty();
        }

        return Mono.defer(() -> jwtTokenProvider
                .validateToken(token, null)
                .filter(Objects::nonNull)
                .cast(Long.class)
                .flatMap(id -> jwtTokenProvider.getClaimsFromToken(token).filter(claims -> {
                    Object roleObj = claims.get("role");
                    if (!(roleObj instanceof String role)) {
                        return false;
                    }
                    return !role.trim().isEmpty();
                }).map(claims -> claims.get("role")).map(roles -> {
                    List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(((String) roles).trim()));
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(id, token, authorities);
                    return (Authentication) auth;
                }))
                .onErrorResume(e -> Mono.empty()));
    }
     */
}
