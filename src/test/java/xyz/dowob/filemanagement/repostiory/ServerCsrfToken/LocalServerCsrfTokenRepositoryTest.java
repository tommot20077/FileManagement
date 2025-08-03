package xyz.dowob.filemanagement.repostiory.ServerCsrfToken;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.security.web.server.csrf.DefaultCsrfToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ServerWebExchange;
import reactor.test.StepVerifier;
import xyz.dowob.filemanagement.config.properties.SecurityProperties;
import xyz.dowob.filemanagement.exception.ValidationException;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * 本地CSRF權杖儲存庫的測試實現。
 * <p>
 * 此測試類驗證 LocalServerCsrfTokenRepository 的本地快取權杖管理功能，
 * 涵蓋CSRF權杖安全機制和本地快取儲存策略的正確實現。
 * <p>
 * 測試範圍包含權杖生成、本地快取操作、權杖驗證機制和生命週期管理。
 * 特別驗證了單機環境下權杖的安全性和效能特性。
 * <p>
 * 響應式操作透過 Mono/Flux 類型實現，本地快取使用併發安全的雜湊表結構。
 * 權杖採用UUID格式確保唯一性，支援過期時間自動清理機制。
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@SpringBootTest
@ActiveProfiles({"test", "demo", "ci"})
@DisplayName("LocalServerCsrfTokenRepository 本地CSRF Token存儲庫測試")
class LocalServerCsrfTokenRepositoryTest {

    @Mock
    private SecurityProperties securityProperties;

    @Mock
    private SecurityProperties.Csrf csrfProperties;

    private LocalServerCsrfTokenRepository localServerCsrfTokenRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // 配置 SecurityProperties mocks
        when(securityProperties.getCsrf()).thenReturn(csrfProperties);
        when(csrfProperties.getHeaderName()).thenReturn("X-CSRF-TOKEN");
        when(csrfProperties.getParameterName()).thenReturn("_csrf");
        when(csrfProperties.getExpiration()).thenReturn(Duration.ofMinutes(30));

