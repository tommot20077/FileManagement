package xyz.dowob.filemanagement.controller.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.component.manager.FilePermissionRuleManager;
import xyz.dowob.filemanagement.component.provider.provider.FolderListTreeProvider;
import xyz.dowob.filemanagement.component.strategy.FileServiceStrategy;
import xyz.dowob.filemanagement.component.strategy.UserLimiterStrategy;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.customenum.ReservedSearchIdEnum;
import xyz.dowob.filemanagement.data.api.ApiResponseDTO;
import xyz.dowob.filemanagement.data.api.PagedResponseDTO;
import xyz.dowob.filemanagement.data.file.dto.FileFilterDTO;
import xyz.dowob.filemanagement.data.file.dto.UserFileListDTO;
import xyz.dowob.filemanagement.entity.UserFileMetadata;
import xyz.dowob.filemanagement.functionInterface.Permission;
import xyz.dowob.filemanagement.service.serviceInterface.FileService;
import xyz.dowob.filemanagement.service.serviceInterface.PermissionService;
import xyz.dowob.filemanagement.service.serviceInterface.UserService;
import xyz.dowob.filemanagement.service.serviceInterface.ValidationService;
import xyz.dowob.filemanagement.unity.ResponseUnity;

import java.util.*;

import static xyz.dowob.filemanagement.customenum.FileEnum.*;

/**
 * 文件控制器基類，用於處理文件相關的請求，為所有文件控制器的基類。
 * 主要負責處理文件管理相關操作，包含文件列表的獲取、文件搜索、刪除和還原等。
 * 所有具體的文件操作控制器會繼承該類來實現具體業務邏輯。
 *
 * @author yuan
 * @program FileManagement
 * @ClassName BaseFileController
 * @create 2025/3/6
 * @Version 1.0
 **/
@RequiredArgsConstructor
public abstract class BaseFileController implements ResponseUnity {
    /**
     * 用戶業務層對象，用於操作用戶資料。
     */
    protected final UserService userService;

    /**
     * 檔案策略，用於決定不同的文件處理策略。
     */
    protected final FileServiceStrategy fileServiceStrategy;

    /**
     * 檔案屬性，提供文件配置和屬性信息。
     */
    protected final FileProperties fileProperties;

    /**
     * 驗證服務，用於文件相關的數據驗證。
     */
    protected final ValidationService validationService;

    /**
     * 權限服務，負責處理文件的訪問控制和權限驗證。
     * 用戶和文件的操作權限校驗。
     */
    protected final PermissionService<UserFileMetadata> permissionService;

    /**
     * 用戶限額策略，用於控制用戶的操作限制。
     */
    protected final UserLimiterStrategy userLimiterStrategy;

    /**
     * 對象轉換工具，用於將 Java 對象與 JSON 之間進行轉換。
     */
    protected final ObjectMapper objectMapper;

    /**
     * 文件權限規則管理器，用於管理文件的權限規則。
     */
    protected final FilePermissionRuleManager filePermissionRuleManager;

    /**
     * 自定義文件類型，表示支持的文件類型枚舉，包含圖片、視頻、音樂、文檔等。
     */
    protected final FileEnum[] CUSTOM_FILE_TYPE = new FileEnum[]{IMAGE, VIDEO, MUSIC, DOCUMENT, ZIP, OTHER, ONLINE_DOCUMENT};


    /**
     * 獲取用戶文件列表，根據資料夾 ID 獲取該資料夾下的文件列表。
     * 可根據文件類型和分頁參數過濾結果。
     * 此方法會根據 {@link ReservedSearchIdEnum} 中的預留 ID 來實現不同的功能。
     *
     * @param exchange 請求對象，包含用戶請求的上下文。
     * @param folderId 資料夾 ID，指定要查詢的資料夾。
     * @param page     分頁頁碼，從 1 開始。
     * @param size     每頁條數，決定返回的最大文件數量。
     * @param types    文件類型，根據文件類型過濾文件列表。
     *
     * @return 返回用戶文件列表，包含文件基本信息及文件路徑。
     */
    public Mono<ResponseEntity<?>> getUserFileList(ServerWebExchange exchange, Long folderId, Integer page, Integer size, List<FileEnum> types) {
        return handleError(userService.getUser(exchange).flatMap(user -> {
            FileService fileService = fileServiceStrategy.getFileService();

            List<Permission<UserFileMetadata>> rules = new ArrayList<>(List.of(filePermissionRuleManager.getAllowShared()));
            if (!Objects.equals(folderId, ReservedSearchIdEnum.RECYCLE_FILE_ID.getId())) {
                rules.add(filePermissionRuleManager.getBlockDeleted());
            }

            return permissionService.validateUserPermission(user, folderId, rules).flatMap(folder -> {
                FileFilterDTO fileFilterDTO = FileFilterDTO.builder().folderId(folderId).types(types).page(page).pageSize(size).build();

                Mono<PagedResponseDTO<UserFileListDTO>> fileListMono = fileService.getUserFileList(user, fileFilterDTO);
                Mono<List<FolderListTreeProvider.FolderNode>> filePathsMono = fileService.getUserFilePaths(folder, user);
                return validationService.validateFileType(folder, FOLDER).then(Mono.zip(fileListMono, filePathsMono).flatMap(tuple -> {
                    HashMap<String, Object> result = new HashMap<>();
                    result.put("userId", user.getId());
                    result.put("username", user.getUsername());
                    result.put("files", tuple.getT1());
                    result.put("filePaths", tuple.getT2());
                    return createResponseEntity(createResponse(exchange, "獲取用戶文件列表成功", result));
                }));
            });
        }), exchange);
    }


