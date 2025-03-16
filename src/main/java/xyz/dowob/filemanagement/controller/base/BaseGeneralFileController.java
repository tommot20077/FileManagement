package xyz.dowob.filemanagement.controller.base;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.Part;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.component.limiter.UserLimiter;
import xyz.dowob.filemanagement.component.manager.FilePermissionRuleManager;
import xyz.dowob.filemanagement.component.strategy.FileServiceStrategy;
import xyz.dowob.filemanagement.component.strategy.UserLimiterStrategy;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.customenum.DownloadActionEnum;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.customenum.ReservedSearchIdEnum;
import xyz.dowob.filemanagement.customenum.UserLimiterEnum;
import xyz.dowob.filemanagement.data.api.ApiResponseDTO;
import xyz.dowob.filemanagement.data.file.bo.UserFileDataBO;
import xyz.dowob.filemanagement.data.file.dto.FileEditDTO;
import xyz.dowob.filemanagement.data.file.dto.FileMetadataDTO;
import xyz.dowob.filemanagement.data.file.dto.UploadChunkDTO;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.entity.UserFileMetadata;
import xyz.dowob.filemanagement.exception.LimitationException;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.functionInterface.Permission;
import xyz.dowob.filemanagement.service.serviceInterface.PermissionService;
import xyz.dowob.filemanagement.service.serviceInterface.UserService;
import xyz.dowob.filemanagement.service.serviceInterface.ValidationService;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 一般檔案控制器的基礎類，用於處理一般檔案的相關請求
 * 此類繼承自 {@link BaseFileController}，用於處理文件相關的請求
 *
 * @author yuan
 * @program FileManagement
 * @ClassName BaseGeneralFileController
 * @create 2025/1/14
 * @Version 1.0
 **/
public abstract class BaseGeneralFileController extends BaseFileController {

