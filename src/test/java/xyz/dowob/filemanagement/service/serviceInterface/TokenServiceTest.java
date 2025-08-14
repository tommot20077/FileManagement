package xyz.dowob.filemanagement.service.serviceInterface;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.dowob.filemanagement.customenum.TokenEnum;
import xyz.dowob.filemanagement.data.user.dto.UserInfoDto;
import xyz.dowob.filemanagement.entity.Token;
import xyz.dowob.filemanagement.entity.User;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TokenService 憑證服務接口測試
 *
 * <p>測試 TokenService 憑證服務接口的組合契約和憑證管理模式，驗證接口在身份認證和權限管理方面的設計正確性。
 * 
 * <p>測試涵蓋的接口功能：
 * <p>- 繼承 CrudService 的 Token 實體 CRUD 操作
 * <p>- generateToken 憑證生成方法的響應式實現
 * <p>- validateToken 憑證驗證方法的安全檢查
 * <p>- revokeToken 憑證撤銷方法的狀態管理
 * <p>- TokenEnum 憑證類型枚舉的正確處理
 * <p>- 憑證生命週期管理和過期檢查
 * <p>- JWT 憑證、重設密碼憑證等多種憑證類型
 *
 * 測試摘要：
 * 
 * 驗證 TokenService 接口作為憑證管理服務層的設計正確性，確保其能夠為不同憑證類型提供統一的管理能力。
 *
 * 前置條件：
 * - TokenService 接口及其父接口可用
 * - TokenEnum 憑證類型枚舉和 Token 實體可用
 * - User 實體類和相關權限管理可用
 * - Reactor WebFlux 響應式編程環境可用
 *
 * 測試步驟：
 * - 驗證接口方法簽名和繼承關係的正確性
 * - 測試憑證生成、驗證、撤銷的完整生命週期
 * - 驗證不同憑證類型的處理邏輯和安全檢查
 * - 測試響應式流處理和異常邊界情況
 *
 * 預期結果：
 * - 接口繼承關係符合憑證服務設計模式
 * - 憑證管理流程滿足安全性需求
 * - 多種憑證類型的處理機制完善
 * - 響應式處理和異常機制正確
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TokenService 憑證服務接口測試")
class TokenServiceTest {

    private TokenService tokenService;
    private User testUser;
    private Token testToken;

