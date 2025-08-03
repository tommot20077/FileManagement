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
import xyz.dowob.filemanagement.component.limiter.UserLimiter;
import xyz.dowob.filemanagement.component.manager.FilePermissionRuleManager;
import xyz.dowob.filemanagement.component.strategy.FileServiceStrategy;
import xyz.dowob.filemanagement.component.strategy.UserLimiterStrategy;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.customenum.RoleEnum;
import xyz.dowob.filemanagement.customenum.TransmissionEnum;
import xyz.dowob.filemanagement.customenum.UserLimiterEnum;
import xyz.dowob.filemanagement.data.file.bo.UserFileDataBO;
import xyz.dowob.filemanagement.data.file.dto.FileMetadataDTO;
import xyz.dowob.filemanagement.data.file.dto.UploadResponseDTO;
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
import static org.mockito.Mockito.when;

/**
 * WebGeneralFileController 測試類
 * 
 * 測試 WebGeneralFileController Web 檔案控制器的基本功能
 * 
 * @author yuan
 */
@SpringBootTest
@ActiveProfiles({"test", "demo", "ci"})
@DisplayName("WebGeneralFileController Web 檔案控制器測試")
class WebGeneralFileControllerTest {

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
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;
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

    private WebGeneralFileController webGeneralFileController;

    private User testUser;
    private UserFileMetadata testFile;
    private ServerWebExchange testExchange;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // 首先初始化測試對象
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setRole(RoleEnum.USER);

        testFile = new UserFileMetadata();
        testFile.setId(1L);
        testFile.setUserId(1L);
        testFile.setFilename("test-file.txt");
        testFile.setFileType(FileEnum.DOCUMENT);

        MockServerHttpRequest request = MockServerHttpRequest.get("/web/v1/files").build();
        testExchange = MockServerWebExchange.from(request);

        // 配置 FileProperties Mock
        when(fileProperties.getUpload()).thenReturn(uploadProperties);
        when(fileProperties.getDownload()).thenReturn(downloadProperties);
        when(uploadProperties.isForceUseServerConfig()).thenReturn(false);
        when(uploadProperties.getDefaultUploadType()).thenReturn(TransmissionEnum.MULTIPART);
        when(downloadProperties.getDownloadCacheHeaderExpireTime()).thenReturn(java.time.Duration.ofHours(1));
        
        // 配置 FilePermissionRuleManager Mock
        when(filePermissionRuleManager.getAllowShared()).thenReturn(allowSharedRule);
        when(filePermissionRuleManager.getAllowOwner()).thenReturn(allowOwnerRule);
        when(filePermissionRuleManager.getBlockNotSearchOperation()).thenReturn(blockNotSearchRule);
        when(filePermissionRuleManager.getBlockDeleted()).thenReturn(blockDeletedRule);
        
        // 配置其他基本 Mock
        when(userService.getUser(any(ServerWebExchange.class))).thenReturn(Mono.just(createTestUser()));
        when(fileServiceStrategy.getFileService()).thenReturn(fileService);
        when(userLimiterStrategy.getUserLimiter(any())).thenReturn(userLimiter);
        when(validationService.validateFileMetadataDTO(any(), any())).thenReturn(Mono.empty());
        when(validationService.validateFileType(any(UserFileMetadata.class), any(FileEnum[].class))).thenReturn(Mono.just(testFile));
        when(permissionService.validateUserPermission(any(User.class), any(List.class), any())).thenReturn(Mono.just(java.util.Map.of()));
        when(fileService.uploadFile(any(), any())).thenReturn(Mono.just(createTestUploadResponse()));
        
        // 配置 ObjectMapper Mock 對象
        try {
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            // This won't happen in mocked scenario
        }
        
        // 手動創建控制器實例
        webGeneralFileController = new WebGeneralFileController(
            userService, fileServiceStrategy, fileProperties, 
            validationService, permissionService, userLimiterStrategy, 
            objectMapper, filePermissionRuleManager
        );
        
