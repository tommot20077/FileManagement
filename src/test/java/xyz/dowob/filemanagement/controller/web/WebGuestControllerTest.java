package xyz.dowob.filemanagement.controller.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.dowob.filemanagement.config.properties.SecurityProperties;
import xyz.dowob.filemanagement.customenum.RoleEnum;
import xyz.dowob.filemanagement.data.response.ApiResponseDTO;
import xyz.dowob.filemanagement.data.user.dto.AuthRequestDTO;
import xyz.dowob.filemanagement.data.user.dto.RegisterDTO;
import xyz.dowob.filemanagement.data.user.dto.ResetPasswordDTO;
import xyz.dowob.filemanagement.data.user.dto.UserEmailDTO;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.service.serviceInterface.AuthorizationService;
import xyz.dowob.filemanagement.service.serviceInterface.UserService;
import xyz.dowob.filemanagement.service.serviceInterface.ValidationService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * WebGuestController 測試類別
 * 
 * <p>全面測試 Web 訪客控制器 {@link xyz.dowob.filemanagement.controller.web.WebGuestController} 的各種功能，
 * 涵蓋 Web 介面下的訪客認證和註冊操作。</p>
 * 
 * <p>測試範圍包括：
 * 
 * - Web 用戶註冊功能和表單驗證
 * - Web 用戶登入功能（包含 Cookie 設置）
 * - Web 密碼重置請求和確認流程
 * - Web 授權狀態檢查和頁面跳轉
 * - Web 表單處理和錯誤顯示
 * 
 * </p>
 * 
 * <p>測試前置條件：
 * 
 * - WebGuestController 類正常載入
 * - BaseGuestController 基礎功能可用
 * - 授權服務和驗證服務可正常模擬
 * - Spring WebFlux 環境配置正確
 * 
 * </p>
 * 
 * <p>測試策略：
 * 
 * - 測試 Web 表單提交和數據驗證
 * - 驗證 Cookie 設置和會話管理
 * - 模擬各種認證失敗場景
 * - 測試頁面跳轉和錯誤處理
 * 
 * </p>
 * 
 * <p>預期測試結果：
 * 
 * - Web 表單處理功能正確執行
 * - 認證和授權機制在 Web 環境下正常工作
 * - Cookie 和會話管理穩定可靠
 * - 錯誤處理和頁面跳轉機制完善
 * 
 * </p>
 * 
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("WebGuestController Web 訪客控制器測試")
class WebGuestControllerTest {

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private UserService userService;

    @Mock
    private SecurityProperties securityProperties;

    @Mock
    private ValidationService validationService;

    private WebGuestController webGuestController;

    private User testUser;
    private ServerWebExchange testExchange;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // 配置 ValidationService Mock 對象
        when(validationService.validateRegisterDTO(any(RegisterDTO.class))).thenReturn(Mono.empty());
        when(validationService.validateAuthRequestDTO(any(AuthRequestDTO.class))).thenReturn(Mono.empty());
        when(validationService.validateResetPasswordDTO(any(ResetPasswordDTO.class))).thenReturn(Mono.empty());
        when(validationService.validateNotNull(any())).thenReturn(Mono.empty());
        
        // 配置 UserService Mock 對象
        when(userService.register(any(RegisterDTO.class))).thenReturn(Mono.empty());
        when(userService.login(any(AuthRequestDTO.class), any(ServerWebExchange.class))).thenReturn(Mono.just("token123"));
        when(userService.sendResetPasswordMail(any(UserEmailDTO.class))).thenReturn(Mono.empty());
        when(userService.resetPassword(any(ResetPasswordDTO.class))).thenReturn(Mono.empty());
        when(userService.getUser(any(ServerWebExchange.class))).thenReturn(Mono.just(createTestUser()));
        
        // 配置 SecurityProperties Mock 對象
        SecurityProperties.Cookie cookieProps = new SecurityProperties.Cookie();
        cookieProps.setTokenName("jwt-token");
        cookieProps.setHttpOnly(true);
        cookieProps.setSecure(false);
        cookieProps.setSameSite("Lax");
        
