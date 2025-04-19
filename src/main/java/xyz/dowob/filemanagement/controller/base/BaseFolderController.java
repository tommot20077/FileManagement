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
import xyz.dowob.filemanagement.component.strategy.FileServiceStrategy;
import xyz.dowob.filemanagement.component.strategy.UserLimiterStrategy;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.customenum.DownloadActionEnum;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.customenum.LogLevelEnum;
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
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 資料夾的控制器抽象類，用於處理資料夾的請求。
 * 此控制器主要負責處理資料夾的增、刪、改、查操作，並繼承自 BaseFileController，後者負責處理文件的基本操作。
 * 所有具體的資料夾操作控制器都會繼承該類來實現具體業務邏輯。
 *
 * @author yuan
 * @program FileManagement
 * @ClassName BaseFolderController
 * @description 資料夾管理控制器抽象基類，處理與資料夾相關的操作。
 * @create 2024-09-30 16:00
 * @Version 1.0
 **/
@RecordLevel(LogLevelEnum.INFO)
public abstract class BaseFolderController extends BaseFileController {

    /**
     * 資料夾列表樹管理器，用於管理資料夾的樹狀結構。
     * 用於提供用戶的資料夾結構視圖。
     */
    protected final FolderListTreeManager folderListTreeManager;

    /**
     * 資料夾業務層對象，負責處理資料夾的創建、刪除、修改等業務邏輯。
     */
    protected final FolderService folderService;

    /**
     * 依賴注入的構造方法，用於初始化資料夾控制器。
     *
     * @param userService           用戶服務，負責用戶相關操作。
     * @param permissionService     權限服務，處理用戶操作的權限校驗。
     * @param fileServiceStrategy   文件服務策略，根據不同的文件操作提供相應的文件服務。
     * @param fileProperties        文件屬性配置，用於加載系統層級的文件屬性配置。
     * @param validationService     驗證服務，對請求參數進行校驗。
     * @param folderService         資料夾業務層服務。
     * @param userLimiterStrategy   用戶限額策略，控制用戶的操作限制。
     * @param objectMapper          對象映射工具，用於將 Java 對象與 JSON 之間進行轉換。
     * @param folderListTreeManager 資料夾樹管理器，處理資料夾樹狀結構的初始化和管理。
     */
    public BaseFolderController(UserService userService, PermissionService<UserFileMetadata> permissionService, FileServiceStrategy fileServiceStrategy, FileProperties fileProperties, ValidationService validationService, FolderService folderService, UserLimiterStrategy userLimiterStrategy, ObjectMapper objectMapper, FilePermissionRuleManager filePermissionRuleManager,
                                @Nullable FolderListTreeManager folderListTreeManager) {
        super(userService,
              fileServiceStrategy,
              fileProperties,
              validationService,
              permissionService,
              userLimiterStrategy,
              objectMapper,
              filePermissionRuleManager
        );
        this.folderListTreeManager = folderListTreeManager;
        this.folderService = folderService;
    }


    /**
     * 刪除資料夾，該操作會刪除資料夾內所有檔案。
     *
     * @param id       資料夾 ID，用來識別要刪除的資料夾。
     * @param exchange 請求對象，包含請求上下文信息。
     *
     * @return 返回刪除結果，成功返回 OK，失敗返回 BAD_REQUEST。
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
                        .then(createResponseEntity(createResponse(exchange, "刪除資料夾成功", null))));
        return handleError(result, exchange);
    }


    /**
     * 編輯資料夾，修改資料夾名稱、位置以及分享狀態。
     *
     * @param fileEditDTO 編輯資料，包含修改資料夾所需的信息（如名稱、位置、分享狀態等）。
     * @param exchange    請求對象，包含請求上下文信息。
     *
     * @return 返回編輯結果，成功返回 OK，失敗返回 BAD_REQUEST。
     */
    public Mono<ResponseEntity<?>> editFolder(FileEditDTO fileEditDTO, ServerWebExchange exchange) {
        Mono<ResponseEntity<?>> responseEntityMono = validationService
                .validateEditFileDTO(fileEditDTO, true)
                .then(validationService.validSpecifyColumns(fileEditDTO, "fileId"))
                .then(userService.getUser(exchange))
                .flatMap(user -> {
                    Map<Long, UserFileMetadata> fileMetadataMap = new HashMap<>();
                    fileMetadataMap.put(Long.parseLong(fileEditDTO.getFileId()), null);
                    if (fileEditDTO.getParentFolderId() != null) {
                        fileMetadataMap.put(fileEditDTO.getParentFolderId(), null);
                    }
                    return permissionService
                            .validateUserPermission(user, fileMetadataMap.keySet())
                            .doOnNext(file -> fileMetadataMap.put(file.getId(), file))
                            .then(Mono.defer(() -> {
                                fileEditDTO.setUserFileMetadata(fileMetadataMap.get(Long.parseLong(fileEditDTO.getFileId())));
                                fileEditDTO.setParentFolderFileMetadata(fileMetadataMap.get(fileEditDTO.getParentFolderId()));
                                return validationService
                                        .validateFileType(fileEditDTO.getUserFileMetadata(), FileEnum.FOLDER)
                                        .then(validationService.validateFileType(fileEditDTO.getParentFolderFileMetadata(), FileEnum.FOLDER))
                                        .then(folderService.editFolder(fileEditDTO, user));
                            }));
                })
                .then(createResponseEntity(createResponse(exchange, "資料夾更新成功", null)));
        return handleError(responseEntityMono, exchange);
    }