        // 配置 Permission Mock 對象的基本行為
        when(allowSharedRule.check(any(), any())).thenReturn(Mono.empty());
        when(allowOwnerRule.check(any(), any())).thenReturn(Mono.empty());
        when(blockNotSearchRule.check(any(), any())).thenReturn(Mono.empty());
        when(blockDeletedRule.check(any(), any())).thenReturn(Mono.empty());
        
        // 配置 FileServiceStrategy Mock 對象
        when(fileServiceStrategy.getFileService()).thenReturn(fileService);
        when(fileServiceStrategy.getFileService(any(FileEnum.class))).thenReturn(fileService);
    }
    
    private User createTestUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setRole(RoleEnum.USER);
        return user;
    }
    
    private UploadResponseDTO createTestUploadResponse() {
        UploadResponseDTO response = new UploadResponseDTO();
        response.setIsFinished(true);
        return response;
    }

    @Test
    @DisplayName("一般測試 - 控制器初始化")
    void testControllerInitialization() {
        assertNotNull(webGeneralFileController);
        assertTrue(webGeneralFileController instanceof xyz.dowob.filemanagement.controller.base.BaseGeneralFileController);
    }

    @Test
    @DisplayName("一般測試 - uploadFile Web 檔案上傳")
    void testUploadFile_basicFunctionality() {
        FileMetadataDTO fileMetadataDTO = new FileMetadataDTO();
        fileMetadataDTO.setFilename("test.txt");
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
        when(fileService.uploadFile(fileMetadataDTO, testUser))
                .thenReturn(Mono.just(uploadResponse));

        StepVerifier.create(webGeneralFileController.uploadFile(fileMetadataDTO, testExchange))
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
    @DisplayName("一般測試 - downloadFile Web 檔案下載")
    void testDownloadFile_basicFunctionality() {
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
                .thenReturn(Mono.just(testFile));
        when(fileService.downloadFile(testFile, testUser, null))
                .thenReturn(Mono.just(fileDataBO));

        StepVerifier.create(webGeneralFileController.downloadFile(action, fileId, testExchange))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(200, responseEntity.getStatusCode().value());
                    
                    Flux<DataBuffer> body = responseEntity.getBody();
                    assertNotNull(body);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - Web 路由映射驗證")
    void testWebRouteMappings() {
        assertTrue(webGeneralFileController.getClass().isAnnotationPresent(org.springframework.web.bind.annotation.RestController.class));
        assertTrue(webGeneralFileController.getClass().isAnnotationPresent(org.springframework.web.bind.annotation.RequestMapping.class));
        
        org.springframework.web.bind.annotation.RequestMapping requestMapping = 
            webGeneralFileController.getClass().getAnnotation(org.springframework.web.bind.annotation.RequestMapping.class);
        assertEquals("/web/v1/files", requestMapping.value()[0]);
    }

    @Test
    @DisplayName("異常測試 - uploadFile 用戶限制超出")
    void testUploadFile_userLimitExceeded() {
        FileMetadataDTO fileMetadataDTO = new FileMetadataDTO();
        fileMetadataDTO.setFilename("test.txt");

        when(userService.getUser(testExchange)).thenReturn(Mono.just(testUser));
        when(userLimiterStrategy.getUserLimiter(UserLimiterEnum.USER_UPLOAD_LIMITER))
                .thenReturn(userLimiter);
        when(userLimiter.tryAcquire(testUser.getId())).thenReturn(Mono.just(false));

        StepVerifier.create(webGeneralFileController.uploadFile(fileMetadataDTO, testExchange))
                .assertNext(responseEntity -> {
                    assertEquals(429, responseEntity.getStatusCode().value());
                    ApiResponseDTO<?> response = (ApiResponseDTO<?>) responseEntity.getBody();
                    assertNotNull(response);
                    assertTrue(response.getMessage().contains("限制錯誤"));
                })
                .verifyComplete();
    }
}