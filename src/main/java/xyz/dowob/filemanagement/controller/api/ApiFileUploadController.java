package xyz.dowob.filemanagement.controller.api;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.Part;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.component.strategy.FileStrategy;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.dto.api.ApiResponseDTO;
import xyz.dowob.filemanagement.dto.file.FileMetadata;
import xyz.dowob.filemanagement.dto.file.UploadChunkDTO;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.service.ServiceInterface.UserService;
import xyz.dowob.filemanagement.service.ServiceInterface.ValidationService;
import xyz.dowob.filemanagement.unity.ResponseUnity;

/**
 * @author yuan
 * @program FileManagement
 * @ClassName ApiFileUploadController
 * @description
 * @create 2024-09-30 16:00
 * @Version 1.0
 **/
@RestController
@RequestMapping("/api/file/upload")
@RequiredArgsConstructor
public class ApiFileUploadController implements ResponseUnity {
    private final FileStrategy fileStrategy;

    private final ValidationService validationService;

    private final UserService userService;

    @PostMapping("/initialTask")
    public Mono<ResponseEntity<?>> uploadFile (@RequestBody FileMetadata fileMetadata, ServerWebExchange exchange) {
        return userService
                .getUser(exchange)
                .switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.AUTHENTICATION_FAILED)))
                .flatMap(user -> {
                    // todo Image硬編碼
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
                                          }));
                })
                .onErrorResume(ValidationException.class, e -> {
                    String errorMessage = String.format("建立上傳任務失敗: %s", e.getMessage());
                    int responseCode = e.getErrorCode().getCode();
                    return createResponseEntity(createResponse(exchange, responseCode, errorMessage, null));
                });
    }

    @PostMapping("/bufferUpload")
    public Mono<ResponseEntity<?>> bufferUpload (@RequestBody UploadChunkDTO uploadChunkDTO, ServerWebExchange exchange) {
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

    @PostMapping("/multipartUpload")
    public Mono<ResponseEntity<?>> multipartUpload (
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

    public Mono<byte[]> formatPartToBytes (Mono<Part> multipartFile) {
        return multipartFile.flatMap(part -> part.content().reduce(DataBuffer::write)).map(dataBuffer -> {
            byte[] bytes = new byte[dataBuffer.readableByteCount()];
            dataBuffer.read(bytes);
            DataBufferUtils.release(dataBuffer);
            return bytes;
        });
    }
}
