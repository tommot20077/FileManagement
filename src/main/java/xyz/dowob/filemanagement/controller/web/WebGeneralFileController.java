package xyz.dowob.filemanagement.controller.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.Part;
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
import xyz.dowob.filemanagement.customenum.TransmissionEnum;
import xyz.dowob.filemanagement.data.file.dto.FileEditDTO;
import xyz.dowob.filemanagement.data.file.dto.FileFilterDTO;
import xyz.dowob.filemanagement.data.file.dto.FileMetadataDTO;
import xyz.dowob.filemanagement.data.file.dto.UploadChunkDTO;
import xyz.dowob.filemanagement.entity.UserFileMetadata;
import xyz.dowob.filemanagement.service.serviceInterface.PermissionService;
import xyz.dowob.filemanagement.service.serviceInterface.UserService;
import xyz.dowob.filemanagement.service.serviceInterface.ValidationService;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文件的 Web 控制器，用於處理文件相關的 Web 請求
 * 用於處理文件的增刪改查操作
 * 繼承自 BaseGeneralFileController，該類為基礎的文件控制器，用於處理文件的基本操作
 *
 * @author yuan
 * @program FileManagement
 * @ClassName WebFileController
 * @description
 * @create 2024-09-30 16:00
 * @Version 1.0
 **/
@RestController
@RequestMapping("/web/v1/files")
public class WebGeneralFileController extends BaseGeneralFileController {
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
     * 上傳文件的 Web 請求，此步驟為預先上傳文件的元數據檢驗檔案的步驟
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
     * 下載文件的 Web 請求，根據文件 ID 下載文件並提供預覽或是下載
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
     * 刪除文件的 Web 請求，根據文件 ID 刪除文件
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
     * 編輯文件的 Web 請求，根據文件 ID 編輯文件
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
     * 上傳文件分塊的 Web 請求，根據文件 ID 上傳文件分塊
     *
     * @param exchange       請求對象
     * @param uploadChunkDTO 上傳文件分塊的元數據
     *
     * @return Mono<ResponseEntity < ?>> 返回上傳文件分塊的結果
     */
    @PostMapping("/upload-chunk")
    public Mono<ResponseEntity<?>> uploadFile(ServerWebExchange exchange,
                                              //@RequestPart(value = "transferTaskId", required = false) String transferTaskId,
                                              //@RequestPart(value = "file", required = false) Mono<Part> filePart,
                                              @RequestBody(required = false) UploadChunkDTO uploadChunkDTO) {
        TransmissionEnum transmissionType = fileProperties.getUpload().getTransmissionType();
        return handleChunkUpload(uploadChunkDTO, exchange);
        //todo 未來支持其他傳輸類型
    }

    /**
     * 此方法為分塊上傳的處理方法
     *
     * @param uploadChunkDTO 上傳文件分塊的元數據
     * @param exchange       請求對象
     *
     * @return Mono<ResponseEntity < ?>> 返回上傳文件分塊的結果
     */
    protected Mono<ResponseEntity<?>> handleChunkUpload(@RequestBody UploadChunkDTO uploadChunkDTO, ServerWebExchange exchange) {
        return super.handleChunkUpload(uploadChunkDTO, exchange);
    }


    /**
     * Multipart 上傳文件的 Web 請求
     *
     * @param transferTaskId 文件 ID
     * @param filePart       文件分塊
     * @param exchange       請求對象
     *
     * @return Mono<ResponseEntity < ?>> 返回上傳結果
     */
    // todo 暫不使用
    public Mono<ResponseEntity<?>> handleMultipartUpload(
            @RequestPart("transferTaskId") String transferTaskId, @RequestPart("file") Mono<Part> filePart, ServerWebExchange exchange) {
        return super.handleMultipartUpload(transferTaskId, filePart, exchange);
    }


    /**
     * 獲取用戶文件列表的Web請求
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
     * 將檔案移動到回收站的 Web 請求
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
     * 還原檔案的 Web 請求
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
     * 搜索文件的 Web 請求
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
