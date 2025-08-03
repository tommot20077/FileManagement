package xyz.dowob.filemanagement.repostiory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.dowob.filemanagement.component.manager.JwtAuthenticationManager;
import xyz.dowob.filemanagement.config.properties.SecurityProperties;
import xyz.dowob.filemanagement.customenum.RoleEnum;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.exception.ValidationException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * JWT 安全上下文存儲庫測試類。
 * 
 * 測試 JwtSecurityContextRepository JWT 安全上下文存儲庫的功能和 Spring Security 集成。
 * 驗證 JWT Token 提取、驗證和安全上下文生成，包括從不同來源（Header、Cookie、WebSocket Protocol）的 Token 提取。
 * 支援多種請求類型（API、WEB、WebSocket、OTHER）的處理及遊客用戶模式。
 * 透過模擬測試驗證身份驗證流程、異常處理和 WebSocket 特殊情況處理。
 * 
 * <p>測試涵蓋的主要功能：
 * <ul>
 * <li>load 方法從不同來源提取和驗證 JWT Token</li>
 * <li>save 方法的不支援實現</li>
 * <li>請求類型判斷和路由處理</li>
 * <li>遊客用戶模式的開啟和關閉</li>
 * <li>WebSocket 特殊錯誤處理機制</li>
 * </ul>
 * 
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@SpringBootTest
@ActiveProfiles({"test", "demo", "ci"})
@DisplayName("JwtSecurityContextRepository JWT安全上下文存儲庫測試")
class JwtSecurityContextRepositoryTest {

    @Mock
    private JwtAuthenticationManager authenticationManager;

    @Mock
    private SecurityProperties securityProperties;

    @Mock
    private SecurityProperties.Cookie cookieProperties;

    @Mock
    private SecurityProperties.JwtToken jwtTokenProperties;

    @Mock
    private SecurityProperties.GuestUser guestUserProperties;

    private JwtSecurityContextRepository jwtSecurityContextRepository;
    private User testUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // 配置 SecurityProperties mocks
        when(securityProperties.getCookie()).thenReturn(cookieProperties);
        when(securityProperties.getJwtToken()).thenReturn(jwtTokenProperties);
        when(securityProperties.getGuestUser()).thenReturn(guestUserProperties);
        
        when(cookieProperties.getTokenName()).thenReturn("jwt_token");
        when(jwtTokenProperties.getWebSocketTokenPrefix()).thenReturn("jwt-");
        when(guestUserProperties.isEnable()).thenReturn(true);

        // 創建測試用的 JwtSecurityContextRepository
        jwtSecurityContextRepository = new JwtSecurityContextRepository(authenticationManager, securityProperties);
        
        // 初始化遊客用戶
        jwtSecurityContextRepository.init();

