package xyz.dowob.filemanagement.controller.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.dowob.filemanagement.component.event.EventSink;
import xyz.dowob.filemanagement.component.manager.FilePermissionRuleManager;
import xyz.dowob.filemanagement.component.strategy.FileServiceStrategy;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.customenum.RoleEnum;
import xyz.dowob.filemanagement.data.event.FileEditedMessage;
import xyz.dowob.filemanagement.data.file.bo.FileEditBO;
import xyz.dowob.filemanagement.data.file.bo.UserFileDataBO;
import xyz.dowob.filemanagement.data.file.dto.EditorContentDTO;
import xyz.dowob.filemanagement.data.file.dto.FileEditDTO;
import xyz.dowob.filemanagement.data.file.dto.FileMetadataDTO;
import xyz.dowob.filemanagement.data.response.ApiResponseDTO;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.entity.UserFileMetadata;
import xyz.dowob.filemanagement.functionInterface.Permission;
import xyz.dowob.filemanagement.service.serviceInterface.FileService;
import xyz.dowob.filemanagement.service.serviceInterface.PermissionService;
import xyz.dowob.filemanagement.service.serviceInterface.UserService;
import xyz.dowob.filemanagement.service.serviceInterface.ValidationService;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

/**
 * WebOnlineFileController 測試類
 * 
 * 測試 WebOnlineFileController Web 在線檔案控制器的基本功能
 * 
 * @author yuan
 */
@SpringBootTest
@ActiveProfiles({"test", "demo", "ci"})
@DisplayName("WebOnlineFileController Web 在線檔案控制器測試")
class WebOnlineFileControllerTest {

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
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;
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

    private WebOnlineFileController webOnlineFileController;

    private User testUser;
    private UserFileMetadata testOnlineFile;
    private ServerWebExchange testExchange;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // 配置 FilePermissionRuleManager Mock
        when(filePermissionRuleManager.getAllowShared()).thenReturn(allowSharedRule);
        when(filePermissionRuleManager.getAllowOwner()).thenReturn(allowOwnerRule);
        when(filePermissionRuleManager.getBlockNotSearchOperation()).thenReturn(blockNotSearchRule);
        when(filePermissionRuleManager.getBlockDeleted()).thenReturn(blockDeletedRule);
        
        // 配置 Permission Mock 對象的基本行為
        when(allowSharedRule.check(any(), any())).thenReturn(Mono.empty());
        when(allowOwnerRule.check(any(), any())).thenReturn(Mono.empty());
        when(blockDeletedRule.check(any(), any())).thenReturn(Mono.empty());
        when(blockNotSearchRule.check(any(), any())).thenReturn(Mono.empty());
        
        // 配置其他基本 Mock
        when(userService.getUser(any(ServerWebExchange.class))).thenReturn(Mono.just(createTestUser()));
        when(fileServiceStrategy.getFileService(any(FileEnum.class))).thenReturn(fileService);
        when(validationService.validateEditFileDTO(any(FileEditDTO.class), any(Boolean.class))).thenReturn(Mono.empty());
        when(permissionService.validateUserPermission(any(User.class), any(List.class), any())).thenReturn(Mono.just(java.util.Map.of()));
        when(fileService.editFile(any(), any())).thenReturn(Mono.empty());
        when(fileService.downloadFile(any(), any(), any())).thenReturn(Mono.just(createTestUserFileDataBO()));
        
