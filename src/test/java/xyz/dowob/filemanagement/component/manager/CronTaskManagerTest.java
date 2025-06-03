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
        verify(mockServerFileMetaRepository).findAllById(eq(Collections.singleton("1")));
        verify(mockUserRepository).save(argThat(savedUser -> savedUser.getUsedStorage() == 1000L));
    }


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
        verify(mockUserFileMetaRepository).deleteAllById(eq(Collections.singletonList("1")));
        verify(mockTransactionalOperator).transactional(any(Mono.class));
    }


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
