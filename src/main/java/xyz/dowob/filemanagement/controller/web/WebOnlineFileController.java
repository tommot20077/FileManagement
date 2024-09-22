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
import xyz.dowob.filemanagement.component.manager.FilePermissionRuleManager;
import xyz.dowob.filemanagement.component.strategy.FileServiceStrategy;
import xyz.dowob.filemanagement.component.strategy.UserLimiterStrategy;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.controller.base.BaseOnlineFileController;
import xyz.dowob.filemanagement.customenum.LogLevelEnum;
import xyz.dowob.filemanagement.data.file.dto.FileEditDTO;
import xyz.dowob.filemanagement.data.file.dto.FileMetadataDTO;
import xyz.dowob.filemanagement.entity.UserFileMetadata;
import xyz.dowob.filemanagement.service.serviceInterface.PermissionService;
import xyz.dowob.filemanagement.service.serviceInterface.UserService;
import xyz.dowob.filemanagement.service.serviceInterface.ValidationService;

/**
 * 在線文件 API 控制器，用於處理在線文件的相關請求
 * 此類繼承自 BaseFileController，用於處理文件相關的請求
 *
 * @author yuan
 * @program FileManagement
 * @ClassName WebOnlineFileController
 * @create 2025/2/11
 * @Version 1.0
 **/
@RestController
@RecordLevel(LogLevelEnum.INFO)
@RequestMapping("/web/v1/docs")
public class WebOnlineFileController extends BaseOnlineFileController {

    /**
     * 构造函數，用於初始化基本的業務層服務
     *
     * @param userService               用戶服務層對象
     * @param fileServiceStrategy       文件服務策略對象，用於選擇適當的文件服務
     * @param fileProperties            文件屬性設置
     * @param validationService         驗證服務對象
     * @param permissionService         用戶文件元數據授權服務
     * @param userLimiterStrategy       用戶限制策略
     * @param objectMapper              用於處理對象映射的工具
     * @param filePermissionRuleManager 文件權限規則管理器
     */
    public WebOnlineFileController(UserService userService, FileServiceStrategy fileServiceStrategy, FileProperties fileProperties, ValidationService validationService, PermissionService<UserFileMetadata> permissionService, UserLimiterStrategy userLimiterStrategy, ObjectMapper objectMapper, FilePermissionRuleManager filePermissionRuleManager) {
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
     * 上傳文件
     *
     * @param fileMetadataDTO 文件元數據傳輸對象
     * @param exchange        服務器 Web 交換對象
     *
     * @return 返回 Mono<ResponseEntity<?>> 用於異步處理請求
     */
    @PostMapping("/upload")
    public Mono<ResponseEntity<?>> uploadFile(@RequestBody FileMetadataDTO fileMetadataDTO, ServerWebExchange exchange) {
        return super.uploadFile(fileMetadataDTO, exchange);
    }


    /**
     * 下載文件
     *
     * @param action   下載行為，預設為預覽
     * @param id       文件 ID
     * @param exchange 服務器 Web 交換對象
     *
     * @return 返回 Mono<ResponseEntity<?>> 用於異步處理請求
     */
    @HideOverLength
    @GetMapping("/{id}")
    public Mono<ResponseEntity<Flux<DataBuffer>>> downloadFile(
            @PathVariable String id, @RequestParam(required = false, defaultValue = "preview") String action, ServerWebExchange exchange) {
        return super.downloadFile(action, id, exchange);
    }


    /**
     * 獲取文件元數據
     *
     * @param id       文件 ID
     * @param exchange 服務器 Web 交換對象
     *
     * @return 返回 Mono<ResponseEntity<?>> 用於異步處理請求
     */
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<?>> deleteFile(@PathVariable String id, ServerWebExchange exchange) {
        return super.deleteFile(id, exchange);
    }


    /**
     * 編輯文件
     *
     * @param fileEditDTO 文件編輯數據傳輸對象
     * @param exchange    服務器 Web 交換對象
     *
     * @return 返回 Mono<ResponseEntity<?>> 用於異步處理請求
     */
    @PutMapping("")
    public Mono<ResponseEntity<?>> editFile(@Validated @RequestBody FileEditDTO fileEditDTO, ServerWebExchange exchange) {
        return super.editFile(fileEditDTO, exchange);
    }


    /**
     * 獲取文件歷史資料列表
     *
     * @param exchange 服務器 Web 交換對象
     * @param id       文件 ID
     *
     * @return 返回 Mono<ResponseEntity<?>> 用於異步處理請求
     */
    @GetMapping("/history/{id}")
    public Mono<ResponseEntity<?>> getHistory(ServerWebExchange exchange,
                                              @PathVariable String id,
                                              @RequestParam(required = false, defaultValue = "1") Integer page,
                                              @RequestParam(required = false) Integer pageSize) {
        return super.getHistory(exchange, id, page, pageSize);
    }


    /**
     * 將檔案移動到回收站的 API 請求
     *
     * @param exchange 請求對象
     * @param id       檔案 ID
     *
     * @return Mono<ResponseEntity < ?>> 返回檔案移動到回收站的結果
     */
    @PostMapping("/remove/{id}")
    public Mono<ResponseEntity<?>> removeFile(ServerWebExchange exchange, @PathVariable String id) {
        return super.removeFile(exchange, id);
    }


    /**
     * 還原檔案的 API 請求
     *
     * @param exchange 請求對象
     * @param id       檔案 ID
     *
     * @return Mono<ResponseEntity < ?>> 返回還原檔案的結果
     */
    @PostMapping("/restore/{id}")
    public Mono<ResponseEntity<?>> restoreFile(ServerWebExchange exchange, @PathVariable String id) {
        return super.restoreFile(exchange, id);
    }
}
