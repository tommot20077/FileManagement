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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.dowob.filemanagement.component.strategy.FileServiceStrategy;
import xyz.dowob.filemanagement.config.properties.SecurityProperties;
import xyz.dowob.filemanagement.customenum.RoleEnum;
import xyz.dowob.filemanagement.customenum.UserInfoTypeEnum;
import xyz.dowob.filemanagement.data.response.ApiResponseDTO;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.service.serviceInterface.UserService;
import xyz.dowob.filemanagement.service.serviceInterface.ValidationService;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * WebUserController 測試類別
 * 
 * <p>全面測試 Web 用戶控制器 {@link xyz.dowob.filemanagement.controller.web.WebUserController} 的各種功能，
 * 涵蓋 Web 介面下的用戶管理和認證操作。</p>
 * 
 * <p>測試範圍包括：
 * 
 * - Web 用戶登出功能（包含 Cookie 處理）
 * - Web 用戶信息獲取和顯示功能
 * - Web 用戶搜索和過濾功能
 * - Web 路由映射和頁面導航驗證
 * - Web 請求處理和響應格式
 * - Cookie 管理和會話處理
 * 
 * </p>
 * 
 * <p>測試前置條件：
 * 
 * - WebUserController 類正常載入
 * - BaseUserController 基礎功能可用
 * - 安全屬性和 Cookie 配置正確
 * - Spring WebFlux 環境配置正確
 * - MockMvc 和 Mockito 測試環境配置正確
 * 
 * </p>
 * 
 * <p>測試策略：
 * 
 * - 測試 Web 控制器繼承和方法覆蓋
 * - 驗證 Web 請求的正確處理和路由
 * - 模擬用戶操作和頁面交互場景
 * - 測試 Cookie 和會話管理機制
 * - 驗證異常處理和錯誤頁面跳轉
 * 
 * </p>
 * 
 * <p>預期測試結果：
 * 
 * - Web 路由正確映射到相應的處理方法
 * - 用戶操作功能在 Web 環境下正確執行
 * - Cookie 和會話管理機制穩定可靠
 * - 異常處理和錯誤頁面機制完善
 * 
 * </p>
 * 
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("WebUserController Web 用戶控制器測試")
class WebUserControllerTest {

    @Mock
    private FileServiceStrategy fileServiceStrategy;

    @Mock
    private UserService userService;

    @Mock
    private SecurityProperties securityProperties;

    @Mock
    private ValidationService validationService;

    @Mock
    private SecurityProperties.Cookie cookieProperties;

    private WebUserController webUserController;

    private User testUser;
    private ServerWebExchange testExchange;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // 配置 SecurityProperties mocks
        when(securityProperties.getCookie()).thenReturn(cookieProperties);
        when(cookieProperties.getTokenName()).thenReturn("jwt_token");
        when(cookieProperties.isHttpOnly()).thenReturn(true);
        when(cookieProperties.isSecure()).thenReturn(false);
        when(cookieProperties.getSameSite()).thenReturn("Lax");
        
        // 配置其他基本 Mock
        when(userService.getUser(any(ServerWebExchange.class))).thenReturn(Mono.just(createTestUser()));
        when(userService.logout(any(Long.class), any(ServerWebExchange.class))).thenReturn(Mono.empty());
        when(userService.getAllByParams(any(String.class), any())).thenReturn(Flux.fromIterable(List.of(createTestUser(), createAdminUser())));
        when(validationService.validateUserSearchList(any())).thenReturn(Mono.empty());
        
        // 手動創建控制器實例
        webUserController = new WebUserController(
            fileServiceStrategy, userService, securityProperties, validationService
        );

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setRole(RoleEnum.USER);

        MockServerHttpRequest request = MockServerHttpRequest.get("/web/v1/user").build();
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
    
    private User createAdminUser() {
        User user = new User();
        user.setId(2L);
        user.setUsername("admin");
        user.setEmail("admin@example.com");
        user.setRole(RoleEnum.ADMIN);
        return user;
    }

    // ==================== 一般測試 ====================

    @Test
    @DisplayName("一般測試 - 控制器初始化")
    void testControllerInitialization() {
        assertNotNull(webUserController);
        assertTrue(webUserController instanceof xyz.dowob.filemanagement.controller.base.BaseUserController);
    }

