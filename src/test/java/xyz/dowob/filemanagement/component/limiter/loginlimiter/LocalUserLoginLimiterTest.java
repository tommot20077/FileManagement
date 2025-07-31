package xyz.dowob.filemanagement.component.limiter.loginlimiter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.dowob.filemanagement.config.properties.SecurityProperties;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * LocalUserLoginLimiter 的測試類別。
 * 
 * 測試 LocalUserLoginLimiter 的用戶登錄限流功能：
 * - tryAcquire(): 嘗試獲取用戶的限流器
 * - release(): 釋放用戶的限流器
 * - destroy(): 銷毀用戶登入計數器
 * 
 * 前置條件：
 * - 測試覆蓋本地緩存的登錄失敗次數限制
 * - 驗證配置參數的正確性
 * - 確保併發環境下的一致性
 * 
 * 測試步驟：
 * - 測試正常限流邏輯
 * - 測試各種邊界情況
 * - 測試異常處理和錯誤恢復
 * 
 * 預期結果：
 * - 限流器應按照配置正確工作
 * - 超過最大失敗次數應被拒絕
 * - 鎖定時間到期後應重置計數
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LocalUserLoginLimiter 測試")
class LocalUserLoginLimiterTest {

    private final int MAX_FAILURE_COUNT = 3;
    
    private final Duration LOCK_TIME = Duration.ofMinutes(5);

    @Mock
    private SecurityProperties securityProperties;
    
    @Mock
    private SecurityProperties.Login loginProperties;

    private LocalUserLoginLimiter localUserLoginLimiter;


    @BeforeEach
    void setUp() {
        // 設定 mock SecurityProperties
        when(securityProperties.getLogin()).thenReturn(loginProperties);
        when(loginProperties.getMaxFailure()).thenReturn(MAX_FAILURE_COUNT);
        when(loginProperties.getLockTime()).thenReturn(LOCK_TIME);
        
        localUserLoginLimiter = new LocalUserLoginLimiter(securityProperties);
    }

    // ==================== 一般測試 ====================

