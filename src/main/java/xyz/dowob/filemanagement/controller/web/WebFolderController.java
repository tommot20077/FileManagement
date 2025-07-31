package xyz.dowob.filemanagement.controller.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nullable;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.HideOverLength;
import xyz.dowob.filemanagement.annotation.RecordLevel;
import xyz.dowob.filemanagement.component.manager.FilePermissionRuleManager;
import xyz.dowob.filemanagement.component.manager.FolderListTreeManager;
import xyz.dowob.filemanagement.component.strategy.FileServiceStrategy;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.controller.base.BaseFolderController;
import xyz.dowob.filemanagement.customenum.LogLevelEnum;
import xyz.dowob.filemanagement.customenum.ReservedSearchIdEnum;
import xyz.dowob.filemanagement.data.file.dto.FileEditDTO;
import xyz.dowob.filemanagement.entity.UserFileMetadata;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.service.serviceInterface.FolderService;
import xyz.dowob.filemanagement.service.serviceInterface.PermissionService;
import xyz.dowob.filemanagement.service.serviceInterface.UserService;
import xyz.dowob.filemanagement.service.serviceInterface.ValidationService;

import java.util.List;

/**
 * 基於反應式 WebFlux 架構的資料夾管理 Web 控制器。
 * <p>
 * 實現完整的資料夾操作 RESTful API，包括檔案列表查詢、資料夾建立與編輯、
 * 路徑查詢、樹狀結構建立等核心功能。支援星標檔案、最近使用檔案、
 * 回收站檔案、分享檔案等特殊檔案集合的查詢操作。
 * <p>
 * 繼承自 {@link BaseFolderController}，採用非阻塞 I/O 模式處理所有 HTTP 請求，
 * 提供高併發環境下的優異性能表現。所有操作均受權限控制機制保護，確保資料安全性。
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see BaseFolderController
 */
@RestController
@RecordLevel(LogLevelEnum.INFO)
@RequestMapping("/web/v1/folders")
public class WebFolderController extends BaseFolderController {
    /**
     * 建構資料夾 Web 控制器實例。
     *
     * @param userService               用戶服務介面，處理用戶相關業務邏輯
     * @param permissionService         權限服務介面，執行用戶操作的權限驗證
     * @param fileServiceStrategy       檔案服務策略，提供不同檔案類型的服務實現
     * @param fileProperties            檔案屬性配置，系統層級檔案相關設定
     * @param validationService         驗證服務介面，執行請求參數的有效性檢查
     * @param folderService             資料夾服務介面，處理資料夾相關業務操作
     * @param objectMapper              JSON 序列化工具，處理 Java 物件與 JSON 的轉換
     * @param filePermissionRuleManager 檔案權限規則管理器，管理檔案存取權限規則
     * @param folderListTreeManager     資料夾樹管理器，管理資料夾樹狀結構的建立與維護，可為 null
     */
    public WebFolderController(UserService userService, PermissionService<UserFileMetadata> permissionService, FileServiceStrategy fileServiceStrategy, FileProperties fileProperties, ValidationService validationService, FolderService folderService, ObjectMapper objectMapper, FilePermissionRuleManager filePermissionRuleManager,
                               @Nullable FolderListTreeManager folderListTreeManager) {
        super(userService,
              permissionService,
              fileServiceStrategy,
              fileProperties,
              validationService,
              folderService, objectMapper, filePermissionRuleManager,
              folderListTreeManager
        );
    }