    /**
     * 搜索文件，根據指定的關鍵字、資料夾 ID、文件類型以及時間範圍等條件進行文件搜索。
     * 這個方法支持對文件進行多條件過濾，包括名稱、類型、創建時間等。
     *
     * @param exchange  請求對象，包含用戶的上下文。
     * @param fileFilterDTO 文件過濾條件的數據對象。
     *
     * @return 返回符合條件的文件列表。
     */
    protected Mono<ResponseEntity<?>> searchFile(ServerWebExchange exchange, FileFilterDTO fileFilterDTO) {
        return handleError(userService.getUser(exchange).flatMap(user -> {
            FileService fileService = fileServiceStrategy.getFileService();
            return validationService.validateFileFilterDTO(fileFilterDTO).then(fileService.searchUserFile(user, fileFilterDTO).flatMap(files -> {
                HashMap<String, Object> result = new HashMap<>();
                result.put("userId", user.getId());
                result.put("username", user.getUsername());
                result.put("files", files);
                result.put("filePaths", Collections.singletonList(new FolderListTreeProvider.FolderNode(null, "root")));
                return createResponseEntity(createResponse(exchange, "搜索文件成功", result));
            }));
        }), exchange);
    }

    /**
     * 移除檔案到回收站，將指定檔案移動至回收站。
     *
     * @param exchange 請求對象。
     * @param id       檔案 ID，指定需要刪除的檔案。
     * @param type     檔案類型，指定檔案的類型進行刪除。
     *
     * @return 返回刪除操作的結果。
     */
    protected Mono<ResponseEntity<?>> removeFile(ServerWebExchange exchange, String id, FileEnum type) {
        FileEnum[] fileType = type == null ? CUSTOM_FILE_TYPE : new FileEnum[]{type};
        return handleError(Mono.defer(() -> userService
                .getUser(exchange)
                .flatMap(user -> permissionService
                        .validateUserPermission(user, Long.parseLong(id))
                        .flatMap(file -> validationService
                                .validateFileType(file, fileType)
                                .then(fileServiceStrategy.getFileService(type).removeFile(file, user))))
                .flatMap(result -> {
                    String message = result ? "回收檔案成功" : "回收檔案失敗";
                    int status = result ? HttpStatus.OK.value() : HttpStatus.BAD_REQUEST.value();
                    ApiResponseDTO<?> apiResponse = createResponse(exchange, status, message, null);
                    return createResponseEntity(apiResponse);
                })), exchange);
    }

    /**
     * 還原檔案，將指定檔案從回收站中還原。
     *
     * @param exchange 請求對象。
     * @param id       檔案 ID，指定需要還原的檔案。
     * @param type     檔案類型，指定檔案的類型進行還原。
     *
     * @return 返回還原操作的結果。
     */
    protected Mono<ResponseEntity<?>> restoreFile(ServerWebExchange exchange, String id, FileEnum type) {
        FileEnum[] fileType = type == null ? CUSTOM_FILE_TYPE : new FileEnum[]{type};
        List<Permission<UserFileMetadata>> rules = List.of(filePermissionRuleManager.getAllowOwner(),
                                                           filePermissionRuleManager.getBlockNotSearchOperation()
        );
        return handleError(Mono.defer(() -> userService
                .getUser(exchange)
                .flatMap(user -> permissionService
                        .validateUserPermission(user, Long.parseLong(id), rules)
                        .flatMap(file -> validationService
                                .validateFileType(file, fileType)
                                .then(fileServiceStrategy.getFileService(type).restoreFile(file, user))))
                .then(Mono.defer(() -> {
                    ApiResponseDTO<?> apiResponse = createResponse(exchange, "還原檔案成功", null);
                    return createResponseEntity(apiResponse);
                }))), exchange);
    }

    /**
     * 獲取文件類型列表，將傳入的字符串類型轉換為對應的枚舉類型。
     * 如果字符串無法匹配對應的枚舉，則忽略該值。
     *
     * @param type 文件類型列表。
     *
     * @return 返回文件類型的枚舉列表。
     */
    protected List<FileEnum> getFileEnums(List<String> type) {
        return Optional.ofNullable(type).orElse(Collections.emptyList()).stream().map(t -> {
            try {
                return FileEnum.valueOf(t.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }).filter(Objects::nonNull).toList();
    }
}
