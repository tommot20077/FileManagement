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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.dowob.filemanagement.component.event.EventSink;
import xyz.dowob.filemanagement.component.manager.FilePermissionRuleManager;
import xyz.dowob.filemanagement.component.strategy.FileServiceStrategy;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.controller.base.BaseControllerTestUtils;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.data.event.FileEditedMessage;
import xyz.dowob.filemanagement.data.file.bo.UserFileDataBO;
import xyz.dowob.filemanagement.data.file.dto.EditorContentDTO;
import xyz.dowob.filemanagement.data.file.dto.FileEditDTO;
import xyz.dowob.filemanagement.data.file.dto.FileMetadataDTO;
import xyz.dowob.filemanagement.data.file.dto.FileVersionDTO;
import xyz.dowob.filemanagement.data.response.ApiResponseDTO;
import xyz.dowob.filemanagement.data.response.PagedResponseDTO;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.entity.UserFileMetadata;
import xyz.dowob.filemanagement.entity.UserOnlineFileHistory;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.functionInterface.Permission;
import xyz.dowob.filemanagement.service.serviceInterface.FileService;
import xyz.dowob.filemanagement.service.serviceInterface.PermissionService;
import xyz.dowob.filemanagement.service.serviceInterface.UserService;
import xyz.dowob.filemanagement.service.serviceInterface.ValidationService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * ApiOnlineFileController 測試類別
 * 
 * <p>全面測試 API 在線檔案控制器 {@link xyz.dowob.filemanagement.controller.api.ApiOnlineFileController} 的各種功能，
 * 涵蓋線上檔案編輯、版本管理和協作功能。</p>
 * 
 * <p>測試範圍包括：
 * 
 * - API 在線檔案創建和初始化
 * - API 在線檔案內容下載和獲取
 * - API 在線檔案編輯和保存功能
 * - API 在線檔案歷史版本管理
 * - API 在線檔案刪除和回收站操作
 * - 檔案編輯事件處理和通知
 * - 權限驗證和併發控制
 * - 異常處理和錯誤響應
 * 
 * </p>
 * 
 * <p>測試前置條件：
 * 
 * - ApiOnlineFileController 類正常載入
 * - BaseOnlineFileController 基礎功能可用
 * - EventSink 事件處理機制正常工作
 * - 檔案服務和權限服務可正常模擬
 * - Spring WebFlux 環境配置正確
 * 
 * </p>
 * 
 * <p>測試策略：
 * 
 * - 測試線上檔案的完整生命週期
 * - 驗證檔案版本管理和歷史記錄
 * - 模擬多用戶協作編輯場景
 * - 測試檔案權限控制和安全驗證
 * - 確保事件通知機制正確運作
 * 
 * </p>
 * 
 * <p>預期測試結果：
 * 
 * - 線上檔案 CRUD 操作正確執行
 * - 版本管理功能穩定可靠
 * - 事件通知和協作功能正常
 * - 權限控制和安全機制完善
 * 
 * </p>
 * 
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@SpringBootTest
@ActiveProfiles({"test", "demo", "ci"})
@DisplayName("ApiOnlineFileController API 在線檔案控制器測試")
class ApiOnlineFileControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private FileServiceStrategy fileServiceStrategy;

    @Mock
    private FileProperties fileProperties;

    @Mock
    private ValidationService validationService;

    @Mock
    private PermissionService<UserFileMetadata> permissionService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private FilePermissionRuleManager filePermissionRuleManager;

    @Mock
    private EventSink<FileEditedMessage> eventSink;

    @Mock
    private FileService fileService;

    @Mock
    private Permission<UserFileMetadata> allowSharedRule;

    @Mock
    private Permission<UserFileMetadata> allowOwnerRule;

    @Mock
    private Permission<UserFileMetadata> blockNotSearchRule;

    @Mock
    private Permission<UserFileMetadata> blockDeletedRule;

    private ApiOnlineFileController apiOnlineFileController;
    private User testUser;
    private UserFileMetadata testOnlineFile;
    private ServerWebExchange testExchange;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // 使用 BaseControllerTestUtils 設置標準配置
        testUser = BaseControllerTestUtils.createTestUser();
        testExchange = BaseControllerTestUtils.createTestExchange();
        
        BaseControllerTestUtils.setupUserServiceMocks(userService, testUser, testExchange);
        BaseControllerTestUtils.setupValidationServiceMocks(validationService);
        BaseControllerTestUtils.setupObjectMapperMocks(objectMapper);
        BaseControllerTestUtils.setupFilePermissionRuleManagerMocks(
            filePermissionRuleManager, allowOwnerRule, blockNotSearchRule, blockDeletedRule, allowSharedRule);
        
        // 設置 PermissionService Mock 配置
        UserFileMetadata testFile = BaseControllerTestUtils.createTestFolder();
        testFile.setFileType(FileEnum.ONLINE_DOCUMENT); // 改為在線文檔類型
        BaseControllerTestUtils.setupPermissionServiceMocks(permissionService, testUser, testFile);

        // 配置 Permission Mock 對象的基本行為
        when(allowSharedRule.check(any(), any())).thenReturn(Mono.empty());
        when(allowOwnerRule.check(any(), any())).thenReturn(Mono.empty());
        when(blockNotSearchRule.check(any(), any())).thenReturn(Mono.empty());
        when(blockDeletedRule.check(any(), any())).thenReturn(Mono.empty());
        
        // 配置 FileServiceStrategy Mock 對象 - 修復主要的 NullPointer 問題
        when(fileServiceStrategy.getFileService()).thenReturn(fileService);
        when(fileServiceStrategy.getFileService(any(FileEnum.class))).thenReturn(fileService);
        when(fileServiceStrategy.getFileService(FileEnum.ONLINE_DOCUMENT)).thenReturn(fileService);
        when(fileServiceStrategy.getFileService(FileEnum.FOLDER)).thenReturn(fileService);
        // 重要：處理 null 參數的情況（ApiOnlineFileController 傳遞 null）
        when(fileServiceStrategy.getFileService(null)).thenReturn(fileService);
        
        // 配置 FileService 的基本方法
        when(fileService.uploadFile(any(), any())).thenReturn(Mono.just(new xyz.dowob.filemanagement.data.file.dto.UploadResponseDTO()));
        when(fileService.downloadFile(any(), any(), any())).thenReturn(Mono.just(new UserFileDataBO()));
        when(fileService.removeFile(any(UserFileMetadata.class), any(User.class))).thenReturn(Mono.empty());
        when(fileService.restoreFile(any(UserFileMetadata.class), any(User.class))).thenReturn(Mono.empty());
        when(fileService.getUserFileList(any(), any())).thenReturn(Mono.just(new PagedResponseDTO<>()));
        when(fileService.getFileVersionList(any(), any(), any(Integer.class), any(Integer.class))).thenReturn(Mono.just(new PagedResponseDTO<>()));
        when(fileService.editFile(any(xyz.dowob.filemanagement.data.file.bo.FileEditBO.class), any(User.class))).thenReturn(Mono.empty());

        apiOnlineFileController = new ApiOnlineFileController(
            userService, fileServiceStrategy, fileProperties, validationService,
            permissionService, objectMapper, filePermissionRuleManager, eventSink);

        testOnlineFile = new UserFileMetadata();
        testOnlineFile.setId(1L);
        testOnlineFile.setUserId(1L);
        testOnlineFile.setFilename("test-document.md");
        testOnlineFile.setFileType(FileEnum.ONLINE_DOCUMENT);
        testOnlineFile.setUploadTime(LocalDateTime.now());
    }

    // ==================== 一般測試 ====================

    @Test
    @DisplayName("一般測試 - 控制器初始化")
    void testControllerInitialization() {
        assertNotNull(apiOnlineFileController);
        assertTrue(apiOnlineFileController instanceof xyz.dowob.filemanagement.controller.base.BaseOnlineFileController);
    }

    @Test
    @DisplayName("一般測試 - uploadFile API 創建在線檔案")
    void testUploadFile_createOnlineDocument() {
        FileMetadataDTO fileMetadataDTO = new FileMetadataDTO();
        fileMetadataDTO.setFilename("new-document.md");
        fileMetadataDTO.setFileType(FileEnum.ONLINE_DOCUMENT);
        fileMetadataDTO.setParentFolderId(1L);

        when(userService.getUser(testExchange)).thenReturn(Mono.just(testUser));
        when(validationService.validateFileMetadataDTO(fileMetadataDTO, testUser))
                .thenReturn(Mono.empty());
        when(permissionService.validateUserPermission(eq(testUser), eq(1L)))
                .thenReturn(Mono.just(testOnlineFile));
        when(permissionService.validateUserPermission(eq(testUser), any(List.class)))
                .thenReturn(Mono.just(java.util.Map.of(1L, testOnlineFile)));
        when(validationService.validateFileType(testOnlineFile, FileEnum.FOLDER))
                .thenReturn(Mono.empty());
        when(fileService.uploadFile(fileMetadataDTO, testUser))
                .thenReturn(Mono.just(new xyz.dowob.filemanagement.data.file.dto.UploadResponseDTO()));

        StepVerifier.create(apiOnlineFileController.uploadFile(fileMetadataDTO, testExchange))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(200, responseEntity.getStatusCode().value());
                    
                    ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                    assertNotNull(response);
                    assertTrue(response.getMessage().contains("成功"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - downloadFile API 下載在線檔案")
    void testDownloadFile_previewMode() {
        String fileId = "1";
        String action = "preview";
        
        UserFileDataBO fileDataBO = new UserFileDataBO();
        fileDataBO.setFilename("test-document.md");
        fileDataBO.setFileSize(1024L);
        fileDataBO.setMimeType("text/markdown");
        fileDataBO.setDataBufferFlux(Flux.just(new DefaultDataBufferFactory().allocateBuffer(1024)));

        when(userService.getUser(testExchange)).thenReturn(Mono.just(testUser));
        when(permissionService.validateUserPermission(eq(testUser), eq(1L), any(List.class)))
                .thenReturn(Mono.just(testOnlineFile));
        when(validationService.validateFileType(eq(testOnlineFile), any(FileEnum[].class)))
                .thenReturn(Mono.empty());
        when(fileService.downloadFile(testOnlineFile, testUser, null))
                .thenReturn(Mono.just(fileDataBO));

        StepVerifier.create(apiOnlineFileController.downloadFile(fileId, action, testExchange))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(200, responseEntity.getStatusCode().value());
                    
                    Flux<DataBuffer> body = responseEntity.getBody();
                    assertNotNull(body);
                })
                .verifyComplete();

    }

    @Test
    @DisplayName("一般測試 - deleteFile API 刪除在線檔案")
    void testDeleteFile_basicDeletion() {
        String fileId = "1";

        when(userService.getUser(testExchange)).thenReturn(Mono.just(testUser));
        when(permissionService.validateUserPermission(eq(testUser), eq(1L), any(List.class)))
                .thenReturn(Mono.just(testOnlineFile));
        when(validationService.validateFileType(eq(testOnlineFile), any(FileEnum[].class)))
                .thenReturn(Mono.empty());
        when(fileService.deleteFile(testOnlineFile, testUser)).thenReturn(Mono.empty());

        StepVerifier.create(apiOnlineFileController.deleteFile(fileId, testExchange))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(200, responseEntity.getStatusCode().value());
                    
                    ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                    assertNotNull(response);
                    assertEquals("刪除成功", response.getMessage());
                })
                .verifyComplete();

    }

    @Test
    @DisplayName("一般測試 - editFile API 編輯在線檔案")
    void testEditFile_basicEdit() {
        FileEditDTO fileEditDTO = new FileEditDTO();
        fileEditDTO.setFileId("1");
        fileEditDTO.setFilename("updated-document.md");
        fileEditDTO.setEditType(xyz.dowob.filemanagement.customenum.EditTypeEnum.EDIT_METADATA); // 添加編輯類型
        fileEditDTO.setParentFolderId(1L); // 設置 parentFolderId 以避免 null 問題
        
        // 創建 EditorContentDTO 對象
        EditorContentDTO content = new EditorContentDTO();
        fileEditDTO.setContent(content);

        when(validationService.validateEditFileDTO(fileEditDTO, false))
                .thenReturn(Mono.empty());
        when(validationService.validSpecifyColumns(fileEditDTO, "fileId"))
                .thenReturn(Mono.empty());
        when(userService.getUser(testExchange)).thenReturn(Mono.just(testUser));
        when(permissionService.validateUserPermission(eq(testUser), any(List.class)))
                .thenReturn(Mono.just(java.util.Map.of(1L, testOnlineFile)));
        when(permissionService.validateUserPermission(eq(testUser), any(List.class), any(List.class)))
                .thenReturn(Mono.just(java.util.Map.of(1L, testOnlineFile)));
        when(validationService.validateFileType(eq(testOnlineFile), any(FileEnum[].class)))
                .thenReturn(Mono.empty());
        when(fileService.editFile(any(), eq(testUser))).thenReturn(Mono.empty());

        StepVerifier.create(apiOnlineFileController.editFile(fileEditDTO, testExchange))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(200, responseEntity.getStatusCode().value());
                    
                    ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                    assertNotNull(response);
                    assertEquals("編輯成功", response.getMessage());
                })
                .verifyComplete();

    }

    @Test
    @DisplayName("一般測試 - getHistory API 獲取檔案歷史版本")
    void testGetHistory_basicFunctionality() {
        String fileId = "1";
        Integer page = 1;
        Integer pageSize = 10;

        UserOnlineFileHistory history1 = new UserOnlineFileHistory();
        history1.setVersion(1L);
        history1.setModifiedBy(1L);
        
        UserOnlineFileHistory history2 = new UserOnlineFileHistory();
        history2.setVersion(2L);
        history2.setModifiedBy(1L);
        
        List<FileVersionDTO> versionList = Arrays.asList(
            new FileVersionDTO(history1),
            new FileVersionDTO(history2)
        );
        PagedResponseDTO<FileVersionDTO> pagedHistory = 
            new PagedResponseDTO<FileVersionDTO>(versionList, 1, page, pageSize, 2L);

        when(userService.getUser(testExchange)).thenReturn(Mono.just(testUser));
        when(permissionService.validateUserPermission(eq(testUser), eq(1L), any(List.class)))
                .thenReturn(Mono.just(testOnlineFile));
        when(validationService.validateFileType(eq(testOnlineFile), any(FileEnum[].class)))
                .thenReturn(Mono.empty());
        when(fileService.getFileVersionList(testUser, testOnlineFile, page, pageSize))
                .thenReturn(Mono.just(pagedHistory));

        StepVerifier.create(apiOnlineFileController.getHistory(testExchange, fileId, page, pageSize))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(200, responseEntity.getStatusCode().value());
                    
                    ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                    assertNotNull(response);
                    assertEquals("獲取歷程記錄成功", response.getMessage());
                })
                .verifyComplete();

    }

    @Test
    @DisplayName("一般測試 - removeFile API 移動到回收站")
    void testRemoveFile_moveToTrash() {
        String fileId = "1";

        when(userService.getUser(testExchange)).thenReturn(Mono.just(testUser));
        when(permissionService.validateUserPermission(testUser, 1L))
                .thenReturn(Mono.just(testOnlineFile));
        when(validationService.validateFileType(eq(testOnlineFile), any(FileEnum[].class)))
                .thenReturn(Mono.empty());
        when(fileService.removeFile(testOnlineFile, testUser)).thenReturn(Mono.just(true));
        when(fileServiceStrategy.getFileService(FileEnum.ONLINE_DOCUMENT))
                .thenReturn(fileService);

        StepVerifier.create(apiOnlineFileController.removeFile(testExchange, fileId))
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
                .thenReturn(Mono.just(testOnlineFile));
        when(validationService.validateFileType(eq(testOnlineFile), any(FileEnum[].class)))
                .thenReturn(Mono.empty());
        // 移除重複的Mock配置，使用全局配置

        StepVerifier.create(apiOnlineFileController.restoreFile(testExchange, fileId))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(200, responseEntity.getStatusCode().value());
                    
                    ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                    assertNotNull(response);
                    assertEquals("還原檔案成功", response.getMessage());
                })
                .verifyComplete();

    }

    // ==================== 異常測試 ====================

    @Test
    @DisplayName("異常測試 - uploadFile 驗證失敗")
    void testUploadFile_validationFailure() {
        FileMetadataDTO fileMetadataDTO = new FileMetadataDTO();
        fileMetadataDTO.setFilename(""); // 無效檔案名

        // 模擬用戶認證失敗的情況
        when(userService.getUser(testExchange))
                .thenReturn(Mono.error(new ValidationException(ValidationException.ErrorCode.AUTHENTICATION_FAILED, "user not found")));

        StepVerifier.create(apiOnlineFileController.uploadFile(fileMetadataDTO, testExchange))
                .assertNext(responseEntity -> {
                    // 驗證失敗應該返回錯誤響應，而不是直接拋出異常
                    assertNotNull(responseEntity);
                    assertEquals(401, responseEntity.getStatusCode().value()); // 用戶未認證
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - downloadFile 檔案不存在")
    void testDownloadFile_fileNotFound() {
        String fileId = "999";
        String action = "preview";

        when(userService.getUser(testExchange)).thenReturn(Mono.just(testUser));
        when(permissionService.validateUserPermission(eq(testUser), eq(999L), any(List.class)))
                .thenReturn(Mono.error(new ValidationException(ValidationException.ErrorCode.NOT_EXISTING_USER_FILE, "999")));

        StepVerifier.create(apiOnlineFileController.downloadFile(fileId, action, testExchange))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(404, responseEntity.getStatusCode().value());
                    
                    // downloadFile 在錯誤情況下應該返回錯誤響應，而不是檔案流
                    assertNotNull(responseEntity.getBody());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("異常測試 - editFile 編輯驗證失敗")
    void testEditFile_validationFailure() {
        FileEditDTO fileEditDTO = new FileEditDTO();
        fileEditDTO.setFileId("invalid");

        when(userService.getUser(testExchange)).thenReturn(Mono.just(testUser));
        when(validationService.validateEditFileDTO(fileEditDTO, false))
                .thenReturn(Mono.error(new ValidationException(ValidationException.ErrorCode.REQUEST_IS_INVALID, "fileId")));

        StepVerifier.create(apiOnlineFileController.editFile(fileEditDTO, testExchange))
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
    @DisplayName("異常測試 - getHistory 權限不足")
    void testGetHistory_insufficientPermission() {
        String fileId = "1";

        when(userService.getUser(testExchange)).thenReturn(Mono.just(testUser));
        when(permissionService.validateUserPermission(eq(testUser), eq(1L), any(List.class)))
                .thenReturn(Mono.error(new ValidationException(ValidationException.ErrorCode.FORBIDDEN)));

        StepVerifier.create(apiOnlineFileController.getHistory(testExchange, fileId, 1, 10))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(403, responseEntity.getStatusCode().value());
                    
                    ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                    assertNotNull(response);
                    assertTrue(response.getMessage().contains("處理失敗"));
                })
                .verifyComplete();
    }

    // ==================== 邊界測試 ====================

    @Test
    @DisplayName("邊界測試 - getHistory 極大分頁參數")
    void testGetHistory_largePageParameters() {
        String fileId = "1";
        Integer largePage = Integer.MAX_VALUE;
        Integer largePageSize = 1000;

        when(userService.getUser(testExchange)).thenReturn(Mono.just(testUser));
        when(permissionService.validateUserPermission(eq(testUser), eq(1L), any(List.class)))
                .thenReturn(Mono.just(testOnlineFile));
        when(validationService.validateFileType(eq(testOnlineFile), any(FileEnum[].class)))
                .thenReturn(Mono.empty());
        when(fileService.getFileVersionList(testUser, testOnlineFile, largePage, largePageSize))
                .thenReturn(Mono.just(new PagedResponseDTO<>()));

        StepVerifier.create(apiOnlineFileController.getHistory(testExchange, fileId, largePage, largePageSize))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(200, responseEntity.getStatusCode().value());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("邊界測試 - 併發在線檔案操作")
    void testConcurrentOnlineFileOperations() {
        String fileId = "1";

        when(userService.getUser(any(ServerWebExchange.class))).thenReturn(Mono.just(testUser));
        when(permissionService.validateUserPermission(eq(testUser), eq(1L), any(List.class)))
                .thenReturn(Mono.just(testOnlineFile));
        when(validationService.validateFileType(eq(testOnlineFile), any(FileEnum[].class)))
                .thenReturn(Mono.empty());
        when(fileService.deleteFile(testOnlineFile, testUser)).thenReturn(Mono.empty());

        // 併發執行多個刪除操作
        Flux<ResponseEntity<?>> concurrentDeletes = Flux.range(1, 3)
                .flatMap(i -> apiOnlineFileController.deleteFile(fileId, testExchange));

        StepVerifier.create(concurrentDeletes)
                .expectNextCount(3)
                .verifyComplete();

    }

    @Test
    @DisplayName("邊界測試 - API 路由映射驗證")
    void testApiRouteMappings() {
        assertTrue(apiOnlineFileController.getClass().isAnnotationPresent(org.springframework.web.bind.annotation.RestController.class));
        assertTrue(apiOnlineFileController.getClass().isAnnotationPresent(org.springframework.web.bind.annotation.RequestMapping.class));
        
        org.springframework.web.bind.annotation.RequestMapping requestMapping = 
            apiOnlineFileController.getClass().getAnnotation(org.springframework.web.bind.annotation.RequestMapping.class);
        assertEquals("/api/v1/docs", requestMapping.value()[0]);
    }

    @Test
    @DisplayName("邊界測試 - 控制器基本功能驗證")
    void testControllerBasicFunctionality() {
        // 驗證控制器基本功能
        assertNotNull(apiOnlineFileController);
    }
}