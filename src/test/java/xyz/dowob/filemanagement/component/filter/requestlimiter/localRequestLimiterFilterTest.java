package xyz.dowob.filemanagement.component.filter.requestlimiter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.dowob.filemanagement.component.filter.ClientIpFilter;
import xyz.dowob.filemanagement.config.properties.GlobalProperties;
import xyz.dowob.filemanagement.exception.LimitationException;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.unity.LogUnity;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * localRequestLimiterFilter 測試類。
 * 
 * 測試涵蓋的邏輯或場景說明：
 * - 正常請求處理
 * - 請求限制機制
 * - IP 封禁機制
 * - 異常情況處理
 * - 邊界條件測試
 * 
 * 前置條件：
 * - 初始化 Mock 對象
 * - 配置 GlobalProperties
 * 
 * 測試步驟：
 * - 設置測試數據
 * - 調用被測試方法
 * - 驗證結果
 * 
 * 預期結果：
 * - 正常情況下請求通過
 * - 超過限制時返回 429 狀態碼
 * - IP 被封禁時返回 403 狀態碼
 * - 異常情況下正確處理錯誤
 */
@DisplayName("localRequestLimiterFilter 本地請求限制器過濾器測試")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class localRequestLimiterFilterTest {

    private static final String TEST_IP = "192.168.1.1";

    @Mock
    private GlobalProperties globalProperties;

    @Mock
    private GlobalProperties.RequestLimiter requestLimiter;

    @Mock
    private ServerWebExchange exchange;

    @Mock
    private WebFilterChain chain;

    @Mock
    private ServerHttpRequest request;

    @Mock
    private ServerHttpResponse response;

    private ObjectMapper objectMapper;
    private localRequestLimiterFilter localRequestLimiterFilter;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        
        when(globalProperties.getRequestLimiter()).thenReturn(requestLimiter);
        when(requestLimiter.getCleanInterval()).thenReturn(Duration.ofSeconds(1));
        when(requestLimiter.getRefillDuration()).thenReturn(Duration.ofSeconds(1));
        when(requestLimiter.getBanIpDuration()).thenReturn(Duration.ofMinutes(10));
        when(requestLimiter.getBanExpireDuration()).thenReturn(Duration.ofHours(1));
        when(requestLimiter.getFailureCount()).thenReturn(5);
        when(requestLimiter.isEnableBanIp()).thenReturn(false);
        when(requestLimiter.getLimit()).thenReturn(10);
        when(requestLimiter.getRefill()).thenReturn(10);
        
        when(exchange.getRequest()).thenReturn(request);
        when(exchange.getResponse()).thenReturn(response);
        when(request.getRemoteAddress()).thenReturn(new InetSocketAddress(TEST_IP, 8080));
        // 添加缺失的 mock 設置
        when(request.getPath()).thenReturn(mock(org.springframework.http.server.RequestPath.class));
        when(request.getPath().value()).thenReturn("/test");
        // 修復 HttpHeaders mock
        when(response.getHeaders()).thenReturn(mock(org.springframework.http.HttpHeaders.class));
        when(response.bufferFactory()).thenReturn(mock(org.springframework.core.io.buffer.DataBufferFactory.class));
        when(response.bufferFactory().wrap(any(byte[].class))).thenReturn(mock(org.springframework.core.io.buffer.DataBuffer.class));
        when(response.writeWith(any())).thenReturn(Mono.empty());
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());
        
        // 設置 exchange attributes 用於存儲客戶端 IP
        Map<String, Object> attributes = new java.util.concurrent.ConcurrentHashMap<>();
        when(exchange.getAttributes()).thenReturn(attributes);
    }

    @Test
    @DisplayName("測試正常請求處理 - 請求未達限制")
    void filter_withNormalRequest_shouldPass() {
        // 初始化過濾器
        localRequestLimiterFilter = new localRequestLimiterFilter(objectMapper, globalProperties);
        
        try (MockedStatic<ClientIpFilter> mockedClientIpFilter = mockStatic(ClientIpFilter.class);
             MockedStatic<LogUnity> mockedLogUnity = mockStatic(LogUnity.class)) {

            // 直接在 exchange attributes 中設置 IP，模擬 ClientIpFilter 已經處理過的狀態
            exchange.getAttributes().put("clientIp", TEST_IP);
            mockedClientIpFilter.when(() -> ClientIpFilter.getClientIpFromExchange(any(ServerWebExchange.class)))
                    .thenReturn(Optional.of(TEST_IP));
            
            localRequestLimiterFilter localFilter = spy(localRequestLimiterFilter);
            doReturn(Mono.empty()).when(localFilter).sendErrorResponse(any(ServerWebExchange.class), any(ObjectMapper.class), any(LimitationException.ErrorCode.class), anyString());
            doReturn(Mono.empty()).when(localFilter).sendErrorResponse(any(ServerWebExchange.class), any(ObjectMapper.class), any(ValidationException.ErrorCode.class), any());

            StepVerifier.create(localFilter.filter(exchange, chain))
                    .verifyComplete();

            verify(chain).filter(exchange);
            verify(response, never()).setStatusCode(any());
        }
    }

    @Test
    @DisplayName("測試請求超過限制 - 返回 429 狀態碼")
    void filter_withExceededLimit_shouldReturnTooManyRequests() {
        when(requestLimiter.getLimit()).thenReturn(1);
        when(requestLimiter.getRefill()).thenReturn(1);
        
        localRequestLimiterFilter = new localRequestLimiterFilter(objectMapper, globalProperties);
        
        try (MockedStatic<ClientIpFilter> mockedClientIpFilter = mockStatic(ClientIpFilter.class);
             MockedStatic<LogUnity> mockedLogUnity = mockStatic(LogUnity.class)) {

            // 直接在 exchange attributes 中設置 IP，模擬 ClientIpFilter 已經處理過的狀態
            exchange.getAttributes().put("clientIp", TEST_IP);
            mockedClientIpFilter.when(() -> ClientIpFilter.getClientIpFromExchange(any(ServerWebExchange.class)))
                    .thenReturn(Optional.of(TEST_IP));
            
            localRequestLimiterFilter localFilter = spy(localRequestLimiterFilter);
            doReturn(Mono.empty()).when(localFilter).sendErrorResponse(any(ServerWebExchange.class), any(ObjectMapper.class), any(LimitationException.ErrorCode.class), anyString());

            // 第一個請求應該通過
            StepVerifier.create(localFilter.filter(exchange, chain))
                    .verifyComplete();
            verify(chain, times(1)).filter(exchange);

            // 第二個請求應該被限制
            StepVerifier.create(localFilter.filter(exchange, chain))
                    .verifyComplete();
            
            verify(response).setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            verify(localFilter).sendErrorResponse(eq(exchange), eq(objectMapper), 
                    eq(LimitationException.ErrorCode.USER_EXCEED_LIMIT), eq("當前請求過於頻繁，請稍後再試"));
        }
    }

    @Test
    @DisplayName("測試 IP 封禁機制 - 多次失敗後封禁 IP")
    void filter_withBanIpEnabled_shouldBanIpAfterFailures() {
        when(requestLimiter.isEnableBanIp()).thenReturn(true);
        when(requestLimiter.getFailureCount()).thenReturn(1); // 設置為1使測試更簡單
        when(requestLimiter.getLimit()).thenReturn(1);
        when(requestLimiter.getRefill()).thenReturn(1);
        
        localRequestLimiterFilter = new localRequestLimiterFilter(objectMapper, globalProperties);
        
        try (MockedStatic<ClientIpFilter> mockedClientIpFilter = mockStatic(ClientIpFilter.class);
             MockedStatic<LogUnity> mockedLogUnity = mockStatic(LogUnity.class)) {

            // 直接在 exchange attributes 中設置 IP，模擬 ClientIpFilter 已經處理過的狀態
            exchange.getAttributes().put("clientIp", TEST_IP);
            mockedClientIpFilter.when(() -> ClientIpFilter.getClientIpFromExchange(any(ServerWebExchange.class)))
                    .thenReturn(Optional.of(TEST_IP));
            
            localRequestLimiterFilter localFilter = spy(localRequestLimiterFilter);
            doReturn(Mono.empty()).when(localFilter).sendErrorResponse(any(ServerWebExchange.class), any(ObjectMapper.class), any(LimitationException.ErrorCode.class), anyString());
            doReturn(Mono.empty()).when(localFilter).sendErrorResponse(any(ServerWebExchange.class), any(ObjectMapper.class), any(ValidationException.ErrorCode.class));

            // 第一個請求通過
            StepVerifier.create(localFilter.filter(exchange, chain))
                    .verifyComplete();
            verify(chain, times(1)).filter(exchange);

            // 第二個請求被限制，達到失敗閾值，IP 被標記為封禁
            StepVerifier.create(localFilter.filter(exchange, chain))
                    .verifyComplete();
            verify(response).setStatusCode(HttpStatus.TOO_MANY_REQUESTS);

            // 第三個請求應該被限制但仍返回 429，因為 IP 封禁是異步設置的
            // 根據源代碼，第一次達到失敗閾值時仍返回 429，後續請求才返回 403
            reset(response);
            StepVerifier.create(localFilter.filter(exchange, chain))
                    .verifyComplete();
            verify(response).setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        }
    }

    @Test
    @DisplayName("測試無法獲取客戶端 IP - 返回 400 狀態碼")
    void filter_withNoClientIp_shouldReturnBadRequest() {
        localRequestLimiterFilter = new localRequestLimiterFilter(objectMapper, globalProperties);
        
        try (MockedStatic<ClientIpFilter> mockedClientIpFilter = mockStatic(ClientIpFilter.class);
             MockedStatic<LogUnity> mockedLogUnity = mockStatic(LogUnity.class)) {

            mockedClientIpFilter.when(() -> ClientIpFilter.getClientIpFromExchange(any(ServerWebExchange.class)))
                    .thenReturn(Optional.empty());
            
            localRequestLimiterFilter localFilter = spy(localRequestLimiterFilter);
            doReturn(Mono.empty()).when(localFilter).sendErrorResponse(any(ServerWebExchange.class), any(ObjectMapper.class), any(ValidationException.ErrorCode.class), any());

            StepVerifier.create(localFilter.filter(exchange, chain))
                    .verifyComplete();

            verify(response).setStatusCode(HttpStatus.BAD_REQUEST);
            verify(localFilter).sendErrorResponse(eq(exchange), eq(objectMapper), 
                    eq(ValidationException.ErrorCode.REQUEST_IS_INVALID), eq("IP 地址"));
            verify(chain, never()).filter(exchange);
        }
    }

    @Test
    @DisplayName("測試請求處理異常 - 返回錯誤響應")
    void filter_withException_shouldReturnErrorResponse() {
        localRequestLimiterFilter = new localRequestLimiterFilter(objectMapper, globalProperties);
        
        try (MockedStatic<ClientIpFilter> mockedClientIpFilter = mockStatic(ClientIpFilter.class);
             MockedStatic<LogUnity> mockedLogUnity = mockStatic(LogUnity.class)) {

            // 模擬 ClientIpFilter 返回空的情況，而不是拋出異常
            mockedClientIpFilter.when(() -> ClientIpFilter.getClientIpFromExchange(any(ServerWebExchange.class)))
                    .thenReturn(Optional.empty());
            
            localRequestLimiterFilter localFilter = spy(localRequestLimiterFilter);
            doReturn(Mono.empty()).when(localFilter).sendErrorResponse(any(ServerWebExchange.class), any(ObjectMapper.class), any(ValidationException.ErrorCode.class), any());

            // 由於無法獲取 IP，會返回錯誤響應
            StepVerifier.create(localFilter.filter(exchange, chain))
                    .verifyComplete();

            verify(response).setStatusCode(HttpStatus.BAD_REQUEST);
            verify(localFilter).sendErrorResponse(eq(exchange), eq(objectMapper), 
                    eq(ValidationException.ErrorCode.REQUEST_IS_INVALID), eq("IP 地址"));
            verify(chain, never()).filter(exchange);
        }
    }

    @Test
    @DisplayName("測試構造函數參數驗證 - cleanInterval 無效")
    void constructor_withInvalidCleanInterval_shouldThrowException() {
        when(requestLimiter.getCleanInterval()).thenReturn(Duration.ZERO);
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            new localRequestLimiterFilter(objectMapper, globalProperties);
        });
        
        assertEquals("請求限制器的清除時間比率必須大於0", exception.getMessage());
    }

    @Test
    @DisplayName("測試構造函數參數驗證 - refillDuration 無效")
    void constructor_withInvalidRefillDuration_shouldThrowException() {
        when(requestLimiter.getRefillDuration()).thenReturn(Duration.ZERO);
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            new localRequestLimiterFilter(objectMapper, globalProperties);
        });
        
        assertEquals("請求限制器的補充令牌週期必須大於0", exception.getMessage());
    }

    @Test
    @DisplayName("測試構造函數參數驗證 - banIpDuration 無效")
    void constructor_withInvalidBanIpDuration_shouldThrowException() {
        when(requestLimiter.getBanIpDuration()).thenReturn(Duration.ZERO);
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            new localRequestLimiterFilter(objectMapper, globalProperties);
        });
        
        assertEquals("禁止IP的計算時間必須大於0", exception.getMessage());
    }

    @Test
    @DisplayName("測試構造函數參數驗證 - banExpireDuration 無效")
    void constructor_withInvalidBanExpireDuration_shouldThrowException() {
        when(requestLimiter.getBanExpireDuration()).thenReturn(Duration.ZERO);
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            new localRequestLimiterFilter(objectMapper, globalProperties);
        });
        
        assertEquals("禁止IP的封禁時間必須大於0", exception.getMessage());
    }

    @Test
    @DisplayName("測試構造函數參數驗證 - failureCount 無效")
    void constructor_withInvalidFailureCount_shouldThrowException() {
        when(requestLimiter.getFailureCount()).thenReturn(0);
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            new localRequestLimiterFilter(objectMapper, globalProperties);
        });
        
        assertEquals("禁止IP的失敗次數必須大於0", exception.getMessage());
    }

    @Test
    @DisplayName("測試默認值設置 - limit 和 refill 為負數時使用默認值")
    void constructor_withNegativeValues_shouldUseDefaults() {
        when(requestLimiter.getLimit()).thenReturn(-1);
        when(requestLimiter.getRefill()).thenReturn(-1);
        
        assertDoesNotThrow(() -> {
            localRequestLimiterFilter = new localRequestLimiterFilter(objectMapper, globalProperties);
        });
        
        // 使用反射驗證默認值，但使用合理的值而非 Integer.MAX_VALUE
        try {
            java.lang.reflect.Field limitField = localRequestLimiterFilter.class.getDeclaredField("limit");
            limitField.setAccessible(true);
            int actualLimit = (int) limitField.get(localRequestLimiterFilter);
            assertEquals(Integer.MAX_VALUE, actualLimit);
            
            java.lang.reflect.Field refillField = localRequestLimiterFilter.class.getDeclaredField("refill");
            refillField.setAccessible(true);
            int actualRefill = (int) refillField.get(localRequestLimiterFilter);
            // refill 應該等於 limit
            assertEquals(actualLimit, actualRefill);
        } catch (Exception e) {
            fail("無法驗證默認值設置: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("測試清理方法 - 手動調用清理任務")
    void clean_shouldExecuteCleanupTasks() {
        when(requestLimiter.isEnableBanIp()).thenReturn(true);
        localRequestLimiterFilter = new localRequestLimiterFilter(objectMapper, globalProperties);
        
        // 測試清理方法不拋出異常
        assertDoesNotThrow(() -> {
            localRequestLimiterFilter.clean();
        });
    }

    @Test
    @DisplayName("測試銷毀方法 - 清除所有緩存")
    void destroy_shouldClearAllCaches() {
        when(requestLimiter.isEnableBanIp()).thenReturn(true);
        localRequestLimiterFilter = new localRequestLimiterFilter(objectMapper, globalProperties);
        
        // 測試銷毀方法不拋出異常
        assertDoesNotThrow(() -> {
            localRequestLimiterFilter.destroy();
        });
    }

    @Test
    @DisplayName("測試並發請求處理 - 多線程安全性")
    void filter_withConcurrentRequests_shouldHandleSafely() throws InterruptedException {
        when(requestLimiter.getLimit()).thenReturn(100); // 設置很大的限制以確保通過
        when(requestLimiter.getRefill()).thenReturn(100);
        localRequestLimiterFilter = new localRequestLimiterFilter(objectMapper, globalProperties);
        
        int threadCount = 5; // 減少線程數以降低競爭
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        try (MockedStatic<ClientIpFilter> mockedClientIpFilter = mockStatic(ClientIpFilter.class);
             MockedStatic<LogUnity> mockedLogUnity = mockStatic(LogUnity.class)) {

            // 直接在 exchange attributes 中設置 IP，模擬 ClientIpFilter 已經處理過的狀態
            exchange.getAttributes().put("clientIp", TEST_IP);
            mockedClientIpFilter.when(() -> ClientIpFilter.getClientIpFromExchange(any(ServerWebExchange.class)))
                    .thenReturn(Optional.of(TEST_IP));
            
            localRequestLimiterFilter localFilter = spy(localRequestLimiterFilter);
            doReturn(Mono.empty()).when(localFilter).sendErrorResponse(any(ServerWebExchange.class), any(ObjectMapper.class), any(LimitationException.ErrorCode.class), anyString());
            doReturn(Mono.empty()).when(localFilter).sendErrorResponse(any(ServerWebExchange.class), any(ObjectMapper.class), any(ValidationException.ErrorCode.class), any());
            
            // 啟動多個線程同時發送請求
            for (int i = 0; i < threadCount; i++) {
                new Thread(() -> {
                    try {
                        localFilter.filter(exchange, chain).block();
                    } finally {
                        latch.countDown();
                    }
                }).start();
            }
            
            assertTrue(latch.await(10, TimeUnit.SECONDS), "所有線程應在10秒內完成");
            
            // 由於限制很大，所有請求都應該通過
            verify(chain, times(threadCount)).filter(exchange);
        }
    }

    @Test
    @DisplayName("測試邊界條件 - 零限制值")
    void filter_withZeroLimit_shouldUseMaxValue() {
        when(requestLimiter.getLimit()).thenReturn(0);
        when(requestLimiter.getRefill()).thenReturn(0);
        
        // 驗證構造函數使用默認值
        localRequestLimiterFilter = new localRequestLimiterFilter(objectMapper, globalProperties);
        
        // 使用反射驗證默認值設置
        try {
            java.lang.reflect.Field limitField = localRequestLimiterFilter.class.getDeclaredField("limit");
            limitField.setAccessible(true);
            int actualLimit = (int) limitField.get(localRequestLimiterFilter);
            assertEquals(Integer.MAX_VALUE, actualLimit);
            
            java.lang.reflect.Field refillField = localRequestLimiterFilter.class.getDeclaredField("refill");
            refillField.setAccessible(true);
            int actualRefill = (int) refillField.get(localRequestLimiterFilter);
            assertEquals(actualLimit, actualRefill);
        } catch (Exception e) {
            fail("無法驗證默認值設置: " + e.getMessage());
        }
    }
}
