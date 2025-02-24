package xyz.dowob.filemanagement.controller.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.Part;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.HideOverLength;
import xyz.dowob.filemanagement.component.limiter.UserLimiter;
import xyz.dowob.filemanagement.component.strategy.FileServiceStrategy;
import xyz.dowob.filemanagement.component.strategy.UserLimiterStrategy;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.controller.base.BaseFileController;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.customenum.ReservedSearchIdEnum;
import xyz.dowob.filemanagement.customenum.TransmissionEnum;
import xyz.dowob.filemanagement.customenum.UserLimiterEnum;
import xyz.dowob.filemanagement.data.api.ApiResponseDTO;
import xyz.dowob.filemanagement.data.file.dto.FileEditDTO;
import xyz.dowob.filemanagement.data.file.dto.FileMetadataDTO;
import xyz.dowob.filemanagement.data.file.dto.UploadChunkDTO;
import xyz.dowob.filemanagement.exception.LimitationException;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.service.serviceInterface.UserService;
import xyz.dowob.filemanagement.service.serviceInterface.ValidationService;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 文件的 API 控制器，用於處理文件相關的 API 請求
 * 用於處理文件的增刪改查操作
 *
 * @author yuan
 * @program FileManagement
 * @ClassName ApiFileUploadController
 * @description
 * @create 2024-09-30 16:00
 * @Version 1.0
 **/
@RestController
@RequestMapping("/api/files")
public class ApiFileController extends BaseFileController {
    /**
     * ObjectMapper 用於對象與 JSON 之間的轉換
     */
    private final ObjectMapper objectMapper;

    /**
     * 用戶限制策略
     */
    private final UserLimiterStrategy userLimiterStrategy;

