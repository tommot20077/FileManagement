package xyz.dowob.filemanagement.controller.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.HideOverLength;
import xyz.dowob.filemanagement.annotation.RecordLevel;
import xyz.dowob.filemanagement.component.manager.FilePermissionRuleManager;
import xyz.dowob.filemanagement.component.strategy.FileServiceStrategy;
import xyz.dowob.filemanagement.component.strategy.UserLimiterStrategy;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.controller.base.BaseGeneralFileController;
import xyz.dowob.filemanagement.customenum.LogLevelEnum;
import xyz.dowob.filemanagement.data.file.dto.FileEditDTO;
import xyz.dowob.filemanagement.data.file.dto.FileFilterDTO;
import xyz.dowob.filemanagement.data.file.dto.FileMetadataDTO;
import xyz.dowob.filemanagement.entity.UserFileMetadata;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.service.serviceInterface.PermissionService;
import xyz.dowob.filemanagement.service.serviceInterface.UserService;
import xyz.dowob.filemanagement.service.serviceInterface.ValidationService;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 基於 WebFlux 的一般檔案 Web 控制器實現，提供檔案 RESTful API 端點。
 * <p>
 * 處理一般檔案的上傳、下載、編輯、刪除、搜尋等操作，支援分塊上傳和檔案預覽功能。
 * 繼承自 {@link BaseGeneralFileController}，採用反應式非阻塞模式確保高併發性能。
 * 集成權限驗證、用戶限流和檔案權限規則管理。
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@RestController
@RecordLevel(LogLevelEnum.INFO)
@RequestMapping("/web/v1/files")
public class WebGeneralFileController extends BaseGeneralFileController {

    /**
     * 構造一般檔案 Web 控制器實例，注入必要的服務依賴。
     *
     * @param userService               用戶服務，處理用戶相關操作
     * @param fileServiceStrategy       檔案服務策略，選擇適當的檔案處理服務
     * @param fileProperties            檔案配置屬性
     * @param validationService         驗證服務，執行業務規則驗證
     * @param permissionService         權限服務，管理檔案訪問權限
     * @param userLimiterStrategy       用戶限流策略，控制請求頻率
     * @param objectMapper              JSON 對象映射器
     * @param filePermissionRuleManager 檔案權限規則管理器
     */
    public WebGeneralFileController(UserService userService, FileServiceStrategy fileServiceStrategy, FileProperties fileProperties, ValidationService validationService, PermissionService<UserFileMetadata> permissionService, UserLimiterStrategy userLimiterStrategy, ObjectMapper objectMapper, FilePermissionRuleManager filePermissionRuleManager) {
        super(userService,
              fileServiceStrategy,
              fileProperties,
              validationService,
              permissionService,
              userLimiterStrategy,
              objectMapper,
              filePermissionRuleManager
        );
    }


    /**
     * 處理檔案上傳初始請求，驗證檔案元資料並決定後續上傳方式。
     * <p>
     * 驗證檔案基本資訊、檢查用戶權限和存儲限制，根據檔案特性返回分塊上傳或直接完成指示。
     *
     * @param fileMetadataDTO 檔案元資料，包含檔案名稱、大小、類型等資訊
     * @param exchange        伺服器 Web 交換對象，包含請求上下文
     * @return 包含上傳狀態和後續操作指示的響應
     */
    @PostMapping("/upload")
    public Mono<ResponseEntity<?>> uploadFile(@RequestBody FileMetadataDTO fileMetadataDTO, ServerWebExchange exchange) {
        return super.uploadFile(fileMetadataDTO, exchange);
    }


    /**
     * 根據檔案 ID 下載檔案內容，支援預覽和下載兩種模式。
     * <p>
     * 驗證用戶訪問權限後以流式方式返回檔案內容，預設為預覽模式。
     * 處理驗證異常並提供適當的錯誤響應。
     *
     * @param action   檔案處理動作，"preview" 為預覽，"download" 為下載
     * @param id       檔案唯一識別碼
     * @param exchange 伺服器 Web 交換對象
     * @return 包含檔案數據流的響應
     */
    @GetMapping("/{id}")
    public Mono<ResponseEntity<Flux<DataBuffer>>> downloadFile(
            @RequestParam(value = "action", defaultValue = "preview", required = false) String action,
            @PathVariable Long id, ServerWebExchange exchange) {
        return super.downloadFile(action, id, exchange).onErrorResume(ValidationException.class, e -> handleDownloadValidationError(e, exchange));
    }


    /**
     * 根據檔案 ID 執行檔案刪除操作。
     * <p>
     * 驗證用戶權限後永久刪除指定檔案及其相關元資料。
     *
     * @param id       檔案唯一識別碼
     * @param exchange 伺服器 Web 交換對象
     * @return 包含刪除操作結果的響應
     */
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<?>> deleteFile(@PathVariable String id, ServerWebExchange exchange) {
        return super.deleteFile(id, exchange);
    }


    /**
     * 處理檔案編輯請求，更新檔案內容或元資料。
     * <p>
     * 驗證編輯權限後根據編輯類型執行相應的檔案修改操作。
     *
     * @param fileEditDTO 檔案編輯資料傳輸對象，包含編輯類型和內容
     * @param exchange    伺服器 Web 交換對象
     * @return 包含編輯操作結果的響應
     */
    @PutMapping("")
    public Mono<ResponseEntity<?>> editFile(@Validated @RequestBody FileEditDTO fileEditDTO, ServerWebExchange exchange) {
        return super.editFile(fileEditDTO, exchange);
    }


