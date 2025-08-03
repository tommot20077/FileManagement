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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.dowob.filemanagement.component.provider.provider.RedisProvider;
import xyz.dowob.filemanagement.config.properties.SecurityProperties;
import xyz.dowob.filemanagement.exception.ValidationException;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Redis CSRF權杖儲存庫的測試實現。
 * <p>
 * 此測試類驗證 RedisServerCsrfTokenRepository 的分散式權杖管理功能，
 * 涵蓋CSRF權杖安全機制和Redis分散式儲存策略的正確實現。
 * <p>
 * 測試範圍包含權杖生成、Redis操作、權杖時效性驗證和批量清理機制。
 * 特別驗證了分散式環境下權杖的一致性和高可用性特性。
 * <p>
 * 響應式操作透過 Mono/Flux 類型實現，Redis儲存使用雜湊表結構管理權杖生命週期。
 * 權杖採用UUID格式確保唯一性，支援時間戳精度驗證和自動過期清理。
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@SpringBootTest
@ActiveProfiles({"test", "demo", "ci"})
@DisplayName("RedisServerCsrfTokenRepository Redis CSRF Token存儲庫測試")
class RedisServerCsrfTokenRepositoryTest {

    @Mock
    private SecurityProperties securityProperties;

    @Mock
    private SecurityProperties.Csrf csrfProperties;

    @Mock
    private RedisProvider redisProvider;

    private RedisServerCsrfTokenRepository redisServerCsrfTokenRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // 配置 SecurityProperties mocks
        when(securityProperties.getCsrf()).thenReturn(csrfProperties);
        when(csrfProperties.getHeaderName()).thenReturn("X-CSRF-TOKEN");
        when(csrfProperties.getParameterName()).thenReturn("_csrf");
        when(csrfProperties.getExpiration()).thenReturn(Duration.ofMinutes(30));