        // 設置測試用戶
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setRole(RoleEnum.USER);
    }

    // ==================== 一般測試 ====================

    /**
     * 測試 load 方法處理 API 請求的 Bearer Token。
     * 
     * 測試從 HTTP Authorization Header 中提取 Bearer Token 進行驗證的基本功能。
     * 
     * 前置條件：
     * - 設定有效的 JWT Token
     * - 模擬 JwtAuthenticationManager 返回成功驗證結果
     * 
     * 測試步驟：
     * - 建立含 Bearer Token 的 API 請求
     * - 呼叫 load 方法進行驗證
     * - 驗證返回的安全上下文
     * 
     * 預期結果：
     * - 成功生成安全上下文
     * - 包含正確的驗證資訊
     */
    @Test
    @DisplayName("一般測試 - load 方法基本功能 - API請求帶Bearer Token")
    void testLoad_apiRequestWithBearerToken() {
        String jwtToken = "test.jwt.token";
        Authentication mockAuth = new UsernamePasswordAuthenticationToken(testUser, null);
        
        when(authenticationManager.authenticate(any())).thenReturn(Mono.just(mockAuth));

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/test")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(jwtSecurityContextRepository.load(exchange))
                .assertNext(securityContext -> {
                    assertNotNull(securityContext);
                    assertNotNull(securityContext.getAuthentication());
                    assertEquals(mockAuth, securityContext.getAuthentication());
                })
                .verifyComplete();

        verify(authenticationManager).authenticate(any());
    }

    @Test
    @DisplayName("一般測試 - load 方法基本功能 - WEB請求從Cookie提取Token")
    void testLoad_webRequestWithCookieToken() {
        String jwtToken = "test.jwt.token";
        Authentication mockAuth = new UsernamePasswordAuthenticationToken(testUser, null);
        
        when(authenticationManager.authenticate(any())).thenReturn(Mono.just(mockAuth));

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/web/test")
                .header(HttpHeaders.COOKIE, "jwt_token=" + jwtToken + "; other=value")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(jwtSecurityContextRepository.load(exchange))
                .assertNext(securityContext -> {
                    assertNotNull(securityContext);
                    assertNotNull(securityContext.getAuthentication());
                })
                .verifyComplete();

        verify(authenticationManager).authenticate(any());
    }

    @Test
    @DisplayName("一般測試 - load 方法基本功能 - WebSocket請求從Protocol提取Token")
    void testLoad_webSocketRequestWithProtocolToken() {
        String jwtToken = "test.jwt.token";
        Authentication mockAuth = new UsernamePasswordAuthenticationToken(testUser, null);
        
        when(authenticationManager.authenticate(any())).thenReturn(Mono.just(mockAuth));

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/ws/test")
                .header("Upgrade", "websocket")
                .header("Sec-WebSocket-Protocol", "jwt-" + jwtToken)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(jwtSecurityContextRepository.load(exchange))
                .assertNext(securityContext -> {
                    assertNotNull(securityContext);
                    assertNotNull(securityContext.getAuthentication());
                })
                .verifyComplete();

        verify(authenticationManager).authenticate(any());
    }

    @Test
    @DisplayName("一般測試 - load 方法 - 遊客用戶處理")
    void testLoad_guestUserHandling() {
        when(guestUserProperties.isEnable()).thenReturn(true);

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/test")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(jwtSecurityContextRepository.load(exchange))
                .assertNext(securityContext -> {
                    assertNotNull(securityContext);
                    Authentication auth = securityContext.getAuthentication();
                    assertNotNull(auth);
                    assertEquals(0L, auth.getPrincipal());
                    assertTrue(auth.getAuthorities().stream()
                            .anyMatch(authority -> authority.getAuthority().equals("VISITOR")));
                })
                .verifyComplete();

        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    @DisplayName("一般測試 - load 方法 - 登錄請求跳過")
    void testLoad_loginRequestSkipped() {
        MockServerHttpRequest request = MockServerHttpRequest
                .post("/guest/login")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(jwtSecurityContextRepository.load(exchange))
                .verifyComplete();

        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    @DisplayName("一般測試 - save 方法不支援")
    void testSave_notSupported() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        SecurityContext context = new SecurityContextImpl();

        Mono<Void> result = jwtSecurityContextRepository.save(exchange, context);
        
        assertNull(result);
    }

    @Test
    @DisplayName("一般測試 - RequestType 枚舉測試")
    void testRequestTypeEnum() {
        // 測試 API 請求
        MockServerHttpRequest apiRequest = MockServerHttpRequest.get("/api/test").build();
        ServerWebExchange apiExchange = MockServerWebExchange.from(apiRequest);
        assertEquals(JwtSecurityContextRepository.RequestType.API, 
                    JwtSecurityContextRepository.RequestType.getRequestType(apiExchange));

        // 測試 WEB 請求
        MockServerHttpRequest webRequest = MockServerHttpRequest.get("/web/test").build();
        ServerWebExchange webExchange = MockServerWebExchange.from(webRequest);
        assertEquals(JwtSecurityContextRepository.RequestType.WEB, 
                    JwtSecurityContextRepository.RequestType.getRequestType(webExchange));

        // 測試 WebSocket 請求
        MockServerHttpRequest wsRequest = MockServerHttpRequest
                .get("/ws/test")
                .header("Upgrade", "websocket")
                .build();
        ServerWebExchange wsExchange = MockServerWebExchange.from(wsRequest);
        assertEquals(JwtSecurityContextRepository.RequestType.WEB_SOCKET, 
                    JwtSecurityContextRepository.RequestType.getRequestType(wsExchange));

        // 測試 OTHER 請求
        MockServerHttpRequest docsRequest = MockServerHttpRequest.get("/docs/api").build();
        ServerWebExchange docsExchange = MockServerWebExchange.from(docsRequest);
        assertEquals(JwtSecurityContextRepository.RequestType.OTHER, 
                    JwtSecurityContextRepository.RequestType.getRequestType(docsExchange));
    }

    @Test
    @DisplayName("一般測試 - 構造函數參數驗證")
    void testConstructorValidation() {
        // 測試正常構造
        assertDoesNotThrow(() -> 
            new JwtSecurityContextRepository(authenticationManager, securityProperties));

        // 測試 Cookie Token Name 為空的情況
        when(cookieProperties.getTokenName()).thenReturn("");
        assertThrows(IllegalArgumentException.class, () -> 
            new JwtSecurityContextRepository(authenticationManager, securityProperties));

        // 測試 WebSocket Token Prefix 為空的情況
        when(cookieProperties.getTokenName()).thenReturn("jwt_token");
        when(jwtTokenProperties.getWebSocketTokenPrefix()).thenReturn("");
        assertThrows(IllegalArgumentException.class, () -> 
            new JwtSecurityContextRepository(authenticationManager, securityProperties));
    }

    @Test
    @DisplayName("一般測試 - 初始化方法驗證")
    void testInitialization() {
        JwtSecurityContextRepository repository = new JwtSecurityContextRepository(authenticationManager, securityProperties);
        
        // 調用初始化方法
        assertDoesNotThrow(() -> repository.init());
    }

    @Test
    @DisplayName("一般測試 - 多種Token來源優先級測試")
    void testTokenSourcePriority() {
        String cookieToken = "cookie.jwt.token";
        String headerToken = "header.jwt.token";
        Authentication mockAuth = new UsernamePasswordAuthenticationToken(testUser, null);
        
        when(authenticationManager.authenticate(any())).thenReturn(Mono.just(mockAuth));

        // WEB請求：Cookie優先於Header
        MockServerHttpRequest webRequest = MockServerHttpRequest
                .get("/web/test")
                .header(HttpHeaders.COOKIE, "jwt_token=" + cookieToken)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + headerToken)
                .build();
        ServerWebExchange webExchange = MockServerWebExchange.from(webRequest);

        StepVerifier.create(jwtSecurityContextRepository.load(webExchange))
                .assertNext(securityContext -> assertNotNull(securityContext))
                .verifyComplete();

        // 驗證使用了Cookie中的Token（應該是第一個被檢查的）
        verify(authenticationManager).authenticate(argThat(auth -> 
            cookieToken.equals(auth.getCredentials())));
    }

    // ==================== 異常測試 ====================

    /**
     * 測試 load 方法處理 JWT Token 驗證失敗情況。
     * 
     * 測試當 JWT Token 驗證失敗時的異常處理機制。
     * 
     * 前置條件：
     * - 設定無效的 JWT Token
     * - 模擬 JwtAuthenticationManager 拋出 ValidationException
     * 
     * 測試步驟：
     * - 建立含無效 JWT Token 的請求
     * - 呼叫 load 方法進行驗證
     * - 觀察異常拋出情況
     * 
     * 預期結果：
     * - 拋出 ValidationException 異常
     * - 不生成任何安全上下文
     */
    @Test
    @DisplayName("異常測試 - JWT Token 驗證失敗")
    void testLoad_jwtValidationFailure() {
        String jwtToken = "invalid.jwt.token";
        
        when(authenticationManager.authenticate(any()))
                .thenReturn(Mono.error(new ValidationException(ValidationException.ErrorCode.JWT_TOKEN_INVALID)));

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/test")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(jwtSecurityContextRepository.load(exchange))
                .expectError(ValidationException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - WebSocket JWT驗證失敗特殊處理")
    void testLoad_webSocketJwtValidationFailure() {
        String jwtToken = "invalid.jwt.token";
        
        when(authenticationManager.authenticate(any()))
                .thenReturn(Mono.error(new ValidationException(ValidationException.ErrorCode.JWT_TOKEN_INVALID)));

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/ws/test")
                .header("Upgrade", "websocket")
                .header("Sec-WebSocket-Protocol", "jwt-" + jwtToken)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(jwtSecurityContextRepository.load(exchange))
                .assertNext(securityContext -> {
                    assertNotNull(securityContext);
                    Authentication auth = securityContext.getAuthentication();
                    assertTrue(auth.getPrincipal().toString().startsWith("ERROR_WEBSOCKET_AUTH_"));
                    assertEquals(ValidationException.ErrorCode.JWT_TOKEN_INVALID, auth.getDetails());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - 遊客用戶禁用時的未授權處理")
    void testLoad_guestUserDisabled() {
        when(guestUserProperties.isEnable()).thenReturn(false);

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/test")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(jwtSecurityContextRepository.load(exchange))
                .verifyComplete(); // 應該返回空，表示未授權
    }

    @Test
    @DisplayName("異常測試 - WebSocket遊客用戶禁用時的錯誤處理")
    void testLoad_webSocketGuestUserDisabled() {
        when(guestUserProperties.isEnable()).thenReturn(false);

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/ws/test")
                .header("Upgrade", "websocket")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(jwtSecurityContextRepository.load(exchange))
                .assertNext(securityContext -> {
                    assertNotNull(securityContext);
                    Authentication auth = securityContext.getAuthentication();
                    assertTrue(auth.getPrincipal().toString().startsWith("ERROR_WEBSOCKET_AUTH_"));
                    assertEquals(ValidationException.ErrorCode.UNAUTHORIZED, auth.getDetails());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - 不支援的請求路徑")
    void testLoad_unsupportedPath() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/unsupported/path")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(jwtSecurityContextRepository.load(exchange))
                .expectError(ValidationException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - 空的Authorization Header")
    void testLoad_emptyAuthorizationHeader() {
        when(guestUserProperties.isEnable()).thenReturn(true);

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/test")
                .header(HttpHeaders.AUTHORIZATION, "")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(jwtSecurityContextRepository.load(exchange))
                .assertNext(securityContext -> {
                    // 應該返回遊客用戶上下文
                    Authentication auth = securityContext.getAuthentication();
                    assertEquals(0L, auth.getPrincipal());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - 無效的Authorization Header格式")
    void testLoad_invalidAuthorizationHeaderFormat() {
        when(guestUserProperties.isEnable()).thenReturn(true);

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/test")
                .header(HttpHeaders.AUTHORIZATION, "Basic invalid")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(jwtSecurityContextRepository.load(exchange))
                .assertNext(securityContext -> {
                    // 應該返回遊客用戶上下文，因為不是Bearer格式
                    Authentication auth = securityContext.getAuthentication();
                    assertEquals(0L, auth.getPrincipal());
                })
                .verifyComplete();
    }

    // ==================== 邊界測試 ====================

    /**
     * 測試 load 方法處理極長 JWT Token。
     * 
     * 測試當 JWT Token 長度非常大時的處理能力。
     * 
     * 前置條件：
     * - 建立長度10000字元的 JWT Token
     * - 模擬成功的驗證結果
     * 
     * 測試步驟：
     * - 使用極長 Token 建立請求
     * - 驗證處理結果
     * 
     * 預期結果：
     * - 方法正常處理極長 Token
     * - 成功生成安全上下文
     */
    @Test
    @DisplayName("邊界測試 - 極長JWT Token")
    void testLoad_veryLongJwtToken() {
        String longJwtToken = "a".repeat(10000);
        Authentication mockAuth = new UsernamePasswordAuthenticationToken(testUser, null);
        
        when(authenticationManager.authenticate(any())).thenReturn(Mono.just(mockAuth));

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/test")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + longJwtToken)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(jwtSecurityContextRepository.load(exchange))
                .assertNext(securityContext -> assertNotNull(securityContext))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 複雜Cookie字符串解析")
    void testLoad_complexCookieParsing() {
        String jwtToken = "complex.jwt.token";
        Authentication mockAuth = new UsernamePasswordAuthenticationToken(testUser, null);
        
        when(authenticationManager.authenticate(any())).thenReturn(Mono.just(mockAuth));

        String complexCookie = "sessionId=abc123; jwt_token=" + jwtToken + "; other=value; path=/; secure";
        
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/web/test")
                .header(HttpHeaders.COOKIE, complexCookie)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(jwtSecurityContextRepository.load(exchange))
                .assertNext(securityContext -> assertNotNull(securityContext))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 空Cookie處理")
    void testLoad_emptyCookieHandling() {
        when(guestUserProperties.isEnable()).thenReturn(true);

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/web/test")
                .header(HttpHeaders.COOKIE, "")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(jwtSecurityContextRepository.load(exchange))
                .assertNext(securityContext -> {
                    // 應該返回遊客用戶上下文
                    Authentication auth = securityContext.getAuthentication();
                    assertEquals(0L, auth.getPrincipal());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - WebSocket Protocol邊界值")
    void testLoad_webSocketProtocolBoundaryValues() {
        when(guestUserProperties.isEnable()).thenReturn(true);
        
        // 測試空Protocol
        MockServerHttpRequest request1 = MockServerHttpRequest
                .get("/ws/test")
                .header("Upgrade", "websocket")
                .header("Sec-WebSocket-Protocol", "")
                .build();
        ServerWebExchange exchange1 = MockServerWebExchange.from(request1);
        
        StepVerifier.create(jwtSecurityContextRepository.load(exchange1))
                .assertNext(securityContext -> {
                    Authentication auth = securityContext.getAuthentication();
                    assertEquals(0L, auth.getPrincipal());
                })
                .verifyComplete();

        // 測試只有前綴沒有Token - 會嘗試使用空字符串驗證但應該Mock返回錯誤
        MockServerHttpRequest request2 = MockServerHttpRequest
                .get("/ws/test")
                .header("Upgrade", "websocket")
                .header("Sec-WebSocket-Protocol", "jwt-")
                .build();
        ServerWebExchange exchange2 = MockServerWebExchange.from(request2);

        // Mock空token的驗證失敗
        when(authenticationManager.authenticate(argThat(auth -> 
            "".equals(auth.getCredentials()))))
            .thenReturn(Mono.error(new ValidationException(ValidationException.ErrorCode.JWT_TOKEN_INVALID)));

        StepVerifier.create(jwtSecurityContextRepository.load(exchange2))
                .assertNext(securityContext -> {
                    Authentication auth = securityContext.getAuthentication();
                    // WebSocket錯誤處理應該返回錯誤認證而非遊客用戶
                    assertTrue(auth.getPrincipal().toString().startsWith("ERROR_WEBSOCKET_AUTH_"));
                    assertEquals(ValidationException.ErrorCode.JWT_TOKEN_INVALID, auth.getDetails());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 請求路徑邊界情況")
    void testLoad_requestPathBoundaryConditions() {
        // 測試根路徑
        MockServerHttpRequest request1 = MockServerHttpRequest.get("/").build();
        ServerWebExchange exchange1 = MockServerWebExchange.from(request1);

        StepVerifier.create(jwtSecurityContextRepository.load(exchange1))
                .expectError(ValidationException.class)
                .verify();

        // 測試非常長的路徑
        String longPath = "/api/" + "a".repeat(10000);
        MockServerHttpRequest request2 = MockServerHttpRequest.get(longPath).build();
        ServerWebExchange exchange2 = MockServerWebExchange.from(request2);

        when(guestUserProperties.isEnable()).thenReturn(true);
        
        StepVerifier.create(jwtSecurityContextRepository.load(exchange2))
                .assertNext(securityContext -> {
                    Authentication auth = securityContext.getAuthentication();
                    assertEquals(0L, auth.getPrincipal());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 併發請求處理")
    void testLoad_concurrentRequests() {
        String jwtToken = "concurrent.test.token";
        Authentication mockAuth = new UsernamePasswordAuthenticationToken(testUser, null);
        
        when(authenticationManager.authenticate(any())).thenReturn(Mono.just(mockAuth));

        // 創建多個並發請求
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/test")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // 併發執行多個load操作
        reactor.core.publisher.Flux<SecurityContext> concurrentLoads = 
                reactor.core.publisher.Flux.range(1, 10)
                        .flatMap(i -> jwtSecurityContextRepository.load(exchange));

        StepVerifier.create(concurrentLoads)
                .expectNextCount(10)
                .verifyComplete();

        verify(authenticationManager, times(10)).authenticate(any());
    }

    @Test
    @DisplayName("邊界測試 - 特殊字符處理")
    void testLoad_specialCharacterHandling() {
        String specialToken = "token.with.special@#$%^&*()characters";
        Authentication mockAuth = new UsernamePasswordAuthenticationToken(testUser, null);
        
        when(authenticationManager.authenticate(any())).thenReturn(Mono.just(mockAuth));

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/test")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + specialToken)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(jwtSecurityContextRepository.load(exchange))
                .assertNext(securityContext -> assertNotNull(securityContext))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - RequestType getName 方法")
    void testRequestTypeGetName() {
        assertEquals("API協議", JwtSecurityContextRepository.RequestType.API.getName());
        assertEquals("WEB協議", JwtSecurityContextRepository.RequestType.WEB.getName());
        assertEquals("WebSocket協議", JwtSecurityContextRepository.RequestType.WEB_SOCKET.getName());
        assertEquals("其他協議", JwtSecurityContextRepository.RequestType.OTHER.getName());
    }

    @Test
    @DisplayName("邊界測試 - 多層Cookie嵌套解析")
    void testLoad_nestedCookieParsing() {
        String jwtToken = "nested.jwt.token";
        Authentication mockAuth = new UsernamePasswordAuthenticationToken(testUser, null);
        
        when(authenticationManager.authenticate(any())).thenReturn(Mono.just(mockAuth));

        // 包含相似名稱的Cookie - 確保jwt_token=值不會和其他類似名稱混淆
        String nestedCookie = "jwt_token_fake=fake; jwt_token=" + jwtToken + "; jwt_token_suffix=suffix";
        
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/web/test")
                .header(HttpHeaders.COOKIE, nestedCookie)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(jwtSecurityContextRepository.load(exchange))
                .assertNext(securityContext -> assertNotNull(securityContext))
                .verifyComplete();

        // 驗證使用了正確的Token
        verify(authenticationManager).authenticate(argThat(auth -> 
            jwtToken.equals(auth.getCredentials())));
    }

    @Test
    @DisplayName("邊界測試 - SecurityContext完整性驗證")
    void testLoad_securityContextIntegrity() {
        String jwtToken = "integrity.test.token";
        Authentication mockAuth = new UsernamePasswordAuthenticationToken(testUser, null);
        
        when(authenticationManager.authenticate(any())).thenReturn(Mono.just(mockAuth));

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/test")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(jwtSecurityContextRepository.load(exchange))
                .assertNext(securityContext -> {
                    assertNotNull(securityContext);
                    assertTrue(securityContext instanceof SecurityContextImpl);
                    assertNotNull(securityContext.getAuthentication());
                    assertEquals(mockAuth, securityContext.getAuthentication());
                })
                .verifyComplete();
    }
}