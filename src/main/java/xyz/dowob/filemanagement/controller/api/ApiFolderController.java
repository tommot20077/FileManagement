package xyz.dowob.filemanagement.controller.api;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.HideOverLength;
import xyz.dowob.filemanagement.component.strategy.FileStrategy;
import xyz.dowob.filemanagement.component.strategy.UserLimiterStrategy;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.controller.base.BaseFileController;
import xyz.dowob.filemanagement.data.file.dto.FileEditDTO;
import xyz.dowob.filemanagement.service.ServiceInterface.FileService;
import xyz.dowob.filemanagement.service.ServiceInterface.UserService;
import xyz.dowob.filemanagement.service.ServiceInterface.ValidationService;

/**
 * 資料夾的 API 控制器，用於處理資料夾的 API 請求
 * 用於處理資料夾的增刪改查操作
 * 繼承自 BaseFileController，該類為基礎的文件控制器，用於處理文件的基本操作
 * @author yuan
 * @program FileManagement
 * @ClassName ApiFileUploadController
 * @description
 * @create 2024-09-30 16:00
 * @Version 1.0
 **/
@RestController
@RequestMapping("/api/folders")
public class ApiFolderController extends BaseFileController {
    public ApiFolderController(FileService fileService, UserService userService, FileStrategy fileStrategy, UserLimiterStrategy userLimiterStrategy, ValidationService validationService, FileProperties fileProperties) {
        super(fileService, userService, fileStrategy, userLimiterStrategy, validationService, fileProperties);
    }


    /**
     * 獲取資料夾內的檔案列表，此 ID 為資料夾 ID
     *
     * @param id       資料夾ID
     * @param exchange 請求對象
     *
     * @return 檔案列表
     */
    @GetMapping({"/{id}"})
    @HideOverLength
    public Mono<ResponseEntity<?>> getFolderFiles(@PathVariable Long id, ServerWebExchange exchange) {
        return super.getUserFileList(exchange, id);
    }

    /**
     * 刪除資料夾，此操作會刪除資料夾下的所有檔案
     *
     * @param id       資料夾ID
     * @param exchange 請求對象
     *
     * @return 刪除結果
     */
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<?>> deleteFolder(@PathVariable String id, ServerWebExchange exchange) {
        return handleError(userService
                                   .getUser(exchange)
                                   .flatMap(user -> fileService.deleteFolder(id, user))
                                   .then(createResponseEntity(createResponse(exchange, "刪除資料夾成功", null))), exchange);
    }

    /**
     * 編輯資料夾，此操作用於修改資料夾名稱、位置以及分享狀態
     *
     * @param fileEditDTO 編輯資料
     * @param exchange    請求對象
     *
     * @return 編輯結果
     */
    //todo 移動到自身子目錄下錯誤檢查
    @PutMapping()
    public Mono<ResponseEntity<?>> editFolder(@Validated @RequestBody FileEditDTO fileEditDTO, ServerWebExchange exchange) {
        return handleError(validationService
                                   .validateEditFileDTO(fileEditDTO, true)
                                   .then(validationService.validSpecifyColumn(fileEditDTO, "fileId"))
                                   .then(userService.getUser(exchange))
                                   .flatMap(user -> fileService.editFolder(fileEditDTO, user))
                                   .then(createResponseEntity(createResponse(exchange, "資料夾更新成功", null))), exchange);
    }

    /**
     * 創建資料夾
     *
     * @param fileEditDTO 資料夾資料
     * @param exchange    請求對象
     *
     * @return 創建結果
     */
    @PostMapping
    public Mono<ResponseEntity<?>> createFolder(@Validated @RequestBody FileEditDTO fileEditDTO, ServerWebExchange exchange) {
        return handleError(validationService
                                   .validateEditFileDTO(fileEditDTO, true)
                                   .then(userService.getUser(exchange))
                                   .flatMap(user -> fileStrategy.getFileService(null).createFolder(fileEditDTO, user))
                                   .then(createResponseEntity(createResponse(exchange, "資料夾建立成功", null))), exchange);
    }
}
