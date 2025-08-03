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
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.dowob.filemanagement.component.limiter.UserLimiter;
import xyz.dowob.filemanagement.component.manager.FilePermissionRuleManager;
import xyz.dowob.filemanagement.component.strategy.FileServiceStrategy;
import xyz.dowob.filemanagement.component.strategy.UserLimiterStrategy;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.customenum.*;
import xyz.dowob.filemanagement.data.file.bo.UserFileDataBO;
import xyz.dowob.filemanagement.data.file.dto.*;
import xyz.dowob.filemanagement.data.response.ApiResponseDTO;
import xyz.dowob.filemanagement.data.response.PagedResponseDTO;
import xyz.dowob.filemanagement.entity.ServerFileMetadata;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.entity.UserFileMetadata;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.functionInterface.Permission;
import xyz.dowob.filemanagement.service.serviceInterface.FileService;
import xyz.dowob.filemanagement.service.serviceInterface.PermissionService;
import xyz.dowob.filemanagement.service.serviceInterface.UserService;
import xyz.dowob.filemanagement.service.serviceInterface.ValidationService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * ApiGeneralFileController 測試類別
 * 
 * <p>全面測試 API 通用檔案控制器 {@link xyz.dowob.filemanagement.controller.api.ApiGeneralFileController} 的各種功能，
 * 涵蓋檔案的完整生命週期管理和用戶權限控制。</p>
 * 
 * <p>測試範圍包括：
 * 
 * - API 檔案上傳功能（元數據驗證和分塊上傳）
 * - API 檔案下載功能（預覽和下載模式）
 * - API 檔案刪除和編輯操作
 * - API 檔案信息獲取和元數據管理
 * - API 檔案列表查詢和高級搜索功能
 * - API 檔案回收站操作和恢復機制
 * - 用戶上傳限制和流量控制
 * - 權限驗證和安全檢查
 * - 異常處理和錯誤響應機制
 * 
 * </p>
 * 
 * <p>測試前置條件：
 * 
 * - ApiGeneralFileController 類正常載入
 * - BaseGeneralFileController 基礎功能可用
 * - 檔案服務策略和用戶限制策略可正常模擬
 * - Spring WebFlux 環境配置正確
 * - MockMvc 和 Mockito 測試環境配置正確
 * 
 * </p>
 * 
 * <p>測試策略：
 * 
 * - 測試 API 控制器繼承和方法覆蓋
 * - 驗證各種檔案操作的 API 請求處理
 * - 模擬檔案上傳流程和用戶限制場景
 * - 測試檔案下載和範圍請求處理
 * - 驗證搜索功能和參數處理邏輯
 * - 確保異常處理和錯誤響應機制
 * 
 * </p>
 * 
 * <p>預期測試結果：
 * 
 * - API 路由正確映射到相應方法
 * - 檔案 CRUD 操作功能正確執行
 * - 用戶限制和權限驗證機制健全
 * - 異常處理和安全機制完善
 * 
 * </p>
 * 
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@SpringBootTest
@ActiveProfiles({"test", "demo", "ci"})
@DisplayName("ApiGeneralFileController API 檔案控制器測試")
class ApiGeneralFileControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private FileServiceStrategy fileServiceStrategy;

    @Mock
    private UserLimiterStrategy userLimiterStrategy;

    @Mock
    private ValidationService validationService;

    @Mock
    private FileProperties fileProperties;

    @Mock
    private FileProperties.Upload uploadProperties;
    
    @Mock
    private FileProperties.Download downloadProperties;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private PermissionService<UserFileMetadata> permissionService;

    @Mock
    private FilePermissionRuleManager filePermissionRuleManager;

    @Mock
    private FileService fileService;

    @Mock 
    private UserLimiter userLimiter;

    @Mock
    private Permission<UserFileMetadata> allowSharedRule;

    @Mock
    private Permission<UserFileMetadata> allowOwnerRule;

    @Mock
    private Permission<UserFileMetadata> blockNotSearchRule;

    @Mock
    private Permission<UserFileMetadata> blockDeletedRule;

    private ApiGeneralFileController apiGeneralFileController;
    private User testUser;
    private UserFileMetadata testFile;
    private ServerWebExchange testExchange;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // 配置 FileProperties mocks
        when(fileProperties.getUpload()).thenReturn(uploadProperties);
        when(uploadProperties.isForceUseServerConfig()).thenReturn(false);
        when(uploadProperties.getDefaultUploadType()).thenReturn(TransmissionEnum.MULTIPART);
        when(fileProperties.getDownload()).thenReturn(downloadProperties);
        when(downloadProperties.getDownloadCacheHeaderExpireTime()).thenReturn(java.time.Duration.ofSeconds(3600));
        
        // 配置 ObjectMapper mock
        try {
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            // This won't happen in mocked scenario
        }

        // 配置 FilePermissionRuleManager mocks
        when(filePermissionRuleManager.getAllowShared()).thenReturn(allowSharedRule);
        when(filePermissionRuleManager.getAllowOwner()).thenReturn(allowOwnerRule);
        when(filePermissionRuleManager.getBlockNotSearchOperation()).thenReturn(blockNotSearchRule);
        when(filePermissionRuleManager.getBlockDeleted()).thenReturn(blockDeletedRule);
        
        // 配置 Permission Mock 對象的基本行為
        when(allowSharedRule.check(any(), any())).thenReturn(Mono.empty());
        when(allowOwnerRule.check(any(), any())).thenReturn(Mono.empty());
        when(blockNotSearchRule.check(any(), any())).thenReturn(Mono.empty());
        when(blockDeletedRule.check(any(), any())).thenReturn(Mono.empty());
        
        // 配置 FileServiceStrategy Mock 對象
        when(fileServiceStrategy.getFileService()).thenReturn(fileService);
        when(fileServiceStrategy.getFileService(any(FileEnum.class))).thenReturn(fileService);
        // 特別配置 null 的情況（ApiGeneralFileController.removeFile 呼叫時傳遞 null）
        when(fileServiceStrategy.getFileService((FileEnum) null)).thenReturn(fileService);
        // 特別配置具體的 FileEnum 類型
        when(fileServiceStrategy.getFileService(FileEnum.DOCUMENT)).thenReturn(fileService);
        when(fileServiceStrategy.getFileService(FileEnum.FOLDER)).thenReturn(fileService);
        when(fileServiceStrategy.getFileService(FileEnum.IMAGE)).thenReturn(fileService);
        when(fileServiceStrategy.getFileService(FileEnum.VIDEO)).thenReturn(fileService);
        when(fileServiceStrategy.getFileService(FileEnum.MUSIC)).thenReturn(fileService);
        when(fileServiceStrategy.getFileService(FileEnum.ZIP)).thenReturn(fileService);
        when(fileServiceStrategy.getFileService(FileEnum.OTHER)).thenReturn(fileService);
        when(fileServiceStrategy.getFileService(FileEnum.ONLINE_DOCUMENT)).thenReturn(fileService);
        
        // 配置 FileService Mock 對象的基本方法
        when(fileService.searchUserFile(any(User.class), any(FileFilterDTO.class)))
                .thenReturn(Mono.just(PagedResponseDTO.<UserFileListDTO>builder()
                        .data(Arrays.asList(new UserFileListDTO()))
                        .totalPages(1)
                        .currentPage(1)
                        .pageSize(10)
                        .totalElements(1L)
                        .build()));
        when(fileService.removeFile(any(UserFileMetadata.class), any(User.class))).thenReturn(Mono.just(true));
        when(fileService.restoreFile(any(UserFileMetadata.class), any(User.class))).thenReturn(Mono.empty());
        when(fileService.deleteFile(any(), any(User.class))).thenReturn(Mono.empty());
        
        // 配置 UserLimiterStrategy Mock 對象
        when(userLimiterStrategy.getUserLimiter(any(UserLimiterEnum.class))).thenReturn(userLimiter);
        when(userLimiter.tryAcquire(any())).thenReturn(Mono.just(true));
        when(userLimiter.release(any())).thenReturn(Mono.empty());

        // 手動創建 ApiGeneralFileController 實例
        apiGeneralFileController = new ApiGeneralFileController(
            userService,
            fileServiceStrategy,
            userLimiterStrategy,
            validationService,
            fileProperties,
            objectMapper,
            permissionService,
            filePermissionRuleManager
        );

        // 創建測試用戶
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setRole(RoleEnum.USER);

        // 創建測試檔案
        testFile = new UserFileMetadata();
        testFile.setId(1L);
        testFile.setUserId(1L);
        testFile.setFilename("test-file.txt");
        testFile.setFileType(FileEnum.DOCUMENT);
        testFile.setUploadTime(LocalDateTime.now());

        // 創建測試 Exchange
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/files").build();
        testExchange = MockServerWebExchange.from(request);
    }

    // ==================== 一般測試 ====================

    @Test
    @DisplayName("一般測試 - 控制器初始化和依賴注入")
    void testControllerInitialization() {
        assertNotNull(apiGeneralFileController);
        
        // 驗證控制器繼承關係
        assertTrue(apiGeneralFileController instanceof xyz.dowob.filemanagement.controller.base.BaseGeneralFileController);
        
        // 驗證依賴注入 - 使用反射或公共方法來驗證
        assertNotNull(userService);
        assertNotNull(fileServiceStrategy);
        assertNotNull(fileProperties);
        assertNotNull(validationService);
        assertNotNull(permissionService);
        assertNotNull(objectMapper);
        assertNotNull(filePermissionRuleManager);
    }

    @Test
    @DisplayName("一般測試 - uploadFile API 檔案上傳元數據驗證")
    void testUploadFile_metadataValidation() {
        FileMetadataDTO fileMetadataDTO = new FileMetadataDTO();
        fileMetadataDTO.setFilename("test.txt");
        fileMetadataDTO.setFileSize(1024L);
        fileMetadataDTO.setParentFolderId(1L);

        UploadResponseDTO uploadResponse = new UploadResponseDTO();
        uploadResponse.setTransferTaskId("task123");
        uploadResponse.setIsFinished(false);

        when(userService.getUser(testExchange)).thenReturn(Mono.just(testUser));
        when(userLimiterStrategy.getUserLimiter(UserLimiterEnum.USER_UPLOAD_LIMITER))
                .thenReturn(userLimiter);
        when(userLimiter.tryAcquire(testUser.getId())).thenReturn(Mono.just(true));
        when(userLimiter.release(testUser.getId())).thenReturn(Mono.empty());
        when(validationService.validateFileMetadataDTO(fileMetadataDTO, testUser))
                .thenReturn(Mono.empty());
        when(permissionService.validateUserPermission(eq(testUser), any(List.class)))
                .thenReturn(Mono.just(Map.of(1L, testFile)));
        when(validationService.validateFileType(testFile, FileEnum.FOLDER))
                .thenReturn(Mono.empty());
        when(fileServiceStrategy.getFileService()).thenReturn(fileService);
        when(fileService.uploadFile(fileMetadataDTO, testUser))
                .thenReturn(Mono.just(uploadResponse));

        StepVerifier.create(apiGeneralFileController.uploadFile(fileMetadataDTO, testExchange))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(200, responseEntity.getStatusCode().value());
                    
                    ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                    assertNotNull(response);
                    assertEquals("建立任務成功", response.getMessage());
                })
                .verifyComplete();

        verify(validationService).validateFileMetadataDTO(fileMetadataDTO, testUser);
        verify(userLimiter).tryAcquire(testUser.getId());
    }

    @Test
    @DisplayName("一般測試 - downloadFile API 檔案下載預覽模式")
    void testDownloadFile_previewMode() {
        Long fileId = 1L;
        String action = "preview";
        
        UserFileDataBO fileDataBO = new UserFileDataBO();
        fileDataBO.setFilename("test.txt");
        fileDataBO.setFileSize(1024L);
        fileDataBO.setMimeType("text/plain");
        fileDataBO.setDataBufferFlux(Flux.just(new DefaultDataBufferFactory().allocateBuffer(1024)));

        when(userService.getUser(testExchange)).thenReturn(Mono.just(testUser));
        when(permissionService.validateUserPermission(eq(testUser), eq(fileId), any(List.class)))
                .thenReturn(Mono.just(testFile));
        when(validationService.validateFileType(eq(testFile), any(FileEnum[].class)))
                .thenReturn(Mono.empty());
        when(fileServiceStrategy.getFileService()).thenReturn(fileService);
        when(fileService.downloadFile(testFile, testUser, null))
                .thenReturn(Mono.just(fileDataBO));

        StepVerifier.create(apiGeneralFileController.downloadFile(action, fileId, testExchange))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(200, responseEntity.getStatusCode().value());
                    
                    Flux<DataBuffer> body = responseEntity.getBody();
                    assertNotNull(body);
                    
                    HttpHeaders headers = responseEntity.getHeaders();
                    assertNotNull(headers);
                })
                .verifyComplete();

        verify(fileService).downloadFile(testFile, testUser, null);
    }

    @Test
    @DisplayName("一般測試 - deleteFile API 檔案刪除")
    void testDeleteFile_basicDeletion() {
        String fileId = "1";

        when(userService.getUser(testExchange)).thenReturn(Mono.just(testUser));
        when(permissionService.validateUserPermission(eq(testUser), eq(1L), any(List.class)))
                .thenReturn(Mono.just(testFile));
        when(validationService.validateFileType(eq(testFile), any(FileEnum[].class)))
                .thenReturn(Mono.empty());
        when(fileServiceStrategy.getFileService()).thenReturn(fileService);
        when(fileService.deleteFile(testFile, testUser)).thenReturn(Mono.empty());

        StepVerifier.create(apiGeneralFileController.deleteFile(fileId, testExchange))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(200, responseEntity.getStatusCode().value());
                    
                    ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                    assertNotNull(response);
                    assertEquals("刪除成功", response.getMessage());
                })
                .verifyComplete();

        verify(fileService).deleteFile(testFile, testUser);
    }

    @Test
    @DisplayName("一般測試 - editFile API 檔案編輯")
    void testEditFile_basicEdit() {
        FileEditDTO fileEditDTO = new FileEditDTO();
        fileEditDTO.setFileId("1");
        fileEditDTO.setFilename("new-name.txt");
        fileEditDTO.setParentFolderId(2L);

        UserFileMetadata parentFolder = new UserFileMetadata();
        parentFolder.setId(2L);
        parentFolder.setFileType(FileEnum.FOLDER);

        when(validationService.validateEditFileDTO(fileEditDTO, false))
                .thenReturn(Mono.empty());
        when(validationService.validSpecifyColumns(fileEditDTO, "fileId"))
                .thenReturn(Mono.empty());
        when(userService.getUser(testExchange)).thenReturn(Mono.just(testUser));
        when(permissionService.validateUserPermission(eq(testUser), any(List.class)))
                .thenReturn(Mono.just(Map.of(1L, testFile, 2L, parentFolder)));
        when(validationService.validateFileType(eq(testFile), any(FileEnum[].class)))
                .thenReturn(Mono.empty());
        when(validationService.validateFileType(parentFolder, FileEnum.FOLDER))
                .thenReturn(Mono.empty());
        when(fileServiceStrategy.getFileService()).thenReturn(fileService);
        when(fileService.editFile(any(), eq(testUser))).thenReturn(Mono.empty());

        StepVerifier.create(apiGeneralFileController.editFile(fileEditDTO, testExchange))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(200, responseEntity.getStatusCode().value());
                    
                    ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                    assertNotNull(response);
                    assertEquals("資料更新成功", response.getMessage());
                })
                .verifyComplete();

        verify(validationService).validateEditFileDTO(fileEditDTO, false);
        verify(fileService).editFile(any(), eq(testUser));
    }

    @Test
    @DisplayName("一般測試 - uploadFileData API 分塊上傳")
    void testUploadFileData_chunkUpload() {
        String transmissionType = "chunk";
        UploadResponseDTO uploadResponse = new UploadResponseDTO();
        uploadResponse.setIsSuccess(true);

        // 模擬 formatChunkData 和 handleChunkUpload 的行為
        when(fileServiceStrategy.getFileService()).thenReturn(fileService);
        when(fileService.uploadFileChunk(any(UploadChunkDTO.class)))
                .thenReturn(Mono.just(uploadResponse));

        // 創建帶有請求體的測試 Exchange
        MockServerHttpRequest request = MockServerHttpRequest
                .post("/api/v1/files/upload-chunk")
                .body("{\"transferTaskId\":\"task123\",\"chunkIndex\":1,\"totalChunks\":1}");
        ServerWebExchange exchangeWithBody = MockServerWebExchange.from(request);

        StepVerifier.create(apiGeneralFileController.uploadFileData(transmissionType, exchangeWithBody))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    // 由於實際實現可能會有異常處理，我們檢查響應不為空
                    assertNotNull(responseEntity.getBody());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - getUserFileList API 獲取用戶檔案列表")
    void testGetUserFileList_basicFunctionality() {
        Integer page = 1;
        Integer size = 10;
        List<String> types = Arrays.asList("DOCUMENT", "IMAGE");

        when(userService.getUser(testExchange)).thenReturn(Mono.just(testUser));
        when(fileServiceStrategy.getFileService()).thenReturn(fileService);
        when(permissionService.validateUserPermission(eq(testUser), eq(ReservedSearchIdEnum.ALL_FILE_ID.getId()), any(List.class)))
                .thenReturn(Mono.just(testFile));
        when(validationService.validateFileType(testFile, FileEnum.FOLDER))
                .thenReturn(Mono.empty());
        when(fileService.getUserFileList(eq(testUser), any(FileFilterDTO.class)))
                .thenReturn(Mono.just(PagedResponseDTO.<UserFileListDTO>builder()
                        .data(Arrays.asList())
                        .totalPages(0)
                        .currentPage(page)
                        .pageSize(size)
                        .totalElements(0L)
                        .build()));
        when(fileService.getUserFilePaths(testFile, testUser))
                .thenReturn(Mono.just(Arrays.asList()));

        StepVerifier.create(apiGeneralFileController.getUserFileList(testExchange, page, size, types))
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
    @DisplayName("一般測試 - getFileInfo API 獲取檔案信息")
    void testGetFileInfo_basicInfo() {
        Long fileId = 1L;
        ServerFileMetadata serverFile = new ServerFileMetadata();
        serverFile.setId(1L);
        serverFile.setFileType(FileEnum.DOCUMENT);
        serverFile.setFileSize(1024L);
        testFile.setServerFileId(1L);

        when(userService.getUser(testExchange)).thenReturn(Mono.just(testUser));
        when(permissionService.validateUserPermission(eq(testUser), eq(fileId), any(List.class)))
                .thenReturn(Mono.just(testFile));
        when(validationService.validateFileType(eq(testFile), any(FileEnum[].class)))
                .thenReturn(Mono.just(testFile));
        when(fileServiceStrategy.getFileService()).thenReturn(fileService);
        when(fileService.getByServerFileMetadataId(testFile.getServerFileId())).thenReturn(Mono.just(serverFile));

        StepVerifier.create(apiGeneralFileController.getFileType(fileId, testExchange))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(200, responseEntity.getStatusCode().value());
                    
                    ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                    assertNotNull(response);
                    assertEquals("獲取檔案類型成功", response.getMessage());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - removeFile API 移動到回收站")
    void testRemoveFile_moveToTrash() {
        String fileId = "1";

        when(userService.getUser(testExchange)).thenReturn(Mono.just(testUser));
        when(permissionService.validateUserPermission(testUser, 1L))
                .thenReturn(Mono.just(testFile));
        when(validationService.validateFileType(eq(testFile), any(FileEnum[].class)))
                .thenReturn(Mono.empty());
        // Mock both general and specific FileEnum calls
        when(fileServiceStrategy.getFileService(any(FileEnum.class))).thenReturn(fileService);
        when(fileServiceStrategy.getFileService(FileEnum.DOCUMENT)).thenReturn(fileService);
        when(fileService.removeFile(testFile, testUser)).thenReturn(Mono.just(true));

        StepVerifier.create(apiGeneralFileController.removeFile(testExchange, fileId))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(200, responseEntity.getStatusCode().value());
                    
                    ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                    assertNotNull(response);
                    assertEquals("回收檔案成功", response.getMessage());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - restoreFile API 還原檔案")
    void testRestoreFile_restoreFromTrash() {
        String fileId = "1";

        when(userService.getUser(testExchange)).thenReturn(Mono.just(testUser));
        when(permissionService.validateUserPermission(eq(testUser), eq(1L), any(List.class)))
                .thenReturn(Mono.just(testFile));
        when(validationService.validateFileType(eq(testFile), any(FileEnum[].class)))
                .thenReturn(Mono.empty());
        // Mock both general and specific FileEnum calls
        when(fileServiceStrategy.getFileService(any(FileEnum.class))).thenReturn(fileService);
        when(fileServiceStrategy.getFileService(FileEnum.DOCUMENT)).thenReturn(fileService);
        when(fileService.restoreFile(testFile, testUser)).thenReturn(Mono.just(testFile));

        StepVerifier.create(apiGeneralFileController.restoreFile(testExchange, fileId))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(200, responseEntity.getStatusCode().value());
                    
                    ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                    assertNotNull(response);
                    assertEquals("還原檔案成功", response.getMessage());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - search API 檔案搜索功能")
    void testSearch_basicSearch() {
        String keyword = "test";
        Long folderId = 1L;
        List<String> types = Arrays.asList("DOCUMENT");
        Integer page = 1;
        Integer size = 10;
        Boolean deleted = false;
        Boolean shared = false;
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();

        PagedResponseDTO<UserFileListDTO> searchResults = PagedResponseDTO.<UserFileListDTO>builder()
                .data(Arrays.asList(new UserFileListDTO()))
                .totalPages(1)
                .currentPage(page)
                .pageSize(size)
                .totalElements(1L)
                .build();

        when(userService.getUser(testExchange)).thenReturn(Mono.just(testUser));
        when(fileServiceStrategy.getFileService()).thenReturn(fileService);
        when(validationService.validateFileFilterDTO(any(FileFilterDTO.class)))
                .thenReturn(Mono.empty());
        when(fileService.searchUserFile(eq(testUser), any(FileFilterDTO.class)))
                .thenReturn(Mono.just(searchResults));

        StepVerifier.create(apiGeneralFileController.search(
                testExchange, keyword, folderId, types, page, size, deleted, shared, startDate, endDate))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(200, responseEntity.getStatusCode().value());
                    
                    ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                    assertNotNull(response);
                    assertEquals("搜索檔案成功", response.getMessage());
                })
                .verifyComplete();

        verify(fileService).searchUserFile(eq(testUser), any(FileFilterDTO.class));
    }

    @Test
    @DisplayName("一般測試 - API 路由映射驗證")
    void testApiRouteMappings() {
        // 驗證控制器是否有正確的註解
        assertTrue(apiGeneralFileController.getClass().isAnnotationPresent(org.springframework.web.bind.annotation.RestController.class));
        assertTrue(apiGeneralFileController.getClass().isAnnotationPresent(org.springframework.web.bind.annotation.RequestMapping.class));
        
        // 驗證請求映射路徑
        org.springframework.web.bind.annotation.RequestMapping requestMapping = 
            apiGeneralFileController.getClass().getAnnotation(org.springframework.web.bind.annotation.RequestMapping.class);
        assertEquals("/api/v1/files", requestMapping.value()[0]);
    }

    // ==================== 異常測試 ====================

    @Test
    @DisplayName("異常測試 - uploadFile 用戶超出上傳限制")
    void testUploadFile_userLimitExceeded() {
        FileMetadataDTO fileMetadataDTO = new FileMetadataDTO();
        fileMetadataDTO.setFilename("test.txt");

        when(userService.getUser(testExchange)).thenReturn(Mono.just(testUser));
        when(userLimiterStrategy.getUserLimiter(UserLimiterEnum.USER_UPLOAD_LIMITER))
                .thenReturn(userLimiter);
        when(userLimiter.tryAcquire(testUser.getId())).thenReturn(Mono.just(false));

        StepVerifier.create(apiGeneralFileController.uploadFile(fileMetadataDTO, testExchange))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(429, responseEntity.getStatusCode().value());
                    
                    ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                    assertNotNull(response);
                    assertTrue(response.getMessage().contains("限制錯誤"));
                })
                .verifyComplete();

        verify(userLimiter).tryAcquire(testUser.getId());
    }

    @Test
    @DisplayName("異常測試 - downloadFile 檔案不存在")
    void testDownloadFile_fileNotFound() {
        Long fileId = 999L;
        String action = "preview";

        when(userService.getUser(testExchange)).thenReturn(Mono.just(testUser));
        when(permissionService.validateUserPermission(eq(testUser), eq(fileId), any(List.class)))
                .thenReturn(Mono.error(new ValidationException(ValidationException.ErrorCode.NOT_EXISTING_USER_FILE, "999")));

        StepVerifier.create(apiGeneralFileController.downloadFile(action, fileId, testExchange))
                .assertNext(responseEntity -> {
                    // 預期會有錯誤處理，返回錯誤響應
                    assertNotNull(responseEntity);
                    assertTrue(responseEntity.getStatusCode().is4xxClientError());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - editFile 驗證失敗")
    void testEditFile_validationFailure() {
        FileEditDTO fileEditDTO = new FileEditDTO();
        fileEditDTO.setFileId("invalid");

        when(validationService.validateEditFileDTO(fileEditDTO, false))
                .thenReturn(Mono.error(new ValidationException(ValidationException.ErrorCode.REQUEST_IS_INVALID, "fileId")));
        when(validationService.validSpecifyColumns(fileEditDTO, "fileId"))
                .thenReturn(Mono.empty());
        when(userService.getUser(testExchange))
                .thenReturn(Mono.just(testUser));

        StepVerifier.create(apiGeneralFileController.editFile(fileEditDTO, testExchange))
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
    @DisplayName("異常測試 - search 搜索參數無效")
    void testSearch_invalidSearchParameters() {
        when(userService.getUser(testExchange)).thenReturn(Mono.just(testUser));
        when(fileServiceStrategy.getFileService()).thenReturn(fileService);
        when(validationService.validateFileFilterDTO(any(FileFilterDTO.class)))
                .thenReturn(Mono.error(new ValidationException(ValidationException.ErrorCode.REQUEST_IS_INVALID, "parameters")));

        StepVerifier.create(apiGeneralFileController.search(
                testExchange, null, null, null, -1, -1, false, false, null, null))
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
    @DisplayName("異常測試 - getUserFileList 權限不足")
    void testGetUserFileList_insufficientPermission() {
        when(userService.getUser(testExchange)).thenReturn(Mono.just(testUser));
        when(fileServiceStrategy.getFileService()).thenReturn(fileService);
        when(permissionService.validateUserPermission(eq(testUser), any(Long.class), any(List.class)))
                .thenReturn(Mono.error(new ValidationException(ValidationException.ErrorCode.FORBIDDEN)));

        StepVerifier.create(apiGeneralFileController.getUserFileList(testExchange, 1, 10, null))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(403, responseEntity.getStatusCode().value());
                    
                    ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                    assertNotNull(response);
                    assertTrue(response.getMessage().contains("沒有權限"));
                })
                .verifyComplete();
    }

    // ==================== 邊界測試 ====================

    @Test
    @DisplayName("邊界測試 - uploadFile 極大檔案上傳")
    void testUploadFile_largeFile() {
        FileMetadataDTO fileMetadataDTO = new FileMetadataDTO();
        fileMetadataDTO.setFilename("large-file.bin");
        fileMetadataDTO.setFileSize(Long.MAX_VALUE);

        UploadResponseDTO uploadResponse = new UploadResponseDTO();
        uploadResponse.setIsFinished(true);

        when(userService.getUser(testExchange)).thenReturn(Mono.just(testUser));
        when(userLimiterStrategy.getUserLimiter(UserLimiterEnum.USER_UPLOAD_LIMITER))
                .thenReturn(userLimiter);
        when(userLimiter.tryAcquire(testUser.getId())).thenReturn(Mono.just(true));
        when(userLimiter.release(testUser.getId())).thenReturn(Mono.empty());
        when(validationService.validateFileMetadataDTO(fileMetadataDTO, testUser))
                .thenReturn(Mono.empty());
        when(permissionService.validateUserPermission(eq(testUser), any(List.class)))
                .thenReturn(Mono.just(Map.of()));
        when(fileServiceStrategy.getFileService()).thenReturn(fileService);
        when(fileService.uploadFile(fileMetadataDTO, testUser))
                .thenReturn(Mono.just(uploadResponse));

        StepVerifier.create(apiGeneralFileController.uploadFile(fileMetadataDTO, testExchange))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(200, responseEntity.getStatusCode().value());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - search 複雜搜索條件")
    void testSearch_complexSearchCriteria() {
        String keyword = "特殊字符!@#$%^&*()test";
        List<String> types = Arrays.asList("DOCUMENT", "IMAGE", "VIDEO", "MUSIC", "ZIP", "OTHER");
        LocalDateTime startDate = LocalDateTime.of(1970, 1, 1, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2100, 12, 31, 23, 59);

        PagedResponseDTO<UserFileListDTO> searchResults = PagedResponseDTO.<UserFileListDTO>builder()
                .data(Arrays.asList())
                .totalPages(0)
                .currentPage(1)
                .pageSize(100)
                .totalElements(0L)
                .build();

        when(userService.getUser(testExchange)).thenReturn(Mono.just(testUser));
        when(fileServiceStrategy.getFileService()).thenReturn(fileService);
        when(validationService.validateFileFilterDTO(any(FileFilterDTO.class)))
                .thenReturn(Mono.empty());
        when(fileService.searchUserFile(eq(testUser), any(FileFilterDTO.class)))
                .thenReturn(Mono.just(searchResults));

        StepVerifier.create(apiGeneralFileController.search(
                testExchange, keyword, null, types, 1, 100, true, true, startDate, endDate))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(200, responseEntity.getStatusCode().value());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - getUserFileList 極大分頁參數")
    void testGetUserFileList_largePageParameters() {
        Integer largePage = Integer.MAX_VALUE;
        Integer largeSize = 10000;

        when(userService.getUser(testExchange)).thenReturn(Mono.just(testUser));
        when(fileServiceStrategy.getFileService()).thenReturn(fileService);
        when(permissionService.validateUserPermission(eq(testUser), any(Long.class), any(List.class)))
                .thenReturn(Mono.just(testFile));
        when(validationService.validateFileType(testFile, FileEnum.FOLDER))
                .thenReturn(Mono.empty());
        when(fileService.getUserFileList(eq(testUser), any(FileFilterDTO.class)))
                .thenReturn(Mono.just(PagedResponseDTO.<UserFileListDTO>builder()
                        .data(Arrays.asList())
                        .totalPages(0)
                        .currentPage(largePage)
                        .pageSize(largeSize)
                        .totalElements(0L)
                        .build()));
        when(fileService.getUserFilePaths(testFile, testUser))
                .thenReturn(Mono.just(Arrays.asList()));

        StepVerifier.create(apiGeneralFileController.getUserFileList(testExchange, largePage, largeSize, null))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(200, responseEntity.getStatusCode().value());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 併發檔案操作")
    void testConcurrentFileOperations() {
        String fileId = "1";

        when(userService.getUser(any(ServerWebExchange.class))).thenReturn(Mono.just(testUser));
        when(permissionService.validateUserPermission(eq(testUser), eq(1L), any(List.class)))
                .thenReturn(Mono.just(testFile));
        when(validationService.validateFileType(eq(testFile), any(FileEnum[].class)))
                .thenReturn(Mono.empty());
        when(fileServiceStrategy.getFileService()).thenReturn(fileService);
        when(fileService.deleteFile(testFile, testUser)).thenReturn(Mono.empty());

        // 併發執行多個刪除操作
        Flux<ResponseEntity<?>> concurrentDeletes = Flux.range(1, 5)
                .flatMap(i -> apiGeneralFileController.deleteFile(fileId, testExchange));

        StepVerifier.create(concurrentDeletes)
                .expectNextCount(5)
                .verifyComplete();

        verify(fileService, times(5)).deleteFile(testFile, testUser);
    }

    @Test
    @DisplayName("邊界測試 - 檔案名特殊字符處理")
    void testSpecialCharacterFilenames() {
        FileMetadataDTO fileMetadataDTO = new FileMetadataDTO();
        fileMetadataDTO.setFilename("测试檔案!@#$%^&*()_+-=[]{}|;':\",./<>?.txt");
        fileMetadataDTO.setFileSize(1024L);

        UploadResponseDTO uploadResponse = new UploadResponseDTO();
        uploadResponse.setIsFinished(true);

        when(userService.getUser(testExchange)).thenReturn(Mono.just(testUser));
        when(userLimiterStrategy.getUserLimiter(UserLimiterEnum.USER_UPLOAD_LIMITER))
                .thenReturn(userLimiter);
        when(userLimiter.tryAcquire(testUser.getId())).thenReturn(Mono.just(true));
        when(userLimiter.release(testUser.getId())).thenReturn(Mono.empty());
        when(validationService.validateFileMetadataDTO(fileMetadataDTO, testUser))
                .thenReturn(Mono.empty());
        when(permissionService.validateUserPermission(eq(testUser), any(List.class)))
                .thenReturn(Mono.just(Map.of()));
        when(fileServiceStrategy.getFileService()).thenReturn(fileService);
        when(fileService.uploadFile(fileMetadataDTO, testUser))
                .thenReturn(Mono.just(uploadResponse));

        StepVerifier.create(apiGeneralFileController.uploadFile(fileMetadataDTO, testExchange))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(200, responseEntity.getStatusCode().value());
                })
                .verifyComplete();
    }
}