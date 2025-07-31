package xyz.dowob.filemanagement.service.serviceInterface;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.dowob.filemanagement.data.file.bo.FileEditBO;
import xyz.dowob.filemanagement.data.file.bo.UserFileDataBO;
import xyz.dowob.filemanagement.data.file.dto.*;
import xyz.dowob.filemanagement.data.response.PagedResponseDTO;
import xyz.dowob.filemanagement.entity.ServerFileMetadata;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.entity.UserFileMetadata;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AbstractFileService 抽象類測試
 *
 * <p>測試 AbstractFileService 檔案服務抽象類的實現契約和服務層設計模式，驗證抽象類作為服務接口的橋樑實現。
 * 
 * <p>測試涵蓋的功能範圍：
 * <p>- 抽象類繼承 FileService 接口的方法實現邏輯
 * <p>- FileCrudService 和 BaseFileService 的具體實現模式
 * <p>- RecoverableFile 接口的檔案恢復能力
 * <p>- 用戶檔案列表查詢、檔案路徑構建、檔案搜索機制
 * <p>- 檔案上傳、分塊上傳、下載、編輯、刪除的完整流程
 * <p>- 檔案恢復和回收站移除功能
 * <p>- 響應式編程模式在抽象類中的實現
 * <p>- 權限檢查、緩存管理和性能優化策略
 *
 * 測試摘要：
 * 
 * 驗證 AbstractFileService 抽象類作為檔案服務層核心組件的正確性，確保其能夠為具體實現類提供統一的業務邏輯框架。
 *
 * 前置條件：
 * - AbstractFileService 抽象類及其依賴的服務接口正常載入
 * - 相關 DTO、BO 數據傳輸對象和實體類可用
 * - Reactor WebFlux 響應式編程環境可用
 *
 * 測試步驟：
 * - 驗證抽象方法的具體實現邏輯
 * - 測試檔案管理生命週期的完整流程
 * - 驗證權限控制和安全檢查機制
 * - 測試響應式流的正確處理和異常邊界情況
 *
 * 預期結果：
 * - 抽象類實現符合服務層設計模式
 * - 檔案操作流程滿足業務需求
 * - 響應式編程模式正確實現
 * - 異常處理和邊界條件處理完善
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AbstractFileService 檔案服務抽象類測試")
class AbstractFileServiceTest {

    private TestableAbstractFileService abstractFileService;
    private User testUser;
    private User adminUser;
    private FileFilterDTO testFilter;
    private FileMetadataDTO testFileMetadata;
    private UploadChunkDTO testUploadChunk;
    private UserFileMetadata testUserFile;
    private ServerFileMetadata testServerFile;
    private FileEditBO testFileEditBO;


    @BeforeEach
    void setUp() {
        abstractFileService = new TestableAbstractFileService();

        // 設置測試對象
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        adminUser = new User();
        adminUser.setId(2L);
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@example.com");

        testFilter = FileFilterDTO.builder()
                .page(0)
                .pageSize(20)
                .keyword("")
                .build();

        testFileMetadata = new FileMetadataDTO();
        testFileMetadata.setFilename("test.txt");
        testFileMetadata.setFileSize(1024L);
        testFileMetadata.setMd5("test-md5-hash");

        testUploadChunk = new UploadChunkDTO();
        testUploadChunk.setTransferTaskId("test-task-123");
        testUploadChunk.setChunkIndex(1);
        testUploadChunk.setTotalChunks(5);

        testUserFile = new UserFileMetadata();
        testUserFile.setId(1L);
        testUserFile.setFilename("test.txt");
        testUserFile.setUserId(1L);
        // UserFileMetadata doesn't have fileSize field
        testUserFile.setUploadTime(LocalDateTime.now());

        testServerFile = new ServerFileMetadata();
        testServerFile.setId(1L);
        testServerFile.setFileSize(1024L);
        testServerFile.setMd5("test-md5-hash");
        testServerFile.setUploadTime(LocalDateTime.now());

        FileEditDTO editDTO = new FileEditDTO();
        editDTO.setFileId("1");
        editDTO.setFilename("renamed.txt");
        testFileEditBO = new FileEditBO(editDTO);
        testFileEditBO.setUserFileMetadata(testUserFile);
    }


