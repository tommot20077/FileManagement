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
import reactor.core.scheduler.Schedulers;
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
import xyz.dowob.filemanagement.data.file.bo.FileEditBO;
import xyz.dowob.filemanagement.data.file.dto.FileEditDTO;
import xyz.dowob.filemanagement.data.file.dto.FileMetadataDTO;
import xyz.dowob.filemanagement.data.file.dto.UploadChunkDTO;
import xyz.dowob.filemanagement.data.response.ApiResponseDTO;
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
 * 基於反應式編程的一般檔案控制器抽象基類，實現通用檔案的完整管理功能。
 * 
 * <p>此類繼承自 BaseFileController，專門處理一般檔案（非線上檔案）的相關操作，
 * 包括檔案上傳、下載、編輯、刪除等核心功能。支援多種傳輸方式（分塊上傳、Multipart上傳），
 * 並整合用戶限流機制確保系統穩定性。</p>
 * 
 * <p>提供靈活的檔案處理策略，支援預覽和下載模式，
 * 並透過權限驗證確保檔案操作的安全性。</p>
 * 
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@RecordLevel(LogLevelEnum.INFO)
public abstract class BaseGeneralFileController extends BaseFileController {

    /**
     * 是否強制使用伺服器設定的標誌。
     * 
     * <p>當此標誌為 true 時，系統將忽略客戶端的傳輸方式選擇，
     * 強制使用伺服器預設的傳輸設定。</p>
     */
    private final boolean isForceUseServerConfig;

    /**
     * 系統預設的檔案上傳傳輸類型。
     * 
     * <p>定義當客戶端未指定傳輸方式或伺服器強制使用設定時的預設傳輸方式。</p>
     */
    private final TransmissionEnum defaultUploadType;

    /**
     * 用戶限額策略管理器，實現使用者操作頻率和資源使用的控制機制。
     * 
     * <p>透過不同的限流策略保護系統資源，防止使用者濫用或超量使用。</p>
     */
    private final UserLimiterStrategy userLimiterStrategy;


    /**
     * 建構函式，初始化一般檔案控制器所需的各項服務和組件。
     *
     * @param userService               使用者服務，提供使用者相關的業務邏輯
     * @param fileServiceStrategy       檔案服務策略，用於選擇適當的檔案服務實現
     * @param fileProperties            檔案系統設定屬性
     * @param validationService         資料驗證服務
     * @param permissionService         使用者檔案權限服務
     * @param objectMapper              JSON 物件映射器
     * @param filePermissionRuleManager 檔案權限規則管理器
     * @param userLimiterStrategy       使用者限額策略管理器
     */
    public BaseGeneralFileController(UserService userService, FileServiceStrategy fileServiceStrategy, FileProperties fileProperties, ValidationService validationService, PermissionService<UserFileMetadata> permissionService, UserLimiterStrategy userLimiterStrategy, ObjectMapper objectMapper, FilePermissionRuleManager filePermissionRuleManager) {
        super(userService, fileServiceStrategy, fileProperties, validationService, permissionService, objectMapper, filePermissionRuleManager);

        this.isForceUseServerConfig = fileProperties.getUpload().isForceUseServerConfig();
        this.defaultUploadType = fileProperties.getUpload().getDefaultUploadType();
        this.userLimiterStrategy = userLimiterStrategy;
    }


    /**
     * 上傳檔案的請求，此步驟為預先上傳檔案的元資料檢驗檔案的步驟
     * 處理完成之後依照結果提供繼續上傳檔案分塊或是完成操作
     *
     * @param fileMetadataDTO 上傳檔案的元資料
     * @param exchange        請求對象
     *
     * @return Mono<ResponseEntity < ?>> 回傳上傳檔案的結果
     */
    public Mono<ResponseEntity<?>> uploadFile(FileMetadataDTO fileMetadataDTO, ServerWebExchange exchange) {
        Mono<ResponseEntity<?>> action = userService.getUser(exchange).flatMap(user -> {
            UserLimiter userLimiter = userLimiterStrategy.getUserLimiter(UserLimiterEnum.USER_UPLOAD_LIMITER);
            return userLimiter.tryAcquire(user.getId()).flatMap(acquire -> {
                if (!acquire) {
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
                                          .flatMapMany(map -> Flux.fromIterable(map.values()))
                                          .flatMap(fileMetadata -> validationService.validateFileType(fileMetadata, FileEnum.FOLDER)))
                        .then(fileServiceStrategy.getFileService().uploadFile(fileMetadataDTO, user).flatMap(transferResponseDTO -> {
                            ApiResponseDTO<?> apiResponse;
                            if (transferResponseDTO.getIsFinished()) {
                                apiResponse = createApiResponse(exchange, "上傳成功", transferResponseDTO);
                            } else {
                                apiResponse = createApiResponse(exchange, "建立任務成功", transferResponseDTO);
                            }
                            return createResponseEntity(apiResponse);
                        }))
                        .publishOn(Schedulers.boundedElastic())
                        .doFinally(signalType -> userLimiter.release(user.getId()).subscribe());
            });
        });
        return handleError(action, exchange);
    }


