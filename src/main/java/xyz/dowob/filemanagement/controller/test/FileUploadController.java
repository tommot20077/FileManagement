package xyz.dowob.filemanagement.controller.test;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.Part;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.component.handler.FileUploadWebSocketHandler;
import xyz.dowob.filemanagement.component.strategy.FileStrategy;
import xyz.dowob.filemanagement.unity.ResponseUnity;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.dto.api.ApiResponseDTO;
import xyz.dowob.filemanagement.dto.file.FileMetadata;
import xyz.dowob.filemanagement.dto.file.UploadChunkDTO;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.service.ServiceInterface.FileService;
import xyz.dowob.filemanagement.service.ServiceInterface.UserService;

/**
 * @author yuan
 * @program FileManagement
 * @ClassName FileUploadController
 * @description
 * @create 2024-09-30 16:00
 * @Version 1.0
 **/
@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileUploadController implements ResponseUnity {
    private final FileStrategy fileStrategy;

    private final FileUploadWebSocketHandler webSocketHandler;

    private final UserService userService;

    @PostMapping("/initialUpload")
    public Mono<ResponseEntity<?>> upload(@RequestBody FileMetadata fileMetadata, ServerWebExchange exchange) {
        return userService
                .getUser(exchange)
                .switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.AUTHENTICATION_FAILED)))
                .flatMap(user -> {
                    FileService fileService = fileStrategy.getFileService(FileEnum.IMAGE);
                    return fileService.uploadFile(fileMetadata, user).flatMap(transferResponseDTO -> {
                        ApiResponseDTO<?> apiResponse;
                        if (transferResponseDTO.getIsFinished()) {
                            apiResponse = createResponse(exchange, "上傳成功", transferResponseDTO);
                        } else {
                            apiResponse = createResponse(exchange, "建立任務成功", transferResponseDTO);
                        }
                        return createResponseEntity(apiResponse);
                    });
                })
                .onErrorResume(ValidationException.class, e -> {
                    String errorMessage = String.format("建立上傳任務失敗: %s", e.getMessage());
                    int responseCode = e.getErrorCode().getCode();
                    return createResponseEntity(createResponse(exchange, responseCode, errorMessage, null));
                });
    }

    @PostMapping("/bufferUpload")
    public Mono<ResponseEntity<?>> bufferUpload(
            @RequestBody UploadChunkDTO uploadChunkDTO, ServerWebExchange exchange) {
        return Mono.just(uploadChunkDTO).flatMap(uploadChunk -> {
            FileService fileService = fileStrategy.getFileService(FileEnum.IMAGE);
            return fileService.uploadFileChunk(uploadChunkDTO);
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
    public Mono<ResponseEntity<?>> multipartUpload(
            @RequestPart("transferTaskId") String transferTaskId, @RequestPart("file") Mono<Part> filePart, ServerWebExchange exchange) {
        return formatPartToBytes(filePart).flatMap(bytes -> {
            UploadChunkDTO uploadChunkDTO = new UploadChunkDTO(transferTaskId, 1, 1, bytes);
            FileService fileService = fileStrategy.getFileService(FileEnum.IMAGE);
            return fileService.uploadFileChunk(uploadChunkDTO);
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

    public Mono<byte[]> formatPartToBytes(Mono<Part> multipartFile) {
        return multipartFile.flatMap(part -> part.content().reduce(DataBuffer::write)).map(dataBuffer -> {
            byte[] bytes = new byte[dataBuffer.readableByteCount()];
            dataBuffer.read(bytes);
            DataBufferUtils.release(dataBuffer);
            return bytes;
        });
    }
}
