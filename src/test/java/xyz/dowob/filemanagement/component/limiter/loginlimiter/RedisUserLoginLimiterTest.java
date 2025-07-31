package xyz.dowob.filemanagement.component.limiter.loginlimiter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.dowob.filemanagement.component.provider.provider.RedisProvider;
import xyz.dowob.filemanagement.config.properties.SecurityProperties;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RedisUserLoginLimiter 的測試類別。
 * 
 * 測試 RedisUserLoginLimiter 基於 Redis 的用戶登錄限流功能：
 * - tryAcquire(): 嘗試獲取用戶的限流器
 * - release(): 釋放用戶的限流器
 * - Redis 操作的正確性和異常處理
 * 
 * 前置條件：
 * - 測試覆蓋 Redis 的登錄失敗次數限制
 * - 驗證 RedisProvider 的調用正確性
 * - 確保反應式程式設計的正確性
 * 
 * 測試步驟：
 * - 測試正常限流邏輯
 * - 測試 Redis 異常情況
 * - 測試各種邊界情況
 * 
 * 預期結果：
 * - 限流器應按照配置正確工作
 * - Redis 異常應被適當處理
 * - 反應式流應正確傳播
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RedisUserLoginLimiter 測試")
class RedisUserLoginLimiterTest {

    private final int MAX_FAILURE_COUNT = 3;
    
    private final Duration LOCK_TIME = Duration.ofMinutes(5);
    
    private final String KEY_PREFIX = "user-login-limiter:";

    @Mock
    private SecurityProperties securityProperties;
    
    @Mock
    private SecurityProperties.Login loginProperties;

    @Mock
    private RedisProvider redisProvider;

    private RedisUserLoginLimiter redisUserLoginLimiter;


    @BeforeEach
    void setUp() {
        // 設定 mock SecurityProperties
        when(securityProperties.getLogin()).thenReturn(loginProperties);
        when(loginProperties.getMaxFailure()).thenReturn(MAX_FAILURE_COUNT);
        when(loginProperties.getLockTime()).thenReturn(LOCK_TIME);
        
        redisUserLoginLimiter = new RedisUserLoginLimiter(securityProperties, redisProvider);
    }

    // ==================== 一般測試 ====================

    @Test
    @DisplayName("一般測試 - 首次登錄嘗試應該成功")
    void testTryAcquire_FirstAttemptShouldSucceed() {
        // 準備測試資料
        String username = "testUser";
        String expectedKey = KEY_PREFIX + username;
        
        // 設定 mock 行為：Redis 中沒有該 key，返回空 Mono
        when(redisProvider.getValue(eq(expectedKey), eq(Integer.class)))
                .thenReturn(Mono.empty());
        when(redisProvider.incrementDelta(eq(expectedKey), eq(1L), eq(LOCK_TIME)))
                .thenReturn(Mono.just(1L));
        
        // 執行測試
        Mono<Boolean> result = redisUserLoginLimiter.tryAcquire(username);
        
        // 驗證結果
        StepVerifier.create(result)
                .assertNext(success -> assertTrue(success))
                .verifyComplete();
        
        // 驗證 Redis 操作
        verify(redisProvider).getValue(expectedKey, Integer.class);
        verify(redisProvider).incrementDelta(expectedKey, 1L, LOCK_TIME);
    }

