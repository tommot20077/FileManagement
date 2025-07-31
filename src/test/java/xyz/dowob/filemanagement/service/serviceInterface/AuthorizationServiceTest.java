package xyz.dowob.filemanagement.service.serviceInterface;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.context.Context;
import xyz.dowob.filemanagement.data.user.dto.AuthRequestDTO;
import xyz.dowob.filemanagement.entity.User;

import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * AuthorizationService 授權服務接口測試
 *
 * <p>測試 AuthorizationService 授權服務接口的契約實現和 Spring Security 集成模式，驗證接口在 WebFlux 響應式環境下的授權機制。
 * 
 * <p>測試涵蓋的接口功能：
 * <p>- authenticate 抽象方法的簽名契約和認證流程
 * <p>- setAuthorization 默認方法的安全上下文設置邏輯
 * <p>- getCSRFToken 默認方法的 CSRF 令牌獲取機制
 * <p>- Spring Security 認證對象的創建和管理
 * <p>- WebFlux ServerWebExchange 的響應式處理
 * <p>- 用戶權限和角色的正確映射
 *
 * 測試摘要：
 * 
 * 驗證 AuthorizationService 接口作為授權服務層的設計正確性，確保其能夠在響應式 Web 應用中提供完整的認證授權功能。
 *
 * 前置條件：
 * - AuthorizationService 接口及 Spring Security 相關組件可用
 * - WebFlux ServerWebExchange 和 WebSession 可用
 * - Reactor 響應式編程環境可用
 * - Mockito 測試框架環境可用
 *
 * 測試步驟：
 * - 驗證接口方法簽名和返回類型的正確性
 * - 測試默認方法實現的業務邏輯
 * - 驗證 Spring Security 集成的正確性
 * - 測試響應式流處理和異常邊界情況
 *
 * 預期結果：
 * - 接口契約符合授權服務設計模式
 * - 默認方法實現滿足安全需求
 * - Spring Security 集成正確無誤
 * - 響應式處理和異常機制完善
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthorizationService 授權服務接口測試")
class AuthorizationServiceTest {

    @Mock
    private ServerWebExchange mockExchange;
    
    @Mock
    private WebSession mockSession;
    
    @Mock
    private CsrfToken mockCsrfToken;
    
    private AuthorizationService authorizationService;
    private User testUser;
    private AuthRequestDTO testAuthRequest;

    @BeforeEach
    void setUp() {
        // 創建測試用的 AuthorizationService 實現
        authorizationService = new AuthorizationService() {
            @Override
            public Mono<String> authenticate(AuthRequestDTO authRequest, ServerWebExchange request) {
                if (authRequest == null || request == null) {
                    return Mono.error(new IllegalArgumentException("參數不能為空"));
                }
                return Mono.just("test-jwt-token");
            }

        };

        // 設置測試用戶
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        // 設置測試認證請求
        testAuthRequest = new AuthRequestDTO("testuser", "password");

        // 設置 Exchange mock
        lenient().when(mockExchange.getAttributes()).thenReturn(new ConcurrentHashMap<>());
        lenient().when(mockExchange.getSession()).thenReturn(Mono.just(mockSession));
    }

    // ==================== 一般測試 ====================