        SecurityProperties.JwtToken jwtProps = new SecurityProperties.JwtToken();
        jwtProps.setExpiration(java.time.Duration.ofHours(24));
        
        when(securityProperties.getCookie()).thenReturn(cookieProps);
        when(securityProperties.getJwtToken()).thenReturn(jwtProps);
        
        // 手動創建 WebGuestController 實例
        webGuestController = new WebGuestController(
            authorizationService,
            userService,
            securityProperties,
            validationService
        );

        testUser = createTestUser();
        MockServerHttpRequest request = MockServerHttpRequest.get("/web/v1/guest").build();
        testExchange = MockServerWebExchange.from(request);
    }
    
    private User createTestUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setRole(RoleEnum.USER);
        return user;
    }

    // ==================== 一般測試 ====================

    @Test
    @DisplayName("一般測試 - 控制器初始化")
    void testControllerInitialization() {
        assertNotNull(webGuestController);
        assertTrue(webGuestController instanceof xyz.dowob.filemanagement.controller.base.BaseGuestController);
    }

    @Test
    @DisplayName("一般測試 - register Web 用戶註冊")
    void testRegister_basicRegistration() {
        RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setUsername("newuser");
        registerDTO.setEmail("newuser@example.com");
        registerDTO.setPassword("password123");

        StepVerifier.create(webGuestController.register(registerDTO, testExchange))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(201, responseEntity.getStatusCode().value());
                    
                    ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                    assertNotNull(response);
                    assertEquals("註冊成功", response.getMessage());
                })
                .verifyComplete();

        verify(validationService).validateRegisterDTO(any(RegisterDTO.class));
        verify(userService).register(any(RegisterDTO.class));
    }

    @Test
    @DisplayName("一般測試 - login Web 用戶登入（包含 Cookie）")
    void testLogin_webModeWithCookie() {
        AuthRequestDTO authRequestDTO = new AuthRequestDTO("testuser", "password123");

        StepVerifier.create(webGuestController.login(authRequestDTO, testExchange))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(200, responseEntity.getStatusCode().value());
                    
                    ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                    assertNotNull(response);
                    assertEquals("登入成功", response.getMessage());
                })
                .verifyComplete();

        verify(validationService).validateAuthRequestDTO(any(AuthRequestDTO.class));
        verify(userService).login(any(AuthRequestDTO.class), any(ServerWebExchange.class));
    }

    @Test
    @DisplayName("一般測試 - checkAuthenticationStatus Web 檢查授權狀態")
    void testCheckAuthenticationStatus_authenticated() {
        StepVerifier.create(webGuestController.checkAuthenticationStatus(testExchange))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(200, responseEntity.getStatusCode().value());
                    
                    ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                    assertNotNull(response);
                    assertEquals("用戶已授權", response.getMessage());
                })
                .verifyComplete();

        verify(userService).getUser(any(ServerWebExchange.class));
    }

    @Test
    @DisplayName("一般測試 - sendResetPasswordMail Web 發送重置密碼郵件")
    void testSendResetPasswordMail_basicFunctionality() {
        UserEmailDTO userEmailDTO = new UserEmailDTO();
        userEmailDTO.setEmail("test@example.com");

        StepVerifier.create(webGuestController.sendResetPasswordMail(userEmailDTO, testExchange))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(200, responseEntity.getStatusCode().value());
                    
                    ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                    assertNotNull(response);
                    assertEquals("重置密碼郵件已發送，請到信箱查收驗證信", response.getMessage());
                })
                .verifyComplete();

        verify(userService).sendResetPasswordMail(any(UserEmailDTO.class));
    }

    @Test
    @DisplayName("一般測試 - resetPassword Web 重置密碼")
    void testResetPassword_basicFunctionality() {
        ResetPasswordDTO resetPasswordDTO = new ResetPasswordDTO();
        resetPasswordDTO.setEmail("test@example.com");
        resetPasswordDTO.setVerificationCode("123456");
        resetPasswordDTO.setNewPassword("newpassword123");
        resetPasswordDTO.setConfirmPassword("newpassword123");

        StepVerifier.create(webGuestController.resetPassword(resetPasswordDTO, testExchange))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(200, responseEntity.getStatusCode().value());
                    
                    ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                    assertNotNull(response);
                    assertEquals("密碼重置成功", response.getMessage());
                })
                .verifyComplete();

        verify(userService).resetPassword(any(ResetPasswordDTO.class));
    }

    @Test
    @DisplayName("一般測試 - Web 路由映射驗證")
    void testWebRouteMappings() {
        assertTrue(webGuestController.getClass().isAnnotationPresent(org.springframework.web.bind.annotation.RestController.class));
        assertTrue(webGuestController.getClass().isAnnotationPresent(org.springframework.web.bind.annotation.RequestMapping.class));
        
        org.springframework.web.bind.annotation.RequestMapping requestMapping = 
            webGuestController.getClass().getAnnotation(org.springframework.web.bind.annotation.RequestMapping.class);
        assertEquals("/web/v1/guest", requestMapping.value()[0]);
    }

    // ==================== 異常測試 ====================

    @Test
    @DisplayName("異常測試 - register 註冊驗證失敗")
    void testRegister_validationFailure() {
        RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setUsername("");

        when(validationService.validateRegisterDTO(any(RegisterDTO.class)))
                .thenReturn(Mono.error(new ValidationException(ValidationException.ErrorCode.REQUEST_IS_INVALID, "username")));

        StepVerifier.create(webGuestController.register(registerDTO, testExchange))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertTrue(responseEntity.getStatusCode().is4xxClientError());
                    
                    ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                    assertNotNull(response);
                    assertTrue(response.getMessage().contains("處理失敗"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - login 登入失敗")
    void testLogin_authenticationFailure() {
        AuthRequestDTO authRequestDTO = new AuthRequestDTO("wronguser", "wrongpassword");

        when(userService.login(any(AuthRequestDTO.class), any(ServerWebExchange.class)))
                .thenReturn(Mono.error(new ValidationException(ValidationException.ErrorCode.WRONG_LOGIN_CREDENTIALS)));

        StepVerifier.create(webGuestController.login(authRequestDTO, testExchange))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertTrue(responseEntity.getStatusCode().is4xxClientError());
                    
                    ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                    assertNotNull(response);
                    assertTrue(response.getMessage().contains("處理失敗"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - checkAuthenticationStatus 用戶未認證")
    void testCheckAuthenticationStatus_notAuthenticated() {
        when(userService.getUser(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        StepVerifier.create(webGuestController.checkAuthenticationStatus(testExchange))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(400, responseEntity.getStatusCode().value());
                    
                    ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                    assertNotNull(response);
                    assertEquals("用戶未授權", response.getMessage());
                })
                .verifyComplete();
    }

    // ==================== 邊界測試 ====================

    @Test
    @DisplayName("邊界測試 - 註解驗證")
    void testAnnotations() {
        try {
            var loginMethod = webGuestController.getClass().getMethod("login", AuthRequestDTO.class, ServerWebExchange.class);
            assertTrue(loginMethod.isAnnotationPresent(xyz.dowob.filemanagement.annotation.HideSensitive.class));
            
            var checkAuthMethod = webGuestController.getClass().getMethod("checkAuthenticationStatus", ServerWebExchange.class);
            assertTrue(checkAuthMethod.isAnnotationPresent(xyz.dowob.filemanagement.annotation.SkipRecord.class));
        } catch (NoSuchMethodException e) {
            fail("Method not found: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("邊界測試 - 併發操作")
    void testConcurrentOperations() {
        RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setUsername("testuser");
        registerDTO.setEmail("test@example.com");
        registerDTO.setPassword("password123");

        reactor.core.publisher.Flux<ResponseEntity<?>> concurrentRegistrations = 
            reactor.core.publisher.Flux.range(1, 3)
                .flatMap(i -> webGuestController.register(registerDTO, testExchange));

        StepVerifier.create(concurrentRegistrations)
                .expectNextCount(3)
                .verifyComplete();

        verify(userService, times(3)).register(any(RegisterDTO.class));
    }
}