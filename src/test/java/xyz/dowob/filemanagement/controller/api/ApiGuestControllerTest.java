package xyz.dowob.filemanagement.controller.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.web.server.csrf.CsrfToken;
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

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ApiGuestController 測試類別
 * 
 * <p>全面測試 API 訪客控制器 {@link xyz.dowob.filemanagement.controller.api.ApiGuestController} 的各種功能，
 * 涵蓋訪客權限下的所有認證和授權相關操作。</p>
 * 
 * <p>測試範圍包括：
 * 
 * - API 用戶註冊功能驗證
 * - API 用戶登入認證處理
 * - API 密碼重置請求和確認
 * - API 授權狀態檢查機制
 * - API CSRF Token 獲取和驗證
 * - 參數驗證和異常處理
 * - 安全性檢查和錯誤響應
 * 
 * </p>
 * 
 * <p>測試前置條件：
 * 
 * - ApiGuestController 類正常載入
 * - BaseGuestController 基礎功能可用
 * - Spring Security 和 WebFlux 環境配置正確
 * - MockMvc 和 Mockito 測試環境配置正確
 * 
 * </p>
 * 
 * <p>測試策略：
 * 
 * - 測試各種認證和授權流程
 * - 驗證安全性措施和權限控制
 * - 模擬各種異常情況和邊界條件
 * - 確保 API 響應格式和狀態碼正確
 * 
 * </p>
 * 
 * <p>預期測試結果：
 * 
 * - 所有訪客 API 端點正確響應
 * - 認證和授權機制按預期工作
 * - 安全驗證和錯誤處理機制完善
 * - CSRF 防護機制正常運作
 * 
 * </p>
 * 
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("ApiGuestController API 訪客控制器測試")
class ApiGuestControllerTest {

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private UserService userService;

    @Mock
    private SecurityProperties securityProperties;

    @Mock
    private ValidationService validationService;

    @Mock
    private CsrfToken csrfToken;

    private ApiGuestController apiGuestController;
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
        
        // 配置 AuthorizationService Mock 對象
        when(csrfToken.getToken()).thenReturn("csrf-token-123");
        when(csrfToken.getHeaderName()).thenReturn("X-CSRF-TOKEN");
        when(csrfToken.getParameterName()).thenReturn("_csrf");
        when(authorizationService.getCSRFToken(any(ServerWebExchange.class))).thenReturn(Mono.just(csrfToken));

        // 手動創建 ApiGuestController 實例
        apiGuestController = new ApiGuestController(
            authorizationService, userService, securityProperties, validationService);

