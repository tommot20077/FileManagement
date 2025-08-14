package xyz.dowob.filemanagement.grpc.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;
import xyz.dowob.filemanagement.component.strategy.FileServiceStrategy;
import xyz.dowob.filemanagement.customenum.TokenEnum;
import xyz.dowob.filemanagement.data.user.dto.AuthRequestDTO;
import xyz.dowob.filemanagement.entity.UserFileMetadata;
import xyz.dowob.filemanagement.grpc.*;
import xyz.dowob.filemanagement.grpc.controller.base.BaseGrpcFileController;
import xyz.dowob.filemanagement.grpc.controller.base.BaseGrpcFolderController;
import xyz.dowob.filemanagement.service.grpc.JwtCacheService;
import xyz.dowob.filemanagement.service.grpc.UserContextCacheService;
import xyz.dowob.filemanagement.service.serviceInterface.PermissionService;
import xyz.dowob.filemanagement.service.serviceInterface.TokenService;
import xyz.dowob.filemanagement.service.serviceInterface.UserService;
import xyz.dowob.filemanagement.service.serviceInterface.ValidationService;
import xyz.dowob.filemanagement.unity.LogUnity;

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * gRPC 檔案處理控制器。
 *
 * <p>此控制器是 gRPC 檔案處理服務的主要入口點，負責：
 * <ul>
 *   <li>接收和處理 gRPC 請求</li>
 *   <li>協調各個基礎控制器的功能</li>
 *   <li>處理流式上傳和下載</li>
 *   <li>管理用戶認證</li>
 * </ul>
 *
 * <p>架構特點：
 * <ul>
 *   <li>職責分離：將業務邏輯委託給基礎控制器</li>
 *   <li>統一驗證：所有輸入都經過 ValidationService</li>
 *   <li>錯誤處理：統一的異常處理機制</li>
 *   <li>高效能：支援流式處理和非阻塞 I/O</li>
 * </ul>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@GrpcService
public class GrpcFileController extends FileProcessingServiceGrpc.FileProcessingServiceImplBase {

    private final BaseGrpcFileController fileController;

    private final BaseGrpcFolderController folderController;

    private final UserService userService;

    private final TokenService tokenService;


    @Autowired
    public GrpcFileController(ValidationService validationService, 
                             UserService userService, 
                             TokenService tokenService, 
                             PermissionService<UserFileMetadata> permissionService, 
                             JwtCacheService jwtCacheService,
                             UserContextCacheService userContextCacheService,
                             FileServiceStrategy fileServiceStrategy, 
                             ObjectMapper objectMapper) {
        this.userService = userService;
        this.tokenService = tokenService;

        this.fileController = new BaseGrpcFileController(validationService,
                                                         userService,
                                                         tokenService,
                                                         permissionService,
                                                         jwtCacheService,
                                                         userContextCacheService,
                                                         fileServiceStrategy,
                                                         objectMapper
        );

        this.folderController = new BaseGrpcFolderController(validationService, 
                                                           userService, 
                                                           tokenService, 
                                                           permissionService, 
                                                           jwtCacheService,
                                                           userContextCacheService,
                                                           fileServiceStrategy);
    }


