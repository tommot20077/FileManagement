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

/**
 * JwtAuthenticationManager JWT 認證管理測試類別。
 *
 * 測試 JwtAuthenticationManager 的 JWT 憑證驗證和認證管理功能，
 * 包括有效憑證的處理、無效憑證的處理和各種異常情況。
 *
 * 前置條件：
 * - 初始化 Mock 依賴項目
 * - 設置 JWT 憑證提供者
 * - 確保認證管理器的正確配置
 *
 * 測試步驟：
 * - 測試有效 JWT 憑證的認證處理
 * - 測試無效 JWT 憑證的處理
 * - 測試憑證驗證過程中的異常情況
 * - 測試憑證聲明的解析和驗證
 *
 * 預期結果：
 * - 有效憑證應成功返回認證物件
 * - 無效憑證應返回空值或異常
 * - 異常情況應被適當處理
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
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


    /**
     * 設置 JWT 憑證提供者的輔助方法。
     *
     * @param lenient 是否使用寬鬆模式
     */
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


    /**
     * 測試驗證有效 JWT 憑證的功能。
     *
     * 測試步驟：
     * - 建立有效的 JWT 憑證和聲明
     * - 設置憑證提供者的模擬行為
     * - 執行認證程序
     * - 驗證返回的認證物件屬性
     *
     * 預期結果：應返回包含用戶 ID 和權限的認證物件
     */
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


    /**
     * 設置 JWT 憑證提供者的輔助方法（非寬鬆模式）。
     */
    private void setupTokenProvider() {
        when(mockTokenStrategy.getTokenProvider(TokenEnum.JWT_AUTHORIZATION_TOKEN)).thenReturn(mockJwtTokenProvider);
    }


    /**
     * 測試驗證無效 JWT 憑證的功能。
     *
     * 測試步驟：
     * - 建立無效的 JWT 憑證
     * - 設置憑證提供者返回空值
     * - 執行認證程序
     * - 驗證返回空的 Mono
     *
     * 預期結果：應返回空的 Mono
     */
    @Test
    @DisplayName("驗證無效的JWT令牌 - 返回空的Mono")
    void authenticate_invalidToken_returnsEmptyMono() {
        setupTokenProvider();
        String token = "invalid.jwt.token";
        Authentication authentication = new TestingAuthenticationToken(null, token);

        when(mockJwtTokenProvider.validateToken(eq(token), any())).thenReturn(Mono.empty());

        StepVerifier.create(jwtAuthenticationManagerUnderTest.authenticate(authentication)).expectNextCount(0).verifyComplete();
    }


    /**
     * 測試憑證驗證過程中拋出異常的處理。
     *
     * 測試步驟：
     * - 設置憑證提供者驗證時拋出異常
     * - 執行認證程序
     * - 驗證返回適當的處理異常
     *
     * 預期結果：應返回 ProcessException
     */
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


    /**
     * 測試憑證聲明解析過程中拋出異常的處理。
     *
     * 測試步驟：
     * - 設置憑證驗證成功但聲明解析失敗
     * - 執行認證程序
     * - 驗證返回適當的處理異常
     *
     * 預期結果：應返回 ProcessException
     */
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


    /**
     * 測試 JWT 憑證中缺少角色聲明的處理。
     *
     * 測試步驟：
     * - 建立不包含角色聲明的 JWT 憑證
     * - 執行認證程序
     * - 驗證返回適當的處理異常
     *
     * 預期結果：應返回 ProcessException
     */
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


    /**
     * 測試認證憑證為空值的處理。
     *
     * 測試步驟：
     * - 建立空憑證的認證物件
     * - 執行認證程序
     * - 驗證返回適當的異常
     *
     * 預期結果：應拋出 ValidationException
     */
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
