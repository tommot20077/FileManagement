package xyz.dowob.filemanagement.controller.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nullable;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.RecordLevel;
import xyz.dowob.filemanagement.component.manager.FilePermissionRuleManager;
import xyz.dowob.filemanagement.component.manager.FolderListTreeManager;
import xyz.dowob.filemanagement.component.provider.provider.FolderListTreeProvider;
import xyz.dowob.filemanagement.component.strategy.FileServiceStrategy;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.customenum.DownloadActionEnum;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.customenum.LogLevelEnum;
import xyz.dowob.filemanagement.data.file.bo.FileEditBO;
import xyz.dowob.filemanagement.data.file.dto.FileEditDTO;
import xyz.dowob.filemanagement.entity.UserFileMetadata;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.functionInterface.Permission;
import xyz.dowob.filemanagement.service.serviceInterface.FolderService;
import xyz.dowob.filemanagement.service.serviceInterface.PermissionService;
import xyz.dowob.filemanagement.service.serviceInterface.UserService;
import xyz.dowob.filemanagement.service.serviceInterface.ValidationService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 基於反應式編程的資料夾控制器抽象基類，實現資料夾管理的完整業務邏輯。
 * 
 * <p>此類繼承自 BaseFileController，專門處理資料夾相關的 CRUD 操作，
 * 包括資料夾的建立、刪除、編輯、下載以及樹狀結構管理。
 * 透過權限驗證機制確保使用者對資料夾操作的安全性。</p>
 * 
 * <p>提供的核心功能包括：資料夾的完整生命週期管理、權限控制、
 * 路徑查詢、樹狀結構初始化以及回收站操作。</p>
 * 
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@RecordLevel(LogLevelEnum.INFO)
public abstract class BaseFolderController extends BaseFileController {

    /**
     * 資料夾列表樹管理器，負責管理和維護使用者資料夾的樹狀結構。
     * 
     * <p>提供資料夾結構的視圖化支援，支援動態樹狀結構的建立和管理。</p>
     */
    protected final FolderListTreeManager folderListTreeManager;

    /**
     * 資料夾業務層服務，提供資料夾相關的核心業務邏輯實現。
     * 
     * <p>負責執行資料夾的建立、刪除、修改、查詢等操作，
     * 並確保業務規則的正確執行。</p>
     */
    protected final FolderService folderService;


    /**
     * 依賴注入的構造方法，用於初始化資料夾控制器。
     *
     * @param userService               用戶服務，負責用戶相關操作。
     * @param permissionService         權限服務，處理用戶操作的權限校驗。
     * @param fileServiceStrategy       檔案服務策略，根據不同的檔案操作提供相應的檔案服務。
     * @param fileProperties            檔案屬性設定，用於加載系統層級的檔案屬性設定。
     * @param validationService         驗證服務，對請求參數進行校驗。
     * @param folderService             資料夾業務層服務。
     * @param objectMapper              對象映射工具，用於將 Java 對象與 JSON 之間進行轉換。
     * @param folderListTreeManager     資料夾樹管理器，處理資料夾樹狀結構的初始化和管理。
     * @param filePermissionRuleManager 檔案權限規則管理器，處理檔案的權限規則。
     */
    public BaseFolderController(UserService userService, PermissionService<UserFileMetadata> permissionService, FileServiceStrategy fileServiceStrategy, FileProperties fileProperties, ValidationService validationService, FolderService folderService, ObjectMapper objectMapper, FilePermissionRuleManager filePermissionRuleManager,
                                @Nullable FolderListTreeManager folderListTreeManager) {
        super(userService, fileServiceStrategy, fileProperties, validationService, permissionService, objectMapper, filePermissionRuleManager);
        this.folderListTreeManager = folderListTreeManager;
        this.folderService = folderService;
    }


