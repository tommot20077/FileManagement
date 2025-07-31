package xyz.dowob.filemanagement.service.serviceInterface;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.dowob.filemanagement.data.file.bo.FileEditBO;
import xyz.dowob.filemanagement.data.file.bo.UserFileDataBO;
import xyz.dowob.filemanagement.data.file.dto.FileEditDTO;
import xyz.dowob.filemanagement.data.file.dto.FileMetadataDTO;
import xyz.dowob.filemanagement.data.file.dto.UploadChunkDTO;
import xyz.dowob.filemanagement.data.file.dto.UploadResponseDTO;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.entity.UserFileMetadata;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FileService 檔案服務接口測試
 *
 * <p>測試 FileService 檔案服務接口的組合契約和多重繼承實現模式，驗證接口作為檔案管理服務層核心的設計正確性。
 * 
 * <p>測試涵蓋的接口功能：
 * <p>- 繼承 FileCrudService 的檔案元數據 CRUD 操作
 * <p>- 繼承 BaseFileService 的檔案查詢和列表功能
 * <p>- 實現 RecoverableFile 的檔案恢復和回收機制
 * <p>- uploadFile 檔案上傳方法的響應式實現
 * <p>- uploadFileChunk 分塊上傳方法的進度處理
 * <p>- downloadFile 檔案下載方法的權限控制
 * <p>- deleteFile 檔案刪除方法的安全檢查
 * <p>- editFile 檔案編輯方法的業務邏輯
 * <p>- 默認方法的空實現模式和可覆寫機制
 *
 * 測試摘要：
 * 
 * 驗證 FileService 接口作為檔案管理服務層的統一入口正確性，確保其多重繼承的設計能夠為檔案操作提供完整的業務能力。
 *
 * 前置條件：
 * - FileService 接口及其父接口可用
 * - 檔案相關 DTO、BO 數據傳輸對象可用
 * - User 和 UserFileMetadata 實體類可用
 * - Reactor WebFlux 響應式編程環境可用
 *
 * 測試步驟：
 * - 驗證接口多重繼承的契約和方法簽名
 * - 測試檔案上傳、下載、編輯、刪除的完整流程
 * - 驗證默認方法實現和權限控制邏輯
 * - 測試響應式流處理和異常邊界情況
 *
 * 預期結果：
 * - 接口繼承關係符合檔案服務設計模式
 * - 檔案操作方法實現滿足業務需求
 * - 默認方法和權限控制機制完善
 * - 響應式處理和異常機制正確
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FileService 檔案服務接口測試")
class FileServiceTest {

    private FileService fileService;
    private User testUser;
    private FileMetadataDTO testFileMetadata;
    private UploadChunkDTO testUploadChunk;
    private UserFileMetadata testUserFile;
    private FileEditBO testFileEditBO;

