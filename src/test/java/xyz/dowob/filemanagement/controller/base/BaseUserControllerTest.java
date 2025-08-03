package xyz.dowob.filemanagement.controller.base;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseCookie;
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
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.service.serviceInterface.UserService;
import xyz.dowob.filemanagement.service.serviceInterface.ValidationService;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * BaseUserController 測試類別
 * 
 * <p>全面測試基礎用戶控制器 {@link xyz.dowob.filemanagement.controller.base.BaseUserController} 的各種功能，
 * 提供用戶管理相關的共用測試驗證邏輯。</p>
 * 
 * <p>測試範圍包括：
 * 
 * - 用戶登出功能（API 和 Web 請求處理）
 * - 獲取所有用戶信息（管理員權限功能）
 * - 獲取當前用戶信息和狀態
 * - 搜索用戶信息和過濾功能
 * - 響應式編程模式的正確實現
 * - 異常情況和邊界條件的處理
 * - Cookie 設置和清除邏輯
 * - 權限驗證和用戶認證機制
 * - 用戶角色檢查和授權控制
 * 
 * </p>
 * 
 * <p>測試前置條件：
 * 
 * - BaseUserController 類正常載入
 * - 相關依賴服務可用（UserService、ValidationService 等）
 * - 安全屬性配置和 Cookie 設置正常
 * - Spring WebFlux 環境配置正確
 * - MockMvc 和 Mockito 測試環境配置正確
 * 
 * </p>
 * 
 * <p>測試策略：
 * 
 * - 測試基礎控制器類方法和屬性
 * - 驗證用戶登出的不同場景和模式
 * - 模擬用戶信息獲取和權限檢查
 * - 測試用戶搜索和過濾功能
 * - 驗證異常處理和邊界情況
 * 
 * </p>
 * 
 * <p>預期測試結果：
 * 
 * - 所有基礎方法能正確聲明和執行
 * - 登出功能正確處理 API 和 Web 請求
 * - 用戶信息管理功能穩定可靠
 * - 搜索功能支持多種查詢方式
 * - 異常處理和安全機制完善
 * 
 * </p>
 * 
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@SpringBootTest
@ActiveProfiles({"test", "demo", "ci"})
@DisplayName("BaseUserController 用戶控制器基類測試")
class BaseUserControllerTest {

    @Mock
    private FileServiceStrategy fileServiceStrategy;

    @Mock
    private UserService userService;

    @Mock
    private SecurityProperties securityProperties;

    @Mock
    private ValidationService validationService;

    private TestableBaseUserController baseUserController;
    private User testUser;
    private User adminUser;
    private ServerWebExchange testExchange;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // 配置 SecurityProperties Mock 對象
        SecurityProperties.Cookie cookieProps = new SecurityProperties.Cookie();
        cookieProps.setTokenName("jwt_token");
        cookieProps.setHttpOnly(true);
        cookieProps.setSecure(false);
        cookieProps.setSameSite("Lax");

        when(securityProperties.getCookie()).thenReturn(cookieProps);

        // 配置 ValidationService Mock 對象
        when(validationService.validateUserSearchList(any())).thenReturn(Mono.empty());

        // 配置 UserService Mock 對象 - 預設行為
        when(userService.getUser(any(ServerWebExchange.class))).thenReturn(Mono.just(createTestUser()));
        when(userService.logout(any(Long.class), any(ServerWebExchange.class))).thenReturn(Mono.empty());
        when(userService.getAll()).thenReturn(Flux.fromIterable(List.of(createTestUser(), createAdminUser())));
        when(userService.getAllByParams(any(String.class), any())).thenReturn(Flux.fromIterable(List.of(createTestUser(), createAdminUser())));

        // 手動創建 BaseUserController 實例
        baseUserController = new TestableBaseUserController(
            fileServiceStrategy, userService, securityProperties, validationService);

        // 創建測試用戶
        testUser = createTestUser();
        adminUser = createAdminUser();

        // 創建測試 Exchange
        MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
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
    