    public ApiFileController(UserService userService, FileServiceStrategy fileServiceStrategy, UserLimiterStrategy userLimiterStrategy, ValidationService validationService, FileProperties fileProperties, ObjectMapper objectMapper) {
        super(userService, fileServiceStrategy, fileProperties, validationService);
        this.objectMapper = objectMapper;
        this.userLimiterStrategy = userLimiterStrategy;
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
        Mono<ResponseEntity<?>> action = userService
                .getUser(exchange)
                .switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.AUTHENTICATION_FAILED)))
                .flatMap(user -> {
                    UserLimiter userLimiter = userLimiterStrategy.getUserLimiter(UserLimiterEnum.USER_UPLOAD_LIMITER);
                    if (!userLimiter.tryAcquire(user.getId())) {
                        return Mono.error(new LimitationException(LimitationException.ErrorCode.USER_EXCEED_LIMIT,
                                                                  UserLimiterEnum.USER_UPLOAD_LIMITER.getError()
                        ));
                    }
                    return validationService
                            .validateFileMetadataDTO(fileMetadataDTO, user)
                            .then(fileServiceStrategy.getFileService().uploadFile(fileMetadataDTO, user).flatMap(transferResponseDTO -> {
                                ApiResponseDTO<?> apiResponse;
                                if (transferResponseDTO.getIsFinished()) {
                                    apiResponse = createResponse(exchange, "上傳成功", transferResponseDTO);
                                } else {
                                    apiResponse = createResponse(exchange, "建立任務成功", transferResponseDTO);
                                }
                                return createResponseEntity(apiResponse);
                            }))
                            .doFinally(signalType -> userLimiter.release(user.getId()));
                });
        return handleError(action, exchange);
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
            @PathVariable String id, ServerWebExchange exchange) {
        return userService.getUser(exchange).flatMap(user -> fileServiceStrategy.getFileService().downloadFile(id, user).map(userFileDataBO -> {
            HttpHeaders headers = new HttpHeaders();
            if ("download".equals(action)) {
                headers.add(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=" + URLEncoder.encode(userFileDataBO.getFilename(), StandardCharsets.UTF_8)
                );
                headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);
            } else {
                headers.add(HttpHeaders.CONTENT_TYPE, FileEnum.getMediaType(userFileDataBO.getFileType(), userFileDataBO.getFilename()));
            }
            headers.add(HttpHeaders.ACCEPT_RANGES, "bytes");
            headers.add(HttpHeaders.CONTENT_LENGTH, String.valueOf(userFileDataBO.getFileSize()));
            return ResponseEntity.ok().headers(headers).body(userFileDataBO.getDataStream());
        })).onErrorResume(ValidationException.class, e -> {
            String errorMessage = String.format("下載失敗: %s", e.getMessage());
            ApiResponseDTO<?> apiResponse = createResponse(exchange, e.getErrorCode().getCode(), errorMessage, null);
            try {
                objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
                return Mono.just(ResponseEntity
                                         .status(e.getErrorCode().getHttpStatus())
                                         .contentType(MediaType.APPLICATION_JSON)
                                         .body(Flux.just(exchange
                                                                 .getResponse()
                                                                 .bufferFactory()
                                                                 .wrap(objectMapper.writeValueAsString(apiResponse).getBytes()))));
            } catch (JsonProcessingException ex) {
                return Mono.error(new RuntimeException(ex));
            }
        });
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
        return handleError(userService.getUser(exchange).flatMap(user -> fileServiceStrategy.getFileService().deleteFile(id, user))
                                   .then(createResponseEntity(createResponse(exchange, "刪除成功", null))), exchange);
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
        return handleError(validationService
                                   .validateEditFileDTO(fileEditDTO, false)
                                   .then(validationService.validSpecifyColumns(fileEditDTO, "fileId"))
                                   .then(userService.getUser(exchange))
                                   .flatMap(user -> fileServiceStrategy.getFileService().editFile(fileEditDTO, user))
                                   .then(createResponseEntity(createResponse(exchange, "資料更新成功", null))), exchange);
    }


    /**
     * 上傳文件分塊的 API 請求，根據文件 ID 上傳文件分塊
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
        TransmissionEnum transmissionType = fileProperties.getTransmissionType();
        return handleChunkUpload(uploadChunkDTO, exchange);
        //todo 未來支持其他傳輸類型
    }

    /**
     * 此方法為分塊上船的處理方法
     *
     * @param uploadChunkDTO 上傳文件分塊的元數據
     * @param exchange       請求對象
     *
     * @return Mono<ResponseEntity < ?>> 返回上傳文件分塊的結果
     */
    private Mono<ResponseEntity<?>> handleChunkUpload(@RequestBody UploadChunkDTO uploadChunkDTO, ServerWebExchange exchange) {
        return handleError(fileServiceStrategy.getFileService(null).uploadFileChunk(uploadChunkDTO).flatMap(transferResponseDTO -> {
            ApiResponseDTO<?> apiResponse;
            if (transferResponseDTO.getIsSuccess()) {
                apiResponse = createResponse(exchange, "上傳成功", transferResponseDTO);
            } else {
                apiResponse = createResponse(exchange, HttpStatus.BAD_REQUEST.value(), "上傳失敗", transferResponseDTO);
            }
            return createResponseEntity(apiResponse);
        }), exchange);
    }


    // todo 暫不使用
    private Mono<ResponseEntity<?>> handleMultipartUpload(
            @RequestPart("transferTaskId") String transferTaskId, @RequestPart("file") Mono<Part> filePart, ServerWebExchange exchange) {
        return formatPartToBytes(filePart).flatMap(bytes -> {
            UploadChunkDTO uploadChunkDTO = new UploadChunkDTO(transferTaskId, 1, 1, bytes);
            return fileServiceStrategy.getFileService().uploadFileChunk(uploadChunkDTO);
        }).flatMap(transferResponseDTO -> {
            ApiResponseDTO<?> apiResponse;
            if (transferResponseDTO.getIsFinished()) {
                apiResponse = createResponse(exchange, "上傳成功", transferResponseDTO);
            } else {
                apiResponse = createResponse(exchange, "建立任務成功", transferResponseDTO);
            }
            return createResponseEntity(apiResponse);
        }).onErrorResume(ValidationException.class, e -> {
            String errorMessage = String.format("上傳失敗: %s", e.getMessage());
            int responseCode = e.getErrorCode().getCode();
            return createResponseEntity(createResponse(exchange, responseCode, errorMessage, null));
        });
    }

    private Mono<byte[]> formatPartToBytes(Mono<Part> multipartFile) {
        return multipartFile.flatMap(part -> part.content().reduce(DataBuffer::write)).map(dataBuffer -> {
            byte[] bytes = new byte[dataBuffer.readableByteCount()];
            dataBuffer.read(bytes);
            DataBufferUtils.release(dataBuffer);
            return bytes;
        });
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
        List<FileEnum> fileEnums = Optional.ofNullable(types).orElse(Collections.emptyList()).stream().map(type -> {
            try {
                return FileEnum.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }).filter(Objects::nonNull).toList();
        return super.getUserFileList(exchange, ReservedSearchIdEnum.ALL_FILE_ID.getId(), page, size, fileEnums);
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
}
