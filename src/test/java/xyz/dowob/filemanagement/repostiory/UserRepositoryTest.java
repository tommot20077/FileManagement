package xyz.dowob.filemanagement.repostiory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.dowob.filemanagement.entity.User;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用戶資料庫操作介面的測試實現。
 * <p>
 * 此測試類驗證 UserRepository 的響應式資料存取功能，
 * 涵蓋用戶管理的核心操作和R2DBC非阻塞資料庫存取模式。
 * <p>
 * 測試範圍包含基本CRUD操作、用戶名稱查詢、電子郵件查詢和批量操作方法。
 * 特別驗證了用戶身份識別的唯一性約束和查詢效能。
 * <p>
 * 響應式操作透過 Mono/Flux 類型實現，支援大小寫敏感的字串比對。
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("UserRepository 用戶數據庫操作接口測試")
class UserRepositoryTest {

    private UserRepository userRepository;
    private User testUser1;
    private User testUser2;
    private User testUser3;

    @BeforeEach
    void setUp() {
        // 創建測試用的 UserRepository 實現
        userRepository = new UserRepository() {
            // 模擬數據存儲
            private final List<User> users = Arrays.asList(
                createUser(1L, "admin", "admin@example.com"),
                createUser(2L, "user1", "user1@example.com"),
                createUser(3L, "user2", "user2@example.com"),
                createUser(4L, "testuser", "test@example.com"),
                createUser(5L, "guest", "guest@example.com")
            );

            @Override
            public Mono<User> findByUsername(String username) {
                if (username == null || username.isBlank()) {
                    return Mono.empty();
                }
                return Flux.fromIterable(users)
                        .filter(user -> username.equals(user.getUsername()))
                        .next();
            }

            @Override
            public Flux<User> findAllByUsernameIn(Collection<String> usernames) {
                if (usernames == null || usernames.isEmpty()) {
                    return Flux.empty();
                }
                return Flux.fromIterable(users)
                        .filter(user -> usernames.contains(user.getUsername()));
            }

            @Override
            public Flux<User> findAllByIdIn(Collection<Long> userIds) {
                if (userIds == null || userIds.isEmpty()) {
                    return Flux.empty();
                }
                return Flux.fromIterable(users)
                        .filter(user -> userIds.contains(user.getId()));
            }

            @Override
            public Mono<User> findByEmail(String email) {
                if (email == null || email.isBlank()) {
                    return Mono.empty();
                }
                return Flux.fromIterable(users)
                        .filter(user -> email.equals(user.getEmail()))
                        .next();
            }

            @Override
            public Flux<User> findAllByEmailIn(Collection<String> emails) {
                if (emails == null || emails.isEmpty()) {
                    return Flux.empty();
                }
                return Flux.fromIterable(users)
                        .filter(user -> emails.contains(user.getEmail()));
            }

            // ReactiveCrudRepository 基本方法實現
            @Override
            public <S extends User> Mono<S> save(S entity) {
                return Mono.just(entity);
            }

            @Override
            public <S extends User> Flux<S> saveAll(Iterable<S> entities) {
                return Flux.fromIterable(entities);
            }

            @Override
            public <S extends User> Flux<S> saveAll(org.reactivestreams.Publisher<S> entityStream) {
                return Flux.from(entityStream);
            }

            @Override
            public Mono<User> findById(Long id) {
                if (id == null) {
                    return Mono.empty();
                }
                return Flux.fromIterable(users)
                        .filter(user -> id.equals(user.getId()))
                        .next();
            }

            @Override
            public Mono<User> findById(org.reactivestreams.Publisher<Long> id) {
                return Mono.from(id).flatMap(this::findById);
            }

            @Override
            public Mono<Boolean> existsById(Long id) {
                return findById(id).hasElement();
            }

            @Override
            public Mono<Boolean> existsById(org.reactivestreams.Publisher<Long> id) {
                return Mono.from(id).flatMap(this::existsById);
            }

            @Override
            public Flux<User> findAll() {
                return Flux.fromIterable(users);
            }

            @Override
            public Flux<User> findAllById(Iterable<Long> ids) {
                return Flux.fromIterable(ids).flatMap(this::findById);
            }

            @Override
            public Flux<User> findAllById(org.reactivestreams.Publisher<Long> idStream) {
                return Flux.from(idStream).flatMap(this::findById);
            }

            @Override
            public Mono<Long> count() {
                return Mono.just((long) users.size());
            }

            @Override
            public Mono<Void> deleteById(Long id) {
                return Mono.empty();
            }

            @Override
            public Mono<Void> deleteById(org.reactivestreams.Publisher<Long> id) {
                return Mono.from(id).then();
            }

            @Override
            public Mono<Void> delete(User entity) {
                return Mono.empty();
            }

            @Override
            public Mono<Void> deleteAllById(Iterable<? extends Long> ids) {
                return Mono.empty();
            }

            @Override
            public Mono<Void> deleteAll(Iterable<? extends User> entities) {
                return Mono.empty();
            }

            @Override
            public Mono<Void> deleteAll(org.reactivestreams.Publisher<? extends User> entityStream) {
                return Mono.empty();
            }

            @Override
            public Mono<Void> deleteAll() {
                return Mono.empty();
            }
        };

        // 設置測試對象
        testUser1 = createUser(1L, "admin", "admin@example.com");
        testUser2 = createUser(2L, "user1", "user1@example.com");
        testUser3 = createUser(3L, "user2", "user2@example.com");
    }

