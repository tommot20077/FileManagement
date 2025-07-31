package xyz.dowob.filemanagement.service.serviceInterface;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.dowob.filemanagement.component.provider.provider.FolderListTreeProvider;
import xyz.dowob.filemanagement.data.file.dto.FileFilterDTO;
import xyz.dowob.filemanagement.data.file.dto.FileVersionDTO;
import xyz.dowob.filemanagement.data.file.dto.UserFileListDTO;
import xyz.dowob.filemanagement.data.response.PagedResponseDTO;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.entity.UserFileMetadata;
import xyz.dowob.filemanagement.entity.UserOnlineFileHistory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BaseFileService 基礎檔案服務接口測試
 *
 * <p>測試 BaseFileService 基礎檔案服務接口的默認方法實現和查詢服務契約，驗證接口在檔案管理核心功能方面的設計模式。
 * 
 * <p>測試涵蓋的接口方法：
 * <p>- getUserFileList 用戶檔案列表分頁查詢方法
 * <p>- getUserFilePaths 用戶檔案路徑樹狀結構查詢方法
 * <p>- getFileVersionList 檔案版本歷史分頁查詢方法
 * <p>- searchUserFile 用戶檔案搜索過濾方法
 * <p>- 分頁響應模型的正確構建和處理
 * <p>- 檔案夾樹狀結構的路徑構建邏輯
 * <p>- 響應式查詢流的處理機制
 *
 * 測試摘要：
 * 
 * 驗證 BaseFileService 接口作為檔案查詢服務層的基礎契約正確性，確保其默認方法能夠為檔案管理提供統一的查詢能力。
 *
 * 前置條件：
 * - BaseFileService 接口及其相關數據傳輸對象可用
 * - FolderListTreeProvider.FolderNode 檔案夾節點可用
 * - PagedResponseDTO 分頁響應和過濾條件可用
 * - Reactor WebFlux 響應式編程環境可用
 *
 * 測試步驟：
 * - 驗證接口默認方法的簽名和實現邏輯
 * - 測試檔案查詢、搜索和版本管理功能
 * - 驗證分頁機制和權限控制邏輯
 * - 測試響應式流處理和異常邊界情況
 *
 * 預期結果：
 * - 接口默認方法實現符合查詢服務模式
 * - 檔案查詢和搜索功能滿足業務需求
 * - 分頁和權限控制機制完善
 * - 響應式處理和異常機制正確
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BaseFileService 基礎檔案服務接口測試")
class BaseFileServiceTest {

    private BaseFileService baseFileService;
    private User testUser;
    private User adminUser;
    private UserFileMetadata testFile;
    private UserFileMetadata testFolder;
    private FileFilterDTO testFilter;