    public BaseGeneralFileController(UserService userService, FileServiceStrategy fileServiceStrategy, FileProperties fileProperties, ValidationService validationService, PermissionService<UserFileMetadata> permissionService, UserLimiterStrategy userLimiterStrategy, ObjectMapper objectMapper, FilePermissionRuleManager filePermissionRuleManager) {
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
     * 上傳文件的請求，此步驟為預先上傳文件的元數據檢驗檔案的步驟
     * 處理完成之後依照結果提供繼續上傳文件分塊或是完成操作
     *
     * @param fileMetadataDTO 上傳文件的元數據
     * @param exchange        請求對象
     *
     * @return Mono<ResponseEntity < ?>> 返回上傳文件的結果
     */
    public Mono<ResponseEntity<?>> uploadFile(FileMetadataDTO fileMetadataDTO, ServerWebExchange exchange) {
        Mono<ResponseEntity<?>> action = userService.getUser(exchange).flatMap(user -> {
            UserLimiter userLimiter = userLimiterStrategy.getUserLimiter(UserLimiterEnum.USER_UPLOAD_LIMITER);
            if (!userLimiter.tryAcquire(user.getId())) {
                return Mono.error(new LimitationException(LimitationException.ErrorCode.USER_EXCEED_LIMIT,
                                                          UserLimiterEnum.USER_UPLOAD_LIMITER.getError()
                ));
            }
            List<Long> fileIds = new ArrayList<>();
            if (fileMetadataDTO.getParentFolderId() != null) {
                fileIds.add(fileMetadataDTO.getParentFolderId());
            }

            return validationService
                    .validateFileMetadataDTO(fileMetadataDTO, user)
                    .thenMany(permissionService
                                      .validateUserPermission(user, fileIds)
                                      .flatMap(fileMetadata -> validationService.validateFileType(fileMetadata, FileEnum.FOLDER)))
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
     * 下載文件的請求，根據文件 ID 下載文件並提供預覽或是下載
     * 預設為預覽
     *
     * @param action   預覽或是下載
     * @param id       文件 ID
     * @param exchange 請求對象
     *
     * @return Mono<ResponseEntity < Flux < DataBuffer>>> 返回文件流
     */
    public Mono<ResponseEntity<Flux<DataBuffer>>> downloadFile(String action, Long id, ServerWebExchange exchange) {
        DownloadActionEnum actionEnum = DownloadActionEnum.getType(action);

        return userService.getUser(exchange).flatMap(user -> {
            String rangeHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.RANGE);
            return processFileDownload(actionEnum, id, user, rangeHeader);
        }).onErrorResume(ValidationException.class, e -> handleValidationError(e, exchange));
    }

    /**
     * 刪除文件的請求，根據文件 ID 刪除文件
     *
     * @param id       文件 ID
     * @param exchange 請求對象
     *
     * @return Mono<ResponseEntity < ?>> 返回刪除文件的結果
     */
    public Mono<ResponseEntity<?>> deleteFile(@PathVariable String id, ServerWebExchange exchange) {
        List<Permission<UserFileMetadata>> rules = new ArrayList<>(List.of(filePermissionRuleManager.getAllowOwner(),
                                                                           filePermissionRuleManager.getBlockNotSearchOperation()
        ));
        Mono<ResponseEntity<?>> result = userService
                .getUser(exchange)
                .flatMap(user -> permissionService
                        .validateUserPermission(user, Long.parseLong(id), rules)
                        .flatMap(file -> validationService
                                .validateFileType(file, CUSTOM_FILE_TYPE)
                                .then(fileServiceStrategy.getFileService().deleteFile(file, user))))
                .then(createResponseEntity(createResponse(exchange, "刪除成功", null)));
        return handleError(result, exchange);
    }

    /**
     * 編輯文件的請求，根據文件 ID 編輯文件
     *
     * @param fileEditDTO 文件編輯的元數據
     * @param exchange    請求對象
     *
     * @return Mono<ResponseEntity < ?>> 返回編輯文件的結果
     */
    public Mono<ResponseEntity<?>> editFile(FileEditDTO fileEditDTO, ServerWebExchange exchange) {
        Mono<ResponseEntity<?>> result = validationService
                .validateEditFileDTO(fileEditDTO, false)
                .then(validationService.validSpecifyColumns(fileEditDTO, "fileId"))
                .then(userService.getUser(exchange))
                .flatMap(user -> {
                    List<Long> fileIds = new ArrayList<>(List.of(Long.parseLong(fileEditDTO.getFileId())));
                    if (fileEditDTO.getParentFolderId() != null) {
                        fileIds.add(fileEditDTO.getParentFolderId());
                    }

                    return permissionService.validateUserPermission(user, fileIds).collectList().flatMap(fileList -> {
                        for (UserFileMetadata file : fileList) {
                            if (file.getId().equals(fileEditDTO.getParentFolderId())) {
                                fileEditDTO.setParentFolderFileMetadata(file);
                            } else if (file.getId().equals(Long.parseLong(fileEditDTO.getFileId()))) {
                                fileEditDTO.setUserFileMetadata(file);
                            }
                        }
                        return validationService
                                .validateFileType(fileEditDTO.getUserFileMetadata(), CUSTOM_FILE_TYPE)
                                .then(validationService.validateFileType(fileEditDTO.getParentFolderFileMetadata(), FileEnum.FOLDER))
                                .then(fileServiceStrategy.getFileService().editFile(fileEditDTO, user));
                    });
                })
                .then(createResponseEntity(createResponse(exchange, "資料更新成功", null)));
        return handleError(result, exchange);
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

    /**
     * 此方法為Multipart上傳的處理方法
     *
     * @param transferTaskId 文件 ID
     * @param filePart       文件分塊
     * @param exchange       請求對象
     *
     * @return Mono<ResponseEntity < ?>> 返回上傳文件分塊的結果
     */
    protected Mono<ResponseEntity<?>> handleMultipartUpload(String transferTaskId, Mono<Part> filePart, ServerWebExchange exchange) {
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

    /**
     * 獲取用戶文件列表的請求
     *
     * @param exchange 請求對象
     *
     * @return Mono<ResponseEntity> 返回用戶文件列表
     */
    public Mono<ResponseEntity<?>> getUserFileList(ServerWebExchange exchange, Integer page, Integer size, List<String> types) {
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
     * 將 Part 對象轉換為 byte[] 類型
     *
     * @param multipartFile Part 對象
     *
     * @return Mono<byte [ ]> 返回 byte[] 類型
     */
    private Mono<byte[]> formatPartToBytes(Mono<Part> multipartFile) {
        return multipartFile.flatMap(part -> part.content().reduce(DataBuffer::write)).map(dataBuffer -> {
            byte[] bytes = new byte[dataBuffer.readableByteCount()];
            dataBuffer.read(bytes);
            DataBufferUtils.release(dataBuffer);
            return bytes;
        });
    }


    /**
     * 處理文件下載的方法
     *
     * @param action 預覽或是下載
     * @param id     文件 ID
     * @param user   用戶對象
     *
     * @return Mono<ResponseEntity < Flux < DataBuffer>>> 返回文件流
     */
    private Mono<ResponseEntity<Flux<DataBuffer>>> processFileDownload(DownloadActionEnum action, Long id, User user, String rangeHeader) {
        return permissionService
                .validateUserPermission(user, id, FilePermissionRuleManager.DefaultRule.WITH_SHARED.getRules(filePermissionRuleManager))
                .flatMap(file -> validationService
                        .validateFileType(file, CUSTOM_FILE_TYPE)
                        .then(downloadAndPrepareResponse(file, user, action, rangeHeader)));
    }


    /**
     * 下載文件並準備返回結果
     *
     * @param file   文件對象
     * @param user   用戶對象
     * @param action 下載類型
     *
     * @return Mono<ResponseEntity < Flux < DataBuffer>>> 返回文件流
     */
    private Mono<ResponseEntity<Flux<DataBuffer>>> downloadAndPrepareResponse(UserFileMetadata file, User user, DownloadActionEnum action, String rangeHeader) {
        return fileServiceStrategy.getFileService().downloadFile(file, user, rangeHeader).map(userFileDataBO -> {
            HttpHeaders headers = prepareHttpHeaders(action, userFileDataBO, rangeHeader);
            HttpStatus status = rangeHeader != null ? HttpStatus.PARTIAL_CONTENT : HttpStatus.OK;
            return ResponseEntity.status(status).headers(headers).body(userFileDataBO.getDataStream());
        });
    }


    /**
     * 準備 Http 標頭
     *
     * @param action         預覽或是下載
     * @param userFileDataBO 文件數據對象
     *
     * @return HttpHeaders 返回 Http 標頭
     */
    private HttpHeaders prepareHttpHeaders(DownloadActionEnum action, UserFileDataBO userFileDataBO, String rangeHeader) {
        HttpHeaders headers = getHttpHeaders(userFileDataBO, rangeHeader);

        if (action.equals(DownloadActionEnum.DOWNLOAD)) {
            String encodedFilename = URLEncoder.encode(userFileDataBO.getFilename(), StandardCharsets.UTF_8);
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + encodedFilename);
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);
        } else {
            headers.add(HttpHeaders.CONTENT_TYPE, FileEnum.getMediaType(userFileDataBO.getFileType(), userFileDataBO.getFilename()));
        }
        String cacheControl = String.format("private, max-age=%d", fileProperties.getDownload().getDownloadCacheHeaderExpireTime());
        headers.add(HttpHeaders.CACHE_CONTROL, cacheControl);
        return headers;
    }

    /**
     * 獲取 Http 標頭
     *
     * @param userFileDataBO 文件數據對象
     * @param rangeHeader    範圍標頭
     *
     * @return HttpHeaders 返回 Http 標頭
     */
    private HttpHeaders getHttpHeaders(UserFileDataBO userFileDataBO, String rangeHeader) {
        HttpHeaders headers = new HttpHeaders();
        long fileSize = userFileDataBO.getFileSize();

        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            String[] ranges = rangeHeader.replace("bytes=", "").split("-");
            long start = Long.parseLong(ranges[0]);
            long end = ranges.length > 1 && !ranges[1].isEmpty() ? Long.parseLong(ranges[1]) : fileSize - 1;

            headers.set(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileSize);
            headers.set(HttpHeaders.CONTENT_LENGTH, String.valueOf(end - start + 1));
            headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
        } else {
            headers.set(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileSize));
            headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
        }
        return headers;
    }


    /**
     * 處理驗證異常，因為回傳格式不同，所以不可使用 {@link #handleError(Mono, ServerWebExchange)} 方法
     * 此方法用於處理文件下載的驗證異常
     *
     * @param e        驗證異常
     * @param exchange 請求對象
     *
     * @return Mono<ResponseEntity < Flux < DataBuffer>>> 返回文件流
     */
    private Mono<ResponseEntity<Flux<DataBuffer>>> handleValidationError(ValidationException e, ServerWebExchange exchange) {
        String errorMessage = String.format("下载失败: %s", e.getMessage());
        ApiResponseDTO<?> apiResponse = createResponse(exchange, e.getErrorCode().getCode(), errorMessage, null);

        try {
            objectMapper.registerModule(new JavaTimeModule());
            byte[] responseBytes = objectMapper.writeValueAsString(apiResponse).getBytes();
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(responseBytes);

            return Mono.just(ResponseEntity.status(e.getErrorCode().getHttpStatus()).contentType(MediaType.APPLICATION_JSON).body(Flux.just(buffer)));
        } catch (JsonProcessingException ex) {
            return Mono.error(new RuntimeException(ex));
        }
    }
}