    /**
     * 永久刪除指定資料夾及其包含的所有子檔案和子資料夾。
     * <p>
     * 此方法實現了強大且危險的資料夾永久刪除功能，將完全移除指定資料夾及其所有內容。
     * 操作不可逆轉，需要謹慎使用。
     * </p>
     * <p>
     * <strong>刪除範圍：</strong>
     * </p>
     * <ul>
     *   <li>指定的資料夾本身</li>
     *   <li>資料夾內的所有檔案（包含已刪除的檔案）</li>
     *   <li>資料夾內的所有子資料夾（遞迴刪除）</li>
     *   <li>相關的檔案元資料和權限記錄</li>
     * </ul>
     * <p>
     * <strong>權限檢查：</strong>
     * </p>
     * <ul>
     *   <li><strong>擁有者權限：</strong>只有資料夾的擁有者才能執行刪除</li>
     *   <li><strong>非搜尋操作：</strong>禁止在搜尋結果中執行刪除操作</li>
     *   <li><strong>檔案類型驗證：</strong>確保指定 ID 對應的是資料夾而非一般檔案</li>
     * </ul>
     * <p>
     * <strong>安全機制：</strong>
     * </p>
     * <ul>
     *   <li>多層權限驗證，防止未授權操作</li>
     *   <li>使用者身份驗證，確保操作合法性</li>
     *   <li>檔案類型檢查，防止誤刪除一般檔案</li>
     * </ul>
     * <p>
     * <strong>使用範例：</strong>
     * </p>
     * <pre>
     * // 刪除用戶的個人資料夾
     * deleteFolder("12345", exchange)
     *     .doOnSuccess(response -&gt; log.info("資料夾刪除成功"))
     *     .doOnError(error -&gt; log.error("資料夾刪除失敗", error))
     * </pre>
     * <p>
     * <strong>重要警告：</strong>
     * </p>
     * <ul>
     *   <li><strong>不可逆操作：</strong>刪除後無法恢復，請慎重操作</li>
     *   <li><strong>影響範圍大：</strong>可能影響大量檔案和子資料夾</li>
     *   <li><strong>性能影響：</strong>大型資料夾的刪除可能耗時較長</li>
     * </ul>
     * 
     * @param id 要刪除的資料夾唯一標識符，必須為有效的資料夾 ID
     * @param exchange Web 交換對象，包含用戶身份和請求上下文資訊
     * @return Mono&lt;ResponseEntity&lt;?&gt;&gt; 非阻塞式回應，成功時返回 200 OK，失敗時返回相應錯誤狀態
     * @see FilePermissionRuleManager
     * @see FolderService#deleteFolder(UserFileMetadata, xyz.dowob.filemanagement.entity.User)
     * @apiNote 此為永久刪除操作，與移至回收站的 removeFile 不同
     * @implNote 使用反應式編程模式，適合高併發環境
     */
    public Mono<ResponseEntity<?>> deleteFolder(String id, ServerWebExchange exchange) {
        List<Permission<UserFileMetadata>> rules = new ArrayList<>(List.of(filePermissionRuleManager.getAllowOwner(),
                                                                           filePermissionRuleManager.getBlockNotSearchOperation()
        ));
        Mono<ResponseEntity<?>> result = userService
                .getUser(exchange)
                .flatMap(user -> permissionService
                        .validateUserPermission(user, Long.parseLong(id), rules)
                        .flatMap(file -> validationService.validateFileType(file, FileEnum.FOLDER).then(folderService.deleteFolder(file, user)))
                        .then(createResponseEntity(createApiResponse(exchange, "刪除資料夾成功", null))));
        return handleError(result, exchange);
    }