    @Test
    @DisplayName("一般測試 - 首次登錄嘗試應該成功")
    void testTryAcquire_FirstAttemptShouldSucceed() {
        // 準備測試資料
        String username = "testUser";
        
        // 執行測試
        Mono<Boolean> result = localUserLoginLimiter.tryAcquire(username);
        
        // 驗證結果
        StepVerifier.create(result)
                .assertNext(success -> assertTrue(success))
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 在最大失敗次數內的嘗試應該成功")
    void testTryAcquire_WithinMaxFailureCount() {
        // 準備測試資料
        String username = "testUser";
        
        // 執行多次嘗試（不超過最大失敗次數）
        for (int i = 1; i <= MAX_FAILURE_COUNT; i++) {
            final int attempt = i; // 創建 final 變數供 lambda 使用
            Mono<Boolean> result = localUserLoginLimiter.tryAcquire(username);
            
            StepVerifier.create(result)
                    .assertNext(success -> assertTrue(success, "第 " + attempt + " 次嘗試應該成功"))
                    .verifyComplete();
        }
    }

    @Test
    @DisplayName("一般測試 - 超過最大失敗次數後應該被拒絕")
    void testTryAcquire_ExceedMaxFailureCount() {
        // 準備測試資料
        String username = "testUser";
        
        // 先進行最大次數的嘗試
        for (int i = 1; i <= MAX_FAILURE_COUNT; i++) {
            localUserLoginLimiter.tryAcquire(username).block();
        }
        
        // 第 MAX_FAILURE_COUNT + 1 次嘗試應該被拒絕
        Mono<Boolean> result = localUserLoginLimiter.tryAcquire(username);
        
        StepVerifier.create(result)
                .assertNext(success -> assertFalse(success))
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 釋放限流器後應該重置計數")
    void testRelease_ShouldResetCount() {
        // 準備測試資料
        String username = "testUser";
        
        // 先進行最大次數的嘗試
        for (int i = 1; i <= MAX_FAILURE_COUNT; i++) {
            localUserLoginLimiter.tryAcquire(username).block();
        }
        
        // 釋放限流器
        Mono<Void> releaseResult = localUserLoginLimiter.release(username);
        StepVerifier.create(releaseResult)
                .verifyComplete();
        
        // 釋放後再次嘗試應該成功
        Mono<Boolean> result = localUserLoginLimiter.tryAcquire(username);
        StepVerifier.create(result)
                .assertNext(success -> assertTrue(success))
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 不同用戶的限流器應該獨立")
    void testTryAcquire_DifferentUsersIndependent() {
        // 準備測試資料
        String user1 = "testUser1";
        String user2 = "testUser2";
        
        // user1 達到最大失敗次數
        for (int i = 1; i <= MAX_FAILURE_COUNT; i++) {
            localUserLoginLimiter.tryAcquire(user1).block();
        }
        
        // user1 的下一次嘗試應該失敗
        StepVerifier.create(localUserLoginLimiter.tryAcquire(user1))
                .assertNext(success -> assertFalse(success))
                .verifyComplete();
        
        // user2 的嘗試應該成功（不受 user1 影響）
        StepVerifier.create(localUserLoginLimiter.tryAcquire(user2))
                .assertNext(success -> assertTrue(success))
                .verifyComplete();
    }

    // ==================== 邊界測試 ====================

    @Test
    @DisplayName("邊界測試 - 最大失敗次數為 1 的情況")
    void testTryAcquire_MaxFailureCountOne() {
        // 準備測試資料：重新創建限流器，最大失敗次數為 1
        when(loginProperties.getMaxFailure()).thenReturn(1);
        LocalUserLoginLimiter limiter = new LocalUserLoginLimiter(securityProperties);
        String username = "testUser";
        
        // 第一次嘗試應該成功
        StepVerifier.create(limiter.tryAcquire(username))
                .assertNext(success -> assertTrue(success))
                .verifyComplete();
        
        // 第二次嘗試應該失敗
        StepVerifier.create(limiter.tryAcquire(username))
                .assertNext(success -> assertFalse(success))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 極短的鎖定時間")
    void testTryAcquire_VeryShortLockTime() throws InterruptedException {
        // 準備測試資料：使用極短的鎖定時間
        Duration shortLockTime = Duration.ofMillis(100);
        when(loginProperties.getLockTime()).thenReturn(shortLockTime);
        LocalUserLoginLimiter limiter = new LocalUserLoginLimiter(securityProperties);
        
        String username = "testUser";
        
        // 達到最大失敗次數
        for (int i = 1; i <= MAX_FAILURE_COUNT; i++) {
            limiter.tryAcquire(username).block();
        }
        
        // 立即嘗試應該失敗
        StepVerifier.create(limiter.tryAcquire(username))
                .assertNext(success -> assertFalse(success))
                .verifyComplete();
        
        // 等待鎖定時間過期
        Thread.sleep(shortLockTime.toMillis() + 50);
        
        // 過期後嘗試應該成功
        StepVerifier.create(limiter.tryAcquire(username))
                .assertNext(success -> assertTrue(success))
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
        
        // 執行測試
        Mono<Boolean> result = localUserLoginLimiter.tryAcquire(longUsername);
        
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
        
        // 執行測試
        Mono<Boolean> result = localUserLoginLimiter.tryAcquire(specialUsername);
        
        // 驗證結果
        StepVerifier.create(result)
                .assertNext(success -> assertTrue(success))
                .verifyComplete();
    }

    // ==================== 異常測試 ====================

    @Test
    @DisplayName("異常測試 - 傳入 null 用戶名應拋出異常")
    void testTryAcquire_NullUsername() {
        // 執行測試並驗證異常
        assertThrows(IllegalArgumentException.class, () -> {
            localUserLoginLimiter.tryAcquire(null).block();
        });
    }

    @Test
    @DisplayName("異常測試 - 傳入非字符串類型的 key")
    void testTryAcquire_NonStringKey() {
        // 準備測試資料
        Integer intKey = 12345;
        
        // 執行測試並驗證異常
        assertThrows(ClassCastException.class, () -> {
            localUserLoginLimiter.tryAcquire(intKey).block();
        });
    }

    @Test
    @DisplayName("異常測試 - 配置最大失敗次數為 0 應拋出異常")
    void testConstructor_MaxFailureCountZero() {
        // 準備測試資料
        when(loginProperties.getMaxFailure()).thenReturn(0);
        
        // 執行測試並驗證異常
        assertThrows(IllegalArgumentException.class, () -> {
            new LocalUserLoginLimiter(securityProperties);
        });
    }

    @Test
    @DisplayName("異常測試 - 配置最大失敗次數為負數應拋出異常")
    void testConstructor_MaxFailureCountNegative() {
        // 準備測試資料
        when(loginProperties.getMaxFailure()).thenReturn(-1);
        
        // 執行測試並驗證異常
        assertThrows(IllegalArgumentException.class, () -> {
            new LocalUserLoginLimiter(securityProperties);
        });
    }

    @Test
    @DisplayName("異常測試 - 配置鎖定時間為負數應拋出異常")
    void testConstructor_NegativeLockTime() {
        // 準備測試資料
        when(loginProperties.getLockTime()).thenReturn(Duration.ofMinutes(-1));
        
        // 執行測試並驗證異常
        assertThrows(IllegalArgumentException.class, () -> {
            new LocalUserLoginLimiter(securityProperties);
        });
    }

    @Test
    @DisplayName("異常測試 - 配置鎖定時間為零應拋出異常")
    void testConstructor_ZeroLockTime() {
        // 準備測試資料
        when(loginProperties.getLockTime()).thenReturn(Duration.ZERO);
        
        // 執行測試並驗證異常
        assertThrows(IllegalArgumentException.class, () -> {
            new LocalUserLoginLimiter(securityProperties);
        });
    }

    @Test
    @DisplayName("異常測試 - 釋放不存在的用戶限流器")
    void testRelease_NonExistentUser() {
        // 準備測試資料
        String nonExistentUser = "nonExistentUser";
        
        // 執行測試：釋放不存在的用戶應該正常完成
        Mono<Void> result = localUserLoginLimiter.release(nonExistentUser);
        
        StepVerifier.create(result)
                .verifyComplete();
    }

    // ==================== 併發測試 ====================

    @Test
    @DisplayName("併發測試 - 多線程同時訪問同一用戶")
    void testConcurrentAccess_SameUser() throws InterruptedException {
        // 準備測試資料
        String username = "concurrentUser";
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CompletableFuture<Boolean>[] futures = new CompletableFuture[threadCount];
        
        // 創建多個線程同時訪問
        for (int i = 0; i < threadCount; i++) {
            futures[i] = CompletableFuture.supplyAsync(() -> {
                return localUserLoginLimiter.tryAcquire(username).block();
            }, executor);
        }
        
        // 等待所有線程完成
        CompletableFuture.allOf(futures).join();
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        
        // 驗證結果：應該有部分成功，部分失敗
        int successCount = 0;
        for (int i = 0; i < futures.length; i++) {
            if (futures[i].join()) {
                successCount++;
            }
        }
        
        // 成功次數應該不超過最大失敗次數
        assertTrue(successCount <= MAX_FAILURE_COUNT, 
                "成功次數(" + successCount + ")不應超過最大失敗次數(" + MAX_FAILURE_COUNT + ")");
    }

    @Test
    @DisplayName("併發測試 - 多線程訪問不同用戶")
    void testConcurrentAccess_DifferentUsers() throws InterruptedException {
        // 準備測試資料
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CompletableFuture<Boolean>[] futures = new CompletableFuture[threadCount];
        
        // 創建多個線程訪問不同用戶
        for (int i = 0; i < threadCount; i++) {
            final int userId = i;
            futures[i] = CompletableFuture.supplyAsync(() -> {
                return localUserLoginLimiter.tryAcquire("user" + userId).block();
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
        
        // 先達到最大失敗次數
        for (int i = 1; i <= MAX_FAILURE_COUNT; i++) {
            localUserLoginLimiter.tryAcquire(username).block();
        }
        
        // 創建兩個線程：一個釋放，一個獲取
        ExecutorService executor = Executors.newFixedThreadPool(2);
        
        CompletableFuture<Void> releaseFuture = CompletableFuture.runAsync(() -> {
            localUserLoginLimiter.release(username).block();
        }, executor);
        
        CompletableFuture<Boolean> acquireFuture = CompletableFuture.supplyAsync(() -> {
            // 稍微延遲以確保釋放有機會執行
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return localUserLoginLimiter.tryAcquire(username).block();
        }, executor);
        
        // 等待完成
        CompletableFuture.allOf(releaseFuture, acquireFuture).join();
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        
        // 驗證結果：釋放後的獲取應該成功
        assertTrue(acquireFuture.join(), "釋放後的獲取應該成功");
    }

    // ==================== 生命週期測試 ====================

    @Test
    @DisplayName("生命週期測試 - destroy 方法應該正常執行")
    void testDestroy() {
        // 準備測試資料
        String username = "testUser";
        
        // 先進行一些操作
        localUserLoginLimiter.tryAcquire(username).block();
        
        // 執行銷毀
        assertDoesNotThrow(() -> {
            localUserLoginLimiter.destroy();
        });
        
        // 銷毀後仍然可以正常使用（CacheConcurrentHashMap 的特性）
        StepVerifier.create(localUserLoginLimiter.tryAcquire(username))
                .assertNext(success -> assertTrue(success))
                .verifyComplete();
    }

    // ==================== 性能測試 ====================

    @Test
    @DisplayName("性能測試 - 大量用戶訪問")
    void testPerformance_ManyUsers() {
        // 準備測試資料
        int userCount = 1000;
        long startTime = System.currentTimeMillis();
        
        // 執行大量用戶訪問
        for (int i = 0; i < userCount; i++) {
            String username = "user" + i;
            Boolean result = localUserLoginLimiter.tryAcquire(username).block();
            assertTrue(result, "用戶 " + username + " 的首次訪問應該成功");
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // 驗證性能：應該在合理時間內完成
        assertTrue(duration < 5000, "大量用戶訪問應該在 5 秒內完成，實際耗時: " + duration + "ms");
    }

    @Test
    @DisplayName("性能測試 - 高頻訪問同一用戶")
    void testPerformance_HighFrequencyAccess() {
        // 準備測試資料
        String username = "highFrequencyUser";
        int accessCount = 10000;
        long startTime = System.currentTimeMillis();
        
        // 執行高頻訪問
        for (int i = 0; i < accessCount; i++) {
            localUserLoginLimiter.tryAcquire(username).block();
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // 驗證性能：應該在合理時間內完成
        assertTrue(duration < 3000, "高頻訪問應該在 3 秒內完成，實際耗時: " + duration + "ms");
    }
}