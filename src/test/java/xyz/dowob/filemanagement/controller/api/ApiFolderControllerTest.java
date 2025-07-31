package xyz.dowob.filemanagement.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.dowob.filemanagement.component.manager.FilePermissionRuleManager;
import xyz.dowob.filemanagement.component.manager.FolderListTreeManager;
import xyz.dowob.filemanagement.component.provider.provider.FolderListTreeProvider;
import xyz.dowob.filemanagement.component.strategy.FileServiceStrategy;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.customenum.ReservedSearchIdEnum;
import xyz.dowob.filemanagement.customenum.RoleEnum;
import xyz.dowob.filemanagement.data.file.bo.UserFileDataBO;
import xyz.dowob.filemanagement.data.file.dto.FileEditDTO;
import xyz.dowob.filemanagement.data.file.dto.FileFilterDTO;
import xyz.dowob.filemanagement.data.file.dto.UserFileListDTO;
import xyz.dowob.filemanagement.data.response.ApiResponseDTO;
import xyz.dowob.filemanagement.data.response.PagedResponseDTO;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.entity.UserFileMetadata;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.functionInterface.Permission;
import xyz.dowob.filemanagement.service.serviceInterface.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * ApiFolderController 測試類別
 * 
 * <p>全面測試 API 資料夾控制器 {@link xyz.dowob.filemanagement.controller.api.ApiFolderController} 的各種功能，
 * 涵蓋資料夾的增刪改查、特殊資料夾操作以及相關異常處理。</p>
 * 
 * <p>測試範圍包括：
 * 
 * - 資料夾檔案列表獲取功能
 * - 特殊檔案夾操作（星標、最近、回收站、全部、分享）
 * - 資料夾創建、編輯、刪除功能
 * - 資料夾樹構建和路徑獲取
 * - 資料夾下載和回收站操作
 * - 參數驗證和錯誤處理
 * 
 * </p>
 * 
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("ApiFolderController API 資料夾控制器測試")
class ApiFolderControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private PermissionService<UserFileMetadata> permissionService;

    @Mock
    private FileServiceStrategy fileServiceStrategy;

    @Mock
    private FileProperties fileProperties;

    @Mock
    private FileProperties.Global globalProperties;

    @Mock
    private ValidationService validationService;

    @Mock
    private FolderService folderService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private FilePermissionRuleManager filePermissionRuleManager;

    @Mock
    private FolderListTreeManager folderListTreeManager;

    @Mock
    private FileService fileService;

    @Mock
    private Permission<UserFileMetadata> allowSharedRule;

    @Mock
    private Permission<UserFileMetadata> blockDeletedRule;

    @Mock
    private Permission<UserFileMetadata> allowOwnerRule;

    @Mock
    private Permission<UserFileMetadata> blockNotSearchRule;

    private ApiFolderController apiFolderController;
    private User testUser;
    private UserFileMetadata testFolder;
    private ServerWebExchange testExchange;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // 配置 FileProperties mocks
        when(fileProperties.getGlobal()).thenReturn(globalProperties);
        when(globalProperties.getEnableUserFolderListTree()).thenReturn(true);

        // 配置 FilePermissionRuleManager mocks
        when(filePermissionRuleManager.getAllowShared()).thenReturn(allowSharedRule);
        when(filePermissionRuleManager.getBlockDeleted()).thenReturn(blockDeletedRule);
        when(filePermissionRuleManager.getAllowOwner()).thenReturn(allowOwnerRule);
        when(filePermissionRuleManager.getBlockNotSearchOperation()).thenReturn(blockNotSearchRule);

        // 配置 Permission Mock 對象的基本行為
        when(allowSharedRule.check(any(), any())).thenReturn(Mono.empty());
        when(blockDeletedRule.check(any(), any())).thenReturn(Mono.empty());
        when(allowOwnerRule.check(any(), any())).thenReturn(Mono.empty());
        when(blockNotSearchRule.check(any(), any())).thenReturn(Mono.empty());

        // 配置 FileServiceStrategy Mock 對象
        when(fileServiceStrategy.getFileService()).thenReturn(fileService);

        // 手動創建 ApiFolderController 實例
        apiFolderController = new ApiFolderController(
            userService,
            permissionService,
            fileServiceStrategy,
            fileProperties,
            validationService,
            folderService,
            objectMapper,
            filePermissionRuleManager,
            folderListTreeManager
        );

        // 創建測試用戶
        testUser = createTestUser();

        // 創建測試資料夾
        testFolder = new UserFileMetadata();
        testFolder.setId(1L);
        testFolder.setUserId(1L);
        testFolder.setFilename("test-folder");
        testFolder.setFileType(FileEnum.FOLDER);
        testFolder.setUploadTime(LocalDateTime.now());

        // 創建測試 Exchange
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/folders").build();
        testExchange = MockServerWebExchange.from(request);
    }

    private User createTestUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setRole(RoleEnum.USER);
        return user;
    }

    // ==================== 一般測試 ====================

    @Test
    @DisplayName("一般測試 - 控制器初始化和依賴注入")
    void testControllerInitialization() {
        assertNotNull(apiFolderController);
        
        // 驗證控制器繼承關係
        assertTrue(apiFolderController instanceof xyz.dowob.filemanagement.controller.base.BaseFolderController);
        
        // 驗證依賴注入 - 驗證 mock 對象存在
        assertNotNull(userService);
        assertNotNull(permissionService);
        assertNotNull(fileServiceStrategy);
        assertNotNull(fileProperties);
        assertNotNull(validationService);
        assertNotNull(folderService);
        assertNotNull(objectMapper);
        assertNotNull(filePermissionRuleManager);
        assertNotNull(folderListTreeManager);
    }

    @Test
    @DisplayName("一般測試 - getFolderFiles API 獲取資料夾檔案列表")
    void testGetFolderFiles_basicFunctionality() {
        Long folderId = 1L;
        Integer page = 1;
        Integer size = 10;
        List<String> types = Arrays.asList("DOCUMENT", "IMAGE");

        List<UserFileListDTO> fileList = Arrays.asList(new UserFileListDTO(), new UserFileListDTO());
        PagedResponseDTO<UserFileListDTO> pagedFiles = PagedResponseDTO.<UserFileListDTO>builder()
                .data(fileList)
                .totalPages(1)
                .currentPage(page)
                .pageSize(size)
                .totalElements(2L)
                .build();
        List<FolderListTreeProvider.FolderNode> filePaths = Arrays.asList(
            new FolderListTreeProvider.FolderNode(1L, "root")
        );

        when(userService.getUser(testExchange)).thenReturn(Mono.just(testUser));
        when(fileServiceStrategy.getFileService()).thenReturn(fileService);
        when(permissionService.validateUserPermission(eq(testUser), eq(folderId), any(List.class)))
                .thenReturn(Mono.just(testFolder));
        when(validationService.validateFileType(testFolder, FileEnum.FOLDER)).thenReturn(Mono.empty());
        when(fileService.getUserFileList(eq(testUser), any(FileFilterDTO.class)))
                .thenReturn(Mono.just(pagedFiles));
        when(fileService.getUserFilePaths(testFolder, testUser)).thenReturn(Mono.just(filePaths));

        StepVerifier.create(apiFolderController.getFolderFiles(folderId, page, size, types, testExchange))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(200, responseEntity.getStatusCode().value());
                    
                    ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                    assertNotNull(response);
                    assertEquals("獲取用戶檔案列表成功", response.getMessage());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - getStarFiles API 獲取星標檔案")
    void testGetStarFiles_basicFunctionality() {
        Integer page = 1;
        Integer size = 10;
        List<String> types = Arrays.asList("DOCUMENT");

        when(userService.getUser(testExchange)).thenReturn(Mono.just(testUser));
        when(fileServiceStrategy.getFileService()).thenReturn(fileService);
        when(permissionService.validateUserPermission(eq(testUser), eq(ReservedSearchIdEnum.STAR_FILE_ID.getId()), any(List.class)))
                .thenReturn(Mono.just(testFolder));
        when(validationService.validateFileType(testFolder, FileEnum.FOLDER)).thenReturn(Mono.empty());
        when(fileService.getUserFileList(eq(testUser), any(FileFilterDTO.class)))
                .thenReturn(Mono.just(PagedResponseDTO.<UserFileListDTO>builder()
                        .data(Collections.emptyList())
                        .totalPages(0)
                        .currentPage(page)
                        .pageSize(size)
                        .totalElements(0L)
                        .build()));
        when(fileService.getUserFilePaths(testFolder, testUser)).thenReturn(Mono.just(Collections.emptyList()));

        StepVerifier.create(apiFolderController.getStarFiles(testExchange, page, size, types))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(200, responseEntity.getStatusCode().value());
                    
                    ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                    assertNotNull(response);
                    assertEquals("獲取用戶檔案列表成功", response.getMessage());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - getRecentlyFiles API 獲取最近檔案")
    void testGetRecentlyFiles_basicFunctionality() {
        List<String> types = Arrays.asList("DOCUMENT");

        when(userService.getUser(testExchange)).thenReturn(Mono.just(testUser));
        when(fileServiceStrategy.getFileService()).thenReturn(fileService);
        when(permissionService.validateUserPermission(eq(testUser), eq(ReservedSearchIdEnum.RECENT_FILE_ID.getId()), any(List.class)))
                .thenReturn(Mono.just(testFolder));
        when(validationService.validateFileType(testFolder, FileEnum.FOLDER)).thenReturn(Mono.empty());
        when(fileService.getUserFileList(eq(testUser), any(FileFilterDTO.class)))
                .thenReturn(Mono.just(PagedResponseDTO.<UserFileListDTO>builder()
                        .data(Collections.emptyList())
                        .totalPages(0)
                        .currentPage(1)
                        .pageSize(0)
                        .totalElements(0L)
                        .build()));
        when(fileService.getUserFilePaths(testFolder, testUser)).thenReturn(Mono.just(Collections.emptyList()));

        StepVerifier.create(apiFolderController.getRecentlyFiles(testExchange, types))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(200, responseEntity.getStatusCode().value());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - getRecycleFiles API 獲取回收站檔案")
    void testGetRecycleFiles_basicFunctionality() {
        Integer page = 1;
        Integer size = 10;

        when(userService.getUser(testExchange)).thenReturn(Mono.just(testUser));
        when(fileServiceStrategy.getFileService()).thenReturn(fileService);
        when(permissionService.validateUserPermission(eq(testUser), eq(ReservedSearchIdEnum.RECYCLE_FILE_ID.getId()), any(List.class)))
                .thenReturn(Mono.just(testFolder));
        when(validationService.validateFileType(testFolder, FileEnum.FOLDER)).thenReturn(Mono.empty());
        when(fileService.getUserFileList(eq(testUser), any(FileFilterDTO.class)))
                .thenReturn(Mono.just(PagedResponseDTO.<UserFileListDTO>builder()
                        .data(Collections.emptyList())
                        .totalPages(0)
                        .currentPage(page)
                        .pageSize(size)
                        .totalElements(0L)
                        .build()));
        when(fileService.getUserFilePaths(testFolder, testUser)).thenReturn(Mono.just(Collections.emptyList()));

        StepVerifier.create(apiFolderController.getRecycleFiles(testExchange, page, size, null))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(200, responseEntity.getStatusCode().value());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - getAllFiles API 獲取所有檔案")
    void testGetAllFiles_basicFunctionality() {
        Integer page = 1;
        Integer size = 10;

        when(userService.getUser(testExchange)).thenReturn(Mono.just(testUser));
        when(fileServiceStrategy.getFileService()).thenReturn(fileService);
        when(permissionService.validateUserPermission(eq(testUser), eq(ReservedSearchIdEnum.ALL_FILE_ID.getId()), any(List.class)))
                .thenReturn(Mono.just(testFolder));
        when(validationService.validateFileType(testFolder, FileEnum.FOLDER)).thenReturn(Mono.empty());
        when(fileService.getUserFileList(eq(testUser), any(FileFilterDTO.class)))
                .thenReturn(Mono.just(PagedResponseDTO.<UserFileListDTO>builder()
                        .data(Collections.emptyList())
                        .totalPages(0)
                        .currentPage(page)
                        .pageSize(size)
                        .totalElements(0L)
                        .build()));
        when(fileService.getUserFilePaths(testFolder, testUser)).thenReturn(Mono.just(Collections.emptyList()));

        StepVerifier.create(apiFolderController.getAllFiles(testExchange, page, size, null))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(200, responseEntity.getStatusCode().value());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - getSharedFiles API 獲取分享檔案")
    void testGetSharedFiles_basicFunctionality() {
        Integer page = 1;
        Integer size = 10;

        when(userService.getUser(testExchange)).thenReturn(Mono.just(testUser));
        when(fileServiceStrategy.getFileService()).thenReturn(fileService);
        when(permissionService.validateUserPermission(eq(testUser), eq(ReservedSearchIdEnum.SHARE_FILE_ID.getId()), any(List.class)))
                .thenReturn(Mono.just(testFolder));
        when(validationService.validateFileType(testFolder, FileEnum.FOLDER)).thenReturn(Mono.empty());
        when(fileService.getUserFileList(eq(testUser), any(FileFilterDTO.class)))
                .thenReturn(Mono.just(PagedResponseDTO.<UserFileListDTO>builder()
                        .data(Collections.emptyList())
                        .totalPages(0)
                        .currentPage(page)
                        .pageSize(size)
                        .totalElements(0L)
                        .build()));
        when(fileService.getUserFilePaths(testFolder, testUser)).thenReturn(Mono.just(Collections.emptyList()));

        StepVerifier.create(apiFolderController.getSharedFiles(testExchange, page, size, null))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(200, responseEntity.getStatusCode().value());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - deleteFolder API 刪除資料夾")
    void testDeleteFolder_basicDeletion() {
        String folderId = "1";

        when(userService.getUser(testExchange)).thenReturn(Mono.just(testUser));
        when(permissionService.validateUserPermission(eq(testUser), eq(1L), any(List.class)))
                .thenReturn(Mono.just(testFolder));
        when(validationService.validateFileType(testFolder, FileEnum.FOLDER)).thenReturn(Mono.empty());
        when(folderService.deleteFolder(testFolder, testUser)).thenReturn(Mono.empty());

        StepVerifier.create(apiFolderController.deleteFolder(folderId, testExchange))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(200, responseEntity.getStatusCode().value());
                    
                    ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                    assertNotNull(response);
                    assertEquals("刪除資料夾成功", response.getMessage());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - editFolder API 編輯資料夾")
    void testEditFolder_basicEdit() {
        FileEditDTO fileEditDTO = new FileEditDTO();
        fileEditDTO.setFileId("1");
        fileEditDTO.setFilename("new-folder-name");
        fileEditDTO.setParentFolderId(2L);

        UserFileMetadata parentFolder = new UserFileMetadata();
        parentFolder.setId(2L);
        parentFolder.setUserId(1L);
        parentFolder.setFilename("parent-folder");
        parentFolder.setFileType(FileEnum.FOLDER);

        when(userService.getUser(testExchange)).thenReturn(Mono.just(testUser));
        when(validationService.validateEditFileDTO(fileEditDTO, true)).thenReturn(Mono.empty());
        when(validationService.validSpecifyColumns(fileEditDTO, "fileId")).thenReturn(Mono.empty());
        when(permissionService.validateUserPermission(eq(testUser), any(List.class)))
                .thenReturn(Mono.just(java.util.Map.of(1L, testFolder, 2L, parentFolder)));
        when(validationService.validateFileType(testFolder, FileEnum.FOLDER)).thenReturn(Mono.empty());
        when(validationService.validateFileType(parentFolder, FileEnum.FOLDER)).thenReturn(Mono.empty());
        when(folderService.editFolder(any(), eq(testUser))).thenReturn(Mono.empty());

        StepVerifier.create(apiFolderController.editFolder(fileEditDTO, testExchange))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(200, responseEntity.getStatusCode().value());
                    
                    ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                    assertNotNull(response);
                    assertEquals("資料夾更新成功", response.getMessage());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - createFolder API 創建資料夾")
    void testCreateFolder_basicCreation() {
        FileEditDTO fileEditDTO = new FileEditDTO();
        fileEditDTO.setFilename("new-folder");
        fileEditDTO.setParentFolderId(1L);

        when(userService.getUser(testExchange)).thenReturn(Mono.just(testUser));
        when(validationService.validateEditFileDTO(fileEditDTO, true)).thenReturn(Mono.empty());
        when(permissionService.validateUserPermission(testUser, 1L)).thenReturn(Mono.just(testFolder));
        when(validationService.validateFileType(testFolder, FileEnum.FOLDER)).thenReturn(Mono.empty());
        when(folderService.createFolder(fileEditDTO, testUser)).thenReturn(Mono.empty());

        StepVerifier.create(apiFolderController.createFolder(fileEditDTO, testExchange))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(200, responseEntity.getStatusCode().value());
                    
                    ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                    assertNotNull(response);
                    assertEquals("資料夾建立成功", response.getMessage());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - buildTree API 構建資料夾樹")
    void testBuildTree_basicFunctionality() {
        when(userService.getUser(testExchange)).thenReturn(Mono.just(testUser));

        StepVerifier.create(apiFolderController.buildTree(testExchange))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(200, responseEntity.getStatusCode().value());
                    
                    ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                    assertNotNull(response);
                    assertEquals("請求建立用戶檔案樹成功", response.getMessage());
                })
                .verifyComplete();

        verify(folderListTreeManager, timeout(1000)).initializeTree(testUser.getId());
    }

    @Test
    @DisplayName("一般測試 - downloadFolder API 下載資料夾")
    void testDownloadFolder_basicDownload() {
        Long folderId = 1L;
        
        UserFileDataBO fileDataBO = new UserFileDataBO();
        fileDataBO.setFilename("folder.zip");
        fileDataBO.setFileSize(1024L);
        fileDataBO.setMimeType("application/zip");
        fileDataBO.setDataBufferFlux(Flux.just(new DefaultDataBufferFactory().allocateBuffer(1024)));

        when(userService.getUser(testExchange)).thenReturn(Mono.just(testUser));
        when(permissionService.validateUserPermission(eq(testUser), eq(folderId), any(List.class)))
                .thenReturn(Mono.just(testFolder));
        when(validationService.validateFileType(testFolder, FileEnum.FOLDER)).thenReturn(Mono.empty());
        when(folderService.downloadFolder(testFolder, testUser)).thenReturn(Mono.just(fileDataBO));

        StepVerifier.create(apiFolderController.downloadFolder(folderId, testExchange))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(200, responseEntity.getStatusCode().value());
                    
                    Flux<DataBuffer> body = responseEntity.getBody();
                    assertNotNull(body);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - getFolderPath API 獲取資料夾路徑")
    void testGetFolderPath_basicFunctionality() {
        Long folderId = 1L;
        List<FolderListTreeProvider.FolderNode> folderPath = Arrays.asList(
            new FolderListTreeProvider.FolderNode(1L, "root"),
            new FolderListTreeProvider.FolderNode(2L, "subfolder")
        );

        when(userService.getUser(testExchange)).thenReturn(Mono.just(testUser));
        when(permissionService.validateUserPermission(eq(testUser), eq(folderId), any(List.class)))
                .thenReturn(Mono.just(testFolder));
        when(validationService.validateFileType(testFolder, FileEnum.FOLDER)).thenReturn(Mono.empty());
        when(folderService.getUserFilePaths(testFolder, testUser)).thenReturn(Mono.just(folderPath));

        StepVerifier.create(apiFolderController.getFolderPath(folderId, testExchange))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(200, responseEntity.getStatusCode().value());
                    
                    ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                    assertNotNull(response);
                    assertEquals("獲取用戶檔案路徑成功", response.getMessage());
                })
                .verifyComplete();
    }

    // ==================== 異常測試 ====================

    @Test
    @DisplayName("異常測試 - getFolderFiles 無效資料夾ID")
    void testGetFolderFiles_invalidFolderId() {
        Long invalidFolderId = -1L;

        StepVerifier.create(apiFolderController.getFolderFiles(invalidFolderId, 1, 10, null, testExchange))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(404, responseEntity.getStatusCode().value());
                    
                    ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                    assertNotNull(response);
                    assertTrue(response.getMessage().contains("請求路徑不存在"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - deleteFolder 資料夾不存在")
    void testDeleteFolder_folderNotFound() {
        String folderId = "999";

        when(userService.getUser(testExchange)).thenReturn(Mono.just(testUser));
        when(permissionService.validateUserPermission(eq(testUser), eq(999L), any(List.class)))
                .thenReturn(Mono.error(new ValidationException(ValidationException.ErrorCode.NOT_EXISTING_USER_FILE, "999")));

        StepVerifier.create(apiFolderController.deleteFolder(folderId, testExchange))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(404, responseEntity.getStatusCode().value());
                    
                    ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                    assertNotNull(response);
                    assertTrue(response.getMessage().contains("使用者檔案不存在"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - createFolder 驗證失敗")
    void testCreateFolder_validationFailure() {
        FileEditDTO fileEditDTO = new FileEditDTO();
        fileEditDTO.setFilename("");

        when(userService.getUser(testExchange)).thenReturn(Mono.just(testUser));
        when(validationService.validateEditFileDTO(fileEditDTO, true))
                .thenReturn(Mono.error(new ValidationException(ValidationException.ErrorCode.REQUEST_IS_INVALID, "filename")));

        StepVerifier.create(apiFolderController.createFolder(fileEditDTO, testExchange))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertTrue(responseEntity.getStatusCode().is4xxClientError());
                    
                    ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                    assertNotNull(response);
                    assertTrue(response.getMessage().contains("處理失敗"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - buildTree 功能未啟用")
    void testBuildTree_featureNotEnabled() {
        when(globalProperties.getEnableUserFolderListTree()).thenReturn(false);
        when(userService.getUser(testExchange)).thenReturn(Mono.just(testUser));

        StepVerifier.create(apiFolderController.buildTree(testExchange))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(200, responseEntity.getStatusCode().value());
                    
                    ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                    assertEquals("當前設定不支持建立用戶檔案樹", response.getMessage());
                })
                .verifyComplete();
    }

    // ==================== 邊界測試 ====================

    @Test
    @DisplayName("邊界測試 - getFolderFiles 極大分頁參數")
    void testGetFolderFiles_largePageParameters() {
        Long folderId = 1L;
        Integer largePage = Integer.MAX_VALUE;
        Integer largeSize = 1000;

        when(userService.getUser(testExchange)).thenReturn(Mono.just(testUser));
        when(fileServiceStrategy.getFileService()).thenReturn(fileService);
        when(permissionService.validateUserPermission(eq(testUser), eq(folderId), any(List.class)))
                .thenReturn(Mono.just(testFolder));
        when(validationService.validateFileType(testFolder, FileEnum.FOLDER)).thenReturn(Mono.empty());
        when(fileService.getUserFileList(eq(testUser), any(FileFilterDTO.class)))
                .thenReturn(Mono.just(PagedResponseDTO.<UserFileListDTO>builder()
                        .data(Collections.emptyList())
                        .totalPages(0)
                        .currentPage(largePage)
                        .pageSize(largeSize)
                        .totalElements(0L)
                        .build()));
        when(fileService.getUserFilePaths(testFolder, testUser)).thenReturn(Mono.just(Collections.emptyList()));

        StepVerifier.create(apiFolderController.getFolderFiles(folderId, largePage, largeSize, null, testExchange))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(200, responseEntity.getStatusCode().value());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 併發資料夾操作")
    void testConcurrentFolderOperations() {
        String folderId = "1";

        when(userService.getUser(any(ServerWebExchange.class))).thenReturn(Mono.just(testUser));
        when(permissionService.validateUserPermission(eq(testUser), eq(1L), any(List.class)))
                .thenReturn(Mono.just(testFolder));
        when(validationService.validateFileType(testFolder, FileEnum.FOLDER)).thenReturn(Mono.empty());
        when(folderService.deleteFolder(testFolder, testUser)).thenReturn(Mono.empty());

        // 併發執行多個刪除操作
        Flux<ResponseEntity<?>> concurrentDeletes = Flux.range(1, 3)
                .flatMap(i -> apiFolderController.deleteFolder(folderId, testExchange));

        StepVerifier.create(concurrentDeletes)
                .expectNextCount(3)
                .verifyComplete();

        verify(folderService, times(3)).deleteFolder(testFolder, testUser);
    }

    @Test
    @DisplayName("邊界測試 - 所有特殊資料夾類型驗證")
    void testAllSpecialFolderTypes() {
        when(userService.getUser(testExchange)).thenReturn(Mono.just(testUser));
        when(fileServiceStrategy.getFileService()).thenReturn(fileService);
        when(permissionService.validateUserPermission(eq(testUser), any(Long.class), any(List.class)))
                .thenReturn(Mono.just(testFolder));
        when(validationService.validateFileType(testFolder, FileEnum.FOLDER)).thenReturn(Mono.empty());
        when(fileService.getUserFileList(eq(testUser), any(FileFilterDTO.class)))
                .thenReturn(Mono.just(PagedResponseDTO.<UserFileListDTO>builder()
                        .data(Collections.emptyList())
                        .totalPages(0)
                        .currentPage(1)
                        .pageSize(10)
                        .totalElements(0L)
                        .build()));
        when(fileService.getUserFilePaths(testFolder, testUser)).thenReturn(Mono.just(Collections.emptyList()));

        // 測試所有特殊資料夾
        StepVerifier.create(apiFolderController.getStarFiles(testExchange, 1, 10, null))
                .expectNextCount(1).verifyComplete();
        StepVerifier.create(apiFolderController.getRecentlyFiles(testExchange, null))
                .expectNextCount(1).verifyComplete();
        StepVerifier.create(apiFolderController.getRecycleFiles(testExchange, 1, 10, null))
                .expectNextCount(1).verifyComplete();
        StepVerifier.create(apiFolderController.getAllFiles(testExchange, 1, 10, null))
                .expectNextCount(1).verifyComplete();
        StepVerifier.create(apiFolderController.getSharedFiles(testExchange, 1, 10, null))
                .expectNextCount(1).verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - API 路由映射驗證")
    void testApiRouteMappings() {
        // 驗證控制器是否有正確的註解
        assertTrue(apiFolderController.getClass().isAnnotationPresent(org.springframework.web.bind.annotation.RestController.class));
        assertTrue(apiFolderController.getClass().isAnnotationPresent(org.springframework.web.bind.annotation.RequestMapping.class));
        
        // 驗證請求映射路徑
        org.springframework.web.bind.annotation.RequestMapping requestMapping = 
            apiFolderController.getClass().getAnnotation(org.springframework.web.bind.annotation.RequestMapping.class);
        assertEquals("/api/v1/folders", requestMapping.value()[0]);
    }
}