    /**
     * 編輯和更新資料夾的各種屬性，包括名稱、位置、分享設定等。
     * <p>
     * 此方法提供了全面的資料夾編輯功能，支援多種屬性的同時更新。
     * 包括安全檢查、權限驗證和業務規則檢查，確保操作的安全性和合理性。
     * </p>
     * <p>
     * <strong>支援的編輯操作：</strong>
     * </p>
     * <ul>
     *   <li><strong>名稱修改：</strong>更新資料夾的顯示名稱</li>
     *   <li><strong>位置移動：</strong>將資料夾移動到不同的父資料夾</li>
     *   <li><strong>分享設定：</strong>修改資料夾的共享權限和可見性</li>
     *   <li><strong>描述更新：</strong>更新資料夾的描述信息</li>
     * </ul>
     * <p>
     * <strong>驗證流程：</strong>
     * </p>
     * <ol>
     *   <li><strong>輸入檢查：</strong>驗證 FileEditDTO 的必要欄位和格式</li>
     *   <li><strong>權限驗證：</strong>檢查用戶對目標資料夾的編輯權限</li>
     *   <li><strong>父資料夾檢查：</strong>如果指定了新的父資料夾，驗證其存在性和權限</li>
     *   <li><strong>類型檢查：</strong>確保目標是資料夾而非一般檔案</li>
     * </ol>
     * <p>
     * <strong>業務規則：</strong>
     * </p>
     * <ul>
     *   <li>不能將資料夾移動到其子資料夾中（避免循環引用）</li>
     *   <li>不能將資料夾移動到沒有權限的位置</li>
     *   <li>修改分享設定時需考慮子檔案的權限繼承</li>
     * </ul>
     * <p>
     * <strong>錯誤處理：</strong>
     * </p>
     * <ul>
     *   <li>輸入驗證失敗時返回 400 Bad Request</li>
     *   <li>權限不足時返回 403 Forbidden</li>
     *   <li>資料夾不存在時返回 404 Not Found</li>
     * </ul>
     * <p>
     * <strong>使用範例：</strong>
     * </p>
     * <pre>
     * FileEditDTO editDTO = FileEditDTO.builder()
     *     .fileId("12345")
     *     .filename("新資料夾名稱")
     *     .parentFolderId(67890L)
     *     .isShared(true)
     *     .build();
     * 
     * editFolder(editDTO, exchange)
     *     .doOnSuccess(response -&gt; log.info("資料夾編輯成功"))
     * </pre>
     * 
     * @param fileEditDTO 檔案編輯資料傳輸對象，包含所有要修改的屬性資訊
     * @param exchange Web 交換對象，包含用戶身份和請求上下文
     * @return Mono&lt;ResponseEntity&lt;?&gt;&gt; 非阻塞式回應，成功時返回 200 OK，失敗時返回相應錯誤狀態
     * @see FileEditDTO
     * @see FileEditBO
     * @see FolderService#editFolder(FileEditBO, xyz.dowob.filemanagement.entity.User)
     * @apiNote 此方法支援批量屬性更新，但需要遵循原子性原則
     * @implNote 使用業務對象 FileEditBO 封裝編輯操作，確保數據一致性
     */
    public Mono<ResponseEntity<?>> editFolder(FileEditDTO fileEditDTO, ServerWebExchange exchange) {
        Mono<ResponseEntity<?>> responseEntityMono = validationService
                .validateEditFileDTO(fileEditDTO, true)
                .then(validationService.validSpecifyColumns(fileEditDTO, "fileId"))
                .then(userService.getUser(exchange))
                .flatMap(user -> {
                    List<Long> fileIds = new ArrayList<>();

                    Long fileId = Long.parseLong(fileEditDTO.getFileId());
                    fileIds.add(fileId);
                    if (fileEditDTO.getParentFolderId() != null) {
                        fileIds.add(fileEditDTO.getParentFolderId());
                    }
                    return permissionService.validateUserPermission(user, fileIds).flatMap(map -> {
                        FileEditBO fileEditBO = new FileEditBO(fileEditDTO);
                        fileEditBO.setUserFileMetadata(map.get(fileId));
                        fileEditBO.setParentFolderFileMetadata(map.get(fileEditDTO.getParentFolderId()));
                        return validationService
                                .validateFileType(fileEditBO.getUserFileMetadata(), FileEnum.FOLDER)
                                .then(validationService.validateFileType(fileEditBO.getParentFolderFileMetadata(), FileEnum.FOLDER))
                                .then(folderService.editFolder(fileEditBO, user));
                    });
                })
                .then(createResponseEntity(createApiResponse(exchange, "資料夾更新成功", null)));
        return handleError(responseEntityMono, exchange);
    }