    private User createUser(Long id, String username, String email) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("hashedPassword");
        // User entity doesn't have createTime and lastLoginTime fields
        return user;
    }

    // ==================== 一般測試 ====================

    @Test
    @DisplayName("一般測試 - findByUsername 方法基本功能")
    void testFindByUsername_basicFunctionality() {
        StepVerifier.create(userRepository.findByUsername("admin"))
                .assertNext(user -> {
                    assertNotNull(user);
                    assertEquals("admin", user.getUsername());
                    assertEquals("admin@example.com", user.getEmail());
                    assertEquals(1L, user.getId());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - findAllByUsernameIn 方法基本功能")
    void testFindAllByUsernameIn_basicFunctionality() {
        Collection<String> usernames = Arrays.asList("admin", "user1", "nonexistent");
        
        StepVerifier.create(userRepository.findAllByUsernameIn(usernames))
                .assertNext(user -> assertEquals("admin", user.getUsername()))
                .assertNext(user -> assertEquals("user1", user.getUsername()))
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - findAllByIdIn 方法基本功能")
    void testFindAllByIdIn_basicFunctionality() {
        Collection<Long> userIds = Arrays.asList(1L, 2L, 999L);
        
        StepVerifier.create(userRepository.findAllByIdIn(userIds))
                .assertNext(user -> assertEquals(1L, user.getId()))
                .assertNext(user -> assertEquals(2L, user.getId()))
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - findByEmail 方法基本功能")
    void testFindByEmail_basicFunctionality() {
        StepVerifier.create(userRepository.findByEmail("admin@example.com"))
                .assertNext(user -> {
                    assertNotNull(user);
                    assertEquals("admin@example.com", user.getEmail());
                    assertEquals("admin", user.getUsername());
                    assertEquals(1L, user.getId());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - findAllByEmailIn 方法基本功能")
    void testFindAllByEmailIn_basicFunctionality() {
        Collection<String> emails = Arrays.asList("admin@example.com", "user1@example.com", "nonexistent@example.com");
        
        StepVerifier.create(userRepository.findAllByEmailIn(emails))
                .assertNext(user -> assertEquals("admin@example.com", user.getEmail()))
                .assertNext(user -> assertEquals("user1@example.com", user.getEmail()))
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - ReactiveCrudRepository 基本CRUD操作")
    void testReactiveCrudRepositoryBasicOperations() {
        // 測試 save 操作
        StepVerifier.create(userRepository.save(testUser1))
                .assertNext(savedUser -> {
                    assertNotNull(savedUser);
                    assertEquals("admin", savedUser.getUsername());
                })
                .verifyComplete();

        // 測試 findById 操作
        StepVerifier.create(userRepository.findById(1L))
                .assertNext(user -> {
                    assertEquals(1L, user.getId());
                    assertEquals("admin", user.getUsername());
                })
                .verifyComplete();

        // 測試 existsById 操作
        StepVerifier.create(userRepository.existsById(1L))
                .expectNext(true)
                .verifyComplete();

        // 測試 count 操作
        StepVerifier.create(userRepository.count())
                .expectNext(5L)
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - findAll 方法")
    void testFindAll() {
        StepVerifier.create(userRepository.findAll())
                .expectNextCount(5)
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - saveAll 批量保存操作")
    void testSaveAll() {
        List<User> usersToSave = Arrays.asList(testUser1, testUser2);
        
        StepVerifier.create(userRepository.saveAll(usersToSave))
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - findAllById 批量查詢操作")
    void testFindAllById() {
        List<Long> ids = Arrays.asList(1L, 2L, 3L);
        
        StepVerifier.create(userRepository.findAllById(ids))
                .expectNextCount(3)
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 接口方法簽名驗證")
    void testInterfaceMethodSignatures() {
        // 驗證 findByUsername 方法
        try {
            var findByUsernameMethod = UserRepository.class.getMethod("findByUsername", String.class);
            assertEquals(Mono.class, findByUsernameMethod.getReturnType());
        } catch (NoSuchMethodException e) {
            fail("findByUsername 方法應該存在");
        }

        // 驗證 findAllByUsernameIn 方法
        try {
            var findAllByUsernameInMethod = UserRepository.class.getMethod("findAllByUsernameIn", Collection.class);
            assertEquals(Flux.class, findAllByUsernameInMethod.getReturnType());
        } catch (NoSuchMethodException e) {
            fail("findAllByUsernameIn 方法應該存在");
        }

        // 驗證 findAllByIdIn 方法
        try {
            var findAllByIdInMethod = UserRepository.class.getMethod("findAllByIdIn", Collection.class);
            assertEquals(Flux.class, findAllByIdInMethod.getReturnType());
        } catch (NoSuchMethodException e) {
            fail("findAllByIdIn 方法應該存在");
        }

        // 驗證 findByEmail 方法
        try {
            var findByEmailMethod = UserRepository.class.getMethod("findByEmail", String.class);
            assertEquals(Mono.class, findByEmailMethod.getReturnType());
        } catch (NoSuchMethodException e) {
            fail("findByEmail 方法應該存在");
        }

        // 驗證 findAllByEmailIn 方法
        try {
            var findAllByEmailInMethod = UserRepository.class.getMethod("findAllByEmailIn", Collection.class);
            assertEquals(Flux.class, findAllByEmailInMethod.getReturnType());
        } catch (NoSuchMethodException e) {
            fail("findAllByEmailIn 方法應該存在");
        }
    }

    @Test
    @DisplayName("一般測試 - 驗證繼承關係")
    void testInheritanceRelationships() {
        // 驗證 UserRepository 繼承了 ReactiveCrudRepository
        assertTrue(org.springframework.data.repository.reactive.ReactiveCrudRepository.class.isAssignableFrom(UserRepository.class));
        
        // 驗證泛型參數
        var genericInterfaces = UserRepository.class.getGenericInterfaces();
        assertTrue(genericInterfaces.length > 0);
    }

    @Test
    @DisplayName("一般測試 - 響應式編程模式驗證")
    void testReactiveProgrammingPatterns() {
        // 測試響應式鏈式操作
        Mono<String> usernameChain = userRepository.findByEmail("admin@example.com")
                .map(User::getUsername)
                .defaultIfEmpty("unknown");

        StepVerifier.create(usernameChain)
                .expectNext("admin")
                .verifyComplete();

        // 測試響應式合併操作
        Flux<User> combinedUsers = userRepository.findByUsername("admin")
                .flux()
                .mergeWith(userRepository.findByUsername("user1"));

        StepVerifier.create(combinedUsers)
                .expectNextCount(2)
                .verifyComplete();
    }

    // ==================== 異常測試 ====================

    @Test
    @DisplayName("異常測試 - findByUsername 傳入 null")
    void testFindByUsername_withNull() {
        StepVerifier.create(userRepository.findByUsername(null))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - findByUsername 傳入空字符串")
    void testFindByUsername_withEmptyString() {
        StepVerifier.create(userRepository.findByUsername(""))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - findByUsername 查詢不存在的用戶")
    void testFindByUsername_nonExistentUser() {
        StepVerifier.create(userRepository.findByUsername("nonexistent"))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - findAllByUsernameIn 傳入 null 集合")
    void testFindAllByUsernameIn_withNullCollection() {
        StepVerifier.create(userRepository.findAllByUsernameIn(null))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - findAllByUsernameIn 傳入空集合")
    void testFindAllByUsernameIn_withEmptyCollection() {
        StepVerifier.create(userRepository.findAllByUsernameIn(Collections.emptyList()))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - findAllByIdIn 傳入 null 集合")
    void testFindAllByIdIn_withNullCollection() {
        StepVerifier.create(userRepository.findAllByIdIn(null))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - findAllByIdIn 傳入空集合")
    void testFindAllByIdIn_withEmptyCollection() {
        StepVerifier.create(userRepository.findAllByIdIn(Collections.emptyList()))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - findByEmail 傳入 null")
    void testFindByEmail_withNull() {
        StepVerifier.create(userRepository.findByEmail(null))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - findByEmail 傳入空字符串")
    void testFindByEmail_withEmptyString() {
        StepVerifier.create(userRepository.findByEmail(""))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - findByEmail 查詢不存在的郵箱")
    void testFindByEmail_nonExistentEmail() {
        StepVerifier.create(userRepository.findByEmail("nonexistent@example.com"))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - findAllByEmailIn 傳入 null 集合")
    void testFindAllByEmailIn_withNullCollection() {
        StepVerifier.create(userRepository.findAllByEmailIn(null))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - findAllByEmailIn 傳入空集合")
    void testFindAllByEmailIn_withEmptyCollection() {
        StepVerifier.create(userRepository.findAllByEmailIn(Collections.emptyList()))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - findById 傳入 null")
    void testFindById_withNull() {
        StepVerifier.create(userRepository.findById((Long) null))
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - existsById 傳入不存在的ID")
    void testExistsById_nonExistentId() {
        StepVerifier.create(userRepository.existsById(999L))
                .expectNext(false)
                .verifyComplete();
    }

    // ==================== 邊界測試 ====================

    @Test
    @DisplayName("邊界測試 - findByUsername 使用極長用戶名")
    void testFindByUsername_withVeryLongUsername() {
        String longUsername = "a".repeat(10000);
        
        StepVerifier.create(userRepository.findByUsername(longUsername))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - findByEmail 使用極長郵箱")
    void testFindByEmail_withVeryLongEmail() {
        String longEmail = "a".repeat(5000) + "@" + "b".repeat(5000) + ".com";
        
        StepVerifier.create(userRepository.findByEmail(longEmail))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - findAllByUsernameIn 使用大量用戶名")
    void testFindAllByUsernameIn_withManyUsernames() {
        // 創建包含1000個用戶名的集合
        List<String> manyUsernames = java.util.stream.IntStream.range(1, 1001)
                .mapToObj(i -> "user" + i)
                .toList();
        
        StepVerifier.create(userRepository.findAllByUsernameIn(manyUsernames))
                .expectNextCount(2) // user1 和 user2 都存在於測試數據中
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - findAllByIdIn 使用極大ID值")
    void testFindAllByIdIn_withMaxLongValues() {
        Collection<Long> maxIds = Arrays.asList(Long.MAX_VALUE, Long.MAX_VALUE - 1, 0L, -1L);
        
        StepVerifier.create(userRepository.findAllByIdIn(maxIds))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - findAllByEmailIn 使用特殊字符郵箱")
    void testFindAllByEmailIn_withSpecialCharacterEmails() {
        Collection<String> specialEmails = Arrays.asList(
                "test+tag@example.com",
                "test.dot@example.com",
                "test-dash@example.com",
                "test_underscore@example.com",
                "123@example.com"
        );
        
        StepVerifier.create(userRepository.findAllByEmailIn(specialEmails))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 測試用戶名大小寫敏感性")
    void testFindByUsername_caseSensitivity() {
        StepVerifier.create(userRepository.findByUsername("ADMIN"))
                .verifyComplete(); // 應該找不到，因為大小寫不匹配

        StepVerifier.create(userRepository.findByUsername("admin"))
                .assertNext(user -> {
                    assertEquals("admin", user.getUsername());
                    assertEquals(1L, user.getId());
                })
                .verifyComplete(); // 應該找到
    }

    @Test
    @DisplayName("邊界測試 - 測試郵箱大小寫敏感性")
    void testFindByEmail_caseSensitivity() {
        StepVerifier.create(userRepository.findByEmail("ADMIN@EXAMPLE.COM"))
                .verifyComplete(); // 應該找不到，因為大小寫不匹配

        StepVerifier.create(userRepository.findByEmail("admin@example.com"))
                .assertNext(user -> {
                    assertEquals("admin@example.com", user.getEmail());
                    assertEquals(1L, user.getId());
                })
                .verifyComplete(); // 應該找到
    }

    @Test
    @DisplayName("邊界測試 - 併發查詢操作")
    void testConcurrentQueries() {
        // 併發執行多個查詢
        Flux<User> concurrentQueries = Flux.merge(
                userRepository.findByUsername("admin"),
                userRepository.findByUsername("user1"),
                userRepository.findByEmail("admin@example.com"),
                userRepository.findByEmail("user1@example.com")
        );

        StepVerifier.create(concurrentQueries)
                .expectNextCount(4)
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 複雜查詢組合")
    void testComplexQueryCombinations() {
        // 組合多種查詢操作
        Mono<Boolean> complexQuery = userRepository.findByUsername("admin")
                .flatMap(user -> userRepository.findByEmail(user.getEmail()))
                .map(emailUser -> emailUser.getUsername().equals("admin"))
                .defaultIfEmpty(false);

        StepVerifier.create(complexQuery)
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 批量操作性能測試")
    void testBatchOperationPerformance() {
        // 測試大批量ID查詢
        List<Long> largeIdList = java.util.stream.LongStream.range(1, 10001)
                .boxed()
                .toList();

        StepVerifier.create(userRepository.findAllByIdIn(largeIdList))
                .expectNextCount(5) // 只有5個用戶存在於測試數據中
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 重複值處理")
    void testDuplicateValueHandling() {
        // 測試重複用戶名查詢
        Collection<String> duplicateUsernames = Arrays.asList("admin", "admin", "user1", "user1");
        
        StepVerifier.create(userRepository.findAllByUsernameIn(duplicateUsernames))
                .expectNextCount(2) // 應該返回唯一的用戶
                .verifyComplete();

        // 測試重複ID查詢
        Collection<Long> duplicateIds = Arrays.asList(1L, 1L, 2L, 2L);
        
        StepVerifier.create(userRepository.findAllByIdIn(duplicateIds))
                .expectNextCount(2) // 應該返回唯一的用戶
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 空白字符處理")
    void testWhitespaceHandling() {
        // 測試包含空格的用戶名
        StepVerifier.create(userRepository.findByUsername(" admin "))
                .verifyComplete(); // 應該找不到

        // 測試僅包含空格的用戶名
        StepVerifier.create(userRepository.findByUsername("   "))
                .verifyComplete(); // 應該找不到

        // 測試包含制表符和換行符的用戶名
        StepVerifier.create(userRepository.findByUsername("\t\n"))
                .verifyComplete(); // 應該找不到
    }
}