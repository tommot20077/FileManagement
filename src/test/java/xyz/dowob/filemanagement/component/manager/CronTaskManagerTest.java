package xyz.dowob.filemanagement.component.manager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.component.provider.providerImplement.JwtTokenProviderImpl;
import xyz.dowob.filemanagement.component.strategy.CsrfTokenRepositoryStrategy;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.customenum.TransfersStatusEnum;
import xyz.dowob.filemanagement.entity.*;
import xyz.dowob.filemanagement.repostiory.*;
import xyz.dowob.filemanagement.repostiory.ServerCsrfToken.CustomServerCsrfTokenRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CronTaskManager 定時任務管理測試類別。
 *
 * 測試 CronTaskManager 的定時任務處理機制，包括 JWT 憑證清理、用戶儲存使用量更新、
 * 檔案傳輸任務清理、過期檔案處理和 CSRF 憑證清理等功能。
 *
 * 前置條件：
 * - 初始化所有必要的 Mock 依賴項目
 * - 設定測試資料和環境
 * - 確保定時任務處理邏輯的正確性
 *
 * 測試步驟：
 * - 測試 JWT 快取憑證的過期清理
 * - 測試用戶儲存使用量的計算和更新
 * - 測試過期檔案傳輸任務的清理
 * - 測試過期檔案的刪除處理
 * - 測試 CSRF 憑證的過期清理
 *
 * 預期結果：
 * - 所有定時任務應按預期執行
 * - 過期項目應被正確識別和清理
 * - 資料庫狀態應正確更新
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CronTaskManager 排程任務邏輯處理測試")
class CronTaskManagerTest {

    @Mock
    private JwtTokenProviderImpl mockJwtTokenProvider;

    @Mock
    private UserRepository mockUserRepository;

    @Mock
    private UserFileMetaRepository mockUserFileMetaRepository;

    @Mock
    private ServerFileMetaRepository mockServerFileMetaRepository;

    @Mock
    private FileTrashRecordRepository mockFileTrashRecordRepository;

    @Mock
    private TransactionalOperator mockTransactionalOperator;

    @Mock
    private CsrfTokenRepositoryStrategy mockCsrfTokenRepositoryStrategy;

    @Mock
    private TransfersTasksManager mockTransfersTasksManager;

    @Mock
    private TransfersTasksRepository mockTransfersTasksRepository;

    @Mock
    private FileProperties mockFileProperties;

    @Mock
    private FileProperties.Upload mockUpload;

    @Mock
    private CustomServerCsrfTokenRepository mockServerCsrfTokenRepository;

    private CronTaskManager cronTaskManagerUnderTest;


    @BeforeEach
    void setUp() {
        cronTaskManagerUnderTest = new CronTaskManager(mockJwtTokenProvider,
                                                       mockUserRepository,
                                                       mockUserFileMetaRepository,
                                                       mockServerFileMetaRepository,
                                                       mockFileTrashRecordRepository,
                                                       mockTransactionalOperator,
                                                       mockCsrfTokenRepositoryStrategy,
                                                       mockTransfersTasksManager,
                                                       mockTransfersTasksRepository,
                                                       mockFileProperties
        );
    }


    /**
     * 測試清理過期 JWT 快取憑證的功能。
     *
     * 測試步驟：
     * - 建立過期和有效的 JWT 憑證
     * - 將憑證加入快取映射
     * - 執行過期憑證清理
     * - 驗證過期憑證被移除而有效憑證保留
     *
     * 預期結果：過期憑證應被清理，有效憑證應保留
     */
    @Test
    @DisplayName("清理過期的 JWT 緩存憑證 - 成功移除過期的憑證")
    void deleteExpiredToken_ExpiredTokens_SuccessfullyRemoved() {
        ConcurrentHashMap<String, JwtTokenProviderImpl.TokenCacheEntity> tokenMap = new ConcurrentHashMap<>();
        JwtTokenProviderImpl.TokenCacheEntity expiredToken = new JwtTokenProviderImpl.TokenCacheEntity("v1",
                                                                                                       1L,
                                                                                                       new Date(System.currentTimeMillis() - 1000)
        );
        JwtTokenProviderImpl.TokenCacheEntity validToken = new JwtTokenProviderImpl.TokenCacheEntity("v2",
                                                                                                     2L,
                                                                                                     new Date(System.currentTimeMillis() + 1000)
        );
        tokenMap.put("expired", expiredToken);
        tokenMap.put("valid", validToken);

        when(mockJwtTokenProvider.getCacheTokenMap()).thenReturn(tokenMap);

        cronTaskManagerUnderTest.deleteExpiredToken();

        assert !tokenMap.containsKey("expired");
        assert tokenMap.containsKey("valid");
    }