        testUser = createTestUser();

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/guest").build();
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
        assertNotNull(apiGuestController);
        assertTrue(apiGuestController instanceof xyz.dowob.filemanagement.controller.base.BaseGuestController);
    }

    @Test
    @DisplayName("一般測試 - register API 用戶註冊")
    void testRegister_basicRegistration() {
        RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setUsername("newuser");
        registerDTO.setEmail("newuser@example.com");
        registerDTO.setPassword("password123");

        when(validationService.validateRegisterDTO(registerDTO)).thenReturn(Mono.empty());
        when(userService.register(registerDTO)).thenReturn(Mono.empty());

        StepVerifier.create(apiGuestController.register(registerDTO, testExchange))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(201, responseEntity.getStatusCode().value());
                    
                    ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                    assertNotNull(response);
                    assertEquals("註冊成功", response.getMessage());
                })
                .verifyComplete();

        verify(validationService).validateRegisterDTO(registerDTO);
        verify(userService).register(registerDTO);
    }

    @Test
    @DisplayName("一般測試 - login API 用戶登入")
    void testLogin_basicLogin() {
        AuthRequestDTO authRequestDTO = new AuthRequestDTO();
        authRequestDTO.setUsername("testuser");
        authRequestDTO.setPassword("password123");

        String expectedToken = "token123";

        when(validationService.validateAuthRequestDTO(authRequestDTO)).thenReturn(Mono.empty());
        when(userService.login(authRequestDTO, testExchange)).thenReturn(Mono.just(expectedToken));

        StepVerifier.create(apiGuestController.login(authRequestDTO, testExchange))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(200, responseEntity.getStatusCode().value());
                    
                    ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                    assertNotNull(response);
                    assertEquals("登入成功", response.getMessage());
                })
                .verifyComplete();

        verify(validationService).validateAuthRequestDTO(authRequestDTO);
        verify(userService).login(authRequestDTO, testExchange);
    }

    @Test
    @DisplayName("一般測試 - checkAuthenticationStatus API 檢查授權狀態")
    void testCheckAuthenticationStatus_authenticated() {
        when(userService.getUser(testExchange)).thenReturn(Mono.just(testUser));

        StepVerifier.create(apiGuestController.checkAuthenticationStatus(testExchange))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(200, responseEntity.getStatusCode().value());
                    
                    ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                    assertNotNull(response);
                    assertEquals("用戶已授權", response.getMessage());
                })
                .verifyComplete();

        verify(userService).getUser(testExchange);
    }

    @Test
    @DisplayName("一般測試 - sendResetPasswordMail API 發送重置密碼郵件")
    void testSendResetPasswordMail_basicFunctionality() {
        UserEmailDTO userEmailDTO = new UserEmailDTO();
        userEmailDTO.setEmail("test@example.com");

        when(validationService.validateNotNull(userEmailDTO)).thenReturn(Mono.empty());
        when(userService.sendResetPasswordMail(userEmailDTO)).thenReturn(Mono.empty());

        StepVerifier.create(apiGuestController.sendResetPasswordMail(userEmailDTO, testExchange))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(200, responseEntity.getStatusCode().value());
                    
                    ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                    assertNotNull(response);
                    assertEquals("重置密碼郵件已發送，請到信箱查收驗證信", response.getMessage());
                })
                .verifyComplete();

        verify(userService).sendResetPasswordMail(userEmailDTO);
    }

    @Test
    @DisplayName("一般測試 - resetPassword API 重置密碼")
    void testResetPassword_basicFunctionality() {
        ResetPasswordDTO resetPasswordDTO = new ResetPasswordDTO();
        resetPasswordDTO.setEmail("test@example.com");
        resetPasswordDTO.setVerificationCode("123456");
        resetPasswordDTO.setNewPassword("newpassword123");
        resetPasswordDTO.setConfirmPassword("newpassword123");

        when(validationService.validateResetPasswordDTO(resetPasswordDTO)).thenReturn(Mono.empty());
        when(userService.resetPassword(resetPasswordDTO)).thenReturn(Mono.empty());

        StepVerifier.create(apiGuestController.resetPassword(resetPasswordDTO, testExchange))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(200, responseEntity.getStatusCode().value());
                    
                    ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                    assertNotNull(response);
                    assertEquals("密碼重置成功", response.getMessage());
                })
                .verifyComplete();

        verify(userService).resetPassword(resetPasswordDTO);
    }

    @Test
    @DisplayName("一般測試 - getCSRFToken API 獲取 CSRF Token")
    void testGetCSRFToken_basicFunctionality() {
        when(csrfToken.getToken()).thenReturn("csrf-token-123");
        when(csrfToken.getHeaderName()).thenReturn("X-CSRF-TOKEN");
        when(csrfToken.getParameterName()).thenReturn("_csrf");
        when(authorizationService.getCSRFToken(testExchange)).thenReturn(Mono.just(csrfToken));

        StepVerifier.create(apiGuestController.getCSRFToken(testExchange))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(200, responseEntity.getStatusCode().value());
                    
                    ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                    assertNotNull(response);
                    assertEquals("獲取 CSRF Token 成功", response.getMessage());
                    
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = (Map<String, Object>) response.getData();
                    assertEquals("csrf-token-123", data.get("token"));
                    assertEquals("X-CSRF-TOKEN", data.get("headerName"));
                    assertEquals("_csrf", data.get("parameterName"));
                })
                .verifyComplete();

        verify(authorizationService).getCSRFToken(testExchange);
    }

    // ==================== 異常測試 ====================

    @Test
    @DisplayName("異常測試 - register 註冊驗證失敗")
    void testRegister_validationFailure() {
        RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setUsername(""); // 無效用戶名

        when(validationService.validateRegisterDTO(registerDTO))
                .thenReturn(Mono.error(new ValidationException(ValidationException.ErrorCode.REQUEST_IS_INVALID, "username")));

        StepVerifier.create(apiGuestController.register(registerDTO, testExchange))
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
        AuthRequestDTO authRequestDTO = new AuthRequestDTO();
        authRequestDTO.setUsername("wronguser");
        authRequestDTO.setPassword("wrongpassword");

        when(validationService.validateAuthRequestDTO(authRequestDTO)).thenReturn(Mono.empty());
        when(userService.login(authRequestDTO, testExchange))
                .thenReturn(Mono.error(new ValidationException(ValidationException.ErrorCode.WRONG_LOGIN_CREDENTIALS)));

        StepVerifier.create(apiGuestController.login(authRequestDTO, testExchange))
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
        when(userService.getUser(testExchange)).thenReturn(Mono.empty());

        StepVerifier.create(apiGuestController.checkAuthenticationStatus(testExchange))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(400, responseEntity.getStatusCode().value());
                    
                    ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                    assertNotNull(response);
                    assertEquals("用戶未授權", response.getMessage());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - sendResetPasswordMail 郵箱不存在")
    void testSendResetPasswordMail_emailNotFound() {
        UserEmailDTO userEmailDTO = new UserEmailDTO();
        userEmailDTO.setEmail("nonexistent@example.com");

        when(validationService.validateNotNull(userEmailDTO)).thenReturn(Mono.empty());
        when(userService.sendResetPasswordMail(userEmailDTO))
                .thenReturn(Mono.error(new ValidationException(ValidationException.ErrorCode.NOT_EXISTING_USER_FILE, "email")));

        StepVerifier.create(apiGuestController.sendResetPasswordMail(userEmailDTO, testExchange))
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
    @DisplayName("異常測試 - resetPassword 重置令牌無效")
    void testResetPassword_invalidToken() {
        ResetPasswordDTO resetPasswordDTO = new ResetPasswordDTO();
        resetPasswordDTO.setEmail("test@example.com");
        resetPasswordDTO.setVerificationCode("invalid-code");
        resetPasswordDTO.setNewPassword("newpassword123");
        resetPasswordDTO.setConfirmPassword("newpassword123");

        when(validationService.validateResetPasswordDTO(resetPasswordDTO)).thenReturn(Mono.empty());
        when(userService.resetPassword(resetPasswordDTO))
                .thenReturn(Mono.error(new ValidationException(ValidationException.ErrorCode.JWT_TOKEN_INVALID)));

        StepVerifier.create(apiGuestController.resetPassword(resetPasswordDTO, testExchange))
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
    @DisplayName("異常測試 - getCSRFToken 獲取失敗")
    void testGetCSRFToken_retrievalFailure() {
        when(authorizationService.getCSRFToken(testExchange))
                .thenReturn(Mono.error(new RuntimeException("CSRF token generation failed")));

        StepVerifier.create(apiGuestController.getCSRFToken(testExchange))
                .expectError(RuntimeException.class)
                .verify();
    }

    // ==================== 邊界測試 ====================

    @Test
    @DisplayName("邊界測試 - API 路由映射驗證")
    void testApiRouteMappings() {
        assertTrue(apiGuestController.getClass().isAnnotationPresent(org.springframework.web.bind.annotation.RestController.class));
        assertTrue(apiGuestController.getClass().isAnnotationPresent(org.springframework.web.bind.annotation.RequestMapping.class));
        
        org.springframework.web.bind.annotation.RequestMapping requestMapping = 
            apiGuestController.getClass().getAnnotation(org.springframework.web.bind.annotation.RequestMapping.class);
        assertEquals("/api/v1/guest", requestMapping.value()[0]);
    }

    @Test
    @DisplayName("邊界測試 - 註解驗證")
    void testAnnotations() {
        // 驗證敏感方法有正確的註解
        try {
            var loginMethod = apiGuestController.getClass().getMethod("login", AuthRequestDTO.class, ServerWebExchange.class);
            assertTrue(loginMethod.isAnnotationPresent(xyz.dowob.filemanagement.annotation.HideSensitive.class));
            
            var csrfMethod = apiGuestController.getClass().getMethod("getCSRFToken", ServerWebExchange.class);
            assertTrue(csrfMethod.isAnnotationPresent(xyz.dowob.filemanagement.annotation.HideSensitive.class));
            
            var checkAuthMethod = apiGuestController.getClass().getMethod("checkAuthenticationStatus", ServerWebExchange.class);
            assertTrue(checkAuthMethod.isAnnotationPresent(xyz.dowob.filemanagement.annotation.SkipRecord.class));
        } catch (NoSuchMethodException e) {
            fail("Method not found: " + e.getMessage());
        }
    }
}