        // 創建測試對象
        localServerCsrfTokenRepository = new LocalServerCsrfTokenRepository(securityProperties);
    }

    // ==================== 一般測試 ====================

    @Test
    @DisplayName("一般測試 - 構造函數初始化")
    void testConstructorInitialization() {
        assertNotNull(localServerCsrfTokenRepository);
        
        // 測試繼承關係
        assertTrue(localServerCsrfTokenRepository instanceof AbstractServerCsrfTokenRepository);
        assertTrue(localServerCsrfTokenRepository instanceof CustomServerCsrfTokenRepository);
    }

    /**
     * 測試CSRF權杖生成的基本功能。
     * <p>
     * 驗證本地儲存庫能正確生成符合安全規範的CSRF權杖。
     *
     * 測試涵蓋的邏輯或場景說明。
     *
     * 前置條件：
     * - 安全性配置正確初始化
     * - 本地快取結構可用
     *
     * 測試步驟：
     * - 建立模擬HTTP請求交換物件
     * - 呼叫權杖生成方法
     * - 驗證權杖格式和安全性屬性
     *
     * 預期結果：
     * - 成功生成UUID格式的唯一權杖
     * - 權杖包含正確的標頭名稱和參數名稱
     */
    @Test
    @DisplayName("一般測試 - generateToken 方法基本功能")
    void testGenerateToken_basicFunctionality() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(localServerCsrfTokenRepository.generateToken(exchange))
                .assertNext(csrfToken -> {
                    assertNotNull(csrfToken);
                    assertEquals("X-CSRF-TOKEN", csrfToken.getHeaderName());
                    assertEquals("_csrf", csrfToken.getParameterName());
                    assertNotNull(csrfToken.getToken());
                    assertFalse(csrfToken.getToken().isEmpty());
                    // 驗證Token格式為UUID
                    assertTrue(csrfToken.getToken().matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - generateToken 生成唯一Token")
    void testGenerateToken_uniqueTokens() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(localServerCsrfTokenRepository.generateToken(exchange)
                .zipWith(localServerCsrfTokenRepository.generateToken(exchange)))
                .assertNext(tokenPair -> {
                    CsrfToken token1 = tokenPair.getT1();
                    CsrfToken token2 = tokenPair.getT2();
                    assertNotEquals(token1.getToken(), token2.getToken());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - saveToken 方法（空實現）")
    void testSaveToken_emptyImplementation() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        CsrfToken token = new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-token");

        StepVerifier.create(localServerCsrfTokenRepository.saveToken(exchange, token))
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - loadToken 方法基本功能")
    void testLoadToken_basicFunctionality() {
        // 先生成一個Token
        MockServerHttpRequest generateRequest = MockServerHttpRequest.get("/generate").build();
        ServerWebExchange generateExchange = MockServerWebExchange.from(generateRequest);

        StepVerifier.create(localServerCsrfTokenRepository.generateToken(generateExchange)
                .flatMap(generatedToken -> {
                    // 創建包含Token的請求
                    MockServerHttpRequest loadRequest = MockServerHttpRequest
                            .get("/load")
                            .header("X-CSRF-TOKEN", generatedToken.getToken())
                            .build();
                    ServerWebExchange loadExchange = MockServerWebExchange.from(loadRequest);
                    
                    return localServerCsrfTokenRepository.loadToken(loadExchange)
                            .map(loadedToken -> new CsrfToken[]{generatedToken, loadedToken});
                }))
                .assertNext(tokens -> {
                    CsrfToken generatedToken = tokens[0];
                    CsrfToken loadedToken = tokens[1];
                    
                    assertEquals(generatedToken.getToken(), loadedToken.getToken());
                    assertEquals(generatedToken.getHeaderName(), loadedToken.getHeaderName());
                    assertEquals(generatedToken.getParameterName(), loadedToken.getParameterName());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - deleteToken 刪除特定Token")
    void testDeleteToken_specificToken() {
        // 先生成一個Token
        MockServerHttpRequest generateRequest = MockServerHttpRequest.get("/generate").build();
        ServerWebExchange generateExchange = MockServerWebExchange.from(generateRequest);

        StepVerifier.create(localServerCsrfTokenRepository.generateToken(generateExchange)
                .flatMap(generatedToken -> 
                    localServerCsrfTokenRepository.deleteToken(generatedToken)
                            .thenReturn(generatedToken)
                ))
                .assertNext(deletedToken -> {
                    // 嘗試加載已刪除的Token，應該失敗
                    MockServerHttpRequest loadRequest = MockServerHttpRequest
                            .get("/load")
                            .header("X-CSRF-TOKEN", deletedToken.getToken())
                            .build();
                    ServerWebExchange loadExchange = MockServerWebExchange.from(loadRequest);
                    
                    StepVerifier.create(localServerCsrfTokenRepository.loadToken(loadExchange))
                            .expectError(ValidationException.class)
                            .verify();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - deleteToken 清理過期Token")
    void testDeleteToken_cleanupExpiredTokens() {
        StepVerifier.create(localServerCsrfTokenRepository.deleteToken(null))
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 完整的Token生命週期")
    void testCompleteTokenLifecycle() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(
            // 1. 生成Token
            localServerCsrfTokenRepository.generateToken(exchange)
                .flatMap(generatedToken -> {
                    // 2. 驗證Token可以加載
                    MockServerHttpRequest loadRequest = MockServerHttpRequest
                            .get("/load")
                            .header("X-CSRF-TOKEN", generatedToken.getToken())
                            .build();
                    ServerWebExchange loadExchange = MockServerWebExchange.from(loadRequest);
                    
                    return localServerCsrfTokenRepository.loadToken(loadExchange)
                            .flatMap(loadedToken -> 
                                // 3. 刪除Token
                                localServerCsrfTokenRepository.deleteToken(loadedToken)
                                        .thenReturn(loadedToken)
                            );
                })
        )
        .assertNext(deletedToken -> {
            assertNotNull(deletedToken);
            // Token已被刪除，後續驗證會在其他測試中進行
        })
        .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 多個Token並發生成")
    void testConcurrentTokenGeneration() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(
            reactor.core.publisher.Flux.range(1, 5)
                    .flatMap(i -> localServerCsrfTokenRepository.generateToken(exchange))
                    .collectList()
        )
        .assertNext(tokens -> {
            assertEquals(5, tokens.size());
            
            // 驗證所有Token都是唯一的
            long uniqueCount = tokens.stream()
                    .map(CsrfToken::getToken)
                    .distinct()
                    .count();
            assertEquals(5, uniqueCount);
        })
        .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - destroy 方法")
    void testDestroy() {
        // destroy方法應該能正常調用而不拋出異常
        assertDoesNotThrow(() -> localServerCsrfTokenRepository.destroy());
    }

    @Test
    @DisplayName("一般測試 - 驗證註解配置")
    void testAnnotations() {
        // 驗證類上的註解
        assertTrue(LocalServerCsrfTokenRepository.class.isAnnotationPresent(org.springframework.stereotype.Component.class));
        assertTrue(LocalServerCsrfTokenRepository.class.isAnnotationPresent(xyz.dowob.filemanagement.annotation.CsrfRepositoryType.class));
        
        var csrfRepoAnnotation = LocalServerCsrfTokenRepository.class.getAnnotation(xyz.dowob.filemanagement.annotation.CsrfRepositoryType.class);
        assertEquals(xyz.dowob.filemanagement.customenum.CsrfTokenRepositoryEnum.LOCAL, csrfRepoAnnotation.value());
    }

    // ==================== 異常測試 ====================

    @Test
    @DisplayName("異常測試 - loadToken 缺少CSRF Token Header")
    void testLoadToken_missingCsrfTokenHeader() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(localServerCsrfTokenRepository.loadToken(exchange))
                .expectError(ValidationException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - loadToken 無效的CSRF Token")
    void testLoadToken_invalidCsrfToken() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/test")
                .header("X-CSRF-TOKEN", "invalid-token-not-exists")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(localServerCsrfTokenRepository.loadToken(exchange))
                .expectError(ValidationException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - loadToken 使用空Token Header")
    void testLoadToken_emptyTokenHeader() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/test")
                .header("X-CSRF-TOKEN", "")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(localServerCsrfTokenRepository.loadToken(exchange))
                .expectError(ValidationException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - generateToken 傳入null exchange")
    void testGenerateToken_withNullExchange() {
        // 這個測試檢查實現是否能處理null輸入
        StepVerifier.create(localServerCsrfTokenRepository.generateToken(null))
                .assertNext(token -> assertNotNull(token.getToken()))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - saveToken 傳入null參數")
    void testSaveToken_withNullParameters() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // 測試null token
        StepVerifier.create(localServerCsrfTokenRepository.saveToken(exchange, null))
                .verifyComplete();

        // 測試null exchange
        CsrfToken token = new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-token");
        StepVerifier.create(localServerCsrfTokenRepository.saveToken(null, token))
                .verifyComplete();

        // 測試雙null
        StepVerifier.create(localServerCsrfTokenRepository.saveToken(null, null))
                .verifyComplete();
    }

    // ==================== 邊界測試 ====================

    @Test
    @DisplayName("邊界測試 - 極長Token處理")
    void testVeryLongToken() {
        String longToken = "a".repeat(10000);
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/test")
                .header("X-CSRF-TOKEN", longToken)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(localServerCsrfTokenRepository.loadToken(exchange))
                .expectError(ValidationException.class)
                .verify();
    }

    @Test
    @DisplayName("邊界測試 - 特殊字符Token處理")
    void testSpecialCharacterToken() {
        String specialToken = "!@#$%^&*()_+-=[]{}|;:,.<>?";
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/test")
                .header("X-CSRF-TOKEN", specialToken)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(localServerCsrfTokenRepository.loadToken(exchange))
                .expectError(ValidationException.class)
                .verify();
    }

    @Test
    @DisplayName("邊界測試 - Unicode字符Token處理")
    void testUnicodeToken() {
        String unicodeToken = "測試Token中文字符🔒🛡️";
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/test")
                .header("X-CSRF-TOKEN", unicodeToken)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(localServerCsrfTokenRepository.loadToken(exchange))
                .expectError(ValidationException.class)
                .verify();
    }

    @Test
    @DisplayName("邊界測試 - 大量Token生成和管理")
    void testMassiveTokenGenerationAndManagement() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // 生成大量Token
        StepVerifier.create(
            reactor.core.publisher.Flux.range(1, 1000)
                    .flatMap(i -> localServerCsrfTokenRepository.generateToken(exchange))
                    .collectList()
        )
        .assertNext(tokens -> {
            assertEquals(1000, tokens.size());
            
            // 驗證所有Token都是唯一的
            long uniqueCount = tokens.stream()
                    .map(CsrfToken::getToken)
                    .distinct()
                    .count();
            assertEquals(1000, uniqueCount);
        })
        .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 併發Token操作")
    void testConcurrentTokenOperations() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // 併發生成和刪除Token
        StepVerifier.create(
            localServerCsrfTokenRepository.generateToken(exchange)
                    .flatMap(generatedToken -> 
                        reactor.core.publisher.Flux.merge(
                            // 併發加載Token
                            reactor.core.publisher.Mono.fromCallable(() -> {
                                MockServerHttpRequest loadRequest = MockServerHttpRequest
                                        .get("/load")
                                        .header("X-CSRF-TOKEN", generatedToken.getToken())
                                        .build();
                                return MockServerWebExchange.from(loadRequest);
                            }).flatMap(loadExchange -> localServerCsrfTokenRepository.loadToken(loadExchange)),
                            
                            // 併發刪除Token
                            localServerCsrfTokenRepository.deleteToken(generatedToken)
                                    .thenReturn(generatedToken)
                        )
                        .collectList()
                    )
        )
        .assertNext(results -> {
            // 應該能處理併發操作而不出錯
            assertNotNull(results);
        })
        .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - Token生命週期邊界")
    void testTokenLifecycleBoundaries() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(
            // 生成Token
            localServerCsrfTokenRepository.generateToken(exchange)
                .flatMap(generatedToken -> {
                    // 立即嘗試多次加載同一Token
                    MockServerHttpRequest loadRequest = MockServerHttpRequest
                            .get("/load")
                            .header("X-CSRF-TOKEN", generatedToken.getToken())
                            .build();
                    ServerWebExchange loadExchange = MockServerWebExchange.from(loadRequest);
                    
                    return reactor.core.publisher.Flux.range(1, 5)
                            .flatMap(i -> localServerCsrfTokenRepository.loadToken(loadExchange))
                            .collectList();
                })
        )
        .assertNext(loadedTokens -> {
            assertEquals(5, loadedTokens.size());
            // 所有加載的Token都應該相同
            String firstToken = loadedTokens.get(0).getToken();
            assertTrue(loadedTokens.stream().allMatch(token -> firstToken.equals(token.getToken())));
        })
        .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - deleteToken 傳入各種Token狀態")
    void testDeleteToken_variousTokenStates() {
        // 刪除null Token（清理過期Token）
        StepVerifier.create(localServerCsrfTokenRepository.deleteToken(null))
                .verifyComplete();

        // 刪除無效Token對象
        CsrfToken invalidToken = new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "non-existent-token");
        StepVerifier.create(localServerCsrfTokenRepository.deleteToken(invalidToken))
                .verifyComplete();

        // 測試創建空字符串Token會拋出異常（DefaultCsrfToken不允許null或空值）
        assertThrows(IllegalArgumentException.class, () -> {
            new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "");
        });
        
        // 測試創建null Token會拋出異常
        assertThrows(IllegalArgumentException.class, () -> {
            new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", null);
        });
    }

    @Test
    @DisplayName("邊界測試 - 多個Header值處理")
    void testMultipleHeaderValues() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/test")
                .header("X-CSRF-TOKEN", "token1", "token2")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // 應該使用第一個header值
        StepVerifier.create(localServerCsrfTokenRepository.loadToken(exchange))
                .expectError(ValidationException.class)
                .verify();
    }

    @Test
    @DisplayName("邊界測試 - Header名稱大小寫敏感性")
    void testHeaderCaseSensitivity() {
        // 先生成一個Token
        MockServerHttpRequest generateRequest = MockServerHttpRequest.get("/generate").build();
        ServerWebExchange generateExchange = MockServerWebExchange.from(generateRequest);

        StepVerifier.create(localServerCsrfTokenRepository.generateToken(generateExchange)
                .flatMap(generatedToken -> {
                    // 使用小寫header名稱
                    MockServerHttpRequest loadRequest = MockServerHttpRequest
                            .get("/load")
                            .header("x-csrf-token", generatedToken.getToken())
                            .build();
                    ServerWebExchange loadExchange = MockServerWebExchange.from(loadRequest);
                    
                    return localServerCsrfTokenRepository.loadToken(loadExchange);
                }))
                .assertNext(token -> {
                    assertNotNull(token);
                    assertNotNull(token.getToken());
                    assertEquals("X-CSRF-TOKEN", token.getHeaderName());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 驗證SecurityProperties配置影響")
    void testSecurityPropertiesConfiguration() {
        // 使用不同的配置創建新實例
        SecurityProperties altSecurityProperties = org.mockito.Mockito.mock(SecurityProperties.class);
        SecurityProperties.Csrf altCsrfProperties = org.mockito.Mockito.mock(SecurityProperties.Csrf.class);
        
        when(altSecurityProperties.getCsrf()).thenReturn(altCsrfProperties);
        when(altCsrfProperties.getHeaderName()).thenReturn("X-CUSTOM-CSRF");
        when(altCsrfProperties.getParameterName()).thenReturn("_custom_csrf");
        when(altCsrfProperties.getExpiration()).thenReturn(Duration.ofMinutes(60));

        LocalServerCsrfTokenRepository altRepository = new LocalServerCsrfTokenRepository(altSecurityProperties);

        MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(altRepository.generateToken(exchange))
                .assertNext(token -> {
                    assertEquals("X-CUSTOM-CSRF", token.getHeaderName());
                    assertEquals("_custom_csrf", token.getParameterName());
                })
                .verifyComplete();
    }
}