    /**
     * 創建資料夾，根據用戶提供的資料夾資料創建新的資料夾。
     *
     * @param fileEditDTO 資料夾資料，包含資料夾名稱、位置等信息。
     * @param exchange    請求對象，包含請求上下文信息。
     *
     * @return 返回創建結果，成功返回 OK，失敗返回 BAD_REQUEST。
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
                    .then(createResponseEntity(createResponse(exchange, "資料夾建立成功", null)));
        }), exchange);
    }


    /**
     * 獲取資料夾的路徑，根據資料夾 ID 返回該資料夾的完整路徑信息。
     *
     * @param exchange 請求對象，包含請求上下文信息。
     * @param fileId   資料夾 ID，用來查找資料夾路徑。
     *
     * @return 返回資料夾路徑信息，成功返回 OK，失敗返回 BAD_REQUEST。
     */
    public Mono<ResponseEntity<?>> getFolderPath(ServerWebExchange exchange, Long fileId) {
        return handleError(userService.getUser(exchange).flatMap(user -> Mono.defer(() -> {
            HashMap<String, Object> result = new HashMap<>();
            return permissionService
                    .validateUserPermission(user, fileId, FilePermissionRuleManager.DefaultRule.WITH_SHARED.getRules(filePermissionRuleManager))
                    .flatMap(file -> validationService
                            .validateFileType(file, FileEnum.FOLDER)
                            .then(folderService.getUserFilePaths(file, user).flatMap(list -> {
                                result.put("filePaths", list);
                                return Mono.just(result);
                            })));
        }).flatMap(result -> createResponseEntity(createResponse(exchange, "獲取用戶檔案路徑成功", result)))), exchange);
    }


    /**
     * 建立用戶資料夾樹，根據系統配置和用戶資料夾結構建立資料夾樹。
     *
     * @param exchange 請求對象，包含請求上下文信息。
     *
     * @return 返回建立資料夾樹的結果，成功返回 OK，失敗返回 BAD_REQUEST。
     */
    public Mono<ResponseEntity<?>> buildTree(ServerWebExchange exchange) {
        return handleError(userService.getUser(exchange).flatMap(user -> {
            if (!fileProperties.getGlobal().getEnableUserFolderListTree()) {
                return createResponseEntity(createResponse(exchange, "當前設定不支持建立用戶檔案樹", null));
            }
            if (folderListTreeManager != null) {
                CompletableFuture.runAsync(() -> folderListTreeManager.initializeTree(user.getId()));
            }
            return (createResponseEntity(createResponse(exchange, "請求建立用戶檔案樹成功", null)));
        }), exchange);
    }


    /**
     * 移動資料夾到回收站。
     *
     * @param exchange 請求對象，包含請求上下文信息。
     * @param id       資料夾 ID，用來標識要回收的資料夾。
     *
     * @return 返回回收結果，成功返回 OK，失敗返回 BAD_REQUEST。
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
     * @return 返回還原結果，成功返回 OK，失敗返回 BAD_REQUEST。
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
     * @return 返回下載結果，成功返回 OK，失敗返回 BAD_REQUEST。
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
