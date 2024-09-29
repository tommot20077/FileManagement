package xyz.dowob.filemanagement.controller.base;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.Part;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.component.strategy.FileStrategy;
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
 * @ClassName BaseFileUploadController
 * @description
 * @create 2024-09-30 16:00
 * @Version 1.0
 **/
@RestController
@RequestMapping("/api/file")
public class BaseFileUploadController extends BaseController {
//abstract
    private final FileStrategy fileStrategy;

    public BaseFileUploadController(UserService userService, FileStrategy fileStrategy) {
        super(userService);
        this.fileStrategy = fileStrategy;
    }

    @PostMapping("/initialUpload")
    public Mono<ResponseEntity<?>> upload(@RequestBody FileMetadata fileMetadata, ServerWebExchange exchange) {
        return super
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
            @RequestPart("transferTaskId") String transferTaskId,
            @RequestPart("chunkIndex") String chunkIndex,
            @RequestPart("totalChunks") String totalChunks,
            @RequestPart("chunkData") Mono<Part> chunkData,
            @RequestPart("md5") String md5,
            ServerWebExchange exchange) {
        return formatPartToBytes(chunkData).flatMap(bytes -> {
            UploadChunkDTO uploadChunkDTO = new UploadChunkDTO(transferTaskId,
                                                               Integer.parseInt(totalChunks),
                                                               Integer.parseInt(chunkIndex),
                                                               bytes,
                                                               md5);
            FileService fileService = fileStrategy.getFileService(FileEnum.IMAGE);
            return fileService.uploadFileChunk(uploadChunkDTO);
        }).flatMap(transferResponseDTO -> {
            ApiResponseDTO<?> apiResponse = createResponse(exchange, "上傳成功", transferResponseDTO);
            return createResponseEntity(apiResponse);
        }).onErrorResume(e -> {
            ApiResponseDTO<?> apiResponse = createResponse(exchange, "上傳失敗", null);
            return createResponseEntity(apiResponse);
        });
    }

    @PostMapping("/multipartUpload")
    public Mono<ResponseEntity<?>> multipartUpload(
            @RequestPart("transferTaskId") String transferTaskId,
            @RequestPart("md5") String md5,
            @RequestPart("file") Mono<Part> filePart,
            ServerWebExchange exchange) {
        return formatPartToBytes(filePart).flatMap(bytes -> {
            UploadChunkDTO uploadChunkDTO = new UploadChunkDTO(transferTaskId, 1, 1, bytes, md5);
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