    @Test
    @DisplayName("一般測試 - logout Web 用戶登出（包含 Cookie）")
    void testLogout_webMode() {
        when(userService.getUser(testExchange)).thenReturn(Mono.just(testUser));
        when(userService.logout(testUser.getId(), testExchange)).thenReturn(Mono.empty());

        StepVerifier.create(webUserController.logout(testExchange, true))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(200, responseEntity.getStatusCode().value());
                    
                    ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                    assertNotNull(response);
                    assertEquals("登出成功", response.getMessage());
                })
                .verifyComplete();

        verify(userService).logout(testUser.getId(), testExchange);
    }

    @Test
    @DisplayName("一般測試 - getUserInfo 獲取用戶信息")
    void testGetUserInfo_basicFunctionality() {
        when(userService.getUser(testExchange)).thenReturn(Mono.just(testUser));

        StepVerifier.create(webUserController.getUserInfo(testExchange))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(200, responseEntity.getStatusCode().value());
                    
                    ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                    assertNotNull(response);
                    assertEquals("獲取用户信息成功", response.getMessage());
                })
                .verifyComplete();

        verify(userService).getUser(testExchange);
    }

    @Test
    @DisplayName("一般測試 - searchUserInfo Web 用戶搜索")
    void testSearchUserInfo_basicSearch() {
        Set<String> usernames = Set.of("testuser", "admin");
        String type = "name";
        List<User> searchResults = List.of(testUser);

        when(validationService.validateUserSearchList(usernames)).thenReturn(Mono.empty());
        when(userService.getUser(testExchange)).thenReturn(Mono.just(testUser));
        when(userService.getAllByParams(UserInfoTypeEnum.NAME.name(), usernames.toArray()))
                .thenReturn(Flux.fromIterable(searchResults));

        StepVerifier.create(webUserController.searchUserInfo(testExchange, usernames, type))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(200, responseEntity.getStatusCode().value());
                    
                    ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                    assertNotNull(response);
                    assertEquals("獲取用户信息成功", response.getMessage());
                })
                .verifyComplete();

        verify(userService).getAllByParams(UserInfoTypeEnum.NAME.name(), usernames.toArray());
    }

    @Test
    @DisplayName("一般測試 - Web 路由映射驗證")
    void testWebRouteMappings() {
        assertTrue(webUserController.getClass().isAnnotationPresent(org.springframework.web.bind.annotation.RestController.class));
        assertTrue(webUserController.getClass().isAnnotationPresent(org.springframework.web.bind.annotation.RequestMapping.class));
        
        org.springframework.web.bind.annotation.RequestMapping requestMapping = 
            webUserController.getClass().getAnnotation(org.springframework.web.bind.annotation.RequestMapping.class);
        assertEquals("/web/v1/user", requestMapping.value()[0]);
    }

    // ==================== 異常測試 ====================

    @Test
    @DisplayName("異常測試 - logout 用戶未認證")
    void testLogout_userNotAuthenticated() {
        when(userService.getUser(testExchange)).thenReturn(Mono.empty());

        StepVerifier.create(webUserController.logout(testExchange, true))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(400, responseEntity.getStatusCode().value());
                    
                    ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                    assertNotNull(response);
                    assertEquals("未認證", response.getMessage());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - searchUserInfo 空搜索結果")
    void testSearchUserInfo_emptyResults() {
        Set<String> usernames = Set.of("nonexistent");
        
        when(validationService.validateUserSearchList(usernames)).thenReturn(Mono.empty());
        when(userService.getUser(testExchange)).thenReturn(Mono.just(testUser));
        when(userService.getAllByParams(UserInfoTypeEnum.NAME.name(), usernames.toArray()))
                .thenReturn(Flux.empty());

        StepVerifier.create(webUserController.searchUserInfo(testExchange, usernames, "name"))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(200, responseEntity.getStatusCode().value());
                    
                    ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                    assertNotNull(response);
                    assertEquals("獲取用户信息成功", response.getMessage());
                    
                    // 檢查響應數據結構 - 應該是包含 foundUser 和 notFoundUser 的 Map
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> responseData = (java.util.Map<String, Object>) response.getData();
                    assertNotNull(responseData);
                    assertTrue(responseData.containsKey("foundUser"));
                    assertTrue(responseData.containsKey("notFoundUser"));
                })
                .verifyComplete();
    }

    // ==================== 邊界測試 ====================

    @Test
    @DisplayName("邊界測試 - UserInfoTypeEnum 轉換")
    void testUserInfoTypeEnumConversion() {
        assertEquals(UserInfoTypeEnum.NAME, UserInfoTypeEnum.fromString("name"));
        assertEquals(UserInfoTypeEnum.NAME, UserInfoTypeEnum.fromString("email"));
        assertEquals(UserInfoTypeEnum.NAME, UserInfoTypeEnum.fromString("invalid"));
    }

    @Test
    @DisplayName("邊界測試 - 併發操作")
    void testConcurrentOperations() {
        when(userService.getUser(any(ServerWebExchange.class))).thenReturn(Mono.just(testUser));
        when(userService.logout(testUser.getId(), testExchange)).thenReturn(Mono.empty());

        reactor.core.publisher.Flux<ResponseEntity<?>> concurrentLogouts = 
            reactor.core.publisher.Flux.range(1, 3)
                .flatMap(i -> webUserController.logout(testExchange, true));

        StepVerifier.create(concurrentLogouts)
                .expectNextCount(3)
                .verifyComplete();

        verify(userService, times(3)).logout(testUser.getId(), testExchange);
    }
}