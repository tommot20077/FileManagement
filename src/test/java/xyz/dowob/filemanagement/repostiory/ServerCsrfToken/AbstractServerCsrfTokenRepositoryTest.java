package xyz.dowob.filemanagement.repostiory.ServerCsrfToken;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.config.properties.SecurityProperties;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * 抽象CSRF權杖儲存庫基礎類的測試實現。
 * <p>
 * 此測試類驗證 AbstractServerCsrfTokenRepository 的基礎框架功能，
 * 涵蓋安全性配置注入和抽象類設計模式的正確實現。
 * <p>
 * 測試範圍包含建構函式初始化、配置屬性注入、繼承關係驗證和抽象方法宣告檢查。
 * 特別驗證了CSRF權杖安全機制的基礎架構和配置管理。
 * <p>
 * 抽象類設計提供統一的安全配置注入點，子類實現具體的權杖儲存策略。
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("AbstractServerCsrfTokenRepository 抽象CSRF Token存儲庫測試")
class AbstractServerCsrfTokenRepositoryTest {

    @Mock
    private SecurityProperties securityProperties;

    @Mock
    private SecurityProperties.Csrf csrfProperties;

    private TestableAbstractServerCsrfTokenRepository testableRepository;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // 配置 SecurityProperties mocks
        when(securityProperties.getCsrf()).thenReturn(csrfProperties);
        when(csrfProperties.getHeaderName()).thenReturn("X-CSRF-TOKEN");
        when(csrfProperties.getParameterName()).thenReturn("_csrf");
        when(csrfProperties.getExpiration()).thenReturn(Duration.ofMinutes(30));