        // 配置 ObjectMapper Mock
        try {
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"message\":\"下載成功\",\"data\":{\"content\":{\"delta\":[]},\"filename\":\"test-document.md\"}}");
        } catch (Exception e) {
            // 處理可能的異常
        }
        
        // 手動創建控制器實例
        webOnlineFileController = new WebOnlineFileController(
            userService, fileServiceStrategy, fileProperties, validationService,
            permissionService, objectMapper, filePermissionRuleManager, eventSink
        );

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setRole(RoleEnum.USER);

        testOnlineFile = new UserFileMetadata();
        testOnlineFile.setId(1L);
        testOnlineFile.setUserId(1L);
        testOnlineFile.setFilename("test-document.md");
        testOnlineFile.setFileType(FileEnum.ONLINE_DOCUMENT);

        MockServerHttpRequest request = MockServerHttpRequest.get("/web/v1/docs").build();
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
    
    private UserFileDataBO createTestUserFileDataBO() {
        UserFileDataBO dataBO = new UserFileDataBO();
        dataBO.setFilename("test-document.md");
        
        // 創建 EditorContentDTO - 簡單的測試內容
        EditorContentDTO editorContent = EditorContentDTO.builder()
                .delta(java.util.List.of()) // 空的 delta 列表用於測試
                .build();
        dataBO.setContent(editorContent);
        
        dataBO.setFileSize(1024L);
        dataBO.setMimeType("text/markdown");
        return dataBO;
    }
    
    private FileMetadataDTO createTestFileMetadata() {
        FileMetadataDTO metadata = new FileMetadataDTO();
        metadata.setFilename("test.txt");
        metadata.setFileSize(1024L);
        return metadata;
    }

    @Test
    @DisplayName("一般測試 - 控制器初始化")
    void testControllerInitialization() {
        assertNotNull(webOnlineFileController);
        assertTrue(webOnlineFileController instanceof xyz.dowob.filemanagement.controller.base.BaseOnlineFileController);
    }

    @Test
    @DisplayName("一般測試 - uploadFile Web 創建在線檔案")
    void testUploadFile_createOnlineDocument() {
        FileMetadataDTO fileMetadataDTO = new FileMetadataDTO();
        fileMetadataDTO.setFilename("new-document.md");
        fileMetadataDTO.setFileType(FileEnum.ONLINE_DOCUMENT);

        when(userService.getUser(testExchange)).thenReturn(Mono.just(testUser));
        when(validationService.validateFileMetadataDTO(fileMetadataDTO, testUser))
                .thenReturn(Mono.empty());
        when(permissionService.validateUserPermission(eq(testUser), any(List.class)))
                .thenReturn(Mono.just(Map.of()));
        when(fileService.uploadFile(fileMetadataDTO, testUser))
                .thenReturn(Mono.just(new xyz.dowob.filemanagement.data.file.dto.UploadResponseDTO()));

        StepVerifier.create(webOnlineFileController.uploadFile(fileMetadataDTO, testExchange))
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
    @DisplayName("一般測試 - downloadFile Web 下載在線檔案")
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
        when(fileService.downloadFile(eq(testOnlineFile), eq(testUser), any(String[].class)))
                .thenReturn(Mono.just(fileDataBO));

        StepVerifier.create(webOnlineFileController.downloadFile(fileId, action, testExchange))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(200, responseEntity.getStatusCode().value());
                    
                    Flux<DataBuffer> body = responseEntity.getBody();
                    assertNotNull(body);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - editFile Web 編輯在線檔案")
    void testEditFile_basicEdit() {
        FileEditDTO fileEditDTO = new FileEditDTO();
        fileEditDTO.setFileId("1");
        fileEditDTO.setParentFolderId(2L);
        fileEditDTO.setFilename("updated-document.md");
        
        // 創建 EditorContentDTO 對象
        EditorContentDTO content = new EditorContentDTO();
        // 設置編輯器內容 - 根據實際需要設置 delta 等
        fileEditDTO.setContent(content);

        when(validationService.validateEditFileDTO(fileEditDTO, false))
                .thenReturn(Mono.empty());
        when(validationService.validSpecifyColumns(fileEditDTO, "fileId"))
                .thenReturn(Mono.empty());
        when(userService.getUser(testExchange)).thenReturn(Mono.just(testUser));
        when(filePermissionRuleManager.getAllowShared()).thenReturn(allowSharedRule);
        when(allowSharedRule.check(any(), any())).thenReturn(Mono.empty());
        when(permissionService.validateUserPermission(eq(testUser), any(List.class), any(List.class)))
                .thenReturn(Mono.just(Map.of(1L, testOnlineFile, 2L, testOnlineFile)));
        when(validationService.validateFileType(eq(testOnlineFile), any(FileEnum[].class)))
                .thenReturn(Mono.empty());
        when(fileServiceStrategy.getFileService(FileEnum.ONLINE_DOCUMENT)).thenReturn(fileService);
        when(fileService.editFile(any(FileEditBO.class), eq(testUser))).thenReturn(Mono.empty());
        doNothing().when(eventSink).emit(any());

        StepVerifier.create(webOnlineFileController.editFile(fileEditDTO, testExchange))
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
    @DisplayName("一般測試 - Web 路由映射驗證")
    void testWebRouteMappings() {
        assertTrue(webOnlineFileController.getClass().isAnnotationPresent(org.springframework.web.bind.annotation.RestController.class));
        assertTrue(webOnlineFileController.getClass().isAnnotationPresent(org.springframework.web.bind.annotation.RequestMapping.class));
        
        org.springframework.web.bind.annotation.RequestMapping requestMapping = 
            webOnlineFileController.getClass().getAnnotation(org.springframework.web.bind.annotation.RequestMapping.class);
        assertEquals("/web/v1/docs", requestMapping.value()[0]);
    }

    @Test
    @DisplayName("異常測試 - uploadFile 驗證失敗")
    void testUploadFile_validationFailure() {
        FileMetadataDTO fileMetadataDTO = new FileMetadataDTO();
        fileMetadataDTO.setFilename("");

        when(userService.getUser(testExchange)).thenReturn(Mono.just(testUser));
        when(validationService.validateFileMetadataDTO(fileMetadataDTO, testUser))
                .thenReturn(Mono.error(new xyz.dowob.filemanagement.exception.ValidationException(
                    xyz.dowob.filemanagement.exception.ValidationException.ErrorCode.REQUEST_IS_INVALID, "filename")));

        StepVerifier.create(webOnlineFileController.uploadFile(fileMetadataDTO, testExchange))
                .expectError()
                .verify();
    }

    @Test
    @DisplayName("邊界測試 - EventSink 依賴驗證")
    void testEventSinkDependency() {
        assertNotNull(eventSink);
    }
}