package xyz.dowob.filemanagement.controller.base;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.RecordLevel;
import xyz.dowob.filemanagement.component.manager.FilePermissionRuleManager;
import xyz.dowob.filemanagement.component.strategy.FileServiceStrategy;
import xyz.dowob.filemanagement.component.strategy.UserLimiterStrategy;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.customenum.DownloadActionEnum;
import xyz.dowob.filemanagement.customenum.EditTypeEnum;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.customenum.LogLevelEnum;
import xyz.dowob.filemanagement.data.api.ApiResponseDTO;
import xyz.dowob.filemanagement.data.file.dto.FileEditDTO;
import xyz.dowob.filemanagement.data.file.dto.FileMetadataDTO;
import xyz.dowob.filemanagement.entity.UserFileMetadata;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.functionInterface.Permission;
import xyz.dowob.filemanagement.service.serviceInterface.FileService;
import xyz.dowob.filemanagement.service.serviceInterface.PermissionService;
import xyz.dowob.filemanagement.service.serviceInterface.UserService;
import xyz.dowob.filemanagement.service.serviceInterface.ValidationService;

import java.util.*;

/**
 * 在線文件控制器抽象類，用於處理在線文件的相關請求
 * 此類繼承自 BaseFileController，用於處理文件相關的請求，包括上傳、下載、刪除、編輯、獲取歷史紀錄等操作。
 * 它依賴於多個服務層對象，處理文件的授權、驗證及操作，並提供異步的 API 回應。
 *
 * @author yuan
 * @program FileManagement
 * @ClassName ApiOnlineFileController
 * @create 2025/2/11
 * @Version 1.0
 */
@RecordLevel(LogLevelEnum.INFO)
public class BaseOnlineFileController extends BaseFileController {

    /**
     * 构造函數，用於初始化基本的業務層服務
     *
     * @param userService               用戶服務層對象
     * @param fileServiceStrategy       文件服務策略對象，用於選擇適當的文件服務
     * @param fileProperties            文件屬性設置
     * @param validationService         驗證服務對象
     * @param permissionService         用戶文件元數據授權服務
     * @param objectMapper              用於處理對象映射的工具
     * @param filePermissionRuleManager 文件權限規則管理器
     */
    public BaseOnlineFileController(UserService userService, FileServiceStrategy fileServiceStrategy, FileProperties fileProperties, ValidationService validationService, PermissionService<UserFileMetadata> permissionService, UserLimiterStrategy userLimiterStrategy, ObjectMapper objectMapper, FilePermissionRuleManager filePermissionRuleManager) {
        super(userService, fileServiceStrategy, fileProperties, validationService, permissionService, objectMapper, filePermissionRuleManager);
    }


    /**
     * 上傳文件的 API 請求
     * 根據文件元數據傳輸對象來上傳文件，並確保用戶有適當的權限。
     *
     * @param fileMetadataDTO 文件元數據傳輸對象，包含有關文件的信息
     * @param exchange        服務器 Web 交換對象
     *
     * @return Mono<ResponseEntity < ?>> 返回異步處理的結果
     */
    public Mono<ResponseEntity<?>> uploadFile(FileMetadataDTO fileMetadataDTO, ServerWebExchange exchange) {
        return handleError(userService.getUser(exchange).flatMap(user -> {
            Mono<UserFileMetadata> parentFolderMono = Mono.empty();
            if (fileMetadataDTO.getParentFolderId() != null) {
                parentFolderMono = permissionService
                        .validateUserPermission(user, fileMetadataDTO.getParentFolderId())
                        .flatMap(file -> validationService.validateFileType(file, FileEnum.FOLDER));
            }

            Mono<ResponseEntity<?>> responseEntityMono = fileServiceStrategy
                    .getFileService(FileEnum.ONLINE_DOCUMENT)
                    .uploadFile(fileMetadataDTO, user)
                    .flatMap(uploadResponseDTO -> createResponseEntity(createResponse(exchange, "上傳成功", uploadResponseDTO)));

            return parentFolderMono.then(responseEntityMono);
        }), exchange);
    }


