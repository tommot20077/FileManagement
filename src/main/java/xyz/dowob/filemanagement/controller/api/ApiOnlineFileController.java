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
 * 在線文件 API 控制器，用於處理在線文件的相關請求。
 * 此類繼承自 BaseOnlineFileController，提供文件上傳、下載、刪除、編輯等功能。
 *
 * @author yuan
 * @program FileManagement
 * @ClassName ApiOnlineFileController
 * @create 2025/2/11
 * @Version 1.0
 */
@RestController
@RecordLevel(LogLevelEnum.INFO)
@RequestMapping("/api/v1/docs")
public class ApiOnlineFileController extends BaseOnlineFileController {
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
     * @param eventSink             文件事件發送器
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
     * 創建新的可編輯文件。
     *
     * @param fileMetadataDTO 包含文件名稱、類型等信息的 DTO。
     * @param exchange        當前請求上下文。
     *
     * @return Mono<ResponseEntity < ?>>，包含創建結果。
     */
    @PostMapping("/upload")
    public Mono<ResponseEntity<?>> uploadFile(@RequestBody FileMetadataDTO fileMetadataDTO, ServerWebExchange exchange) {
        return super.uploadFile(fileMetadataDTO, exchange);
    }


    /**
     * 下載指定文件。
     *
     * @param action   下載行為，預設為預覽。
     * @param id       目標文件的 ID。
     * @param exchange 當前請求上下文。
     *
     * @return Mono<ResponseEntity < ?>>，包含文件下載鏈接或內容。
     */
    @HideOverLength
    @GetMapping("/{id}")
    public Mono<ResponseEntity<Flux<DataBuffer>>> downloadFile(
            @PathVariable String id, @RequestParam(required = false, defaultValue = "preview") String action, ServerWebExchange exchange) {
        return super.downloadFile(action, id, exchange);
    }


    /**
     * 刪除指定文件。
     *
     * @param id       目標文件的 ID。
     * @param exchange 當前請求上下文。
     *
     * @return Mono<ResponseEntity < ?>>，包含刪除結果。
     */
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<?>> deleteFile(@PathVariable String id, ServerWebExchange exchange) {
        return super.deleteFile(id, exchange);
    }


    /**
     * 編輯指定文件內容。
     *
     * @param fileEditDTO 包含編輯內容的 DTO。
     * @param exchange    當前請求上下文。
     *
     * @return Mono<ResponseEntity < ?>>，包含編輯結果。
     */
    @PutMapping("")
    public Mono<ResponseEntity<?>> editFile(@Validated @RequestBody FileEditDTO fileEditDTO, ServerWebExchange exchange) {
        return super.editFile(fileEditDTO, exchange);
    }


    /**
     * 獲取指定文件的歷史版本列表。
     *
     * @param id       目標文件的 ID。
     * @param page     分頁參數，默認為 1。
     * @param pageSize 每頁數量，默認值根據後端配置。
     * @param exchange 當前請求上下文。
     *
     * @return Mono<ResponseEntity < ?>>，包含歷史版本列表。
     */
    @GetMapping("/history/{id}")
    public Mono<ResponseEntity<?>> getHistory(ServerWebExchange exchange,
                                              @PathVariable String id,
                                              @RequestParam(required = false, defaultValue = "1") Integer page,
                                              @RequestParam(required = false) Integer pageSize) {
        return super.getHistory(exchange, id, page, pageSize);
    }


    /**
     * 將指定文件移動到回收站。
     *
     * @param id       目標文件的 ID。
     * @param exchange 當前請求上下文。
     *
     * @return Mono<ResponseEntity < ?>>，包含移動結果。
     */
    @PostMapping("/remove/{id}")
    public Mono<ResponseEntity<?>> removeFile(ServerWebExchange exchange, @PathVariable String id) {
        return super.removeFile(exchange, id, null);
    }


    /**
     * 還原回收站中的指定文件。
     *
     * @param id       目標文件的 ID。
     * @param exchange 當前請求上下文。
     *
     * @return Mono<ResponseEntity < ?>>，包含還原結果。
     */
    @PostMapping("/restore/{id}")
    public Mono<ResponseEntity<?>> restoreFile(ServerWebExchange exchange, @PathVariable String id) {
        return super.restoreFile(exchange, id, null);
    }
}