    @BeforeEach
    void setUp() {
        // 創建測試用的 FileService 實現
        fileService = new FileService() {
            @Override
            public Mono<UploadResponseDTO> uploadFile(FileMetadataDTO fileMetadataDTO, User user) {
                if (fileMetadataDTO == null) {
                    return Mono.error(new IllegalArgumentException("檔案元數據不能為空"));
                }
                if (user == null) {
                    return Mono.error(new IllegalArgumentException("用戶不能為空"));
                }
                if (user.getId() == null || user.getUsername() == null) {
                    return Mono.error(new IllegalArgumentException("用戶屬性不能為空"));
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
                if (!user.getUsername().equals("admin") && !file.getUserId().equals(user.getId())) {
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
            public Mono<UserFileMetadata> createUserFileMetadata() {
                return Mono.just(new UserFileMetadata());
            }


            @Override
            public Mono<UserFileMetadata> getUserFileMetadataById(Long id) {
                return Mono.just(testUserFile);
            }


            @Override
            public Flux<UserFileMetadata> getAllUserFileMetadata() {
                return Flux.just(testUserFile);
            }


            @Override
            public Mono<UserFileMetadata> updateUserFileMetadata(UserFileMetadata entity) {
                return Mono.just(entity);
            }


            @Override
            public Mono<Void> deleteUserFileMetadata(UserFileMetadata entity) {
                return Mono.empty();
            }


            @Override
            public Mono<xyz.dowob.filemanagement.entity.ServerFileMetadata> createServerFileMetadata() {
                return Mono.just(new xyz.dowob.filemanagement.entity.ServerFileMetadata());
            }


            @Override
            public Mono<xyz.dowob.filemanagement.entity.ServerFileMetadata> getByServerFileMetadataId(Long id) {
                return Mono.just(new xyz.dowob.filemanagement.entity.ServerFileMetadata());
            }


            @Override
            public Flux<xyz.dowob.filemanagement.entity.ServerFileMetadata> getAllServerFileMetadata() {
                // 實現 FileCrudService 的抽象方法
                return Flux.empty();
            }


            @Override
            public Mono<xyz.dowob.filemanagement.entity.ServerFileMetadata> updateServerFileMetadata(xyz.dowob.filemanagement.entity.ServerFileMetadata entity) {
                // 實現 FileCrudService 的抽象方法
                if (entity == null) {
                    return Mono.error(new IllegalArgumentException("服務器檔案元數據不能為空"));
                }
                return Mono.just(entity);
            }


            @Override
            public Mono<Void> deleteServerFileMetadata(xyz.dowob.filemanagement.entity.ServerFileMetadata entity) {
                // 實現 FileCrudService 的抽象方法
                if (entity == null) {
                    return Mono.error(new IllegalArgumentException("服務器檔案元數據不能為空"));
                }
                return Mono.empty();
            }


            @Override
            public Mono<UserFileMetadata> restoreFile(UserFileMetadata file, User user) {
                return Mono.just(file);
            }

            @Override
            public Flux<UserFileMetadata> restoreFile(Iterable<UserFileMetadata> files, User user) {
                return Flux.fromIterable(files);
            }

            @Override
            public Mono<Boolean> removeFile(UserFileMetadata file, User user) {
                return Mono.just(true);
            }

            @Override
            public Mono<Boolean> removeFile(Iterable<UserFileMetadata> files, User user) {
                return Mono.just(true);
            }
        };

        // 設置測試對象
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

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
        // UserFileMetadata doesn't have setFileSize method
        testUserFile.setUploadTime(LocalDateTime.now());

        testFileEditBO = new FileEditBO(new FileEditDTO());
        testFileEditBO.setUserFileMetadata(testUserFile);
    }

    // ==================== 一般測試 ====================

    @Test
    @DisplayName("一般測試 - uploadFile 方法基本功能")
    void testUploadFile_basicFunctionality() {
        StepVerifier.create(fileService.uploadFile(testFileMetadata, testUser))
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
    @DisplayName("一般測試 - uploadFileChunk 方法基本功能")
    void testUploadFileChunk_basicFunctionality() {
        StepVerifier.create(fileService.uploadFileChunk(testUploadChunk))
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
        
        StepVerifier.create(fileService.uploadFileChunk(testUploadChunk))
                .assertNext(response -> {
                    assertEquals(5, response.getChunkIndex());
                    assertEquals(100.0, response.getProgress()); // 5/5 * 100
                    assertTrue(response.getIsFinished()); // 最後一個分塊，已完成
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - downloadFile 方法基本功能")
    void testDownloadFile_basicFunctionality() {
        StepVerifier.create(fileService.downloadFile(testUserFile, testUser))
                .assertNext(fileData -> {
                    assertNotNull(fileData);
                    assertEquals("test.txt", fileData.getFilename());
                    assertEquals(1024L, fileData.getFileSize());
                    assertEquals("application/octet-stream", fileData.getMimeType());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - deleteFile 方法基本功能")
    void testDeleteFile_basicFunctionality() {
        StepVerifier.create(fileService.deleteFile(testUserFile, testUser))
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - editFile 方法基本功能")
    void testEditFile_basicFunctionality() {
        StepVerifier.create(fileService.editFile(testFileEditBO, testUser))
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 接口方法簽名驗證")
    void testInterfaceMethodSignatures() {
        // 驗證 uploadFile 方法
        try {
            var uploadMethod = FileService.class.getMethod("uploadFile", FileMetadataDTO.class, User.class);
            assertEquals(Mono.class, uploadMethod.getReturnType());
            assertTrue(uploadMethod.isDefault());
        } catch (NoSuchMethodException e) {
            fail("uploadFile 方法應該存在");
        }

        // 驗證 uploadFileChunk 方法
        try {
            var uploadChunkMethod = FileService.class.getMethod("uploadFileChunk", UploadChunkDTO.class);
            assertEquals(Mono.class, uploadChunkMethod.getReturnType());
            assertTrue(uploadChunkMethod.isDefault());
        } catch (NoSuchMethodException e) {
            fail("uploadFileChunk 方法應該存在");
        }

        // 驗證 downloadFile 方法
        try {
            var downloadMethod = FileService.class.getMethod("downloadFile", UserFileMetadata.class, User.class, String[].class);
            assertEquals(Mono.class, downloadMethod.getReturnType());
            assertTrue(downloadMethod.isDefault());
        } catch (NoSuchMethodException e) {
            fail("downloadFile 方法應該存在");
        }

        // 驗證 deleteFile 方法
        try {
            var deleteMethod = FileService.class.getMethod("deleteFile", UserFileMetadata.class, User.class);
            assertEquals(Mono.class, deleteMethod.getReturnType());
            assertTrue(deleteMethod.isDefault());
        } catch (NoSuchMethodException e) {
            fail("deleteFile 方法應該存在");
        }

        // 驗證 editFile 方法
        try {
            var editMethod = FileService.class.getMethod("editFile", FileEditBO.class, User.class);
            assertEquals(Mono.class, editMethod.getReturnType());
            assertTrue(editMethod.isDefault());
        } catch (NoSuchMethodException e) {
            fail("editFile 方法應該存在");
        }
    }

    @Test
    @DisplayName("一般測試 - 驗證繼承關係")
    void testInheritanceRelationships() {
        // 驗證 FileService 繼承了 FileCrudService
        assertTrue(xyz.dowob.filemanagement.unity.FileCrudService.class.isAssignableFrom(FileService.class));
        
        // 驗證 FileService 繼承了 BaseFileService
        assertTrue(BaseFileService.class.isAssignableFrom(FileService.class));
        
        // 驗證 FileService 實現了 RecoverableFile
        assertTrue(RecoverableFile.class.isAssignableFrom(FileService.class));
    }

    @Test
    @DisplayName("一般測試 - 管理員權限操作")
    void testAdminPermissions() {
        User adminUser = new User();
        adminUser.setId(2L);
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@example.com");

        // 管理員可以下載其他用戶的檔案
        testUserFile.setUserId(99L); // 不同的用戶ID
        
        StepVerifier.create(fileService.downloadFile(testUserFile, adminUser))
                .assertNext(fileData -> {
                    assertNotNull(fileData);
                    assertEquals("test.txt", fileData.getFilename());
                })
                .verifyComplete();

        // 管理員可以刪除其他用戶的檔案
        StepVerifier.create(fileService.deleteFile(testUserFile, adminUser))
                .verifyComplete();

        // 管理員可以編輯其他用戶的檔案
        testFileEditBO.getUserFileMetadata().setUserId(99L);
        StepVerifier.create(fileService.editFile(testFileEditBO, adminUser))
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 檔案上傳流程")
    void testFileUploadWorkflow() {
        // 完整檔案上傳 -> 分塊上傳流程
        Mono<String> uploadWorkflow = fileService.uploadFile(testFileMetadata, testUser)
                .flatMap(uploadResponse -> {
                    assertTrue(uploadResponse.getIsSuccess());
                    return fileService.uploadFileChunk(testUploadChunk);
                })
                .flatMap(chunkResponse -> {
                    assertTrue(chunkResponse.getIsSuccess());
                    return fileService.downloadFile(testUserFile, testUser);
                })
                .map(fileData -> "上傳流程完成: " + fileData.getFilename());

        StepVerifier.create(uploadWorkflow)
                .expectNext("上傳流程完成: test.txt")
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 檔案管理生命週期")
    void testFileManagementLifecycle() {
        // 上傳 -> 下載 -> 編輯 -> 刪除
        Mono<String> lifecycle = fileService.uploadFile(testFileMetadata, testUser)
                .flatMap(uploadResponse -> fileService.downloadFile(testUserFile, testUser))
                .flatMap(fileData -> fileService.editFile(testFileEditBO, testUser))
                .then(fileService.deleteFile(testUserFile, testUser))
                .thenReturn("生命週期完成");

        StepVerifier.create(lifecycle)
                .expectNext("生命週期完成")
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 多分塊上傳模擬")
    void testMultipleChunkUpload() {
        // 模擬多個分塊依序上傳
        reactor.core.publisher.Flux<UploadResponseDTO> multiChunkUpload = 
                reactor.core.publisher.Flux.range(1, 5)
                        .map(i -> {
                            UploadChunkDTO chunk = new UploadChunkDTO();
                            chunk.setTransferTaskId("task-multi");
                            chunk.setChunkIndex(i);
                            chunk.setTotalChunks(5);
                            return chunk;
                        })
                        .flatMap(fileService::uploadFileChunk);

        StepVerifier.create(multiChunkUpload)
                .expectNextCount(5)
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 默認方法的空實現驗證")
    void testDefaultMethodEmptyImplementations() {
        // 創建純接口實現，測試默認方法返回空值
        FileService emptyFileService = new FileService() {
            @Override
            public Mono<UserFileMetadata> createUserFileMetadata() {
                return Mono.just(new UserFileMetadata());
            }


            @Override
            public Mono<UserFileMetadata> getUserFileMetadataById(Long id) {
                return Mono.empty();
            }


            @Override
            public Flux<UserFileMetadata> getAllUserFileMetadata() {
                return Flux.empty();
            }


            @Override
            public Mono<UserFileMetadata> updateUserFileMetadata(UserFileMetadata entity) {
                return Mono.just(entity);
            }


            @Override
            public Mono<Void> deleteUserFileMetadata(UserFileMetadata entity) {
                return Mono.empty();
            }


            @Override
            public Mono<xyz.dowob.filemanagement.entity.ServerFileMetadata> createServerFileMetadata() {
                return Mono.just(new xyz.dowob.filemanagement.entity.ServerFileMetadata());
            }


            @Override
            public Mono<xyz.dowob.filemanagement.entity.ServerFileMetadata> getByServerFileMetadataId(Long id) {
                return Mono.empty();
            }


            @Override
            public Flux<xyz.dowob.filemanagement.entity.ServerFileMetadata> getAllServerFileMetadata() {
                return Flux.empty();
            }


            @Override
            public Mono<xyz.dowob.filemanagement.entity.ServerFileMetadata> updateServerFileMetadata(xyz.dowob.filemanagement.entity.ServerFileMetadata entity) {
                return Mono.just(entity);
            }


            @Override
            public Mono<Void> deleteServerFileMetadata(xyz.dowob.filemanagement.entity.ServerFileMetadata entity) {
                return Mono.empty();
            }


            @Override
            public Mono<UserFileMetadata> restoreFile(UserFileMetadata file, User user) {
                return Mono.empty();
            }

            @Override
            public Flux<UserFileMetadata> restoreFile(Iterable<UserFileMetadata> files, User user) {
                return Flux.empty();
            }

            @Override
            public Mono<Boolean> removeFile(UserFileMetadata file, User user) {
                return Mono.just(false);
            }

            @Override
            public Mono<Boolean> removeFile(Iterable<UserFileMetadata> files, User user) {
                return Mono.just(false);
            }
        };
        
        // 測試 uploadFile 默認方法
        StepVerifier.create(emptyFileService.uploadFile(testFileMetadata, testUser))
                .verifyComplete(); // 默認返回 Mono.empty()

        // 測試 uploadFileChunk 默認方法
        StepVerifier.create(emptyFileService.uploadFileChunk(testUploadChunk))
                .verifyComplete(); // 默認返回 Mono.empty()

        // 測試 downloadFile 默認方法
        StepVerifier.create(emptyFileService.downloadFile(testUserFile, testUser))
                .verifyComplete(); // 默認返回 Mono.empty()

        // 測試 deleteFile 默認方法
        StepVerifier.create(emptyFileService.deleteFile(testUserFile, testUser))
                .verifyComplete(); // 默認返回 Mono.empty()

        // 測試 editFile 默認方法
        StepVerifier.create(emptyFileService.editFile(testFileEditBO, testUser))
                .verifyComplete(); // 默認返回 Mono.empty()
    }

    // ==================== 異常測試 ====================

    @Test
    @DisplayName("異常測試 - uploadFile 傳入 null 檔案元數據")
    void testUploadFile_withNullMetadata() {
        StepVerifier.create(fileService.uploadFile(null, testUser))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - uploadFile 傳入 null 用戶")
    void testUploadFile_withNullUser() {
        StepVerifier.create(fileService.uploadFile(testFileMetadata, null))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - uploadFileChunk 傳入 null 分塊數據")
    void testUploadFileChunk_withNullChunkData() {
        StepVerifier.create(fileService.uploadFileChunk(null))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - uploadFileChunk 傳入 null 任務ID")
    void testUploadFileChunk_withNullTaskId() {
        testUploadChunk.setTransferTaskId(null);
        
        StepVerifier.create(fileService.uploadFileChunk(testUploadChunk))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - downloadFile 傳入 null 檔案")
    void testDownloadFile_withNullFile() {
        StepVerifier.create(fileService.downloadFile(null, testUser))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - downloadFile 傳入 null 用戶")
    void testDownloadFile_withNullUser() {
        StepVerifier.create(fileService.downloadFile(testUserFile, null))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - downloadFile 權限不足")
    void testDownloadFile_insufficientPermission() {
        testUserFile.setUserId(99L); // 不同的用戶ID
        
        StepVerifier.create(fileService.downloadFile(testUserFile, testUser))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - deleteFile 傳入 null 檔案")
    void testDeleteFile_withNullFile() {
        StepVerifier.create(fileService.deleteFile(null, testUser))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - deleteFile 權限不足")
    void testDeleteFile_insufficientPermission() {
        testUserFile.setUserId(99L); // 不同的用戶ID
        
        StepVerifier.create(fileService.deleteFile(testUserFile, testUser))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - editFile 傳入 null 編輯數據")
    void testEditFile_withNullEditData() {
        StepVerifier.create(fileService.editFile(null, testUser))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - editFile 用戶檔案元數據為空")
    void testEditFile_withNullUserFileMetadata() {
        testFileEditBO.setUserFileMetadata(null);
        
        StepVerifier.create(fileService.editFile(testFileEditBO, testUser))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - editFile 權限不足")
    void testEditFile_insufficientPermission() {
        testFileEditBO.getUserFileMetadata().setUserId(99L); // 不同的用戶ID
        
        StepVerifier.create(fileService.editFile(testFileEditBO, testUser))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    // ==================== 邊界測試 ====================

    @Test
    @DisplayName("邊界測試 - uploadFileChunk 零分塊索引")
    void testUploadFileChunk_withZeroChunkIndex() {
        testUploadChunk.setChunkIndex(0);
        testUploadChunk.setTotalChunks(1);
        
        StepVerifier.create(fileService.uploadFileChunk(testUploadChunk))
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
        
        StepVerifier.create(fileService.uploadFileChunk(testUploadChunk))
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
        testFileMetadata.setFilename("a".repeat(10000));
        
        StepVerifier.create(fileService.uploadFile(testFileMetadata, testUser))
                .assertNext(response -> {
                    assertTrue(response.getIsSuccess());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 空檔案名")
    void testFileOperations_withEmptyFileName() {
        testFileMetadata.setFilename("");
        
        StepVerifier.create(fileService.uploadFile(testFileMetadata, testUser))
                .assertNext(response -> {
                    assertTrue(response.getIsSuccess());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 零檔案大小")
    void testFileOperations_withZeroFileSize() {
        testFileMetadata.setFileSize(0L);
        
        StepVerifier.create(fileService.uploadFile(testFileMetadata, testUser))
                .assertNext(response -> {
                    assertTrue(response.getIsSuccess());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 極大檔案大小")
    void testFileOperations_withMaxFileSize() {
        testFileMetadata.setFileSize(Long.MAX_VALUE);
        
        StepVerifier.create(fileService.uploadFile(testFileMetadata, testUser))
                .assertNext(response -> {
                    assertTrue(response.getIsSuccess());
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

        StepVerifier.create(fileService.uploadFile(testFileMetadata, userWithNullProps))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("邊界測試 - 驗證接口完整性")
    void testInterfaceCompleteness() {
        // 驗證接口是 public 的
        assertTrue(java.lang.reflect.Modifier.isPublic(FileService.class.getModifiers()));
        
        // 驗證接口是 interface
        assertTrue(FileService.class.isInterface());
        
        // 驗證 FileService 特有方法數量
        assertEquals(5, FileService.class.getDeclaredMethods().length);
        
        // 驗證所有方法都是默認方法
        long defaultMethodCount = Arrays.stream(FileService.class.getDeclaredMethods())
                .filter(java.lang.reflect.Method::isDefault)
                .count();
        assertEquals(5, defaultMethodCount);
        
        // 驗證繼承關係
        assertTrue(xyz.dowob.filemanagement.unity.FileCrudService.class.isAssignableFrom(FileService.class));
        assertTrue(BaseFileService.class.isAssignableFrom(FileService.class));
        assertTrue(RecoverableFile.class.isAssignableFrom(FileService.class));
    }

    @Test
    @DisplayName("邊界測試 - 併發檔案操作")
    void testConcurrentFileOperations() {
        reactor.core.publisher.Flux<UploadResponseDTO> concurrentUploads = 
                reactor.core.publisher.Flux.range(1, 10)
                        .flatMap(i -> fileService.uploadFile(testFileMetadata, testUser));

        StepVerifier.create(concurrentUploads)
                .expectNextCount(10)
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 複雜檔案操作流程")
    void testComplexFileOperationWorkflow() {
        // 多種檔案操作組合流程
        Mono<String> complexWorkflow = reactor.core.publisher.Flux.range(1, 3)
                .flatMap(i -> {
                    FileMetadataDTO metadata = new FileMetadataDTO();
                    metadata.setFilename("file" + i + ".txt");
                    metadata.setFileSize(1024L * i);
                    metadata.setMd5("hash" + i);
                    return fileService.uploadFile(metadata, testUser);
                })
                .collectList()
                .flatMap(uploadResponses -> {
                    assertEquals(3, uploadResponses.size());
                    return fileService.downloadFile(testUserFile, testUser);
                })
                .flatMap(fileData -> {
                    assertNotNull(fileData);
                    return fileService.editFile(testFileEditBO, testUser);
                })
                .then(fileService.deleteFile(testUserFile, testUser))
                .thenReturn("複雜操作流程完成");

        StepVerifier.create(complexWorkflow)
                .expectNext("複雜操作流程完成")
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 錯誤恢復和重試機制")
    void testErrorRecoveryAndRetry() {
        // 先嘗試無效操作，然後重試有效操作
        FileMetadataDTO invalidMetadata = new FileMetadataDTO();
        invalidMetadata.setFilename(null); // 無效檔案名

        Mono<String> retryUpload = fileService.uploadFile(invalidMetadata, testUser)
                .onErrorResume(error -> fileService.uploadFile(testFileMetadata, testUser))
                .map(response -> "重試上傳成功");

        StepVerifier.create(retryUpload)
                .expectNext("重試上傳成功")
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
                        .flatMap(fileService::uploadFileChunk);

        StepVerifier.create(manyChunks)
                .expectNextCount(1000)
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 下載選項參數測試")
    void testDownloadFile_withOptionalParameters() {
        // 測試下載時的可選參數（如Range頭）
        String[] rangeOptions = {"bytes=0-1023", "bytes=1024-2047"};
        
        StepVerifier.create(fileService.downloadFile(testUserFile, testUser, rangeOptions))
                .assertNext(fileData -> {
                    assertNotNull(fileData);
                    assertEquals("test.txt", fileData.getFilename());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 空下載選項參數")
    void testDownloadFile_withEmptyOptionalParameters() {
        String[] emptyOptions = {};
        
        StepVerifier.create(fileService.downloadFile(testUserFile, testUser, emptyOptions))
                .assertNext(fileData -> {
                    assertNotNull(fileData);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - null 下載選項參數")
    void testDownloadFile_withNullOptionalParameters() {
        String[] nullOptions = null;
        
        StepVerifier.create(fileService.downloadFile(testUserFile, testUser, nullOptions))
                .assertNext(fileData -> {
                    assertNotNull(fileData);
                })
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

        StepVerifier.create(fileService.uploadFile(extremeMetadata, testUser))
                .assertNext(response -> {
                    assertTrue(response.getIsSuccess());
                    assertTrue(response.getIsFinished());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 分塊上傳邊界情況")
    void testChunkUploadBoundaryConditions() {
        // 測試單分塊上傳（totalChunks = 1）
        UploadChunkDTO singleChunk = new UploadChunkDTO();
        singleChunk.setTransferTaskId("single-chunk-task");
        singleChunk.setChunkIndex(1);
        singleChunk.setTotalChunks(1);

        StepVerifier.create(fileService.uploadFileChunk(singleChunk))
                .assertNext(response -> {
                    assertEquals(1, response.getChunkIndex());
                    assertEquals(1, response.getTotalChunks());
                    assertEquals(100.0, response.getProgress());
                    assertTrue(response.getIsFinished());
                })
                .verifyComplete();
    }
}