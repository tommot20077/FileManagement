package xyz.dowob.filemanagement.controller.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.HideOverLength;
import xyz.dowob.filemanagement.annotation.RecordLevel;
import xyz.dowob.filemanagement.component.event.EventSink;
import xyz.dowob.filemanagement.component.manager.FilePermissionRuleManager;
import xyz.dowob.filemanagement.component.strategy.FileServiceStrategy;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.controller.base.BaseOnlineFileController;
import xyz.dowob.filemanagement.customenum.LogLevelEnum;
import xyz.dowob.filemanagement.data.event.FileEditedMessage;
import xyz.dowob.filemanagement.data.file.dto.FileEditDTO;
import xyz.dowob.filemanagement.data.file.dto.FileMetadataDTO;
import xyz.dowob.filemanagement.entity.UserFileMetadata;
import xyz.dowob.filemanagement.service.serviceInterface.PermissionService;
import xyz.dowob.filemanagement.service.serviceInterface.UserService;
import xyz.dowob.filemanagement.service.serviceInterface.ValidationService;

/**
 * 基於 WebFlux 的線上檔案 Web 控制器，處理線上檔案相關的 RESTful API 操作。
 * <p>
 * 提供線上檔案的完整生命週期管理功能，包括檔案上傳、下載、編輯、刪除、歷史版本管理等操作。
 * 支援即時協作編輯功能，透過事件機制通知其他使用者檔案變更。
 * <p>
 * 此控制器繼承自 {@link BaseOnlineFileController}，採用反應式編程模式處理線上檔案操作，
 * 確保在高併發線上協作場景下的性能表現。所有操作均遵循權限控制機制。
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@RestController
@RecordLevel(LogLevelEnum.INFO)
@RequestMapping("/web/v1/docs")
public class WebOnlineFileController extends BaseOnlineFileController {

    /**
     * 線上檔案 Web 控制器的構造方法。
     *
     * @param userService               用戶服務層對象
     * @param fileServiceStrategy       檔案服務策略對象，用於選擇適當的檔案服務
     * @param fileProperties            檔案屬性設定
     * @param validationService         驗證服務對象
     * @param permissionService         用戶檔案元資料授權服務
     * @param objectMapper              用於處理對象映射的工具
     * @param filePermissionRuleManager 檔案權限規則管理器
     * @param eventSink                 檔案事件發送器
     */
    public WebOnlineFileController(UserService userService, FileServiceStrategy fileServiceStrategy, FileProperties fileProperties, ValidationService validationService, PermissionService<UserFileMetadata> permissionService, ObjectMapper objectMapper, FilePermissionRuleManager filePermissionRuleManager, EventSink<FileEditedMessage> eventSink) {
        super(userService,
              fileServiceStrategy,
              fileProperties,
              validationService,
              permissionService,
              objectMapper,
              filePermissionRuleManager, eventSink
        );
    }


    /**
     * 上傳新的線上檔案至伺服器。
     * <p>
     * 接收檔案元資料和內容資訊，驗證用戶權限後將檔案儲存到系統中。
     * 支援多種檔案格式，並會自動驗證檔案內容和大小限制。
     * 上傳成功後會觸發檔案事件通知相關使用者。
     *
     * @param fileMetadataDTO 包含檔案名稱、類型、內容等元資料的傳輸對象，不可為 null
     * @param exchange        伺服器Web交換對象，包含當前HTTP請求的完整上下文資訊
     *
     * @return 包含上傳結果的響應實體 Mono，成功時返回檔案識別符和基本資訊，失敗時返回錯誤訊息
     */
    @PostMapping("/upload")
    public Mono<ResponseEntity<?>> uploadFile(@RequestBody FileMetadataDTO fileMetadataDTO, ServerWebExchange exchange) {
        return super.uploadFile(fileMetadataDTO, exchange);
    }


    /**
     * 下載或預覽指定的線上檔案。
     * <p>
     * 根據指定的檔案ID和行為類型，擷取檔案內容並以適當的格式返回。
     * 支援預覽和下載兩種模式，預覽模式會設定適當的Content-Type頭，
     * 下載模式則會設定Content-Disposition為附件。
     * 所有操作都會驗證用戶對檔案的訪問權限。
     *
     * @param id       要下載的檔案唯一識別符，不可為 null 或空字串
     * @param action   下載行為類型，預設為"preview"（預覽），可選"download"（下載）
     * @param exchange 伺服器Web交換對象，包含當前HTTP請求的完整上下文資訊
     *
     * @return 包含檔案二進位流的響應實體 Mono，成功時返回檔案內容流，失敗時返回錯誤響應
     */
    @HideOverLength
    @GetMapping("/{id}")
    public Mono<ResponseEntity<Flux<DataBuffer>>> downloadFile(
            @PathVariable String id, @RequestParam(required = false, defaultValue = "preview") String action, ServerWebExchange exchange) {
        return super.downloadFile(action, id, exchange);
    }


    /**
     * 刪除指定的線上檔案。
     * <p>
     * 永久性刪除指定的檔案，包括檔案內容和所有相關的元資料記錄。
     * 此操作不可撤銷，會將檔案從系統中完全移除。
     * 操作前會驗證用戶是否具有刪除權限。
     *
     * @param id       要刪除的檔案唯一識別符，不可為 null 或空字串
     * @param exchange 伺服器Web交換對象，包含當前HTTP請求的完整上下文資訊
     *
     * @return 包含刪除結果的響應實體 Mono，成功時返回確認訊息，失敗時返回錯誤詳情
     */
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<?>> deleteFile(@PathVariable String id, ServerWebExchange exchange) {
        return super.deleteFile(id, exchange);
    }


    /**
     * 編輯線上檔案的內容和屬性。
     * <p>
     * 支援修改檔案內容、檔案名稱、訪問權限等屬性。
     * 編輯操作會自動創建檔案歷史版本，以便後續可以復原。
     * 所有編輯操作都會觸發即時事件通知，支援多人協作編輯。
     *
     * @param fileEditDTO 包含檔案ID、新內容及其他編輯資訊的傳輸對象，經過驗證註解處理，不可為 null
     * @param exchange    伺服器Web交換對象，包含當前HTTP請求的完整上下文資訊
     *
     * @return 包含編輯結果的響應實體 Mono，成功時返回更新後的檔案資訊，失敗時返回錯誤訊息
     */
    @PutMapping("")
    public Mono<ResponseEntity<?>> editFile(@Validated @RequestBody FileEditDTO fileEditDTO, ServerWebExchange exchange) {
        return super.editFile(fileEditDTO, exchange);
    }


    /**
     * 獲取指定檔案的歷史版本記錄列表。
     * <p>
     * 返回指定檔案的所有歷史修改記錄，包括修改時間、修改者、版本說明等資訊。
     * 支援分頁查詢，可指定頁碼和每頁數量以控制返回的資料量。
     * 所有操作都會驗證用戶對檔案的訪問權限。
     *
     * @param exchange 伺服器Web交換對象，包含當前HTTP請求的完整上下文資訊
     * @param id       要查詢歷史的檔案唯一識別符，不可為 null 或空字串
     * @param page     頁碼，從1開始，預設為1
     * @param pageSize 每頁數量，選擇性參數，系統會使用預設值
     *
     * @return 包含檔案歷史列表的響應實體 Mono，成功時返回分頁歷史資料，失敗時返回錯誤訊息
     */
    @GetMapping("/history/{id}")
    public Mono<ResponseEntity<?>> getHistory(ServerWebExchange exchange,
                                              @PathVariable String id,
                                              @RequestParam(required = false, defaultValue = "1") Integer page,
                                              @RequestParam(required = false) Integer pageSize) {
        return super.getHistory(exchange, id, page, pageSize);
    }


    /**
     * 將指定檔案移動到回收站。
     * <p>
     * 软刪除指定的檔案，將其標記為已刪除狀態但不立即永久刪除。
     * 檔案會被放入回收站，用戶可以在一定時間內還原檔案。
     * 操作前會驗證用戶是否具有刪除權限。
     *
     * @param exchange 伺服器Web交換對象，包含當前HTTP請求的完整上下文資訊
     * @param id       要移動到回收站的檔案唯一識別符，不可為 null 或空字串
     *
     * @return 包含移動結果的響應實體 Mono，成功時返回確認訊息，失敗時返回錯誤詳情
     */
    @PostMapping("/remove/{id}")
    public Mono<ResponseEntity<?>> removeFile(ServerWebExchange exchange, @PathVariable String id) {
        return super.removeFile(exchange, id);
    }


    /**
     * 從回收站中還原指定的檔案。
     * <p>
     * 將先前被移動到回收站的檔案還原到正常狀態，
     * 使其重新可被用戶正常訪問和操作。
     * 操作前會驗證檔案是否確實在回收站中以及用戶是否具有還原權限。
     *
     * @param exchange 伺服器Web交換對象，包含當前HTTP請求的完整上下文資訊
     * @param id       要還原的檔案唯一識別符，不可為 null 或空字串
     *
     * @return 包含還原結果的響應實體 Mono，成功時返回確認訊息，失敗時返回錯誤詳情
     */
    @PostMapping("/restore/{id}")
    public Mono<ResponseEntity<?>> restoreFile(ServerWebExchange exchange, @PathVariable String id) {
        return super.restoreFile(exchange, id);
    }
}
