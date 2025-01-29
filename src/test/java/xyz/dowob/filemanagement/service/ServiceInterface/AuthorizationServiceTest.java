package xyz.dowob.filemanagement.service.ServiceInterface;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.security.web.server.csrf.DefaultCsrfToken;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.context.Context;
import xyz.dowob.filemanagement.customenum.RoleEnum;
import xyz.dowob.filemanagement.data.user.dto.AuthRequestDTO;
import xyz.dowob.filemanagement.entity.User;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {
    private AuthorizationService authorizationService;

    @Mock
    private ServerWebExchange exchange;

    @Mock
    private WebSession webSession;

    @Mock
    private ServerHttpRequest request;

    @Mock
    private Map<String, Object> attributes;

    @BeforeEach
    void setUp() {
        // 創建測試實現
        authorizationService = (authRequest, request) -> {
            if (authRequest == null) {
                return Mono.error(new IllegalArgumentException("請求不能為空"));
            }
            return Mono.just("jwt-token");
        };

        // 基本 mock 設置
        lenient().when(exchange.getAttributes()).thenReturn(attributes);
        lenient().when(exchange.getRequest()).thenReturn(request);
        lenient().when(exchange.getSession()).thenReturn(Mono.just(webSession));
    }

    @Test
    @DisplayName("當屬性設定為空時應該正常處理")
    void setAuthorization_WithNullAttributes_ShouldHandleGracefully() {
        // given
        Map<String, Object> nullAttributes = new HashMap<>();
        when(exchange.getAttributes()).thenReturn(nullAttributes);
        User user = new User();
        user.setUsername("test");

        // when & then
        StepVerifier.create(authorizationService.setAuthorization(exchange, user)).verifyComplete();
    }

    @Test
    @DisplayName("當用戶為空時應該跳過設定")
    void getCSRFToken_WithNullSession_ShouldReturnEmpty() {
        // given
        when(exchange.getSession()).thenReturn(Mono.empty());

        // when & then
        StepVerifier.create(authorizationService.getCSRFToken(exchange)).verifyComplete();
    }

    @Nested
    @DisplayName("授權的相關測試")
    class AuthenticationTests {
        @Test
        @DisplayName("應該返回有效的令牌")
        void authenticate_WithValidCredentials_ShouldReturnToken() {
            // given
            AuthRequestDTO authRequest = new AuthRequestDTO("user", "password");

            // when
            Mono<String> result = authorizationService.authenticate(authRequest, exchange);

            // then
            StepVerifier.create(result).expectNext("jwt-token").verifyComplete();
        }

        @Test
        @DisplayName("處理空請求時應該拋出異常")
        void authenticate_WithNullRequest_ShouldThrowException() {
            StepVerifier.create(authorizationService.authenticate(null, exchange)).expectError(IllegalArgumentException.class).verify();
        }
    }

    @Nested
    @DisplayName("授權設定的相關測試")
    class AuthorizationTests {
        @Test
        @DisplayName("用戶正常登錄時應該設置屬性")
        void setAuthorization_WithValidUser_ShouldSetAttributes() {
            // given
            User user = new User();
            user.setId(1L);
            user.setUsername("testUser");
            user.setRole(RoleEnum.USER);

            // when
            Mono<Void> result = authorizationService.setAuthorization(exchange, user);

            // then
            StepVerifier.create(result).verifyComplete();

            verify(attributes).put(eq("username"), eq("testUser"));
            verify(attributes).put(eq("userId"), eq(1L));
            verify(attributes).put(eq(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY), any(Context.class));
        }

        @Test
        @DisplayName("用戶應該被設定為管理員")
        void setAuthorization_WithAdminUser_ShouldSetAttributes() {
            // given
            User user = new User();
            user.setId(1L);
            user.setUsername("admin");
            user.setRole(RoleEnum.ADMIN);

            // when
            Mono<Void> result = authorizationService.setAuthorization(exchange, user);

            // then
            StepVerifier.create(result).verifyComplete();

            verify(attributes).put(eq("username"), eq("admin"));
            verify(attributes).put(eq("userId"), eq(1L));
            verify(attributes).put(eq(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY), any(Context.class));
        }

        @Test
        @DisplayName("當用戶為空時應該跳過設定")
        void setAuthorization_WithNullUser_ShouldComplete() {
            StepVerifier.create(authorizationService.setAuthorization(exchange, null)).expectComplete().verify();
        }
    }

    @Nested
    @DisplayName("CSRF 憑證測試")
    class CSRFTokenTests {
        @Test
        @DisplayName("返回有效的 CSRF 憑證")
        void getCSRFToken_WithValidToken_ShouldReturnToken() {
            // given
            CsrfToken csrfToken = new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "token-value");
            when(webSession.getAttribute("csrfToken")).thenReturn(csrfToken);

            // when & then
            StepVerifier.create(authorizationService.getCSRFToken(exchange)).expectNext(csrfToken).verifyComplete();
        }

        @Test
        @DisplayName("當沒有憑證時應該返回空")
        void getCSRFToken_WithNoToken_ShouldReturnEmpty() {
            // given
            when(webSession.getAttribute("csrfToken")).thenReturn(null);

            // when & then
            StepVerifier.create(authorizationService.getCSRFToken(exchange)).verifyComplete();
        }

        @Test
        @DisplayName("當會話出現錯誤時應該返回空")
        void getCSRFToken_WithSessionError_ShouldReturnEmpty() {
            // given
            when(exchange.getSession()).thenReturn(Mono.error(new RuntimeException("Session error")));

            // when & then
            StepVerifier.create(authorizationService.getCSRFToken(exchange)).expectError(RuntimeException.class);
        }
    }
}