    /**
     * 在指定位置創建新的資料夾，支援巢狀目錄結構和權限控制。
     * <p>
     * 此方法提供了完整的資料夾創建功能，支援在任意有權限的位置創建資料夾。
     * 包括適當的權限檢查、名稱驗證和目錄結構維護，確保資料夾的合理性和安全性。
     * </p>
     * <p>
     * <strong>創建特性：</strong>
     * </p>
     * <ul>
     *   <li><strong>靖活位置：</strong>支援在根目錄或任意子資料夾中創建</li>
     *   <li><strong>自動權限：</strong>新資料夾繼承父資料夾的權限設定</li>
     *   <li><strong>名稱驗證：</strong>檢查資料夾名稱的合法性和唯一性</li>
     *   <li><strong>巢狀支援：</strong>支援多層級資料夾結構</li>
     * </ul>
     * <p>
     * <strong>驗證檢查：</strong>
     * </p>
     * <ol>
     *   <li><strong>輸入驗證：</strong>檢查 FileEditDTO 的必要欄位</li>
     *   <li><strong>父資料夾檢查：</strong>如果指定了父資料夾，驗證其存在性和權限</li>
     *   <li><strong>權限驗證：</strong>確保用戶有在指定位置創建資料夾的權限</li>
     *   <li><strong>類型檢查：</strong>確保父目錄是資料夾類型</li>
     * </ol>
     * <p>
     * <strong>預設設定：</strong>
     * </p>
     * <ul>
     *   <li>新資料夾的擁有者為當前用戶</li>
     *   <li>繼承父資料夾的可見性設定</li>
     *   <li>初始權限設為私有（非共享）</li>
     * </ul>
     * <p>
     * <strong>錯誤處理：</strong>
     * </p>
     * <ul>
     *   <li><strong>名稱衝突：</strong>在同一位置已存在同名資料夾</li>
     *   <li><strong>權限不足：</strong>用戶沒有在指定位置創建的權限</li>
     *   <li><strong>非法位置：</strong>父資料夾不存在或不是資料夾類型</li>
     * </ul>
     * <p>
     * <strong>使用範例：</strong>
     * </p>
     * <pre>
     * // 在根目錄創建資料夾
     * FileEditDTO rootFolder = FileEditDTO.builder()
     *     .filename("我的文檔")
     *     .parentFolderId(null) // 根目錄
     *     .build();
     * 
     * // 在指定資料夾內創建子資料夾
     * FileEditDTO subFolder = FileEditDTO.builder()
     *     .filename("子資料夾")
     *     .parentFolderId(12345L)
     *     .build();
     * </pre>
     * 
     * @param fileEditDTO 資料夾創建資料傳輸對象，包含名稱、位置等信息
     * @param exchange Web 交換對象，包含用戶身份和請求上下文
     * @return Mono&lt;ResponseEntity&lt;?&gt;&gt; 非阻塞式回應，成功時返回 200 OK 和新資料夾資訊
     * @see FileEditDTO
     * @see FolderService#createFolder(FileEditDTO, xyz.dowob.filemanagement.entity.User)
     * @apiNote 創建的資料夾將立即可用於檔案上傳和其他操作
     * @implNote 使用事務性操作確保資料一致性，失敗時自動回滾
     */
    public Mono<ResponseEntity<?>> createFolder(FileEditDTO fileEditDTO, ServerWebExchange exchange) {
        return handleError(validationService.validateEditFileDTO(fileEditDTO, true).then(userService.getUser(exchange)).flatMap(user -> {
                               Mono<UserFileMetadata> parentFolderMono = Mono.empty();

                               if (fileEditDTO.getParentFolderId() != null) {
                                   parentFolderMono = permissionService
                                           .validateUserPermission(user, fileEditDTO.getParentFolderId())
                                           .flatMap(file -> validationService.validateFileType(file, FileEnum.FOLDER));
                               }
                               return parentFolderMono
                                       .then(folderService.createFolder(fileEditDTO, user))
                                       .then(createResponseEntity(createApiResponse(exchange, "資料夾建立成功", null)));
                           }), exchange
        );
    }