        // 創建測試對象
        testableRepository = new TestableAbstractServerCsrfTokenRepository(securityProperties);
    }


    @Test
    @DisplayName("一般測試 - 構造函數初始化")
    void testConstructorInitialization() {
        assertNotNull(testableRepository);

        // 驗證繼承關係
        assertTrue(testableRepository instanceof AbstractServerCsrfTokenRepository);
        assertTrue(testableRepository instanceof CustomServerCsrfTokenRepository);
    }

    // ==================== 一般測試 ====================


    @Test
    @DisplayName("一般測試 - SecurityProperties配置注入")
    void testSecurityPropertiesInjection() {
        // 驗證配置屬性正確注入
        assertEquals("X-CSRF-TOKEN", testableRepository.getCsrfTokenHeader());
        assertEquals("_csrf", testableRepository.getCsrfTokenParameter());
        assertEquals(Duration.ofMinutes(30), testableRepository.getExpireTime());
    }


    @Test
    @DisplayName("一般測試 - 抽象類屬性為protected")
    void testProtectedFields() {
        // 通過反射驗證屬性可見性
        try {
            var headerField = AbstractServerCsrfTokenRepository.class.getDeclaredField("csrfTokenHeader");
            var paramField = AbstractServerCsrfTokenRepository.class.getDeclaredField("csrfTokenParameter");
            var expireField = AbstractServerCsrfTokenRepository.class.getDeclaredField("expireTime");

            // 驗證是protected且final
            assertTrue(java.lang.reflect.Modifier.isProtected(headerField.getModifiers()));
            assertTrue(java.lang.reflect.Modifier.isFinal(headerField.getModifiers()));

            assertTrue(java.lang.reflect.Modifier.isProtected(paramField.getModifiers()));
            assertTrue(java.lang.reflect.Modifier.isFinal(paramField.getModifiers()));

            assertTrue(java.lang.reflect.Modifier.isProtected(expireField.getModifiers()));
            assertTrue(java.lang.reflect.Modifier.isFinal(expireField.getModifiers()));
        } catch (NoSuchFieldException e) {
            fail("Protected fields should exist in AbstractServerCsrfTokenRepository");
        }
    }


    @Test
    @DisplayName("一般測試 - 抽象方法聲明檢查")
    void testAbstractMethodDeclarations() {
        // 驗證抽象類實現了CustomServerCsrfTokenRepository接口的所有方法
        assertTrue(CustomServerCsrfTokenRepository.class.isAssignableFrom(AbstractServerCsrfTokenRepository.class));

        // 檢查類是否為抽象類
        assertTrue(java.lang.reflect.Modifier.isAbstract(AbstractServerCsrfTokenRepository.class.getModifiers()));
    }


    @Test
    @DisplayName("一般測試 - 接口方法實現檢查")
    void testInterfaceMethodImplementation() {
        // 驗證測試實現類正確實現了所有抽象方法
        assertDoesNotThrow(() -> {
            testableRepository.generateToken(null);
            testableRepository.saveToken(null, null);
            testableRepository.loadToken(null);
            testableRepository.deleteToken(null);
        });
    }


    @Test
    @DisplayName("一般測試 - 不同配置值的正確處理")
    void testDifferentConfigurationValues() {
        // 使用不同的配置創建新實例
        SecurityProperties altSecurityProperties = org.mockito.Mockito.mock(SecurityProperties.class);
        SecurityProperties.Csrf altCsrfProperties = org.mockito.Mockito.mock(SecurityProperties.Csrf.class);

        when(altSecurityProperties.getCsrf()).thenReturn(altCsrfProperties);
        when(altCsrfProperties.getHeaderName()).thenReturn("X-CUSTOM-CSRF");
        when(altCsrfProperties.getParameterName()).thenReturn("_custom_csrf");
        when(altCsrfProperties.getExpiration()).thenReturn(Duration.ofHours(1));

        TestableAbstractServerCsrfTokenRepository altRepository =
                new TestableAbstractServerCsrfTokenRepository(altSecurityProperties);

        assertEquals("X-CUSTOM-CSRF", altRepository.getCsrfTokenHeader());
        assertEquals("_custom_csrf", altRepository.getCsrfTokenParameter());
        assertEquals(Duration.ofHours(1), altRepository.getExpireTime());
    }


    @Test
    @DisplayName("一般測試 - 配置屬性不可變性")
    void testConfigurationImmutability() {
        // 驗證配置屬性在構造後不可更改
        String originalHeader = testableRepository.getCsrfTokenHeader();
        String originalParameter = testableRepository.getCsrfTokenParameter();
        Duration originalExpireTime = testableRepository.getExpireTime();

        // 驗證字段是final的（通過反射檢查修飾符）
        try {
            var headerField = AbstractServerCsrfTokenRepository.class.getDeclaredField("csrfTokenHeader");
            assertTrue(java.lang.reflect.Modifier.isFinal(headerField.getModifiers()),
                       "csrfTokenHeader field should be final");

            var parameterField = AbstractServerCsrfTokenRepository.class.getDeclaredField("csrfTokenParameter");
            assertTrue(java.lang.reflect.Modifier.isFinal(parameterField.getModifiers()),
                       "csrfTokenParameter field should be final");

            var expireTimeField = AbstractServerCsrfTokenRepository.class.getDeclaredField("expireTime");
            assertTrue(java.lang.reflect.Modifier.isFinal(expireTimeField.getModifiers()),
                       "expireTime field should be final");
        } catch (NoSuchFieldException e) {
            fail("Required fields should exist");
        }

        // 驗證值沒有被修改
        assertEquals(originalHeader, testableRepository.getCsrfTokenHeader());
        assertEquals(originalParameter, testableRepository.getCsrfTokenParameter());
        assertEquals(originalExpireTime, testableRepository.getExpireTime());
    }


    @Test
    @DisplayName("異常測試 - 傳入null SecurityProperties")
    void testNullSecurityProperties() {
        assertThrows(NullPointerException.class, () -> {
            new TestableAbstractServerCsrfTokenRepository(null);
        });
    }

    // ==================== 異常測試 ====================


    @Test
    @DisplayName("異常測試 - SecurityProperties返回null的Csrf配置")
    void testNullCsrfProperties() {
        SecurityProperties nullCsrfSecurityProperties = org.mockito.Mockito.mock(SecurityProperties.class);
        when(nullCsrfSecurityProperties.getCsrf()).thenReturn(null);

        assertThrows(NullPointerException.class, () -> {
            new TestableAbstractServerCsrfTokenRepository(nullCsrfSecurityProperties);
        });
    }


    @Test
    @DisplayName("異常測試 - Csrf配置返回null值")
    void testNullCsrfConfigurationValues() {
        SecurityProperties badConfigSecurityProperties = org.mockito.Mockito.mock(SecurityProperties.class);
        SecurityProperties.Csrf badCsrfProperties = org.mockito.Mockito.mock(SecurityProperties.Csrf.class);

        when(badConfigSecurityProperties.getCsrf()).thenReturn(badCsrfProperties);

        // 測試null header name
        when(badCsrfProperties.getHeaderName()).thenReturn(null);
        when(badCsrfProperties.getParameterName()).thenReturn("_csrf");
        when(badCsrfProperties.getExpiration()).thenReturn(Duration.ofMinutes(30));

        TestableAbstractServerCsrfTokenRepository repositoryWithNullHeader =
                new TestableAbstractServerCsrfTokenRepository(badConfigSecurityProperties);

        assertNull(repositoryWithNullHeader.getCsrfTokenHeader());

        // 測試null parameter name
        when(badCsrfProperties.getHeaderName()).thenReturn("X-CSRF-TOKEN");
        when(badCsrfProperties.getParameterName()).thenReturn(null);

        TestableAbstractServerCsrfTokenRepository repositoryWithNullParam =
                new TestableAbstractServerCsrfTokenRepository(badConfigSecurityProperties);

        assertNull(repositoryWithNullParam.getCsrfTokenParameter());

        // 測試null expiration
        when(badCsrfProperties.getParameterName()).thenReturn("_csrf");
        when(badCsrfProperties.getExpiration()).thenReturn(null);

        TestableAbstractServerCsrfTokenRepository repositoryWithNullExpiration =
                new TestableAbstractServerCsrfTokenRepository(badConfigSecurityProperties);

        assertNull(repositoryWithNullExpiration.getExpireTime());
    }


    @Test
    @DisplayName("邊界測試 - 極端配置值")
    void testExtremeConfigurationValues() {
        SecurityProperties extremeSecurityProperties = org.mockito.Mockito.mock(SecurityProperties.class);
        SecurityProperties.Csrf extremeCsrfProperties = org.mockito.Mockito.mock(SecurityProperties.Csrf.class);

        when(extremeSecurityProperties.getCsrf()).thenReturn(extremeCsrfProperties);

        // 極長的header name
        String veryLongHeader = "X-".repeat(1000) + "CSRF-TOKEN";
        when(extremeCsrfProperties.getHeaderName()).thenReturn(veryLongHeader);
        when(extremeCsrfProperties.getParameterName()).thenReturn("_csrf");
        when(extremeCsrfProperties.getExpiration()).thenReturn(Duration.ofNanos(1));

        TestableAbstractServerCsrfTokenRepository extremeRepository =
                new TestableAbstractServerCsrfTokenRepository(extremeSecurityProperties);

        assertEquals(veryLongHeader, extremeRepository.getCsrfTokenHeader());
        assertEquals(Duration.ofNanos(1), extremeRepository.getExpireTime());
    }

    // ==================== 邊界測試 ====================


    @Test
    @DisplayName("邊界測試 - 空字符串配置值")
    void testEmptyStringConfigurationValues() {
        SecurityProperties emptySecurityProperties = org.mockito.Mockito.mock(SecurityProperties.class);
        SecurityProperties.Csrf emptyCsrfProperties = org.mockito.Mockito.mock(SecurityProperties.Csrf.class);

        when(emptySecurityProperties.getCsrf()).thenReturn(emptyCsrfProperties);
        when(emptyCsrfProperties.getHeaderName()).thenReturn("");
        when(emptyCsrfProperties.getParameterName()).thenReturn("");
        when(emptyCsrfProperties.getExpiration()).thenReturn(Duration.ZERO);

        TestableAbstractServerCsrfTokenRepository emptyRepository =
                new TestableAbstractServerCsrfTokenRepository(emptySecurityProperties);

        assertEquals("", emptyRepository.getCsrfTokenHeader());
        assertEquals("", emptyRepository.getCsrfTokenParameter());
        assertEquals(Duration.ZERO, emptyRepository.getExpireTime());
    }


    @Test
    @DisplayName("邊界測試 - 特殊字符配置值")
    void testSpecialCharacterConfigurationValues() {
        SecurityProperties specialSecurityProperties = org.mockito.Mockito.mock(SecurityProperties.class);
        SecurityProperties.Csrf specialCsrfProperties = org.mockito.Mockito.mock(SecurityProperties.Csrf.class);

        when(specialSecurityProperties.getCsrf()).thenReturn(specialCsrfProperties);
        when(specialCsrfProperties.getHeaderName()).thenReturn("X-CSRF-TOKEN!@#$%^&*()");
        when(specialCsrfProperties.getParameterName()).thenReturn("_csrf_中文_🔒");
        when(specialCsrfProperties.getExpiration()).thenReturn(Duration.ofDays(365));

        TestableAbstractServerCsrfTokenRepository specialRepository =
                new TestableAbstractServerCsrfTokenRepository(specialSecurityProperties);

        assertEquals("X-CSRF-TOKEN!@#$%^&*()", specialRepository.getCsrfTokenHeader());
        assertEquals("_csrf_中文_🔒", specialRepository.getCsrfTokenParameter());
        assertEquals(Duration.ofDays(365), specialRepository.getExpireTime());
    }


    @Test
    @DisplayName("邊界測試 - Duration極值")
    void testDurationExtremeValues() {
        SecurityProperties durationSecurityProperties = org.mockito.Mockito.mock(SecurityProperties.class);
        SecurityProperties.Csrf durationCsrfProperties = org.mockito.Mockito.mock(SecurityProperties.Csrf.class);

        when(durationSecurityProperties.getCsrf()).thenReturn(durationCsrfProperties);
        when(durationCsrfProperties.getHeaderName()).thenReturn("X-CSRF-TOKEN");
        when(durationCsrfProperties.getParameterName()).thenReturn("_csrf");

        // 測試最大Duration
        when(durationCsrfProperties.getExpiration()).thenReturn(Duration.ofSeconds(Long.MAX_VALUE, 999999999));

        TestableAbstractServerCsrfTokenRepository maxDurationRepository =
                new TestableAbstractServerCsrfTokenRepository(durationSecurityProperties);

        assertEquals(Duration.ofSeconds(Long.MAX_VALUE, 999999999), maxDurationRepository.getExpireTime());

        // 測試負Duration
        when(durationCsrfProperties.getExpiration()).thenReturn(Duration.ofSeconds(-1));

        TestableAbstractServerCsrfTokenRepository negativeDurationRepository =
                new TestableAbstractServerCsrfTokenRepository(durationSecurityProperties);

        assertEquals(Duration.ofSeconds(-1), negativeDurationRepository.getExpireTime());
    }


    @Test
    @DisplayName("邊界測試 - 繼承層次驗證")
    void testInheritanceHierarchy() {
        // 驗證完整的繼承鏈
        assertTrue(CustomServerCsrfTokenRepository.class.isAssignableFrom(TestableAbstractServerCsrfTokenRepository.class));
        assertTrue(org.springframework.security.web.server.csrf.ServerCsrfTokenRepository.class
                  .isAssignableFrom(TestableAbstractServerCsrfTokenRepository.class));

        // 驗證接口方法存在
        try {
            TestableAbstractServerCsrfTokenRepository.class.getMethod("generateToken", ServerWebExchange.class);
            TestableAbstractServerCsrfTokenRepository.class.getMethod("saveToken", ServerWebExchange.class, CsrfToken.class);
            TestableAbstractServerCsrfTokenRepository.class.getMethod("loadToken", ServerWebExchange.class);
            TestableAbstractServerCsrfTokenRepository.class.getMethod("deleteToken", CsrfToken.class);
        } catch (NoSuchMethodException e) {
            fail("All required interface methods should be implemented: " + e.getMessage());
        }
    }


    @Test
    @DisplayName("邊界測試 - 多實例隔離性")
    void testMultipleInstanceIsolation() {
        // 創建多個實例，驗證它們的配置互不影響
        SecurityProperties config1 = org.mockito.Mockito.mock(SecurityProperties.class);
        SecurityProperties.Csrf csrf1 = org.mockito.Mockito.mock(SecurityProperties.Csrf.class);
        when(config1.getCsrf()).thenReturn(csrf1);
        when(csrf1.getHeaderName()).thenReturn("HEADER-1");
        when(csrf1.getParameterName()).thenReturn("param1");
        when(csrf1.getExpiration()).thenReturn(Duration.ofMinutes(10));

        SecurityProperties config2 = org.mockito.Mockito.mock(SecurityProperties.class);
        SecurityProperties.Csrf csrf2 = org.mockito.Mockito.mock(SecurityProperties.Csrf.class);
        when(config2.getCsrf()).thenReturn(csrf2);
        when(csrf2.getHeaderName()).thenReturn("HEADER-2");
        when(csrf2.getParameterName()).thenReturn("param2");
        when(csrf2.getExpiration()).thenReturn(Duration.ofMinutes(20));

        TestableAbstractServerCsrfTokenRepository repo1 = new TestableAbstractServerCsrfTokenRepository(config1);
        TestableAbstractServerCsrfTokenRepository repo2 = new TestableAbstractServerCsrfTokenRepository(config2);

        // 驗證實例間的配置隔離
        assertEquals("HEADER-1", repo1.getCsrfTokenHeader());
        assertEquals("HEADER-2", repo2.getCsrfTokenHeader());
        assertEquals("param1", repo1.getCsrfTokenParameter());
        assertEquals("param2", repo2.getCsrfTokenParameter());
        assertEquals(Duration.ofMinutes(10), repo1.getExpireTime());
        assertEquals(Duration.ofMinutes(20), repo2.getExpireTime());
    }

    // 測試用的具體實現類
    private static class TestableAbstractServerCsrfTokenRepository extends AbstractServerCsrfTokenRepository {

        public TestableAbstractServerCsrfTokenRepository(SecurityProperties securityProperties) {
            super(securityProperties);
        }

        @Override
        public Mono<CsrfToken> generateToken(ServerWebExchange exchange) {
            return Mono.empty();
        }

        @Override
        public Mono<Void> saveToken(ServerWebExchange exchange, CsrfToken token) {
            return Mono.empty();
        }

        @Override
        public Mono<CsrfToken> loadToken(ServerWebExchange exchange) {
            return Mono.empty();
        }

        @Override
        public Mono<Void> deleteToken(CsrfToken token) {
            return Mono.empty();
        }

        // 提供getter方法用於測試
        public String getCsrfTokenHeader() {
            return csrfTokenHeader;
        }

        public String getCsrfTokenParameter() {
            return csrfTokenParameter;
        }

        public Duration getExpireTime() {
            return expireTime;
        }
    }
}