package xyz.dowob.filemanagement.functionInterface;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.dowob.filemanagement.customenum.RoleEnum;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.entity.UserFileMetadata;
import xyz.dowob.filemanagement.exception.ValidationException;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FunctionalInterface 測試類
 * 
 * 測試所有函數式接口的各種功能，包括：
 * - Permission<T> 權限檢查函數式接口
 * - Operation 操作執行函數式接口
 * - CacheRule<T> 緩存規則函數式接口
 * - Lambda 表達式和方法引用
 * - 函數式接口組合和鏈式調用
 * - 異常處理和響應式編程
 * - 泛型函數式接口的類型安全
 * 
 * 前置條件：
 * - 所有函數式接口正常定義
 * - @FunctionalInterface 註解正確
 * - 響應式編程支持正常
 * 
 * 測試步驟：
 * - 創建函數式接口實現
 * - 測試 Lambda 表達式和方法引用
 * - 驗證泛型類型安全性
 * - 測試異常處理和響應式操作
 * 
 * 預期結果：
 * - 所有函數式接口都能正確工作
 * - Lambda 表達式和方法引用正常
 * - 異常處理和響應式操作正確
 * 
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("FunctionalInterface 函數式接口測試")
class FunctionalInterfaceTest {

    private User testUser;
    private UserFileMetadata testFile;

    /**
     * 測試前置設定。
     * 
     * 初始化測試所需的用戶和檔案元資料對象。
     * 
     * 前置條件：
     * - 無
     * 
     * 測試步驟：
     * - 創建測試用戶對象並設定基本屬性
     * - 創建測試檔案元資料對象並設定基本屬性
     * 
     * 預期結果：
     * - 測試對象初始化完成
     */
    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setRole(RoleEnum.USER);

