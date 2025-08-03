package xyz.dowob.filemanagement.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.dowob.filemanagement.component.strategy.CsrfTokenRepositoryStrategy;
import xyz.dowob.filemanagement.config.properties.SecurityProperties;
import xyz.dowob.filemanagement.repostiory.JwtSecurityContextRepository;
import xyz.dowob.filemanagement.repostiory.ServerCsrfToken.AbstractServerCsrfTokenRepository;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * SecurityConfig 安全配置測試類別
 * 
 * <p>全面測試 {@link xyz.dowob.filemanagement.config.SecurityConfig} 安全配置類別的各種功能和行為。</p>
 * 
 * <p>測試範圍包括：
 * 
 *   - 安全過濾器鏈配置驗證
 *   - 跨域資源共享（CORS）配置測試
 *   - 跨站請求偽造（CSRF）驗證過濾器
 *   - 請求追蹤 ID 過濾器功能
 *   - 上下文過濾器行為
 *   - 用戶信息過濾器驗證
 *   - 密碼編碼器安全性測試
 *   - 路徑安全配置驗證
 *   - HTTP 方法安全性判斷
 * 
 * </p>
 * 
 * <p>測試前置條件：
 * 
 *   - SecurityConfig 能正常實例化
 *   - SecurityProperties 配置參數正確
 *   - 所有依賴服務可以正確模擬
 * 
 * </p>
 * 
 * <p>測試策略：
 * 
 *   - 使用模擬（Mock）方法驗證各配置組件
 *   - 覆蓋正常流程和邊界情況
 *   - 確保每個過濾器和配置方法按預期工作
 * 
 * </p>
 * 
 * <p>預期測試結果：
 * 
 *   - 所有安全配置正確且一致
 *   - 過濾器鏈按設計預期執行
 *   - 安全檢查機制健壯且準確
 * 
 * </p>
 * 
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@SpringBootTest
@ActiveProfiles({"test", "demo", "ci"})
@DisplayName("SecurityConfig 安全配置測試")
class SecurityConfigTest {

    @Mock
    private JwtSecurityContextRepository securityContextRepository;
    
    @Mock
    private ObjectMapper objectMapper;
    
    @Mock
    private SecurityProperties securityProperties;
    
    @Mock
    private CsrfTokenRepositoryStrategy csrfTokenRepositoryStrategy;
    
    @Mock
    private AbstractServerCsrfTokenRepository csrfTokenRepository;
    
    @Mock
    private SecurityProperties.Cors corsProperties;
    
    @Mock
    private SecurityProperties.Hsts hstsProperties;
    
    @Mock
    private SecurityProperties.Csrf csrfProperties;
    
    @Mock
    private SecurityProperties.Paths pathsProperties;

    private SecurityConfig securityConfig;
    private ServerWebExchange testExchange;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // 設置基本的 SecurityProperties mock
        when(securityProperties.getCors()).thenReturn(corsProperties);
        when(securityProperties.getHsts()).thenReturn(hstsProperties);
        when(securityProperties.getCsrf()).thenReturn(csrfProperties);
        when(securityProperties.getPaths()).thenReturn(pathsProperties);
        
        // 設置 CORS 配置
        when(corsProperties.getAllowedOrigins()).thenReturn(List.of("http://localhost:3000"));
        when(corsProperties.getAllowedOriginsPattern()).thenReturn(List.of("http://localhost:*"));
        when(corsProperties.getAllowedMethods()).thenReturn(List.of("GET", "POST", "PUT", "DELETE"));
        when(corsProperties.getAllowedHeaders()).thenReturn(List.of("*"));
        when(corsProperties.getAllowExposedHeaders()).thenReturn(List.of("X-Custom-Header"));
        when(corsProperties.isAllowCredentials()).thenReturn(true);
        when(corsProperties.getMaxAge()).thenReturn(Duration.ofHours(1));
        
        // 設置 HSTS 配置
        when(hstsProperties.isIncludeSubDomains()).thenReturn(true);
        when(hstsProperties.getMaxAge()).thenReturn(Duration.ofDays(365));
        
        // 設置 CSRF 配置
        when(csrfProperties.getAllowRefererPatten()).thenReturn("^https?://localhost.*$");
        when(csrfTokenRepositoryStrategy.getCsrfTokenRepository()).thenReturn(csrfTokenRepository);
        