    @BeforeEach
    void setUp() {
        // 創建測試用的 TokenService 實現
        tokenService = new TokenService() {
            @Override
            public Mono<Token> create() {
                Token newToken = new Token();
                newToken.setId(1L);
                newToken.setUserId(100L);
                newToken.setJwtTokenVersion("v1.0");
                // JWT憑證版本設置
                newToken.setJwtTokenExpireTime(LocalDateTime.now().plusDays(1));
                return Mono.just(newToken);
            }

            @Override
            public Mono<Token> getById(Long id) {
                if (id == null || id <= 0) {
                    return Mono.empty();
                }
                Token token = new Token();
                token.setId(id);
                token.setUserId(100L);
                token.setJwtTokenVersion("v1.0");
                token.setJwtTokenExpireTime(LocalDateTime.now().plusDays(1));
                return Mono.just(token);
            }

            @Override
            public Flux<Token> getAll() {
                return Flux.range(1, 3)
                        .map(i -> {
                            Token token = new Token();
                            token.setId(i.longValue());
                            token.setUserId(100L);
                            token.setJwtTokenVersion("v1.0");
                            token.setJwtTokenExpireTime(LocalDateTime.now().plusDays(1));
                            return token;
                        });
            }

            @Override
            public Flux<Token> getAllByParams(String type, Object... args) {
                if ("userId".equals(type) && args.length > 0) {
                    Long userId = (Long) args[0];
                    return getAll().filter(token -> token.getUserId() == userId);
                }
                if ("tokenType".equals(type) && args.length > 0) {
                    String tokenType = args[0].toString();
                    return getAll().filter(token -> token.getJwtTokenVersion().equals(tokenType));
                }
                return getAll();
            }

            @Override
            public Mono<Token> update(Token entity) {
                if (entity == null || entity.getId() <= 0) {
                    return Mono.error(new IllegalArgumentException("憑證實體或ID不能為空"));
                }
                return Mono.just(entity);
            }

            @Override
            public Mono<Void> delete(Token entity) {
                if (entity == null || entity.getId() <= 0) {
                    return Mono.error(new IllegalArgumentException("憑證實體或ID不能為空"));
                }
                return Mono.empty();
            }

            @Override
            public Mono<String> generateToken(User user, TokenEnum tokenType) {
                if (user == null) {
                    return Mono.error(new IllegalArgumentException("用戶不能為空"));
                }
                if (tokenType == null) {
                    return Mono.error(new IllegalArgumentException("憑證類型不能為空"));
                }
                if (user.getId() == null) {
                    return Mono.error(new IllegalArgumentException("用戶ID不能為空"));
                }
                
                String tokenValue = tokenType.name().toLowerCase() + "_" + user.getId() + "_" + System.currentTimeMillis();
                return Mono.just(tokenValue);
            }

            @Override
            public Mono<Long> validateToken(String token, Long userId, TokenEnum tokenType) {
                if (token == null || token.trim().isEmpty()) {
                    return Mono.error(new IllegalArgumentException("憑證不能為空"));
                }
                if (userId == null) {
                    return Mono.error(new IllegalArgumentException("用戶ID不能為空"));
                }
                if (tokenType == null) {
                    return Mono.error(new IllegalArgumentException("憑證類型不能為空"));
                }
                
                // 模擬憑證驗證邏輯
                if (token.startsWith(tokenType.name().toLowerCase() + "_" + userId + "_")) {
                    return Mono.just(userId);
                }
                return Mono.error(new IllegalArgumentException("憑證驗證失敗"));
            }

            @Override
            public Mono<Void> revokeToken(Long userId, TokenEnum tokenType) {
                if (userId == null) {
                    return Mono.error(new IllegalArgumentException("用戶ID不能為空"));
                }
                if (tokenType == null) {
                    return Mono.error(new IllegalArgumentException("憑證類型不能為空"));
                }
                return Mono.empty();
            }
            
            @Override
            public Mono<UserInfoDto> extractUserInfoFromToken(String token, TokenEnum tokenType) {
                if (token == null || token.trim().isEmpty()) {
                    return Mono.error(new IllegalArgumentException("憑證不能為空"));
                }
                if (tokenType == null) {
                    return Mono.error(new IllegalArgumentException("憑證類型不能為空"));
                }
                
                // 從憑證中提取用戶資訊 - 憑證格式為 "jwt_authorization_token_userId_timestamp"
                if (token.startsWith(tokenType.name().toLowerCase() + "_")) {
                    String[] parts = token.split("_");
                    // jwt_authorization_token_1_1234567890 有5個部分
                    if (parts.length >= 5) {
                        try {
                            Long userId = Long.parseLong(parts[3]); // userId 在索引 3
                            UserInfoDto userInfo = new UserInfoDto();
                            userInfo.setUserId(userId);
                            userInfo.setUsername("testuser" + userId);
                            userInfo.setRole(userId == 1L ? "ADMIN" : "USER");
                            userInfo.setTokenExpiry(new java.util.Date(System.currentTimeMillis() + 86400000));
                            userInfo.setTokenVersion("v1.0");
                            return Mono.just(userInfo);
                        } catch (NumberFormatException e) {
                            return Mono.error(new IllegalArgumentException("憑證格式錯誤"));
                        }
                    }
                }
                return Mono.error(new IllegalArgumentException("憑證驗證失敗"));
            }
        };

        // 設置測試對象
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        testToken = new Token();
        testToken.setId(1L);
        testToken.setUserId(1L);
        testToken.setJwtTokenVersion("v1.0");
        // JWT憑證版本設置  
        testToken.setJwtTokenExpireTime(LocalDateTime.now().plusDays(1));
    }

    // ==================== 一般測試 ====================