        testFile = new UserFileMetadata();
        testFile.setId(1L);
        testFile.setUserId(1L);
        testFile.setFilename("test.txt");
    }

    // ==================== Permission<T> 接口測試 ====================

    /**
     * 測試 Permission 接口基本功能。
     * 
     * 驗證 Permission 函數式接口能夠正確處理權限檢查，包括允許權限的情況。
     * 
     * 前置條件：
     * - 測試用戶和檔案對象已初始化
     * 
     * 測試步驟：
     * - 創建允許所有操作的權限檢查 Lambda 表達式
     * - 執行權限檢查並驗證結果
     * 
     * 預期結果：
     * - 權限檢查成功完成，無異常拋出
     */
    @Test
    @DisplayName("一般測試 - Permission 接口基本功能")
    void testPermission_basicFunctionality() {
        // 創建一個允許所有操作的權限檢查
        Permission<UserFileMetadata> allowAllPermission = (user, file) -> Mono.empty();

        StepVerifier.create(allowAllPermission.check(testUser, testFile))
                .verifyComplete();
    }

    /**
     * 測試 Permission 接口拒絕權限功能。
     * 
     * 驗證 Permission 函數式接口能夠正確拒絕權限並返回相應的異常。
     * 
     * 前置條件：
     * - 測試用戶和檔案對象已初始化
     * 
     * 測試步驟：
     * - 創建拒絕所有操作的權限檢查 Lambda 表達式
     * - 執行權限檢查並驗證返回的異常類型和錯誤代碼
     * 
     * 預期結果：
     * - 權限檢查返回 ValidationException 異常
     * - 異常錯誤代碼為 FORBIDDEN
     */
    @Test
    @DisplayName("一般測試 - Permission 接口拒絕權限")
    void testPermission_denyPermission() {
        // 創建一個拒絕所有操作的權限檢查
        Permission<UserFileMetadata> denyAllPermission = (user, file) -> 
                Mono.just(new ValidationException(ValidationException.ErrorCode.FORBIDDEN));

        StepVerifier.create(denyAllPermission.check(testUser, testFile))
                .assertNext(throwable -> {
                    assertNotNull(throwable);
                    assertTrue(throwable instanceof ValidationException);
                    ValidationException ex = (ValidationException) throwable;
                    assertEquals(ValidationException.ErrorCode.FORBIDDEN, ex.getErrorCode());
                })
                .verifyComplete();
    }

    /**
     * 測試 Permission 接口條件性權限檢查。
     * 
     * 驗證 Permission 函數式接口能夠根據條件進行權限檢查，包括檔案所有者驗證。
     * 
     * 前置條件：
     * - 測試用戶和檔案對象已初始化
     * 
     * 測試步驟：
     * - 創建基於用戶 ID 的權限檢查 Lambda 表達式
     * - 測試檔案所有者訪問權限
     * - 測試非檔案所有者訪問權限
     * 
     * 預期結果：
     * - 檔案所有者訪問成功
     * - 非檔案所有者訪問被拒絕並返回異常
     */
    @Test
    @DisplayName("一般測試 - Permission 接口條件性權限檢查")
    void testPermission_conditionalCheck() {
        // 創建基於用戶ID的權限檢查
        Permission<UserFileMetadata> ownerOnlyPermission = (user, file) -> {
            if (user.getId().equals(file.getUserId())) {
                return Mono.empty(); // 允許
            } else {
                return Mono.just(new ValidationException(ValidationException.ErrorCode.FORBIDDEN));
            }
        };

        // 測試檔案所有者訪問
        StepVerifier.create(ownerOnlyPermission.check(testUser, testFile))
                .verifyComplete();

        // 測試非檔案所有者訪問
        testFile.setUserId(999L);
        StepVerifier.create(ownerOnlyPermission.check(testUser, testFile))
                .assertNext(throwable -> {
                    assertTrue(throwable instanceof ValidationException);
                })
                .verifyComplete();
    }

    /**
     * 測試 Permission 接口基於角色的權限檢查。
     * 
     * 驗證 Permission 函數式接口能夠根據用戶角色進行權限檢查。
     * 
     * 前置條件：
     * - 測試用戶和檔案對象已初始化
     * 
     * 測試步驟：
     * - 創建僅允許管理員操作的權限檢查 Lambda 表達式
     * - 測試普通用戶權限檢查
     * - 測試管理員用戶權限檢查
     * 
     * 預期結果：
     * - 普通用戶訪問被拒絕
     * - 管理員用戶訪問成功
     */
    @Test
    @DisplayName("一般測試 - Permission 接口基於角色的權限檢查")
    void testPermission_roleBasedCheck() {
        Permission<UserFileMetadata> adminOnlyPermission = (user, file) -> {
            if (RoleEnum.ADMIN.equals(user.getRole())) {
                return Mono.empty();
            } else {
                return Mono.just(new ValidationException(ValidationException.ErrorCode.FORBIDDEN));
            }
        };

        // 測試普通用戶
        StepVerifier.create(adminOnlyPermission.check(testUser, testFile))
                .assertNext(throwable -> {
                    assertTrue(throwable instanceof ValidationException);
                })
                .verifyComplete();

        // 測試管理員用戶
        testUser.setRole(RoleEnum.ADMIN);
        StepVerifier.create(adminOnlyPermission.check(testUser, testFile))
                .verifyComplete();
    }

    /**
     * 測試 Permission 接口泛型類型安全性。
     * 
     * 驗證 Permission 函數式接口的泛型特性，確保類型安全和不同類型的權限檢查。
     * 
     * 前置條件：
     * - 測試用戶對象已初始化
     * 
     * 測試步驟：
     * - 創建 String 類型的權限檢查 Lambda 表達式
     * - 創建 Integer 類型的權限檢查 Lambda 表達式
     * - 測試不同條件下的權限檢查結果
     * 
     * 預期結果：
     * - 符合條件的請求通過權限檢查
     * - 不符合條件的請求被拒絕並返回相應異常
     */
    @Test
    @DisplayName("一般測試 - Permission 接口泛型類型安全")
    void testPermission_genericTypeSafety() {
        // 測試不同泛型類型的權限接口
        Permission<String> stringPermission = (user, str) -> {
            if (str.length() > 10) {
                return Mono.just(new ValidationException(ValidationException.ErrorCode.REQUEST_IS_INVALID, "字符串過長"));
            }
            return Mono.empty();
        };

        Permission<Integer> intPermission = (user, num) -> {
            if (num < 0) {
                return Mono.just(new ValidationException(ValidationException.ErrorCode.REQUEST_IS_INVALID, "數字為負數"));
            }
            return Mono.empty();
        };

        // 測試字符串權限
        StepVerifier.create(stringPermission.check(testUser, "short"))
                .verifyComplete();

        StepVerifier.create(stringPermission.check(testUser, "very long string that exceeds limit"))
                .assertNext(throwable -> {
                    assertTrue(throwable instanceof ValidationException);
                })
                .verifyComplete();

        // 測試整數權限
        StepVerifier.create(intPermission.check(testUser, 42))
                .verifyComplete();

        StepVerifier.create(intPermission.check(testUser, -1))
                .assertNext(throwable -> {
                    assertTrue(throwable instanceof ValidationException);
                })
                .verifyComplete();
    }

    // ==================== Operation 接口測試 ====================

    /**
     * 測試 Operation 接口基本功能。
     * 
     * 驗證 Operation 函數式接口能夠正確執行基本操作。
     * 
     * 前置條件：
     * - 無
     * 
     * 測試步驟：
     * - 創建簡單的操作 Lambda 表達式
     * - 執行操作並驗證執行狀態
     * 
     * 預期結果：
     * - 操作正常執行且無異常拋出
     * - 操作執行狀態正確更新
     */
    @Test
    @DisplayName("一般測試 - Operation 接口基本功能")
    void testOperation_basicFunctionality() {
        AtomicBoolean executed = new AtomicBoolean(false);

        Operation operation = () -> executed.set(true);

        assertDoesNotThrow(() -> operation.execute());
        assertTrue(executed.get());
    }

    /**
     * 測試 Operation 接口異常處理。
     * 
     * 驗證 Operation 函數式接口能夠正確處理執行過程中的異常。
     * 
     * 前置條件：
     * - 無
     * 
     * 測試步驟：
     * - 創建會拋出異常的操作 Lambda 表達式
     * - 執行操作並捕獲異常
     * - 驗證異常類型和訊息
     * 
     * 預期結果：
     * - 正確拋出 RuntimeException
     * - 異常訊息符合預期
     */
    @Test
    @DisplayName("一般測試 - Operation 接口異常處理")
    void testOperation_exceptionHandling() {
        Operation throwingOperation = () -> {
            throw new RuntimeException("Operation failed");
        };

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            throwingOperation.execute();
        });

        assertEquals("Operation failed", exception.getMessage());
    }

    /**
     * 測試 Operation 接口多步驟操作。
     * 
     * 驗證 Operation 函數式接口能夠執行複雜的多步驟操作。
     * 
     * 前置條件：
     * - 無
     * 
     * 測試步驟：
     * - 創建包含多個步驟的操作 Lambda 表達式
     * - 執行操作並驗證每個步驟的執行結果
     * 
     * 預期結果：
     * - 所有步驟正確執行
     * - 最終結果符合預期
     */
    @Test
    @DisplayName("一般測試 - Operation 接口多步驟操作")
    void testOperation_multiStepOperation() {
        AtomicInteger counter = new AtomicInteger(0);
        AtomicReference<String> result = new AtomicReference<>();

        Operation complexOperation = () -> {
            counter.incrementAndGet();
            counter.incrementAndGet();
            result.set("Operation completed with count: " + counter.get());
        };

        assertDoesNotThrow(() -> complexOperation.execute());
        assertEquals(2, counter.get());
        assertEquals("Operation completed with count: 2", result.get());
    }

    /**
     * 測試 Operation 接口方法引用。
     * 
     * 驗證 Operation 函數式接口能夠使用方法引用形式執行操作。
     * 
     * 前置條件：
     * - 測試類中存在可引用的方法
     * 
     * 測試步驟：
     * - 創建使用方法引用的操作
     * - 執行操作並驗證方法是否被正確調用
     * 
     * 預期結果：
     * - 操作正常執行
     * - 被引用的方法正確執行
     */
    @Test
    @DisplayName("一般測試 - Operation 接口方法引用")
    void testOperation_methodReference() {
        AtomicBoolean methodCalled = new AtomicBoolean(false);

        // 使用方法引用
        Operation methodRefOperation = () -> this.testMethod(methodCalled);

        assertDoesNotThrow(() -> methodRefOperation.execute());
        assertTrue(methodCalled.get());
    }


    /**
     * 測試用的輔助方法。
     * 
     * 用於測試方法引用功能的輔助方法。
     * 
     * @param flag 用於標記方法是否被調用的原子布爾值
     */
    private void testMethod(AtomicBoolean flag) {
        flag.set(true);
    }

    // ==================== CacheRule<T> 接口測試 ====================


    /**
     * 測試 Operation 接口鏈式執行。
     * 
     * 驗證 Operation 函數式接口能夠支持多個操作的鏈式執行。
     * 
     * 前置條件：
     * - 無
     * 
     * 測試步驟：
     * - 創建多個相關的操作 Lambda 表達式
     * - 依序執行多個操作
     * - 驗證最終執行狀態
     * 
     * 預期結果：
     * - 所有操作依序正確執行
     * - 最終狀態反映所有操作的執行結果
     */
    @Test
    @DisplayName("一般測試 - Operation 接口鏈式執行")
    void testOperation_chainedExecution() {
        AtomicInteger step = new AtomicInteger(0);

        Operation step1 = () -> step.set(1);
        Operation step2 = () -> step.set(2);
        Operation step3 = () -> step.set(3);

        // 鏈式執行多個操作
        assertDoesNotThrow(() -> {
            step1.execute();
            step2.execute();
            step3.execute();
        });

        assertEquals(3, step.get());
    }


    /**
     * 測試 CacheRule 接口基本功能。
     * 
     * 驗證 CacheRule 函數式接口能夠正確處理緩存規則的基本操作。
     * 
     * 前置條件：
     * - 無
     * 
     * 測試步驟：
     * - 創建基本的緩存規則 Lambda 表達式
     * - 執行緩存操作並驗證參數傳遞
     * 
     * 預期結果：
     * - 緩存操作正常執行
     * - 緩存值和過期時間正確傳遞
     */
    @Test
    @DisplayName("一般測試 - CacheRule 接口基本功能")
    void testCacheRule_basicFunctionality() {
        AtomicReference<String> cachedValue = new AtomicReference<>();
        AtomicReference<Duration> cachedDuration = new AtomicReference<>();

        CacheRule<String> stringCacheRule = (value, expire) -> {
            cachedValue.set(value);
            cachedDuration.set(expire);
            return Mono.empty();
        };

        Duration testDuration = Duration.ofMinutes(30);
        String testValue = "cached data";

        StepVerifier.create(stringCacheRule.apply(testValue, testDuration))
                .verifyComplete();

        assertEquals(testValue, cachedValue.get());
        assertEquals(testDuration, cachedDuration.get());
    }


    /**
     * 測試 CacheRule 接口不同類型緩存。
     * 
     * 驗證 CacheRule 函數式接口的泛型特性，支持不同類型的緩存操作。
     * 
     * 前置條件：
     * - 測試檔案對象已初始化
     * 
     * 測試步驟：
     * - 創建檔案類型的緩存規則 Lambda 表達式
     * - 創建數字類型的緩存規則 Lambda 表達式
     * - 分別執行不同類型的緩存操作
     * 
     * 預期結果：
     * - 不同類型的緩存操作都能正確執行
     * - 類型安全得到保證
     */
    @Test
    @DisplayName("一般測試 - CacheRule 接口不同類型緩存")
    void testCacheRule_differentTypes() {
        // 測試對象緩存
        AtomicReference<UserFileMetadata> cachedFile = new AtomicReference<>();
        CacheRule<UserFileMetadata> fileCacheRule = (file, expire) -> {
            cachedFile.set(file);
            return Mono.empty();
        };

        StepVerifier.create(fileCacheRule.apply(testFile, Duration.ofHours(1)))
                .verifyComplete();

        assertEquals(testFile, cachedFile.get());

        // 測試數字緩存
        AtomicReference<Integer> cachedNumber = new AtomicReference<>();
        CacheRule<Integer> numberCacheRule = (number, expire) -> {
            cachedNumber.set(number);
            return Mono.empty();
        };

        StepVerifier.create(numberCacheRule.apply(42, Duration.ofMinutes(15)))
                .verifyComplete();

        assertEquals(42, cachedNumber.get());
    }


    /**
     * 測試 CacheRule 接口緩存過期處理。
     * 
     * 驗證 CacheRule 函數式接口能夠正確處理緩存過期時間的邊界情況。
     * 
     * 前置條件：
     * - 無
     * 
     * 測試步驟：
     * - 創建具有過期時間驗證的緩存規則
     * - 測試有效過期時間的處理
     * - 測試無效過期時間的異常處理
     * 
     * 預期結果：
     * - 有效過期時間正常處理
     * - 無效過期時間拋出相應異常
     */
    @Test
    @DisplayName("一般測試 - CacheRule 接口緩存過期處理")
    void testCacheRule_expirationHandling() {
        CacheRule<String> expirationAwareCacheRule = (value, expire) -> {
            if (expire.isZero() || expire.isNegative()) {
                return Mono.error(new IllegalArgumentException("Invalid expiration time"));
            }
            return Mono.empty();
        };

        // 測試有效過期時間
        StepVerifier.create(expirationAwareCacheRule.apply("valid", Duration.ofMinutes(30)))
                .verifyComplete();

        // 測試零過期時間
        StepVerifier.create(expirationAwareCacheRule.apply("invalid", Duration.ZERO))
                .expectError(IllegalArgumentException.class)
                .verify();

        // 測試負數過期時間
        StepVerifier.create(expirationAwareCacheRule.apply("invalid", Duration.ofMinutes(-1)))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    // ==================== 異常測試 ====================


    /**
     * 測試 CacheRule 接口響應式緩存操作。
     * 
     * 驗證 CacheRule 函數式接口能夠與響應式編程模式配合使用。
     * 
     * 前置條件：
     * - 無
     * 
     * 測試步驟：
     * - 創建響應式的緩存規則 Lambda 表達式
     * - 執行響應式緩存操作並驗證執行次數
     * 
     * 預期結果：
     * - 響應式緩存操作正確執行
     * - 操作計數器正確更新
     */
    @Test
    @DisplayName("一般測試 - CacheRule 接口響應式緩存操作")
    void testCacheRule_reactiveCaching() {
        AtomicInteger cacheOperationCount = new AtomicInteger(0);

        CacheRule<String> reactiveCacheRule = (value, expire) -> {
            return Mono.fromCallable(() -> {
                cacheOperationCount.incrementAndGet();
                return value + " cached";
            }).then();
        };

        StepVerifier.create(reactiveCacheRule.apply("data", Duration.ofMinutes(30)))
                .verifyComplete();

        assertEquals(1, cacheOperationCount.get());
    }


    /**
     * 測試 Permission 接口空值處理。
     * 
     * 驗證 Permission 函數式接口能夠正確處理空值參數的異常情況。
     * 
     * 前置條件：
     * - 測試用戶和檔案對象已初始化
     * 
     * 測試步驟：
     * - 創建具有空值檢查的權限檢查 Lambda 表達式
     * - 測試空用戶的情況
     * - 測試空檔案的情況
     * - 測試雙重空值的情況
     * 
     * 預期結果：
     * - 所有空值情況都正確拋出 ValidationException
     * - 異常訊息正確包含空值錯誤信息
     */
    @Test
    @DisplayName("異常測試 - Permission 接口空值處理")
    void testPermission_nullHandling() {
        Permission<UserFileMetadata> nullSafePermission = (user, file) -> {
            if (user == null || file == null) {
                return Mono.just(new ValidationException(ValidationException.ErrorCode.REQUEST_IS_INVALID, "用戶或檔案為空"));
            }
            return Mono.empty();
        };

        // 測試空用戶
        StepVerifier.create(nullSafePermission.check(null, testFile))
                .assertNext(throwable -> {
                    assertTrue(throwable instanceof ValidationException);
                })
                .verifyComplete();

        // 測試空檔案
        StepVerifier.create(nullSafePermission.check(testUser, null))
                .assertNext(throwable -> {
                    assertTrue(throwable instanceof ValidationException);
                })
                .verifyComplete();

        // 測試都為空
        StepVerifier.create(nullSafePermission.check(null, null))
                .assertNext(throwable -> {
                    assertTrue(throwable instanceof ValidationException);
                })
                .verifyComplete();
    }


    /**
     * 測試 Operation 接口檢查異常處理。
     * 
     * 驗證 Operation 函數式接口能夠正確處理檢查異常（Checked Exception）。
     * 
     * 前置條件：
     * - 無
     * 
     * 測試步驟：
     * - 創建會拋出檢查異常的操作 Lambda 表達式
     * - 執行操作並捕獲檢查異常
     * - 驗證異常類型和訊息
     * 
     * 預期結果：
     * - 正確拋出檢查異常
     * - 異常訊息符合預期
     */
    @Test
    @DisplayName("異常測試 - Operation 接口檢查異常")
    void testOperation_checkedException() {
        Operation checkedExceptionOperation = () -> {
            throw new Exception("Checked exception occurred");
        };

        Exception exception = assertThrows(Exception.class, () -> {
            checkedExceptionOperation.execute();
        });

        assertEquals("Checked exception occurred", exception.getMessage());
    }

    // ==================== 邊界測試 ====================


    /**
     * 測試 CacheRule 接口錯誤處理。
     * 
     * 驗證 CacheRule 函數式接口能夠正確處理緩存操作中的錯誤情況。
     * 
     * 前置條件：
     * - 無
     * 
     * 測試步驟：
     * - 創建具有錯誤處理邏輯的緩存規則
     * - 測試正常緩存操作
     * - 測試錯誤情況下的異常處理
     * 
     * 預期結果：
     * - 正常情況下緩存操作成功
     * - 錯誤情況下正確拋出 RuntimeException
     */
    @Test
    @DisplayName("異常測試 - CacheRule 接口錯誤處理")
    void testCacheRule_errorHandling() {
        CacheRule<String> errorCacheRule = (value, expire) -> {
            if ("error".equals(value)) {
                return Mono.error(new RuntimeException("Cache operation failed"));
            }
            return Mono.empty();
        };

        // 測試正常情況
        StepVerifier.create(errorCacheRule.apply("normal", Duration.ofMinutes(30)))
                .verifyComplete();

        // 測試錯誤情況
        StepVerifier.create(errorCacheRule.apply("error", Duration.ofMinutes(30)))
                .expectError(RuntimeException.class)
                .verify();
    }


    /**
     * 測試函數式接口組合。
     * 
     * 驗證多個函數式接口能夠組合使用，實現複雜的業務邏輯。
     * 
     * 前置條件：
     * - 測試用戶和檔案對象已初始化
     * 
     * 測試步驟：
     * - 創建多個獨立的權限檢查 Lambda 表達式
     * - 組合多個權限檢查為綜合權限檢查
     * - 測試組合權限檢查的各種情況
     * 
     * 預期結果：
     * - 組合權限檢查正確執行
     * - 不同條件下的權限檢查結果符合預期
     */
    @Test
    @DisplayName("邊界測試 - 函數式接口組合")
    void testFunctionalInterface_composition() {
        // 組合多個權限檢查
        Permission<UserFileMetadata> ownerCheck = (user, file) ->
                user.getId().equals(file.getUserId()) ?
                Mono.empty() :
                Mono.just(new ValidationException(ValidationException.ErrorCode.FORBIDDEN));

        Permission<UserFileMetadata> roleCheck = (user, file) ->
                RoleEnum.USER.equals(user.getRole()) || RoleEnum.ADMIN.equals(user.getRole()) ?
                Mono.empty() :
                Mono.just(new ValidationException(ValidationException.ErrorCode.FORBIDDEN));

        // 組合權限檢查
        Permission<UserFileMetadata> combinedPermission = (user, file) ->
                ownerCheck.check(user, file)
                    .switchIfEmpty(roleCheck.check(user, file));

        // 測試通過的情況
        StepVerifier.create(combinedPermission.check(testUser, testFile))
                .verifyComplete();

        // 測試失敗的情況
        testFile.setUserId(999L);
        testUser.setRole(RoleEnum.VISITOR);
        StepVerifier.create(combinedPermission.check(testUser, testFile))
                .assertNext(throwable -> {
                    assertTrue(throwable instanceof ValidationException);
                })
                .verifyComplete();
    }


    /**
     * 測試函數式接口極值處理。
     * 
     * 驗證函數式接口能夠正確處理極端值和邊界情況。
     * 
     * 前置條件：
     * - 無
     * 
     * 測試步驟：
     * - 創建具有極值檢查的緩存規則
     * - 測試正常範圍內的極值
     * - 測試超出範圍的極值
     * 
     * 預期結果：
     * - 正常範圍內的極值正確處理
     * - 超出範圍的極值拋出相應異常
     */
    @Test
    @DisplayName("邊界測試 - 極值處理")
    void testFunctionalInterface_extremeValues() {
        // 測試極大過期時間
        CacheRule<String> extremeCacheRule = (value, expire) -> {
            if (expire.toDays() > 365) {
                return Mono.error(new IllegalArgumentException("Expiration too long"));
            }
            return Mono.empty();
        };

        StepVerifier.create(extremeCacheRule.apply("data", Duration.ofDays(100)))
                .verifyComplete();

        StepVerifier.create(extremeCacheRule.apply("data", Duration.ofDays(400)))
                .expectError(IllegalArgumentException.class)
                .verify();
    }


    /**
     * 測試函數式接口併發操作。
     * 
     * 驗證函數式接口在併發環境下的線程安全性和正確性。
     * 
     * 前置條件：
     * - 無
     * 
     * 測試步驟：
     * - 創建併發安全的操作 Lambda 表達式
     * - 使用 CompletableFuture 並發執行多個操作
     * - 驗證併發執行的正確性
     * 
     * 預期結果：
     * - 所有併發操作都正確執行
     * - 操作計數器正確反映執行次數
     */
    @Test
    @DisplayName("邊界測試 - 併發操作")
    void testFunctionalInterface_concurrentOperations() {
        AtomicInteger operationCount = new AtomicInteger(0);

        Operation concurrentOperation = () -> operationCount.incrementAndGet();

        // 併發執行多個操作
        assertDoesNotThrow(() -> {
            java.util.concurrent.CompletableFuture[] futures = new java.util.concurrent.CompletableFuture[10];
            for (int i = 0; i < 10; i++) {
                futures[i] = java.util.concurrent.CompletableFuture.runAsync(() -> {
                    try {
                        concurrentOperation.execute();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            }
            java.util.concurrent.CompletableFuture.allOf(futures).join();
        });

        assertEquals(10, operationCount.get());
    }

    // ==================== 輔助方法 ====================


    /**
     * 測試 Lambda 表達式複雜性。
     * 
     * 驗證複雜的 Lambda 表達式在函數式接口中的正確執行，包括多層邏輯判斷。
     * 
     * 前置條件：
     * - 測試用戶和檔案對象已初始化
     * 
     * 測試步驟：
     * - 創建包含複雜邏輯的權限檢查 Lambda 表達式
     * - 測試檔案所有者的權限檢查
     * - 測試管理員的權限檢查
     * - 測試高級用戶對共享檔案的權限檢查
     * - 測試普通用戶對他人檔案的權限檢查
     * 
     * 預期結果：
     * - 複雜邏輯判斷正確執行
     * - 不同用戶角色和檔案類型的權限檢查結果符合預期
     */
    @Test
    @DisplayName("邊界測試 - Lambda 表達式複雜性")
    void testFunctionalInterface_complexLambdas() {
        // 複雜的權限檢查邏輯
        Permission<UserFileMetadata> complexPermission = (user, file) -> {
            return Mono.fromCallable(() -> {
                // 模擬複雜的權限計算
                boolean hasPermission = user.getId().equals(file.getUserId())
                    || RoleEnum.ADMIN.equals(user.getRole())
                    || (RoleEnum.ADVANCED_USER.equals(user.getRole()) && file.getFilename().startsWith("shared_"));

                if (!hasPermission) {
                    return new ValidationException(ValidationException.ErrorCode.FORBIDDEN);
                }
                return null;
            }).flatMap(error -> error != null ? Mono.just(error) : Mono.empty());
        };

        // 測試檔案所有者
        StepVerifier.create(complexPermission.check(testUser, testFile))
                .verifyComplete();

        // 測試管理員
        User admin = new User();
        admin.setId(999L);
        admin.setRole(RoleEnum.ADMIN);
        StepVerifier.create(complexPermission.check(admin, testFile))
                .verifyComplete();

        // 測試高級用戶訪問共享檔案
        User advancedUser = new User();
        advancedUser.setId(888L);
        advancedUser.setRole(RoleEnum.ADVANCED_USER);
        testFile.setFilename("shared_document.txt");
        testFile.setUserId(777L);

        StepVerifier.create(complexPermission.check(advancedUser, testFile))
                .verifyComplete();

        // 測試普通用戶訪問他人檔案
        testFile.setFilename("private_document.txt");
        StepVerifier.create(complexPermission.check(testUser, testFile))
                .assertNext(throwable -> {
                    assertTrue(throwable instanceof ValidationException);
                })
                .verifyComplete();
    }
}