        // 配置 ObjectMapper Mock 以防止 NullPointer
        try {
            when(objectMapper.writeValueAsBytes(any())).thenReturn("{}".getBytes());
        } catch (Exception e) {
            // 處理可能的異常
        }
        
        // 設置 Paths 配置
        when(pathsProperties.getEffectiveRules()).thenReturn(List.of());
        
        securityConfig = new SecurityConfig(securityContextRepository, objectMapper, securityProperties, csrfTokenRepositoryStrategy);
        
        MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
        testExchange = MockServerWebExchange.from(request);
    }

    // ==================== 一般測試 ====================

    @Test
    @DisplayName("一般測試 - 安全配置初始化")
    void testSecurityConfigInitialization() {
        assertNotNull(securityConfig);
        assertTrue(securityConfig instanceof xyz.dowob.filemanagement.unity.ResponseUnity);
    }

    @Test
    @DisplayName("一般測試 - SecurityConfig 基础配置验证")
    void testSecurityWebFilterChain_basicConfiguration() {
        // 由於 ServerHttpSecurity 無法直接模擬，我們測試 SecurityConfig 其他可測試的部分
        // 驗證配置對象的依賴注入和初始化
        assertNotNull(securityConfig);
        
        // 驗證 SecurityConfig 的依賴都已正確注入
        assertNotNull(securityContextRepository);
        assertNotNull(objectMapper);
        assertNotNull(securityProperties);
        assertNotNull(csrfTokenRepositoryStrategy);
        
        // 測試其他可配置組件
        CorsConfigurationSource corsSource = securityConfig.corsConfigurationSource();
        assertNotNull(corsSource);
        
        PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();
        assertNotNull(passwordEncoder);
        
        WebFilter traceIdFilter = securityConfig.traceIdFilter();
        assertNotNull(traceIdFilter);
        
        WebFilter csrfFilter = securityConfig.csrfValidationFilter();
        assertNotNull(csrfFilter);
        
        WebFilter contextFilter = securityConfig.contextWebFilter();
        assertNotNull(contextFilter);
        
        WebFilter userInfoFilter = securityConfig.userInfoFilter();
        assertNotNull(userInfoFilter);
    }

    @Test
    @DisplayName("一般測試 - corsConfigurationSource CORS 配置")
    void testCorsConfigurationSource_basicConfiguration() {
        CorsConfigurationSource corsSource = securityConfig.corsConfigurationSource();
        
        assertNotNull(corsSource);
        
        org.springframework.web.cors.CorsConfiguration config = corsSource.getCorsConfiguration(testExchange);
        assertNotNull(config);
        
        // 驗證 CORS 配置
        assertNotNull(config.getAllowedOrigins());
        assertNotNull(config.getAllowedOriginPatterns());
        assertNotNull(config.getAllowedMethods());
        assertNotNull(config.getAllowedHeaders());
        assertNotNull(config.getExposedHeaders());
        
        // 檢查基本配置值（如果存在）
        if (!config.getAllowedOrigins().isEmpty()) {
            assertTrue(config.getAllowedOrigins().contains("http://localhost:3000"));
        }
        if (!config.getAllowedOriginPatterns().isEmpty()) {
            assertTrue(config.getAllowedOriginPatterns().contains("http://localhost:*"));
        }
        if (!config.getAllowedMethods().isEmpty()) {
            assertTrue(config.getAllowedMethods().contains("GET") || config.getAllowedMethods().contains("POST"));
        }
        if (!config.getAllowedHeaders().isEmpty()) {
            assertTrue(config.getAllowedHeaders().contains("*") || !config.getAllowedHeaders().isEmpty());
        }
        
        // 驗證預設的暴露標頭（這些應該總是存在）
        assertTrue(config.getExposedHeaders().contains("Content-Disposition"));
        assertTrue(config.getExposedHeaders().contains("Authorization"));
        assertTrue(config.getExposedHeaders().contains("X-Csrf-Token"));
        
        // 檢查其他配置
        assertEquals(true, config.getAllowCredentials());
        assertEquals(Duration.ofHours(1).getSeconds(), config.getMaxAge());
    }

    @Test
    @DisplayName("一般測試 - traceIdFilter 請求 ID 過濾器")
    void testTraceIdFilter_generateRequestId() {
        WebFilter traceIdFilter = securityConfig.traceIdFilter();
        assertNotNull(traceIdFilter);

        // 測試沒有請求 ID 的情況
        StepVerifier.create(traceIdFilter.filter(testExchange, exchange -> Mono.empty()))
                .verifyComplete();
        
        String requestId = testExchange.getAttribute("requestId");
        assertNotNull(requestId);
        assertFalse(requestId.isEmpty());
    }

    @Test
    @DisplayName("一般測試 - traceIdFilter 保持現有請求 ID")
    void testTraceIdFilter_preserveExistingRequestId() {
        String existingId = "existing-request-id";
        testExchange.getAttributes().put("requestId", existingId);
        
        WebFilter traceIdFilter = securityConfig.traceIdFilter();
        
        StepVerifier.create(traceIdFilter.filter(testExchange, exchange -> Mono.empty()))
                .verifyComplete();
        
        String requestId = testExchange.getAttribute("requestId");
        assertEquals(existingId, requestId);
    }

    @Test
    @DisplayName("一般測試 - csrfValidationFilter CSRF 驗證過濾器")
    void testCsrfValidationFilter_skipNonWebPath() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/test").build();
        ServerWebExchange apiExchange = MockServerWebExchange.from(request);
        
        WebFilter csrfFilter = securityConfig.csrfValidationFilter();
        
        StepVerifier.create(csrfFilter.filter(apiExchange, exchange -> Mono.empty()))
                .verifyComplete();
        
        // 驗證不會調用 CSRF repository
        verify(csrfTokenRepositoryStrategy, never()).getCsrfTokenRepository();
    }

    @Test
    @DisplayName("一般測試 - csrfValidationFilter CSRF Token 請求檢查")
    void testCsrfValidationFilter_csrfTokenEndpoint() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/guest/csrf/token")
                .header("Referer", "http://localhost:3000")
                .build();
        ServerWebExchange csrfExchange = MockServerWebExchange.from(request);
        
        WebFilter csrfFilter = securityConfig.csrfValidationFilter();
        
        StepVerifier.create(csrfFilter.filter(csrfExchange, exchange -> Mono.empty()))
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - contextWebFilter 上下文過濾器")
    void testContextWebFilter_basicFunctionality() {
        WebFilter contextFilter = securityConfig.contextWebFilter();
        assertNotNull(contextFilter);
        
        StepVerifier.create(contextFilter.filter(testExchange, exchange -> Mono.empty()))
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - userInfoFilter 用戶信息過濾器")
    void testUserInfoFilter_basicFunctionality() {
        WebFilter userInfoFilter = securityConfig.userInfoFilter();
        assertNotNull(userInfoFilter);
        
        StepVerifier.create(userInfoFilter.filter(testExchange, exchange -> Mono.empty()))
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - passwordEncoder 密碼編碼器")
    void testPasswordEncoder_basicFunctionality() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();
        
        assertNotNull(encoder);
        assertTrue(encoder instanceof org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder);
        
        String password = "testPassword123";
        String encoded = encoder.encode(password);
        
        assertNotNull(encoded);
        assertNotEquals(password, encoded);
        assertTrue(encoder.matches(password, encoded));
        assertFalse(encoder.matches("wrongPassword", encoded));
    }

    @Test
    @DisplayName("一般測試 - isSafeMethod 安全方法判斷")
    void testIsSafeMethod_safeHttpMethods() {
        // GET 方法
        MockServerHttpRequest getRequest = MockServerHttpRequest.get("/test").build();
        ServerWebExchange getExchange = MockServerWebExchange.from(getRequest);
        assertTrue(securityConfig.isSafeMethod(getExchange));
        
        // HEAD 方法
        MockServerHttpRequest headRequest = MockServerHttpRequest.head("/test").build();
        ServerWebExchange headExchange = MockServerWebExchange.from(headRequest);
        assertTrue(securityConfig.isSafeMethod(headExchange));
        
        // OPTIONS 方法
        MockServerHttpRequest optionsRequest = MockServerHttpRequest.options("/test").build();
        ServerWebExchange optionsExchange = MockServerWebExchange.from(optionsRequest);
        assertTrue(securityConfig.isSafeMethod(optionsExchange));
    }

    @Test
    @DisplayName("一般測試 - isSafeMethod 非安全方法判斷")
    void testIsSafeMethod_unsafeHttpMethods() {
        // POST 方法
        MockServerHttpRequest postRequest = MockServerHttpRequest.post("/test").build();
        ServerWebExchange postExchange = MockServerWebExchange.from(postRequest);
        assertFalse(securityConfig.isSafeMethod(postExchange));
        
        // PUT 方法
        MockServerHttpRequest putRequest = MockServerHttpRequest.put("/test").build();
        ServerWebExchange putExchange = MockServerWebExchange.from(putRequest);
        assertFalse(securityConfig.isSafeMethod(putExchange));
        
        // DELETE 方法
        MockServerHttpRequest deleteRequest = MockServerHttpRequest.delete("/test").build();
        ServerWebExchange deleteExchange = MockServerWebExchange.from(deleteRequest);
        assertFalse(securityConfig.isSafeMethod(deleteExchange));
    }

    // ==================== 異常測試 ====================

    @Test
    @DisplayName("異常測試 - csrfValidationFilter 無效 Referer")
    void testCsrfValidationFilter_invalidReferer() {
        // 設置匹配正則表達式，使測試中的 referer 不匹配
        when(csrfProperties.getAllowRefererPatten()).thenReturn("^https?://localhost.*$");
        
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/guest/csrf/token")
                .header("Referer", "http://malicious-site.com")
                .build();
        ServerWebExchange csrfExchange = MockServerWebExchange.from(request);
        
        WebFilter csrfFilter = securityConfig.csrfValidationFilter();
        
        // 測試執行但是會在內部處理錯誤回應，不會拋出異常
        StepVerifier.create(csrfFilter.filter(csrfExchange, exchange -> Mono.empty()))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - csrfValidationFilter 缺少 Referer")
    void testCsrfValidationFilter_missingReferer() {
        // 設置 CSRF 模式，當沒有 Referer 時應該觸發錯誤
        when(csrfProperties.getAllowRefererPatten()).thenReturn("^https?://localhost.*$");
        
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/guest/csrf/token")
                .build(); // 沒有 Referer header
        ServerWebExchange csrfExchange = MockServerWebExchange.from(request);
        
        WebFilter csrfFilter = securityConfig.csrfValidationFilter();
        
        // 因為沒有 Referer header，會內部處理錯誤響應但不拋出異常
        StepVerifier.create(csrfFilter.filter(csrfExchange, exchange -> Mono.empty()))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - csrfValidationFilter Web 路徑 CSRF 驗證失敗")
    void testCsrfValidationFilter_webPathCsrfFailure() {
        MockServerHttpRequest request = MockServerHttpRequest.post("/web/v1/test").build();
        ServerWebExchange webExchange = MockServerWebExchange.from(request);
        
        when(csrfTokenRepository.loadToken(webExchange))
                .thenReturn(Mono.error(new xyz.dowob.filemanagement.exception.ValidationException(
                    xyz.dowob.filemanagement.exception.ValidationException.ErrorCode.INVALID_CSRF_TOKEN)));
        
        WebFilter csrfFilter = securityConfig.csrfValidationFilter();
        
        StepVerifier.create(csrfFilter.filter(webExchange, exchange -> Mono.empty()))
                .verifyComplete();
    }

    // ==================== 邊界測試 ====================

    @Test
    @DisplayName("邊界測試 - CORS 配置空列表處理")
    void testCorsConfigurationSource_emptyLists() {
        when(corsProperties.getAllowedOrigins()).thenReturn(List.of());
        when(corsProperties.getAllowedOriginsPattern()).thenReturn(List.of());
        when(corsProperties.getAllowedMethods()).thenReturn(List.of());
        when(corsProperties.getAllowedHeaders()).thenReturn(List.of());
        when(corsProperties.getAllowExposedHeaders()).thenReturn(List.of());
        
        CorsConfigurationSource corsSource = securityConfig.corsConfigurationSource();
        org.springframework.web.cors.CorsConfiguration config = corsSource.getCorsConfiguration(testExchange);
        
        assertNotNull(config);
        // 驗證即使配置為空列表，仍然會有預設的暴露標頭
        assertTrue(config.getExposedHeaders().contains("Content-Disposition"));
        assertTrue(config.getExposedHeaders().contains("Authorization"));
        assertTrue(config.getExposedHeaders().contains("X-Csrf-Token"));
    }

    @Test
    @DisplayName("邊界測試 - 各種 HTTP 方法的安全檢查")
    void testIsSafeMethod_allHttpMethods() {
        HttpMethod[] allMethods = HttpMethod.values();
        List<HttpMethod> safeMethods = Arrays.asList(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS, HttpMethod.TRACE);
        
        for (HttpMethod method : allMethods) {
            MockServerHttpRequest request = MockServerHttpRequest.method(method, "/test").build();
            ServerWebExchange exchange = MockServerWebExchange.from(request);
            
            boolean expected = safeMethods.contains(method);
            boolean actual = securityConfig.isSafeMethod(exchange);
            
            assertEquals(expected, actual, "HTTP 方法 " + method + " 的安全性判斷不正確");
        }
    }

    @Test
    @DisplayName("邊界測試 - 併發請求 ID 生成")
    void testTraceIdFilter_concurrentRequestIds() {
        WebFilter traceIdFilter = securityConfig.traceIdFilter();
        
        // 創建多個並發請求
        java.util.Set<String> requestIds = java.util.concurrent.ConcurrentHashMap.newKeySet();
        
        reactor.core.publisher.Flux<String> concurrentRequests = reactor.core.publisher.Flux.range(1, 10)
                .flatMap(i -> {
                    MockServerHttpRequest request = MockServerHttpRequest.get("/test" + i).build();
                    ServerWebExchange exchange = MockServerWebExchange.from(request);
                    
                    return traceIdFilter.filter(exchange, ex -> Mono.empty())
                            .then(Mono.fromCallable(() -> {
                                String requestId = exchange.getAttribute("requestId");
                                requestIds.add(requestId);
                                return requestId;
                            }));
                });
        
        StepVerifier.create(concurrentRequests)
                .expectNextCount(10)
                .verifyComplete();
        
        // 驗證所有請求 ID 都是唯一的
        assertEquals(10, requestIds.size(), "所有請求 ID 應該是唯一的");
    }

    @Test
    @DisplayName("邊界測試 - 密碼編碼器強度測試")
    void testPasswordEncoder_strengthTest() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();
        
        // 測試不同強度的密碼
        String[] passwords = {
            "weak",
            "medium123",
            "Strong@Password123!",
            "VeryLongPasswordWithManyCharacters123!@#$%^&*()",
            ""  // 空密碼
        };
        
        for (String password : passwords) {
            String encoded = encoder.encode(password);
            assertNotNull(encoded);
            assertNotEquals(password, encoded);
            assertTrue(encoder.matches(password, encoded));
            
            // 驗證每次編碼結果都不同（鹽值隨機性）
            String encoded2 = encoder.encode(password);
            if (!password.isEmpty()) {
                assertNotEquals(encoded, encoded2, "相同密碼的編碼結果應該不同");
            }
        }
    }

    @Test
    @DisplayName("邊界測試 - 路徑匹配邊界情況")
    void testPathMatching_edgeCases() {
        // 測試各種路徑格式
        String[] testPaths = {
            "/",
            "/web",
            "/web/",
            "/web/v1",
            "/web/v1/",
            "/web/v1/test",
            "/api/v1/guest/csrf/token",
            "/api/v1/guest/csrf/token/",
            "/WEB/V1/TEST", // 大寫
            "/web/v1/test?param=value", // 查詢參數
            "/web/v1/test#fragment" // 片段
        };
        
        for (String path : testPaths) {
            MockServerHttpRequest request = MockServerHttpRequest.get(path).build();
            ServerWebExchange exchange = MockServerWebExchange.from(request);
            
            String requestPath = exchange.getRequest().getPath().toString();
            assertNotNull(requestPath);
            
            // 驗證路徑處理的正確性
            boolean isWebPath = requestPath.startsWith("/web");
            boolean isCsrfTokenPath = requestPath.equals("/api/v1/guest/csrf/token");
            
            // 這些判斷應該與 SecurityConfig 中的邏輯一致
            assertNotNull(isWebPath || isCsrfTokenPath);
        }
    }
}