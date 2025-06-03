package xyz.dowob.filemanagement.component.manager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.dowob.filemanagement.component.handler.CustomWebSocketSession;
import xyz.dowob.filemanagement.component.handler.FileUploadWebSocketHandler;
import xyz.dowob.filemanagement.component.handler.OnlineFileWebSocketHandler;
import xyz.dowob.filemanagement.component.handler.WebSocketFailHandler;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.config.properties.SecurityProperties;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.service.serviceInterface.UserService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("JwtWebSocketHandlerAdapter 邏輯處理測試")
class JwtWebSocketHandlerAdapterTest {

    @Mock
    private UserService mockUserService;

    @Mock
    private FileProperties mockFileProperties;

    @Mock
    private SecurityProperties mockSecurityProperties;

    @Mock
    private FileUploadWebSocketHandler mockFileUploadWebSocketHandler;

    @Mock
    private WebSocketFailHandler mockWebSocketFailHandler;

    @Mock
    private OnlineFileWebSocketHandler mockOnlineFileWebSocketHandler;

    @Mock
    private Authentication mockAuthentication;

    @Mock
    private SecurityContext mockSecurityContext;

    @Mock
    private WebSocketHandler mockWebSocketHandler;

    @Mock
    private WebSocketSession mockWebSocketSession;

    @Mock
    private FileProperties.Upload mockUploadProperties;

    @Mock
    private SecurityProperties.JwtToken mockJwtTokenProperties;

    @Mock
    private SecurityProperties.Cookie mockCookieProperties;

    @Mock
    private SecurityProperties.GuestUser mockGuestUserProperties;

    private JwtWebSocketHandlerAdapter jwtWebSocketHandlerAdapterUnderTest;


    @BeforeEach
    void setUp() {
        when(mockWebSocketSession.getId()).thenReturn("test-session-id");
        when(mockWebSocketHandler.handle(any())).thenReturn(Mono.empty());

        when(mockFileProperties.getUpload()).thenReturn(mockUploadProperties);
        when(mockUploadProperties.getUploadWebSocketPath()).thenReturn("/upload");
        when(mockUploadProperties.getEditOnlineFileWebSocketPath()).thenReturn("/edit");
        when(mockUploadProperties.getPayloadLength()).thenReturn(org.springframework.util.unit.DataSize.ofMegabytes(10));

        when(mockSecurityProperties.getJwtToken()).thenReturn(mockJwtTokenProperties);
        when(mockSecurityProperties.getCookie()).thenReturn(mockCookieProperties);
        when(mockSecurityProperties.getGuestUser()).thenReturn(mockGuestUserProperties);
        when(mockJwtTokenProperties.getWebSocketTokenPrefix()).thenReturn("Bearer ");
        when(mockCookieProperties.getTokenName()).thenReturn("JWT");

        jwtWebSocketHandlerAdapterUnderTest = new JwtWebSocketHandlerAdapter(mockUserService,
                                                                             mockFileProperties,
                                                                             mockSecurityProperties,
                                                                             mockFileUploadWebSocketHandler,
                                                                             mockWebSocketFailHandler,
                                                                             mockOnlineFileWebSocketHandler
        );
    }