    /**
     * 下載文件的 API 請求
     * 根據文件 ID 和用戶授權，下載指定的在線文件。
     *
     * @param id       文件 ID
     * @param exchange 服務器 Web 交換對象
     *
     * @return Mono<ResponseEntity < ?>> 返回異步處理的結果，包含文件內容和文件名
     */
    public Mono<ResponseEntity<Flux<DataBuffer>>> downloadFile(String action, String id, ServerWebExchange exchange) {
        FileService fileService = fileServiceStrategy.getFileService(FileEnum.ONLINE_DOCUMENT);
        DownloadActionEnum actionEnum = DownloadActionEnum.getType(action);
        Collection<Permission<UserFileMetadata>> rules = FilePermissionRuleManager.DefaultRule.WITH_SHARED.getRules(filePermissionRuleManager);
        return userService.getUser(exchange).flatMap(user -> {
            return permissionService.validateUserPermission(user, Long.parseLong(id), rules).flatMap(file -> {
                return validationService
                        .validateFileType(file, FileEnum.ONLINE_DOCUMENT)
                        .then(fileService.downloadFile(file, user, actionEnum.name()).flatMap(userFileDataBO -> {
                            try {
                                if (actionEnum == DownloadActionEnum.PREVIEW) {
                                    Map<String, Object> data = new HashMap<>();
                                    data.put("content", userFileDataBO.getContent());
                                    data.put("filename", userFileDataBO.getFilename());
                                    ApiResponseDTO<?> apiResponse = createResponse(exchange, "下載成功", data);
                                    byte[] responseBytes = objectMapper.writeValueAsString(apiResponse).getBytes();
                                    DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(responseBytes);
                                    return Mono.just(ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body(Flux.just(buffer)));
                                }

                                HttpHeaders headers = prepareHttpHeaders(DownloadActionEnum.DOWNLOAD, userFileDataBO, null, false);
                                return Mono.just(ResponseEntity.status(200).headers(headers).body(userFileDataBO.getDataBufferFlux()));
                            } catch (JsonProcessingException ex) {
                                return Mono.error(new RuntimeException(ex));
                            }
                        }));
            });
        }).onErrorResume(ValidationException.class, e -> handleDownloadValidationError(e, exchange));
    }


    /**
     * 刪除文件的 API 請求
     * 根據文件 ID 和用戶授權，刪除指定的在線文件。
     *
     * @param id       文件 ID
     * @param exchange 服務器 Web 交換對象
     *
     * @return Mono<ResponseEntity < ?>> 返回異步處理的結果，表示文件刪除成功
     */
    public Mono<ResponseEntity<?>> deleteFile(String id, ServerWebExchange exchange) {
        return handleError(userService.getUser(exchange).flatMap(user -> {
            List<Permission<UserFileMetadata>> rules = new ArrayList<>(List.of(filePermissionRuleManager.getAllowOwner(),
                                                                               filePermissionRuleManager.getBlockNotSearchOperation()
            ));
            return permissionService
                    .validateUserPermission(user, Long.parseLong(id), rules)
                    .flatMap(file -> validationService
                            .validateFileType(file, FileEnum.ONLINE_DOCUMENT)
                            .then(fileServiceStrategy.getFileService(FileEnum.ONLINE_DOCUMENT).deleteFile(file, user))
                            .then(createResponseEntity(createResponse(exchange, "刪除成功", null))));
        }), exchange);
    }


