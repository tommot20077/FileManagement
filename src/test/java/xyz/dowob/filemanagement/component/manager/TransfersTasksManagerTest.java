package xyz.dowob.filemanagement.component.manager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.unit.DataSize;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.customenum.TransfersStatusEnum;
import xyz.dowob.filemanagement.data.file.dto.FileMetadataDTO;
import xyz.dowob.filemanagement.entity.TransfersTask;
import xyz.dowob.filemanagement.exception.ProcessException;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.repostiory.TransfersTasksRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TransfersTasksManager 傳輸任務管理測試類別。
 *
 * 測試 TransfersTasksManager 的檔案傳輸任務管理功能，包括任務註冊、獲取、
 * 建立、更新和銷毀等操作。
 *
 * 前置條件：
 * - 初始化 Mock 依賴項目
 * - 設置檔案屬性配置
 * - 確保任務狀態管理的正確性
 *
 * 測試步驟：
 * - 測試上傳任務的註冊功能
 * - 測試傳輸任務的獲取和過濾
 * - 測試任務的建立和更新機制
 * - 測試異常情況和限制處理
 * - 測試管理器銷毀時的清理機制
 *
 * 預期結果：
 * - 任務應被正確管理和追蹤
 * - 限制和驗證應正確執行
 * - 異常情況應被適當處理
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TransfersTasksManager 邏輯處理測試")
class TransfersTasksManagerTest {

    @Mock
    private TransfersTasksRepository mockTransfersTasksRepository;

    @Mock
    private FileProperties mockFileProperties;

    private TransfersTasksManager transfersTasksManagerUnderTest;


    @BeforeEach
    void setUp() {
        transfersTasksManagerUnderTest = new TransfersTasksManager(mockTransfersTasksRepository, mockFileProperties);
    }