    @Test
    @DisplayName("一般測試 - 在最大失敗次數內的嘗試應該成功")
    void testTryAcquire_WithinMaxFailureCount() {
        // 準備測試資料
        String username = "testUser";
        String expectedKey = KEY_PREFIX + username;
        
        // 設定 mock 行為：當前計數為 2，增加後為 3（等於最大失敗次數）
        when(redisProvider.getValue(eq(expectedKey), eq(Integer.class)))
                .thenReturn(Mono.just(2));
        when(redisProvider.incrementDelta(eq(expectedKey), eq(1L), eq(LOCK_TIME)))
                .thenReturn(Mono.just(3L));
        
        // 執行測試
        Mono<Boolean> result = redisUserLoginLimiter.tryAcquire(username);
        
        // 驗證結果
        StepVerifier.create(result)
                .assertNext(success -> assertTrue(success))
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 已達到最大失敗次數應該被拒絕")
    void testTryAcquire_AlreadyExceedMaxFailureCount() {
        // 準備測試資料
        String username = "testUser";
        String expectedKey = KEY_PREFIX + username;
        
        // 設定 mock 行為：當前計數已超過最大失敗次數
        when(redisProvider.getValue(eq(expectedKey), eq(Integer.class)))
                .thenReturn(Mono.just(MAX_FAILURE_COUNT + 1));
        
        // 執行測試
        Mono<Boolean> result = redisUserLoginLimiter.tryAcquire(username);
        
        // 驗證結果
        StepVerifier.create(result)
                .assertNext(success -> assertFalse(success))
                .verifyComplete();
        
        // 驗證 Redis 操作：不應該調用 incrementDelta
        verify(redisProvider).getValue(expectedKey, Integer.class);
        verify(redisProvider, never()).incrementDelta(anyString(), anyLong(), any(Duration.class));
    }

    @Test
    @DisplayName("一般測試 - 增加後超過最大失敗次數應該被拒絕")
    void testTryAcquire_ExceedAfterIncrement() {
        // 準備測試資料
        String username = "testUser";
        String expectedKey = KEY_PREFIX + username;
        
        // 設定 mock 行為：當前計數為最大值，增加後超過
        when(redisProvider.getValue(eq(expectedKey), eq(Integer.class)))
                .thenReturn(Mono.just(MAX_FAILURE_COUNT));
        when(redisProvider.incrementDelta(eq(expectedKey), eq(1L), eq(LOCK_TIME)))
                .thenReturn(Mono.just((long) MAX_FAILURE_COUNT + 1));
        
        // 執行測試
        Mono<Boolean> result = redisUserLoginLimiter.tryAcquire(username);
        
        // 驗證結果
        StepVerifier.create(result)
                .assertNext(success -> assertFalse(success))
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 釋放限流器應該刪除 Redis key")
    void testRelease_ShouldDeleteRedisKey() {
        // 準備測試資料
        String username = "testUser";
        String expectedKey = KEY_PREFIX + username;
        
        // 設定 mock 行為
        when(redisProvider.delete(eq(expectedKey)))
                .thenReturn(Mono.just(1L));
        
        // 執行測試
        Mono<Void> result = redisUserLoginLimiter.release(username);
        
        // 驗證結果
        StepVerifier.create(result)
                .verifyComplete();
        
        // 驗證 Redis 操作
        verify(redisProvider).delete(expectedKey);
    }

    @Test
    @DisplayName("一般測試 - 不同類型的 key 應該正確轉換為字符串")
    void testTryAcquire_DifferentKeyTypes() {
        // 準備測試資料
        Integer intKey = 12345;
        String expectedKey = KEY_PREFIX + intKey.toString();
        
        // 設定 mock 行為
        when(redisProvider.getValue(eq(expectedKey), eq(Integer.class)))
                .thenReturn(Mono.empty());
        when(redisProvider.incrementDelta(eq(expectedKey), eq(1L), eq(LOCK_TIME)))
                .thenReturn(Mono.just(1L));
        
        // 執行測試
        Mono<Boolean> result = redisUserLoginLimiter.tryAcquire(intKey);
        
        // 驗證結果
        StepVerifier.create(result)
                .assertNext(success -> assertTrue(success))
                .verifyComplete();
        
        // 驗證使用正確的 key
        verify(redisProvider).getValue(expectedKey, Integer.class);
    }

    // ==================== 邊界測試 ====================

    @Test
    @DisplayName("邊界測試 - 最大失敗次數為 1 的情況")
    void testTryAcquire_MaxFailureCountOne() {
        // 準備測試資料：重新創建限流器，最大失敗次數為 1
        when(loginProperties.getMaxFailure()).thenReturn(1);
        RedisUserLoginLimiter limiter = new RedisUserLoginLimiter(securityProperties, redisProvider);
        
        String username = "testUser";
        String expectedKey = KEY_PREFIX + username;
        
        // 設定 mock 行為：增加後計數為 1
        when(redisProvider.getValue(eq(expectedKey), eq(Integer.class)))
                .thenReturn(Mono.empty());
        when(redisProvider.incrementDelta(eq(expectedKey), eq(1L), eq(LOCK_TIME)))
                .thenReturn(Mono.just(1L));
        
        // 執行測試
        StepVerifier.create(limiter.tryAcquire(username))
                .assertNext(success -> assertTrue(success))
                .verifyComplete();
        
        // 再次嘗試：增加後計數為 2，應該失敗
        when(redisProvider.getValue(eq(expectedKey), eq(Integer.class)))
                .thenReturn(Mono.just(1));
        when(redisProvider.incrementDelta(eq(expectedKey), eq(1L), eq(LOCK_TIME)))
                .thenReturn(Mono.just(2L));
        
        StepVerifier.create(limiter.tryAcquire(username))
                .assertNext(success -> assertFalse(success))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 長用戶名處理")
    void testTryAcquire_LongUsername() {
        // 準備測試資料：創建很長的用戶名
        StringBuilder longUsernameBuilder = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longUsernameBuilder.append("LongUserName").append(i);
        }
        String longUsername = longUsernameBuilder.toString();
        String expectedKey = KEY_PREFIX + longUsername;
        
        // 設定 mock 行為
        when(redisProvider.getValue(eq(expectedKey), eq(Integer.class)))
                .thenReturn(Mono.empty());
        when(redisProvider.incrementDelta(eq(expectedKey), eq(1L), eq(LOCK_TIME)))
                .thenReturn(Mono.just(1L));
        
        // 執行測試
        Mono<Boolean> result = redisUserLoginLimiter.tryAcquire(longUsername);
        
        // 驗證結果
        StepVerifier.create(result)
                .assertNext(success -> assertTrue(success))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 特殊字符用戶名")
    void testTryAcquire_SpecialCharacterUsername() {
        // 準備測試資料：包含特殊字符的用戶名
        String specialUsername = "user@#$%^&*()_+-={}[]|\\:;\"'<>?,./測試用戶🎯";
        String expectedKey = KEY_PREFIX + specialUsername;
        
        // 設定 mock 行為
        when(redisProvider.getValue(eq(expectedKey), eq(Integer.class)))
                .thenReturn(Mono.empty());
        when(redisProvider.incrementDelta(eq(expectedKey), eq(1L), eq(LOCK_TIME)))
                .thenReturn(Mono.just(1L));
        
        // 執行測試
        Mono<Boolean> result = redisUserLoginLimiter.tryAcquire(specialUsername);
        
        // 驗證結果
        StepVerifier.create(result)
                .assertNext(success -> assertTrue(success))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - Redis 返回計數為 0")
    void testTryAcquire_RedisReturnsZero() {
        // 準備測試資料
        String username = "testUser";
        String expectedKey = KEY_PREFIX + username;
        
        // 設定 mock 行為：Redis 返回 0
        when(redisProvider.getValue(eq(expectedKey), eq(Integer.class)))
                .thenReturn(Mono.just(0));
        when(redisProvider.incrementDelta(eq(expectedKey), eq(1L), eq(LOCK_TIME)))
                .thenReturn(Mono.just(1L));
        
        // 執行測試
        Mono<Boolean> result = redisUserLoginLimiter.tryAcquire(username);
        
        // 驗證結果
        StepVerifier.create(result)
                .assertNext(success -> assertTrue(success))
                .verifyComplete();
    }

    // ==================== 異常測試 ====================

    @Test
    @DisplayName("異常測試 - 傳入 null key 應該拋出異常")
    void testTryAcquire_NullKey() {
        // 執行測試並驗證異常
        assertThrows(NullPointerException.class, () -> {
            redisUserLoginLimiter.tryAcquire(null).block();
        });
    }

    @Test
    @DisplayName("異常測試 - Redis getValue 操作失敗")
    void testTryAcquire_RedisGetValueError() {
        // 準備測試資料
        String username = "testUser";
        String expectedKey = KEY_PREFIX + username;
        
        // 設定 mock 行為：Redis 操作失敗
        when(redisProvider.getValue(eq(expectedKey), eq(Integer.class)))
                .thenReturn(Mono.error(new RuntimeException("Redis connection failed")));
        
        // 執行測試
        Mono<Boolean> result = redisUserLoginLimiter.tryAcquire(username);
        
        // 驗證結果：應該傳播錯誤
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - Redis incrementDelta 操作失敗")
    void testTryAcquire_RedisIncrementError() {
        // 準備測試資料
        String username = "testUser";
        String expectedKey = KEY_PREFIX + username;
        
        // 設定 mock 行為
        when(redisProvider.getValue(eq(expectedKey), eq(Integer.class)))
                .thenReturn(Mono.empty());
        when(redisProvider.incrementDelta(eq(expectedKey), eq(1L), eq(LOCK_TIME)))
                .thenReturn(Mono.error(new RuntimeException("Redis increment failed")));
        
        // 執行測試
        Mono<Boolean> result = redisUserLoginLimiter.tryAcquire(username);
        
        // 驗證結果：應該傳播錯誤
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - Redis delete 操作失敗")
    void testRelease_RedisDeleteError() {
        // 準備測試資料
        String username = "testUser";
        String expectedKey = KEY_PREFIX + username;
        
        // 設定 mock 行為：delete 操作失敗
        when(redisProvider.delete(eq(expectedKey)))
                .thenReturn(Mono.error(new RuntimeException("Redis delete failed")));
        
        // 執行測試
        Mono<Void> result = redisUserLoginLimiter.release(username);
        
        // 驗證結果：應該傳播錯誤
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - 配置最大失敗次數為 0 應拋出異常")
    void testConstructor_MaxFailureCountZero() {
        // 準備測試資料
        when(loginProperties.getMaxFailure()).thenReturn(0);
        
        // 執行測試並驗證異常
        assertThrows(IllegalArgumentException.class, () -> {
            new RedisUserLoginLimiter(securityProperties, redisProvider);
        });
    }

    @Test
    @DisplayName("異常測試 - 配置最大失敗次數為負數應拋出異常")
    void testConstructor_MaxFailureCountNegative() {
        // 準備測試資料
        when(loginProperties.getMaxFailure()).thenReturn(-1);
        
        // 執行測試並驗證異常
        assertThrows(IllegalArgumentException.class, () -> {
            new RedisUserLoginLimiter(securityProperties, redisProvider);
        });
    }

    @Test
    @DisplayName("異常測試 - 配置鎖定時間為負數應拋出異常")
    void testConstructor_NegativeLockTime() {
        // 準備測試資料
        when(loginProperties.getLockTime()).thenReturn(Duration.ofMinutes(-1));
        
        // 執行測試並驗證異常
        assertThrows(IllegalArgumentException.class, () -> {
            new RedisUserLoginLimiter(securityProperties, redisProvider);
        });
    }

    @Test
    @DisplayName("異常測試 - 配置鎖定時間為零應拋出異常")
    void testConstructor_ZeroLockTime() {
        // 準備測試資料
        when(loginProperties.getLockTime()).thenReturn(Duration.ZERO);
        
        // 執行測試並驗證異常
        assertThrows(IllegalArgumentException.class, () -> {
            new RedisUserLoginLimiter(securityProperties, redisProvider);
        });
    }

    @Test
    @DisplayName("異常測試 - Redis 返回 null")
    void testTryAcquire_RedisReturnsNull() {
        // 準備測試資料
        String username = "testUser";
        String expectedKey = KEY_PREFIX + username;
        
        // 設定 mock 行為：getValue 完成但沒有值，switchIfEmpty 應該提供默認值 0
        when(redisProvider.getValue(eq(expectedKey), eq(Integer.class)))
                .thenReturn(Mono.empty());
        when(redisProvider.incrementDelta(eq(expectedKey), eq(1L), eq(LOCK_TIME)))
                .thenReturn(Mono.just(1L));
        
        // 執行測試
        Mono<Boolean> result = redisUserLoginLimiter.tryAcquire(username);
        
        // 驗證結果：應該使用默認值 0，然後成功
        StepVerifier.create(result)
                .assertNext(success -> assertTrue(success))
                .verifyComplete();
    }

    // ==================== 併發測試 ====================

    @Test
    @DisplayName("併發測試 - 多線程同時訪問同一用戶")
    void testConcurrentAccess_SameUser() throws InterruptedException {
        // 準備測試資料
        String username = "concurrentUser";
        String expectedKey = KEY_PREFIX + username;
        int threadCount = 10;
        
        // 設定 mock 行為：每次調用都返回當前計數
        when(redisProvider.getValue(eq(expectedKey), eq(Integer.class)))
                .thenReturn(Mono.just(0), Mono.just(1), Mono.just(2), Mono.just(3), 
                           Mono.just(4), Mono.just(5), Mono.just(6), Mono.just(7),
                           Mono.just(8), Mono.just(9));
        when(redisProvider.incrementDelta(eq(expectedKey), eq(1L), eq(LOCK_TIME)))
                .thenReturn(Mono.just(1L), Mono.just(2L), Mono.just(3L), Mono.just(4L),
                           Mono.just(5L), Mono.just(6L), Mono.just(7L), Mono.just(8L),
                           Mono.just(9L), Mono.just(10L));
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CompletableFuture<Boolean>[] futures = new CompletableFuture[threadCount];
        
        // 創建多個線程同時訪問
        for (int i = 0; i < threadCount; i++) {
            futures[i] = CompletableFuture.supplyAsync(() -> {
                return redisUserLoginLimiter.tryAcquire(username).block();
            }, executor);
        }
        
        // 等待所有線程完成
        CompletableFuture.allOf(futures).join();
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        
        // 驗證結果：Redis 操作應該被調用
        verify(redisProvider, atLeast(threadCount)).getValue(expectedKey, Integer.class);
    }

    @Test
    @DisplayName("併發測試 - 多線程訪問不同用戶")
    void testConcurrentAccess_DifferentUsers() throws InterruptedException {
        // 準備測試資料
        int threadCount = 5;
        
        // 設定 mock 行為：所有用戶都是首次訪問
        when(redisProvider.getValue(anyString(), eq(Integer.class)))
                .thenReturn(Mono.empty());
        when(redisProvider.incrementDelta(anyString(), eq(1L), eq(LOCK_TIME)))
                .thenReturn(Mono.just(1L));
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CompletableFuture<Boolean>[] futures = new CompletableFuture[threadCount];
        
        // 創建多個線程訪問不同用戶
        for (int i = 0; i < threadCount; i++) {
            final int userId = i;
            futures[i] = CompletableFuture.supplyAsync(() -> {
                return redisUserLoginLimiter.tryAcquire("user" + userId).block();
            }, executor);
        }
        
        // 等待所有線程完成
        CompletableFuture.allOf(futures).join();
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        
        // 驗證結果：所有不同用戶的首次嘗試都應該成功
        for (int i = 0; i < threadCount; i++) {
            assertTrue(futures[i].join(), "用戶 user" + i + " 的首次嘗試應該成功");
        }
    }

    @Test
    @DisplayName("併發測試 - 同時釋放和獲取")
    void testConcurrentReleaseAndAcquire() throws InterruptedException {
        // 準備測試資料
        String username = "testUser";
        String expectedKey = KEY_PREFIX + username;
        
        // 設定 mock 行為
        when(redisProvider.delete(eq(expectedKey)))
                .thenReturn(Mono.just(1L));
        when(redisProvider.getValue(eq(expectedKey), eq(Integer.class)))
                .thenReturn(Mono.empty());
        when(redisProvider.incrementDelta(eq(expectedKey), eq(1L), eq(LOCK_TIME)))
                .thenReturn(Mono.just(1L));
        
        // 創建兩個線程：一個釋放，一個獲取
        ExecutorService executor = Executors.newFixedThreadPool(2);
        
        CompletableFuture<Void> releaseFuture = CompletableFuture.runAsync(() -> {
            redisUserLoginLimiter.release(username).block();
        }, executor);
        
        CompletableFuture<Boolean> acquireFuture = CompletableFuture.supplyAsync(() -> {
            return redisUserLoginLimiter.tryAcquire(username).block();
        }, executor);
        
        // 等待完成
        CompletableFuture.allOf(releaseFuture, acquireFuture).join();
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        
        // 驗證結果：獲取應該成功
        assertTrue(acquireFuture.join(), "併發情況下的獲取應該成功");
    }

    // ==================== 反應式流測試 ====================

    @Test
    @DisplayName("反應式流測試 - switchIfEmpty 正確處理空值")
    void testReactiveStream_SwitchIfEmpty() {
        // 準備測試資料
        String username = "testUser";
        String expectedKey = KEY_PREFIX + username;
        
        // 設定 mock 行為：getValue 返回空，switchIfEmpty 應該提供 0
        when(redisProvider.getValue(eq(expectedKey), eq(Integer.class)))
                .thenReturn(Mono.empty());
        when(redisProvider.incrementDelta(eq(expectedKey), eq(1L), eq(LOCK_TIME)))
                .thenReturn(Mono.just(1L));
        
        // 執行測試
        Mono<Boolean> result = redisUserLoginLimiter.tryAcquire(username);
        
        // 驗證結果：空值應該被正確處理
        StepVerifier.create(result)
                .assertNext(success -> assertTrue(success))
                .verifyComplete();
    }

    @Test
    @DisplayName("反應式流測試 - flatMap 鏈路正確執行")
    void testReactiveStream_FlatMapChain() {
        // 準備測試資料
        String username = "testUser";
        String expectedKey = KEY_PREFIX + username;
        
        // 設定 mock 行為：模擬完整的反應式鏈路
        when(redisProvider.getValue(eq(expectedKey), eq(Integer.class)))
                .thenReturn(Mono.just(1));
        when(redisProvider.incrementDelta(eq(expectedKey), eq(1L), eq(LOCK_TIME)))
                .thenReturn(Mono.just(2L));
        
        // 執行測試
        Mono<Boolean> result = redisUserLoginLimiter.tryAcquire(username);
        
        // 驗證結果：整個鏈路應該正確執行
        StepVerifier.create(result)
                .assertNext(success -> assertTrue(success))
                .verifyComplete();
        
        // 驗證調用順序
        verify(redisProvider).getValue(expectedKey, Integer.class);
        verify(redisProvider).incrementDelta(expectedKey, 1L, LOCK_TIME);
    }

    @Test
    @DisplayName("反應式流測試 - then() 操作符正確工作")
    void testReactiveStream_ThenOperator() {
        // 準備測試資料
        String username = "testUser";
        String expectedKey = KEY_PREFIX + username;
        
        // 設定 mock 行為
        when(redisProvider.delete(eq(expectedKey)))
                .thenReturn(Mono.just(1L));
        
        // 執行測試
        Mono<Void> result = redisUserLoginLimiter.release(username);
        
        // 驗證結果：then() 應該正確轉換類型
        StepVerifier.create(result)
                .verifyComplete();
    }

    // ==================== 性能測試 ====================

    @Test
    @DisplayName("性能測試 - 大量用戶訪問")
    void testPerformance_ManyUsers() {
        // 準備測試資料
        int userCount = 100;
        
        // 設定 mock 行為：所有操作都成功
        when(redisProvider.getValue(anyString(), eq(Integer.class)))
                .thenReturn(Mono.empty());
        when(redisProvider.incrementDelta(anyString(), eq(1L), eq(LOCK_TIME)))
                .thenReturn(Mono.just(1L));
        
        long startTime = System.currentTimeMillis();
        
        // 執行大量用戶訪問
        for (int i = 0; i < userCount; i++) {
            String username = "user" + i;
            Boolean result = redisUserLoginLimiter.tryAcquire(username).block();
            assertTrue(result, "用戶 " + username + " 的首次訪問應該成功");
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // 驗證性能：應該在合理時間內完成
        assertTrue(duration < 5000, "大量用戶訪問應該在 5 秒內完成，實際耗時: " + duration + "ms");
    }

    @Test
    @DisplayName("性能測試 - 反應式操作的延遲處理")
    void testPerformance_ReactiveLatency() {
        // 準備測試資料
        String username = "testUser";
        String expectedKey = KEY_PREFIX + username;
        
        // 設定 mock 行為：模擬網路延遲
        when(redisProvider.getValue(eq(expectedKey), eq(Integer.class)))
                .thenReturn(Mono.<Integer>empty().delayElement(Duration.ofMillis(50)));
        when(redisProvider.incrementDelta(eq(expectedKey), eq(1L), eq(LOCK_TIME)))
                .thenReturn(Mono.just(1L).delayElement(Duration.ofMillis(50)));
        
        long startTime = System.currentTimeMillis();
        
        // 執行測試
        Boolean result = redisUserLoginLimiter.tryAcquire(username).block();
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // 驗證結果和性能
        assertTrue(result);
        assertTrue(duration >= 50, "應該等待網路延遲，實際耗時: " + duration + "ms");
        assertTrue(duration < 2000, "總延遲不應超過 2 秒，實際耗時: " + duration + "ms");
    }
}