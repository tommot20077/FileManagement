package xyz.dowob.filemanagement.controller.api;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.HideOverLength;
import xyz.dowob.filemanagement.component.strategy.FileServiceStrategy;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.controller.base.BaseFileController;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.data.file.dto.FileEditDTO;
import xyz.dowob.filemanagement.data.file.dto.FileMetadataDTO;
import xyz.dowob.filemanagement.service.serviceInterface.UserService;
import xyz.dowob.filemanagement.service.serviceInterface.ValidationService;

import java.util.Map;

/**
 * 在線文件 API 控制器，用於處理在線文件的相關請求
 * 此類繼承自 BaseFileController，用於處理文件相關的請求
 *
 * @author yuan
 * @program FileManagement
 * @ClassName ApiOnlineFileController
 * @create 2025/2/11
 * @Version 1.0
 **/
@RestController
@RequestMapping("/api/docs")
public class ApiOnlineFileController extends BaseFileController {
    /**
     * 驗證服務
     */
    private final ValidationService validationService;

    public ApiOnlineFileController(UserService userService, FileServiceStrategy fileServiceStrategy, FileProperties fileProperties, ValidationService validationService) {
        super(userService, fileServiceStrategy, fileProperties);
        this.validationService = validationService;
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
        return handleError(userService.getUser(exchange).flatMap(user -> {
            return fileServiceStrategy
                    .getFileService(FileEnum.ONLINE_DOCUMENT)
                    .uploadFile(fileMetadataDTO, user)
                    .flatMap(uploadResponseDTO -> createResponseEntity(createResponse(exchange, "上傳成功", uploadResponseDTO)));
        }), exchange);
    }

    /**
     * 下載文件
     *
     * @param id       文件 ID
     * @param exchange 服務器 Web 交換對象
     *
     * @return 返回 Mono<ResponseEntity<?>> 用於異步處理請求
     */
    @GetMapping("/{id}")
    @HideOverLength
    public Mono<ResponseEntity<?>> downloadFile(@PathVariable String id, ServerWebExchange exchange) {
        return handleError(userService.getUser(exchange).flatMap(user -> {
            return fileServiceStrategy.getFileService(FileEnum.ONLINE_DOCUMENT).downloadFile(id, user).flatMap(userFileDataBO -> {
                Map<String, Object> data = Map.of("content", userFileDataBO.getContent(), "filename", userFileDataBO.getFileName());
                return createResponseEntity(createResponse(exchange, "下載成功", data));
            });
        }), exchange);
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
        return handleError(userService
                                   .getUser(exchange)
                                   .flatMap(user -> fileServiceStrategy
                                           .getFileService(FileEnum.ONLINE_DOCUMENT)
                                           .deleteFile(id, user)
                                           .then(createResponseEntity(createResponse(exchange, "刪除成功", null)))), exchange);
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
        return handleError(validationService
                                   .validateEditFileDTO(fileEditDTO, false)
                                   .then(userService.getUser(exchange))
                                   .flatMap(user -> fileServiceStrategy
                                           .getFileService(FileEnum.ONLINE_DOCUMENT)
                                           .editFile(fileEditDTO, user))
                                   .then(createResponseEntity(createResponse(exchange, "編輯成功", null))), exchange);
    }

    @GetMapping("/history/{id}")
    public Mono<ResponseEntity<?>> getHistory(ServerWebExchange exchange,
                                              @PathVariable String id,
                                              @RequestParam(required = false, defaultValue = "1") Integer page,
                                              @RequestParam(required = false) Integer pageSize) {
        return handleError(userService.getUser(exchange).flatMap(user -> {
            return fileServiceStrategy
                    .getFileService(FileEnum.ONLINE_DOCUMENT)
                    .getFileVersionList(user, id, page, pageSize)
                    .flatMap(history -> createResponseEntity(createResponse(exchange, "獲取歷史成功", history)));
        }), exchange);
    }

}