    /**
     * 測試註冊上傳任務的成功情況。
     *
     * 測試步驟：
     * - 建立檔案元資料和傳輸任務 ID
     * - 設置檔案大小限制配置
     * - 執行任務註冊
     * - 驗證任務被正確儲存和管理
     *
     * 預期結果：任務應成功註冊且加入管理清單
     */
    @Test
    @DisplayName("註冊上傳任務 - 成功註冊")
    void registerUploadTask_successfulRegistration_returnsMonoEmpty() {
        FileMetadataDTO fileMetadataDTO = new FileMetadataDTO();
        fileMetadataDTO.setMd5("testMd5");
        fileMetadataDTO.setFileSize(100L);
        String transferTaskId = "testTransferId";

        FileProperties.Upload uploadProperties = mock(FileProperties.Upload.class);
        when(mockFileProperties.getUpload()).thenReturn(uploadProperties);
        when(uploadProperties.getMaxUploadFileSize()).thenReturn(DataSize.ofBytes(200L));
        when(mockTransfersTasksRepository.save(any(TransfersTask.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(transfersTasksManagerUnderTest.registerUploadTask(fileMetadataDTO, transferTaskId)).verifyComplete();

        assertThat(transfersTasksManagerUnderTest.getTransfersTask("testMd5", TransfersStatusEnum.UPLOADING)).hasSize(1);
        verify(mockTransfersTasksRepository).save(any(TransfersTask.class));
    }


    /**
     * 測試註冊重複 MD5 上傳任務的異常處理。
     *
     * 測試步驟：
     * - 先建立一個正在上傳的任務
     * - 嘗試使用相同 MD5 註冊新任務
     * - 驗證拋出適當的驗證異常
     *
     * 預期結果：應拋出 ValidationException 且包含現有任務 ID
     */
    @Test
    @DisplayName("註冊上傳任務 - 已存在相同MD5且正在上傳的任務 - 拋出 ValidationException")
    void registerUploadTask_existingTaskWithSameMd5AndUploadingStatus_throwsValidationException() {
        String md5 = "testMd5";
        FileMetadataDTO fileMetadataDTO = new FileMetadataDTO();
        fileMetadataDTO.setMd5(md5);
        fileMetadataDTO.setFileSize(100L);
        String transferTaskId = "testTransferId";
        String existingTransferTaskId = "existingTestTransferId";

        TransfersTask existingTask = createGenericTask(md5, existingTransferTaskId, TransfersStatusEnum.UPLOADING, 100L);
        when(mockTransfersTasksRepository.save(any(TransfersTask.class))).thenReturn(Mono.just(existingTask));
        transfersTasksManagerUnderTest.createTransfersTask(fileMetadataDTO, existingTransferTaskId, TransfersStatusEnum.UPLOADING).block();

        StepVerifier
                .create(transfersTasksManagerUnderTest.registerUploadTask(fileMetadataDTO, transferTaskId))
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.EXISTING_TRANSFER_TASK && throwable
                        .getMessage()
                        .contains(existingTransferTaskId))
                .verify();
    }


    /**
     * 建立通用傳輸任務的輔助方法。
     *
     * @param md5 檔案 MD5 值
     * @param taskId 任務 ID
     * @param status 任務狀態
     * @param fileSize 檔案大小
     * @return 傳輸任務物件
     */
    private TransfersTask createGenericTask(String md5, String taskId, TransfersStatusEnum status, long fileSize) {
        TransfersTask task = new TransfersTask();
        task.setMd5(md5);
        task.setTransferTaskId(taskId);
        task.setStatus(status);
        task.setFileSize(fileSize);
        task.setStartTime(LocalDateTime.now());
        return task;
    }


    /**
     * 測試檔案大小超過限制時的異常處理。
     *
     * 測試步驟：
     * - 設置檔案大小超過配置限制
     * - 嘗試註冊上傳任務
     * - 驗證拋出檔案大小限制異常
     *
     * 預期結果：應拋出 ValidationException 且不儲存任務
     */
    @Test
    @DisplayName("註冊上傳任務 - 檔案大小超過限制 - 拋出 ValidationException")
    void registerUploadTask_fileSizeExceedsLimit_throwsValidationException() {
        FileMetadataDTO fileMetadataDTO = new FileMetadataDTO();
        fileMetadataDTO.setMd5("testMd5");
        fileMetadataDTO.setFileSize(300L);
        String transferTaskId = "testTransferId";

        FileProperties.Upload uploadProperties = mock(FileProperties.Upload.class);
        when(mockFileProperties.getUpload()).thenReturn(uploadProperties);
        when(uploadProperties.getMaxUploadFileSize()).thenReturn(DataSize.ofBytes(200L));

        StepVerifier
                .create(transfersTasksManagerUnderTest.registerUploadTask(fileMetadataDTO, transferTaskId))
                .expectErrorMatches(throwable -> throwable instanceof ValidationException && ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.FILE_SIZE_LIMIT)
                .verify();
        verify(mockTransfersTasksRepository, never()).save(any(TransfersTask.class));
    }


    @Test
    @DisplayName("獲取傳輸任務 - 存在任務且無狀態過濾 - 返回所有任務")
    void getTransfersTask_tasksExistNoStatusFilter_returnsAllTasks() {
        String md5 = "testMd5_getNoFilter";
        String taskId = "id_getNoFilter";
        FileMetadataDTO fileMetadataDTO = new FileMetadataDTO();
        fileMetadataDTO.setMd5(md5);
        fileMetadataDTO.setFileSize(100L);

        TransfersTask taskToCreate = createGenericTask(md5, taskId, TransfersStatusEnum.UPLOADING, 100L);
        when(mockTransfersTasksRepository.save(any(TransfersTask.class))).thenReturn(Mono.just(taskToCreate));
        transfersTasksManagerUnderTest.createTransfersTask(fileMetadataDTO, taskId, TransfersStatusEnum.UPLOADING).block();

        List<TransfersTask> tasks = transfersTasksManagerUnderTest.getTransfersTask(md5, null);

        assertThat(tasks).hasSize(1);
        assertThat(tasks.getFirst().getTransferTaskId()).isEqualTo(taskId);
        assertThat(tasks.getFirst().getStatus()).isEqualTo(TransfersStatusEnum.UPLOADING);
    }


    @Test
    @DisplayName("獲取傳輸任務 - 存在任務且有狀態過濾 - 返回符合狀態的任務")
    void getTransfersTask_tasksExistWithStatusFilter_returnsMatchingTasks() {
        String md5 = "testMd5_getWithFilter";
        String taskId = "id_getWithFilter";
        FileMetadataDTO fileMetadataDTO = new FileMetadataDTO();
        fileMetadataDTO.setMd5(md5);
        fileMetadataDTO.setFileSize(100L);

        TransfersTask taskToCreate = createGenericTask(md5, taskId, TransfersStatusEnum.UPLOADING, 100L);
        when(mockTransfersTasksRepository.save(any(TransfersTask.class))).thenReturn(Mono.just(taskToCreate));
        transfersTasksManagerUnderTest.createTransfersTask(fileMetadataDTO, taskId, TransfersStatusEnum.UPLOADING).block();

        List<TransfersTask> tasks = transfersTasksManagerUnderTest.getTransfersTask(md5, TransfersStatusEnum.UPLOADING);

        assertThat(tasks).hasSize(1);
        assertThat(tasks.getFirst().getTransferTaskId()).isEqualTo(taskId);
        assertThat(tasks.getFirst().getStatus()).isEqualTo(TransfersStatusEnum.UPLOADING);

        List<TransfersTask> noTasks = transfersTasksManagerUnderTest.getTransfersTask(md5, TransfersStatusEnum.COMPLETED);
        assertThat(noTasks).isEmpty();
    }


    @Test
    @DisplayName("創建傳輸任務 (3個參數) - 成功創建 - 返回MonoEmpty")
    void createTransfersTask_3params_successfulCreation_returnsMonoEmpty() {
        String md5 = "testMd5_3params";
        String transferTaskId = "testTransferId_3params";
        TransfersStatusEnum status = TransfersStatusEnum.DOWNLOADING;
        FileMetadataDTO fileMetadataDTO = new FileMetadataDTO();
        fileMetadataDTO.setMd5(md5);
        fileMetadataDTO.setFileSize(150L);

        TransfersTask taskToCreate = createGenericTask(md5, transferTaskId, status, 150L);
        when(mockTransfersTasksRepository.save(any(TransfersTask.class))).thenReturn(Mono.just(taskToCreate));

        StepVerifier.create(transfersTasksManagerUnderTest.createTransfersTask(fileMetadataDTO, transferTaskId, status)).verifyComplete();

        List<TransfersTask> tasks = transfersTasksManagerUnderTest.getTransfersTask(md5, status);
        assertThat(tasks).hasSize(1);
        TransfersTask createdTask = tasks.getFirst();
        assertThat(createdTask.getTransferTaskId()).isEqualTo(transferTaskId);
        assertThat(createdTask.getMd5()).isEqualTo(md5);
        assertThat(createdTask.getFileSize()).isEqualTo(150L);
        assertThat(createdTask.getStatus()).isEqualTo(status);
        assertThat(createdTask.getStartTime()).isNotNull();
        verify(mockTransfersTasksRepository).save(any(TransfersTask.class));
    }


    @Test
    @DisplayName("創建傳輸任務 (5個參數) - 成功創建 - 返回MonoEmpty")
    void createTransfersTask_5params_successfulCreation_returnsMonoEmpty() {
        String md5 = "testMd5_5params";
        String transferTaskId = "testTransferId_5params";
        String gridFsId = "testGridFsId";
        String message = "Test message";
        TransfersStatusEnum status = TransfersStatusEnum.COMPLETED;
        FileMetadataDTO fileMetadataDTO = new FileMetadataDTO();
        fileMetadataDTO.setMd5(md5);
        fileMetadataDTO.setFileSize(250L);

        TransfersTask taskToCreate = createGenericTask(md5, transferTaskId, status, 250L);
        taskToCreate.setGridFsId(gridFsId);
        taskToCreate.setMessage(message);
        when(mockTransfersTasksRepository.save(any(TransfersTask.class))).thenReturn(Mono.just(taskToCreate));

        StepVerifier
                .create(transfersTasksManagerUnderTest.createTransfersTask(fileMetadataDTO, transferTaskId, gridFsId, message, status))
                .verifyComplete();

        List<TransfersTask> tasks = transfersTasksManagerUnderTest.getTransfersTask(md5, status);
        assertThat(tasks).hasSize(1);
        TransfersTask createdTask = tasks.getFirst();
        assertThat(createdTask.getTransferTaskId()).isEqualTo(transferTaskId);
        assertThat(createdTask.getMd5()).isEqualTo(md5);
        assertThat(createdTask.getFileSize()).isEqualTo(250L);
        assertThat(createdTask.getGridFsId()).isEqualTo(gridFsId);
        assertThat(createdTask.getMessage()).isEqualTo(message);
        assertThat(createdTask.getStatus()).isEqualTo(status);
        assertThat(createdTask.getStartTime()).isNotNull();
        verify(mockTransfersTasksRepository).save(any(TransfersTask.class));
    }


    @Test
    @DisplayName("更新傳輸任務 - 任務未完成 - 成功更新")
    void updateTransfersTask_taskNotFinished_updatesSuccessfully() {
        String md5 = "updateMd5";
        String transferTaskId = "updateTransferId";
        FileMetadataDTO fileMetadataDTO = new FileMetadataDTO();
        fileMetadataDTO.setMd5(md5);
        fileMetadataDTO.setFileSize(100L);

        when(mockTransfersTasksRepository.save(any(TransfersTask.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        transfersTasksManagerUnderTest.createTransfersTask(fileMetadataDTO, transferTaskId, TransfersStatusEnum.UPLOADING).block();

        TransfersStatusEnum newStatus = TransfersStatusEnum.DOWNLOADING;
        String newMessage = "Now Downloading";
        String newGridFsId = "newGridFsId";

        List<TransfersTask> initialTasks = transfersTasksManagerUnderTest.getTransfersTask(md5, TransfersStatusEnum.UPLOADING);
        assertThat(initialTasks).hasSize(1);

        transfersTasksManagerUnderTest.updateTransfersTask(md5, transferTaskId, newStatus, newMessage, newGridFsId, false).block();

        List<TransfersTask> tasks = transfersTasksManagerUnderTest.getTransfersTask(md5, newStatus);
        assertThat(tasks).hasSize(1);
        TransfersTask updatedTask = tasks.getFirst();
        assertThat(updatedTask.getStatus()).isEqualTo(newStatus);
        assertThat(updatedTask.getMessage()).isEqualTo(newMessage);
        assertThat(updatedTask.getGridFsId()).isEqualTo(newGridFsId);
        assertThat(updatedTask.getFinishTime()).isNull();

        verify(mockTransfersTasksRepository, times(2)).save(any(TransfersTask.class));
    }


    @Test
    @DisplayName("更新傳輸任務 - 任務完成 - 成功更新並從活躍任務中移除")
    void updateTransfersTask_taskFinished_updatesSuccessfullyAndRemovesFromActive() {
        String md5 = "finishMd5";
        String transferTaskId = "finishTransferId";
        FileMetadataDTO fileMetadataDTO = new FileMetadataDTO();
        fileMetadataDTO.setMd5(md5);
        fileMetadataDTO.setFileSize(120L);

        when(mockTransfersTasksRepository.save(any(TransfersTask.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        transfersTasksManagerUnderTest.createTransfersTask(fileMetadataDTO, transferTaskId, TransfersStatusEnum.UPLOADING).block();

        TransfersStatusEnum newStatus = TransfersStatusEnum.COMPLETED;
        String newMessage = "Upload complete";

        List<TransfersTask> initialTasks = transfersTasksManagerUnderTest.getTransfersTask(md5, TransfersStatusEnum.UPLOADING);
        assertThat(initialTasks).hasSize(1);

        transfersTasksManagerUnderTest.updateTransfersTask(md5, transferTaskId, newStatus, newMessage, null, true).block();

        assertThat(transfersTasksManagerUnderTest.getTransfersTask(md5, null)).isEmpty();

        verify(mockTransfersTasksRepository, times(2)).save(any(TransfersTask.class));
    }


    @Test
    @DisplayName("更新傳輸任務 - 任務不存在 - 拋出 ProcessException")
    void updateTransfersTask_taskNotFound_throwsProcessException() {
        String md5 = "nonExistentMd5ForUpdate";
        String transferTaskId = "nonExistentTransferId";
        StepVerifier
                .create(transfersTasksManagerUnderTest.updateTransfersTask(md5, transferTaskId, TransfersStatusEnum.FAILED, "Error", null, true))
                .expectErrorMatches(throwable -> throwable instanceof ProcessException && ((ProcessException) throwable).getErrorCode() == ProcessException.ErrorCode.NOT_EXISTING_MD5_TRANSFERS_TASK)
                .verify();
        verify(mockTransfersTasksRepository, never()).save(any(TransfersTask.class));
    }


    @Test
    @DisplayName("銷毀任務管理器 - 無未完成任務 - 不執行任何操作")
    void destroy_noUnfinishedTasks_doesNothing() {
        when(mockTransfersTasksRepository.findAllByStatusIn(List.of(TransfersStatusEnum.UPLOADING,
                                                                    TransfersStatusEnum.DOWNLOADING
        ))).thenReturn(Flux.empty());

        StepVerifier.create(transfersTasksManagerUnderTest.destroy()).verifyComplete();

        verify(mockTransfersTasksRepository, never()).saveAll(anyIterable());
        verify(mockTransfersTasksRepository, never()).save(any(TransfersTask.class));
    }


    @Test
    @DisplayName("銷毀任務管理器 - 存在未完成任務 - 更新任務狀態為失敗")
    void destroy_unfinishedTasksExist_updatesTasksToFailed() {
        TransfersTask task1 = createGenericTask("md5_1", "task1", TransfersStatusEnum.UPLOADING, 100L);
        TransfersTask task2 = createGenericTask("md5_2", "task2", TransfersStatusEnum.DOWNLOADING, 200L);
        List<TransfersTask> unfinishedTasks = List.of(task1, task2);

        when(mockTransfersTasksRepository.findAllByStatusIn(List.of(TransfersStatusEnum.UPLOADING,
                                                                    TransfersStatusEnum.DOWNLOADING
        ))).thenReturn(Flux.fromIterable(unfinishedTasks));
        when(mockTransfersTasksRepository.saveAll(anyIterable())).thenReturn(Flux.empty());

        StepVerifier.create(transfersTasksManagerUnderTest.destroy()).verifyComplete();

        verify(mockTransfersTasksRepository).saveAll(argThat((List<TransfersTask> tasksToSave) -> tasksToSave.size() == 2 && tasksToSave
                .stream()
                .allMatch(t -> t.getStatus() == TransfersStatusEnum.FAILED && t
                        .getMessage()
                        .equals("伺服器關閉，任務被取消") && t.getFinishTime() != null)));
        verify(mockTransfersTasksRepository, never()).save(any(TransfersTask.class));
    }
}
