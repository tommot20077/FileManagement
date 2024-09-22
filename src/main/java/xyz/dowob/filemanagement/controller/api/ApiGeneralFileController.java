package xyz.dowob.filemanagement.controller.api;

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
import xyz.dowob.filemanagement.component.manager.FilePermissionRuleManager;
import xyz.dowob.filemanagement.component.strategy.FileServiceStrategy;
import xyz.dowob.filemanagement.component.strategy.UserLimiterStrategy;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.controller.base.BaseGeneralFileController;
import xyz.dowob.filemanagement.data.file.dto.FileEditDTO;
import xyz.dowob.filemanagement.data.file.dto.FileFilterDTO;
import xyz.dowob.filemanagement.data.file.dto.FileMetadataDTO;
import xyz.dowob.filemanagement.entity.UserFileMetadata;
import xyz.dowob.filemanagement.service.serviceInterface.PermissionService;
import xyz.dowob.filemanagement.service.serviceInterface.UserService;
import xyz.dowob.filemanagement.service.serviceInterface.ValidationService;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文件的 API 控制器，用於處理文件相關的 API 請求
 * 用於處理文件的增刪改查操作
 * 繼承自 BaseGeneralFileController，該類為基礎的文件控制器，用於處理文件的基本操作
 *
 * @author yuan
 * @program FileManagement
 * @ClassName ApiFileController
 * @description
 * @create 2024-09-30 16:00
 * @Version 1.0
 **/
@RestController
@RequestMapping("/api/v1/files")
public class ApiGeneralFileController extends BaseGeneralFileController {
    public ApiGeneralFileController(UserService userService, FileServiceStrategy fileServiceStrategy, UserLimiterStrategy userLimiterStrategy, ValidationService validationService, FileProperties fileProperties, ObjectMapper objectMapper, PermissionService<UserFileMetadata> permissionService, FilePermissionRuleManager filePermissionRuleManager) {
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
     * 上傳文件的 API 請求，此步驟為預先上傳文件的元數據檢驗檔案的步驟
     * 處理完成之後依照結果提供繼續上傳文件分塊或是完成操作
     *
     * @param fileMetadataDTO 上傳文件的元數據
     * @param exchange        請求對象
     *
     * @return Mono<ResponseEntity < ?>> 返回上傳文件的結果
     */
    @PostMapping("/upload")
    public Mono<ResponseEntity<?>> uploadFile(@RequestBody FileMetadataDTO fileMetadataDTO, ServerWebExchange exchange) {
        return super.uploadFile(fileMetadataDTO, exchange);
    }


    /**
     * 上傳文件分塊的 API 請求，根據文件 ID 上傳文件分塊
     * 會依照用戶選擇的上傳方式進行上傳
     *
     * @param exchange 請求對象
     *
     * @return Mono<ResponseEntity < ?>> 返回上傳文件分塊的結果
     */
    @PostMapping("/upload-chunk")
    public Mono<ResponseEntity<?>> uploadFileData(
            @RequestParam(name = "type", required = false) String transmissionType, ServerWebExchange exchange) {
        return handleError(super.uploadFileData(transmissionType, exchange), exchange);
    }


    /**
     * 刪除文件的 API 請求，根據文件 ID 刪除文件
     *
     * @param id       文件 ID
     * @param exchange 請求對象
     *
     * @return Mono<ResponseEntity < ?>> 返回刪除文件的結果
     */
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<?>> deleteFile(@PathVariable String id, ServerWebExchange exchange) {
        return super.deleteFile(id, exchange);
    }


    /**
     * 下載文件的 API 請求，根據文件 ID 下載文件並提供預覽或是下載
     * 預設為預覽
     *
     * @param id       文件 ID
     * @param action   預覽或是下載
     * @param exchange 請求對象
     *
     * @return Mono<ResponseEntity < Flux < DataBuffer>>> 返回文件流
     */
    @GetMapping("/{id}")
    public Mono<ResponseEntity<Flux<DataBuffer>>> downloadFile(
            @RequestParam(value = "action", defaultValue = "preview", required = false) String action,
            @PathVariable Long id, ServerWebExchange exchange) {
        return super.downloadFile(action, id, exchange);
    }


    /**
     * 獲取文件的信息的 API 請求，根據文件 ID 獲取文件的信息
     *
     * @param id       文件 ID
     * @param exchange 請求對象
     *
     * @return Mono<ResponseEntity < ?>> 返回文件的信息
     */
    @GetMapping("/{id}/info")
    public Mono<ResponseEntity<?>> getFileInfo(@PathVariable Long id, ServerWebExchange exchange) {
        return super.getFileType(id, exchange);
    }


    /**
     * 編輯文件的 API 請求，根據文件 ID 編輯文件
     *
     * @param fileEditDTO 文件編輯的元數據
     * @param exchange    請求對象
     *
     * @return Mono<ResponseEntity < ?>> 返回編輯文件的結果
     */
    @PutMapping("")
    public Mono<ResponseEntity<?>> editFile(@Validated @RequestBody FileEditDTO fileEditDTO, ServerWebExchange exchange) {
        return super.editFile(fileEditDTO, exchange);
    }


    /**
     * 獲取用戶文件列表的API請求
     *
     * @param exchange 請求對象
     *
     * @return Mono<ResponseEntity> 返回用戶文件列表
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
     * 將檔案移動到回收站的 API 請求
     *
     * @param exchange 請求對象
     * @param id       檔案 ID
     *
     * @return Mono<ResponseEntity < ?>> 返回檔案移動到回收站的結果
     */
    @PostMapping("/remove/{id}")
    public Mono<ResponseEntity<?>> removeFile(ServerWebExchange exchange, @PathVariable String id) {
        return super.removeFile(exchange, id, null);
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
        return super.restoreFile(exchange, id, null);
    }


    /**
     * 搜索文件的 API 請求
     *
     * @param exchange  請求對象，包含用戶的上下文。
     * @param keyword   關鍵字，用於模糊匹配文件名稱。
     * @param folderId  資料夾 ID，指定要搜索的資料夾。
     * @param page      分頁頁碼。
     * @param size      每頁顯示的文件數量。
     * @param types     文件類型，用於過濾特定類型的文件。
     * @param startDate 文件創建時間的起始時間，用於範圍過濾。
     * @param endDate   文件創建時間的結束時間，用於範圍過濾。
     * @param deleted   是否包含已刪除的文件
     * @param shared    是否包含已共享的文件
     *
     * @return Mono<ResponseEntity < ?>> 返回搜索文件的結果
     */
    @GetMapping("/search")
    public Mono<ResponseEntity<?>> search(ServerWebExchange exchange,
                                          @RequestParam(value = "keyword", required = false) String keyword,
                                          @RequestParam(value = "folder", required = false) Long folderId,
                                          @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
                                          @RequestParam(value = "size", required = false) Integer size,
                                          @RequestParam(value = "type", required = false) List<String> types,
                                          @RequestParam(value = "deleted", required = false) Boolean deleted,
                                          @RequestParam(value = "shared", required = false) Boolean shared,
                                          @RequestParam(value = "start", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                              LocalDateTime startDate,
                                          @RequestParam(value = "end", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                              LocalDateTime endDate) {
        FileFilterDTO fileFilterDTO = new FileFilterDTO(keyword, folderId, getFileEnums(types), page, size, startDate, endDate, deleted, shared);
        return super.searchFile(exchange, fileFilterDTO);
    }
}
