package xyz.dowob.filemanagement.controller.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FormFieldPart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import xyz.dowob.filemanagement.annotation.RecordLevel;
import xyz.dowob.filemanagement.annotation.SkipRecord;
import xyz.dowob.filemanagement.component.limiter.UserLimiter;
import xyz.dowob.filemanagement.component.manager.FilePermissionRuleManager;
import xyz.dowob.filemanagement.component.strategy.FileServiceStrategy;
import xyz.dowob.filemanagement.component.strategy.UserLimiterStrategy;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.customenum.*;
import xyz.dowob.filemanagement.data.api.ApiResponseDTO;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
@RecordLevel(LogLevelEnum.INFO)
public abstract class BaseGeneralFileController extends BaseFileController {

    /**
     * 是否強制使用伺服器配置
     */
    private final boolean isForceUseServerConfig;

    /**
     * 預設的上傳類型
     */
    private final TransmissionEnum defaultUploadType;


    /**
     * 構造函數，初始化基本的業務層服務
     *
     * @param userService         用戶服務層對象
     * @param fileServiceStrategy 文件服務策略對象，用於選擇適當的文件服務
     * @param fileProperties      文件屬性設置
     * @param validationService   驗證服務對象
     * @param permissionService   用戶文件元數據授權服務
     * @param userLimiterStrategy 用戶限制策略
     * @param objectMapper        用於處理對象映射的工具
     */
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
        this.isForceUseServerConfig = fileProperties.getUpload().isForceUseServerConfig();
        this.defaultUploadType = fileProperties.getUpload().getDefaultUploadType();
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
    @SkipRecord
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
    @SkipRecord
    private Mono<ResponseEntity<Flux<DataBuffer>>> downloadAndPrepareResponse(UserFileMetadata file, User user, DownloadActionEnum action, String rangeHeader) {
        return fileServiceStrategy.getFileService().downloadFile(file, user, rangeHeader).map(userFileDataBO -> {
            HttpHeaders headers = prepareHttpHeaders(action, userFileDataBO, rangeHeader, true);
            HttpStatus status = rangeHeader != null ? HttpStatus.PARTIAL_CONTENT : HttpStatus.OK;
            return ResponseEntity.status(status).headers(headers).body(userFileDataBO.getDataBufferFlux());
        });
    }