    /**
     * 流式檔案上傳。
     *
     * <p>支援大檔案分塊上傳，包含 MD5 校驗和秒傳功能。
     */
    @Override
    public StreamObserver<FileUploadChunk> uploadFile(StreamObserver<UploadProgress> responseObserver) {
        return new StreamObserver<>() {
            private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            private final AtomicLong totalReceived = new AtomicLong(0);

            private FileUploadMetadata metadata;

            private MessageDigest md5Digest;

            {
                try {
                    md5Digest = MessageDigest.getInstance("MD5");
                } catch (NoSuchAlgorithmException e) {
                    responseObserver.onError(e);
                }
            }

            @Override
            public void onNext(FileUploadChunk chunk) {
                try {
                    if (chunk.hasMetadata()) {
                        metadata = chunk.getMetadata();
                        LogUnity.debug("收到上傳元資料 - 檔案: %s, 大小: %d", metadata.getFilename(), metadata.getTotalSize());

                        responseObserver.onNext(UploadProgress.newBuilder().setPercentage(0.0).setMessage("開始上傳").setIsCompleted(false).build());

                    } else if (chunk.hasChunkData()) {
                        byte[] data = chunk.getChunkData().toByteArray();
                        buffer.write(data);
                        md5Digest.update(data);

                        long received = totalReceived.addAndGet(data.length);
                        double percentage = metadata != null && metadata.getTotalSize() > 0 ? (double) received / metadata.getTotalSize() * 100 : 0;

                        responseObserver.onNext(UploadProgress
                                                        .newBuilder()
                                                        .setPercentage(percentage)
                                                        .setMessage(String.format("已上傳 %.2f%%", percentage))
                                                        .setIsCompleted(false)
                                                        .build());

                    } else if (chunk.hasChecksum()) {
                        handleCompleteUpload(chunk.getChecksum(), responseObserver);
                    }
                } catch (Exception e) {
                    LogUnity.error("處理上傳 chunk 時發生錯誤: %s", e, e.getMessage());
                    responseObserver.onError(e);
                }
            }


            @Override
            public void onError(Throwable t) {
                LogUnity.error("上傳流錯誤: %s", t, t.getMessage());
                responseObserver.onNext(UploadProgress
                                                .newBuilder()
                                                .setPercentage(0)
                                                .setMessage("上傳失敗")
                                                .setErrorMessage(t.getMessage())
                                                .setIsCompleted(true)
                                                .build());
                responseObserver.onCompleted();
            }


            @Override
            public void onCompleted() {
                if (metadata != null && buffer.size() > 0) {
                    String calculatedMd5 = xyz.dowob.filemanagement.grpc.controller.base.BaseGrpcController.bytesToHex(md5Digest.digest());
                    UploadChecksum checksum = UploadChecksum.newBuilder().setMd5(calculatedMd5).setTotalChunks(1).build();
                    handleCompleteUpload(checksum, responseObserver);
                }
                responseObserver.onCompleted();
            }


            private void handleCompleteUpload(UploadChecksum checksum, StreamObserver<UploadProgress> observer) {
                if (metadata == null) {
                    observer.onNext(UploadProgress
                                            .newBuilder()
                                            .setPercentage(0)
                                            .setMessage("上傳失敗：缺少元資料")
                                            .setErrorMessage("Missing metadata")
                                            .setIsCompleted(true)
                                            .build());
                    return;
                }

                // 使用基礎控制器的驗證和上傳方法
                fileController
                        .validateAndUploadFile(metadata.getAuth().getUserId() != 0 ? metadata.getAuth().getUserId() : null,
                                               metadata.getAuth().getJwtToken(),
                                               metadata.getFilename(),
                                               metadata.getParentFolderId(),
                                               checksum.getMd5(),
                                               buffer.toByteArray(),
                                               metadata.getMimeType()
                        )
                        .subscribe(response -> {
                                       observer.onNext(UploadProgress
                                                               .newBuilder()
                                                               .setPercentage(100.0)
                                                               .setMessage(response.getIsSuccess() ? "上傳成功" : "上傳失敗")
                                                               .setIsCompleted(true)
                                                               .setIsInstantUpload(response.getIsFinished() && response.getProgress() == 100.0)
                                                               .setFileId(response.getFileId() != null ? response.getFileId() : 0)
                                                               .build());
                                   }, error -> {
                                       LogUnity.error("上傳失敗: %s", error, error.getMessage());
                                       observer.onNext(UploadProgress
                                                               .newBuilder()
                                                               .setPercentage(0)
                                                               .setMessage("上傳失敗")
                                                               .setErrorMessage(error.getMessage())
                                                               .setIsCompleted(true)
                                                               .build());
                                   }
                        );
            }
        };
    }


