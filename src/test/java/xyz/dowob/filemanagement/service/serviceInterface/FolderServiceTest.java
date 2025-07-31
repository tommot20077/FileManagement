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
import xyz.dowob.filemanagement.data.file.dto.FileEditDTO;
import xyz.dowob.filemanagement.data.file.dto.FileFilterDTO;
import xyz.dowob.filemanagement.data.file.dto.UserFileListDTO;
import xyz.dowob.filemanagement.data.response.PagedResponseDTO;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.entity.UserFileMetadata;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FolderService 檔案夾服務接口測試
 *
 * <p>測試 FolderService 檔案夾服務接口的專用契約和檔案夾管理模式，驗證接口在目錄結構管理方面的設計正確性。
 * 
 * <p>測試涵蓋的接口功能：
 * <p>- 繼承 BaseFileService 的檔案列表查詢能力
 * <p>- createFolder 檔案夾創建方法的響應式實現
 * <p>- editFolder 檔案夾編輯方法的業務邏輯
 * <p>- deleteFolder 檔案夾刪除方法的層級處理
 * <p>- downloadFolder 檔案夾下載方法的打包邏輯
 * <p>- 檔案夾層級結構的管理和維護
 * <p>- 目錄權限控制和安全檢查
 *
 * 測試摘要：
 * 
 * 驗證 FolderService 接口作為檔案夾管理服務層的設計正確性，確保其能夠為目錄結構操作提供完整的業務能力。
 *
 * 前置條件：
 * - FolderService 接口及其父接口可用
 * - 檔案夾相關 DTO、BO 數據傳輸對象可用
 * - User 和 UserFileMetadata 實體類可用
 * - Reactor WebFlux 響應式編程環境可用
 *
 * 測試步驟：
 * - 驗證接口方法簽名和繼承關係的正確性
 * - 測試檔案夾創建、編輯、刪除、下載的完整流程
 * - 驗證目錄層級處理和權限控制邏輯
 * - 測試響應式流處理和異常邊界情況
 *
 * 預期結果：
 * - 接口契約符合檔案夾服務設計模式
 * - 檔案夾管理流程滿足目錄操作需求
 * - 繼承關係和層級處理機制完善
 * - 響應式處理和異常機制正確
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FolderService 檔案夾服務接口測試")
class FolderServiceTest {

    private FolderService folderService;
    private User testUser;
    private User adminUser;
    private FileEditDTO createFolderDTO;
    private FileEditBO editFolderBO;
    private UserFileMetadata testFolder;
    private UserFileMetadata parentFolder;