    /**
     * 獲取文件的信息，根據文件 ID 獲取文件的信息
     *
     * @param id       文件 ID
     * @param exchange 請求對象
     *
     * @return Mono<ResponseEntity < ?>> 返回文件信息
     */
    public Mono<ResponseEntity<?>> getFileType(Long id, ServerWebExchange exchange) {
        Mono<ResponseEntity<?>> result = userService
                .getUser(exchange)
                .flatMap(user -> permissionService
                        .validateUserPermission(user, id, FilePermissionRuleManager.DefaultRule.WITH_SHARED.getRules(filePermissionRuleManager))
                        .flatMap(file -> validationService.validateFileType(file, CUSTOM_FILE_TYPE))
                        .flatMap(file -> fileServiceStrategy
                                .getFileService()
                                .getByServerFileMetadataId(file.getServerFileId())
                                .flatMap(serverFileMetadata -> {
                                    String fileType = FileEnum.getMediaType(serverFileMetadata.getFileType(), file.getFilename());
                                    long fileSize = serverFileMetadata.getFileSize();
                                    ApiResponseDTO<?> apiResponse = createResponse(exchange,
                                                                                   "獲取文件類型成功",
                                                                                   Map.of("X-File-Content-Type", fileType, "X-File-Size", fileSize)
                                    );
                                    return createResponseEntity(apiResponse);
                                })));

        return handleError(result, exchange);
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
     * 上傳文件的請求，根據文件 ID 上傳文件分塊
     * 根據用戶的選擇的上傳方式以及伺服器的配置來決定上傳的方式
     * 並將任務 ID 以及文件分塊的內容傳遞給具體的處理方法
     *
     * @param transmissionType 上傳方式
     * @param exchange         請求對象
     *
     * @return Mono<ResponseEntity < ?>> 返回上傳文件分塊的結果
     */
    protected Mono<ResponseEntity<?>> uploadFileData(String transmissionType, ServerWebExchange exchange) {
        TransmissionEnum transmissionEnum = getChooseTransmissionType(transmissionType);
        switch (transmissionEnum) {
            case MULTIPART:
                return formatMultipartData(exchange).flatMap(tuple2 -> handleMultipartUpload(tuple2.getT1(), tuple2.getT2(), exchange));
            case CHUNK:
                return formatChunkData(exchange).flatMap(uploadChunkDTO -> handleChunkUpload(uploadChunkDTO, exchange));
        }
        return Mono.error(new ValidationException(ValidationException.ErrorCode.REQUEST_IS_INVALID, "type"));
    }


    /**
     * 將請求的數據轉換為 UploadChunkDTO 類型
     *
     * @param exchange 請求對象
     *
     * @return Mono<UploadChunkDTO> 返回 UploadChunkDTO 類型
     */
    protected Mono<UploadChunkDTO> formatChunkData(ServerWebExchange exchange) {
        return exchange.getRequest().getBody().collectList().flatMap(dataBuffers -> {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            dataBuffers.forEach(buffer -> {
                byte[] bytes = new byte[buffer.readableByteCount()];
                buffer.read(bytes);
                DataBufferUtils.release(buffer);
                baos.write(bytes, 0, bytes.length);
            });

            ObjectMapper objectMapper = new ObjectMapper();
            try {
                return Mono.just(objectMapper.readValue(baos.toByteArray(), UploadChunkDTO.class));
            } catch (IOException e) {
                return Mono.error(new ValidationException(ValidationException.ErrorCode.INVALID_JSON_CONTENT));
            }
        });
    }

    
    /**
     * 此方法為分塊上傳的處理方法
     *
     * @param uploadChunkDTO 上傳文件分塊的元數據
     * @param exchange       請求對象
     *
     * @return Mono<ResponseEntity < ?>> 返回上傳文件分塊的結果
     */
    protected Mono<ResponseEntity<?>> handleChunkUpload(UploadChunkDTO uploadChunkDTO, ServerWebExchange exchange) {
        return handleError(fileServiceStrategy.getFileService().uploadFileChunk(uploadChunkDTO).flatMap(transferResponseDTO -> {
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
     * 將請求的 Multipart 類型轉換獲取 文件的 ID 以及文件分塊
     *
     * @param exchange 請求對象
     *
     * @return Mono<Tuple2 < String, Mono < Part>>> 返回 Multipart 類型
     */
    protected Mono<Tuple2<String, Mono<Part>>> formatMultipartData(ServerWebExchange exchange) {
        return exchange.getMultipartData().flatMap(multipartData -> {
            Map<String, Part> singleValueMap = multipartData.toSingleValueMap();
            Part filePart = singleValueMap.get("file");
            Part transferTaskIdPart = singleValueMap.get("transferTaskId");
            if (filePart == null) {
                return Mono.error(new ValidationException(ValidationException.ErrorCode.REQUEST_IS_INVALID, "file"));
            }

            String transferTaskId;
            if (transferTaskIdPart instanceof FormFieldPart transferTaskPart) {
                transferTaskId = transferTaskPart.value();
            } else {
                return Mono.error(new ValidationException(ValidationException.ErrorCode.REQUEST_IS_INVALID, "transferTaskId"));
            }
            return Mono.just(Tuples.of(transferTaskId, Mono.just(filePart)));
        });
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
        return filePart.map(Part::content).flatMap(dataBufferFlux -> {
            UploadChunkDTO uploadChunkDTO = new UploadChunkDTO(transferTaskId, 1, 1, dataBufferFlux);
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
     * 獲取用戶選擇的上傳方式，如果伺服器配置為強制使用伺服器配置則使用伺服器配置
     * 否則使用用戶選擇的上傳方式，如果用戶選擇的上傳方式為空則使用伺服器配置
     *
     * @param uploadType 上傳方式
     *
     * @return TransmissionEnum 返回上傳方式
     */
    @SkipRecord
    protected TransmissionEnum getChooseTransmissionType(String uploadType) {
        if (isForceUseServerConfig) {
            return defaultUploadType;
        }
        return Objects.requireNonNullElse(TransmissionEnum.fromType(uploadType), defaultUploadType);
    }
}