    /**
     * 下載檔案的請求，根據檔案 ID 下載檔案並提供預覽或是下載
     * 預設為預覽
     *
     * @param action   預覽或是下載
     * @param id       檔案 ID
     * @param exchange 請求對象
     *
     * @return Mono<ResponseEntity < Flux < DataBuffer>>> 回傳檔案流
     */
    public Mono<ResponseEntity<Flux<DataBuffer>>> downloadFile(String action, Long id, ServerWebExchange exchange) {
        DownloadActionEnum actionEnum = DownloadActionEnum.getType(action);

        return userService.getUser(exchange).flatMap(user -> {
            String rangeHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.RANGE);
            return processFileDownload(actionEnum, id, user, rangeHeader);
        });
    }

    /**
     * 處理檔案下載的方法
     *
     * @param action 預覽或是下載
     * @param id     檔案 ID
     * @param user   用戶對象
     *
     * @return Mono<ResponseEntity < Flux < DataBuffer>>> 回傳檔案流
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
     * 下載檔案並準備回傳結果
     *
     * @param file   檔案對象
     * @param user   用戶對象
     * @param action 下載類型
     *
     * @return Mono<ResponseEntity < Flux < DataBuffer>>> 回傳檔案流
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
     * 獲取檔案的信息，根據檔案 ID 獲取檔案的信息
     *
     * @param id       檔案 ID
     * @param exchange 請求對象
     *
     * @return Mono<ResponseEntity < ?>> 回傳檔案信息
     */
    public Mono<ResponseEntity<?>> getFileType(Long id, ServerWebExchange exchange) {
        Mono<ResponseEntity<?>> result = userService.getUser(exchange).flatMap(user -> {
            Collection<Permission<UserFileMetadata>> rules = FilePermissionRuleManager.DefaultRule.WITH_SHARED.getRules(filePermissionRuleManager);
            return permissionService.validateUserPermission(user, id, rules);
        }).flatMap(file -> validationService.validateFileType(file, CUSTOM_FILE_TYPE)).flatMap(file -> {
            return fileServiceStrategy.getFileService().getByServerFileMetadataId(file.getServerFileId()).flatMap(serverFileMetadata -> {
                String mediaType = FileEnum.getMediaType(serverFileMetadata.getFileType(), file.getFilename());
                long fileSize = serverFileMetadata.getFileSize();
                Map<String, Object> data = Map.of("X-File-Content-Type", mediaType, "X-File-Size", fileSize);
                ApiResponseDTO<?> apiResponse = createApiResponse(exchange, "獲取檔案類型成功", data);
                return createResponseEntity(apiResponse);
            });
        });
        return handleError(result, exchange);
    }


    /**
     * 刪除檔案的請求，根據檔案 ID 刪除檔案
     *
     * @param id       檔案 ID
     * @param exchange 請求對象
     *
     * @return Mono<ResponseEntity < ?>> 回傳刪除檔案的結果
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
                .then(createResponseEntity(createApiResponse(exchange, "刪除成功", null)));
        return handleError(result, exchange);
    }


    /**
     * 編輯檔案的請求，根據檔案 ID 編輯檔案
     *
     * @param fileEditDTO 檔案編輯的元資料
     * @param exchange    請求對象
     *
     * @return Mono<ResponseEntity < ?>> 回傳編輯檔案的結果
     */
    public Mono<ResponseEntity<?>> editFile(FileEditDTO fileEditDTO, ServerWebExchange exchange) {
        Mono<ResponseEntity<?>> result = validationService
                .validateEditFileDTO(fileEditDTO, false)
                .then(validationService.validSpecifyColumns(fileEditDTO, "fileId"))
                .then(userService.getUser(exchange))
                .flatMap(user -> {
                    Long fileId = Long.parseLong(fileEditDTO.getFileId());
                    List<Long> fileIds = new ArrayList<>(List.of(fileId));
                    if (fileEditDTO.getParentFolderId() != null) {
                        fileIds.add(fileEditDTO.getParentFolderId());
                    }

                    return permissionService.validateUserPermission(user, fileIds).flatMap(map -> {
                        FileEditBO fileEditBO = new FileEditBO(fileEditDTO);
                        fileEditBO.setParentFolderFileMetadata(map.get(fileEditDTO.getParentFolderId()));
                        fileEditBO.setUserFileMetadata(map.get(fileId));

                        return validationService
                                .validateFileType(fileEditBO.getUserFileMetadata(), CUSTOM_FILE_TYPE)
                                .then(validationService.validateFileType(fileEditBO.getParentFolderFileMetadata(), FileEnum.FOLDER))
                                .then(fileServiceStrategy.getFileService().editFile(fileEditBO, user));
                    });
                })
                .then(createResponseEntity(createApiResponse(exchange, "資料更新成功", null)));
        return handleError(result, exchange);
    }


    /**
     * 上傳檔案的請求，根據檔案 ID 上傳檔案分塊
     * 根據用戶的選擇的上傳方式以及伺服器的設定來決定上傳的方式
     * 並將任務 ID 以及檔案分塊的內容傳遞給具體的處理方法
     *
     * @param transmissionType 上傳方式
     * @param exchange         請求對象
     *
     * @return Mono<ResponseEntity < ?>> 回傳上傳檔案分塊的結果
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
     * 獲取用戶選擇的上傳方式，如果伺服器設定為強制使用伺服器設定則使用伺服器設定
     * 否則使用用戶選擇的上傳方式，如果用戶選擇的上傳方式為空則使用伺服器設定
     *
     * @param uploadType 上傳方式
     *
     * @return TransmissionEnum 回傳上傳方式
     */
    @SkipRecord
    protected TransmissionEnum getChooseTransmissionType(String uploadType) {
        if (isForceUseServerConfig) {
            return defaultUploadType;
        }
        return Objects.requireNonNullElse(TransmissionEnum.fromType(uploadType), defaultUploadType);
    }

    /**
     * 將請求的 Multipart 類型轉換獲取 檔案的 ID 以及檔案分塊
     *
     * @param exchange 請求對象
     *
     * @return Mono<Tuple2 < String, Mono < Part>>> 回傳 Multipart 類型
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
     * @param transferTaskId 檔案 ID
     * @param filePart       檔案分塊
     * @param exchange       請求對象
     *
     * @return Mono<ResponseEntity < ?>> 回傳上傳檔案分塊的結果
     */
    protected Mono<ResponseEntity<?>> handleMultipartUpload(String transferTaskId, Mono<Part> filePart, ServerWebExchange exchange) {
        return filePart.map(Part::content).flatMap(dataBufferFlux -> {
            UploadChunkDTO uploadChunkDTO = new UploadChunkDTO(transferTaskId, 1, 1, dataBufferFlux);
            return fileServiceStrategy.getFileService().uploadFileChunk(uploadChunkDTO);
        }).flatMap(transferResponseDTO -> {
            ApiResponseDTO<?> apiResponse;
            if (transferResponseDTO.getIsFinished()) {
                apiResponse = createApiResponse(exchange, "上傳成功", transferResponseDTO);
            } else {
                apiResponse = createApiResponse(exchange, "建立任務成功", transferResponseDTO);
            }
            return createResponseEntity(apiResponse);
        }).onErrorResume(ValidationException.class, e -> {
                             String errorMessage = String.format("上傳失敗: %s", e.getMessage());
                             int responseCode = e.getErrorCode().getCode();
                             return createResponseEntity(createApiResponse(exchange, responseCode, errorMessage, null));
                         }
        );
    }

    /**
     * 將請求的資料轉換為 UploadChunkDTO 類型
     *
     * @param exchange 請求對象
     *
     * @return Mono<UploadChunkDTO> 回傳 UploadChunkDTO 類型
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
     * @param uploadChunkDTO 上傳檔案分塊的元資料
     * @param exchange       請求對象
     *
     * @return Mono<ResponseEntity < ?>> 回傳上傳檔案分塊的結果
     */
    protected Mono<ResponseEntity<?>> handleChunkUpload(UploadChunkDTO uploadChunkDTO, ServerWebExchange exchange) {
        return handleError(fileServiceStrategy.getFileService().uploadFileChunk(uploadChunkDTO).flatMap(transferResponseDTO -> {
                               ApiResponseDTO<?> apiResponse;
                               if (transferResponseDTO.getIsSuccess()) {
                                   apiResponse = createApiResponse(exchange, "上傳成功", transferResponseDTO);
                               } else {
                                   apiResponse = createApiResponse(exchange, HttpStatus.BAD_REQUEST.value(), "上傳失敗", transferResponseDTO);
                               }
                               return createResponseEntity(apiResponse);
                           }), exchange
        );
    }

    /**
     * 獲取用戶檔案列表的請求
     *
     * @param exchange 請求對象
     *
     * @return Mono<ResponseEntity> 回傳用戶檔案列表
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
}