    /**
     * 編輯文件的 API 請求
     * 根據編輯數據傳輸對象，對指定的在線文件進行編輯，並進行必要的授權檢查。
     *
     * @param fileEditDTO 文件編輯數據傳輸對象，包含要編輯的文件和所需修改的屬性
     * @param exchange    服務器 Web 交換對象
     *
     * @return Mono<ResponseEntity < ?>> 返回異步處理的結果，表示文件編輯成功
     */
    public Mono<ResponseEntity<?>> editFile(FileEditDTO fileEditDTO, ServerWebExchange exchange) {
        return handleError(validationService.validateEditFileDTO(fileEditDTO, false).then(userService.getUser(exchange)).flatMap(user -> {
            List<Long> fileIds = new ArrayList<>();
            List<Permission<UserFileMetadata>> rules = new ArrayList<>();
            fileIds.add(Long.parseLong(fileEditDTO.getFileId()));
            if (fileEditDTO.getParentFolderId() != null && fileEditDTO.getEditType() == EditTypeEnum.EDIT_METADATA) {
                fileIds.add(fileEditDTO.getParentFolderId());
                rules.add(filePermissionRuleManager.getAllowOwner());
            }
            if (fileEditDTO.getEditType() != EditTypeEnum.EDIT_METADATA) {
                rules.add(filePermissionRuleManager.getAllowShared());
            }

            return permissionService.validateUserPermission(user, fileIds, rules).collectList().flatMap(files -> {
                files.forEach(file -> {
                    if (file.getId().equals(fileEditDTO.getParentFolderId())) {
                        fileEditDTO.setParentFolderFileMetadata(file);
                    } else if (file.getId().equals(Long.parseLong(fileEditDTO.getFileId()))) {
                        fileEditDTO.setUserFileMetadata(file);
                    }
                });
                return validationService
                        .validateFileType(fileEditDTO.getUserFileMetadata(), FileEnum.ONLINE_DOCUMENT)
                        .then(validationService.validateFileType(fileEditDTO.getParentFolderFileMetadata(), FileEnum.FOLDER))
                        .then(fileServiceStrategy.getFileService(FileEnum.ONLINE_DOCUMENT).editFile(fileEditDTO, user));
            });
        }).then(createResponseEntity(createResponse(exchange, "編輯成功", null))), exchange);
    }


    /**
     * 獲取文件歷史資料的 API 請求
     * 根據文件 ID 和授權，返回文件的歷史版本記錄。
     *
     * @param exchange 服務器 Web 交換對象
     * @param id       文件 ID
     * @param page     分頁參數，指定當前頁
     * @param pageSize 分頁參數，指定每頁大小
     *
     * @return Mono<ResponseEntity < ?>> 返回異步處理的結果，包含文件的歷史版本記錄
     */
    public Mono<ResponseEntity<?>> getHistory(ServerWebExchange exchange, String id, Integer page, Integer pageSize) {
        Mono<ResponseEntity<?>> responseEntityMono = userService.getUser(exchange).flatMap(user -> {
            return permissionService
                    .validateUserPermission(user,
                                            Long.parseLong(id),
                                            FilePermissionRuleManager.DefaultRule.WITH_SHARED.getRules(filePermissionRuleManager)
                    )
                    .flatMap(file -> validationService
                            .validateFileType(file, FileEnum.ONLINE_DOCUMENT)
                            .then(fileServiceStrategy.getFileService(FileEnum.ONLINE_DOCUMENT).getFileVersionList(user, file, page, pageSize))
                            .flatMap(history -> createResponseEntity(createResponse(exchange, "獲取歷程記錄成功", history))));
        });
        return handleError(responseEntityMono, exchange);
    }


    /**
     * 將檔案移動到回收站的 API 請求
     * 根據檔案 ID 和授權，將檔案移動到回收站。
     *
     * @param exchange 請求對象
     * @param id       檔案 ID
     *
     * @return Mono<ResponseEntity < ?>> 返回檔案移動到回收站的結果
     */
    public Mono<ResponseEntity<?>> removeFile(ServerWebExchange exchange, String id) {
        return super.removeFile(exchange, id, null);
    }


    /**
     * 還原檔案的 API 請求
     * 根據檔案 ID 和授權，將檔案還原。
     *
     * @param exchange 請求對象
     * @param id       檔案 ID
     *
     * @return Mono<ResponseEntity < ?>> 返回還原檔案的結果
     */
    public Mono<ResponseEntity<?>> restoreFile(ServerWebExchange exchange, String id) {
        return super.restoreFile(exchange, id, null);
    }
}