    @Test
    @DisplayName("一般測試 - generateToken 方法基本功能")
    void testGenerateToken_basicFunctionality() {
        StepVerifier.create(tokenService.generateToken(testUser, TokenEnum.JWT_AUTHORIZATION_TOKEN))
                .assertNext(token -> {
                    assertNotNull(token);
                    assertTrue(token.startsWith("jwt_authorization_token_1_"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - validateToken 方法基本功能")
    void testValidateToken_basicFunctionality() {
        String validToken = "jwt_authorization_token_1_1234567890";
        
        StepVerifier.create(tokenService.validateToken(validToken, 1L, TokenEnum.JWT_AUTHORIZATION_TOKEN))
                .expectNext(1L)
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - revokeToken 方法基本功能")
    void testRevokeToken_basicFunctionality() {
        StepVerifier.create(tokenService.revokeToken(1L, TokenEnum.JWT_AUTHORIZATION_TOKEN))
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 繼承 CrudService 的 create 方法")
    void testInheritedCreate() {
        StepVerifier.create(tokenService.create())
                .assertNext(token -> {
                    assertEquals(1L, token.getId());
                    assertEquals(100L, token.getUserId());
                    assertEquals("v1.0", token.getJwtTokenVersion());
                    // JWT憑證版本檢查
                    assertNotNull(token.getJwtTokenExpireTime());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 繼承 CrudService 的 getById 方法")
    void testInheritedGetById() {
        StepVerifier.create(tokenService.getById(5L))
                .assertNext(token -> {
                    assertEquals(5L, token.getId());
                    assertEquals(100L, token.getUserId());
                    assertNotNull(token.getJwtTokenExpireTime());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 繼承 CrudService 的 getAll 方法")
    void testInheritedGetAll() {
        StepVerifier.create(tokenService.getAll())
                .expectNextCount(3)
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 繼承 CrudService 的 getAllByParams 方法")
    void testInheritedGetAllByParams() {
        StepVerifier.create(tokenService.getAllByParams("userId", 100L))
                .expectNextCount(3)
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 按憑證類型查詢")
    void testGetAllByParams_byTokenType() {
        StepVerifier.create(tokenService.getAllByParams("tokenType", "v1.0"))
                .expectNextCount(3)
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 繼承 CrudService 的 update 方法")
    void testInheritedUpdate() {
        StepVerifier.create(tokenService.update(testToken))
                .expectNext(testToken)
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 繼承 CrudService 的 delete 方法")
    void testInheritedDelete() {
        StepVerifier.create(tokenService.delete(testToken))
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - extractUserInfoFromToken 方法基本功能")
    void testExtractUserInfoFromToken_basicFunctionality() {
        String validToken = "jwt_authorization_token_1_1234567890";
        
        StepVerifier.create(tokenService.extractUserInfoFromToken(validToken, TokenEnum.JWT_AUTHORIZATION_TOKEN))
                .assertNext(userInfo -> {
                    assertNotNull(userInfo);
                    assertEquals(1L, userInfo.getUserId());
                    assertEquals("testuser1", userInfo.getUsername());
                    assertEquals("ADMIN", userInfo.getRole());
                    assertNotNull(userInfo.getTokenExpiry());
                    assertEquals("v1.0", userInfo.getTokenVersion());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - extractUserInfoFromToken 不同用戶角色")
    void testExtractUserInfoFromToken_differentRoles() {
        // 測試管理員
        String adminToken = "jwt_authorization_token_1_1234567890";
        StepVerifier.create(tokenService.extractUserInfoFromToken(adminToken, TokenEnum.JWT_AUTHORIZATION_TOKEN))
                .assertNext(userInfo -> {
                    assertEquals("ADMIN", userInfo.getRole());
                    assertTrue(userInfo.isAdmin());
                })
                .verifyComplete();
        
        // 測試普通用戶
        String userToken = "jwt_authorization_token_2_1234567890";
        StepVerifier.create(tokenService.extractUserInfoFromToken(userToken, TokenEnum.JWT_AUTHORIZATION_TOKEN))
                .assertNext(userInfo -> {
                    assertEquals("USER", userInfo.getRole());
                    assertFalse(userInfo.isAdmin());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 接口方法簽名驗證")
    void testInterfaceMethodSignatures() {
        // 驗證 TokenService 特有方法
        try {
            var generateMethod = TokenService.class.getMethod("generateToken", User.class, TokenEnum.class);
            assertEquals(Mono.class, generateMethod.getReturnType());
            assertTrue(generateMethod.isAnnotationPresent(xyz.dowob.filemanagement.annotation.HideSensitive.class));
        } catch (NoSuchMethodException e) {
            fail("generateToken 方法應該存在");
        }

        try {
            var validateMethod = TokenService.class.getMethod("validateToken", String.class, Long.class, TokenEnum.class);
            assertEquals(Mono.class, validateMethod.getReturnType());
        } catch (NoSuchMethodException e) {
            fail("validateToken 方法應該存在");
        }

        try {
            var revokeMethod = TokenService.class.getMethod("revokeToken", Long.class, TokenEnum.class);
            assertEquals(Mono.class, revokeMethod.getReturnType());
        } catch (NoSuchMethodException e) {
            fail("revokeToken 方法應該存在");
        }
        
        try {
            var extractUserInfoMethod = TokenService.class.getMethod("extractUserInfoFromToken", String.class, TokenEnum.class);
            assertEquals(Mono.class, extractUserInfoMethod.getReturnType());
        } catch (NoSuchMethodException e) {
            fail("extractUserInfoFromToken 方法應該存在");
        }
    }

    @Test
    @DisplayName("一般測試 - 驗證繼承 CrudService 接口")
    void testExtendsInterface() {
        // 驗證 TokenService 繼承了 CrudService
        assertTrue(CrudService.class.isAssignableFrom(TokenService.class));
        
        // 驗證泛型參數
        var interfaces = TokenService.class.getGenericInterfaces();
        assertEquals(1, interfaces.length);
    }

    @Test
    @DisplayName("一般測試 - 不同憑證類型的生成")
    void testGenerateToken_differentTypes() {
        // 測試不同類型的憑證生成
        StepVerifier.create(tokenService.generateToken(testUser, TokenEnum.RESET_PASSWORD_TOKEN))
                .assertNext(token -> assertTrue(token.startsWith("reset_password_token_1_")))
                .verifyComplete();

        StepVerifier.create(tokenService.generateToken(testUser, TokenEnum.RESET_PASSWORD_TOKEN))
                .assertNext(token -> assertTrue(token.startsWith("reset_password_token_1_")))
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 憑證生命週期流程")
    void testTokenLifecycle() {
        // 生成 -> 驗證 -> 撤銷
        Mono<String> lifecycle = tokenService.generateToken(testUser, TokenEnum.JWT_AUTHORIZATION_TOKEN)
                .flatMap(token -> {
                    assertNotNull(token);
                    return tokenService.validateToken(token, testUser.getId(), TokenEnum.JWT_AUTHORIZATION_TOKEN);
                })
                .flatMap(userId -> {
                    assertEquals(testUser.getId(), userId);
                    return tokenService.revokeToken(userId, TokenEnum.JWT_AUTHORIZATION_TOKEN);
                })
                .thenReturn("完成");

        StepVerifier.create(lifecycle)
                .expectNext("完成")
                .verifyComplete();
    }

    // ==================== 異常測試 ====================

    @Test
    @DisplayName("異常測試 - generateToken 傳入 null 用戶")
    void testGenerateToken_withNullUser() {
        StepVerifier.create(tokenService.generateToken(null, TokenEnum.JWT_AUTHORIZATION_TOKEN))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - generateToken 傳入 null 憑證類型")
    void testGenerateToken_withNullTokenType() {
        StepVerifier.create(tokenService.generateToken(testUser, null))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - generateToken 傳入無 ID 用戶")
    void testGenerateToken_withUserWithoutId() {
        User userWithoutId = new User();
        userWithoutId.setUsername("test");
        
        StepVerifier.create(tokenService.generateToken(userWithoutId, TokenEnum.JWT_AUTHORIZATION_TOKEN))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - validateToken 傳入 null 憑證")
    void testValidateToken_withNullToken() {
        StepVerifier.create(tokenService.validateToken(null, 1L, TokenEnum.JWT_AUTHORIZATION_TOKEN))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - validateToken 傳入空憑證")
    void testValidateToken_withEmptyToken() {
        StepVerifier.create(tokenService.validateToken("", 1L, TokenEnum.JWT_AUTHORIZATION_TOKEN))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - validateToken 傳入 null 用戶ID")
    void testValidateToken_withNullUserId() {
        StepVerifier.create(tokenService.validateToken("valid-token", null, TokenEnum.JWT_AUTHORIZATION_TOKEN))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - validateToken 傳入 null 憑證類型")
    void testValidateToken_withNullTokenType() {
        StepVerifier.create(tokenService.validateToken("valid-token", 1L, null))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - validateToken 傳入無效憑證")
    void testValidateToken_withInvalidToken() {
        StepVerifier.create(tokenService.validateToken("invalid-token", 1L, TokenEnum.JWT_AUTHORIZATION_TOKEN))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - revokeToken 傳入 null 用戶ID")
    void testRevokeToken_withNullUserId() {
        StepVerifier.create(tokenService.revokeToken(null, TokenEnum.JWT_AUTHORIZATION_TOKEN))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - revokeToken 傳入 null 憑證類型")
    void testRevokeToken_withNullTokenType() {
        StepVerifier.create(tokenService.revokeToken(1L, null))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - update 傳入 null 憑證")
    void testUpdate_withNullToken() {
        StepVerifier.create(tokenService.update(null))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - update 傳入無 ID 憑證")
    void testUpdate_withTokenWithoutId() {
        Token tokenWithoutId = new Token();
        tokenWithoutId.setUserId(1L);
        
        StepVerifier.create(tokenService.update(tokenWithoutId))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - delete 傳入 null 憑證")
    void testDelete_withNullToken() {
        StepVerifier.create(tokenService.delete(null))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - delete 傳入無 ID 憑證")
    void testDelete_withTokenWithoutId() {
        Token tokenWithoutId = new Token();
        tokenWithoutId.setUserId(1L);
        
        StepVerifier.create(tokenService.delete(tokenWithoutId))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    // ==================== 邊界測試 ====================

    @Test
    @DisplayName("邊界測試 - getById 傳入 null ID")
    void testGetById_withNullId() {
        StepVerifier.create(tokenService.getById(null))
                .verifyComplete(); // 應該返回空
    }

    @Test
    @DisplayName("邊界測試 - getById 使用極大 ID 值")
    void testGetById_withMaxId() {
        StepVerifier.create(tokenService.getById(Long.MAX_VALUE))
                .assertNext(token -> {
                    assertEquals(Long.MAX_VALUE, token.getId());
                    assertNotNull(token.getJwtTokenVersion());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - getById 使用負數 ID")
    void testGetById_withNegativeId() {
        StepVerifier.create(tokenService.getById(-1L))
                .verifyComplete(); // 應該返回空
    }

    @Test
    @DisplayName("邊界測試 - validateToken 使用極長憑證")
    void testValidateToken_withVeryLongToken() {
        String longToken = "jwt_authorization_token_1_" + "a".repeat(10000);
        
        StepVerifier.create(tokenService.validateToken(longToken, 1L, TokenEnum.JWT_AUTHORIZATION_TOKEN))
                .expectNext(1L)
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - validateToken 使用空白字符憑證")
    void testValidateToken_withWhitespaceToken() {
        StepVerifier.create(tokenService.validateToken("   ", 1L, TokenEnum.JWT_AUTHORIZATION_TOKEN))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("邊界測試 - generateToken 使用極大用戶ID")
    void testGenerateToken_withMaxUserId() {
        testUser.setId(Long.MAX_VALUE);
        
        StepVerifier.create(tokenService.generateToken(testUser, TokenEnum.JWT_AUTHORIZATION_TOKEN))
                .assertNext(token -> {
                    assertTrue(token.startsWith("jwt_authorization_token_" + Long.MAX_VALUE + "_"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 併發憑證生成")
    void testConcurrentTokenGeneration() {
        StepVerifier.create(Flux.range(1, 10)
                .flatMap(i -> tokenService.generateToken(testUser, TokenEnum.JWT_AUTHORIZATION_TOKEN)))
                .expectNextCount(10)
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 響應式流超時處理")
    void testReactiveTimeout() {
        StepVerifier.create(tokenService.generateToken(testUser, TokenEnum.JWT_AUTHORIZATION_TOKEN)
                .timeout(Duration.ofSeconds(1)))
                .assertNext(token -> assertNotNull(token))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 所有憑證類型的生成")
    void testGenerateToken_allTokenTypes() {
        TokenEnum[] allTypes = TokenEnum.values();
        
        StepVerifier.create(Flux.fromArray(allTypes)
                .flatMap(tokenType -> tokenService.generateToken(testUser, tokenType)))
                .expectNextCount(allTypes.length)
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - extractUserInfoFromToken 傳入 null 憑證")
    void testExtractUserInfoFromToken_withNullToken() {
        StepVerifier.create(tokenService.extractUserInfoFromToken(null, TokenEnum.JWT_AUTHORIZATION_TOKEN))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - extractUserInfoFromToken 傳入空憑證")
    void testExtractUserInfoFromToken_withEmptyToken() {
        StepVerifier.create(tokenService.extractUserInfoFromToken("", TokenEnum.JWT_AUTHORIZATION_TOKEN))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - extractUserInfoFromToken 傳入 null 憑證類型")
    void testExtractUserInfoFromToken_withNullTokenType() {
        StepVerifier.create(tokenService.extractUserInfoFromToken("valid-token", null))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - extractUserInfoFromToken 傳入無效憑證")
    void testExtractUserInfoFromToken_withInvalidToken() {
        StepVerifier.create(tokenService.extractUserInfoFromToken("invalid-token", TokenEnum.JWT_AUTHORIZATION_TOKEN))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("邊界測試 - 驗證接口完整性")
    void testInterfaceCompleteness() {
        // 驗證接口是 public 的
        assertTrue(java.lang.reflect.Modifier.isPublic(TokenService.class.getModifiers()));
        
        // 驗證接口是 interface
        assertTrue(TokenService.class.isInterface());
        
        // 驗證 TokenService 特有方法數量（不包括繼承的） - 現在是4個方法
        assertEquals(4, TokenService.class.getDeclaredMethods().length);
        
        // 驗證繼承關係
        assertTrue(CrudService.class.isAssignableFrom(TokenService.class));
    }

    @Test
    @DisplayName("邊界測試 - 憑證格式驗證")
    void testTokenFormatValidation() {
        // 測試正確格式的憑證
        String correctToken = "jwt_authorization_token_123_1234567890";
        StepVerifier.create(tokenService.validateToken(correctToken, 123L, TokenEnum.JWT_AUTHORIZATION_TOKEN))
                .expectNext(123L)
                .verifyComplete();

        // 測試錯誤格式的憑證
        String wrongToken = "wrong_format_token";
        StepVerifier.create(tokenService.validateToken(wrongToken, 123L, TokenEnum.JWT_AUTHORIZATION_TOKEN))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("邊界測試 - 用戶ID不匹配的憑證驗證")
    void testValidateToken_userIdMismatch() {
        String tokenForUser1 = "jwt_authorization_token_1_1234567890";
        
        // 用錯誤的用戶ID驗證
        StepVerifier.create(tokenService.validateToken(tokenForUser1, 2L, TokenEnum.JWT_AUTHORIZATION_TOKEN))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("邊界測試 - 憑證類型不匹配的驗證")
    void testValidateToken_tokenTypeMismatch() {
        String accessToken = "jwt_authorization_token_1_1234567890";
        
        // 用錯誤的憑證類型驗證
        StepVerifier.create(tokenService.validateToken(accessToken, 1L, TokenEnum.RESET_PASSWORD_TOKEN))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("邊界測試 - 錯誤恢復和重試")
    void testErrorRecoveryAndRetry() {
        Mono<String> retryGeneration = tokenService.generateToken(testUser, TokenEnum.JWT_AUTHORIZATION_TOKEN)
                .onErrorResume(throwable -> {
                    // 模擬錯誤恢復
                    return tokenService.generateToken(testUser, TokenEnum.RESET_PASSWORD_TOKEN);
                });

        StepVerifier.create(retryGeneration)
                .assertNext(token -> assertNotNull(token))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 複雜憑證管理流程")
    void testComplexTokenWorkflow() {
        // 生成多種憑證 -> 驗證 -> 部分撤銷 -> 重新生成
        Mono<String> complexWorkflow = tokenService.generateToken(testUser, TokenEnum.JWT_AUTHORIZATION_TOKEN)
                .flatMap(accessToken -> tokenService.generateToken(testUser, TokenEnum.RESET_PASSWORD_TOKEN)
                        .map(refreshToken -> Arrays.asList(accessToken, refreshToken)))
                .flatMap(tokens -> {
                    // 驗證所有憑證
                    return Flux.fromIterable(tokens)
                            .flatMap(token -> {
                                TokenEnum type = token.startsWith("jwt_authorization_token") ? 
                                    TokenEnum.JWT_AUTHORIZATION_TOKEN : TokenEnum.RESET_PASSWORD_TOKEN;
                                return tokenService.validateToken(token, testUser.getId(), type);
                            })
                            .collectList();
                })
                .flatMap(userIds -> {
                    // 撤銷訪問憑證
                    return tokenService.revokeToken(testUser.getId(), TokenEnum.JWT_AUTHORIZATION_TOKEN);
                })
                .then(tokenService.generateToken(testUser, TokenEnum.JWT_AUTHORIZATION_TOKEN))
                .map(newToken -> "工作流程完成: " + newToken);

        StepVerifier.create(complexWorkflow)
                .assertNext(result -> assertTrue(result.startsWith("工作流程完成:")))
                .verifyComplete();
    }
}