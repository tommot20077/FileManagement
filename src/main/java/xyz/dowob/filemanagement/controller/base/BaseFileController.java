package xyz.dowob.filemanagement.controller.base;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.component.provider.provider.FolderListTreeProvider;
import xyz.dowob.filemanagement.component.strategy.FileServiceStrategy;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.customenum.FilePermissionRule;
import xyz.dowob.filemanagement.customenum.ReservedSearchIdEnum;
import xyz.dowob.filemanagement.data.api.ApiResponseDTO;
import xyz.dowob.filemanagement.data.api.PagedResponseDTO;
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
 * 檔案控制器的基礎類
 *
 * @author yuan
 * @program FileManagement
 * @ClassName BaseFileController
 * @create 2025/1/14
 * @Version 1.0
 **/
@RequiredArgsConstructor
public abstract class BaseFileController implements ResponseUnity {
    /**
     * 用戶業務層對象
     */
    protected final UserService userService;

    /**
     * 檔案策略
     */
    protected final FileServiceStrategy fileServiceStrategy;

    /**
     * 檔案屬性
     */
    protected final FileProperties fileProperties;

    /**
     * 驗證服務
     */
    protected final ValidationService validationService;

    /**
     * 權限服務
     */
    protected final PermissionService<UserFileMetadata> permissionService;

    /**
     * 自定義文件類型
     */
    protected final FileEnum[] CUSTOM_FILE_TYPE = new FileEnum[]{IMAGE, VIDEO, MUSIC, DOCUMENT, ZIP, OTHER, ONLINE_DOCUMENT};

    /**
     * 獲取用戶文件列表，此 ID 為資料夾 ID
     * 根據預留Id實現不同的功能
     * {@link ReservedSearchIdEnum}
     *
     * @param exchange 請求對象
     *
     * @return 返回用戶文件列表
     */
    public Mono<ResponseEntity<?>> getUserFileList(ServerWebExchange exchange, Long folderId, Integer page, Integer size, List<FileEnum> types) {
        return handleError(userService.getUser(exchange).flatMap(user -> {
            FileService fileService = fileServiceStrategy.getFileService();

            List<Permission<UserFileMetadata>> rules = new ArrayList<>(List.of(FilePermissionRule.ALLOW_SHARED));
            if (!Objects.equals(folderId, ReservedSearchIdEnum.RECYCLE_FILE_ID.getId())) {
                rules.add(FilePermissionRule.BLOCK_DELETED);
            }

            return permissionService.validateUserPermission(user, folderId, rules).flatMap(folder -> {
                Mono<PagedResponseDTO<UserFileListDTO>> fileListMono = fileService.getUserFileList(user, folderId, page, size, types);
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
     * 移除檔案到回收站
     *
     * @param exchange 請求對象
     * @param id       檔案ID
     * @param type     檔案類型
     *
     * @return 返回刪除結果
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
     * 還原檔案
     *
     * @param exchange 請求對象
     * @param id       檔案ID
     * @param type     檔案類型
     *
     * @return 返回還原結果
     */
    protected Mono<ResponseEntity<?>> restoreFile(ServerWebExchange exchange, String id, FileEnum type) {
        FileEnum[] fileType = type == null ? CUSTOM_FILE_TYPE : new FileEnum[]{type};
        List<Permission<UserFileMetadata>> rules = List.of(FilePermissionRule.ALLOW_OWNER, FilePermissionRule.BLOCK_NOT_SEARCH_OPERATION);
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
     * 獲取文件類型
     *
     * @param type 文件類型
     *
     * @return 返回文件類型列表
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