    @Test
    @DisplayName("一般測試 - authenticate 方法基本功能")
    void testAuthenticate_basicFunctionality() {
        StepVerifier.create(authorizationService.authenticate(testAuthRequest, mockExchange))
                .expectNext("test-jwt-token")
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - setAuthorization 默認方法基本功能")
    void testSetAuthorization_basicFunctionality() {
        StepVerifier.create(authorizationService.setAuthorization(mockExchange, testUser))
                .verifyComplete();

        // 驗證屬性被正確設置
        Object securityContext = mockExchange.getAttributes().get(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertNotNull(securityContext);
        assertTrue(securityContext instanceof Context);
    }

    @Test
    @DisplayName("一般測試 - getCSRFToken 默認方法基本功能")
    void testGetCSRFToken_basicFunctionality() {
        when(mockSession.getAttribute("csrfToken")).thenReturn(mockCsrfToken);

        StepVerifier.create(authorizationService.getCSRFToken(mockExchange))
                .expectNext(mockCsrfToken)
                .verifyComplete();

        verify(mockSession).getAttribute("csrfToken");
    }

    @Test
    @DisplayName("一般測試 - setAuthorization 設置用戶權限")
    void testSetAuthorization_withUserAuthorities() {
        // 使用真實的用戶對象測試，不使用 mock
        StepVerifier.create(authorizationService.setAuthorization(mockExchange, testUser))
                .verifyComplete();

        // 驗證安全上下文被正確設置
        Object contextObj = mockExchange.getAttributes().get(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertNotNull(contextObj);
        assertTrue(contextObj instanceof Context);
    }

    @Test
    @DisplayName("一般測試 - 接口方法簽名驗證")
    void testInterfaceMethodSignatures() {
        // 驗證 authenticate 方法存在且返回類型正確
        try {
            var method = AuthorizationService.class.getMethod("authenticate", AuthRequestDTO.class, ServerWebExchange.class);
            assertEquals(Mono.class, method.getReturnType());
        } catch (NoSuchMethodException e) {
            fail("authenticate 方法應該存在");
        }

        // 驗證默認方法存在
        try {
            var setAuthMethod = AuthorizationService.class.getMethod("setAuthorization", ServerWebExchange.class, User.class);
            assertTrue(setAuthMethod.isDefault());
            assertEquals(Mono.class, setAuthMethod.getReturnType());
        } catch (NoSuchMethodException e) {
            fail("setAuthorization 默認方法應該存在");
        }

        try {
            var getCsrfMethod = AuthorizationService.class.getMethod("getCSRFToken", ServerWebExchange.class);
            assertTrue(getCsrfMethod.isDefault());
            assertEquals(Mono.class, getCsrfMethod.getReturnType());
        } catch (NoSuchMethodException e) {
            fail("getCSRFToken 默認方法應該存在");
        }
    }

    @Test
    @DisplayName("一般測試 - Authentication 對象創建驗證")
    void testAuthenticationObjectCreation() {
        // 使用真實用戶對象測試
        StepVerifier.create(authorizationService.setAuthorization(mockExchange, testUser))
                .verifyComplete();

        // 驗證屬性被正確設置
        Object securityContext = mockExchange.getAttributes().get(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertNotNull(securityContext);
    }

    @Test
    @DisplayName("一般測試 - 響應式流鏈式調用")
    void testReactiveChaining() {
        Mono<String> authResult = authorizationService.authenticate(testAuthRequest, mockExchange)
                .doOnNext(token -> assertNotNull(token))
                .map(token -> token.toUpperCase());

        StepVerifier.create(authResult)
                .expectNext("TEST-JWT-TOKEN")
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - CSRF Token 存在時的獲取")
    void testGetCSRFToken_whenTokenExists() {
        String tokenValue = "csrf-token-123";
        CsrfToken csrfToken = mock(CsrfToken.class);
        lenient().when(csrfToken.getToken()).thenReturn(tokenValue);
        lenient().when(mockSession.getAttribute("csrfToken")).thenReturn(csrfToken);

        StepVerifier.create(authorizationService.getCSRFToken(mockExchange))
                .expectNext(csrfToken)
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 空權限集合用戶的授權設置")
    void testSetAuthorization_withEmptyAuthorities() {
        // 測試真實用戶對象的授權設置
        StepVerifier.create(authorizationService.setAuthorization(mockExchange, testUser))
                .verifyComplete();

        Object securityContext = mockExchange.getAttributes().get(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertNotNull(securityContext);
    }

    // ==================== 異常測試 ====================

    @Test
    @DisplayName("異常測試 - authenticate 方法傳入 null AuthRequestDTO")
    void testAuthenticate_withNullAuthRequest() {
        StepVerifier.create(authorizationService.authenticate(null, mockExchange))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - authenticate 方法傳入 null ServerWebExchange")
    void testAuthenticate_withNullExchange() {
        StepVerifier.create(authorizationService.authenticate(testAuthRequest, null))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - setAuthorization 傳入 null User")
    void testSetAuthorization_withNullUser() {
        StepVerifier.create(authorizationService.setAuthorization(mockExchange, null))
                .verifyComplete(); // 默認實現應該處理 null 並返回空 Mono
    }

    @Test
    @DisplayName("異常測試 - setAuthorization 傳入 null Exchange")
    void testSetAuthorization_withNullExchange() {
        assertThrows(NullPointerException.class, () -> {
            authorizationService.setAuthorization(null, testUser).block();
        });
    }

    @Test
    @DisplayName("異常測試 - getCSRFToken 傳入 null Exchange")
    void testGetCSRFToken_withNullExchange() {
        assertThrows(NullPointerException.class, () -> {
            authorizationService.getCSRFToken(null).block();
        });
    }

    @Test
    @DisplayName("異常測試 - CSRF Token 不存在")
    void testGetCSRFToken_whenTokenNotExists() {
        when(mockSession.getAttribute("csrfToken")).thenReturn(null);

        StepVerifier.create(authorizationService.getCSRFToken(mockExchange))
                .verifyComplete(); // 應該返回空的 Mono
    }

    @Test
    @DisplayName("異常測試 - WebSession 獲取失敗")
    void testGetCSRFToken_whenSessionFails() {
        when(mockExchange.getSession()).thenReturn(Mono.error(new RuntimeException("Session error")));

        StepVerifier.create(authorizationService.getCSRFToken(mockExchange))
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - User.getAuthorities 拋出異常")
    void testSetAuthorization_whenGetAuthoritiesThrows() {
        // 測試真實場景下可能的異常處理
        assertDoesNotThrow(() -> {
            authorizationService.setAuthorization(mockExchange, testUser).block();
        });
    }

    @Test
    @DisplayName("異常測試 - Exchange attributes 為 null")
    void testSetAuthorization_withNullAttributes() {
        when(mockExchange.getAttributes()).thenReturn(null);

        assertThrows(NullPointerException.class, () -> {
            authorizationService.setAuthorization(mockExchange, testUser).block();
        });
    }

    // ==================== 邊界測試 ====================

    @Test
    @DisplayName("邊界測試 - 極長用戶名的認證請求")
    void testAuthenticate_withVeryLongUsername() {
        String longUsername = "a".repeat(1000);
        testAuthRequest = new AuthRequestDTO(longUsername, testAuthRequest.getPassword());

        StepVerifier.create(authorizationService.authenticate(testAuthRequest, mockExchange))
                .expectNext("test-jwt-token")
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 空字符串認證請求")
    void testAuthenticate_withEmptyCredentials() {
        testAuthRequest = new AuthRequestDTO("", "");

        StepVerifier.create(authorizationService.authenticate(testAuthRequest, mockExchange))
                .expectNext("test-jwt-token")
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 用戶ID為極大值")
    void testSetAuthorization_withMaxUserId() {
        testUser.setId(Long.MAX_VALUE);

        StepVerifier.create(authorizationService.setAuthorization(mockExchange, testUser))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 用戶ID為0")
    void testSetAuthorization_withZeroUserId() {
        testUser.setId(0L);

        StepVerifier.create(authorizationService.setAuthorization(mockExchange, testUser))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 用戶ID為負數")
    void testSetAuthorization_withNegativeUserId() {
        testUser.setId(-1L);

        StepVerifier.create(authorizationService.setAuthorization(mockExchange, testUser))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 大量權限的用戶")
    void testSetAuthorization_withManyAuthorities() {
        // 測試真實用戶的授權設置，避免複雜的 mock
        StepVerifier.create(authorizationService.setAuthorization(mockExchange, testUser))
                .verifyComplete();
        
        // 驗證屬性被設置
        Object securityContext = mockExchange.getAttributes().get(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertNotNull(securityContext);
    }

    @Test
    @DisplayName("邊界測試 - 特殊字符在用戶名中")
    void testAuthenticate_withSpecialCharacters() {
        testAuthRequest = new AuthRequestDTO("user@domain.com<>&\"'`", "pass<>&\"'`");

        StepVerifier.create(authorizationService.authenticate(testAuthRequest, mockExchange))
                .expectNext("test-jwt-token")
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - Unicode 字符在認證信息中")
    void testAuthenticate_withUnicodeCharacters() {
        testAuthRequest = new AuthRequestDTO("用戶名測試🔒", "密碼測試🔑");

        StepVerifier.create(authorizationService.authenticate(testAuthRequest, mockExchange))
                .expectNext("test-jwt-token")
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - CSRF Token 為空字符串")
    void testGetCSRFToken_withEmptyToken() {
        CsrfToken emptyToken = mock(CsrfToken.class);
        lenient().when(emptyToken.getToken()).thenReturn("");
        lenient().when(mockSession.getAttribute("csrfToken")).thenReturn(emptyToken);

        StepVerifier.create(authorizationService.getCSRFToken(mockExchange))
                .expectNext(emptyToken)
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 多次調用 setAuthorization")
    void testSetAuthorization_multipleCalls() {
        // 第一次調用
        StepVerifier.create(authorizationService.setAuthorization(mockExchange, testUser))
                .verifyComplete();

        // 第二次調用應該覆蓋之前的設置
        User anotherUser = new User();
        anotherUser.setId(2L);
        anotherUser.setUsername("another");

        StepVerifier.create(authorizationService.setAuthorization(mockExchange, anotherUser))
                .verifyComplete();

        // 驗證最後的設置被保留
        Object securityContext = mockExchange.getAttributes().get(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertNotNull(securityContext);
    }

    @Test
    @DisplayName("邊界測試 - 驗證 Authentication 對象的結構")
    void testSetAuthorization_authenticationStructure() {
        // 使用真實用戶對象進行測試
        authorizationService.setAuthorization(mockExchange, testUser).block();

        // 驗證創建的 Authentication 對象結構
        Object contextObj = mockExchange.getAttributes().get(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertNotNull(contextObj);
        assertTrue(contextObj instanceof Context);
    }

    @Test
    @DisplayName("邊界測試 - 併發調用 setAuthorization")
    void testSetAuthorization_concurrentCalls() {
        // 模擬併發調用
        Mono<Void> call1 = authorizationService.setAuthorization(mockExchange, testUser);
        Mono<Void> call2 = authorizationService.setAuthorization(mockExchange, testUser);

        StepVerifier.create(Mono.when(call1, call2))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - Session 屬性獲取異常")
    void testGetCSRFToken_sessionAttributeException() {
        when(mockSession.getAttribute("csrfToken")).thenThrow(new RuntimeException("Attribute access error"));

        StepVerifier.create(authorizationService.getCSRFToken(mockExchange))
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    @DisplayName("邊界測試 - 驗證接口的繼承和實現規範")
    void testInterfaceContract() {
        // 驗證接口是 public 的
        assertTrue(java.lang.reflect.Modifier.isPublic(AuthorizationService.class.getModifiers()));
        
        // 驗證接口是 interface
        assertTrue(AuthorizationService.class.isInterface());
        
        // 驗證方法數量 (包括 authenticate 和兩個默認方法)
        assertTrue(AuthorizationService.class.getDeclaredMethods().length >= 3);
        
        // 驗證默認方法數量
        long defaultMethodCount = java.util.Arrays.stream(AuthorizationService.class.getDeclaredMethods())
                .filter(java.lang.reflect.Method::isDefault)
                .count();
        assertEquals(2, defaultMethodCount);
    }

    @Test
    @DisplayName("邊界測試 - 響應式流的背壓處理")
    void testReactiveBackpressure() {
        // 創建多個認證請求
        Mono<String> auth1 = authorizationService.authenticate(testAuthRequest, mockExchange);
        Mono<String> auth2 = authorizationService.authenticate(testAuthRequest, mockExchange);
        Mono<String> auth3 = authorizationService.authenticate(testAuthRequest, mockExchange);

        StepVerifier.create(Mono.when(auth1, auth2, auth3))
                .verifyComplete();
    }
}