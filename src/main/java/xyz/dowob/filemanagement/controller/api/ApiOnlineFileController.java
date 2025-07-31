package xyz.dowob.filemanagement.controller.api;

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
 * 基於 WebFlux 反應式編程的線上檔案 RESTful API 控制器實現。
 * <p>
 * 提供非阻塞的線上檔案管理操作，包括線上編輯檔案的創建、
 * 下載、編輯、刪除、還原和歷史版本管理。特別支持即時編輯功能，
 * 包括檔案編輯事件發布和版本追蹤。
 * <p>
 * 繼承自 {@link BaseOnlineFileController}，採用策略模式實現多種檔案服務操作。
 * 所有端點要求用戶認證，並透過註解實現細粒度權限控制。
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@RestController
@RecordLevel(LogLevelEnum.INFO)
@RequestMapping("/api/v1/docs")
public class ApiOnlineFileController extends BaseOnlineFileController {
    /**
     * 初始化線上檔案 API 控制器，透過依賴注入設定所需服務組件。
     *
     * @param userService 用戶服務，處理用戶身份認證與用戶資料管理
     * @param fileServiceStrategy 檔案服務策略，提供多種檔案操作實現
     * @param fileProperties 檔案設定屬性，定義檔案處理的基本參數
     * @param validationService 驗證服務，執行請求參數的格式檢查
     * @param permissionService 權限服務，實現用戶檔案操作的權限驗證
     * @param objectMapper JSON 對象映射器，處理數據序列化與反序列化
     * @param filePermissionRuleManager 檔案權限規則管理器，控制檔案存取規則
     * @param eventSink 檔案編輯事件發布器，發布檔案更改事件
     */
    public ApiOnlineFileController(UserService userService, FileServiceStrategy fileServiceStrategy, FileProperties fileProperties, ValidationService validationService, PermissionService<UserFileMetadata> permissionService, ObjectMapper objectMapper, FilePermissionRuleManager filePermissionRuleManager, EventSink<FileEditedMessage> eventSink) {
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
     * 創建新的線上可編輯檔案。
     * <p>
     * 此端點接受檔案元資料，創建一個新的可編輯檔案實例。
     * 支援多種檔案類型的線上編輯功能，包括文本文件、文檔等。
     * 創建成功後將返回檔案的識別資訊和基本屬性。
     *
     * @param fileMetadataDTO 包含檔案元資料的資料傳輸物件，包括檔案名稱、類型、內容等資訊
     * @param exchange WebFlux 伺服器交換物件，包含請求上下文和用戶認證資訊
     * @return 包含檔案創建結果的反應式響應實體，成功時返回檔案基本資訊
     */
    @PostMapping("/upload")
    public Mono<ResponseEntity<?>> uploadFile(@RequestBody FileMetadataDTO fileMetadataDTO, ServerWebExchange exchange) {
        return super.uploadFile(fileMetadataDTO, exchange);
    }


    /**
     * 下載指定的線上檔案，支持預覽和下載模式。
     * <p>
     * 提供檔案的下載和預覽功能。預覽模式適用於在瀏覽器中直接查看檔案內容，
     * 下載模式則觸發檔案下載到本地設備。系統會根據檔案類型自動設定適當的
     * Content-Type 和 Content-Disposition 標頭。
     *
     * @param id 檔案識別碼，用於唯一標識要下載的檔案
     * @param action 操作類型，預設值為 "preview"（預覽），可選 "download"（下載）
     * @param exchange WebFlux 伺服器交換物件，包含請求上下文和用戶認證資訊
     * @return 包含檔案資料流的反應式響應實體，以 DataBuffer 流的形式返回檔案內容
     */
    @HideOverLength
    @GetMapping("/{id}")
    public Mono<ResponseEntity<Flux<DataBuffer>>> downloadFile(
            @PathVariable String id, @RequestParam(required = false, defaultValue = "preview") String action, ServerWebExchange exchange) {
        return super.downloadFile(action, id, exchange);
    }


    /**
     * 永久刪除指定的線上檔案。
     * <p>
     * 執行檔案的永久刪除操作，此操作不可逆。檔案將從系統中完全移除，
     * 包括其所有版本歷史記錄。用戶必須具備相應的刪除權限才能執行此操作。
     *
     * @param id 檔案識別碼，用於唯一標識要刪除的檔案
     * @param exchange WebFlux 伺服器交換物件，包含請求上下文和用戶認證資訊
     * @return 包含刪除操作結果的反應式響應實體，成功時返回確認訊息
     */
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<?>> deleteFile(@PathVariable String id, ServerWebExchange exchange) {
        return super.deleteFile(id, exchange);
    }


    /**
     * 編輯指定線上檔案的內容。
     * <p>
     * 更新線上檔案的內容，支援即時編輯功能。編輯操作會自動創建新的版本記錄，
     * 保留檔案的變更歷史。系統會發布檔案編輯事件以通知其他相關組件。
     * 支援協作編輯和衝突檢測機制。
     *
     * @param fileEditDTO 包含檔案編輯資訊的資料傳輸物件，包括檔案ID、新內容、編輯類型等
     * @param exchange WebFlux 伺服器交換物件，包含請求上下文和用戶認證資訊
     * @return 包含編輯操作結果的反應式響應實體，成功時返回更新後的檔案資訊
     */
    @PutMapping("")
    public Mono<ResponseEntity<?>> editFile(@Validated @RequestBody FileEditDTO fileEditDTO, ServerWebExchange exchange) {
        return super.editFile(fileEditDTO, exchange);
    }


    /**
     * 獲取指定線上檔案的歷史版本清單。
     * <p>
     * 提供檔案編輯歷史的分頁查詢功能，按時間順序返回所有版本記錄。
     * 每個版本記錄包含編輯時間、編輯者資訊、變更摘要等詳細資訊。
     * 支援分頁查詢以提高大量歷史記錄的查詢效能。
     *
     * @param exchange WebFlux 伺服器交換物件，包含請求上下文和用戶認證資訊
     * @param id 檔案識別碼，用於唯一標識要查詢歷史的檔案
     * @param page 分頁頁碼，預設值為 1，必須為正整數
     * @param pageSize 每頁項目數量，可選參數，未指定時使用系統預設值
     * @return 包含歷史版本清單的反應式響應實體，以分頁格式返回版本記錄
     */
    @GetMapping("/history/{id}")
    public Mono<ResponseEntity<?>> getHistory(ServerWebExchange exchange,
                                              @PathVariable String id,
                                              @RequestParam(required = false, defaultValue = "1") Integer page,
                                              @RequestParam(required = false) Integer pageSize) {
        return super.getHistory(exchange, id, page, pageSize);
    }


    /**
     * 將指定線上檔案移動到回收站。
     * <p>
     * 執行軟刪除操作，將檔案標記為已刪除並移動到回收站。
     * 檔案不會立即從系統中移除，而是保留在回收站中，
     * 允許用戶在一定時間內進行還原操作。
     *
     * @param exchange WebFlux 伺服器交換物件，包含請求上下文和用戶認證資訊
     * @param id 檔案識別碼，用於唯一標識要移動到回收站的檔案
     * @return 包含移動操作結果的反應式響應實體，成功時返回確認訊息
     */
    @PostMapping("/remove/{id}")
    public Mono<ResponseEntity<?>> removeFile(ServerWebExchange exchange, @PathVariable String id) {
        return super.removeFile(exchange, id, null);
    }


    /**
     * 從回收站還原指定線上檔案。
     * <p>
     * 恢復已刪除的檔案，將其從回收站狀態恢復為正常可用狀態。
     * 還原後檔案將重新出現在用戶的檔案清單中，並保持原有的
     * 權限設定和所有歷史版本記錄。
     *
     * @param exchange WebFlux 伺服器交換物件，包含請求上下文和用戶認證資訊
     * @param id 檔案識別碼，用於唯一標識要還原的檔案
     * @return 包含還原操作結果的反應式響應實體，成功時返回還原後的檔案資訊
     */
    @PostMapping("/restore/{id}")
    public Mono<ResponseEntity<?>> restoreFile(ServerWebExchange exchange, @PathVariable String id) {
        return super.restoreFile(exchange, id, null);
    }
}