    /**
     * 測試檢查並更新用戶儲存使用量的功能。
     *
     * 測試步驟：
     * - 建立測試用戶和檔案元資料
     * - 模擬儲存庫回傳用戶、檔案和伺服器檔案資料
     * - 執行儲存使用量檢查
     * - 驗證用戶的使用量被正確計算和更新
     *
     * 預期結果：用戶的儲存使用量應被正確更新
     */
    @Test
    @DisplayName("檢查並更新用戶的存儲使用量 - 成功更新使用量")
    void checkUserStorageLimit_ValidUser_SuccessfullyUpdated() {
        User user = new User();
        user.setId(1L);

        UserFileMetadata fileMeta = new UserFileMetadata();
        fileMeta.setFileType(FileEnum.DOCUMENT);
        fileMeta.setServerFileId(1L);

        ServerFileMetadata serverFile = new ServerFileMetadata();
        serverFile.setId(1L);
        serverFile.setFileSize(1000L);

        when(mockUserRepository.findAll()).thenReturn(Flux.just(user));
        when(mockUserFileMetaRepository.findAllByUserId(eq(1L))).thenReturn(Flux.just(fileMeta));
        when(mockServerFileMetaRepository.findAllById(anySet())).thenReturn(Flux.just(serverFile));
        when(mockUserRepository.save(any(User.class))).thenReturn(Mono.just(user));

        cronTaskManagerUnderTest.checkUserStorageLimit();

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(mockUserRepository).findAll();
        verify(mockUserFileMetaRepository).findAllByUserId(eq(1L));
        verify(mockServerFileMetaRepository).findAllById(eq(Collections.singleton(1L)));
        verify(mockUserRepository).save(argThat(savedUser -> savedUser.getUsedStorage() == 1000L));
    }


    /**
     * 測試清理過期檔案傳輸任務的功能。
     *
     * 測試步驟：
     * - 建立過期的傳輸任務
     * - 設定檔案上傳配置
     * - 執行過期任務清理
     * - 驗證過期任務狀態被更新為失敗
     *
     * 預期結果：過期的傳輸任務應被標記為失敗狀態
     */
    @Test
    @DisplayName("清理過期的檔案傳輸任務 - 成功清理過期任務")
    void cleanExpireTransferTask_ExpiredTasks_SuccessfullyCleaned() {
        TransfersTask expiredTask = new TransfersTask();
        expiredTask.setMd5("testMd5");
        expiredTask.setTransferTaskId("testTaskId");
        expiredTask.setStartTime(LocalDateTime.now().minusHours(5));
        expiredTask.setStatus(TransfersStatusEnum.UPLOADING);

        when(mockFileProperties.getUpload()).thenReturn(mockUpload);
        when(mockUpload.getMaxUploadDuration()).thenReturn(Duration.ofHours(4));
        when(mockTransfersTasksRepository.findAllByStatusInAndStartTimeBefore(any(), any())).thenReturn(Flux.just(expiredTask));
        when(mockTransfersTasksManager.updateTransfersTask(anyString(), anyString(), any(), anyString(), any(), any())).thenReturn(Mono.empty());

        cronTaskManagerUnderTest.cleanExpireTransferTask();

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(mockTransfersTasksManager).updateTransfersTask(eq(expiredTask.getMd5()),
                                                              eq(expiredTask.getTransferTaskId()),
                                                              eq(TransfersStatusEnum.FAILED),
                                                              eq("上傳超時"),
                                                              isNull(),
                                                              eq(true)
        );
    }


    /**
     * 測試刪除過期檔案並更新儲存使用量的功能。
     *
     * 測試步驟：
     * - 建立測試用戶和過期檔案記錄
     * - 模擬儲存庫操作和交易管理
     * - 執行過期檔案刪除
     * - 驗證檔案被刪除且用戶儲存使用量被更新
     *
     * 預期結果：過期檔案應被刪除，用戶儲存使用量應被更新
     */
    @Test
    @DisplayName("刪除過期檔案並更新存儲使用量 - 成功刪除過期檔案")
    void deleteExpiredFile_ExpiredFiles_SuccessfullyDeleted() {
        User user = new User();
        user.setId(1L);

        FileTrashRecord expiredFile = new FileTrashRecord();
        expiredFile.setUserId(1L);
        expiredFile.setFileId(1L);
        expiredFile.setDeleteTime(LocalDateTime.now().minusDays(1));

        when(mockUserRepository.findAll()).thenReturn(Flux.just(user));
        when(mockFileTrashRecordRepository.findAllByUserIdAndDeleteTimeBefore(any(Long.class), any(LocalDateTime.class))).thenReturn(Flux.just(
                expiredFile));
        when(mockTransactionalOperator.transactional(any(Mono.class))).thenReturn(Mono.empty());
        when(mockUserRepository.save(any(User.class))).thenReturn(Mono.just(user));
        when(mockUserFileMetaRepository.findAllByUserId(any(Long.class))).thenReturn(Flux.empty());
        when(mockUserFileMetaRepository.deleteAllById(anyList())).thenReturn(Mono.empty());

        cronTaskManagerUnderTest.deleteExpiredFile();

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(mockFileTrashRecordRepository).findAllByUserIdAndDeleteTimeBefore(eq(1L), any(LocalDateTime.class));
        verify(mockUserFileMetaRepository).deleteAllById(anyList());
        verify(mockTransactionalOperator).transactional(any(Mono.class));
    }


    /**
     * 測試清理過期 CSRF 憑證的功能。
     *
     * 測試步驟：
     * - 設定 CSRF 憑證儲存庫策略
     * - 執行過期憑證清理
     * - 驗證清理操作被正確調用
     *
     * 預期結果：過期的 CSRF 憑證應被清理
     */
    @Test
    @DisplayName("清理過期的 CSRF 憑證 - 成功清理過期憑證")
    void clearExpiredToken_ExpiredTokens_SuccessfullyCleared() {
        when(mockCsrfTokenRepositoryStrategy.getCsrfTokenRepository()).thenReturn(mockServerCsrfTokenRepository);
        when(mockServerCsrfTokenRepository.deleteToken(null)).thenReturn(Mono.empty());

        cronTaskManagerUnderTest.clearExpiredToken();

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(mockServerCsrfTokenRepository).deleteToken(null);
    }
}