    /**
     * 簡單檔案上傳（小檔案）。
     */
    @Override
    public void uploadFileSimple(UploadFileRequest request, StreamObserver<UploadFileResponse> responseObserver) {
        Mono<UploadFileResponse> uploadMono = fileController
                .validateAndUploadFile(request.getAuth().getUserId() != 0 ? request.getAuth().getUserId() : null,
                                       request.getAuth().getJwtToken(),
                                       request.getFilename(),
                                       request.getParentFolderId(),
                                       request.getMd5(),
                                       request.getContent().toByteArray(),
                                       request.getMimeType()
                )
                .map(response -> UploadFileResponse
                        .newBuilder()
                        .setSuccess(response.getIsSuccess())
                        .setFileId(response.getFileId() != null ? response.getFileId() : 0)
                        .setIsInstantUpload(response.getIsFinished() && response.getProgress() == 100.0)
                        .setMessage(response.getMessage())
                        .build());

        fileController.subscribeWithGrpcHandler(uploadMono, responseObserver);
    }


    /**
     * 獲取檔案內容。
     */
    @Override
    public void getFileContent(GetFileRequest request, StreamObserver<FileContentResponse> responseObserver) {
        fileController.getFileContent(request, responseObserver);
    }


    /**
     * 獲取檔案元資料。
     */
    @Override
    public void getFileMetadata(GetFileMetadataRequest request, StreamObserver<FileMetadataResponse> responseObserver) {
        fileController.getFileMetadata(request, responseObserver);
    }


    /**
     * 刪除檔案。
     */
    @Override
    public void deleteFile(DeleteFileRequest request, StreamObserver<DeleteFileResponse> responseObserver) {
        fileController.deleteFile(request, responseObserver);
    }


    /**
     * 移動檔案。
     */
    @Override
    public void moveFile(MoveFileRequest request, StreamObserver<MoveFileResponse> responseObserver) {
        folderController.moveFile(request, responseObserver);
    }


    /**
     * 複製檔案。
     */
    @Override
    public void copyFile(CopyFileRequest request, StreamObserver<CopyFileResponse> responseObserver) {
        folderController.copyFile(request, responseObserver);
    }


    /**
     * 列出資料夾內容。
     */
    @Override
    public void listFolder(ListFolderRequest request, StreamObserver<ListFolderResponse> responseObserver) {
        folderController.listFolder(request, responseObserver);
    }


    /**
     * 創建資料夾。
     */
    @Override
    public void createFolder(CreateFolderRequest request, StreamObserver<CreateFolderResponse> responseObserver) {
        folderController.createFolder(request, responseObserver);
    }


    /**
     * 用戶認證。
     */
    @Override
    public void authenticateUser(AuthenticationRequest request, StreamObserver<AuthenticationResponse> responseObserver) {
        AuthRequestDTO authRequest = AuthRequestDTO.builder().username(request.getUsername()).password(request.getPassword()).build();

        Mono<AuthenticationResponse> authMono = userService
                .login(authRequest, null)
                .flatMap(jwtToken -> tokenService
                        .extractUserInfoFromToken(jwtToken, TokenEnum.JWT_AUTHORIZATION_TOKEN)
                        .map(userInfo -> Tuples.of(jwtToken, userInfo)))
                .map(tokenAndUserInfo -> {
                    String jwtToken = tokenAndUserInfo.getT1();
                    var userInfo = tokenAndUserInfo.getT2();

                    return AuthenticationResponse
                            .newBuilder()
                            .setSuccess(true)
                            .setJwtToken(jwtToken)
                            .setUserId(userInfo.getUserId())
                            .setUsername(userInfo.getUsername() != null ? userInfo.getUsername() : "")
                            .setRole(userInfo.getRole() != null ? userInfo.getRole() : "")
                            .build();
                });

        fileController.subscribeWithGrpcHandler(authMono, responseObserver);
    }

