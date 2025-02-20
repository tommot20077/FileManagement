package xyz.dowob.filemanagement.controller.api;

import jakarta.annotation.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.HideOverLength;
import xyz.dowob.filemanagement.component.manager.FolderListTreeManager;
import xyz.dowob.filemanagement.component.strategy.FileServiceStrategy;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.controller.base.BaseFileController;
import xyz.dowob.filemanagement.data.file.dto.FileEditDTO;
import xyz.dowob.filemanagement.service.serviceInterface.FolderService;
import xyz.dowob.filemanagement.service.serviceInterface.UserService;
import xyz.dowob.filemanagement.service.serviceInterface.ValidationService;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 資料夾的 API 控制器，用於處理資料夾的 API 請求
 * 用於處理資料夾的增刪改查操作
 * 繼承自 BaseFileController，該類為基礎的文件控制器，用於處理文件的基本操作
 *
 * @author yuan
 * @program FileManagement
 * @ClassName ApiFileUploadController
 * @description
 * @create 2024-09-30 16:00
 * @Version 1.0
 **/
@RestController
@RequestMapping("/api/folders")
public class ApiFolderController extends BaseFileController {
    private final FolderListTreeManager folderListTreeManager;
    private final ValidationService validationService;
    private final FolderService folderService;

    public ApiFolderController(UserService userService, FileServiceStrategy fileServiceStrategy, FileProperties fileProperties, @Nullable
    FolderListTreeManager folderListTreeManager, ValidationService validationService, FolderService folderService) {
        super(userService, fileServiceStrategy, fileProperties);
        this.folderListTreeManager = folderListTreeManager;
        this.validationService = validationService;
        this.folderService = folderService;
    }


    /**
     * 獲取資料夾內的檔案列表，此 ID 為資料夾 ID
     *
     * @param id       資料夾ID
     * @param exchange 請求對象
     *
     * @return 檔案列表
     */
    @GetMapping({"/{id}"})
    @HideOverLength
    public Mono<ResponseEntity<?>> getFolderFiles(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "1") Integer page, ServerWebExchange exchange,
            @RequestParam(required = false) Integer size, @RequestParam(required = false) List<String> type) {
        return super.getUserFileList(exchange, id, page, size, getFileEnums(type));
    }

    /**
     * 獲取星標檔案列表
     *
     * @param exchange 請求對象
     * @param page     分頁頁碼
     * @param size     分頁大小
     * @param type     檔案類型
     *
     * @return 星標檔案列表
     */
    @GetMapping("/star")
    public Mono<ResponseEntity<?>> getStarFiles(ServerWebExchange exchange,
                                                @RequestParam(required = false, defaultValue = "1") Integer page,
                                                @RequestParam(required = false) Integer size,
                                                @RequestParam(required = false) List<String> type) {
        return super.getUserFileList(exchange, -2L, page, size, getFileEnums(type));
    }

    /**
     * 獲取最近使用檔案列表
     *
     * @param exchange 請求對象
     * @param type     檔案類型
     *
     * @return 最近檔案列表
     */
    @GetMapping("recently")
    public Mono<ResponseEntity<?>> getRecentlyFiles(ServerWebExchange exchange, @RequestParam(required = false) List<String> type) {
        return super.getUserFileList(exchange, -3L, 1, null, getFileEnums(type));
    }

    /**
     * 獲取所有檔案列表
     *
     * @param exchange 請求對象
     * @param page     分頁頁碼
     * @param size     分頁大小
     * @param type     檔案類型
     *
     * @return 所有檔案列表
     */
    @GetMapping("/all")
    public Mono<ResponseEntity<?>> getAllFiles(ServerWebExchange exchange,
                                               @RequestParam(required = false, defaultValue = "1") Integer page,
                                               @RequestParam(required = false) Integer size,
                                               @RequestParam(required = false) List<String> type) {
        return super.getUserFileList(exchange, -1L, page, size, getFileEnums(type));
    }

    /**
     * 刪除資料夾，此操作會刪除資料夾下的所有檔案
     *
     * @param id       資料夾ID
     * @param exchange 請求對象
     *
     * @return 刪除結果
     */
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<?>> deleteFolder(@PathVariable String id, ServerWebExchange exchange) {
        return handleError(userService.getUser(exchange).flatMap(user -> folderService.deleteFolder(id, user))
                                   .then(createResponseEntity(createResponse(exchange, "刪除資料夾成功", null))), exchange);
    }

    /**
     * 編輯資料夾，此操作用於修改資料夾名稱、位置以及分享狀態
     *
     * @param fileEditDTO 編輯資料
     * @param exchange    請求對象
     *
     * @return 編輯結果
     */
    @PutMapping()
    public Mono<ResponseEntity<?>> editFolder(@Validated @RequestBody FileEditDTO fileEditDTO, ServerWebExchange exchange) {
        return handleError(validationService
                                   .validateEditFileDTO(fileEditDTO, true)
                                   .then(validationService.validSpecifyColumns(fileEditDTO, "fileId"))
                                   .then(userService.getUser(exchange))
                                   .flatMap(user -> folderService.editFolder(fileEditDTO, user))
                                   .then(createResponseEntity(createResponse(exchange, "資料夾更新成功", null))), exchange);
    }

    /**
     * 創建資料夾
     *
     * @param fileEditDTO 資料夾資料
     * @param exchange    請求對象
     *
     * @return 創建結果
     */
    @PostMapping
    public Mono<ResponseEntity<?>> createFolder(@RequestBody FileEditDTO fileEditDTO, ServerWebExchange exchange) {
        return handleError(validationService
                                   .validateEditFileDTO(fileEditDTO, true)
                                   .then(userService.getUser(exchange))
                                   .flatMap(user -> folderService.createFolder(fileEditDTO, user))
                                   .then(createResponseEntity(createResponse(exchange, "資料夾建立成功", null))), exchange);
    }

    @GetMapping("/path/{fileId}")
    public Mono<ResponseEntity<?>> getFolderPath(ServerWebExchange exchange, @PathVariable Long fileId) {
        return handleError(userService.getUser(exchange).flatMap(user -> Mono.defer(() -> {
            HashMap<String, Object> result = new HashMap<>();
            return folderService.getUserFilePaths(fileId, user).flatMap(list -> {
                result.put("filePaths", list);
                return Mono.just(result);
            });
        }).flatMap(result -> createResponseEntity(createResponse(exchange, "獲取用戶檔案路徑成功", result)))), exchange);
    }


    @PostMapping("/fileTree")
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

}