    /**
     * 獲取指定資料夾的完整路徑铈，從根目錄到目標資料夾的所有上級資料夾。
     * <p>
     * 此方法提供了導航功能所需的路徑資訊，将資料夾的層次結構以易於理解的方式呈現。
     * 用於在前端顯示面包屑導航、檔案瀏覽器路徑顯示等功能。
     * </p>
     * <p>
     * <strong>路徑資訊包含：</strong>
     * </p>
     * <ul>
     *   <li><strong>目錄名稱：</strong>每個資料夾的顯示名稱</li>
     *   <li><strong>資料夾 ID：</strong>每個資料夾的唯一標識符</li>
     *   <li><strong>層次結構：</strong>從根目錄到目標的完整路徑</li>
     *   <li><strong>權限信息：</strong>用戶對每個資料夾的訪問權限</li>
     * </ul>
     * <p>
     * <strong>權限檢查：</strong>
     * </p>
     * <ul>
     *   <li><strong>訪問權限：</strong>用戶必須對目標資料夾有訪問權限</li>
     *   <li><strong>共享檔案：</strong>支援訪問共享給用戶的資料夾</li>
     *   <li><strong>路徑安全：</strong>只返回用戶有權限訪問的路徑部分</li>
     * </ul>
     * <p>
     * <strong>輸出格式：</strong>
     * </p>
     * <pre>
     * {
     *   "filePaths": [
     *     {"id": null, "name": "root"},
     *     {"id": 1, "name": "我的文檔"},
     *     {"id": 12, "name": "工作文檔"},
     *     {"id": 123, "name": "目標資料夾"}
     *   ]
     * }
     * </pre>
     * <p>
     * <strong>使用場景：</strong>
     * </p>
     * <ul>
     *   <li><strong>面包屑導航：</strong>在檔案瀏覽器中顯示當前位置</li>
     *   <li><strong>路徑編輯：</strong>在檔案上傳或移動時選擇目標位置</li>
     *   <li><strong>樊狀檢視：</strong>顯示檔案的完整組織結構</li>
     * </ul>
     * <p>
     * <strong>性能優化：</strong>
     * </p>
     * <ul>
     *   <li>使用緩存機制避免重複查詢</li>
     *   <li>懶性加載，只加載必要的資料夾信息</li>
     *   <li>支援大量巢狀資料夾的高效處理</li>
     * </ul>
     * 
     * @param exchange Web 交換對象，包含用戶身份和請求上下文
     * @param fileId 目標資料夾的唯一標識符，用於定位要獲取路徑的資料夾
     * @return Mono&lt;ResponseEntity&lt;?&gt;&gt; 包含資料夾路徑信息的非阻塞式回應
     * @see FolderListTreeProvider.FolderNode
     * @see FolderService#getUserFilePaths(UserFileMetadata, xyz.dowob.filemanagement.entity.User)
     * @apiNote 路徑信息包含完整的層次結構，適合用於導航組件
     * @implNote 使用權限管理機制確保路徑安全性，只顯示允許訪問的部分
     */
    public Mono<ResponseEntity<?>> getFolderPath(ServerWebExchange exchange, Long fileId) {
        Mono<ResponseEntity<?>> action = userService.getUser(exchange).flatMap(user -> Mono.defer(() -> {
            HashMap<String, Object> result = new HashMap<>();
            return permissionService
                    .validateUserPermission(user, fileId, FilePermissionRuleManager.DefaultRule.WITH_SHARED.getRules(filePermissionRuleManager))
                    .flatMap(file -> validationService
                            .validateFileType(file, FileEnum.FOLDER)
                            .then(folderService.getUserFilePaths(file, user).flatMap(list -> {
                                result.put("filePaths", list);
                                return Mono.just(result);
                            })));
        }).flatMap(result -> createResponseEntity(createApiResponse(exchange, "獲取用戶檔案路徑成功", result))));
        return handleError(action, exchange);
    }