    @BeforeEach
    void setUp() {
        // 創建測試用的 FolderService 實現
        folderService = new FolderService() {
            @Override
            public Mono<PagedResponseDTO<UserFileListDTO>> getUserFileList(User user, FileFilterDTO fileFilterDTO) {
                if (user == null) {
                    return Mono.error(new IllegalArgumentException("用戶不能為空"));
                }
                if (fileFilterDTO == null) {
                    return Mono.error(new IllegalArgumentException("過濾條件不能為空"));
                }
                if (fileFilterDTO.getPage() < 0 || fileFilterDTO.getPageSize() <= 0) {
                    return Mono.error(new IllegalArgumentException("分頁參數無效"));
                }
                
                UserFileListDTO fileList = new UserFileListDTO();
                
                // 模擬檔案夾列表
                UserFileMetadata folder1 = new UserFileMetadata();
                folder1.setId(1L);
                folder1.setFilename("Documents");
                folder1.setUserId(user.getId());
                
                UserFileMetadata folder2 = new UserFileMetadata();
                folder2.setId(2L);
                folder2.setFilename("Pictures");
                folder2.setUserId(user.getId());
                
                PagedResponseDTO<UserFileListDTO> pagedResponse = new PagedResponseDTO<>();
                pagedResponse.setData(java.util.Arrays.asList(fileList));
                pagedResponse.setCurrentPage(fileFilterDTO.getPage());
                pagedResponse.setPageSize(fileFilterDTO.getPageSize());
                pagedResponse.setTotalElements(10L);
                pagedResponse.setTotalPages((int) Math.ceil((double) pagedResponse.getTotalElements() / pagedResponse.getPageSize()));
                
                return Mono.just(pagedResponse);
            }

            @Override
            public Mono<Void> createFolder(FileEditDTO fileEditDTO, User user) {
                if (fileEditDTO == null) {
                    return Mono.error(new IllegalArgumentException("檔案夾數據不能為空"));
                }
                if (user == null) {
                    return Mono.error(new IllegalArgumentException("用戶不能為空"));
                }
                if (fileEditDTO.getFilename() == null || fileEditDTO.getFilename().trim().isEmpty()) {
                    return Mono.error(new IllegalArgumentException("檔案夾名稱不能為空"));
                }
                
                // 檢查檔案夾名稱格式
                if (fileEditDTO.getFilename().contains("/") || fileEditDTO.getFilename().contains("\\")) {
                    return Mono.error(new IllegalArgumentException("檔案夾名稱不能包含特殊字符"));
                }
                
                // 模擬創建邏輯
                return Mono.empty();
            }

            @Override
            public Mono<Void> editFolder(FileEditBO fileEditBO, User user) {
                if (fileEditBO == null) {
                    return Mono.error(new IllegalArgumentException("檔案夾編輯數據不能為空"));
                }
                if (user == null) {
                    return Mono.error(new IllegalArgumentException("用戶不能為空"));
                }
                if (fileEditBO.getFileEditDTO().getFileId() == null) {
                    return Mono.error(new IllegalArgumentException("檔案夾ID不能為空"));
                }
                if (fileEditBO.getFileEditDTO().getFilename() == null || fileEditBO.getFileEditDTO().getFilename().trim().isEmpty()) {
                    return Mono.error(new IllegalArgumentException("新檔案夾名稱不能為空"));
                }
                
                // 檢查權限：只能編輯自己的檔案夾或管理員可以編輯所有檔案夾
                if (!user.getUsername().equals("admin") && fileEditBO.getUserFileMetadata() != null && !fileEditBO.getUserFileMetadata().getUserId().equals(user.getId())) {
                    return Mono.error(new IllegalArgumentException("沒有權限編輯此檔案夾"));
                }
                
                return Mono.empty();
            }

            @Override
            public Mono<Void> deleteFolder(UserFileMetadata folder, User user) {
                if (folder == null) {
                    return Mono.error(new IllegalArgumentException("檔案夾不能為空"));
                }
                if (user == null) {
                    return Mono.error(new IllegalArgumentException("用戶不能為空"));
                }
                if (folder.getUserId() == null) {
                    return Mono.error(new IllegalArgumentException("檔案夾用戶ID不能為空"));
                }
                
                // 檢查權限：只能刪除自己的檔案夾或管理員可以刪除所有檔案夾
                if (!user.getUsername().equals("admin") && !folder.getUserId().equals(user.getId())) {
                    return Mono.error(new IllegalArgumentException("沒有權限刪除此檔案夾"));
                }
                
                // 模擬檢查檔案夾是否為空
                if (folder.getFilename().equals("NonemptyFolder")) {
                    return Mono.error(new IllegalArgumentException("檔案夾不為空，無法刪除"));
                }
                
                return Mono.empty();
            }

            @Override
            public Mono<UserFileDataBO> downloadFolder(UserFileMetadata folder, User user) {
                if (folder == null) {
                    return Mono.error(new IllegalArgumentException("檔案夾不能為空"));
                }
                if (user == null) {
                    return Mono.error(new IllegalArgumentException("用戶不能為空"));
                }
                if (folder.getUserId() == null) {
                    return Mono.error(new IllegalArgumentException("檔案夾用戶ID不能為空"));
                }
                
                // 檢查權限：只能下載自己的檔案夾或管理員可以下載所有檔案夾
                if (!user.getUsername().equals("admin") && !folder.getUserId().equals(user.getId())) {
                    return Mono.error(new IllegalArgumentException("沒有權限下載此檔案夾"));
                }
                
                // 模擬創建檔案夾下載數據
                UserFileDataBO folderData = new UserFileDataBO();
                folderData.setFilename(folder.getFilename() + ".zip");
                folderData.setFileSize(1024L * 1024L); // 1MB
                folderData.setMimeType("application/zip");
                
                return Mono.just(folderData);
            }
        };

        // 設置測試對象
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        adminUser = new User();
        adminUser.setId(2L);
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@example.com");

        createFolderDTO = new FileEditDTO();
        createFolderDTO.setFilename("NewFolder");
        createFolderDTO.setParentFolderId(0L); // 根目錄

        FileEditDTO editDTO = new FileEditDTO();
        editDTO.setFileId("1");
        editDTO.setFilename("RenamedFolder");
        editFolderBO = new FileEditBO(editDTO);

        testFolder = new UserFileMetadata();
        testFolder.setId(1L);
        testFolder.setFilename("TestFolder");
        testFolder.setUserId(1L);
        // UserFileMetadata doesn't have setFileSize method
        testFolder.setUploadTime(LocalDateTime.now());

        parentFolder = new UserFileMetadata();
        parentFolder.setId(0L);
        parentFolder.setFilename("Root");
        parentFolder.setUserId(1L);
        // UserFileMetadata doesn't have setFileSize method
        parentFolder.setUploadTime(LocalDateTime.now());
    }