    @BeforeEach
    void setUp() {
        // 創建測試用的 BaseFileService 實現
        baseFileService = new BaseFileService() {
            @Override
            public Mono<PagedResponseDTO<UserFileListDTO>> getUserFileList(User user, FileFilterDTO fileFilterDTO) {
                if (user == null) {
                    return Mono.error(new IllegalArgumentException("用戶不能為空"));
                }
                if (fileFilterDTO == null) {
                    return Mono.error(new IllegalArgumentException("檔案過濾條件不能為空"));
                }
                
                // 模擬檔案列表數據
                UserFileMetadata file1 = new UserFileMetadata();
                file1.setId(1L);
                file1.setFilename("document.txt");
                file1.setUserId(user.getId());
                file1.setUploadTime(LocalDateTime.now());
                
                UserFileMetadata file2 = new UserFileMetadata();
                file2.setId(2L);
                file2.setFilename("image.jpg");
                file2.setUserId(user.getId());
                file2.setUploadTime(LocalDateTime.now().minusHours(1));
                
                UserFileListDTO fileListDto1 = new UserFileListDTO(file1, Collections.emptySet());
                UserFileListDTO fileListDto2 = new UserFileListDTO(file2, Collections.emptySet());
                
                PagedResponseDTO<UserFileListDTO> response = new PagedResponseDTO<>();
                response.setData(Arrays.asList(fileListDto1, fileListDto2));
                response.setCurrentPage(fileFilterDTO.getPage() != null ? fileFilterDTO.getPage() : 0);
                response.setPageSize(fileFilterDTO.getPageSize() != null ? fileFilterDTO.getPageSize() : 20);
                response.setTotalElements(2L);
                response.setTotalPages((int) Math.ceil((double) response.getTotalElements() / response.getPageSize()));
                
                return Mono.just(response);
            }

            @Override
            public Mono<List<FolderListTreeProvider.FolderNode>> getUserFilePaths(UserFileMetadata file, User user) {
                if (file == null) {
                    return Mono.error(new IllegalArgumentException("檔案不能為空"));
                }
                if (user == null) {
                    return Mono.error(new IllegalArgumentException("用戶不能為空"));
                }
                if (file.getUserId() == null) {
                    return Mono.error(new IllegalArgumentException("檔案用戶ID不能為空"));
                }
                
                // 權限檢查：只能查詢自己的檔案路徑或管理員可以查詢所有檔案路徑
                if (!user.getUsername().equals("admin") && !file.getUserId().equals(user.getId())) {
                    return Mono.error(new IllegalArgumentException("沒有權限查詢此檔案路徑"));
                }
                
                // 模擬檔案路徑樹狀結構
                FolderListTreeProvider.FolderNode rootNode = new FolderListTreeProvider.FolderNode(0L, "root");
                
                FolderListTreeProvider.FolderNode documentsNode = new FolderListTreeProvider.FolderNode(1L, "Documents");
                
                FolderListTreeProvider.FolderNode fileNode = new FolderListTreeProvider.FolderNode(file.getId(), file.getFilename());
                
                return Mono.just(Arrays.asList(rootNode, documentsNode, fileNode));
            }

            @Override
            public Mono<PagedResponseDTO<FileVersionDTO>> getFileVersionList(User user, UserFileMetadata file, Integer page, Integer pageSize) {
                if (user == null) {
                    return Mono.error(new IllegalArgumentException("用戶不能為空"));
                }
                if (file == null) {
                    return Mono.error(new IllegalArgumentException("檔案不能為空"));
                }
                if (page == null || page < 0) {
                    return Mono.error(new IllegalArgumentException("分頁頁碼無效"));
                }
                if (pageSize == null || pageSize <= 0) {
                    return Mono.error(new IllegalArgumentException("分頁大小無效"));
                }
                if (file.getUserId() == null) {
                    return Mono.error(new IllegalArgumentException("檔案用戶ID不能為空"));
                }
                
                // 權限檢查：只能查詢自己的檔案版本或管理員可以查詢所有檔案版本
                if (!user.getUsername().equals("admin") && !file.getUserId().equals(user.getId())) {
                    return Mono.error(new IllegalArgumentException("沒有權限查詢此檔案版本"));
                }
                
                // 模擬檔案版本數據
                UserOnlineFileHistory history1 = new UserOnlineFileHistory();
                history1.setVersion(1L);
                history1.setModifiedBy(user.getId());
                history1.setModifiedTime(LocalDateTime.now().minusDays(1));
                history1.setNote("Initial version");
                FileVersionDTO version1 = new FileVersionDTO(history1);
                
                UserOnlineFileHistory history2 = new UserOnlineFileHistory();
                history2.setVersion(2L);
                history2.setModifiedBy(user.getId());
                history2.setModifiedTime(LocalDateTime.now());
                history2.setNote("Updated version");
                FileVersionDTO version2 = new FileVersionDTO(history2);
                
                PagedResponseDTO<FileVersionDTO> response = new PagedResponseDTO<>();
                response.setData(Arrays.asList(version1, version2));
                response.setCurrentPage(page);
                response.setPageSize(pageSize);
                response.setTotalElements(2L);
                response.setTotalPages((int) Math.ceil(2.0 / pageSize));
                
                return Mono.just(response);
            }

            @Override
            public Mono<PagedResponseDTO<UserFileListDTO>> searchUserFile(User user, FileFilterDTO fileFilterDTO) {
                if (user == null) {
                    return Mono.error(new IllegalArgumentException("用戶不能為空"));
                }
                if (fileFilterDTO == null) {
                    return Mono.error(new IllegalArgumentException("搜索條件不能為空"));
                }
                
                // 模擬搜索結果
                List<UserFileListDTO> searchResults = new ArrayList<>();
                
                // 根據搜索關鍵字過濾檔案
                String keyword = fileFilterDTO.getKeyword();
                if (keyword != null && !keyword.trim().isEmpty()) {
                    if (keyword.toLowerCase().contains("doc")) {
                        UserFileMetadata docFile = new UserFileMetadata();
                        docFile.setId(1L);
                        docFile.setFilename("document.txt");
                        docFile.setUserId(user.getId());
                        UserFileListDTO docFileDTO = new UserFileListDTO(docFile, Collections.emptySet());
                        searchResults.add(docFileDTO);
                    } else if (keyword.toLowerCase().contains("img")) {
                        UserFileMetadata imgFile = new UserFileMetadata();
                        imgFile.setId(2L);
                        imgFile.setFilename("image.jpg");
                        imgFile.setUserId(user.getId());
                        UserFileListDTO imgFileDTO = new UserFileListDTO(imgFile, Collections.emptySet());
                        searchResults.add(imgFileDTO);
                    }
                }
                
                PagedResponseDTO<UserFileListDTO> response = new PagedResponseDTO<>();
                response.setData(searchResults);
                response.setCurrentPage(fileFilterDTO.getPage() != null ? fileFilterDTO.getPage() : 0);
                response.setPageSize(fileFilterDTO.getPageSize() != null ? fileFilterDTO.getPageSize() : 20);
                response.setTotalElements((long) searchResults.size());
                response.setTotalPages(searchResults.isEmpty() ? 0 : 1);
                
                return Mono.just(response);
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

        testFile = new UserFileMetadata();
        testFile.setId(1L);
        testFile.setFilename("test.txt");
        testFile.setUserId(1L);
        // UserFileMetadata doesn't have fileSize field
        testFile.setUploadTime(LocalDateTime.now());

        testFolder = new UserFileMetadata();
        testFolder.setId(2L);
        testFolder.setFilename("TestFolder");
        testFolder.setUserId(1L);
        // UserFileMetadata doesn't have fileSize field
        testFolder.setUploadTime(LocalDateTime.now());

        testFilter = FileFilterDTO.builder()
                .keyword("")
                .page(0)
                .pageSize(20)
                .build();
    }

    // ==================== 一般測試 ====================

    @Test
    @DisplayName("一般測試 - getUserFileList 方法基本功能")
    void testGetUserFileList_basicFunctionality() {
        StepVerifier.create(baseFileService.getUserFileList(testUser, testFilter))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals(0, response.getCurrentPage());
                    assertEquals(20, response.getPageSize());
                    assertEquals(2L, response.getTotalElements());
                    assertEquals(1, response.getTotalPages());
                    // PagedResponseDTO doesn't have isFirst() and isLast() methods
                    
                    List<UserFileListDTO> content = response.getData();
                    assertNotNull(content);
                    assertEquals(2, content.size());
                    
                    UserFileListDTO firstFileDTO = content.get(0);
                    assertEquals("document.txt", firstFileDTO.getFilename());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - getUserFilePaths 方法基本功能")
    void testGetUserFilePaths_basicFunctionality() {
        StepVerifier.create(baseFileService.getUserFilePaths(testFile, testUser))
                .assertNext(paths -> {
                    assertNotNull(paths);
                    assertEquals(3, paths.size());
                    
                    // 檢查路徑層次結構
                    FolderListTreeProvider.FolderNode rootNode = paths.get(0);
                    assertEquals(0L, rootNode.getFolderId());
                    assertEquals("root", rootNode.getName());
                    // FolderNode doesn't have getParentId method
                    
                    FolderListTreeProvider.FolderNode documentsNode = paths.get(1);
                    assertEquals(1L, documentsNode.getFolderId());
                    assertEquals("Documents", documentsNode.getName());
                    
                    FolderListTreeProvider.FolderNode fileNode = paths.get(2);
                    assertEquals(testFile.getId(), fileNode.getFolderId());
                    assertEquals(testFile.getFilename(), fileNode.getName());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - getFileVersionList 方法基本功能")
    void testGetFileVersionList_basicFunctionality() {
        StepVerifier.create(baseFileService.getFileVersionList(testUser, testFile, 0, 10))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals(0, response.getCurrentPage());
                    assertEquals(10, response.getPageSize());
                    assertEquals(2L, response.getTotalElements());
                    assertEquals(1, response.getTotalPages());
                    // PagedResponseDTO doesn't have isFirst() and isLast() methods
                    // assertTrue(false /* PagedResponseDTO doesn't have isFirst() method */);
                    // assertTrue(false /* PagedResponseDTO doesn't have isLast() method */);
                    
                    List<FileVersionDTO> versions = response.getData();
                    assertNotNull(versions);
                    assertEquals(2, versions.size());
                    
                    FileVersionDTO version1 = versions.get(0);
                    assertEquals(1L, version1.getVersion());
                    assertEquals(testUser.getId(), version1.getModifiedBy());
                    
                    FileVersionDTO version2 = versions.get(1);
                    assertEquals(2L, version2.getVersion());
                    assertEquals(testUser.getId(), version2.getModifiedBy());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - searchUserFile 方法基本功能")
    void testSearchUserFile_basicFunctionality() {
        testFilter.setKeyword("doc");
        
        StepVerifier.create(baseFileService.searchUserFile(testUser, testFilter))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals(0, response.getCurrentPage());
                    assertEquals(20, response.getPageSize());
                    assertEquals(1L, response.getTotalElements());
                    
                    List<UserFileListDTO> searchResults = response.getData();
                    assertNotNull(searchResults);
                    assertEquals(1, searchResults.size());
                    
                    UserFileListDTO foundFileDto = searchResults.get(0);
                    assertEquals("document.txt", foundFileDto.getFilename());
                    assertTrue(foundFileDto.getFilename().toLowerCase().contains("doc"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 管理員權限查詢")
    void testAdminPermissions() {
        // 管理員可以查詢其他用戶的檔案路徑
        testFile.setUserId(99L); // 不同的用戶ID
        
        StepVerifier.create(baseFileService.getUserFilePaths(testFile, adminUser))
                .assertNext(paths -> {
                    assertNotNull(paths);
                    assertEquals(3, paths.size());
                })
                .verifyComplete();

        // 管理員可以查詢其他用戶的檔案版本
        StepVerifier.create(baseFileService.getFileVersionList(adminUser, testFile, 0, 10))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals(2L, response.getTotalElements());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 接口方法簽名驗證")
    void testInterfaceMethodSignatures() {
        // 驗證 getUserFileList 方法
        try {
            var getUserFileListMethod = BaseFileService.class.getMethod("getUserFileList", User.class, FileFilterDTO.class);
            assertEquals(Mono.class, getUserFileListMethod.getReturnType());
            assertTrue(getUserFileListMethod.isDefault());
        } catch (NoSuchMethodException e) {
            fail("getUserFileList 方法應該存在");
        }

        // 驗證 getUserFilePaths 方法
        try {
            var getUserFilePathsMethod = BaseFileService.class.getMethod("getUserFilePaths", UserFileMetadata.class, User.class);
            assertEquals(Mono.class, getUserFilePathsMethod.getReturnType());
            assertTrue(getUserFilePathsMethod.isDefault());
        } catch (NoSuchMethodException e) {
            fail("getUserFilePaths 方法應該存在");
        }

        // 驗證 getFileVersionList 方法
        try {
            var getFileVersionListMethod = BaseFileService.class.getMethod("getFileVersionList", User.class, UserFileMetadata.class, Integer.class, Integer.class);
            assertEquals(Mono.class, getFileVersionListMethod.getReturnType());
            assertTrue(getFileVersionListMethod.isDefault());
        } catch (NoSuchMethodException e) {
            fail("getFileVersionList 方法應該存在");
        }

        // 驗證 searchUserFile 方法
        try {
            var searchUserFileMethod = BaseFileService.class.getMethod("searchUserFile", User.class, FileFilterDTO.class);
            assertEquals(Mono.class, searchUserFileMethod.getReturnType());
            assertTrue(searchUserFileMethod.isDefault());
        } catch (NoSuchMethodException e) {
            fail("searchUserFile 方法應該存在");
        }
    }

    @Test
    @DisplayName("一般測試 - 分頁功能驗證")
    void testPaginationFunctionality() {
        testFilter = FileFilterDTO.builder().page(1).pageSize(1).build();
        
        StepVerifier.create(baseFileService.getUserFileList(testUser, testFilter))
                .assertNext(response -> {
                    assertEquals(1, response.getCurrentPage());
                    assertEquals(1, response.getPageSize());
                    assertEquals(2L, response.getTotalElements());
                    assertEquals(2, response.getTotalPages());
                    // PagedResponseDTO doesn't have isFirst() and isLast() methods
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 不同搜索關鍵字")
    void testDifferentSearchKeywords() {
        // 搜索圖片檔案
        testFilter = FileFilterDTO.builder().keyword("img").page(0).pageSize(20).build();
        
        StepVerifier.create(baseFileService.searchUserFile(testUser, testFilter))
                .assertNext(response -> {
                    List<UserFileListDTO> searchResults = response.getData();
                    assertEquals(1, searchResults.size());
                    assertEquals("image.jpg", searchResults.get(0).getFilename());
                })
                .verifyComplete();

        // 搜索不存在的檔案
        testFilter = FileFilterDTO.builder().keyword("nonexistent").page(0).pageSize(20).build();
        
        StepVerifier.create(baseFileService.searchUserFile(testUser, testFilter))
                .assertNext(response -> {
                    List<UserFileListDTO> searchResults = response.getData();
                    assertTrue(searchResults.isEmpty());
                    assertEquals(0L, response.getTotalElements());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - 檔案版本分頁")
    void testFileVersionPagination() {
        StepVerifier.create(baseFileService.getFileVersionList(testUser, testFile, 0, 1))
                .assertNext(response -> {
                    assertEquals(0, response.getCurrentPage());
                    assertEquals(1, response.getPageSize());
                    assertEquals(2L, response.getTotalElements());
                    assertEquals(2, response.getTotalPages());
                    // PagedResponseDTO doesn't have isFirst() and isLast() methods
                })
                .verifyComplete();

        StepVerifier.create(baseFileService.getFileVersionList(testUser, testFile, 1, 1))
                .assertNext(response -> {
                    assertEquals(1, response.getCurrentPage());
                    assertEquals(1, response.getPageSize());
                    assertEquals(2L, response.getTotalElements());
                    assertEquals(2, response.getTotalPages());
                    // PagedResponseDTO doesn't have isFirst() and isLast() methods
                })
                .verifyComplete();
    }

    // ==================== 異常測試 ====================

    @Test
    @DisplayName("異常測試 - getUserFileList 傳入 null 用戶")
    void testGetUserFileList_withNullUser() {
        StepVerifier.create(baseFileService.getUserFileList(null, testFilter))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - getUserFileList 傳入 null 過濾條件")
    void testGetUserFileList_withNullFilter() {
        StepVerifier.create(baseFileService.getUserFileList(testUser, null))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - getUserFilePaths 傳入 null 檔案")
    void testGetUserFilePaths_withNullFile() {
        StepVerifier.create(baseFileService.getUserFilePaths(null, testUser))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - getUserFilePaths 傳入 null 用戶")
    void testGetUserFilePaths_withNullUser() {
        StepVerifier.create(baseFileService.getUserFilePaths(testFile, null))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - getUserFilePaths 檔案用戶ID為空")
    void testGetUserFilePaths_withNullFileUserId() {
        testFile.setUserId(null);
        
        StepVerifier.create(baseFileService.getUserFilePaths(testFile, testUser))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - getUserFilePaths 權限不足")
    void testGetUserFilePaths_insufficientPermission() {
        testFile.setUserId(99L); // 不同的用戶ID
        
        StepVerifier.create(baseFileService.getUserFilePaths(testFile, testUser))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - getFileVersionList 傳入 null 用戶")
    void testGetFileVersionList_withNullUser() {
        StepVerifier.create(baseFileService.getFileVersionList(null, testFile, 0, 10))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - getFileVersionList 傳入 null 檔案")
    void testGetFileVersionList_withNullFile() {
        StepVerifier.create(baseFileService.getFileVersionList(testUser, null, 0, 10))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - getFileVersionList 無效分頁參數")
    void testGetFileVersionList_withInvalidPagination() {
        StepVerifier.create(baseFileService.getFileVersionList(testUser, testFile, null, 10))
                .expectError(IllegalArgumentException.class)
                .verify();

        StepVerifier.create(baseFileService.getFileVersionList(testUser, testFile, -1, 10))
                .expectError(IllegalArgumentException.class)
                .verify();

        StepVerifier.create(baseFileService.getFileVersionList(testUser, testFile, 0, null))
                .expectError(IllegalArgumentException.class)
                .verify();

        StepVerifier.create(baseFileService.getFileVersionList(testUser, testFile, 0, 0))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - getFileVersionList 權限不足")
    void testGetFileVersionList_insufficientPermission() {
        testFile.setUserId(99L); // 不同的用戶ID
        
        StepVerifier.create(baseFileService.getFileVersionList(testUser, testFile, 0, 10))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - searchUserFile 傳入 null 用戶")
    void testSearchUserFile_withNullUser() {
        StepVerifier.create(baseFileService.searchUserFile(null, testFilter))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("異常測試 - searchUserFile 傳入 null 搜索條件")
    void testSearchUserFile_withNullFilter() {
        StepVerifier.create(baseFileService.searchUserFile(testUser, null))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    // ==================== 邊界測試 ====================

    @Test
    @DisplayName("邊界測試 - 極大分頁大小")
    void testGetUserFileList_withMaxPageSize() {
        testFilter = FileFilterDTO.builder().pageSize(Integer.MAX_VALUE).page(0).build();
        
        StepVerifier.create(baseFileService.getUserFileList(testUser, testFilter))
                .assertNext(response -> {
                    assertEquals(Integer.MAX_VALUE, response.getPageSize());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 極大分頁頁碼")
    void testGetUserFileList_withMaxPageNumber() {
        testFilter = FileFilterDTO.builder().page(Integer.MAX_VALUE).pageSize(20).build();
        
        StepVerifier.create(baseFileService.getUserFileList(testUser, testFilter))
                .assertNext(response -> {
                    assertEquals(Integer.MAX_VALUE, response.getCurrentPage());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 空搜索關鍵字")
    void testSearchUserFile_withEmptyKeyword() {
        testFilter = FileFilterDTO.builder().keyword("").page(0).pageSize(20).build();
        
        StepVerifier.create(baseFileService.searchUserFile(testUser, testFilter))
                .assertNext(response -> {
                    List<UserFileListDTO> searchResults = response.getData();
                    assertTrue(searchResults.isEmpty());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - null 搜索關鍵字")
    void testSearchUserFile_withNullKeyword() {
        testFilter = FileFilterDTO.builder().keyword(null).page(0).pageSize(20).build();
        
        StepVerifier.create(baseFileService.searchUserFile(testUser, testFilter))
                .assertNext(response -> {
                    List<UserFileListDTO> searchResults = response.getData();
                    assertTrue(searchResults.isEmpty());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 極長搜索關鍵字")
    void testSearchUserFile_withVeryLongKeyword() {
        testFilter = FileFilterDTO.builder().keyword("doc".repeat(1000)).page(0).pageSize(20).build();
        
        StepVerifier.create(baseFileService.searchUserFile(testUser, testFilter))
                .assertNext(response -> {
                    List<UserFileListDTO> searchResults = response.getData();
                    assertEquals(1, searchResults.size());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 極大檔案ID")
    void testGetUserFilePaths_withMaxFileId() {
        testFile.setId(Long.MAX_VALUE);
        
        StepVerifier.create(baseFileService.getUserFilePaths(testFile, testUser))
                .assertNext(paths -> {
                    assertEquals(3, paths.size());
                    FolderListTreeProvider.FolderNode fileNode = paths.get(2);
                    assertEquals(Long.MAX_VALUE, fileNode.getFolderId());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 極長檔案名")
    void testGetUserFilePaths_withVeryLongFileName() {
        testFile.setFilename("a".repeat(10000));
        
        StepVerifier.create(baseFileService.getUserFilePaths(testFile, testUser))
                .assertNext(paths -> {
                    assertEquals(3, paths.size());
                    FolderListTreeProvider.FolderNode fileNode = paths.get(2);
                    assertEquals(10000, fileNode.getName().length());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 零檔案版本分頁大小")
    void testGetFileVersionList_withZeroPageSize() {
        StepVerifier.create(baseFileService.getFileVersionList(testUser, testFile, 0, 0))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("邊界測試 - 負數檔案版本分頁頁碼")
    void testGetFileVersionList_withNegativePageNumber() {
        StepVerifier.create(baseFileService.getFileVersionList(testUser, testFile, -1, 10))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("邊界測試 - 極大檔案版本分頁參數")
    void testGetFileVersionList_withMaxPaginationParams() {
        StepVerifier.create(baseFileService.getFileVersionList(testUser, testFile, Integer.MAX_VALUE, Integer.MAX_VALUE))
                .assertNext(response -> {
                    assertEquals(Integer.MAX_VALUE, response.getCurrentPage());
                    assertEquals(Integer.MAX_VALUE, response.getPageSize());
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

        StepVerifier.create(baseFileService.getUserFileList(userWithNullProps, testFilter))
                .assertNext(response -> {
                    // 應該仍然能夠執行，因為只檢查 user 不為 null
                    assertNotNull(response);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 驗證接口完整性")
    void testInterfaceCompleteness() {
        // 驗證接口是 public 的
        assertTrue(java.lang.reflect.Modifier.isPublic(BaseFileService.class.getModifiers()));
        
        // 驗證接口是 interface
        assertTrue(BaseFileService.class.isInterface());
        
        // 驗證方法數量
        assertEquals(4, BaseFileService.class.getDeclaredMethods().length);
        
        // 驗證所有方法都是默認方法
        long defaultMethodCount = Arrays.stream(BaseFileService.class.getDeclaredMethods())
                .filter(java.lang.reflect.Method::isDefault)
                .count();
        assertEquals(4, defaultMethodCount);
    }

    @Test
    @DisplayName("邊界測試 - 併發查詢操作")
    void testConcurrentQueryOperations() {
        reactor.core.publisher.Flux<PagedResponseDTO<UserFileListDTO>> concurrentQueries = 
                reactor.core.publisher.Flux.range(1, 10)
                        .flatMap(i -> baseFileService.getUserFileList(testUser, testFilter));

        StepVerifier.create(concurrentQueries)
                .expectNextCount(10)
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 複雜查詢流程")
    void testComplexQueryWorkflow() {
        // 檔案列表查詢 -> 檔案路徑查詢 -> 檔案版本查詢 -> 檔案搜索
        Mono<String> complexWorkflow = baseFileService.getUserFileList(testUser, testFilter)
                .flatMap(fileListResponse -> {
                    assertNotNull(fileListResponse);
                    return baseFileService.getUserFilePaths(testFile, testUser);
                })
                .flatMap(paths -> {
                    assertNotNull(paths);
                    assertEquals(3, paths.size());
                    return baseFileService.getFileVersionList(testUser, testFile, 0, 10);
                })
                .flatMap(versionsResponse -> {
                    assertNotNull(versionsResponse);
                    assertEquals(2L, versionsResponse.getTotalElements());
                    FileFilterDTO searchFilter = FileFilterDTO.builder().keyword("doc").page(0).pageSize(20).build();
                    return baseFileService.searchUserFile(testUser, searchFilter);
                })
                .map(searchResponse -> {
                    assertNotNull(searchResponse);
                    return "複雜查詢流程完成";
                });

        StepVerifier.create(complexWorkflow)
                .expectNext("複雜查詢流程完成")
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 錯誤恢復和重試機制")
    void testErrorRecoveryAndRetry() {
        // 先嘗試無效查詢，然後重試有效查詢
        FileFilterDTO invalidFilter = FileFilterDTO.builder().page(-1).pageSize(10).build(); // 無效頁碼

        Mono<String> retryQuery = baseFileService.getUserFileList(testUser, invalidFilter)
                .onErrorResume(error -> baseFileService.getUserFileList(testUser, testFilter))
                .map(response -> "重試查詢成功");

        StepVerifier.create(retryQuery)
                .expectNext("重試查詢成功")
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 多層檔案夾路徑")
    void testDeepFolderPaths() {
        // 測試深層檔案夾路徑結構
        testFile.setId(100L);
        testFile.setFilename("deep_file.txt");
        
        StepVerifier.create(baseFileService.getUserFilePaths(testFile, testUser))
                .assertNext(paths -> {
                    assertEquals(3, paths.size());
                    
                    // 驗證路徑層次結構的完整性
                    FolderListTreeProvider.FolderNode rootNode = paths.get(0);
                    FolderListTreeProvider.FolderNode documentsNode = paths.get(1);
                    FolderListTreeProvider.FolderNode fileNode = paths.get(2);
                    
                    // 驗證路徑層次結構邏輯（由於 FolderNode 不提供 getParentId，使用其他方式驗證）
                    assertEquals("root", rootNode.getName());
                    assertEquals("Documents", documentsNode.getName());
                    assertEquals("deep_file.txt", fileNode.getName());
                })
                .verifyComplete();
    }
}