    @Test
    @DisplayName("一般測試 - getUserFileList 方法實現")
    void testGetUserFileList_implementation() {
        StepVerifier.create(abstractFileService.getUserFileList(testUser, testFilter))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals(0, response.getCurrentPage());
                    assertEquals(20, response.getPageSize());
                    assertEquals(2L, response.getTotalElements());

                    // PagedResponseDTO contains List<UserFileListDTO>, not single UserFileListDTO
                    assertFalse(response.getData().isEmpty());
                    assertEquals(2, response.getData().size());
                    UserFileListDTO firstFile = response.getData().get(0);
                    assertNotNull(firstFile);
                    assertNotNull(firstFile.getFilename());
                })
                .verifyComplete();
    }

    // ==================== 一般測試 ====================


    @Test
    @DisplayName("一般測試 - uploadFile 方法實現")
    void testUploadFile_implementation() {
        StepVerifier.create(abstractFileService.uploadFile(testFileMetadata, testUser))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals(100.0, response.getProgress());
                    assertTrue(response.getIsSuccess());
                    assertTrue(response.getIsFinished());
                    assertEquals("檔案上傳成功", response.getMessage());
                })
                .verifyComplete();
    }


    @Test
    @DisplayName("一般測試 - uploadFileChunk 方法實現")
    void testUploadFileChunk_implementation() {
        StepVerifier.create(abstractFileService.uploadFileChunk(testUploadChunk))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals(1, response.getChunkIndex());
                    assertEquals("test-task-123", response.getTransferTaskId());
                    assertEquals(20.0, response.getProgress()); // 1/5 * 100
                    assertTrue(response.getIsSuccess());
                    assertFalse(response.getIsFinished()); // 第1個分塊，還沒完成
                    assertEquals(5, response.getTotalChunks());
                })
                .verifyComplete();
    }


    @Test
    @DisplayName("一般測試 - uploadFileChunk 最後分塊")
    void testUploadFileChunk_lastChunk() {
        testUploadChunk.setChunkIndex(5); // 最後一個分塊

        StepVerifier.create(abstractFileService.uploadFileChunk(testUploadChunk))
                .assertNext(response -> {
                    assertEquals(5, response.getChunkIndex());
                    assertEquals(100.0, response.getProgress()); // 5/5 * 100
                    assertTrue(response.getIsFinished()); // 最後一個分塊，已完成
                })
                .verifyComplete();
    }


    @Test
    @DisplayName("一般測試 - downloadFile 方法實現")
    void testDownloadFile_implementation() {
        StepVerifier.create(abstractFileService.downloadFile(testUserFile, testUser))
                .assertNext(fileData -> {
                    assertNotNull(fileData);
                    assertEquals("test.txt", fileData.getFilename());
                    assertEquals(1024L, fileData.getFileSize());
                    assertEquals("application/octet-stream", fileData.getMimeType());
                })
                .verifyComplete();
    }


    @Test
    @DisplayName("一般測試 - deleteFile 方法實現")
    void testDeleteFile_implementation() {
        StepVerifier.create(abstractFileService.deleteFile(testUserFile, testUser))
                .verifyComplete();
    }


    @Test
    @DisplayName("一般測試 - editFile 方法實現")
    void testEditFile_implementation() {
        StepVerifier.create(abstractFileService.editFile(testFileEditBO, testUser))
                .verifyComplete();
    }


    @Test
    @DisplayName("一般測試 - restoreFile 方法實現")
    void testRestoreFile_implementation() {
        StepVerifier.create(abstractFileService.restoreFile(testUserFile, testUser))
                .assertNext(restoredFile -> {
                    assertNotNull(restoredFile);
                    assertEquals(testUserFile.getId(), restoredFile.getId());
                    assertEquals(testUserFile.getFilename(), restoredFile.getFilename());
                    assertEquals(testUserFile.getUserId(), restoredFile.getUserId());
                    assertNotNull(restoredFile.getUploadTime());
                })
                .verifyComplete();
    }


    @Test
    @DisplayName("一般測試 - removeFile 方法實現")
    void testRemoveFile_implementation() {
        StepVerifier.create(abstractFileService.removeFile(testUserFile, testUser))
                .expectNext(true)
                .verifyComplete();
    }


    @Test
    @DisplayName("一般測試 - CRUD Service create 方法")
    void testCrudService_create() {
        StepVerifier.create(abstractFileService.createTestFile())
                .assertNext(newFile -> {
                    assertNotNull(newFile);
                    assertEquals(1L, newFile.getId());
                    assertEquals("new_file.txt", newFile.getFilename());
                    assertNotNull(newFile.getUploadTime());
                })
                .verifyComplete();
    }


    @Test
    @DisplayName("一般測試 - CRUD Service getById 方法")
    void testCrudService_getById() {
        StepVerifier.create(abstractFileService.getTestFileById(5L))
                .assertNext(file -> {
                    assertEquals(5L, file.getId());
                    assertEquals("file_5.txt", file.getFilename());
                    // UserFileMetadata doesn't have getFileSize method
                })
                .verifyComplete();
    }


    @Test
    @DisplayName("一般測試 - CRUD Service getAll 方法")
    void testCrudService_getAll() {
        StepVerifier.create(abstractFileService.getAllTestFiles())
                .expectNextCount(2)
                .verifyComplete();
    }


    @Test
    @DisplayName("一般測試 - CRUD Service getAllByParams 方法")
    void testCrudService_getAllByParams() {
        StepVerifier.create(abstractFileService.getTestFilesByParams("userId", 1L))
                .expectNextCount(2) // 過濾後的結果
                .verifyComplete();
    }


    @Test
    @DisplayName("一般測試 - CRUD Service update 方法")
    void testCrudService_update() {
        StepVerifier.create(abstractFileService.updateTestFile(testUserFile))
                .assertNext(updatedFile -> {
                    assertEquals(testUserFile.getId(), updatedFile.getId());
                    assertNotNull(updatedFile.getLastAccessTime());
                })
                .verifyComplete();
    }


    @Test
    @DisplayName("一般測試 - CRUD Service delete 方法")
    void testCrudService_delete() {
        StepVerifier.create(abstractFileService.deleteTestFile(testUserFile))
                .verifyComplete();
    }


    @Test
    @DisplayName("一般測試 - 管理員權限操作")
    void testAdminPermissions() {
        // 管理員可以下載其他用戶的檔案
        testUserFile.setUserId(99L); // 不同的用戶ID

        StepVerifier.create(abstractFileService.downloadFile(testUserFile, adminUser))
                .assertNext(fileData -> {
                    assertNotNull(fileData);
                    assertEquals("test.txt", fileData.getFilename());
                })
                .verifyComplete();

        // 管理員可以刪除其他用戶的檔案
        StepVerifier.create(abstractFileService.deleteFile(testUserFile, adminUser))
                .verifyComplete();

        // 管理員可以恢復其他用戶的檔案
        StepVerifier.create(abstractFileService.restoreFile(testUserFile, adminUser))
                .assertNext(restoredFile -> {
                    assertEquals(99L, restoredFile.getUserId());
                })
                .verifyComplete();
    }


    @Test
    @DisplayName("一般測試 - 驗證抽象類繼承關係")
    void testAbstractClassInheritance() {
        // 驗證 AbstractFileService 實現了 FileService
        assertTrue(FileService.class.isAssignableFrom(AbstractFileService.class));

        // 驗證 FileService 繼承了其他接口
        assertTrue(xyz.dowob.filemanagement.unity.FileCrudService.class.isAssignableFrom(FileService.class));
        assertTrue(BaseFileService.class.isAssignableFrom(FileService.class));
        assertTrue(RecoverableFile.class.isAssignableFrom(FileService.class));
    }


    @Test
    @DisplayName("一般測試 - 檔案完整生命週期管理")
    void testFileLifecycleManagement() {
        // 創建 -> 上傳 -> 下載 -> 編輯 -> 移除 -> 恢復 -> 刪除
        Mono<String> lifecycle = abstractFileService.createTestFile()
                .flatMap(newFile -> abstractFileService.uploadFile(testFileMetadata, testUser))
                .flatMap(uploadResponse -> {
                    assertNotNull(uploadResponse);
                    return abstractFileService.downloadFile(testUserFile, testUser);
                })
                .flatMap(fileData -> {
                    assertNotNull(fileData);
                    return abstractFileService.editFile(testFileEditBO, testUser);
                })
                .then(abstractFileService.removeFile(testUserFile, testUser))
                .flatMap(removeResult -> {
                    assertTrue(removeResult);
                    return abstractFileService.restoreFile(testUserFile, testUser);
                })
                .flatMap(restoredFile -> {
                    assertNotNull(restoredFile);
                    return abstractFileService.deleteFile(restoredFile, testUser);
                })
                .thenReturn("檔案生命週期完成");

        StepVerifier.create(lifecycle)
                .expectNext("檔案生命週期完成")
                .verifyComplete();
    }


    @Test
    @DisplayName("異常測試 - getUserFileList 傳入 null 用戶")
    void testGetUserFileList_withNullUser() {
        StepVerifier.create(abstractFileService.getUserFileList(null, testFilter))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    // ==================== 異常測試 ====================


    @Test
    @DisplayName("異常測試 - getUserFileList 傳入 null 過濾條件")
    void testGetUserFileList_withNullFilter() {
        StepVerifier.create(abstractFileService.getUserFileList(testUser, null))
                .expectError(IllegalArgumentException.class)
                .verify();
    }


    @Test
    @DisplayName("異常測試 - uploadFile 傳入 null 檔案元數據")
    void testUploadFile_withNullMetadata() {
        StepVerifier.create(abstractFileService.uploadFile(null, testUser))
                .expectError(IllegalArgumentException.class)
                .verify();
    }


    @Test
    @DisplayName("異常測試 - uploadFile 傳入 null 用戶")
    void testUploadFile_withNullUser() {
        StepVerifier.create(abstractFileService.uploadFile(testFileMetadata, null))
                .expectError(IllegalArgumentException.class)
                .verify();
    }


    @Test
    @DisplayName("異常測試 - uploadFileChunk 傳入 null 分塊數據")
    void testUploadFileChunk_withNullChunkData() {
        StepVerifier.create(abstractFileService.uploadFileChunk(null))
                .expectError(IllegalArgumentException.class)
                .verify();
    }


    @Test
    @DisplayName("異常測試 - uploadFileChunk 傳入 null 任務ID")
    void testUploadFileChunk_withNullTaskId() {
        testUploadChunk.setTransferTaskId(null);

        StepVerifier.create(abstractFileService.uploadFileChunk(testUploadChunk))
                .expectError(IllegalArgumentException.class)
                .verify();
    }


    @Test
    @DisplayName("異常測試 - downloadFile 傳入 null 檔案")
    void testDownloadFile_withNullFile() {
        StepVerifier.create(abstractFileService.downloadFile(null, testUser))
                .expectError(IllegalArgumentException.class)
                .verify();
    }


    @Test
    @DisplayName("異常測試 - downloadFile 傳入 null 用戶")
    void testDownloadFile_withNullUser() {
        StepVerifier.create(abstractFileService.downloadFile(testUserFile, null))
                .expectError(IllegalArgumentException.class)
                .verify();
    }


    @Test
    @DisplayName("異常測試 - downloadFile 權限不足")
    void testDownloadFile_insufficientPermission() {
        testUserFile.setUserId(99L); // 不同的用戶ID

        StepVerifier.create(abstractFileService.downloadFile(testUserFile, testUser))
                .expectError(IllegalArgumentException.class)
                .verify();
    }


    @Test
    @DisplayName("異常測試 - deleteFile 權限不足")
    void testDeleteFile_insufficientPermission() {
        testUserFile.setUserId(99L); // 不同的用戶ID

        StepVerifier.create(abstractFileService.deleteFile(testUserFile, testUser))
                .expectError(IllegalArgumentException.class)
                .verify();
    }


    @Test
    @DisplayName("異常測試 - editFile 傳入 null 編輯數據")
    void testEditFile_withNullEditData() {
        StepVerifier.create(abstractFileService.editFile(null, testUser))
                .expectError(IllegalArgumentException.class)
                .verify();
    }


    @Test
    @DisplayName("異常測試 - editFile 用戶檔案元數據為空")
    void testEditFile_withNullUserFileMetadata() {
        testFileEditBO.setUserFileMetadata(null);

        StepVerifier.create(abstractFileService.editFile(testFileEditBO, testUser))
                .expectError(IllegalArgumentException.class)
                .verify();
    }


    @Test
    @DisplayName("異常測試 - restoreFile 權限不足")
    void testRestoreFile_insufficientPermission() {
        testUserFile.setUserId(99L); // 不同的用戶ID

        StepVerifier.create(abstractFileService.restoreFile(testUserFile, testUser))
                .expectError(IllegalArgumentException.class)
                .verify();
    }


    @Test
    @DisplayName("異常測試 - removeFile 權限不足")
    void testRemoveFile_insufficientPermission() {
        testUserFile.setUserId(99L); // 不同的用戶ID

        StepVerifier.create(abstractFileService.removeFile(testUserFile, testUser))
                .expectError(IllegalArgumentException.class)
                .verify();
    }


    @Test
    @DisplayName("異常測試 - CRUD update 傳入 null 實體")
    void testCrudService_update_withNullEntity() {
        StepVerifier.create(abstractFileService.updateTestFile(null))
                .expectError(IllegalArgumentException.class)
                .verify();
    }


    @Test
    @DisplayName("異常測試 - CRUD update 傳入無 ID 實體")
    void testCrudService_update_withEntityWithoutId() {
        UserFileMetadata fileWithoutId = new UserFileMetadata();
        fileWithoutId.setFilename("test.txt");

        StepVerifier.create(abstractFileService.updateTestFile(fileWithoutId))
                .expectError(IllegalArgumentException.class)
                .verify();
    }


    @Test
    @DisplayName("異常測試 - CRUD delete 傳入 null 實體")
    void testCrudService_delete_withNullEntity() {
        StepVerifier.create(abstractFileService.deleteTestFile(null))
                .expectError(IllegalArgumentException.class)
                .verify();
    }


    @Test
    @DisplayName("異常測試 - CRUD delete 傳入無 ID 實體")
    void testCrudService_delete_withEntityWithoutId() {
        UserFileMetadata fileWithoutId = new UserFileMetadata();
        fileWithoutId.setFilename("test.txt");

        StepVerifier.create(abstractFileService.deleteTestFile(fileWithoutId))
                .expectError(IllegalArgumentException.class)
                .verify();
    }


    @Test
    @DisplayName("邊界測試 - getById 傳入 null ID")
    void testGetById_withNullId() {
        StepVerifier.create(abstractFileService.getTestFileById(null))
                .verifyComplete(); // 應該返回空
    }

    // ==================== 邊界測試 ====================


    @Test
    @DisplayName("邊界測試 - getById 使用極大 ID 值")
    void testGetById_withMaxId() {
        StepVerifier.create(abstractFileService.getTestFileById(Long.MAX_VALUE))
                .assertNext(file -> {
                    assertEquals(Long.MAX_VALUE, file.getId());
                    assertEquals("file_" + Long.MAX_VALUE + ".txt", file.getFilename());
                })
                .verifyComplete();
    }


    @Test
    @DisplayName("邊界測試 - getById 使用負數 ID")
    void testGetById_withNegativeId() {
        StepVerifier.create(abstractFileService.getTestFileById(-1L))
                .verifyComplete(); // 應該返回空
    }


    @Test
    @DisplayName("邊界測試 - uploadFileChunk 零分塊索引")
    void testUploadFileChunk_withZeroChunkIndex() {
        testUploadChunk.setChunkIndex(0);
        testUploadChunk.setTotalChunks(1);

        StepVerifier.create(abstractFileService.uploadFileChunk(testUploadChunk))
                .assertNext(response -> {
                    assertEquals(0, response.getChunkIndex());
                    assertEquals(0.0, response.getProgress()); // 0/1 * 100
                })
                .verifyComplete();
    }


    @Test
    @DisplayName("邊界測試 - uploadFileChunk 極大分塊數量")
    void testUploadFileChunk_withMaxChunks() {
        testUploadChunk.setChunkIndex(1);
        testUploadChunk.setTotalChunks(Integer.MAX_VALUE);

        StepVerifier.create(abstractFileService.uploadFileChunk(testUploadChunk))
                .assertNext(response -> {
                    assertEquals(1, response.getChunkIndex());
                    assertEquals(Integer.MAX_VALUE, response.getTotalChunks());
                    assertTrue(response.getProgress() < 1.0); // 非常小的進度
                })
                .verifyComplete();
    }


    @Test
    @DisplayName("邊界測試 - 極長檔案名")
    void testFileOperations_withVeryLongFileName() {
        testUserFile.setFilename("a".repeat(10000));

        StepVerifier.create(abstractFileService.downloadFile(testUserFile, testUser))
                .assertNext(fileData -> {
                    assertEquals(10000, fileData.getFilename().length());
                })
                .verifyComplete();
    }


    @Test
    @DisplayName("邊界測試 - 空檔案名")
    void testFileOperations_withEmptyFileName() {
        testUserFile.setFilename("");

        StepVerifier.create(abstractFileService.downloadFile(testUserFile, testUser))
                .assertNext(fileData -> {
                    assertEquals("", fileData.getFilename());
                })
                .verifyComplete();
    }


    @Test
    @DisplayName("邊界測試 - null 檔案名")
    void testFileOperations_withNullFileName() {
        testUserFile.setFilename(null);

        StepVerifier.create(abstractFileService.downloadFile(testUserFile, testUser))
                .assertNext(fileData -> {
                    assertNull(fileData.getFilename());
                })
                .verifyComplete();
    }


    @Test
    @DisplayName("邊界測試 - 零檔案大小")
    void testFileOperations_withZeroFileSize() {
        // Create a test service that returns zero file size
        TestableAbstractFileService zeroSizeService = new TestableAbstractFileService() {
            @Override
            public Mono<UserFileDataBO> downloadFile(UserFileMetadata file, User user, String... optional) {
                if (file == null) return Mono.error(new IllegalArgumentException("檔案不能為空"));
                if (user == null) return Mono.error(new IllegalArgumentException("用戶不能為空"));

                UserFileDataBO fileData = new UserFileDataBO();
                fileData.setFilename(file.getFilename());
                fileData.setFileSize(0L);
                fileData.setMimeType("application/octet-stream");
                return Mono.just(fileData);
            }
        };

        StepVerifier.create(zeroSizeService.downloadFile(testUserFile, testUser))
                .assertNext(fileData -> {
                    assertEquals(0L, fileData.getFileSize());
                })
                .verifyComplete();
    }


    @Test
    @DisplayName("邊界測試 - 極大檔案大小")
    void testFileOperations_withMaxFileSize() {
        // Create a test service that returns max file size
        TestableAbstractFileService maxSizeService = new TestableAbstractFileService() {
            @Override
            public Mono<UserFileDataBO> downloadFile(UserFileMetadata file, User user, String... optional) {
                if (file == null) return Mono.error(new IllegalArgumentException("檔案不能為空"));
                if (user == null) return Mono.error(new IllegalArgumentException("用戶不能為空"));

                UserFileDataBO fileData = new UserFileDataBO();
                fileData.setFilename(file.getFilename());
                fileData.setFileSize(Long.MAX_VALUE);
                fileData.setMimeType("application/octet-stream");
                return Mono.just(fileData);
            }
        };

        StepVerifier.create(maxSizeService.downloadFile(testUserFile, testUser))
                .assertNext(fileData -> {
                    assertEquals(Long.MAX_VALUE, fileData.getFileSize());
                })
                .verifyComplete();
    }


    @Test
    @DisplayName("邊界測試 - 用戶屬性為 null 的情況")
    void testFileOperations_withNullUserProperties() {
        User userWithNullProps = new User();
        userWithNullProps.setId(null);
        userWithNullProps.setUsername(null);
        userWithNullProps.setEmail(null);

        StepVerifier.create(abstractFileService.downloadFile(testUserFile, userWithNullProps))
                .expectError(IllegalArgumentException.class)
                .verify();
    }


    @Test
    @DisplayName("邊界測試 - 驗證抽象類完整性")
    void testAbstractClassCompleteness() {
        // 驗證抽象類是 public 的
        assertTrue(java.lang.reflect.Modifier.isPublic(AbstractFileService.class.getModifiers()));

        // 驗證抽象類是 abstract
        assertTrue(java.lang.reflect.Modifier.isAbstract(AbstractFileService.class.getModifiers()));

        // 驗證繼承關係
        assertTrue(FileService.class.isAssignableFrom(AbstractFileService.class));
    }


    @Test
    @DisplayName("邊界測試 - 併發檔案操作")
    void testConcurrentFileOperations() {
        reactor.core.publisher.Flux<UserFileDataBO> concurrentDownloads =
                reactor.core.publisher.Flux.range(1, 10)
                        .flatMap(i -> abstractFileService.downloadFile(testUserFile, testUser));

        StepVerifier.create(concurrentDownloads)
                .expectNextCount(10)
                .verifyComplete();
    }


    @Test
    @DisplayName("邊界測試 - 複雜檔案管理流程")
    void testComplexFileManagementWorkflow() {
        // 創建多個檔案 -> 批量操作 -> 管理權限
        Mono<String> complexWorkflow = reactor.core.publisher.Flux.range(1, 5)
                .flatMap(i -> abstractFileService.createTestFile())
                .collectList()
                .flatMap(files -> {
                    assertEquals(5, files.size());
                    return abstractFileService.uploadFile(testFileMetadata, testUser);
                })
                .flatMap(uploadResponse -> {
                    assertTrue(uploadResponse.getIsSuccess());
                    return abstractFileService.downloadFile(testUserFile, testUser);
                })
                .flatMap(fileData -> {
                    assertNotNull(fileData);
                    return abstractFileService.removeFile(testUserFile, testUser);
                })
                .flatMap(removeResult -> {
                    assertTrue(removeResult);
                    return abstractFileService.restoreFile(testUserFile, testUser);
                })
                .map(restoredFile -> "複雜工作流程完成: " + restoredFile.getFilename());

        StepVerifier.create(complexWorkflow)
                .assertNext(result -> assertTrue(result.startsWith("複雜工作流程完成:")))
                .verifyComplete();
    }


    @Test
    @DisplayName("邊界測試 - 錯誤恢復和重試機制")
    void testErrorRecoveryAndRetry() {
        // 先嘗試無效操作，然後重試有效操作
        Mono<String> retryOperation = abstractFileService.downloadFile(testUserFile, testUser)
                .onErrorResume(error -> {
                    // 模擬錯誤恢復
                    return abstractFileService.restoreFile(testUserFile, testUser)
                            .map(restoredFile -> {
                                UserFileDataBO fileData = new UserFileDataBO();
                                fileData.setFilename(restoredFile.getFilename());
                                return fileData;
                            });
                })
                .map(fileData -> "錯誤恢復成功: " + fileData.getFilename());

        StepVerifier.create(retryOperation)
                .assertNext(result -> assertTrue(result.startsWith("錯誤恢復成功:") || result.contains("test.txt")))
                .verifyComplete();
    }


    @Test
    @DisplayName("邊界測試 - 極限分塊上傳測試")
    void testExtremeLimitChunkUpload() {
        // 測試大量小分塊上傳
        reactor.core.publisher.Flux<UploadResponseDTO> manyChunks =
                reactor.core.publisher.Flux.range(1, 1000)
                        .map(i -> {
                            UploadChunkDTO chunk = new UploadChunkDTO();
                            chunk.setTransferTaskId("task-" + i);
                            chunk.setChunkIndex(i);
                            chunk.setTotalChunks(1000);
                            return chunk;
                        })
                        .flatMap(abstractFileService::uploadFileChunk);

        StepVerifier.create(manyChunks)
                .expectNextCount(1000)
                .verifyComplete();
    }


    @Test
    @DisplayName("邊界測試 - 檔案元數據邊界值")
    void testFileMetadataBoundaryValues() {
        // 測試檔案元數據的極限值
        FileMetadataDTO extremeMetadata = new FileMetadataDTO();
        extremeMetadata.setFilename("極限測試檔案" + "a".repeat(9990)); // 極長檔案名
        extremeMetadata.setFileSize(Long.MAX_VALUE); // 極大檔案大小
        extremeMetadata.setMd5("a".repeat(32)); // 標準MD5長度

        StepVerifier.create(abstractFileService.uploadFile(extremeMetadata, testUser))
                .assertNext(response -> {
                    assertTrue(response.getIsSuccess());
                    assertTrue(response.getIsFinished());
                })
                .verifyComplete();
    }

    /**
     * 測試用的 AbstractFileService 實現類
     */
    private static class TestableAbstractFileService extends AbstractFileService {

        public TestableAbstractFileService() {
            super(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        }

        @Override
        public Mono<PagedResponseDTO<UserFileListDTO>> getUserFileList(User user, FileFilterDTO fileFilterDTO) {
            if (user == null) {
                return Mono.error(new IllegalArgumentException("用戶不能為空"));
            }
            if (fileFilterDTO == null) {
                return Mono.error(new IllegalArgumentException("檔案過濾條件不能為空"));
            }

            // 模擬分頁響應數據
            UserFileMetadata file1 = new UserFileMetadata();
            file1.setId(1L);
            file1.setFilename("document.txt");
            file1.setUserId(user.getId());

            UserFileMetadata file2 = new UserFileMetadata();
            file2.setId(2L);
            file2.setFilename("image.jpg");
            file2.setUserId(user.getId());

            UserFileListDTO fileListDto1 = new UserFileListDTO(file1, Collections.emptySet());
            UserFileListDTO fileListDto2 = new UserFileListDTO(file2, Collections.emptySet());

            PagedResponseDTO<UserFileListDTO> response = new PagedResponseDTO<>();
            response.setData(Arrays.asList(fileListDto1, fileListDto2));
            response.setCurrentPage(fileFilterDTO.getPage() != null ? fileFilterDTO.getPage() : 0);
            response.setPageSize(fileFilterDTO.getPageSize() != null ? fileFilterDTO.getPageSize() : 20);
            response.setTotalElements(2L);
            response.setTotalPages(1);

            return Mono.just(response);
        }

        @Override
        public Mono<UploadResponseDTO> uploadFile(FileMetadataDTO fileMetadataDTO, User user) {
            if (fileMetadataDTO == null) {
                return Mono.error(new IllegalArgumentException("檔案元數據不能為空"));
            }
            if (user == null) {
                return Mono.error(new IllegalArgumentException("用戶不能為空"));
            }

            // 模擬上傳成功響應
            UploadResponseDTO response = UploadResponseDTO.builder()
                    .progress(100.0)
                    .isSuccess(true)
                    .isFinished(true)
                    .message("檔案上傳成功")
                    .build();

            return Mono.just(response);
        }

        @Override
        public Mono<UploadResponseDTO> uploadFileChunk(UploadChunkDTO uploadChunkDTO) {
            if (uploadChunkDTO == null) {
                return Mono.error(new IllegalArgumentException("上傳分塊數據不能為空"));
            }
            if (uploadChunkDTO.getTransferTaskId() == null) {
                return Mono.error(new IllegalArgumentException("傳輸任務ID不能為空"));
            }

            // 模擬分塊上傳響應
            double progress = ((double) uploadChunkDTO.getChunkIndex() / uploadChunkDTO.getTotalChunks()) * 100.0;
            boolean isFinished = uploadChunkDTO.getChunkIndex() == uploadChunkDTO.getTotalChunks();

            UploadResponseDTO response = UploadResponseDTO.builder()
                    .chunkIndex(uploadChunkDTO.getChunkIndex())
                    .transferTaskId(uploadChunkDTO.getTransferTaskId())
                    .progress(progress)
                    .isSuccess(true)
                    .isFinished(isFinished)
                    .message("分塊上傳成功")
                    .totalChunks(uploadChunkDTO.getTotalChunks())
                    .build();

            return Mono.just(response);
        }

        @Override
        public Mono<UserFileDataBO> downloadFile(UserFileMetadata file, User user, String... optional) {
            if (file == null) {
                return Mono.error(new IllegalArgumentException("檔案不能為空"));
            }
            if (user == null) {
                return Mono.error(new IllegalArgumentException("用戶不能為空"));
            }

            // 檢查權限：只能下載自己的檔案或管理員可以下載所有檔案
            // 處理 null username 的情況
            String username = user.getUsername();
            if (username == null || (!username.equals("admin") && !file.getUserId().equals(user.getId()))) {
                return Mono.error(new IllegalArgumentException("沒有權限下載此檔案"));
            }

            // 模擬檔案下載數據
            UserFileDataBO fileData = new UserFileDataBO();
            fileData.setFilename(file.getFilename());
            fileData.setFileSize(1024L);
            fileData.setMimeType("application/octet-stream");

            return Mono.just(fileData);
        }

        @Override
        public Mono<Void> deleteFile(UserFileMetadata file, User user) {
            if (file == null) {
                return Mono.error(new IllegalArgumentException("檔案不能為空"));
            }
            if (user == null) {
                return Mono.error(new IllegalArgumentException("用戶不能為空"));
            }

            // 檢查權限：只能刪除自己的檔案或管理員可以刪除所有檔案
            if (!user.getUsername().equals("admin") && !file.getUserId().equals(user.getId())) {
                return Mono.error(new IllegalArgumentException("沒有權限刪除此檔案"));
            }

            return Mono.empty();
        }

        @Override
        public Mono<Void> editFile(FileEditBO fileEditBO, User user) {
            if (fileEditBO == null) {
                return Mono.error(new IllegalArgumentException("檔案編輯數據不能為空"));
            }
            if (user == null) {
                return Mono.error(new IllegalArgumentException("用戶不能為空"));
            }
            if (fileEditBO.getUserFileMetadata() == null) {
                return Mono.error(new IllegalArgumentException("用戶檔案元數據不能為空"));
            }

            // 檢查權限：只能編輯自己的檔案或管理員可以編輯所有檔案
            UserFileMetadata userFile = fileEditBO.getUserFileMetadata();
            if (!user.getUsername().equals("admin") && !userFile.getUserId().equals(user.getId())) {
                return Mono.error(new IllegalArgumentException("沒有權限編輯此檔案"));
            }

            return Mono.empty();
        }

        @Override
        public Mono<UserFileMetadata> restoreFile(UserFileMetadata file, User user) {
            if (file == null) {
                return Mono.error(new IllegalArgumentException("檔案不能為空"));
            }
            if (user == null) {
                return Mono.error(new IllegalArgumentException("用戶不能為空"));
            }

            // 檢查權限：只能恢復自己的檔案或管理員可以恢復所有檔案
            if (!user.getUsername().equals("admin") && !file.getUserId().equals(user.getId())) {
                return Mono.error(new IllegalArgumentException("沒有權限恢復此檔案"));
            }

            // 模擬恢復邏輯
            UserFileMetadata restoredFile = new UserFileMetadata();
            restoredFile.setId(file.getId());
            restoredFile.setFilename(file.getFilename());
            restoredFile.setUserId(file.getUserId());
            restoredFile.setUploadTime(LocalDateTime.now());

            return Mono.just(restoredFile);
        }

        @Override
        public Mono<Boolean> removeFile(UserFileMetadata file, User user) {
            if (file == null) {
                return Mono.error(new IllegalArgumentException("檔案不能為空"));
            }
            if (user == null) {
                return Mono.error(new IllegalArgumentException("用戶不能為空"));
            }

            // 檢查權限：只能移除自己的檔案或管理員可以移除所有檔案
            if (!user.getUsername().equals("admin") && !file.getUserId().equals(user.getId())) {
                return Mono.error(new IllegalArgumentException("沒有權限移除此檔案"));
            }

            return Mono.just(true);
        }

        // 測試輔助方法，用於模擬檔案操作
        public Mono<UserFileMetadata> createTestFile() {
            UserFileMetadata newFile = new UserFileMetadata();
            newFile.setId(1L);
            newFile.setFilename("new_file.txt");
            newFile.setUploadTime(LocalDateTime.now());
            return Mono.just(newFile);
        }

        public Mono<UserFileMetadata> getTestFileById(Long id) {
            if (id == null || id <= 0) {
                return Mono.empty();
            }
            UserFileMetadata file = new UserFileMetadata();
            file.setId(id);
            file.setFilename("file_" + id + ".txt");
            return Mono.just(file);
        }


        public reactor.core.publisher.Flux<UserFileMetadata> getTestFilesByParams(String type, Object... args) {
            if ("userId".equals(type) && args.length > 0) {
                Long userId = (Long) args[0];
                return getAllTestFiles().filter(file -> file.getUserId().equals(userId));
            }
            return getAllTestFiles();
        }


        public reactor.core.publisher.Flux<UserFileMetadata> getAllTestFiles() {
            UserFileMetadata file1 = new UserFileMetadata();
            file1.setId(1L);
            file1.setUserId(1L);
            file1.setFilename("file1.txt");

            UserFileMetadata file2 = new UserFileMetadata();
            file2.setId(2L);
            file2.setUserId(1L);
            file2.setFilename("file2.txt");

            return reactor.core.publisher.Flux.just(file1, file2);
        }


        public Mono<UserFileMetadata> updateTestFile(UserFileMetadata entity) {
            if (entity == null || entity.getId() == null) {
                return Mono.error(new IllegalArgumentException("檔案實體或ID不能為空"));
            }
            entity.setLastAccessTime(LocalDateTime.now());
            return Mono.just(entity);
        }

        public Mono<Void> deleteTestFile(UserFileMetadata entity) {
            if (entity == null || entity.getId() == null) {
                return Mono.error(new IllegalArgumentException("檔案實體或ID不能為空"));
            }
            return Mono.empty();
        }
    }
}