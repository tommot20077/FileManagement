package xyz.dowob.filemanagement.controller.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.ArgumentMatchers;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.component.manager.FilePermissionRuleManager;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.config.properties.SecurityProperties;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.customenum.RoleEnum;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.entity.UserFileMetadata;
import xyz.dowob.filemanagement.functionInterface.Permission;
import xyz.dowob.filemanagement.service.serviceInterface.PermissionService;
import xyz.dowob.filemanagement.service.serviceInterface.UserService;
import xyz.dowob.filemanagement.service.serviceInterface.ValidationService;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Controller 測試的通用工具類
 * 
 * 提供共用的 Mock 配置和測試數據設置，以減少重複代碼並確保一致的測試設置。
 * 此類解決了 Controller 層測試中常見的 Mock 配置問題：
 * - NullPointer 異常（Mock 對象返回 null）
 * - 鏈式調用中斷
 * - JSON 序列化失敗
 * - 權限規則配置缺失
 * 
 * 前置條件：
 * - Mockito 測試環境已配置
 * - Spring Boot 測試上下文可用
 * 
 * 測試步驟：
 * - 繼承此類或使用其靜態方法配置 Mock 對象
 * - 調用相應的設置方法初始化測試環境
 * 
 * 預期結果：
 * - Mock 對象配置完整，避免 NullPointer 異常
 * - 權限規則正確設置
 * - JSON 序列化正常工作
 * 
 * @author yuan
 */
public class BaseControllerTestUtils {

    /**
     * 配置基本的 UserService Mock 對象
     * 解決用戶認證和獲取相關的 NullPointer 問題
     * 
     * @param userService Mock 的 UserService
     * @param testUser 測試用戶對象
     * @param exchange 測試用的 ServerWebExchange
     */
    public static void setupUserServiceMocks(UserService userService, User testUser, ServerWebExchange exchange) {
        // 基本用戶獲取
        when(userService.getUser(exchange)).thenReturn(Mono.just(testUser));
        when(userService.getUser(any(ServerWebExchange.class))).thenReturn(Mono.just(testUser));
        
        // 用戶查詢相關
        when(userService.getAll()).thenReturn(Flux.just(testUser));
        when(userService.getAllByParams(anyString(), any())).thenReturn(Flux.just(testUser));
        
        // 登出操作
        when(userService.logout(any(Long.class), any(ServerWebExchange.class))).thenReturn(Mono.empty());
    }

    /**
     * 配置 ValidationService Mock 對象
     * 解決驗證服務相關的配置缺失問題
     * 
     * @param validationService Mock 的 ValidationService
     */
    public static void setupValidationServiceMocks(ValidationService validationService) {
        // 基本驗證方法返回空 Mono（表示驗證通過）
        when(validationService.validateEditFileDTO(any(), any(Boolean.class))).thenReturn(Mono.empty());
        when(validationService.validSpecifyColumns(any(), anyString())).thenReturn(Mono.empty());
        when(validationService.validateFileType(any(), any(FileEnum.class))).thenReturn(Mono.empty());
        when(validationService.validateFileType(any(), any(FileEnum[].class))).thenReturn(Mono.empty());
        when(validationService.validateUserSearchList(any())).thenReturn(Mono.empty());
        
        // 對於 null 參數的處理 - 使用 ArgumentMatchers.isNull() 代替直接傳 null
        when(validationService.validateFileType(ArgumentMatchers.isNull(), any(FileEnum.class))).thenReturn(Mono.empty());
    }

    /**
     * 配置 ObjectMapper Mock 對象
     * 解決 JSON 序列化相關的 NullPointer 問題
     * 
     * @param objectMapper Mock 的 ObjectMapper
     */
    public static void setupObjectMapperMocks(ObjectMapper objectMapper) {
        try {
            // 配置基本的序列化操作
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            when(objectMapper.writeValueAsBytes(any())).thenReturn("{}".getBytes());
        } catch (Exception e) {
            // 忽略異常，因為這是 Mock 配置
        }
    }