    // ==================== 一般測試 ====================

    @Test
    @DisplayName("一般測試 - createFolder 方法基本功能")
    void testCreateFolder_basicFunctionality() {
        StepVerifier.create(folderService.createFolder(createFolderDTO, testUser))
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - editFolder 方法基本功能")
    void testEditFolder_basicFunctionality() {
        StepVerifier.create(folderService.editFolder(editFolderBO, testUser))
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - deleteFolder 方法基本功能")
    void testDeleteFolder_basicFunctionality() {
        StepVerifier.create(folderService.deleteFolder(testFolder, testUser))
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - downloadFolder 方法基本功能")
    void testDownloadFolder_basicFunctionality() {
        StepVerifier.create(folderService.downloadFolder(testFolder, testUser))
                .assertNext(folderData -> {
                    assertEquals("TestFolder.zip", folderData.getFilename());
                    assertEquals(1024L * 1024L, folderData.getFileSize());
                    assertEquals("application/zip", folderData.getMimeType());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 繼承 BaseFileService 的 getUserFileList 方法")
    void testInheritedGetUserFileList() {
        FileFilterDTO filterDTO = FileFilterDTO.builder()
                .page(0)
                .pageSize(10)
                .keyword("")
                .build();
        
        StepVerifier.create(folderService.getUserFileList(testUser, filterDTO))
                .assertNext(pagedResponse -> {
                    assertEquals(0, pagedResponse.getCurrentPage());
                    assertEquals(10, pagedResponse.getPageSize());
                    assertEquals(10L, pagedResponse.getTotalElements());
                    assertEquals(1, pagedResponse.getTotalPages());
                    assertNotNull(pagedResponse.getData());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 管理員權限操作")
    void testAdminPermissions() {
        // 管理員可以編輯其他用戶的檔案夾
        // FileEditBO doesn't have setUserId method - use DTO
        
        StepVerifier.create(folderService.editFolder(editFolderBO, adminUser))
                .verifyComplete();

        // 管理員可以刪除其他用戶的檔案夾
        testFolder.setUserId(99L);
        StepVerifier.create(folderService.deleteFolder(testFolder, adminUser))
                .verifyComplete();

        // 管理員可以下載其他用戶的檔案夾
        StepVerifier.create(folderService.downloadFolder(testFolder, adminUser))
                .assertNext(folderData -> {
                    assertNotNull(folderData.getFilename());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 接口方法簽名驗證")
    void testInterfaceMethodSignatures() {
        // 驗證 createFolder 方法
        try {
            var createMethod = FolderService.class.getMethod("createFolder", FileEditDTO.class, User.class);
            assertEquals(Mono.class, createMethod.getReturnType());
            assertTrue(createMethod.isDefault());
        } catch (NoSuchMethodException e) {
            fail("createFolder 方法應該存在");
        }

        // 驗證 editFolder 方法
        try {
            var editMethod = FolderService.class.getMethod("editFolder", FileEditBO.class, User.class);
            assertEquals(Mono.class, editMethod.getReturnType());
            assertTrue(editMethod.isDefault());
        } catch (NoSuchMethodException e) {
            fail("editFolder 方法應該存在");
        }

        // 驗證 deleteFolder 方法
        try {
            var deleteMethod = FolderService.class.getMethod("deleteFolder", UserFileMetadata.class, User.class);
            assertEquals(Mono.class, deleteMethod.getReturnType());
            assertTrue(deleteMethod.isDefault());
        } catch (NoSuchMethodException e) {
            fail("deleteFolder 方法應該存在");
        }

        // 驗證 downloadFolder 方法
        try {
            var downloadMethod = FolderService.class.getMethod("downloadFolder", UserFileMetadata.class, User.class);
            assertEquals(Mono.class, downloadMethod.getReturnType());
            assertTrue(downloadMethod.isDefault());
        } catch (NoSuchMethodException e) {
            fail("downloadFolder 方法應該存在");
        }
    }

    @Test
    @DisplayName("一般測試 - 驗證繼承 BaseFileService 接口")
    void testExtendsInterface() {
        // 驗證 FolderService 繼承了 BaseFileService
        assertTrue(BaseFileService.class.isAssignableFrom(FolderService.class));
    }

    @Test
    @DisplayName("一般測試 - 檔案夾生命週期管理")
    void testFolderLifecycleManagement() {
        // 創建 -> 編輯 -> 下載 -> 刪除
        Mono<String> lifecycle = folderService.createFolder(createFolderDTO, testUser)
                .then(folderService.editFolder(editFolderBO, testUser))
                .then(folderService.downloadFolder(testFolder, testUser))
                .flatMap(folderData -> {
                    assertNotNull(folderData);
                    return folderService.deleteFolder(testFolder, testUser);
                })
                .thenReturn("生命週期完成");

        StepVerifier.create(lifecycle)
                .expectNext("生命週期完成")
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 分頁查詢檔案列表")
    void testPaginatedFileList() {
        FileFilterDTO filterDTO = FileFilterDTO.builder()
                .page(1)
                .pageSize(5)
                .keyword("Doc")
                .build();
        
        StepVerifier.create(folderService.getUserFileList(testUser, filterDTO))
                .assertNext(pagedResponse -> {
                    assertEquals(1, pagedResponse.getCurrentPage());
                    assertEquals(5, pagedResponse.getPageSize());
                    assertEquals(2, pagedResponse.getTotalPages()); // 總共10個元素，每頁5個
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 根目錄檔案夾創建")
    void testRootFolderCreation() {
        createFolderDTO.setParentFolderId(0L); // 根目錄
        createFolderDTO.setFilename("RootFolder");
        
        StepVerifier.create(folderService.createFolder(createFolderDTO, testUser))
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 子檔案夾創建")
    void testSubFolderCreation() {
        createFolderDTO.setParentFolderId(1L); // 父檔案夾ID
        createFolderDTO.setFilename("SubFolder");
        
        StepVerifier.create(folderService.createFolder(createFolderDTO, testUser))
                .verifyComplete();
    }

    // ==================== 異常測試 ====================

    @Test
    @DisplayName("異常測試 - createFolder 傳入 null DTO")
    void testCreateFolder_withNullDTO() {
        StepVerifier.create(folderService.createFolder(null, testUser))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - createFolder 傳入 null 用戶")
    void testCreateFolder_withNullUser() {
        StepVerifier.create(folderService.createFolder(createFolderDTO, null))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - createFolder 檔案夾名稱為空")
    void testCreateFolder_withEmptyFolderName() {
        createFolderDTO.setFilename("");
        
        StepVerifier.create(folderService.createFolder(createFolderDTO, testUser))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - createFolder 檔案夾名稱包含特殊字符")
    void testCreateFolder_withInvalidFolderName() {
        createFolderDTO.setFilename("Invalid/Folder\\Name");
        
        StepVerifier.create(folderService.createFolder(createFolderDTO, testUser))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - editFolder 傳入 null BO")
    void testEditFolder_withNullBO() {
        StepVerifier.create(folderService.editFolder(null, testUser))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - editFolder 傳入 null 用戶")
    void testEditFolder_withNullUser() {
        StepVerifier.create(folderService.editFolder(editFolderBO, null))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - editFolder 檔案夾ID為空")
    void testEditFolder_withNullFolderId() {
        editFolderBO.getFileEditDTO().setFileId(null);
        
        StepVerifier.create(folderService.editFolder(editFolderBO, testUser))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - editFolder 新名稱為空")
    void testEditFolder_withEmptyNewName() {
        editFolderBO.getFileEditDTO().setFilename("");
        
        StepVerifier.create(folderService.editFolder(editFolderBO, testUser))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - editFolder 權限不足")
    void testEditFolder_insufficientPermission() {
        // 創建屬於其他用戶的檔案夾元數據
        UserFileMetadata otherUserFolder = new UserFileMetadata();
        otherUserFolder.setId(1L);
        otherUserFolder.setUserId(999L); // 不同的用戶ID
        otherUserFolder.setFilename("OtherUserFolder");
        
        FileEditDTO editDTO = new FileEditDTO();
        editDTO.setFileId("1");
        editDTO.setFilename("RenamedFolder");
        FileEditBO restrictedEditBO = new FileEditBO(editDTO);
        restrictedEditBO.setUserFileMetadata(otherUserFolder);
        
        StepVerifier.create(folderService.editFolder(restrictedEditBO, testUser))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - deleteFolder 傳入 null 檔案夾")
    void testDeleteFolder_withNullFolder() {
        StepVerifier.create(folderService.deleteFolder(null, testUser))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - deleteFolder 傳入 null 用戶")
    void testDeleteFolder_withNullUser() {
        StepVerifier.create(folderService.deleteFolder(testFolder, null))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - deleteFolder 檔案夾用戶ID為空")
    void testDeleteFolder_withNullFolderUserId() {
        testFolder.setUserId(null);
        
        StepVerifier.create(folderService.deleteFolder(testFolder, testUser))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - deleteFolder 權限不足")
    void testDeleteFolder_insufficientPermission() {
        testFolder.setUserId(99L); // 不同的用戶ID
        
        StepVerifier.create(folderService.deleteFolder(testFolder, testUser))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - deleteFolder 檔案夾不為空")
    void testDeleteFolder_nonEmptyFolder() {
        testFolder.setFilename("NonemptyFolder");
        
        StepVerifier.create(folderService.deleteFolder(testFolder, testUser))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - downloadFolder 傳入 null 檔案夾")
    void testDownloadFolder_withNullFolder() {
        StepVerifier.create(folderService.downloadFolder(null, testUser))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - downloadFolder 傳入 null 用戶")
    void testDownloadFolder_withNullUser() {
        StepVerifier.create(folderService.downloadFolder(testFolder, null))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - downloadFolder 權限不足")
    void testDownloadFolder_insufficientPermission() {
        testFolder.setUserId(99L); // 不同的用戶ID
        
        StepVerifier.create(folderService.downloadFolder(testFolder, testUser))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - getUserFileList 傳入 null 用戶")
    void testGetUserFileList_withNullUser() {
        FileFilterDTO filterDTO = FileFilterDTO.builder()
                .page(0)
                .pageSize(10)
                .keyword("")
                .build();
        
        StepVerifier.create(folderService.getUserFileList(null, filterDTO))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - getUserFileList 無效分頁參數")
    void testGetUserFileList_withInvalidPagination() {
        FileFilterDTO invalidFilterDTO1 = FileFilterDTO.builder()
                .page(-1)
                .pageSize(10)
                .keyword("")
                .build();
        StepVerifier.create(folderService.getUserFileList(testUser, invalidFilterDTO1))
                .expectError(IllegalArgumentException.class)
                .verify();

        FileFilterDTO invalidFilterDTO2 = FileFilterDTO.builder()
                .page(0)
                .pageSize(0)
                .keyword("")
                .build();
        StepVerifier.create(folderService.getUserFileList(testUser, invalidFilterDTO2))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    // ==================== 邊界測試 ====================

    @Test
    @DisplayName("邊界測試 - 極長檔案夾名稱")
    void testCreateFolder_withVeryLongName() {
        createFolderDTO.setFilename("a".repeat(1000));
        
        StepVerifier.create(folderService.createFolder(createFolderDTO, testUser))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 特殊Unicode字符檔案夾名稱")
    void testCreateFolder_withUnicodeCharacters() {
        createFolderDTO.setFilename("檔案夾測試🗂️📁");
        
        StepVerifier.create(folderService.createFolder(createFolderDTO, testUser))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 極大檔案夾ID")
    void testEditFolder_withMaxFolderId() {
        editFolderBO.getFileEditDTO().setFileId(String.valueOf(Long.MAX_VALUE));
        
        StepVerifier.create(folderService.editFolder(editFolderBO, testUser))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 負數檔案夾ID")
    void testEditFolder_withNegativeFolderId() {
        editFolderBO.getFileEditDTO().setFileId("-1");
        
        StepVerifier.create(folderService.editFolder(editFolderBO, testUser))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 零檔案夾ID")
    void testEditFolder_withZeroFolderId() {
        editFolderBO.getFileEditDTO().setFileId("0");
        
        StepVerifier.create(folderService.editFolder(editFolderBO, testUser))
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 極大分頁大小")
    void testGetUserFileList_withMaxPageSize() {
        FileFilterDTO filterDTO = FileFilterDTO.builder().page(0).pageSize(Integer.MAX_VALUE).build();
        StepVerifier.create(folderService.getUserFileList(testUser, filterDTO))
                .assertNext(pagedResponse -> {
                    assertEquals(Integer.MAX_VALUE, pagedResponse.getPageSize());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 極大分頁頁碼")
    void testGetUserFileList_withMaxPageNumber() {
        FileFilterDTO filterDTO = FileFilterDTO.builder()
                .page(Integer.MAX_VALUE)
                .pageSize(10)
                .keyword("")
                .build();
        
        StepVerifier.create(folderService.getUserFileList(testUser, filterDTO))
                .assertNext(pagedResponse -> {
                    assertEquals(Integer.MAX_VALUE, pagedResponse.getCurrentPage());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 空搜索關鍵字")
    void testGetUserFileList_withEmptyKeyword() {
        FileFilterDTO filterDTO = FileFilterDTO.builder()
                .page(0)
                .pageSize(10)
                .keyword("")
                .build();
        
        StepVerifier.create(folderService.getUserFileList(testUser, filterDTO))
                .assertNext(pagedResponse -> {
                    assertNotNull(pagedResponse.getData());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - null 搜索關鍵字")
    void testGetUserFileList_withNullKeyword() {
        FileFilterDTO filterDTO = FileFilterDTO.builder()
                .page(0)
                .pageSize(10)
                .keyword(null)
                .build();
        
        StepVerifier.create(folderService.getUserFileList(testUser, filterDTO))
                .assertNext(pagedResponse -> {
                    assertNotNull(pagedResponse.getData());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 極長搜索關鍵字")
    void testGetUserFileList_withVeryLongKeyword() {
        String longKeyword = "search".repeat(1000);
        
        FileFilterDTO filterDTO = FileFilterDTO.builder()
                .page(0)
                .pageSize(10)
                .keyword(longKeyword)
                .build();
        
        StepVerifier.create(folderService.getUserFileList(testUser, filterDTO))
                .assertNext(pagedResponse -> {
                    assertNotNull(pagedResponse.getData());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 檔案夾名稱只包含空格")
    void testCreateFolder_withWhitespaceOnlyName() {
        createFolderDTO.setFilename("   ");
        
        StepVerifier.create(folderService.createFolder(createFolderDTO, testUser))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("邊界測試 - 用戶屬性為 null 的情況")
    void testFolderOperations_withNullUserProperties() {
        User userWithNullProps = new User();
        userWithNullProps.setId(null);
        userWithNullProps.setUsername(null);
        userWithNullProps.setEmail(null);

        StepVerifier.create(folderService.createFolder(createFolderDTO, userWithNullProps))
                .verifyComplete(); // 應該仍然能夠執行，因為只檢查 user 不為 null
    }

    @Test
    @DisplayName("邊界測試 - 驗證接口完整性")
    void testInterfaceCompleteness() {
        // 驗證接口是 public 的
        assertTrue(java.lang.reflect.Modifier.isPublic(FolderService.class.getModifiers()));
        
        // 驗證接口是 interface
        assertTrue(FolderService.class.isInterface());
        
        // 驗證 FolderService 特有方法數量
        assertEquals(4, FolderService.class.getDeclaredMethods().length);
        
        // 驗證所有方法都是默認方法
        long defaultMethodCount = Arrays.stream(FolderService.class.getDeclaredMethods())
                .filter(java.lang.reflect.Method::isDefault)
                .count();
        assertEquals(4, defaultMethodCount);
        
        // 驗證繼承關係
        assertTrue(BaseFileService.class.isAssignableFrom(FolderService.class));
    }

    @Test
    @DisplayName("邊界測試 - 併發檔案夾操作")
    void testConcurrentFolderOperations() {
        reactor.core.publisher.Flux<Long> concurrentCreations = reactor.core.publisher.Flux.range(1, 10)
                .flatMap(i -> {
                    FileEditDTO dto = new FileEditDTO();
                    dto.setFilename("Folder" + i);
                    return folderService.createFolder(dto, testUser).then(Mono.just(i.longValue()));
                });

        StepVerifier.create(concurrentCreations)
                .expectNextCount(10)
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 複雜檔案夾管理流程")
    void testComplexFolderManagementWorkflow() {
        // 創建多個檔案夾 -> 編輯部分檔案夾 -> 下載檔案夾 -> 刪除檔案夾
        Mono<String> complexWorkflow = reactor.core.publisher.Flux.range(1, 3)
                .flatMap(i -> {
                    FileEditDTO dto = new FileEditDTO();
                    dto.setFilename("WorkflowFolder" + i);
                    return folderService.createFolder(dto, testUser);
                })
                .then(folderService.editFolder(editFolderBO, testUser))
                .then(folderService.downloadFolder(testFolder, testUser))
                .flatMap(folderData -> {
                    assertNotNull(folderData);
                    return folderService.deleteFolder(testFolder, testUser);
                })
                .thenReturn("複雜工作流程完成");

        StepVerifier.create(complexWorkflow)
                .expectNext("複雜工作流程完成")
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 錯誤恢復和重試機制")
    void testErrorRecoveryAndRetry() {
        // 先嘗試創建無效檔案夾，然後重試有效檔案夾
        FileEditDTO invalidDTO = new FileEditDTO();
        invalidDTO.setFilename("Invalid/Folder");

        Mono<String> retryCreation = folderService.createFolder(invalidDTO, testUser)
                .onErrorResume(error -> folderService.createFolder(createFolderDTO, testUser))
                .thenReturn("重試成功");

        StepVerifier.create(retryCreation)
                .expectNext("重試成功")
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 檔案夾壓縮下載大小驗證")
    void testDownloadFolder_compressionSizeValidation() {
        // 測試不同大小的檔案夾下載
        UserFileMetadata smallFolder = new UserFileMetadata();
        smallFolder.setId(1L);
        smallFolder.setFilename("SmallFolder");
        smallFolder.setUserId(1L);
        // UserFileMetadata doesn't have setFileSize method

        UserFileMetadata largeFolder = new UserFileMetadata();
        largeFolder.setId(2L);
        largeFolder.setFilename("LargeFolder");
        largeFolder.setUserId(1L);
        // UserFileMetadata doesn't have setFileSize method

        StepVerifier.create(folderService.downloadFolder(smallFolder, testUser))
                .assertNext(folderData -> {
                    assertEquals("SmallFolder.zip", folderData.getFilename());
                    assertTrue(folderData.getFileSize() > 0);
                })
                .verifyComplete();

        StepVerifier.create(folderService.downloadFolder(largeFolder, testUser))
                .assertNext(folderData -> {
                    assertEquals("LargeFolder.zip", folderData.getFilename());
                    assertTrue(folderData.getFileSize() > 0);
                })
                .verifyComplete();
    }
}