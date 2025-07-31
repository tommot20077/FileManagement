package xyz.dowob.filemanagement.component.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.CsrfRepositoryType;
import xyz.dowob.filemanagement.config.properties.SecurityProperties;
import xyz.dowob.filemanagement.customenum.CsrfTokenRepositoryEnum;
import xyz.dowob.filemanagement.repostiory.ServerCsrfToken.CustomServerCsrfTokenRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * CsrfTokenRepositoryStrategy 測試類別。
 * 
 * <p>測試 CsrfTokenRepositoryStrategy 的 CSRF 斑證存储庫策略選擇功能，包括：
 * <ul>
 * <li>正常情況下的存储庫匹配與選擇</li>
 * <li>無效配置的異常處理</li>
 * <li>多個匹配存储庫的選擇策略</li>
 * <li>空存储庫清單的異常處理</li>
 * <li>註解配置的驗證與匹配</li>
 * </ul>
 * 
 * <p>測試涵蓋策略模式的所有核心逻輯，包含正常情況、異常處理和邊界條件。
 * 透過模擬不同的 CSRF 斑證存储庫實現，驗證策略選擇的正確性和健壯性。
 * 
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CsrfTokenRepositoryStrategy 邏輯處理測試")
class CsrfTokenRepositoryStrategyTest {

    @Mock
    private SecurityProperties securityProperties;

    private SecurityProperties.Csrf csrfConfig;

    @BeforeEach
    void setup() {
        csrfConfig = mock(SecurityProperties.Csrf.class);
        lenient().when(securityProperties.getCsrf()).thenReturn(csrfConfig);
    }

    @Test
    @DisplayName("正常情況：成功匹配到CsrfTokenRepository - 返回正確的Repository")
    void whenValidConfig_thenSelectCorrectRepository() {
        lenient().when(csrfConfig.getCsrfTokenRepository()).thenReturn(CsrfTokenRepositoryEnum.REDIS);

        List<CustomServerCsrfTokenRepository> repositories = List.of(
            new TestRepositoryA(),
            new TestRepositoryB()
        );

        CsrfTokenRepositoryStrategy strategy =
            new CsrfTokenRepositoryStrategy(repositories, securityProperties);

        assertTrue(strategy.getCsrfTokenRepository() instanceof TestRepositoryA);
    }

    @Test
    @DisplayName("異常情況：找不到匹配的CsrfTokenRepository - 拋出IllegalArgumentException")
    void whenNoMatchingRepository_thenThrowException() {
        when(csrfConfig.getCsrfTokenRepository()).thenReturn(null);

        List<CustomServerCsrfTokenRepository> repositories = List.of(
            new TestRepositoryA(),
            new TestRepositoryB()
        );

        assertThrows(IllegalArgumentException.class,
            () -> new CsrfTokenRepositoryStrategy(repositories, securityProperties));
    }

    @Test
    @DisplayName("邊界條件：多個匹配 - 選擇第一個符合條件的Repository")
    void whenMultipleMatches_thenSelectFirstOne() {
        when(csrfConfig.getCsrfTokenRepository()).thenReturn(CsrfTokenRepositoryEnum.REDIS);

        CustomServerCsrfTokenRepository firstRepo = new TestRepositoryA();
        List<CustomServerCsrfTokenRepository> repositories = List.of(
            firstRepo,
            new TestRepositoryA()
        );

        CsrfTokenRepositoryStrategy strategy =
            new CsrfTokenRepositoryStrategy(repositories, securityProperties);

        assertSame(firstRepo, strategy.getCsrfTokenRepository());
    }

    @Test
    @DisplayName("異常情況：傳入的CsrfTokenRepository列表為空 - 拋出IllegalArgumentException")
    void whenEmptyRepositoryList_thenThrowException() {
        SecurityProperties customProperties = mock(SecurityProperties.class);
        List<CustomServerCsrfTokenRepository> repositories = List.of();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> new CsrfTokenRepositoryStrategy(repositories, customProperties));

        assertEquals("找不到對應的 CsrfTokenRepository", exception.getMessage());
    }

    @CsrfRepositoryType(CsrfTokenRepositoryEnum.REDIS)
    static class TestRepositoryA implements CustomServerCsrfTokenRepository {
        @Override
        public Mono<Void> deleteToken(CsrfToken token) {
            return null;
        }

        @Override
        public Mono<CsrfToken> generateToken(ServerWebExchange exchange) {
            return null;
        }

        @Override
        public Mono<Void> saveToken(ServerWebExchange exchange, CsrfToken token) {
            return null;
        }

        @Override
        public Mono<CsrfToken> loadToken(ServerWebExchange exchange) {
            return null;
        }
    }

    @CsrfRepositoryType(CsrfTokenRepositoryEnum.LOCAL)
    static class TestRepositoryB implements CustomServerCsrfTokenRepository {
        @Override
        public Mono<Void> deleteToken(CsrfToken token) {
            return null;
        }

        @Override
        public Mono<CsrfToken> generateToken(ServerWebExchange exchange) {
            return null;
        }

        @Override
        public Mono<Void> saveToken(ServerWebExchange exchange, CsrfToken token) {
            return null;
        }

        @Override
        public Mono<CsrfToken> loadToken(ServerWebExchange exchange) {
            return null;
        }
    }
}