        // 創建測試對象
        redisServerCsrfTokenRepository = new RedisServerCsrfTokenRepository(securityProperties, redisProvider);
    }

    // ==================== 一般測試 ====================

    @Test
    @DisplayName("一般測試 - 構造函數初始化")
    void testConstructorInitialization() {
        assertNotNull(redisServerCsrfTokenRepository);
        
        // 測試繼承關係
        assertTrue(redisServerCsrfTokenRepository instanceof AbstractServerCsrfTokenRepository);
        assertTrue(redisServerCsrfTokenRepository instanceof CustomServerCsrfTokenRepository);
    }

    @Test
    @DisplayName("一般測試 - generateToken 方法基本功能")
    void testGenerateToken_basicFunctionality() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // Mock Redis操作
        when(redisProvider.setHashMap(eq("X-CSRF-TOKEN"), any(String.class), any(Long.class), any(Duration.class)))
                .thenReturn(Mono.empty());

        StepVerifier.create(redisServerCsrfTokenRepository.generateToken(exchange))
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

        // Mock Redis操作
        when(redisProvider.setHashMap(eq("X-CSRF-TOKEN"), any(String.class), any(Long.class), any(Duration.class)))
                .thenReturn(Mono.empty());

        StepVerifier.create(redisServerCsrfTokenRepository.generateToken(exchange)
                .zipWith(redisServerCsrfTokenRepository.generateToken(exchange)))
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

        StepVerifier.create(redisServerCsrfTokenRepository.saveToken(exchange, token))
                .verifyComplete();
    }

    /**
     * 測試從Redis載入有效CSRF權杖的基本功能。
     * <p>
     * 驗證分散式儲存庫能正確從Redis載入並驗證權杖時效性。
     *
     * 測試涵蓋的邏輯或場景說明。
     *
     * 前置條件：
     * - Redis連線正常且包含有效權杖資料
     * - 權杖未超過設定的過期時間
     *
     * 測試步驟：
     * - 模擬包含CSRF權杖標頭的HTTP請求
     * - 設定Redis返回未來時間戳（表示權杖有效）
     * - 呼叫權杖載入方法並驗證結果
     *
     * 預期結果：
     * - 成功載入權杖並返回正確的權杖物件
     * - 權杖屬性與原始設定一致
     */
    @Test
    @DisplayName("一般測試 - loadToken 方法基本功能 - 有效Token")
    void testLoadToken_basicFunctionality_validToken() {
        String testToken = "test-csrf-token";
        long futureTime = Instant.now().getEpochSecond() + 1800; // 30分鐘後

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/test")
                .header("X-CSRF-TOKEN", testToken)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // Mock Redis返回有效時間
        when(redisProvider.getHashMap("X-CSRF-TOKEN", testToken, Integer.class))
                .thenReturn(Mono.just((int) futureTime));

        StepVerifier.create(redisServerCsrfTokenRepository.loadToken(exchange))
                .assertNext(loadedToken -> {
                    assertEquals("X-CSRF-TOKEN", loadedToken.getHeaderName());
                    assertEquals("_csrf", loadedToken.getParameterName());
                    assertEquals(testToken, loadedToken.getToken());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - loadToken 方法基本功能 - 過期Token")
    void testLoadToken_basicFunctionality_expiredToken() {
        String testToken = "expired-csrf-token";
        long pastTime = Instant.now().getEpochSecond() - 1800; // 30分鐘前

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/test")
                .header("X-CSRF-TOKEN", testToken)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // Mock Redis返回過期時間
        when(redisProvider.getHashMap("X-CSRF-TOKEN", testToken, Integer.class))
                .thenReturn(Mono.just((int) pastTime));

        StepVerifier.create(redisServerCsrfTokenRepository.loadToken(exchange))
                .expectError(ValidationException.class)
                .verify();
    }

    @Test
    @DisplayName("一般測試 - deleteToken 刪除特定Token")
    void testDeleteToken_specificToken() {
        CsrfToken token = new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-token");

        // Mock Redis刪除操作
        when(redisProvider.deleteHash("X-CSRF-TOKEN", "test-token"))
                .thenReturn(Mono.just(1L));

        StepVerifier.create(redisServerCsrfTokenRepository.deleteToken(token))
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - deleteToken 清理過期Token")
    void testDeleteToken_cleanupExpiredTokens() {
        long currentTime = Instant.now().getEpochSecond();
        long expiredTime1 = currentTime - 1000;
        long expiredTime2 = currentTime - 2000;
        long validTime = currentTime + 1000;

        // Mock Redis返回所有Token
        when(redisProvider.getAllHashMap("X-CSRF-TOKEN", String.class, Long.class))
                .thenReturn(Flux.fromIterable(java.util.List.of(
                    java.util.Map.entry("expired-token-1", expiredTime1),
                    java.util.Map.entry("expired-token-2", expiredTime2),
                    java.util.Map.entry("valid-token", validTime)
                )));

        // Mock Redis批量刪除操作
        when(redisProvider.deleteHash(eq("X-CSRF-TOKEN"), anyList()))
                .thenReturn(Mono.just(2L));

        StepVerifier.create(redisServerCsrfTokenRepository.deleteToken(null))
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 完整的Token生命週期")
    void testCompleteTokenLifecycle() {
        MockServerHttpRequest generateRequest = MockServerHttpRequest.get("/generate").build();
        ServerWebExchange generateExchange = MockServerWebExchange.from(generateRequest);

        // Mock生成Token的Redis操作
        when(redisProvider.setHashMap(eq("X-CSRF-TOKEN"), any(String.class), any(Long.class), any(Duration.class)))
                .thenReturn(Mono.empty());

        StepVerifier.create(redisServerCsrfTokenRepository.generateToken(generateExchange))
                .assertNext(generatedToken -> {
                    assertNotNull(generatedToken);
                    
                    // 模擬加載Token
                    long futureTime = Instant.now().getEpochSecond() + 1800;
                    MockServerHttpRequest loadRequest = MockServerHttpRequest
                            .get("/load")
                            .header("X-CSRF-TOKEN", generatedToken.getToken())
                            .build();
                    ServerWebExchange loadExchange = MockServerWebExchange.from(loadRequest);

                    when(redisProvider.getHashMap("X-CSRF-TOKEN", generatedToken.getToken(), Integer.class))
                            .thenReturn(Mono.just((int) futureTime));

                    StepVerifier.create(redisServerCsrfTokenRepository.loadToken(loadExchange))
                            .assertNext(loadedToken -> {
                                assertEquals(generatedToken.getToken(), loadedToken.getToken());
                            })
                            .verifyComplete();

                    // 模擬刪除Token
                    when(redisProvider.deleteHash("X-CSRF-TOKEN", generatedToken.getToken()))
                            .thenReturn(Mono.just(1L));

                    StepVerifier.create(redisServerCsrfTokenRepository.deleteToken(generatedToken))
                            .verifyComplete();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 驗證註解配置")
    void testAnnotations() {
        // 驗證類上的註解
        assertTrue(RedisServerCsrfTokenRepository.class.isAnnotationPresent(org.springframework.stereotype.Component.class));
        assertTrue(RedisServerCsrfTokenRepository.class.isAnnotationPresent(xyz.dowob.filemanagement.annotation.CsrfRepositoryType.class));
        
        var csrfRepoAnnotation = RedisServerCsrfTokenRepository.class.getAnnotation(xyz.dowob.filemanagement.annotation.CsrfRepositoryType.class);
        assertEquals(xyz.dowob.filemanagement.customenum.CsrfTokenRepositoryEnum.REDIS, csrfRepoAnnotation.value());
    }

    // ==================== 異常測試 ====================

    @Test
    @DisplayName("異常測試 - loadToken 缺少CSRF Token Header")
    void testLoadToken_missingCsrfTokenHeader() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(redisServerCsrfTokenRepository.loadToken(exchange))
                .expectError(ValidationException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - loadToken Redis中不存在Token")
    void testLoadToken_tokenNotInRedis() {
        String testToken = "non-existent-token";

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/test")
                .header("X-CSRF-TOKEN", testToken)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // Mock Redis返回空
        when(redisProvider.getHashMap("X-CSRF-TOKEN", testToken, Integer.class))
                .thenReturn(Mono.empty());

        StepVerifier.create(redisServerCsrfTokenRepository.loadToken(exchange))
                .expectError(ValidationException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - generateToken Redis操作失敗")
    void testGenerateToken_redisOperationFails() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // Mock Redis操作失敗
        when(redisProvider.setHashMap(eq("X-CSRF-TOKEN"), any(String.class), any(Long.class), any(Duration.class)))
                .thenReturn(Mono.error(new RuntimeException("Redis connection failed")));

        StepVerifier.create(redisServerCsrfTokenRepository.generateToken(exchange))
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - deleteToken Redis操作失敗")
    void testDeleteToken_redisOperationFails() {
        CsrfToken token = new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-token");

        // Mock Redis刪除操作失敗
        when(redisProvider.deleteHash("X-CSRF-TOKEN", "test-token"))
                .thenReturn(Mono.error(new RuntimeException("Redis delete failed")));

        StepVerifier.create(redisServerCsrfTokenRepository.deleteToken(token))
                .expectError(RuntimeException.class)
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

        // Mock Redis返回空（因為空字符串不會存在於Redis中）
        when(redisProvider.getHashMap("X-CSRF-TOKEN", "", Integer.class))
                .thenReturn(Mono.empty());

        StepVerifier.create(redisServerCsrfTokenRepository.loadToken(exchange))
                .expectError(ValidationException.class)
                .verify();
    }

    // ==================== 邊界測試 ====================

    @Test
    @DisplayName("邊界測試 - Token時間邊界驗證")
    void testTokenTimeBoundary() {
        String testToken = "boundary-test-token";
        long currentTime = Instant.now().getEpochSecond();
        long boundaryTime = currentTime + 1; // 1秒後過期

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/test")
                .header("X-CSRF-TOKEN", testToken)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // Mock Redis返回邊界時間
        when(redisProvider.getHashMap("X-CSRF-TOKEN", testToken, Integer.class))
                .thenReturn(Mono.just((int) boundaryTime));

        StepVerifier.create(redisServerCsrfTokenRepository.loadToken(exchange))
                .assertNext(loadedToken -> {
                    assertEquals(testToken, loadedToken.getToken());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 極長Token處理")
    void testVeryLongToken() {
        String longToken = "a".repeat(10000);
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/test")
                .header("X-CSRF-TOKEN", longToken)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // Mock Redis返回空（極長Token通常不存在）
        when(redisProvider.getHashMap("X-CSRF-TOKEN", longToken, Integer.class))
                .thenReturn(Mono.empty());

        StepVerifier.create(redisServerCsrfTokenRepository.loadToken(exchange))
                .expectError(ValidationException.class)
                .verify();
    }

    @Test
    @DisplayName("邊界測試 - 大量過期Token清理")
    void testMassiveExpiredTokenCleanup() {
        long currentTime = Instant.now().getEpochSecond();
        
        // 創建大量過期Token
        java.util.List<Map.Entry<String, Long>> massiveExpiredTokens = 
                java.util.stream.IntStream.range(1, 10001)
                        .mapToObj(i -> java.util.Map.entry("expired-token-" + i, currentTime - i))
                        .collect(java.util.stream.Collectors.toList());

        // Mock Redis返回大量過期Token
        when(redisProvider.getAllHashMap("X-CSRF-TOKEN", String.class, Long.class))
                .thenReturn(Flux.fromIterable(massiveExpiredTokens));

        // Mock Redis批量刪除操作
        when(redisProvider.deleteHash(eq("X-CSRF-TOKEN"), anyList()))
                .thenReturn(Mono.just(10000L));

        StepVerifier.create(redisServerCsrfTokenRepository.deleteToken(null))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 併發Token操作")
    void testConcurrentTokenOperations() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // Mock Redis操作
        when(redisProvider.setHashMap(eq("X-CSRF-TOKEN"), any(String.class), any(Long.class), any(Duration.class)))
                .thenReturn(Mono.empty());

        // 併發生成多個Token
        StepVerifier.create(
            reactor.core.publisher.Flux.range(1, 100)
                    .flatMap(i -> redisServerCsrfTokenRepository.generateToken(exchange))
                    .collectList()
        )
        .assertNext(tokens -> {
            assertEquals(100, tokens.size());
            
            // 驗證所有Token都是唯一的
            long uniqueCount = tokens.stream()
                    .map(CsrfToken::getToken)
                    .distinct()
                    .count();
            assertEquals(100, uniqueCount);
        })
        .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - Redis連接異常恢復")
    void testRedisConnectionRecovery() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // 第一次調用失敗
        when(redisProvider.setHashMap(eq("X-CSRF-TOKEN"), any(String.class), any(Long.class), any(Duration.class)))
                .thenReturn(Mono.error(new RuntimeException("Connection timeout")))
                .thenReturn(Mono.empty()); // 第二次成功

        // 第一次調用應該失敗
        StepVerifier.create(redisServerCsrfTokenRepository.generateToken(exchange))
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    @DisplayName("邊界測試 - deleteToken 處理各種Token狀態")
    void testDeleteToken_variousTokenStates() {
        // 刪除null Token（清理過期Token）
        when(redisProvider.getAllHashMap("X-CSRF-TOKEN", String.class, Long.class))
                .thenReturn(Flux.empty());

        StepVerifier.create(redisServerCsrfTokenRepository.deleteToken(null))
                .verifyComplete();

        // 刪除Token值為null的Token (使用Mock)
        CsrfToken nullValueToken = org.mockito.Mockito.mock(CsrfToken.class);
        when(nullValueToken.getToken()).thenReturn(null);
        when(redisProvider.getAllHashMap("X-CSRF-TOKEN", String.class, Long.class))
                .thenReturn(Flux.empty());

        StepVerifier.create(redisServerCsrfTokenRepository.deleteToken(nullValueToken))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 時間戳精度測試")
    void testTimestampPrecision() {
        String testToken = "precision-test-token";
        long expiredTime = Instant.now().getEpochSecond() - 1; // 1 second ago (expired)

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/test")
                .header("X-CSRF-TOKEN", testToken)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // 測試過期時間的邊界情況
        when(redisProvider.getHashMap("X-CSRF-TOKEN", testToken, Integer.class))
                .thenReturn(Mono.just((int) expiredTime));

        // 由於token已經過期，應該返回INVALID_CSRF_TOKEN錯誤
        StepVerifier.create(redisServerCsrfTokenRepository.loadToken(exchange))
                .expectErrorMatches(throwable -> 
                    throwable instanceof ValidationException && 
                     ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.INVALID_CSRF_TOKEN)
                .verify();
    }

    @Test
    @DisplayName("邊界測試 - Redis數據類型轉換")
    void testRedisDataTypeConversion() {
        String testToken = "type-conversion-test";
        
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/test")
                .header("X-CSRF-TOKEN", testToken)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // Mock Redis返回的數據類型可能不是預期的Integer
        when(redisProvider.getHashMap("X-CSRF-TOKEN", testToken, Integer.class))
                .thenReturn(Mono.error(new ClassCastException("Cannot cast to Integer")));

        StepVerifier.create(redisServerCsrfTokenRepository.loadToken(exchange))
                .expectError(ClassCastException.class)
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

        // Mock Redis返回空（特殊字符Token通常不存在）
        when(redisProvider.getHashMap("X-CSRF-TOKEN", specialToken, Integer.class))
                .thenReturn(Mono.empty());

        StepVerifier.create(redisServerCsrfTokenRepository.loadToken(exchange))
                .expectError(ValidationException.class)
                .verify();
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

        RedisServerCsrfTokenRepository altRepository = new RedisServerCsrfTokenRepository(altSecurityProperties, redisProvider);

        MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // Mock Redis操作
        when(redisProvider.setHashMap(eq("X-CUSTOM-CSRF"), any(String.class), any(Long.class), any(Duration.class)))
                .thenReturn(Mono.empty());

        StepVerifier.create(altRepository.generateToken(exchange))
                .assertNext(token -> {
                    assertEquals("X-CUSTOM-CSRF", token.getHeaderName());
                    assertEquals("_custom_csrf", token.getParameterName());
                })
                .verifyComplete();
    }
}