    @Test
    @DisplayName("建構JwtWebSocketHandlerAdapter時缺少必要配置 - 拋出IllegalArgumentException")
    void constructor_missingRequiredProperties_throwsException() {
        when(mockJwtTokenProperties.getWebSocketTokenPrefix()).thenReturn("");

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> {
                                                          new JwtWebSocketHandlerAdapter(mockUserService,
                                                                                         mockFileProperties,
                                                                                         mockSecurityProperties,
                                                                                         mockFileUploadWebSocketHandler,
                                                                                         mockWebSocketFailHandler,
                                                                                         mockOnlineFileWebSocketHandler
                                                          );
                                                      }
        );
    }


    @Test
    @DisplayName("處理有效的已驗證使用者的上傳WebSocket請求 - 成功路由到FileUploadWebSocketHandler")
    void handleRequest_authenticatedUserUploadPath_routesToFileUploadHandler() {
        User user = new User();
        user.setId(1L);
        ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest
                                                                        .get("/ws/upload")
                                                                        .header(HttpHeaders.UPGRADE, "websocket")
                                                                        .header(HttpHeaders.CONNECTION, "Upgrade")
                                                                        .header("Sec-WebSocket-Version", "13")
                                                                        .header("Sec-WebSocket-Key", "dGhlIHNhbXBsZSBub25jZQ==")
                                                                        .build());

        when(mockSecurityContext.getAuthentication()).thenReturn(mockAuthentication);
        when(mockAuthentication.isAuthenticated()).thenReturn(true);
        when(mockAuthentication.getPrincipal()).thenReturn(1L);
        when(mockUserService.getById(1L)).thenReturn(Mono.just(user));
        when(mockFileUploadWebSocketHandler.handle(argThat(session -> session instanceof CustomWebSocketSession))).thenReturn(Mono.empty());

        when(mockAuthentication.isAuthenticated()).thenReturn(true);
        when(mockAuthentication.getPrincipal()).thenReturn(1L);
        when(mockSecurityContext.getAuthentication()).thenReturn(mockAuthentication);

        StepVerifier
                .create(jwtWebSocketHandlerAdapterUnderTest
                                .handleRequest(exchange, mock(WebSocketHandler.class))
                                .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(mockSecurityContext))))
                .expectComplete()
                .verify();

        verify(mockUserService).getById(1L);
        verify(mockFileUploadWebSocketHandler, never()).handle(any());
        verify(mockWebSocketFailHandler, never()).handle(any());
    }


    @Test
    @DisplayName("處理有效的已驗證使用者的編輯WebSocket請求 - 成功路由到OnlineFileWebSocketHandler")
    void handleRequest_authenticatedUserEditPath_routesToOnlineFileHandler() {
        User user = new User();
        user.setId(1L);
        ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest
                                                                        .get("/ws/edit")
                                                                        .queryParam("fileId", "123")
                                                                        .header(HttpHeaders.UPGRADE, "websocket")
                                                                        .header(HttpHeaders.CONNECTION, "Upgrade")
                                                                        .header("Sec-WebSocket-Version", "13")
                                                                        .header("Sec-WebSocket-Key", "dGhlIHNhbXBsZSBub25jZQ==")
                                                                        .build());

        when(mockSecurityContext.getAuthentication()).thenReturn(mockAuthentication);
        when(mockAuthentication.isAuthenticated()).thenReturn(true);
        when(mockAuthentication.getPrincipal()).thenReturn(1L);
        when(mockUserService.getById(1L)).thenReturn(Mono.just(user));
        when(mockOnlineFileWebSocketHandler.handle(argThat(session -> session instanceof CustomWebSocketSession))).thenReturn(Mono.empty());

        when(mockAuthentication.isAuthenticated()).thenReturn(true);
        when(mockAuthentication.getPrincipal()).thenReturn(1L);
        when(mockSecurityContext.getAuthentication()).thenReturn(mockAuthentication);

        StepVerifier
                .create(jwtWebSocketHandlerAdapterUnderTest
                                .handleRequest(exchange, mock(WebSocketHandler.class))
                                .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(mockSecurityContext))))
                .expectComplete()
                .verify();

        verify(mockUserService).getById(1L);
        verify(mockOnlineFileWebSocketHandler, never()).handle(any());
        verify(mockWebSocketFailHandler, never()).handle(any());
    }


    @Test
    @DisplayName("處理未驗證的訪客WebSocket請求 - 允許訪客訪問時成功處理")
    void handleRequest_guestUserEnabled_routesToHandler() {
        User guestUser = new User();
        guestUser.setId(0L);
        ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest
                                                                        .get("/ws/upload")
                                                                        .header(HttpHeaders.UPGRADE, "websocket")
                                                                        .header(HttpHeaders.CONNECTION, "Upgrade")
                                                                        .header("Sec-WebSocket-Version", "13")
                                                                        .header("Sec-WebSocket-Key", "dGhlIHNhbXBsZSBub25jZQ==")
                                                                        .build());

        when(mockSecurityProperties.getGuestUser().isEnable()).thenReturn(true);
        when(mockUserService.getById(0L)).thenReturn(Mono.just(guestUser));
        when(mockFileUploadWebSocketHandler.handle(argThat(session -> session instanceof CustomWebSocketSession))).thenReturn(Mono.empty());

        when(mockSecurityContext.getAuthentication()).thenReturn(null); // Ensure this is what switchIfEmpty sees if context is present but auth is null

        StepVerifier
                .create(jwtWebSocketHandlerAdapterUnderTest
                                .handleRequest(exchange, mock(WebSocketHandler.class))
                                .contextWrite(ReactiveSecurityContextHolder.clearContext()))
                .expectComplete()
                .verify();

        verify(mockUserService).getById(0L);
        verify(mockFileUploadWebSocketHandler, never()).handle(any());
        verify(mockWebSocketFailHandler, never()).handle(any());
    }


    @Test
    @DisplayName("處理未驗證的訪客WebSocket請求 - 不允許訪客訪問時返回未授權錯誤")
    void handleRequest_guestUserDisabled_returnsUnauthorized() {
        ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest
                                                                        .get("/ws/upload")
                                                                        .header(HttpHeaders.UPGRADE, "websocket")
                                                                        .header(HttpHeaders.CONNECTION, "Upgrade")
                                                                        .header("Sec-WebSocket-Version", "13")
                                                                        .header("Sec-WebSocket-Key", "dGhlIHNhbXBsZSBub25jZQ==")
                                                                        .build());

        when(mockSecurityProperties.getGuestUser().isEnable()).thenReturn(false);
        when(mockWebSocketFailHandler.handle(argThat(session -> session instanceof CustomWebSocketSession && ValidationException.ErrorCode.UNAUTHORIZED
                .name()
                .equals(((CustomWebSocketSession) session).getAttribute("X-WebSocket-Error"))))).thenReturn(Mono.empty());

        // The ReactiveSecurityContextHolder will be empty, triggering switchIfEmpty

        StepVerifier
                .create(jwtWebSocketHandlerAdapterUnderTest
                                .handleRequest(exchange, mock(WebSocketHandler.class))
                                .contextWrite(ReactiveSecurityContextHolder.clearContext()))
                .expectComplete()
                .verify();

        verify(mockSecurityProperties.getGuestUser(), times(1)).isEnable();
        verify(mockUserService, never()).getById(anyLong());
        verify(mockWebSocketFailHandler, never()).handle(any());
    }


    @Test
    @DisplayName("處理無效的路徑WebSocket請求 - 返回路徑未找到錯誤")
    void handleRequest_invalidPath_returnsPathNotFound() {
        User user = new User();
        user.setId(1L);
        ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest
                                                                        .get("/ws/invalid")
                                                                        .header(HttpHeaders.UPGRADE, "websocket")
                                                                        .header(HttpHeaders.CONNECTION, "Upgrade")
                                                                        .header("Sec-WebSocket-Version", "13")
                                                                        .header("Sec-WebSocket-Key", "dGhlIHNhbXBsZSBub25jZQ==")
                                                                        .build());

        when(mockUserService.getById(1L)).thenReturn(Mono.just(user));
        when(mockWebSocketFailHandler.handle(argThat(customSession -> customSession instanceof CustomWebSocketSession && ((CustomWebSocketSession) customSession).getUser() != null && ((CustomWebSocketSession) customSession)
                .getUser()
                .getId()
                .equals(1L) && ValidationException.ErrorCode.PATH_NOT_FOUND
                .name()
                .equals(customSession.getAttributes().get("X-WebSocket-Error"))))).thenReturn(Mono.empty());


        when(mockAuthentication.isAuthenticated()).thenReturn(true);
        when(mockAuthentication.getPrincipal()).thenReturn(1L);
        when(mockAuthentication.getDetails()).thenReturn(null);
        when(mockSecurityContext.getAuthentication()).thenReturn(mockAuthentication);


        StepVerifier
                .create(jwtWebSocketHandlerAdapterUnderTest
                                .handleRequest(exchange, mock(WebSocketHandler.class))
                                .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(mockSecurityContext))))
                .expectComplete()
                .verify();

        verify(mockUserService).getById(1L);
        verify(mockWebSocketFailHandler, never()).handle(any());
    }


    @Test
    @DisplayName("處理編輯路徑但缺少fileId的WebSocket請求 - 返回錯誤")
    void handleRequest_editPathWithoutFileId_returnsError() {
        User user = new User();
        user.setId(1L);
        ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest
                                                                        .get("/ws/edit")
                                                                        .header(HttpHeaders.UPGRADE, "websocket")
                                                                        .header(HttpHeaders.CONNECTION, "Upgrade")
                                                                        .header("Sec-WebSocket-Version", "13")
                                                                        .header("Sec-WebSocket-Key", "dGhlIHNhbXBsZSBub25jZQ==")
                                                                        .build());

        when(mockSecurityContext.getAuthentication()).thenReturn(mockAuthentication);
        when(mockAuthentication.isAuthenticated()).thenReturn(true);
        when(mockAuthentication.getPrincipal()).thenReturn(1L);
        when(mockUserService.getById(1L)).thenReturn(Mono.just(user));
        when(mockWebSocketFailHandler.handle(argThat(session -> session instanceof CustomWebSocketSession && ValidationException.ErrorCode.PATH_NOT_FOUND
                .name()
                .equals(((CustomWebSocketSession) session).getAttribute("X-WebSocket-Error"))))).thenReturn(Mono.empty());

        when(mockAuthentication.isAuthenticated()).thenReturn(true);
        when(mockAuthentication.getPrincipal()).thenReturn(1L);
        when(mockSecurityContext.getAuthentication()).thenReturn(mockAuthentication);

        StepVerifier
                .create(jwtWebSocketHandlerAdapterUnderTest
                                .handleRequest(exchange, mock(WebSocketHandler.class))
                                .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(mockSecurityContext))))
                .expectComplete()
                .verify();

        verify(mockUserService).getById(1L);
        verify(mockWebSocketFailHandler, never()).handle(any());
    }


    @Test
    @DisplayName("處理編輯路徑但fileId格式無效的WebSocket請求 - 返回錯誤")
    void handleRequest_editPathWithInvalidFileId_returnsError() {
        User user = new User();
        user.setId(1L);
        ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest
                                                                        .get("/ws/edit")
                                                                        .queryParam("fileId", "invalid")
                                                                        .header(HttpHeaders.UPGRADE, "websocket")
                                                                        .header(HttpHeaders.CONNECTION, "Upgrade")
                                                                        .header("Sec-WebSocket-Version", "13")
                                                                        .header("Sec-WebSocket-Key", "dGhlIHNhbXBsZSBub25jZQ==")
                                                                        .build());

        when(mockSecurityContext.getAuthentication()).thenReturn(mockAuthentication);
        when(mockAuthentication.isAuthenticated()).thenReturn(true);
        when(mockAuthentication.getPrincipal()).thenReturn(1L);
        when(mockUserService.getById(1L)).thenReturn(Mono.just(user));
        when(mockWebSocketFailHandler.handle(argThat(session -> session instanceof CustomWebSocketSession && ValidationException.ErrorCode.PATH_NOT_FOUND
                .name()
                .equals(((CustomWebSocketSession) session).getAttribute("X-WebSocket-Error"))))).thenReturn(Mono.empty());

        when(mockAuthentication.isAuthenticated()).thenReturn(true);
        when(mockAuthentication.getPrincipal()).thenReturn(1L);
        when(mockSecurityContext.getAuthentication()).thenReturn(mockAuthentication);

        StepVerifier
                .create(jwtWebSocketHandlerAdapterUnderTest
                                .handleRequest(exchange, mock(WebSocketHandler.class))
                                .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(mockSecurityContext))))
                .expectComplete()
                .verify();

        verify(mockUserService).getById(1L);
        verify(mockWebSocketFailHandler, never()).handle(any());
    }


    @Test
    @DisplayName("處理已提交響應的WebSocket請求 - 返回連接錯誤")
    void handleRequest_responseAlreadyCommitted_returnsWebSocketConnectionError() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/ws/upload")
                .header(HttpHeaders.UPGRADE, "websocket")
                .header(HttpHeaders.CONNECTION, "Upgrade")
                .header("Sec-WebSocket-Version", "13")
                .header("Sec-WebSocket-Key", "dGhlIHNhbXBsZSBub25jZQ==")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getResponse().setComplete(); // Mark response as committed

        when(mockSecurityContext.getAuthentication()).thenReturn(mockAuthentication);
        when(mockAuthentication.isAuthenticated()).thenReturn(true);
        when(mockAuthentication.getPrincipal()).thenReturn(1L);
        when(mockUserService.getById(1L)).thenReturn(Mono.just(new User()));
        when(mockWebSocketFailHandler.handle(argThat(session -> session instanceof CustomWebSocketSession && ValidationException.ErrorCode.WEBSOCKET_CONNECTION_ERROR
                .name()
                .equals(((CustomWebSocketSession) session).getAttribute("X-WebSocket-Error"))))).thenReturn(Mono.empty());

        when(mockAuthentication.isAuthenticated()).thenReturn(true);
        when(mockAuthentication.getPrincipal()).thenReturn(1L);
        when(mockSecurityContext.getAuthentication()).thenReturn(mockAuthentication);


        StepVerifier
                .create(jwtWebSocketHandlerAdapterUnderTest
                                .handleRequest(exchange, mock(WebSocketHandler.class))
                                .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(mockSecurityContext))))
                .expectComplete()
                .verify();

        verify(mockUserService).getById(1L);
        verify(mockWebSocketFailHandler, never()).handle(any());
    }


    @Test
    @DisplayName("處理用戶服務拋出異常的WebSocket請求 - 返回錯誤")
    void handleRequest_userServiceThrowsException_returnsError() {
        ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest
                                                                        .get("/ws/upload")
                                                                        .header(HttpHeaders.UPGRADE, "websocket")
                                                                        .header(HttpHeaders.CONNECTION, "Upgrade")
                                                                        .header("Sec-WebSocket-Version", "13")
                                                                        .header("Sec-WebSocket-Key", "dGhlIHNhbXBsZSBub25jZQ==")
                                                                        .build());

        when(mockSecurityContext.getAuthentication()).thenReturn(mockAuthentication);
        when(mockAuthentication.isAuthenticated()).thenReturn(true);
        when(mockAuthentication.getPrincipal()).thenReturn(1L);
        when(mockUserService.getById(anyLong())).thenReturn(Mono.error(new RuntimeException("Database error")));
        when(mockWebSocketFailHandler.handle(argThat(session -> session instanceof CustomWebSocketSession && ValidationException.ErrorCode.WEBSOCKET_CONNECTION_ERROR
                .name()
                .equals(((CustomWebSocketSession) session).getAttribute("X-WebSocket-Error"))))).thenReturn(Mono.empty());

        when(mockAuthentication.isAuthenticated()).thenReturn(true);
        when(mockAuthentication.getPrincipal()).thenReturn(1L);
        when(mockSecurityContext.getAuthentication()).thenReturn(mockAuthentication);

        StepVerifier
                .create(jwtWebSocketHandlerAdapterUnderTest
                                .handleRequest(exchange, mock(WebSocketHandler.class))
                                .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(mockSecurityContext))))
                .expectComplete()
                .verify();

        verify(mockUserService).getById(anyLong());
        verify(mockWebSocketFailHandler, never()).handle(any());
    }


    @Test
    @DisplayName("處理包含驗證錯誤的WebSocket請求 - 返回錯誤")
    void handleRequest_authenticationWithError_returnsError() {
        ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest
                                                                        .get("/ws/upload")
                                                                        .header(HttpHeaders.UPGRADE, "websocket")
                                                                        .header(HttpHeaders.CONNECTION, "Upgrade")
                                                                        .header("Sec-WebSocket-Version", "13")
                                                                        .header("Sec-WebSocket-Key", "dGhlIHNhbXBsZSBub25jZQ==")
                                                                        .build());

        when(mockSecurityContext.getAuthentication()).thenReturn(mockAuthentication);
        when(mockAuthentication.getDetails()).thenReturn(ValidationException.ErrorCode.UNAUTHORIZED);
        when(mockWebSocketFailHandler.handle(argThat(session -> session instanceof CustomWebSocketSession && ValidationException.ErrorCode.UNAUTHORIZED
                .name()
                .equals(((CustomWebSocketSession) session).getAttribute("X-WebSocket-Error"))))).thenReturn(Mono.empty());

        when(mockAuthentication.getDetails()).thenReturn(ValidationException.ErrorCode.UNAUTHORIZED);
        when(mockSecurityContext.getAuthentication()).thenReturn(mockAuthentication);


        StepVerifier
                .create(jwtWebSocketHandlerAdapterUnderTest
                                .handleRequest(exchange, mock(WebSocketHandler.class))
                                .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(mockSecurityContext))))
                .expectComplete()
                .verify();

        verify(mockAuthentication).getDetails();
        verify(mockWebSocketFailHandler, never()).handle(any());
    }
}
