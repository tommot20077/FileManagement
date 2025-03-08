package xyz.dowob.filemanagement.controller.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.HideOverLength;
import xyz.dowob.filemanagement.component.manager.FolderListTreeManager;
import xyz.dowob.filemanagement.component.strategy.FileServiceStrategy;
import xyz.dowob.filemanagement.component.strategy.UserLimiterStrategy;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.controller.base.BaseFolderController;
import xyz.dowob.filemanagement.customenum.ReservedSearchIdEnum;
import xyz.dowob.filemanagement.data.file.dto.FileEditDTO;
import xyz.dowob.filemanagement.entity.UserFileMetadata;
import xyz.dowob.filemanagement.service.serviceInterface.FolderService;
import xyz.dowob.filemanagement.service.serviceInterface.PermissionService;
import xyz.dowob.filemanagement.service.serviceInterface.UserService;
import xyz.dowob.filemanagement.service.serviceInterface.ValidationService;

import java.util.List;

/**
 * 資料夾 Web 控制器
 * <p>
 * 提供資料夾相關的操作，包括：
 * - 查詢資料夾內的檔案列表
 * - 查詢星標檔案、最近使用檔案和所有檔案
 * - 新增、刪除、編輯、還原和移動資料夾
 * - 獲取資料夾路徑和建立檔案樹
 * <p>
 * 此類繼承自 {@link BaseFolderController}，並透過 RESTful API 提供對外的資料夾管理功能。
 */
@RestController
@RequestMapping("/web/v1/folders")
public class WebFolderController extends BaseFolderController {
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
    public WebFolderController(UserService userService, PermissionService<UserFileMetadata> permissionService, FileServiceStrategy fileServiceStrategy, FileProperties fileProperties, ValidationService validationService, FolderService folderService, UserLimiterStrategy userLimiterStrategy, ObjectMapper objectMapper,
                               @Nullable FolderListTreeManager folderListTreeManager) {
        super(userService,
              permissionService,
              fileServiceStrategy,
              fileProperties,
              validationService,
              folderService,
              userLimiterStrategy,
              objectMapper,
              folderListTreeManager
        );
    }

    /**
     * 獲取資料夾內的檔案列表
     *
     * @param id       資料夾 ID
     * @param page     分頁頁碼，預設為 1
     * @param size     每頁大小，可選
     * @param type     過濾的檔案類型，可選
     * @param exchange WebFlux 請求上下文
     *
     * @return 檔案列表
     */
    @GetMapping("/{id}")
    @HideOverLength
    public Mono<ResponseEntity<?>> getFolderFiles(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false) Integer size, @RequestParam(required = false) List<String> type, ServerWebExchange exchange) {
        return super.getUserFileList(exchange, id, page, size, getFileEnums(type));
    }

    /**
     * 獲取星標檔案列表
     *
     * @param exchange WebFlux 請求上下文
     * @param page     分頁頁碼，預設為 1
     * @param size     每頁大小，可選
     * @param type     過濾的檔案類型，可選
     *
     * @return 星標檔案列表
     */
    @GetMapping("/star")
    public Mono<ResponseEntity<?>> getStarFiles(ServerWebExchange exchange,
                                                @RequestParam(required = false, defaultValue = "1") Integer page,
                                                @RequestParam(required = false) Integer size, @RequestParam(required = false) List<String> type) {
        return super.getUserFileList(exchange, ReservedSearchIdEnum.STAR_FILE_ID.getId(), page, size, getFileEnums(type));
    }

    /**
     * 獲取最近使用的檔案列表
     *
     * @param exchange WebFlux 請求上下文
     * @param type     過濾的檔案類型，可選
     *
     * @return 最近檔案列表
     */
    @GetMapping("/recently")
    public Mono<ResponseEntity<?>> getRecentlyFiles(ServerWebExchange exchange, @RequestParam(required = false) List<String> type) {
        return super.getUserFileList(exchange, ReservedSearchIdEnum.RECENT_FILE_ID.getId(), 1, null, getFileEnums(type));
    }

    /**
     * 創建資料夾
     *
     * @param fileEditDTO 資料夾資訊
     * @param exchange    WebFlux 請求上下文
     *
     * @return 創建結果
     */
    @PostMapping
    public Mono<ResponseEntity<?>> createFolder(@RequestBody FileEditDTO fileEditDTO, ServerWebExchange exchange) {
        return super.createFolder(fileEditDTO, exchange);
    }

    /**
     * 獲取資料夾的路徑，根據資料夾 ID 返回該資料夾的完整路徑信息。
     *
     * @param exchange 請求對象，包含請求上下文信息。
     * @param id       資料夾 ID，用來查找資料夾路徑。
     *
     * @return 返回資料夾路徑信息，成功返回 OK，失敗返回 BAD_REQUEST。
     */
    @GetMapping("/path/{id}")
    public Mono<ResponseEntity<?>> getFolderPath(@PathVariable Long id, ServerWebExchange exchange) {
        return super.getFolderPath(exchange, id);
    }

    /**
     * 刪除資料夾及其內容
     *
     * @param id       資料夾 ID
     * @param exchange WebFlux 請求上下文
     *
     * @return 刪除結果
     */
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<?>> deleteFolder(@PathVariable String id, ServerWebExchange exchange) {
        return super.deleteFolder(id, exchange);
    }

    /**
     * 編輯資料夾
     *
     * @param fileEditDTO 資料夾資訊
     * @param exchange    WebFlux 請求上下文
     *
     * @return 編輯結果
     */
    @PutMapping
    public Mono<ResponseEntity<?>> editFolder(@RequestBody @Validated FileEditDTO fileEditDTO, ServerWebExchange exchange) {
        return super.editFolder(fileEditDTO, exchange);
    }

    /**
     * 還原已刪除的資料夾
     *
     * @param id       資料夾 ID
     * @param exchange WebFlux 請求上下文
     *
     * @return 還原結果
     */
    @PostMapping("/restore/{id}")
    public Mono<ResponseEntity<?>> restoreFile(ServerWebExchange exchange, @PathVariable String id) {
        return super.restoreFile(exchange, id);
    }

    /**
     * 移動資料夾到回收站
     *
     * @param exchange 請求對象
     * @param id       資料夾ID
     *
     * @return 刪除結果
     */
    @PostMapping("/remove/{id}")
    public Mono<ResponseEntity<?>> removeFile(ServerWebExchange exchange, @PathVariable String id) {
        return super.removeFile(exchange, id);
    }

    /**
     * 建立用戶資料夾樹，根據系統配置和用戶資料夾結構建立資料夾樹。
     *
     * @param exchange 請求對象，包含請求上下文信息。
     *
     * @return 返回建立資料夾樹的結果，成功返回 OK，失敗返回 BAD_REQUEST。
     */
    @PostMapping("/tree")
    public Mono<ResponseEntity<?>> buildTree(ServerWebExchange exchange) {
        return super.buildTree(exchange);
    }
}