    /**
     * 建立用戶資料夾樹，根據系統設定和用戶資料夾結構建立資料夾樹。
     *
     * @param exchange 請求對象，包含請求上下文信息。
     *
     * @return 回傳建立資料夾樹的結果，成功回傳 OK，失敗回傳 BAD_REQUEST。
     */
    public Mono<ResponseEntity<?>> buildTree(ServerWebExchange exchange) {
        return handleError(userService.getUser(exchange).flatMap(user -> {
                               if (!fileProperties.getGlobal().getEnableUserFolderListTree()) {
                                   return createResponseEntity(createApiResponse(exchange, "當前設定不支持建立用戶檔案樹", null));
                               }
                               if (folderListTreeManager != null) {
                                   CompletableFuture.runAsync(() -> folderListTreeManager.initializeTree(user.getId()));
                               }
                               return (createResponseEntity(createApiResponse(exchange, "請求建立用戶檔案樹成功", null)));
                           }), exchange
        );
    }


    /**
     * 移動資料夾到回收站。
     *
     * @param exchange 請求對象，包含請求上下文信息。
     * @param id       資料夾 ID，用來標識要回收的資料夾。
     *
     * @return 回傳回收結果，成功回傳 OK，失敗回傳 BAD_REQUEST。
     */
    public Mono<ResponseEntity<?>> removeFile(ServerWebExchange exchange, String id) {
        return super.removeFile(exchange, id, FileEnum.FOLDER);
    }


    /**
     * 還原資料夾，將回收站中的資料夾還原到原來的位置。
     *
     * @param exchange 請求對象，包含請求上下文信息。
     * @param id       資料夾 ID，用來標識要還原的資料夾。
     *
     * @return 回傳還原結果，成功回傳 OK，失敗回傳 BAD_REQUEST。
     */
    public Mono<ResponseEntity<?>> restoreFile(ServerWebExchange exchange, String id) {
        return super.restoreFile(exchange, id, FileEnum.FOLDER);
    }


    /**
     * 下載資料夾，將資料夾及其內容打包下載。
     *
     * @param id       資料夾 ID，用來標識要下載的資料夾。
     * @param exchange 請求對象，包含請求上下文信息。
     *
     * @return 回傳下載結果，成功回傳 OK，失敗回傳 BAD_REQUEST。
     */
    public Mono<ResponseEntity<Flux<DataBuffer>>> downloadFolder(Long id, ServerWebExchange exchange) {
        return userService.getUser(exchange).flatMap(user -> {
            return permissionService
                    .validateUserPermission(user, id, FilePermissionRuleManager.DefaultRule.WITH_SHARED.getRules(filePermissionRuleManager))
                    .flatMap(folder -> validationService
                            .validateFileType(folder, FileEnum.FOLDER)
                            .then(folderService.downloadFolder(folder, user).map(userFileDataBO -> {
                                HttpHeaders headers = prepareHttpHeaders(DownloadActionEnum.DOWNLOAD, userFileDataBO, null, false);
                                return ResponseEntity.status(HttpStatus.OK).headers(headers).body(userFileDataBO.getDataBufferFlux());
                            })));
        }).onErrorResume(ValidationException.class, e -> handleDownloadValidationError(e, exchange));
    }
}
