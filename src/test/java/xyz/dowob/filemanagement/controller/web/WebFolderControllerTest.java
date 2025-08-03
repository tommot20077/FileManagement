package xyz.dowob.filemanagement.controller.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.dowob.filemanagement.component.manager.FilePermissionRuleManager;
import xyz.dowob.filemanagement.component.manager.FolderListTreeManager;
import xyz.dowob.filemanagement.component.strategy.FileServiceStrategy;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.customenum.ReservedSearchIdEnum;
import xyz.dowob.filemanagement.customenum.RoleEnum;
import xyz.dowob.filemanagement.data.file.dto.FileEditDTO;
import xyz.dowob.filemanagement.data.file.dto.FileFilterDTO;
import xyz.dowob.filemanagement.data.file.dto.UserFileListDTO;
import xyz.dowob.filemanagement.data.response.ApiResponseDTO;
import xyz.dowob.filemanagement.data.response.PagedResponseDTO;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.entity.UserFileMetadata;
import xyz.dowob.filemanagement.functionInterface.Permission;
import xyz.dowob.filemanagement.service.serviceInterface.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * WebFolderController 測試類
 * 
 * 測試 WebFolderController Web 資料夾控制器的基本功能
 * 
 * @author yuan
 */
@SpringBootTest
@ActiveProfiles({"test", "demo", "ci"})
@DisplayName("WebFolderController Web 資料夾控制器測試")
class WebFolderControllerTest {

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
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    @Mock
    private FilePermissionRuleManager filePermissionRuleManager;
    @Mock
    private FolderListTreeManager folderListTreeManager;
    @Mock
    private FileService fileService;
    @Mock
    private Permission<UserFileMetadata> allowSharedRule;

    private WebFolderController webFolderController;

    private User testUser;
    private UserFileMetadata testFolder;
    private ServerWebExchange testExchange;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // 配置 FileProperties Mock
        when(fileProperties.getGlobal()).thenReturn(globalProperties);
        when(globalProperties.getEnableUserFolderListTree()).thenReturn(true);
        
        // 配置 FilePermissionRuleManager Mock
        when(filePermissionRuleManager.getAllowShared()).thenReturn(allowSharedRule);
        
        // 配置其他基本 Mock
        when(userService.getUser(any(ServerWebExchange.class))).thenReturn(Mono.just(createTestUser()));
        when(fileServiceStrategy.getFileService(any())).thenReturn(fileService);
        when(validationService.validateEditFileDTO(any(FileEditDTO.class), any(Boolean.class))).thenReturn(Mono.empty());
        when(permissionService.validateUserPermission(any(User.class), any(Long.class), any(List.class))).thenReturn(Mono.just(createTestFolder()));
        when(folderService.createFolder(any(), any())).thenReturn(Mono.empty());
        when(fileService.getUserFileList(any(), any())).thenReturn(Mono.just(createTestUserFileListDTO()));
        // FolderListTreeManager is @Nullable and doesn't need specific mocks
        
        // 手動創建控制器實例
        webFolderController = new WebFolderController(
            userService, permissionService, fileServiceStrategy, fileProperties,
            validationService, folderService, objectMapper, 
            filePermissionRuleManager, folderListTreeManager
        );
        
        // 配置 Permission Mock 對象的基本行為
        when(allowSharedRule.check(any(), any())).thenReturn(Mono.empty());
        
        // 配置 FileServiceStrategy Mock 對象
        when(fileServiceStrategy.getFileService(any())).thenReturn(fileService);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setRole(RoleEnum.USER);

        testFolder = new UserFileMetadata();
        testFolder.setId(1L);
        testFolder.setUserId(1L);
        testFolder.setFilename("test-folder");
        testFolder.setFileType(FileEnum.FOLDER);

        MockServerHttpRequest request = MockServerHttpRequest.get("/web/v1/folders").build();
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
    
    private UserFileMetadata createTestFolder() {
        UserFileMetadata folder = new UserFileMetadata();
        folder.setId(1L);
        folder.setUserId(1L);
        folder.setFilename("test-folder");
        folder.setFileType(FileEnum.FOLDER);
        return folder;
    }
    
    private PagedResponseDTO<UserFileListDTO> createTestUserFileListDTO() {
        UserFileListDTO fileList = new UserFileListDTO();
        return PagedResponseDTO.<UserFileListDTO>builder()
                .data(Collections.singletonList(fileList))
                .totalPages(1)
                .currentPage(1)
                .pageSize(10)
                .totalElements(1L)
                .build();
    }