    /**
     * 驗證 JWT 令牌的有效性和撤銷狀態。
     */
    @Override
    public void validateToken(TokenValidationRequest request, StreamObserver<TokenValidationResponse> responseObserver) {
        String jwtToken = request.getJwtToken();
        Long expectedUserId = request.getUserId() != 0 ? request.getUserId() : null;

        if (jwtToken == null || jwtToken.trim().isEmpty()) {
            responseObserver.onNext(TokenValidationResponse.newBuilder()
                    .setSuccess(false)
                    .setIsValid(false)
                    .setIsRevoked(false)
                    .setErrorMessage("JWT token is required")
                    .build());
            responseObserver.onCompleted();
            return;
        }

        Mono<TokenValidationResponse> validationMono = tokenService
                .validateToken(jwtToken, expectedUserId, TokenEnum.JWT_AUTHORIZATION_TOKEN)
                .flatMap(userId -> 
                    // 如果驗證成功，獲取用戶詳細信息
                    tokenService.extractUserInfoFromToken(jwtToken, TokenEnum.JWT_AUTHORIZATION_TOKEN)
                        .map(userInfo -> TokenValidationResponse.newBuilder()
                                .setSuccess(true)
                                .setIsValid(true)
                                .setIsRevoked(false)
                                .setUserId(userInfo.getUserId())
                                .setUsername(userInfo.getUsername() != null ? userInfo.getUsername() : "")
                                .setRole(userInfo.getRole() != null ? userInfo.getRole() : "")
                                .setExpiresAt(userInfo.getTokenExpiry() != null ? userInfo.getTokenExpiry().getTime() : 0L)
                                .build()))
                .onErrorResume(error -> {
                    LogUnity.warn("Token validation failed: " + error.getMessage());
                    
                    // 判斷是撤銷還是其他錯誤
                    boolean isRevoked = error.getMessage().contains("revoked") || 
                                      error.getMessage().contains("blacklisted");
                    
                    return Mono.just(TokenValidationResponse.newBuilder()
                            .setSuccess(true)  // 成功執行了驗證，但令牌無效
                            .setIsValid(false)
                            .setIsRevoked(isRevoked)
                            .setErrorMessage(error.getMessage())
                            .build());
                });

        fileController.subscribeWithGrpcHandler(validationMono, responseObserver);
    }

    // 注意：以下方法暫未實現，需要根據實際需求添加

    @Override
    public void checkPermission(PermissionRequest request, StreamObserver<PermissionResponse> responseObserver) {
        // TODO: 實現權限檢查
        responseObserver.onNext(PermissionResponse.newBuilder().setHasPermission(false).setMessage("功能尚未實現").build());
        responseObserver.onCompleted();
    }


    @Override
    public void lockFile(LockFileRequest request, StreamObserver<LockFileResponse> responseObserver) {
        // TODO: 實現檔案鎖定
        responseObserver.onNext(LockFileResponse.newBuilder().setSuccess(false).setErrorMessage("功能尚未實現").build());
        responseObserver.onCompleted();
    }


    @Override
    public void unlockFile(UnlockFileRequest request, StreamObserver<UnlockFileResponse> responseObserver) {
        // TODO: 實現檔案解鎖
        responseObserver.onNext(UnlockFileResponse.newBuilder().setSuccess(false).setErrorMessage("功能尚未實現").build());
        responseObserver.onCompleted();
    }


    @Override
    public void findFiles(FindFilesRequest request, StreamObserver<FindFilesResponse> responseObserver) {
        // TODO: 實現檔案搜尋
        responseObserver.onNext(FindFilesResponse.newBuilder().setSuccess(false).setErrorMessage("功能尚未實現").build());
        responseObserver.onCompleted();
    }
}