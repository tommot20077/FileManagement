package xyz.dowob.filemanagement.grpc.controller.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;
import xyz.dowob.filemanagement.component.strategy.FileServiceStrategy;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.data.file.bo.UserFileDataBO;
import xyz.dowob.filemanagement.data.file.dto.FileMetadataDTO;
import xyz.dowob.filemanagement.data.file.dto.UploadResponseDTO;
import xyz.dowob.filemanagement.entity.ServerFileMetadata;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.entity.UserFileMetadata;
import xyz.dowob.filemanagement.grpc.*;
import xyz.dowob.filemanagement.service.grpc.JwtCacheService;
import xyz.dowob.filemanagement.service.grpc.UserContextCacheService;
import xyz.dowob.filemanagement.service.serviceInterface.*;
import xyz.dowob.filemanagement.unity.LogUnity;

import java.util.Optional;

/**
 * gRPC 檔案控制器基礎類。
 *
 * <p>提供檔案相關的 gRPC 服務基礎實現，包括：
 * <ul>
 *   <li>檔案上傳（包含驗證）</li>
 *   <li>檔案下載</li>
 *   <li>檔案刪除</li>
 *   <li>檔案元資料查詢</li>
 * </ul>
 *
 * <p>特點：
 * <ul>
 *   <li>整合 ValidationService 進行輸入驗證</li>
 *   <li>支援大檔案分塊傳輸</li>
 *   <li>提供秒傳功能</li>
 *   <li>統一的錯誤處理</li>
 * </ul>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
public class BaseGrpcFileController extends BaseGrpcController {

    /**
     * 檔案服務策略管理器。
     */
    private final FileServiceStrategy fileServiceStrategy;

    /**
     * JSON 物件映射器。
     */
    private final ObjectMapper objectMapper;


    /**
     * 建構函數。
     *
     * @param validationService        驗證服務
     * @param userService              用戶服務
     * @param tokenService             令牌服務
     * @param permissionService        權限服務
     * @param jwtCacheService          JWT 快取服務
     * @param userContextCacheService  用戶上下文快取服務
     * @param fileServiceStrategy      檔案服務策略
     * @param objectMapper             JSON映射器
     */
    public BaseGrpcFileController(ValidationService validationService, 
                                  UserService userService, 
                                  TokenService tokenService, 
                                  PermissionService<UserFileMetadata> permissionService,
                                  JwtCacheService jwtCacheService,
                                  UserContextCacheService userContextCacheService,
                                  FileServiceStrategy fileServiceStrategy, 
                                  ObjectMapper objectMapper) {
        super(validationService, userService, tokenService, permissionService, jwtCacheService, userContextCacheService);
        this.fileServiceStrategy = fileServiceStrategy;
        this.objectMapper = objectMapper;
    }


    /**
     * 驗證用戶並執行檔案上傳。
     *
     * <p>此方法包含完整的驗證流程：
     * <ol>
     *   <li>驗證 JWT 令牌</li>
     *   <li>獲取用戶資訊</li>
     *   <li>驗證檔案元資料（檔案名稱、大小等）</li>
     *   <li>執行檔案上傳</li>
     * </ol>
     *
     * @param userId         用戶 ID
     * @param token          JWT 令牌
     * @param filename       檔案名稱
     * @param parentFolderId 上級目錄 ID
     * @param md5            檔案 MD5 值
     * @param content        檔案內容
     * @param mimeType       MIME 類型
     *
     * @return 上傳結果的 Mono
     */
    public Mono<UploadResponseDTO> validateAndUploadFile(Long userId, String token, String filename, Long parentFolderId, String md5, byte[] content, String mimeType) {
        return validateAndGetUser(token, userId).flatMap(user -> {
            FileMetadataDTO metadata = new FileMetadataDTO();
            metadata.setFilename(filename);
            metadata.setParentFolderId(parentFolderId);
            metadata.setMd5(md5);
            metadata.setFileSize((long) content.length);
            metadata.setUser(user);
            // mimeType 不是 FileMetadataDTO 的屬性，在上傳時單獨處理

            // 驗證檔案元資料
            return validationService.validateFileMetadataDTO(metadata, user).then(Mono.defer(() -> {
                FileService fileService = fileServiceStrategy.getFileService();
                return fileService.uploadFile(metadata, user);
            }));
        });
    }


    /**
     * 獲取檔案內容。
     *
     * @param request          檔案請求
     * @param responseObserver 響應觀察者
     */
    public void getFileContent(GetFileRequest request, StreamObserver<FileContentResponse> responseObserver) {
        User user = new User();
        user.setId(request.getAuth().getUserId() != 0 ? request.getAuth().getUserId() : null);

        Mono<UserFileDataBO> downloadMono = validateAndGetUser(request.getAuth())
                .flatMap(validatedUser -> permissionService.validateUserPermission(validatedUser, request.getFileId()))
                .flatMap(file -> {
                    FileService fileService = fileServiceStrategy.getFileService(file.getFileType());
                    return fileService.downloadFile(file, user);
                });

        downloadMono.subscribe(userFileDataBO -> handleFileContent(userFileDataBO, responseObserver),
                               error -> handleGrpcError(error, responseObserver)
        );
    }


    /**
     * 處理檔案內容輸出。
     */
    private void handleFileContent(UserFileDataBO userFileDataBO, StreamObserver<FileContentResponse> responseObserver) {
        // 處理線上文件內容
        if (userFileDataBO.getContent() != null) {
            try {
                String jsonContent = objectMapper.writeValueAsString(userFileDataBO.getContent());
                byte[] contentBytes = jsonContent.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                sendContentInChunks(contentBytes, responseObserver);
            } catch (Exception e) {
                LogUnity.error("序列化線上檔案內容失敗: %s", e, e.getMessage());
                responseObserver.onNext(FileContentResponse.newBuilder().setErrorMessage(e.getMessage()).build());
                responseObserver.onCompleted();
            }
            return;
        }

        // 處理二進位數據
        Mono<byte[]> dataBufferByte = userFileDataBO.getDataBufferByte();
        if (dataBufferByte != null) {
            dataBufferByte.subscribe(data -> {
                                         sendContentInChunks(data, responseObserver);
                                         responseObserver.onCompleted();
                                     }, error -> {
                                         LogUnity.error("讀取檔案資料失敗: %s", error, error.getMessage());
                                         responseObserver.onNext(FileContentResponse.newBuilder().setErrorMessage(error.getMessage()).build());
                                         responseObserver.onCompleted();
                                     }
            );
            return;
        }

        // 處理流式數據
        Flux<DataBuffer> dataBufferFlux = userFileDataBO.getDataBufferFlux();
        if (dataBufferFlux != null) {
            dataBufferFlux.subscribe(dataBuffer -> {
                                         try {
                                             byte[] chunk = new byte[dataBuffer.readableByteCount()];
                                             dataBuffer.read(chunk);
                                             responseObserver.onNext(FileContentResponse.newBuilder().setContent(ByteString.copyFrom(chunk)).setIsFinal(false).build());
                                         } finally {
                                             DataBufferUtils.release(dataBuffer);
                                         }
                                     }, error -> {
                                         LogUnity.error("處理資料流失敗: %s", error, error.getMessage());
                                         responseObserver.onNext(FileContentResponse.newBuilder().setErrorMessage(error.getMessage()).build());
                                         responseObserver.onCompleted();
                                     }, () -> {
                                         responseObserver.onNext(FileContentResponse.newBuilder().setContent(ByteString.EMPTY).setIsFinal(true).build());
                                         responseObserver.onCompleted();
                                     }
            );
            return;
        }

        // 無內容
        responseObserver.onNext(FileContentResponse.newBuilder().setContent(ByteString.EMPTY).setTotalSize(0L).setIsFinal(true).build());
        responseObserver.onCompleted();
    }


    /**
     * 分塊發送檔案內容。
     */
    private void sendContentInChunks(byte[] data, StreamObserver<FileContentResponse> responseObserver) {
        int chunkSize = 64 * 1024; // 64KB
        for (int i = 0; i < data.length; i += chunkSize) {
            int end = Math.min(i + chunkSize, data.length);
            byte[] chunk = new byte[end - i];
            System.arraycopy(data, i, chunk, 0, end - i);

            responseObserver.onNext(FileContentResponse
                                            .newBuilder()
                                            .setContent(ByteString.copyFrom(chunk))
                                            .setTotalSize(data.length)
                                            .setIsFinal(end >= data.length)
                                            .build());
        }
    }


    /**
     * 獲取檔案元資料。
     *
     * @param request          元資料請求
     * @param responseObserver 響應觀察者
     */
    public void getFileMetadata(GetFileMetadataRequest request, StreamObserver<FileMetadataResponse> responseObserver) {
        User user = new User();
        user.setId(request.getAuth().getUserId() != 0 ? request.getAuth().getUserId() : null);

        Mono<FileMetadataResponse> metadataMono = validateAndGetUser(request.getAuth())
                .flatMap(validatedUser -> permissionService.validateUserPermission(validatedUser, request.getFileId()))
                .flatMap(fileMetadata -> {
                    if (fileMetadata.getServerFileId() != null) {
                        FileService fileService = fileServiceStrategy.getFileService(fileMetadata.getFileType());
                        return fileService
                                .getByServerFileMetadataId(fileMetadata.getServerFileId())
                                .map(serverFileMetadata -> Tuples.of(Optional.of(serverFileMetadata), fileMetadata));
                    }
                    return Mono.just(Tuples.of(Optional.<ServerFileMetadata>empty(), fileMetadata));
                })
                .map(tuple2 -> {
                    Optional<ServerFileMetadata> serverFileMetadataOpt = tuple2.getT1();
                    UserFileMetadata userFileMetadata = tuple2.getT2();
                    ServerFileMetadata serverFileMetadata = serverFileMetadataOpt.orElse(null);

                    xyz.dowob.filemanagement.grpc.FileInfo fileInfo = convertToFileInfo(userFileMetadata, serverFileMetadata);
                    return FileMetadataResponse.newBuilder().setSuccess(true).setFileInfo(fileInfo).build();
                });

        subscribeWithGrpcHandler(metadataMono, responseObserver);
    }


    /**
     * 將 UserFileMetadata 轉換為 gRPC FileInfo。
     */
    private FileInfo convertToFileInfo(UserFileMetadata userFileMetadata, ServerFileMetadata serverFileMetadata) {
        long fileSize = serverFileMetadata != null ? serverFileMetadata.getFileSize() : 0L;
        String mimeType = serverFileMetadata != null ? serverFileMetadata.getMimeType() : "application/octet-stream";

        return FileInfo
                .newBuilder()
                .setId(userFileMetadata.getId())
                .setName(userFileMetadata.getFilename())
                .setSize(fileSize)
                .setIsDirectory(userFileMetadata.getFileType() == FileEnum.FOLDER)
                .setCreatedTime(userFileMetadata.getUploadTime() != null ? userFileMetadata
                        .getUploadTime()
                        .toInstant(java.time.ZoneOffset.UTC)
                        .toEpochMilli() : 0)
                .setModifiedTime(userFileMetadata.getLastAccessTime() != null ? userFileMetadata
                        .getLastAccessTime()
                        .toInstant(java.time.ZoneOffset.UTC)
                        .toEpochMilli() : 0)
                .setParentId(userFileMetadata.getParentFolderId() != null ? userFileMetadata.getParentFolderId() : 0)
                .setMimeType(mimeType)
                .build();
    }


    /**
     * 刪除檔案（軟刪除）。
     *
     * @param request          刪除請求
     * @param responseObserver 響應觀察者
     */
    public void deleteFile(DeleteFileRequest request, StreamObserver<DeleteFileResponse> responseObserver) {
        User user = new User();
        user.setId(request.getAuth().getUserId() != 0 ? request.getAuth().getUserId() : null);

        Mono<DeleteFileResponse> deleteMono = validateAndGetUser(request.getAuth())
                .flatMap(validatedUser -> permissionService.validateUserPermission(validatedUser, request.getFileId()))
                .flatMap(file -> {
                    FileService fileService = fileServiceStrategy.getFileService(file.getFileType());
                    return fileService.removeFile(file, user);
                })
                .map(result -> DeleteFileResponse.newBuilder().setSuccess(true).setMessage("已移動到回收站").build());

        subscribeWithGrpcHandler(deleteMono, responseObserver);
    }
}