    @Test
    @DisplayName("一般測試 - 控制器初始化")
    void testControllerInitialization() {
        assertNotNull(webFolderController);
        assertTrue(webFolderController instanceof xyz.dowob.filemanagement.controller.base.BaseFolderController);
    }

    @Test
    @DisplayName("一般測試 - getFolderFiles Web 獲取資料夾檔案列表")
    void testGetFolderFiles_basicFunctionality() {
        Long folderId = 1L;
        Integer page = 1;
        Integer size = 10;
        List<String> types = Arrays.asList("DOCUMENT");

        PagedResponseDTO<UserFileListDTO> pagedFiles = PagedResponseDTO.<UserFileListDTO>builder()
                .data(Arrays.asList(new UserFileListDTO()))
                .totalPages(1)
                .currentPage(page)
                .pageSize(size)
                .totalElements(1L)
                .build();

        when(userService.getUser(testExchange)).thenReturn(Mono.just(testUser));
        when(fileServiceStrategy.getFileService()).thenReturn(fileService);
        when(permissionService.validateUserPermission(eq(testUser), eq(folderId), any(List.class)))
                .thenReturn(Mono.just(testFolder));
        when(validationService.validateFileType(testFolder, FileEnum.FOLDER)).thenReturn(Mono.empty());
        when(fileService.getUserFileList(eq(testUser), any(FileFilterDTO.class)))
                .thenReturn(Mono.just(pagedFiles));
        when(fileService.getUserFilePaths(testFolder, testUser)).thenReturn(Mono.just(Collections.emptyList()));

        StepVerifier.create(webFolderController.getFolderFiles(folderId, page, size, types, testExchange))
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
    @DisplayName("一般測試 - getStarFiles Web 獲取星標檔案")
    void testGetStarFiles_basicFunctionality() {
        when(userService.getUser(testExchange)).thenReturn(Mono.just(testUser));
        when(fileServiceStrategy.getFileService()).thenReturn(fileService);
        when(permissionService.validateUserPermission(eq(testUser), eq(ReservedSearchIdEnum.STAR_FILE_ID.getId()), any(List.class)))
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

        StepVerifier.create(webFolderController.getStarFiles(testExchange, 1, 10, null))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(200, responseEntity.getStatusCode().value());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("一般測試 - createFolder Web 創建資料夾")
    void testCreateFolder_basicCreation() {
        FileEditDTO fileEditDTO = new FileEditDTO();
        fileEditDTO.setFilename("new-folder");

        when(validationService.validateEditFileDTO(fileEditDTO, true)).thenReturn(Mono.empty());
        when(userService.getUser(testExchange)).thenReturn(Mono.just(testUser));
        when(folderService.createFolder(fileEditDTO, testUser)).thenReturn(Mono.empty());

        StepVerifier.create(webFolderController.createFolder(fileEditDTO, testExchange))
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
    @DisplayName("一般測試 - buildTree Web 構建資料夾樹")
    void testBuildTree_basicFunctionality() {
        when(userService.getUser(testExchange)).thenReturn(Mono.just(testUser));

        StepVerifier.create(webFolderController.buildTree(testExchange))
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
    @DisplayName("一般測試 - Web 路由映射驗證")
    void testWebRouteMappings() {
        assertTrue(webFolderController.getClass().isAnnotationPresent(org.springframework.web.bind.annotation.RestController.class));
        assertTrue(webFolderController.getClass().isAnnotationPresent(org.springframework.web.bind.annotation.RequestMapping.class));
        
        org.springframework.web.bind.annotation.RequestMapping requestMapping = 
            webFolderController.getClass().getAnnotation(org.springframework.web.bind.annotation.RequestMapping.class);
        assertEquals("/web/v1/folders", requestMapping.value()[0]);
    }

    @Test
    @DisplayName("異常測試 - getFolderFiles 無效資料夾ID")
    void testGetFolderFiles_invalidFolderId() {
        Long invalidFolderId = -1L;

        StepVerifier.create(webFolderController.getFolderFiles(invalidFolderId, 1, 10, null, testExchange))
                .assertNext(responseEntity -> {
                    assertNotNull(responseEntity);
                    assertEquals(404, responseEntity.getStatusCode().value());
                })
                .verifyComplete();
    }
}