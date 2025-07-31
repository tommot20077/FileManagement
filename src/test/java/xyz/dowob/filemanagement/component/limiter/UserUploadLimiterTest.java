package xyz.dowob.filemanagement.component.limiter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;
import xyz.dowob.filemanagement.config.properties.FileProperties;

import static org.mockito.Mockito.when;

/**
 * UserUploadLimiter 用戶上傳限流器測試類別。
 * 
 * 測試 UserUploadLimiter 的用戶上傳限流功能，包括許可獲取、釋放和配置處理。
 * 
 * 前置條件：
 * - 初始化 Mock 依賴項目
 * - 設置用戶上傳限制配置
 * - 確保信號量機制正常運作
 * 
 * 測試步驟：
 * - 測試新用戶和現有用戶的許可獲取
 * - 測試達到限制後的拒絕機制
 * - 測試許可釋放和映射清理
 * - 測試不同限制配置下的行為
 * 
 * 預期結果：
 * - 限流器應按配置正確控制用戶上傳許可
 * - 超過限制的請求應被拒絕
 * - 許可釋放應正確更新可用許可數
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserUploadLimiter 邏輯處理測試")
class UserUploadLimiterTest {

    private final int DEFAULT_MAX_UPLOADS = 3;

    private final Long USER_ID_1 = 1L;

    private final Long USER_ID_2 = 2L;

    @Mock
    private FileProperties mockFileProperties;

    @Mock
    private FileProperties.Upload mockUploadProperties;

    private UserUploadLimiter userUploadLimiterUnderTest;


    @BeforeEach
    void setUp() {
        setupUserUploadLimiter(DEFAULT_MAX_UPLOADS);
    }

    private void setupUserUploadLimiter(int maxUploads) {
        when(mockFileProperties.getUpload()).thenReturn(mockUploadProperties);
        when(mockUploadProperties.getMaxUploadTaskLimit()).thenReturn(maxUploads);
        userUploadLimiterUnderTest = new UserUploadLimiter(mockFileProperties);
    }

    /**
     * 測試新用戶首次獲取上傳許可的功能。
     *
     * 測試步驟：
     * - 使用新用戶 ID 嘗試獲取許可
     * - 驗證返回結果為成功
     *
     * 預期結果：新用戶應成功獲取許可
     */
    @Test
    @DisplayName("新用戶首次獲取許可 - 成功獲取")
    void tryAcquire_newUser_acquireSuccessfully() {
        StepVerifier.create(userUploadLimiterUnderTest.tryAcquire(USER_ID_1))
                .expectNext(true)
                .verifyComplete();
    }

    /**
     * 測試現有用戶在限制範圍內多次獲取許可的功能。
     *
     * 測試步驟：
     * - 在最大上傳限制內多次獲取許可
     * - 驗證每次獲取都返回成功
     *
     * 預期結果：在限制內的所有許可獲取都應成功
     */
    @Test
    @DisplayName("現有用戶在限制內獲取許可 - 成功獲取")
    void tryAcquire_existingUser_acquireSuccessfullyWithinLimit() {
        for (int i = 0; i < DEFAULT_MAX_UPLOADS; i++) {
            StepVerifier.create(userUploadLimiterUnderTest.tryAcquire(USER_ID_1))
                    .expectNext(true)
                    .verifyComplete();
        }
    }

    /**
     * 測試用戶達到最大上傳限制後獲取許可被拒絕的功能。
     *
     * 測試步驟：
     * - 先獲取最大數量的許可
     * - 嘗試獲取額外的許可
     * - 驗證額外許可獲取失敗
     *
     * 預期結果：超過限制的許可獲取應失敗
     */
    @Test
    @DisplayName("現有用戶達到限制後獲取許可 - 獲取失敗")
    void tryAcquire_existingUser_acquireFailsWhenLimitReached() {
        for (int i = 0; i < DEFAULT_MAX_UPLOADS; i++) {
            userUploadLimiterUnderTest.tryAcquire(USER_ID_1).block();
        }

        StepVerifier.create(userUploadLimiterUnderTest.tryAcquire(USER_ID_1))
                .expectNext(false)
                .verifyComplete();
    }

    /**
     * 測試多個用戶的上傳限制相互獨立的機制。
     *
     * 測試步驟：
     * - 讓第一個用戶達到上傳限制
     * - 讓第二個用戶獲取許可
     * - 驗證兩個用戶的限制互不影響
     *
     * 預期結果：不同用戶的上傳限制應相互獨立
     */
    @Test
    @DisplayName("多個用戶獨立獲取許可 - 互不影響")
    void tryAcquire_multipleUsers_independentLimits() {
        for (int i = 0; i < DEFAULT_MAX_UPLOADS; i++) {
            StepVerifier.create(userUploadLimiterUnderTest.tryAcquire(USER_ID_1))
                    .expectNext(true)
                    .verifyComplete();
        }
        StepVerifier.create(userUploadLimiterUnderTest.tryAcquire(USER_ID_1))
                .expectNext(false)
                .verifyComplete();

        for (int i = 0; i < DEFAULT_MAX_UPLOADS; i++) {
            StepVerifier.create(userUploadLimiterUnderTest.tryAcquire(USER_ID_2))
                    .expectNext(true)
                    .verifyComplete();
        }
        StepVerifier.create(userUploadLimiterUnderTest.tryAcquire(USER_ID_2))
                .expectNext(false)
                .verifyComplete();
    }

    /**
     * 測試上傳限制為 1 時的許可獲取行為。
     *
     * 測試步驟：
     * - 設置上傳限制為 1
     * - 首次獲取許可並驗證成功
     * - 第二次獲取許可並驗證失敗
     *
     * 預期結果：限制為 1 時只能獲取一個許可
     */
    @Test
    @DisplayName("上傳限制為1 - 首次獲取成功，第二次失敗")
    void tryAcquire_limitIsOne_acquireOnceThenFail() {
        setupUserUploadLimiter(1);

        StepVerifier.create(userUploadLimiterUnderTest.tryAcquire(USER_ID_1))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(userUploadLimiterUnderTest.tryAcquire(USER_ID_1))
                .expectNext(false)
                .verifyComplete();
    }

    /**
     * 測試上傳限制為 0 時的許可獲取行為。
     *
     * 測試步驟：
     * - 設置上傳限制為 0
     * - 嘗試獲取許可
     * - 驗證獲取失敗
     *
     * 預期結果：限制為 0 時所有許可獲取都應失敗
     */
    @Test
    @DisplayName("上傳限制為0 - 總是獲取失敗")
    void tryAcquire_limitIsZero_alwaysFail() {
        setupUserUploadLimiter(0);

        StepVerifier.create(userUploadLimiterUnderTest.tryAcquire(USER_ID_1))
                .expectNext(false)
                .verifyComplete();
    }

    /**
     * 測試釋放已獲取許可的功能。
     *
     * 測試步驟：
     * - 用戶獲取許可
     * - 釋放該許可
     * - 驗證釋放操作成功
     * - 驗證可以重新獲取許可
     *
     * 預期結果：許可釋放後應可重新獲取
     */
    @Test
    @DisplayName("釋放已獲取許可的用戶 - 許可成功釋放且後續可再獲取")
    void release_existingUserWithAcquiredPermit_permitReleasedAndCanReacquire() {
        userUploadLimiterUnderTest.tryAcquire(USER_ID_1).block();

        StepVerifier.create(userUploadLimiterUnderTest.release(USER_ID_1))
                .verifyComplete();

        StepVerifier.create(userUploadLimiterUnderTest.tryAcquire(USER_ID_1))
                .expectNext(true)
                .verifyComplete();
    }

    /**
     * 測試釋放最後一個許可時信號量清理機制（限制為 1）。
     *
     * 測試步驟：
     * - 設置限制為 1
     * - 獲取唯一許可
     * - 釋放該許可
     * - 驗證信號量從映射中移除
     * - 驗證後續獲取行為如新用戶
     *
     * 預期結果：最後一個許可釋放後信號量應被清理
     */
    @Test
    @DisplayName("釋放最後一個許可(限制為1) - 信號量從映射中移除且後續獲取如新")
    void release_lastPermit_semaphoreRemovedFromMap_whenLimitIsOne() {
        setupUserUploadLimiter(1);

        userUploadLimiterUnderTest.tryAcquire(USER_ID_1).block();

        StepVerifier.create(userUploadLimiterUnderTest.release(USER_ID_1))
                .verifyComplete();
        StepVerifier.create(userUploadLimiterUnderTest.tryAcquire(USER_ID_1))
                .expectNext(true)
                .verifyComplete();
        StepVerifier.create(userUploadLimiterUnderTest.tryAcquire(USER_ID_1))
                .expectNext(false)
                .verifyComplete();
    }
    
    /**
     * 測試釋放所有許可時信號量清理機制（限制大於 1）。
     *
     * 測試步驟：
     * - 獲取所有可用許可
     * - 逐一釋放所有許可
     * - 驗證信號量從映射中移除
     * - 驗證後續獲取行為如新用戶
     *
     * 預期結果：所有許可釋放後信號量應被清理
     */
    @Test
    @DisplayName("釋放所有許可(限制大於1) - 信號量從映射中移除且後續獲取如新")
    void release_lastPermit_semaphoreRemovedFromMap_whenLimitIsMultiple() {
        setupUserUploadLimiter(DEFAULT_MAX_UPLOADS);

        for (int i = 0; i < DEFAULT_MAX_UPLOADS; i++) {
            userUploadLimiterUnderTest.tryAcquire(USER_ID_1).block();
        }

        for (int i = 0; i < DEFAULT_MAX_UPLOADS; i++) {
            StepVerifier.create(userUploadLimiterUnderTest.release(USER_ID_1)).verifyComplete();
        }

        for (int i = 0; i < DEFAULT_MAX_UPLOADS; i++) {
             StepVerifier.create(userUploadLimiterUnderTest.tryAcquire(USER_ID_1))
                .expectNext(true)
                .verifyComplete();
        }
        StepVerifier.create(userUploadLimiterUnderTest.tryAcquire(USER_ID_1))
                .expectNext(false)
                .verifyComplete();
    }


    /**
     * 測試釋放非最後一個許可時信號量保留機制。
     *
     * 測試步驟：
     * - 設置限制為 2
     * - 獲取兩個許可
     * - 釋放其中一個許可
     * - 驗證信號量仍在映射中
     * - 驗證可再獲取一個許可但無法獲取第三個
     *
     * 預期結果：非最後許可釋放時信號量應保留
     */
    @Test
    @DisplayName("釋放許可（限制大於1，非最後一個） - 信號量未從映射中移除")
    void release_notLastPermit_semaphoreNotRemovedFromMap() {
        setupUserUploadLimiter(2);
        userUploadLimiterUnderTest.tryAcquire(USER_ID_1).block();
        userUploadLimiterUnderTest.tryAcquire(USER_ID_1).block();

        StepVerifier.create(userUploadLimiterUnderTest.release(USER_ID_1))
                .verifyComplete();

        StepVerifier.create(userUploadLimiterUnderTest.tryAcquire(USER_ID_1))
                .expectNext(true)
                .verifyComplete();
        StepVerifier.create(userUploadLimiterUnderTest.tryAcquire(USER_ID_1))
                .expectNext(false)
                .verifyComplete();
    }

    /**
     * 測試釋放不存在用戶許可的容錯機制。
     *
     * 測試步驟：
     * - 使用不存在的用戶 ID 釋放許可
     * - 驗證操作正常完成且無錯誤
     *
     * 預期結果：釋放不存在用戶的許可應無副作用
     */
    @Test
    @DisplayName("釋放不存在用戶的許可 - 無錯誤")
    void release_nonExistingUser_noError() {
        Long nonExistingUserId = 99L;
        StepVerifier.create(userUploadLimiterUnderTest.release(nonExistingUserId))
                .verifyComplete();
    }

    /**
     * 測試銷毀方法清除所有用戶信號量映射的功能。
     *
     * 測試步驟：
     * - 用戶獲取所有可用許可
     * - 驗證無法再獲取許可
     * - 調用 destroy 方法
     * - 驗證後續獲取行為如新用戶
     *
     * 預期結果：銷毀後所有用戶狀態應重置
     */
    @Test
    @DisplayName("調用 destroy 方法 - 清除用戶信號量映射且後續獲取行為如新用戶")
    void destroy_clearsMapAndSubsequentAcquireBehavesAsNew() {
        for (int i = 0; i < DEFAULT_MAX_UPLOADS; i++) {
            userUploadLimiterUnderTest.tryAcquire(USER_ID_1).block();
        }

        StepVerifier.create(userUploadLimiterUnderTest.tryAcquire(USER_ID_1))
                .expectNext(false)
                .verifyComplete();

        userUploadLimiterUnderTest.destroy();

        for (int i = 0; i < DEFAULT_MAX_UPLOADS; i++) {
            StepVerifier.create(userUploadLimiterUnderTest.tryAcquire(USER_ID_1))
                    .expectNext(true)
                    .verifyComplete();
        }
        StepVerifier.create(userUploadLimiterUnderTest.tryAcquire(USER_ID_1))
                .expectNext(false)
                .verifyComplete();
    }
}
