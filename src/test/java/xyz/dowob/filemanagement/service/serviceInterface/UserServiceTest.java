package xyz.dowob.filemanagement.service.serviceInterface;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.dowob.filemanagement.data.user.dto.AuthRequestDTO;
import xyz.dowob.filemanagement.data.user.dto.RegisterDTO;
import xyz.dowob.filemanagement.data.user.dto.ResetPasswordDTO;
import xyz.dowob.filemanagement.data.user.dto.UserEmailDTO;
import xyz.dowob.filemanagement.entity.User;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;

/**
 * UserService 用戶服務接口測試
 *
 * <p>測試 UserService 用戶服務接口的組合契約和用戶業務模式，驗證接口在用戶生命週期管理方面的設計正確性。
 * 
 * <p>測試涵蓋的接口功能：
 * <p>- 繼承 CrudService 的 User 實體 CRUD 操作
 * <p>- register 用戶註冊方法的身份驗證和安全檢查
 * <p>- login 用戶登入方法的認證流程和會話管理
 * <p>- logout 用戶登出方法的會話清理和狀態更新
 * <p>- changePassword 密碼修改方法的安全檢查
 * <p>- changeEmail 郵件修改方法的驗證流程
 * <p>- sendResetPasswordMail 密碼重置郵件發送方法
 * <p>- resetPassword 密碼重置方法的憑證驗證
 * <p>- getUser 用戶獲取方法的權限控制
 *
 * 測試摘要：
 * 
 * 驗證 UserService 接口作為用戶管理服務層的設計正確性，確保其能夠為用戶生命週期操作提供完整的業務能力。
 *
 * 前置條件：
 * - UserService 接口及其父接口可用
 * - 用戶相關 DTO、User 實體類可用
 * - Spring WebFlux ServerWebExchange 可用
 * - Reactor WebFlux 響應式編程環境可用
 * - Mockito 測試框架環境可用
 *
 * 測試步驟：
 * - 驗證接口方法簽名和繼承關係的正確性
 * - 測試用戶註冊、登入、登出的完整流程
 * - 驗證密碼和郵件修改的安全機制
 * - 測試響應式流處理和異常邊界情況
 *
 * 預期結果：
 * - 接口繼承關係符合用戶服務設計模式
 * - 用戶業務流程滿足安全性和便利性需求
 * - 身份認證和權限控制機制完善
 * - 響應式處理和異常機制正確
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 用戶業務邏輯接口測試")
class UserServiceTest {

    @Mock
    private ServerWebExchange mockExchange;
    
    private UserService userService;
    private User testUser;
    private RegisterDTO testRegisterDTO;
    private AuthRequestDTO testAuthRequestDTO;
    private ResetPasswordDTO testResetPasswordDTO;
    private UserEmailDTO testUserEmailDTO;

    @BeforeEach
    void setUp() {
        // 創建測試用的 UserService 實現
        userService = new UserService() {
            @Override
            public Mono<User> create() {
                User newUser = new User();
                newUser.setId(1L);
                newUser.setUsername("newuser");
                return Mono.just(newUser);
            }

            @Override
            public Mono<User> getById(Long id) {
                if (id == null || id <= 0) {
                    return Mono.empty();
                }
                User user = new User();
                user.setId(id);
                user.setUsername("user" + id);
                user.setEmail("user" + id + "@example.com");
                return Mono.just(user);
            }

            @Override
            public Flux<User> getAll() {
                return Flux.range(1, 3)
                        .map(i -> {
                            User user = new User();
                            user.setId(i.longValue());
                            user.setUsername("user" + i);
                            return user;
                        });
            }

            @Override
            public Flux<User> getAllByParams(String type, Object... args) {
                if ("username".equals(type) && args.length > 0) {
                    String usernameFilter = args[0].toString();
                    return getAll().filter(user -> user.getUsername().contains(usernameFilter));
                }
                return getAll();
            }

            @Override
            public Mono<User> update(User entity) {
                if (entity == null || entity.getId() == null) {
                    return Mono.error(new IllegalArgumentException("用戶實體或ID不能為空"));
                }
                return Mono.just(entity);
            }

            @Override
            public Mono<Void> delete(User entity) {
                if (entity == null || entity.getId() == null) {
                    return Mono.error(new IllegalArgumentException("用戶實體或ID不能為空"));
                }
                return Mono.empty();
            }

            @Override
            public Mono<Void> register(RegisterDTO registerUserDTO) {
                if (registerUserDTO == null) {
                    return Mono.error(new IllegalArgumentException("註冊信息不能為空"));
                }
                if (registerUserDTO.getUsername() == null || registerUserDTO.getUsername().trim().isEmpty()) {
                    return Mono.error(new IllegalArgumentException("用戶名不能為空"));
                }
                return Mono.empty();
            }

            @Override
            public Mono<String> login(AuthRequestDTO authRequestDTO, ServerWebExchange request) {
                if (authRequestDTO == null || request == null) {
                    return Mono.error(new IllegalArgumentException("認證請求或請求對象不能為空"));
                }
                if ("admin".equals(authRequestDTO.getUsername()) && "password".equals(authRequestDTO.getPassword())) {
                    return Mono.just("jwt-token-12345");
                }
                return Mono.error(new IllegalArgumentException("用戶名或密碼錯誤"));
            }

            @Override
            public Mono<Void> logout(Long userId, ServerWebExchange request) {
                if (userId == null || request == null) {
                    return Mono.error(new IllegalArgumentException("用戶ID或請求對象不能為空"));
                }
                return Mono.empty();
            }

            @Override
            public Mono<User> changePassword(User user) {
                if (user == null || user.getId() == null) {
                    return Mono.error(new IllegalArgumentException("用戶實體或ID不能為空"));
                }
                // 模擬密碼已更改
                user.setPassword("newHashedPassword");
                return Mono.just(user);
            }

            @Override
            public Mono<User> changeEmail(User user) {
                if (user == null || user.getId() == null) {
                    return Mono.error(new IllegalArgumentException("用戶實體或ID不能為空"));
                }
                if (user.getEmail() == null || !user.getEmail().contains("@")) {
                    return Mono.error(new IllegalArgumentException("無效的電子郵件地址"));
                }
                return Mono.just(user);
            }

            @Override
            public Mono<Void> sendResetPasswordMail(UserEmailDTO userEmailDTO) {
                if (userEmailDTO == null) {
                    return Mono.error(new IllegalArgumentException("郵件DTO不能為空"));
                }
                if (userEmailDTO.getEmail() == null || !userEmailDTO.getEmail().contains("@")) {
                    return Mono.error(new IllegalArgumentException("無效的電子郵件地址"));
                }
                return Mono.empty();
            }

            @Override
            public Mono<Void> resetPassword(ResetPasswordDTO resetPasswordDTO) {
                if (resetPasswordDTO == null) {
                    return Mono.error(new IllegalArgumentException("重置密碼DTO不能為空"));
                }
                if (resetPasswordDTO.getVerificationCode() == null || resetPasswordDTO.getVerificationCode().trim().isEmpty()) {
                    return Mono.error(new IllegalArgumentException("重置密碼令牌不能為空"));
                }
                if (resetPasswordDTO.getNewPassword() == null || resetPasswordDTO.getNewPassword().length() < 6) {
                    return Mono.error(new IllegalArgumentException("新密碼長度不能少於6位"));
                }
                return Mono.empty();
            }

            @Override
            public Mono<User> getUser(ServerWebExchange exchange) {
                if (exchange == null) {
                    return Mono.error(new IllegalArgumentException("請求對象不能為空"));
                }
                // 模擬從 exchange 中獲取用戶信息
                User user = new User();
                user.setId(100L);
                user.setUsername("currentuser");
                return Mono.just(user);
            }

        };

        // 設置測試對象
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("oldPassword");

        testRegisterDTO = new RegisterDTO();
        testRegisterDTO.setUsername("newuser");
        testRegisterDTO.setEmail("newuser@example.com");
        testRegisterDTO.setPassword("password123");

        testAuthRequestDTO = new AuthRequestDTO("admin", "password");

        testResetPasswordDTO = new ResetPasswordDTO();
        testResetPasswordDTO.setVerificationCode("reset-token-123");
        testResetPasswordDTO.setNewPassword("newpassword123");

        testUserEmailDTO = new UserEmailDTO();
        testUserEmailDTO.setEmail("test@example.com");

        // 設置 Exchange mock
        lenient().when(mockExchange.getAttributes()).thenReturn(new ConcurrentHashMap<>());
    }

    // ==================== 一般測試 ====================

    @Test
    @DisplayName("一般測試 - register 方法基本功能")
    void testRegister_basicFunctionality() {
        StepVerifier.create(userService.register(testRegisterDTO))
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - login 方法基本功能")
    void testLogin_basicFunctionality() {
        StepVerifier.create(userService.login(testAuthRequestDTO, mockExchange))
                .expectNext("jwt-token-12345")
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - logout 方法基本功能")
    void testLogout_basicFunctionality() {
        StepVerifier.create(userService.logout(1L, mockExchange))
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - changePassword 方法基本功能")
    void testChangePassword_basicFunctionality() {
        StepVerifier.create(userService.changePassword(testUser))
                .assertNext(user -> {
                    assertEquals(testUser.getId(), user.getId());
                    assertEquals("newHashedPassword", user.getPassword());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - changeEmail 方法基本功能")
    void testChangeEmail_basicFunctionality() {
        testUser.setEmail("newemail@example.com");
        
        StepVerifier.create(userService.changeEmail(testUser))
                .assertNext(user -> {
                    assertEquals(testUser.getId(), user.getId());
                    assertEquals("newemail@example.com", user.getEmail());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - sendResetPasswordMail 方法基本功能")
    void testSendResetPasswordMail_basicFunctionality() {
        StepVerifier.create(userService.sendResetPasswordMail(testUserEmailDTO))
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - resetPassword 方法基本功能")
    void testResetPassword_basicFunctionality() {
        StepVerifier.create(userService.resetPassword(testResetPasswordDTO))
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - getUser 方法基本功能")
    void testGetUser_basicFunctionality() {
        StepVerifier.create(userService.getUser(mockExchange))
                .assertNext(user -> {
                    assertEquals(100L, user.getId());
                    assertEquals("currentuser", user.getUsername());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 繼承 CrudService 的 create 方法")
    void testInheritedCreate() {
        StepVerifier.create(userService.create())
                .assertNext(user -> {
                    assertEquals(1L, user.getId());
                    assertEquals("newuser", user.getUsername());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 繼承 CrudService 的 getById 方法")
    void testInheritedGetById() {
        StepVerifier.create(userService.getById(5L))
                .assertNext(user -> {
                    assertEquals(5L, user.getId());
                    assertEquals("user5", user.getUsername());
                    assertEquals("user5@example.com", user.getEmail());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 繼承 CrudService 的 getAll 方法")
    void testInheritedGetAll() {
        StepVerifier.create(userService.getAll())
                .expectNextCount(3)
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 繼承 CrudService 的 getAllByParams 方法")
    void testInheritedGetAllByParams() {
        StepVerifier.create(userService.getAllByParams("username", "user1"))
                .assertNext(user -> assertTrue(user.getUsername().contains("user1")))
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 繼承 CrudService 的 update 方法")
    void testInheritedUpdate() {
        StepVerifier.create(userService.update(testUser))
                .expectNext(testUser)
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 繼承 CrudService 的 delete 方法")
    void testInheritedDelete() {
        StepVerifier.create(userService.delete(testUser))
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 接口方法簽名驗證")
    void testInterfaceMethodSignatures() {
        // 驗證 UserService 特有方法
        try {
            var registerMethod = UserService.class.getMethod("register", RegisterDTO.class);
            assertEquals(Mono.class, registerMethod.getReturnType());
        } catch (NoSuchMethodException e) {
            fail("register 方法應該存在");
        }

        try {
            var loginMethod = UserService.class.getMethod("login", AuthRequestDTO.class, ServerWebExchange.class);
            assertEquals(Mono.class, loginMethod.getReturnType());
        } catch (NoSuchMethodException e) {
            fail("login 方法應該存在");
        }

        try {
            var logoutMethod = UserService.class.getMethod("logout", Long.class, ServerWebExchange.class);
            assertEquals(Mono.class, logoutMethod.getReturnType());
        } catch (NoSuchMethodException e) {
            fail("logout 方法應該存在");
        }

        try {
            var changePasswordMethod = UserService.class.getMethod("changePassword", User.class);
            assertEquals(Mono.class, changePasswordMethod.getReturnType());
        } catch (NoSuchMethodException e) {
            fail("changePassword 方法應該存在");
        }

        try {
            var changeEmailMethod = UserService.class.getMethod("changeEmail", User.class);
            assertEquals(Mono.class, changeEmailMethod.getReturnType());
        } catch (NoSuchMethodException e) {
            fail("changeEmail 方法應該存在");
        }

        try {
            var sendResetMethod = UserService.class.getMethod("sendResetPasswordMail", UserEmailDTO.class);
            assertEquals(Mono.class, sendResetMethod.getReturnType());
        } catch (NoSuchMethodException e) {
            fail("sendResetPasswordMail 方法應該存在");
        }

        try {
            var resetPasswordMethod = UserService.class.getMethod("resetPassword", ResetPasswordDTO.class);
            assertEquals(Mono.class, resetPasswordMethod.getReturnType());
        } catch (NoSuchMethodException e) {
            fail("resetPassword 方法應該存在");
        }

        try {
            var getUserMethod = UserService.class.getMethod("getUser", ServerWebExchange.class);
            assertEquals(Mono.class, getUserMethod.getReturnType());
        } catch (NoSuchMethodException e) {
            fail("getUser 方法應該存在");
        }
    }

    @Test
    @DisplayName("一般測試 - 驗證繼承 CrudService 接口")
    void testExtendsInterface() {
        // 驗證 UserService 繼承了 CrudService
        assertTrue(CrudService.class.isAssignableFrom(UserService.class));
        
        // 驗證泛型參數
        var interfaces = UserService.class.getGenericInterfaces();
        assertEquals(1, interfaces.length);
    }

    @Test
    @DisplayName("一般測試 - 用戶生命週期流程")
    void testUserLifecycle() {
        // 註冊 -> 登錄 -> 修改密碼 -> 登出
        Mono<String> lifecycle = userService.register(testRegisterDTO)
                .then(userService.login(testAuthRequestDTO, mockExchange))
                .flatMap(token -> {
                    assertNotNull(token);
                    return userService.changePassword(testUser);
                })
                .flatMap(user -> userService.logout(user.getId(), mockExchange).thenReturn("完成"));

        StepVerifier.create(lifecycle)
                .expectNext("完成")
                .verifyComplete();
    }

    // ==================== 異常測試 ====================

    @Test
    @DisplayName("異常測試 - register 傳入 null DTO")
    void testRegister_withNullDTO() {
        StepVerifier.create(userService.register(null))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - register 傳入空用戶名")
    void testRegister_withEmptyUsername() {
        testRegisterDTO.setUsername("");
        
        StepVerifier.create(userService.register(testRegisterDTO))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - login 傳入 null AuthRequestDTO")
    void testLogin_withNullAuthRequest() {
        StepVerifier.create(userService.login(null, mockExchange))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - login 傳入 null ServerWebExchange")
    void testLogin_withNullExchange() {
        StepVerifier.create(userService.login(testAuthRequestDTO, null))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - login 傳入錯誤憑證")
    void testLogin_withWrongCredentials() {
        testAuthRequestDTO = new AuthRequestDTO("wronguser", "wrongpass");
        
        StepVerifier.create(userService.login(testAuthRequestDTO, mockExchange))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - logout 傳入 null 用戶ID")
    void testLogout_withNullUserId() {
        StepVerifier.create(userService.logout(null, mockExchange))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - changePassword 傳入 null 用戶")
    void testChangePassword_withNullUser() {
        StepVerifier.create(userService.changePassword(null))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - changePassword 傳入無ID用戶")
    void testChangePassword_withUserWithoutId() {
        User userWithoutId = new User();
        userWithoutId.setUsername("test");
        
        StepVerifier.create(userService.changePassword(userWithoutId))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - changeEmail 傳入無效郵箱")
    void testChangeEmail_withInvalidEmail() {
        testUser.setEmail("invalid-email");
        
        StepVerifier.create(userService.changeEmail(testUser))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - sendResetPasswordMail 傳入 null DTO")
    void testSendResetPasswordMail_withNullDTO() {
        StepVerifier.create(userService.sendResetPasswordMail(null))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - sendResetPasswordMail 傳入無效郵箱")
    void testSendResetPasswordMail_withInvalidEmail() {
        testUserEmailDTO.setEmail("invalid-email");
        
        StepVerifier.create(userService.sendResetPasswordMail(testUserEmailDTO))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - resetPassword 傳入 null DTO")
    void testResetPassword_withNullDTO() {
        StepVerifier.create(userService.resetPassword(null))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - resetPassword 傳入空令牌")
    void testResetPassword_withEmptyToken() {
        testResetPasswordDTO.setVerificationCode("");
        
        StepVerifier.create(userService.resetPassword(testResetPasswordDTO))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - resetPassword 傳入過短密碼")
    void testResetPassword_withShortPassword() {
        testResetPasswordDTO.setNewPassword("123");
        
        StepVerifier.create(userService.resetPassword(testResetPasswordDTO))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - getUser 傳入 null Exchange")
    void testGetUser_withNullExchange() {
        StepVerifier.create(userService.getUser(null))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    // ==================== 邊界測試 ====================

    @Test
    @DisplayName("邊界測試 - register 使用極長用戶名")
    void testRegister_withVeryLongUsername() {
        String longUsername = "a".repeat(1000);
        testRegisterDTO.setUsername(longUsername);
        
        StepVerifier.create(userService.register(testRegisterDTO))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - login 使用特殊字符憑證")
    void testLogin_withSpecialCharacters() {
        testAuthRequestDTO = new AuthRequestDTO("admin<>&\"'`", "password<>&\"'`");
        
        StepVerifier.create(userService.login(testAuthRequestDTO, mockExchange))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("邊界測試 - logout 使用極大用戶ID")
    void testLogout_withMaxUserId() {
        StepVerifier.create(userService.logout(Long.MAX_VALUE, mockExchange))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - logout 使用負數用戶ID")
    void testLogout_withNegativeUserId() {
        StepVerifier.create(userService.logout(-1L, mockExchange))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - changeEmail 使用極長郵箱")
    void testChangeEmail_withVeryLongEmail() {
        String longEmail = "a".repeat(1000) + "@example.com";
        testUser.setEmail(longEmail);
        
        StepVerifier.create(userService.changeEmail(testUser))
                .expectNext(testUser)
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - changeEmail 使用 Unicode 字符")
    void testChangeEmail_withUnicodeCharacters() {
        testUser.setEmail("用戶測試🔒@example.com");
        
        StepVerifier.create(userService.changeEmail(testUser))
                .expectNext(testUser)
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - resetPassword 使用最短有效密碼")
    void testResetPassword_withMinValidPassword() {
        testResetPasswordDTO.setNewPassword("123456"); // 剛好6位
        
        StepVerifier.create(userService.resetPassword(testResetPasswordDTO))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - resetPassword 使用極長密碼")
    void testResetPassword_withVeryLongPassword() {
        String longPassword = "a".repeat(10000);
        testResetPasswordDTO.setNewPassword(longPassword);
        
        StepVerifier.create(userService.resetPassword(testResetPasswordDTO))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - resetPassword 使用特殊字符密碼")
    void testResetPassword_withSpecialCharacterPassword() {
        testResetPasswordDTO.setNewPassword("密碼測試🔑<>&\"'`");
        
        StepVerifier.create(userService.resetPassword(testResetPasswordDTO))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 併發用戶註冊")
    void testConcurrentRegister() {
        RegisterDTO dto1 = new RegisterDTO();
        dto1.setUsername("user1");
        dto1.setEmail("user1@example.com");
        
        RegisterDTO dto2 = new RegisterDTO();
        dto2.setUsername("user2");
        dto2.setEmail("user2@example.com");
        
        RegisterDTO dto3 = new RegisterDTO();
        dto3.setUsername("user3");
        dto3.setEmail("user3@example.com");

        StepVerifier.create(Mono.when(
                userService.register(dto1),
                userService.register(dto2),
                userService.register(dto3)
        )).verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 響應式流超時處理")
    void testReactiveTimeout() {
        StepVerifier.create(userService.login(testAuthRequestDTO, mockExchange)
                .timeout(Duration.ofSeconds(1)))
                .expectNext("jwt-token-12345")
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 用戶ID為0")
    void testGetById_withZeroId() {
        StepVerifier.create(userService.getById(0L))
                .verifyComplete(); // 應該返回空
    }

    @Test
    @DisplayName("邊界測試 - 郵箱格式邊界測試")
    void testEmailBoundary() {
        // 最短有效郵箱
        testUserEmailDTO.setEmail("a@b.c");
        StepVerifier.create(userService.sendResetPasswordMail(testUserEmailDTO))
                .verifyComplete();

        // 包含多個@符號
        testUserEmailDTO.setEmail("test@sub@domain.com");
        StepVerifier.create(userService.sendResetPasswordMail(testUserEmailDTO))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 空白字符處理")
    void testWhitespaceHandling() {
        testRegisterDTO.setUsername("   ");
        
        StepVerifier.create(userService.register(testRegisterDTO))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("邊界測試 - 密碼重置令牌邊界")
    void testResetTokenBoundary() {
        // 只有空格的令牌
        testResetPasswordDTO.setVerificationCode("   ");
        
        StepVerifier.create(userService.resetPassword(testResetPasswordDTO))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("邊界測試 - 驗證接口完整性")
    void testInterfaceCompleteness() {
        // 驗證接口是 public 的
        assertTrue(java.lang.reflect.Modifier.isPublic(UserService.class.getModifiers()));
        
        // 驗證接口是 interface
        assertTrue(UserService.class.isInterface());
        
        // 驗證 UserService 特有方法數量（不包括繼承的）
        assertEquals(8, UserService.class.getDeclaredMethods().length);
        
        // 驗證繼承關係
        assertTrue(CrudService.class.isAssignableFrom(UserService.class));
    }

    @Test
    @DisplayName("邊界測試 - 錯誤恢復和重試")
    void testErrorRecoveryAndRetry() {
        AuthRequestDTO wrongAuth = new AuthRequestDTO("wrong", "wrong");

        Mono<String> retryLogin = userService.login(wrongAuth, mockExchange)
                .onErrorReturn("登錄失敗")
                .flatMap(result -> {
                    if ("登錄失敗".equals(result)) {
                        return userService.login(testAuthRequestDTO, mockExchange);
                    }
                    return Mono.just(result);
                });

        StepVerifier.create(retryLogin)
                .expectNext("jwt-token-12345")
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 複雜用戶業務流程")
    void testComplexUserWorkflow() {
        // 註冊 -> 登錄 -> 修改郵箱 -> 發送重置郵件 -> 重置密碼 -> 登出
        Mono<String> complexWorkflow = userService.register(testRegisterDTO)
                .then(userService.login(testAuthRequestDTO, mockExchange))
                .flatMap(token -> {
                    testUser.setEmail("newemail@example.com");
                    return userService.changeEmail(testUser);
                })
                .flatMap(user -> userService.sendResetPasswordMail(testUserEmailDTO))
                .then(userService.resetPassword(testResetPasswordDTO))
                .then(userService.logout(testUser.getId(), mockExchange))
                .thenReturn("工作流程完成");

        StepVerifier.create(complexWorkflow)
                .expectNext("工作流程完成")
                .verifyComplete();
    }
}