    /**
     * 處理檔案分塊上傳請求，支援多種傳輸方式。
     * <p>
     * 根據傳輸類型參數選擇適當的上傳處理方式，包括分塊上傳和 Multipart 上傳。
     * 整合錯誤處理機制確保上傳過程的穩定性。
     *
     * @param transmissionType 傳輸類型，指定上傳方式
     * @param exchange         伺服器 Web 交換對象
     * @return 包含上傳處理結果的響應
     */
    @PostMapping("/upload-chunk")
    public Mono<ResponseEntity<?>> uploadFileData(
            @RequestParam(name = "type", required = false) String transmissionType, ServerWebExchange exchange) {
        return handleError(super.uploadFileData(transmissionType, exchange), exchange);
    }


    /**
     * 獲取當前用戶的檔案列表，支援分頁和類型過濾。
     * <p>
     * 返回用戶有權訪問的檔案清單，支援按檔案類型篩選和分頁顯示。
     * 使用 @HideOverLength 註解隱藏過長的響應內容以避免日誌污染。
     *
     * @param exchange 伺服器 Web 交換對象
     * @param page     分頁頁碼，預設為 1
     * @param size     每頁檔案數量，可選參數
     * @param types    檔案類型過濾清單，可選參數
     * @return 包含分頁檔案列表的響應
     */
    @HideOverLength
    @GetMapping("/user-file-list")
    public Mono<ResponseEntity<?>> getUserFileList(ServerWebExchange exchange,
                                                   @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
                                                   @RequestParam(value = "size", required = false) Integer size,
                                                   @RequestParam(value = "type", required = false) List<String> types) {
        return super.getUserFileList(exchange, page, size, types);
    }


    /**
     * 根據檔案 ID 獲取檔案詳細資訊。
     * <p>
     * 返回檔案的元資料、類型資訊和相關屬性，用於檔案資訊展示。
     *
     * @param id       檔案唯一識別碼
     * @param exchange 伺服器 Web 交換對象
     * @return 包含檔案詳細資訊的響應
     */
    @GetMapping("/{id}/info")
    public Mono<ResponseEntity<?>> getFileInfo(@PathVariable Long id, ServerWebExchange exchange) {
        return super.getFileType(id, exchange);
    }


    /**
     * 將指定檔案移動到回收站，實現軟刪除。
     * <p>
     * 將檔案標記為已刪除狀態而非立即物理刪除，允許後續恢復操作。
     *
     * @param exchange 伺服器 Web 交換對象
     * @param id       檔案唯一識別碼
     * @return 包含移動操作結果的響應
     */
    @PostMapping("/remove/{id}")
    public Mono<ResponseEntity<?>> removeFile(ServerWebExchange exchange, @PathVariable String id) {
        return super.removeFile(exchange, id, null);
    }


    /**
     * 從回收站還原指定檔案。
     * <p>
     * 將已標記刪除的檔案恢復為正常狀態，使其重新可用。
     *
     * @param exchange 伺服器 Web 交換對象
     * @param id       檔案唯一識別碼
     * @return 包含還原操作結果的響應
     */
    @PostMapping("/restore/{id}")
    public Mono<ResponseEntity<?>> restoreFile(ServerWebExchange exchange, @PathVariable String id) {
        return super.restoreFile(exchange, id, null);
    }


    /**
     * 根據多種條件搜尋檔案，支援複合過濾和分頁。
     * <p>
     * 提供關鍵字匹配、資料夾範圍、檔案類型、時間範圍等多維度搜尋功能。
     * 支援搜尋已刪除和已共享檔案，滿足不同使用場景需求。
     *
     * @param exchange  伺服器 Web 交換對象
     * @param keyword   檔案名稱關鍵字，支援模糊匹配
     * @param folderId  指定搜尋的資料夾範圍
     * @param types     檔案類型過濾清單
     * @param page      分頁頁碼，預設為 1
     * @param size      每頁檔案數量，預設為 0（不限制）
     * @param deleted   是否包含已刪除檔案，預設為 false
     * @param shared    是否包含已共享檔案，預設為 false
     * @param startDate 檔案建立時間範圍起始
     * @param endDate   檔案建立時間範圍結束
     * @return 包含搜尋結果的分頁響應
     */
    @GetMapping("/search")
    public Mono<ResponseEntity<?>> search(ServerWebExchange exchange,
                                          @RequestParam(value = "keyword", required = false) String keyword,
                                          @RequestParam(value = "folder", required = false) Long folderId,
                                          @RequestParam(value = "type", required = false) List<String> types,
                                          @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
                                          @RequestParam(value = "size", required = false, defaultValue = "0") Integer size,
                                          @RequestParam(value = "deleted", required = false, defaultValue = "false") Boolean deleted,
                                          @RequestParam(value = "shared", required = false, defaultValue = "false") Boolean shared,
                                          @RequestParam(value = "start", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                          LocalDateTime startDate,
                                          @RequestParam(value = "end", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                          LocalDateTime endDate) {
        FileFilterDTO fileFilterDTO = new FileFilterDTO(keyword, folderId, getFileEnums(types), page, size, startDate, endDate, deleted, shared);
        return super.searchFile(exchange, fileFilterDTO);
    }
}
