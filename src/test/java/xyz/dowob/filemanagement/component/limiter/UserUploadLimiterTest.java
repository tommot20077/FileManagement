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

    @Test
    @DisplayName("新用戶首次獲取許可 - 成功獲取")
    void tryAcquire_newUser_acquireSuccessfully() {
        StepVerifier.create(userUploadLimiterUnderTest.tryAcquire(USER_ID_1))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    @DisplayName("現有用戶在限制內獲取許可 - 成功獲取")
    void tryAcquire_existingUser_acquireSuccessfullyWithinLimit() {
        for (int i = 0; i < DEFAULT_MAX_UPLOADS; i++) {
            StepVerifier.create(userUploadLimiterUnderTest.tryAcquire(USER_ID_1))
                    .expectNext(true)
                    .verifyComplete();
        }
    }

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

    @Test
    @DisplayName("上傳限制為0 - 總是獲取失敗")
    void tryAcquire_limitIsZero_alwaysFail() {
        setupUserUploadLimiter(0);

        StepVerifier.create(userUploadLimiterUnderTest.tryAcquire(USER_ID_1))
                .expectNext(false)
                .verifyComplete();
    }

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

    @Test
    @DisplayName("釋放不存在用戶的許可 - 無錯誤")
    void release_nonExistingUser_noError() {
        Long nonExistingUserId = 99L;
        StepVerifier.create(userUploadLimiterUnderTest.release(nonExistingUserId))
                .verifyComplete();
    }

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