    /**
     * 取得指定資料夾內的檔案列表，支援分頁與類型過濾。
     *
     * @param id       資料夾識別碼，必須為非負數
     * @param page     分頁頁碼，預設值為 1，從 1 開始計算
     * @param size     每頁顯示檔案數量，未指定時使用系統預設值
     * @param type     檔案類型過濾條件，可指定多種類型進行篩選
     * @param exchange 反應式 Web 請求交換物件，包含請求上下文資訊
     *
     * @return 反應式單值，包含分頁檔案列表的 HTTP 響應實體
     */
    @GetMapping("/{id}")
    @HideOverLength
    public Mono<ResponseEntity<?>> getFolderFiles(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false) Integer size, @RequestParam(required = false) List<String> type, ServerWebExchange exchange) {
        return handleError(Mono.defer(() -> {
            if (id < 0) {
                return Mono.error(new ValidationException(ValidationException.ErrorCode.PATH_NOT_FOUND));
            }
            return super.getUserFileList(exchange, id, page, size, getFileEnums(type));
        }), exchange);
    }


    /**
     * 取得使用者標記為星標的檔案列表。
     *
     * @param exchange 反應式 Web 請求交換物件，包含使用者認證資訊
     * @param page     分頁頁碼，預設值為 1，從 1 開始計算
     * @param size     每頁顯示檔案數量，未指定時使用系統預設值
     * @param type     檔案類型過濾條件，可指定多種類型進行篩選
     *
     * @return 反應式單值，包含星標檔案列表的 HTTP 響應實體
     */
    @GetMapping("/star")
    public Mono<ResponseEntity<?>> getStarFiles(ServerWebExchange exchange,
                                                @RequestParam(required = false, defaultValue = "1") Integer page,
                                                @RequestParam(required = false) Integer size, @RequestParam(required = false) List<String> type) {
        return super.getUserFileList(exchange, ReservedSearchIdEnum.STAR_FILE_ID.getId(), page, size, getFileEnums(type));
    }


    /**
     * 取得使用者最近存取的檔案列表，按時間倒序排列。
     *
     * @param exchange 反應式 Web 請求交換物件，包含使用者認證資訊
     * @param type     檔案類型過濾條件，可指定多種類型進行篩選
     *
     * @return 反應式單值，包含最近使用檔案列表的 HTTP 響應實體
     */
    @GetMapping("/recently")
    public Mono<ResponseEntity<?>> getRecentlyFiles(ServerWebExchange exchange, @RequestParam(required = false) List<String> type) {
        return super.getUserFileList(exchange, ReservedSearchIdEnum.RECENT_FILE_ID.getId(), 1, null, getFileEnums(type));
    }


    /**
     * 取得回收站中的已刪除檔案列表。
     *
     * @param exchange 反應式 Web 請求交換物件，包含使用者認證資訊
     * @param page     分頁頁碼，預設值為 1，從 1 開始計算
     * @param size     每頁顯示檔案數量，未指定時使用系統預設值
     * @param type     檔案類型過濾條件，可指定多種類型進行篩選
     *
     * @return 反應式單值，包含回收站檔案列表的 HTTP 響應實體
     */
    @GetMapping("/recycle")
    public Mono<ResponseEntity<?>> getRecycleFiles(ServerWebExchange exchange,
                                                   @RequestParam(required = false, defaultValue = "1") Integer page,
                                                   @RequestParam(required = false) Integer size, @RequestParam(required = false) List<String> type) {
        return super.getUserFileList(exchange, ReservedSearchIdEnum.RECYCLE_FILE_ID.getId(), page, size, getFileEnums(type));
    }


    /**
     * 取得使用者擁有的所有檔案列表，不包含已刪除檔案。
     *
     * @param exchange 反應式 Web 請求交換物件，包含使用者認證資訊
     * @param page     分頁頁碼，預設值為 1，從 1 開始計算
     * @param size     每頁顯示檔案數量，未指定時使用系統預設值
     * @param type     檔案類型過濾條件，可指定多種類型進行篩選
     *
     * @return 反應式單值，包含所有檔案列表的 HTTP 響應實體
     */
    @GetMapping("/all")
    public Mono<ResponseEntity<?>> getAllFiles(ServerWebExchange exchange,
                                               @RequestParam(required = false, defaultValue = "1") Integer page,
                                               @RequestParam(required = false) Integer size, @RequestParam(required = false) List<String> type) {
        return super.getUserFileList(exchange, ReservedSearchIdEnum.ALL_FILE_ID.getId(), page, size, getFileEnums(type));
    }


    /**
     * 取得使用者已分享的檔案列表。
     *
     * @param exchange 反應式 Web 請求交換物件，包含使用者認證資訊
     * @param page     分頁頁碼，預設值為 1，從 1 開始計算
     * @param size     每頁顯示檔案數量，未指定時使用系統預設值
     * @param type     檔案類型過濾條件，可指定多種類型進行篩選
     *
     * @return 反應式單值，包含分享檔案列表的 HTTP 響應實體
     */
    @GetMapping("/shared")
    public Mono<ResponseEntity<?>> getSharedFiles(ServerWebExchange exchange,
                                                  @RequestParam(required = false, defaultValue = "1") Integer page,
                                                  @RequestParam(required = false) Integer size, @RequestParam(required = false) List<String> type) {
        return super.getUserFileList(exchange, ReservedSearchIdEnum.SHARE_FILE_ID.getId(), page, size, getFileEnums(type));
    }

    /**
     * 取得指定資料夾的完整路徑資訊，包含從根目錄到目標資料夾的路徑階層。
     *
     * @param id       資料夾識別碼
     * @param exchange 反應式 Web 請求交換物件，包含請求上下文資訊
     *
     * @return 反應式單值，包含資料夾路徑階層資訊的 HTTP 響應實體
     */
    @GetMapping("/path/{id}")
    public Mono<ResponseEntity<?>> getFolderPath(@PathVariable Long id, ServerWebExchange exchange) {
        return super.getFolderPath(exchange, id);
    }

    /**
     * 永久刪除指定資料夾及其所有子資料夾與檔案，此操作不可復原。
     *
     * @param id       資料夾識別碼字串
     * @param exchange 反應式 Web 請求交換物件，包含請求上下文資訊
     *
     * @return 反應式單值，包含刪除操作結果的 HTTP 響應實體
     */
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<?>> deleteFolder(@PathVariable String id, ServerWebExchange exchange) {
        return super.deleteFolder(id, exchange);
    }

    /**
     * 編輯資料夾的基本資訊，如名稱、描述等屬性。
     *
     * @param fileEditDTO 資料夾編輯資訊傳輸物件，包含待更新的資料夾屬性
     * @param exchange    反應式 Web 請求交換物件，包含請求上下文資訊
     *
     * @return 反應式單值，包含編輯操作結果的 HTTP 響應實體
     */
    @PutMapping
    public Mono<ResponseEntity<?>> editFolder(@RequestBody @Validated FileEditDTO fileEditDTO, ServerWebExchange exchange) {
        return super.editFolder(fileEditDTO, exchange);
    }

    /**
     * 建立新的資料夾，在指定的父資料夾下建立子資料夾。
     *
     * @param fileEditDTO 資料夾建立資訊傳輸物件，包含資料夾名稱、父資料夾等資訊
     * @param exchange    反應式 Web 請求交換物件，包含請求上下文資訊
     *
     * @return 反應式單值，包含建立操作結果的 HTTP 響應實體
     */
    @PostMapping
    public Mono<ResponseEntity<?>> createFolder(@RequestBody FileEditDTO fileEditDTO, ServerWebExchange exchange) {
        return super.createFolder(fileEditDTO, exchange);
    }

    /**
     * 建立使用者的完整資料夾樹狀結構，用於前端顯示導覽樹。
     *
     * @param exchange 反應式 Web 請求交換物件，包含使用者認證資訊
     *
     * @return 反應式單值，包含資料夾樹狀結構的 HTTP 響應實體
     */
    @PostMapping("/tree")
    public Mono<ResponseEntity<?>> buildTree(ServerWebExchange exchange) {
        return super.buildTree(exchange);
    }

    /**
     * 將資料夾移動到回收站，實現軟刪除功能。
     *
     * @param exchange 反應式 Web 請求交換物件，包含請求上下文資訊
     * @param id       資料夾識別碼字串
     *
     * @return 反應式單值，包含移動操作結果的 HTTP 響應實體
     */
    @PostMapping("/remove/{id}")
    public Mono<ResponseEntity<?>> removeFile(ServerWebExchange exchange, @PathVariable String id) {
        return super.removeFile(exchange, id);
    }

    /**
     * 從回收站還原資料夾到原始位置或指定位置。
     *
     * @param exchange 反應式 Web 請求交換物件，包含請求上下文資訊
     * @param id       資料夾識別碼字串
     *
     * @return 反應式單值，包含還原操作結果的 HTTP 響應實體
     */
    @PostMapping("/restore/{id}")
    public Mono<ResponseEntity<?>> restoreFile(ServerWebExchange exchange, @PathVariable String id) {
        return super.restoreFile(exchange, id);
    }

    /**
     * 下載資料夾，將資料夾及其所有內容打包為壓縮檔案並以串流方式傳輸。
     *
     * @param id       資料夾識別碼
     * @param exchange 反應式 Web 請求交換物件，包含請求上下文資訊
     *
     * @return 反應式單值，包含壓縮檔案資料流的 HTTP 響應實體
     */
    @GetMapping("/{id}/download")
    public Mono<ResponseEntity<Flux<DataBuffer>>> downloadFolder(@PathVariable Long id, ServerWebExchange exchange) {
        return super.downloadFolder(id, exchange);
    }
}