    /**
     * 配置檔案權限規則管理器 Mock 對象
     * 解決權限規則相關的配置缺失問題
     * 
     * @param filePermissionRuleManager Mock 的 FilePermissionRuleManager
     * @param allowOwnerRule Mock 的 allowOwner 規則
     * @param blockNotSearchOperationRule Mock 的 blockNotSearchOperation 規則
     * @param blockDeletedRule Mock 的 blockDeleted 規則
     * @param allowSharedRule Mock 的 allowShared 規則
     */
    public static void setupFilePermissionRuleManagerMocks(
            FilePermissionRuleManager filePermissionRuleManager,
            Permission<UserFileMetadata> allowOwnerRule,
            Permission<UserFileMetadata> blockNotSearchOperationRule,
            Permission<UserFileMetadata> blockDeletedRule,
            Permission<UserFileMetadata> allowSharedRule) {
        
        when(filePermissionRuleManager.getAllowOwner()).thenReturn(allowOwnerRule);
        when(filePermissionRuleManager.getBlockNotSearchOperation()).thenReturn(blockNotSearchOperationRule);
        when(filePermissionRuleManager.getBlockDeleted()).thenReturn(blockDeletedRule);
        when(filePermissionRuleManager.getAllowShared()).thenReturn(allowSharedRule);
    }

    /**
     * 配置 FileProperties Mock 對象
     * 解決檔案屬性配置相關的問題
     * 
     * @param fileProperties Mock 的 FileProperties
     * @param globalProperties Mock 的 Global 屬性
     */
    public static void setupFilePropertiesMocks(FileProperties fileProperties, FileProperties.Global globalProperties) {
        when(fileProperties.getGlobal()).thenReturn(globalProperties);
        when(globalProperties.getEnableUserFolderListTree()).thenReturn(true);
    }

    /**
     * 配置 SecurityProperties Mock 對象
     * 解決安全屬性配置相關的問題
     * 
     * @param securityProperties Mock 的 SecurityProperties
     */
    public static void setupSecurityPropertiesMocks(SecurityProperties securityProperties) {
        SecurityProperties.Cookie cookieProperties = new SecurityProperties.Cookie();
        cookieProperties.setTokenName("jwt-token");
        cookieProperties.setHttpOnly(true);
        cookieProperties.setSecure(false);
        cookieProperties.setSameSite("Lax");
        
        when(securityProperties.getCookie()).thenReturn(cookieProperties);
    }

    /**
     * 創建標準的測試用戶對象
     * 
     * @return 配置好的測試用戶
     */
    public static User createTestUser() {
        User testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setRole(RoleEnum.USER);
        return testUser;
    }

    /**
     * 創建管理員測試用戶對象
     * 
     * @return 配置好的管理員用戶
     */
    public static User createAdminUser() {
        User adminUser = new User();
        adminUser.setId(2L);
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@example.com");
        adminUser.setRole(RoleEnum.ADMIN);
        return adminUser;
    }

    /**
     * 創建標準的測試資料夾對象
     * 
     * @return 配置好的測試資料夾
     */
    public static UserFileMetadata createTestFolder() {
        UserFileMetadata testFolder = new UserFileMetadata();
        testFolder.setId(1L);
        testFolder.setUserId(1L);
        testFolder.setFilename("testfolder");
        testFolder.setFileType(FileEnum.FOLDER);
        return testFolder;
    }

    /**
     * 創建標準的測試 ServerWebExchange 對象
     * 
     * @return 配置好的測試 Exchange
     */
    public static ServerWebExchange createTestExchange() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
        return MockServerWebExchange.from(request);
    }

    /**
     * 配置權限服務的常用 Mock 行為
     * 解決權限驗證相關的 Mock 配置問題
     * 
     * @param permissionService Mock 的 PermissionService
     * @param testUser 測試用戶
     * @param testFile 測試檔案/資料夾
     */
    @SuppressWarnings("unchecked")
    public static void setupPermissionServiceMocks(
            PermissionService<UserFileMetadata> permissionService, 
            User testUser, 
            UserFileMetadata testFile) {
        
        // 單一檔案權限驗證
        when(permissionService.validateUserPermission(eq(testUser), eq(testFile.getId())))
                .thenReturn(Mono.just(testFile));
        
        // 帶規則的權限驗證
        when(permissionService.validateUserPermission(any(User.class), any(Long.class), any(java.util.List.class)))
                .thenReturn(Mono.just(testFile));
        
        // 多檔案權限驗證 - 使用泛型通配符解決類型匹配問題
        when(permissionService.validateUserPermission(any(User.class), any(java.util.List.class)))
                .thenReturn((Mono) Mono.just(java.util.Map.of(testFile.getId(), testFile)));
    }
}