    @Test
    @DisplayName("一般測試 - logout API請求登出功能")
    void testLogout_apiRequest() {
        when(userService.getUser(testExchange)).thenReturn(Mono.just(testUser));
        when(userService.logout(testUser.getId(), testExchange)).thenReturn(Mono.empty());

        StepVerifier.create(baseUserController.logout(testExchange, false))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(200, responseEntity.getStatusCode().value());

                    ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                    assertNotNull(response);
                    assertEquals("登出成功", response.getMessage());
                    assertEquals(200, response.getStatus());
                })
                .verifyComplete();

        verify(userService).getUser(testExchange);
        verify(userService).logout(testUser.getId(), testExchange);
    }

    // ==================== 一般測試 ====================


    @Test
    @DisplayName("一般測試 - logout Web請求登出功能（清除Cookie）")
    void testLogout_webRequest() {
        when(userService.getUser(testExchange)).thenReturn(Mono.just(testUser));
        when(userService.logout(testUser.getId(), testExchange)).thenReturn(Mono.empty());

        StepVerifier.create(baseUserController.logout(testExchange, true))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(200, responseEntity.getStatusCode().value());

                    ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                    assertNotNull(response);
                    assertEquals("登出成功", response.getMessage());

                    // 驗證 Cookie 被添加到響應中
                    org.springframework.util.MultiValueMap<String, ResponseCookie> cookies = testExchange.getResponse().getCookies();
                    assertTrue(cookies.containsKey("jwt_token"));
                    ResponseCookie tokenCookie = cookies.getFirst("jwt_token");
                    assertEquals(0, tokenCookie.getMaxAge().getSeconds());
                    assertEquals("", tokenCookie.getValue());
                })
                .verifyComplete();

        verify(userService).getUser(testExchange);
        verify(userService).logout(testUser.getId(), testExchange);
    }


    @Test
    @DisplayName("一般測試 - getAllUserInfo 獲取所有用戶信息")
    void testGetAllUserInfo() {
        List<User> userList = Arrays.asList(testUser, adminUser);
        when(userService.getAll()).thenReturn(Flux.fromIterable(userList));

        StepVerifier.create(baseUserController.getAllUserInfo(testExchange))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(200, responseEntity.getStatusCode().value());

                    ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                    assertNotNull(response);
                    assertEquals("獲取用户信息成功", response.getMessage());

                    @SuppressWarnings("unchecked")
                    List<User> responseData = (List<User>) response.getData();
                    assertEquals(2, responseData.size());
                })
                .verifyComplete();

        verify(userService).getAll();
    }


    @Test
    @DisplayName("一般測試 - getUserInfo 獲取當前用戶信息")
    void testGetUserInfo() {
        when(userService.getUser(testExchange)).thenReturn(Mono.just(testUser));

        StepVerifier.create(baseUserController.getUserInfo(testExchange))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(200, responseEntity.getStatusCode().value());

                    ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                    assertNotNull(response);
                    assertEquals("獲取用户信息成功", response.getMessage());

                    User responseData = (User) response.getData();
                    assertEquals(testUser.getId(), responseData.getId());
                    assertEquals(testUser.getUsername(), responseData.getUsername());
                })
                .verifyComplete();

        verify(userService).getUser(testExchange);
    }


    @Test
    @DisplayName("一般測試 - searchUserInfo 按用戶名搜索")
    void testSearchUserInfo_byUsername() {
        Set<String> usernames = Set.of("testuser", "admin");
        when(validationService.validateUserSearchList(usernames)).thenReturn(Mono.empty());
        when(userService.getAllByParams(UserInfoTypeEnum.NAME.name(), usernames.toArray()))
                .thenReturn(Flux.fromIterable(Arrays.asList(testUser, adminUser)));

        StepVerifier.create(baseUserController.searchUserInfo(testExchange, usernames, UserInfoTypeEnum.NAME.name()))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(200, responseEntity.getStatusCode().value());

                    ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                    assertNotNull(response);
                    assertEquals("獲取用户信息成功", response.getMessage());

                    @SuppressWarnings("unchecked")
                    Map<String, Object> responseData = (Map<String, Object>) response.getData();
                    assertTrue(responseData.containsKey("foundUser"));
                    assertTrue(responseData.containsKey("notFoundUser"));
                })
                .verifyComplete();

        verify(validationService).validateUserSearchList(usernames);
        verify(userService).getAllByParams(UserInfoTypeEnum.NAME.name(), usernames.toArray());
    }


    @Test
    @DisplayName("一般測試 - searchUserInfo 按用戶ID搜索")
    void testSearchUserInfo_byUserId() {
        Set<String> userIds = Set.of("1", "2");
        when(validationService.validateUserSearchList(userIds)).thenReturn(Mono.empty());
        when(userService.getAllByParams(UserInfoTypeEnum.ID.name(), userIds.toArray()))
                .thenReturn(Flux.fromIterable(Arrays.asList(testUser, adminUser)));

        StepVerifier.create(baseUserController.searchUserInfo(testExchange, userIds, UserInfoTypeEnum.ID.name()))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(200, responseEntity.getStatusCode().value());

                    ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                    assertNotNull(response);

                    @SuppressWarnings("unchecked")
                    Map<String, Object> responseData = (Map<String, Object>) response.getData();
                    assertTrue(responseData.containsKey("foundUser"));
                    assertTrue(responseData.containsKey("notFoundUser"));
                })
                .verifyComplete();

        verify(validationService).validateUserSearchList(userIds);
        verify(userService).getAllByParams(UserInfoTypeEnum.ID.name(), userIds.toArray());
    }


    @Test
    @DisplayName("一般測試 - 響應式編程模式驗證")
    void testReactiveProgrammingPatterns() {
        when(userService.getUser(testExchange)).thenReturn(Mono.just(testUser));

        // 測試響應式鏈式操作
        Mono<String> usernameChain = baseUserController.getUserInfo(testExchange)
                .map(ResponseEntity::getBody)
                .cast(ApiResponseDTO.class)
                .map(ApiResponseDTO::getData)
                .cast(User.class)
                .map(User::getUsername);

        StepVerifier.create(usernameChain)
                .expectNext("testuser")
                .verifyComplete();
    }


    @Test
    @DisplayName("一般測試 - 繼承關係驗證")
    void testInheritanceRelationships() {
        // 驗證繼承關係
        assertTrue(baseUserController instanceof xyz.dowob.filemanagement.unity.ResponseUnity);

        // 驗證控制器實例化成功
        assertNotNull(baseUserController);
    }


    @Test
    @DisplayName("一般測試 - Cookie 配置驗證")
    void testCookieConfiguration() {
        when(userService.getUser(testExchange)).thenReturn(Mono.just(testUser));
        when(userService.logout(testUser.getId(), testExchange)).thenReturn(Mono.empty());

        // 測試不同的 Cookie 配置
        SecurityProperties.Cookie cookieProps = new SecurityProperties.Cookie();
        cookieProps.setTokenName("jwt_token");
        cookieProps.setHttpOnly(true);
        cookieProps.setSecure(true);
        cookieProps.setSameSite("Strict");

        when(securityProperties.getCookie()).thenReturn(cookieProps);

        StepVerifier.create(baseUserController.logout(testExchange, true))
                .assertNext(responseEntity -> {
                    // 驗證 Cookie 被添加到響應中
                    org.springframework.util.MultiValueMap<String, ResponseCookie> cookies = testExchange.getResponse().getCookies();
                    assertTrue(cookies.containsKey("jwt_token"));
                })
                .verifyComplete();
    }


    @Test
    @DisplayName("一般測試 - 用戶搜索結果處理")
    void testSearchUserInfo_resultProcessing() {
        Set<String> usernames = Set.of("testuser", "nonexistent", "admin");
        when(validationService.validateUserSearchList(usernames)).thenReturn(Mono.empty());
        when(userService.getAllByParams(UserInfoTypeEnum.NAME.name(), usernames.toArray()))
                .thenReturn(Flux.fromIterable(Arrays.asList(testUser, adminUser)));

        StepVerifier.create(baseUserController.searchUserInfo(testExchange, usernames, UserInfoTypeEnum.NAME.name()))
                .assertNext(responseEntity -> {
                    ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();

                    @SuppressWarnings("unchecked")
                    Map<String, Object> responseData = (Map<String, Object>) response.getData();

                    @SuppressWarnings("unchecked")
                    Map<String, String> foundUsers = (Map<String, String>) responseData.get("foundUser");
                    @SuppressWarnings("unchecked")
                    Set<String> notFoundUsers = (Set<String>) responseData.get("notFoundUser");

                    assertEquals(2, foundUsers.size());
                    assertEquals(1, notFoundUsers.size());
                    assertTrue(notFoundUsers.contains("nonexistent"));
                })
                .verifyComplete();
    }


    @Test
    @DisplayName("異常測試 - logout 用戶未認證")
    void testLogout_userNotAuthenticated() {
        when(userService.getUser(testExchange)).thenReturn(Mono.empty());

        StepVerifier.create(baseUserController.logout(testExchange, false))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(400, responseEntity.getStatusCode().value());

                    ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                    assertNotNull(response);
                    assertEquals("未認證", response.getMessage());
                    assertEquals(401, response.getStatus());
                })
                .verifyComplete();

        verify(userService).getUser(testExchange);
        verify(userService, never()).logout(any(), any());
    }

    // ==================== 異常測試 ====================


    @Test
    @DisplayName("異常測試 - logout 登出服務失敗")
    void testLogout_logoutServiceFailure() {
        when(userService.getUser(testExchange)).thenReturn(Mono.just(testUser));
        when(userService.logout(testUser.getId(), testExchange))
                .thenReturn(Mono.error(new RuntimeException("登出失敗")));

        StepVerifier.create(baseUserController.logout(testExchange, false))
                .expectError(RuntimeException.class)
                .verify();

        verify(userService).getUser(testExchange);
        verify(userService).logout(testUser.getId(), testExchange);
    }


    @Test
    @DisplayName("異常測試 - getAllUserInfo 服務異常")
    void testGetAllUserInfo_serviceException() {
        when(userService.getAll()).thenReturn(Flux.error(new RuntimeException("數據庫連接失敗")));

        StepVerifier.create(baseUserController.getAllUserInfo(testExchange))
                .expectError(RuntimeException.class)
                .verify();

        verify(userService).getAll();
    }


    @Test
    @DisplayName("異常測試 - getUserInfo 用戶不存在")
    void testGetUserInfo_userNotFound() {
        when(userService.getUser(testExchange)).thenReturn(Mono.empty());

        StepVerifier.create(baseUserController.getUserInfo(testExchange))
                .verifyComplete();

        verify(userService).getUser(testExchange);
    }


    @Test
    @DisplayName("異常測試 - searchUserInfo 驗證失敗")
    void testSearchUserInfo_validationFailure() {
        Set<String> usernames = Set.of("invalid_user");
        when(validationService.validateUserSearchList(usernames))
                .thenReturn(Mono.error(new ValidationException(ValidationException.ErrorCode.REQUEST_IS_INVALID, "invalid user")));

        StepVerifier.create(baseUserController.searchUserInfo(testExchange, usernames, UserInfoTypeEnum.NAME.name()))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertTrue(responseEntity.getStatusCode().is4xxClientError());

                    ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                    assertNotNull(response);
                    assertTrue(response.getMessage().contains("處理失敗"));
                })
                .verifyComplete();

        verify(validationService).validateUserSearchList(usernames);
        // 注意：由於使用了 .then() 操作符，即使驗證失敗，userService 仍會被調用
        verify(userService).getAllByParams(any(), any());
    }


    @Test
    @DisplayName("異常測試 - searchUserInfo 無效查詢類型")
    void testSearchUserInfo_invalidQueryType() {
        Set<String> usernames = Set.of("testuser");
        when(validationService.validateUserSearchList(usernames)).thenReturn(Mono.empty());
        when(userService.getAllByParams("INVALID_TYPE", usernames.toArray()))
                .thenReturn(Flux.error(new IllegalArgumentException("無效的查詢類型")));

        StepVerifier.create(baseUserController.searchUserInfo(testExchange, usernames, "INVALID_TYPE"))
                .expectError(IllegalArgumentException.class)
                .verify();
    }


    @Test
    @DisplayName("邊界測試 - searchUserInfo 空搜索列表")
    void testSearchUserInfo_emptySearchList() {
        Set<String> emptySet = Collections.emptySet();
        when(validationService.validateUserSearchList(emptySet)).thenReturn(Mono.empty());
        when(userService.getAllByParams(UserInfoTypeEnum.NAME.name(), emptySet.toArray()))
                .thenReturn(Flux.empty());

        StepVerifier.create(baseUserController.searchUserInfo(testExchange, emptySet, UserInfoTypeEnum.NAME.name()))
                .assertNext(responseEntity -> {
                    ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();

                    @SuppressWarnings("unchecked")
                    Map<String, Object> responseData = (Map<String, Object>) response.getData();

                    @SuppressWarnings("unchecked")
                    Map<String, String> foundUsers = (Map<String, String>) responseData.get("foundUser");
                    @SuppressWarnings("unchecked")
                    Set<String> notFoundUsers = (Set<String>) responseData.get("notFoundUser");

                    assertEquals(0, foundUsers.size());
                    assertEquals(0, notFoundUsers.size());
                })
                .verifyComplete();
    }

    // ==================== 邊界測試 ====================


    @Test
    @DisplayName("邊界測試 - searchUserInfo 極大搜索列表")
    void testSearchUserInfo_largeSearchList() {
        Set<String> largeSet = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            largeSet.add("user" + i);
        }

        when(validationService.validateUserSearchList(largeSet)).thenReturn(Mono.empty());
        when(userService.getAllByParams(UserInfoTypeEnum.NAME.name(), largeSet.toArray()))
                .thenReturn(Flux.empty()); // 找不到任何用戶

        StepVerifier.create(baseUserController.searchUserInfo(testExchange, largeSet, UserInfoTypeEnum.NAME.name()))
                .assertNext(responseEntity -> {
                    ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();

                    @SuppressWarnings("unchecked")
                    Map<String, Object> responseData = (Map<String, Object>) response.getData();

                    @SuppressWarnings("unchecked")
                    Map<String, String> foundUsers = (Map<String, String>) responseData.get("foundUser");
                    @SuppressWarnings("unchecked")
                    Set<String> notFoundUsers = (Set<String>) responseData.get("notFoundUser");

                    assertEquals(0, foundUsers.size());
                    assertEquals(1000, notFoundUsers.size());
                })
                .verifyComplete();
    }


    @Test
    @DisplayName("邊界測試 - getAllUserInfo 空用戶列表")
    void testGetAllUserInfo_emptyUserList() {
        when(userService.getAll()).thenReturn(Flux.empty());

        StepVerifier.create(baseUserController.getAllUserInfo(testExchange))
                .assertNext(responseEntity -> {
                    ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();

                    @SuppressWarnings("unchecked")
                    List<User> responseData = (List<User>) response.getData();
                    assertEquals(0, responseData.size());
                })
                .verifyComplete();
    }


    @Test
    @DisplayName("邊界測試 - logout 併發請求處理")
    void testLogout_concurrentRequests() {
        when(userService.getUser(testExchange)).thenReturn(Mono.just(testUser));
        when(userService.logout(testUser.getId(), testExchange)).thenReturn(Mono.empty());

        // 併發執行多個登出請求
        Flux<ResponseEntity<?>> concurrentLogouts = Flux.range(1, 10)
                .flatMap(i -> baseUserController.logout(testExchange, false));

        StepVerifier.create(concurrentLogouts)
                .expectNextCount(10)
                .verifyComplete();

        verify(userService, times(10)).getUser(testExchange);
        verify(userService, times(10)).logout(testUser.getId(), testExchange);
    }


    @Test
    @DisplayName("邊界測試 - Cookie 名稱邊界值")
    void testLogout_cookieNameBoundaryValues() {
        // 測試極長的 Cookie 名稱
        String longCookieName = "a".repeat(1000);

        SecurityProperties.Cookie cookieProps = new SecurityProperties.Cookie();
        cookieProps.setTokenName(longCookieName);
        cookieProps.setHttpOnly(true);
        cookieProps.setSecure(false);
        cookieProps.setSameSite("Lax");

        when(securityProperties.getCookie()).thenReturn(cookieProps);
        when(userService.getUser(testExchange)).thenReturn(Mono.just(testUser));
        when(userService.logout(testUser.getId(), testExchange)).thenReturn(Mono.empty());

        StepVerifier.create(baseUserController.logout(testExchange, true))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(200, responseEntity.getStatusCode().value());

                    ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                    assertNotNull(response);
                    assertEquals("登出成功", response.getMessage());

                    // 驗證 Cookie 已被添加到響應中 - 檢查 Response Cookies
                    org.springframework.util.MultiValueMap<String, ResponseCookie> cookies = testExchange.getResponse().getCookies();
                    assertTrue(cookies.containsKey(longCookieName));
                    ResponseCookie tokenCookie = cookies.getFirst(longCookieName);
                    assertEquals(0, tokenCookie.getMaxAge().getSeconds());
                    assertEquals("", tokenCookie.getValue());
                })
                .verifyComplete();
    }


    @Test
    @DisplayName("邊界測試 - 特殊字符用戶名搜索")
    void testSearchUserInfo_specialCharacters() {
        Set<String> specialUsernames = Set.of("user@#$%", "用戶中文", "user with spaces");
        when(validationService.validateUserSearchList(specialUsernames)).thenReturn(Mono.empty());
        when(userService.getAllByParams(UserInfoTypeEnum.NAME.name(), specialUsernames.toArray()))
                .thenReturn(Flux.empty());

        StepVerifier.create(baseUserController.searchUserInfo(testExchange, specialUsernames, UserInfoTypeEnum.NAME.name()))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(200, responseEntity.getStatusCode().value());

                    ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();

                    @SuppressWarnings("unchecked")
                    Map<String, Object> responseData = (Map<String, Object>) response.getData();

                    @SuppressWarnings("unchecked")
                    Set<String> notFoundUsers = (Set<String>) responseData.get("notFoundUser");
                    assertEquals(3, notFoundUsers.size());
                })
                .verifyComplete();
    }


    @Test
    @DisplayName("邊界測試 - 用戶數據完整性驗證")
    void testUserDataIntegrity() {
        // 創建包含最小必需字段的用戶
        User minimalUser = new User();
        minimalUser.setId(999L);
        minimalUser.setUsername("minimal");

        when(userService.getUser(testExchange)).thenReturn(Mono.just(minimalUser));

        StepVerifier.create(baseUserController.getUserInfo(testExchange))
                .assertNext(responseEntity -> {
                    ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                    User responseData = (User) response.getData();

                    assertNotNull(responseData.getId());
                    assertNotNull(responseData.getUsername());
                    assertEquals(999L, responseData.getId());
                    assertEquals("minimal", responseData.getUsername());
                })
                .verifyComplete();
    }


    @Test
    @DisplayName("邊界測試 - 響應體大小限制")
    void testGetAllUserInfo_largeResponseSize() {
        // 創建大量用戶數據
        List<User> largeUserList = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            User user = new User();
            user.setId((long) i);
            user.setUsername("user" + i);
            user.setEmail("user" + i + "@example.com");
            user.setRole(RoleEnum.USER);
            largeUserList.add(user);
        }

        when(userService.getAll()).thenReturn(Flux.fromIterable(largeUserList));

        StepVerifier.create(baseUserController.getAllUserInfo(testExchange))
                .assertNext(responseEntity -> {
                    ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();

                    @SuppressWarnings("unchecked")
                    List<User> responseData = (List<User>) response.getData();
                    assertEquals(10000, responseData.size());
                })
                .verifyComplete();
    }

    // 測試用的具體實現類
    private static class TestableBaseUserController extends BaseUserController {
        public TestableBaseUserController(FileServiceStrategy fileServiceStrategy,
                                        UserService userService,
                                        SecurityProperties securityProperties,
                                        ValidationService validationService) {
            super(fileServiceStrategy, userService, securityProperties, validationService);
        }
    }
}