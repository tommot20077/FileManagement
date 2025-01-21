package xyz.dowob.filemanagement.controller.api;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.Part;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.HideOverLength;
import xyz.dowob.filemanagement.component.limiter.UserLimiter;
import xyz.dowob.filemanagement.component.strategy.FileStrategy;
import xyz.dowob.filemanagement.component.strategy.UserLimiterStrategy;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.controller.base.BaseFileController;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.customenum.TransmissionEnum;
import xyz.dowob.filemanagement.customenum.UserLimiterEnum;
import xyz.dowob.filemanagement.dto.api.ApiResponseDTO;
import xyz.dowob.filemanagement.dto.file.FileMetadata;
import xyz.dowob.filemanagement.dto.file.UploadChunkDTO;
import xyz.dowob.filemanagement.exception.LimitationException;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.service.ServiceInterface.FileService;
import xyz.dowob.filemanagement.service.ServiceInterface.UserService;
import xyz.dowob.filemanagement.service.ServiceInterface.ValidationService;

/**
 * @author yuan
 * @program FileManagement
 * @ClassName ApiFileUploadController
 * @description
 * @create 2024-09-30 16:00
 * @Version 1.0
 **/
@RestController
@RequestMapping("/api/guest/file")
public class ApiFileUploadController extends BaseFileController {
    public ApiFileUploadController(FileService fileService, UserService userService, FileStrategy fileStrategy, UserLimiterStrategy userLimiterStrategy, ValidationService validationService, FileProperties fileProperties) {
        super(fileService, userService, fileStrategy, userLimiterStrategy, validationService, fileProperties);
    }

    @PostMapping("/upload/initialTask")
    public Mono<ResponseEntity<?>> uploadFile(@RequestBody FileMetadata fileMetadata, ServerWebExchange exchange) {
        return userService
                .getUser(exchange)
                .switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.AUTHENTICATION_FAILED)))
                // todo Image硬編碼
                .flatMap(user -> {
                    UserLimiter userLimiter = userLimiterStrategy.getUserLimiter(UserLimiterEnum.USER_UPLOAD_LIMITER);
                    if (!userLimiter.tryAcquire(user.getId())) {
                        return Mono.error(new LimitationException(LimitationException.ErrorCode.USER_EXCEED_LIMIT,
                                                                  UserLimiterEnum.USER_UPLOAD_LIMITER.getError()
                        ));
                    }
                    return validationService
                            .validateFileMetadataDTO(fileMetadata)
                            .then(fileStrategy
                                          .getFileService(FileEnum.IMAGE)
                                          .uploadFile(fileMetadata, user)
                                          .flatMap(transferResponseDTO -> {
                                              ApiResponseDTO<?> apiResponse;
                                              if (transferResponseDTO.getIsFinished()) {
                                                  apiResponse = createResponse(exchange, "上傳成功", transferResponseDTO);
                                              } else {
                                                  apiResponse = createResponse(exchange, "建立任務成功", transferResponseDTO);
                                              }
                                              return createResponseEntity(apiResponse);
                                          }))
                            .doFinally(signalType -> userLimiter.release(user.getId()));
                })
                .onErrorResume(ValidationException.class, e -> {
                    String errorMessage = String.format("建立上傳任務失敗: %s", e.getMessage());
                    int responseCode = e.getErrorCode().getCode();
                    return createResponseEntity(createResponse(exchange, responseCode, errorMessage, null));
                }).onErrorResume(LimitationException.class, e -> {
                    String errorMessage = String.format("建立上傳任務失敗: %s", e.getMessage());
                    int responseCode = e.getErrorCode().getCode();
                    return createResponseEntity(createResponse(exchange, responseCode, errorMessage, null), 429);
                });
    }

    @PostMapping("/upload/uploadFileData")
    public Mono<ResponseEntity<?>> uploadFile(ServerWebExchange exchange,
                                              @RequestPart(value = "transferTaskId", required = false) String transferTaskId,
                                              @RequestPart(value = "file", required = false) Mono<Part> filePart,
                                              @RequestBody(required = false) UploadChunkDTO uploadChunkDTO) {
        TransmissionEnum transmissionType = fileProperties.getTransmissionType();
        return handleChunkUpload(uploadChunkDTO, exchange);
        //todo 未來支持其他傳輸類型
    }

    private Mono<ResponseEntity<?>> handleChunkUpload(@RequestBody UploadChunkDTO uploadChunkDTO, ServerWebExchange exchange) {
        return Mono.just(uploadChunkDTO).flatMap(uploadChunk -> {
            // todo Image硬編碼
            return fileStrategy.getFileService(FileEnum.IMAGE).uploadFileChunk(uploadChunkDTO);
        }).flatMap(transferResponseDTO -> {
            ApiResponseDTO<?> apiResponse;
            if (transferResponseDTO.getIsSuccess()) {
                apiResponse = createResponse(exchange, "上傳成功", transferResponseDTO);
            } else {
                apiResponse = createResponse(exchange, 400, "上傳失敗", transferResponseDTO);
            }
            return createResponseEntity(apiResponse);
        });
    }

    // todo 暫不使用
    private Mono<ResponseEntity<?>> handleMultipartUpload(
            @RequestPart("transferTaskId") String transferTaskId, @RequestPart("file") Mono<Part> filePart, ServerWebExchange exchange) {
        return formatPartToBytes(filePart).flatMap(bytes -> {
            UploadChunkDTO uploadChunkDTO = new UploadChunkDTO(transferTaskId, 1, 1, bytes);
            // todo Image硬編碼
            return fileStrategy.getFileService(FileEnum.IMAGE).uploadFileChunk(uploadChunkDTO);
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

    /**
     * 獲取用戶文件列表的API請求
     *
     * @param exchange 請求對象
     *
     * @return Mono<ResponseEntity> 返回用戶文件列表
     */
    @HideOverLength
    @GetMapping("/getUserFileList")
    public Mono<ResponseEntity<?>> getUserFileList(ServerWebExchange exchange) {
        return super.getUserFileList(exchange);
    }

    @GetMapping("/download")
    public Mono<ResponseEntity<Flux<DataBuffer>>> downloadFile(ServerWebExchange exchange,
                                                               @RequestParam String fileId,
                                                               @RequestParam(value = "action", defaultValue = "download", required = false)
                                                               String action) {
        return userService.getUser(exchange).flatMap(user -> {
            // todo Image硬編碼
            Flux<DataBuffer> data = fileStrategy.getFileService(FileEnum.IMAGE).downloadFile(fileId, user);

            HttpHeaders headers = new HttpHeaders();
            if ("download".equals(action)) {
                headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileId);
                headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);
            } else {
                headers.add(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_JPEG_VALUE);
            }

            return Mono.just(ResponseEntity.ok().headers(headers).body(data));

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
}
