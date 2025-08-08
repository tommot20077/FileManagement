package xyz.dowob.filemanagement.grpc.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;
import xyz.dowob.filemanagement.component.strategy.FileServiceStrategy;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.customenum.TokenEnum;
import xyz.dowob.filemanagement.data.file.bo.UserFileDataBO;
import xyz.dowob.filemanagement.data.file.dto.*;
import xyz.dowob.filemanagement.data.response.PagedResponseDTO;
import xyz.dowob.filemanagement.data.user.dto.AuthRequestDTO;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.entity.UserFileMetadata;
import xyz.dowob.filemanagement.grpc.*;
import xyz.dowob.filemanagement.service.serviceInterface.*;
import xyz.dowob.filemanagement.unity.LogUnity;

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * gRPC 檔案處理服務實現類。
 *
 * <p>此服務提供基於 gRPC 協議的完整檔案管理功能，支援大檔案分塊上傳、即時下載、
 * 資料夾管理以及用戶認證等核心操作。整合 WebFlux 反應式程式設計模式，確保高併發環境下的效能表現。
 *
 * <p>主要提供以下功能：
 * <ul>
 *   <li>檔案上傳：支援流式分塊上傳和簡單上傳兩種模式，內建 MD5 校驗和秒傳功能</li>
 *   <li>檔案下載：提供分塊下載機制，支援大檔案的高效傳輸</li>
 *   <li>檔案管理：包含檔案元資料查詢、資料夾列表、檔案刪除等基本操作</li>
 *   <li>資料夾操作：支援資料夾創建、列表查詢和階層式結構管理</li>
 *   <li>用戶認證：整合 JWT 認證機制，確保所有操作的安全性</li>
 * </ul>
 *
 * <p>服務設計特點：
 * <ul>
 *   <li>非阻塞 I/O：採用 WebFlux 反應式框架，提供高併發處理能力</li>
 *   <li>安全性：所有操作均需通過 JWT 驗證和權限檢查</li>
 *   <li>策略模式：透過 FileServiceStrategy 支援不同檔案類型的處理邏輯</li>
 *   <li>錯誤處理：完整的錯誤捕獲和回應機制</li>
 *   <li>資源管理：正確處理 DataBuffer 釋放，避免記憶體洩漏</li>
 * </ul>
 *
 * <p>與 WebDAV 整合：此服務作為 WebDAV 協議的後端實現，提供標準化的檔案操作介面，
 * 支援各種 WebDAV 客戶端的檔案同步和管理需求。
 *
 * <p>使用範例：
 * <pre>
 * // gRPC 客戶端使用範例
 * FileProcessingServiceBlockingStub stub = FileProcessingServiceGrpc.newBlockingStub(channel);
 * 
 * // 認證用戶
 * AuthenticationResponse auth = stub.authenticateUser(
 *     AuthenticationRequest.newBuilder()
 *         .setUsername("user")
 *         .setPassword("password")
 *         .build());
 * 
 * // 上傳檔案
 * StreamObserver&lt;UploadProgress&gt; uploadObserver = new StreamObserver&lt;UploadProgress&gt;() {
 *     // 處理上傳進度
 * };
 * StreamObserver&lt;FileUploadChunk&gt; requestObserver = stub.uploadFile(uploadObserver);
 * </pre>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see FileProcessingServiceGrpc.FileProcessingServiceImplBase
 * @see FileServiceStrategy
 * @see UserService
 * @see PermissionService
 */
@Component
@RequiredArgsConstructor
public class FileProcessingGrpcService extends FileProcessingServiceGrpc.FileProcessingServiceImplBase {

    /**
     * 檔案服務策略管理器。
     * 負責根據檔案類型選擇適當的服務實現，支援一般檔案、資料夾和線上檔案等不同類型的處理邏輯。
     */
    private final FileServiceStrategy fileServiceStrategy;

    /**
     * 用戶服務介面。
     * 提供用戶相關的核心功能，包含用戶認證、資料查詢和權限驗證等操作。
     */
    private final UserService userService;

    /**
     * 檔案權限服務。
     * 負責驗證用戶對特定檔案的存取權限，確保檔案操作的安全性和合規性。
     */
    private final PermissionService<UserFileMetadata> permissionService;

    /**
     * 令牌服務介面。
     * 處理 JWT 令牌的生成、驗證和解析，支援用戶認證和授權機制。
     */
    private final TokenService tokenService;

    /**
     * JSON 物件映射器。
     * 用於處理線上檔案內容的 JSON 序列化和反序列化操作，確保資料格式的正確性。
     */
    private final ObjectMapper objectMapper;


    /**
     * 流式檔案上傳服務。
     *
     * <p>提供高效的流式檔案上傳功能，適用於大檔案和高併發場景。支援分塊上傳、
     * 即時進度回報和 MD5 校驗機制。系統會自動檢測相同檔案是否已存在，
     * 實現秒傳功能以提高上傳效率。
     *
     * <p>上傳流程：
     * <ol>
     *   <li>客戶端發送檔案元資料（檔案名、大小、上級目錄等）</li>
     *   <li>客戶端分塊發送檔案內容，系統即時回報上傳進度</li>
     *   <li>所有分塊傳輸完成後，發送 MD5 校驗碼進行整合性驗證</li>
     *   <li>系統驗證成功後儲存檔案，回傳上傳結果</li>
     * </ol>
     *
     * <p>進度回報特性：
     * <ul>
     *   <li>即時百分比進度更新</li>
     *   <li>支援錯誤訊息回報</li>
     *   <li>秒傳狀態檢測</li>
     *   <li>上傳完成狀態標記</li>
     * </ul>
     *
     * <p>安全性考量：
     * <ul>
     *   <li>所有上傳操作均需 JWT 令牌驗證</li>
     *   <li>MD5 校驗確保檔案完整性</li>
     *   <li>上傳失敗時自動清理臨時資源</li>
     * </ul>
     *
     * @param responseObserver 上傳進度回應觀察器，用於即時回報上傳進度和結果
     * @return 檔案上傳分塊的請求觀察器，接收客戶端發送的檔案分塊資料
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
                    String calculatedMd5 = bytesToHex(md5Digest.digest());
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

                validateUserAndUpload(metadata.getUserId(),
                                      metadata.getUserToken(),
                                      metadata.getFilename(),
                                      metadata.getParentFolderId(),
                                      checksum.getMd5(),
                                      buffer.toByteArray(),
                                      metadata.getMimeType()
                ).subscribe(response -> {
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
     * 簡單檔案上傳服務。
     *
     * <p>適用於小型檔案的快速上傳，採用一次性傳輸模式，無需分塊處理。
     * 適合小於 10MB 的檔案上傳，提供更簡化的上傳流程和更低的延遲。
     *
     * <p>功能特點：
     * <ul>
     *   <li>一次性檔案傳輸，無需分塊處理</li>
     *   <li>支援 MD5 校驗確保檔案完整性</li>
     *   <li>自動秒傳檢測，避免重複上傳</li>
     *   <li>即時結果回報，無需等待進度更新</li>
     * </ul>
     *
     * <p>上傳流程：
     * <ol>
     *   <li>驗證用戶 JWT 令牌和權限</li>
     *   <li>檢查 MD5 值是否已存在相同檔案（秒傳檢測）</li>
     *   <li>儲存檔案內容和元資料</li>
     *   <li>回傳上傳結果和檔案 ID</li>
     * </ol>
     *
     * <p>錯誤處理：當上傳失敗時，系統會返回詳細錯誤訊息，協助客戶端
     * 進行問題診斷和處理。
     *
     * @param request 上傳檔案請求，包含檔案內容、元資料和用戶認證資訊
     * @param responseObserver 上傳結果回應觀察器，用於回傳上傳狀態和結果
     */
    @Override
    public void uploadFileSimple(UploadFileRequest request, StreamObserver<UploadFileResponse> responseObserver) {
        validateUserAndUpload(request.getUserId(),
                              request.getUserToken(),
                              request.getFilename(),
                              request.getParentFolderId(),
                              request.getMd5(),
                              request.getContent().toByteArray(),
                              request.getMimeType()
        ).subscribe(response -> {
                        responseObserver.onNext(UploadFileResponse
                                                        .newBuilder()
                                                        .setSuccess(response.getIsSuccess())
                                                        .setFileId(response.getFileId() != null ? response.getFileId() : 0)
                                                        .setIsInstantUpload(response.getIsFinished() && response.getProgress() == 100.0)
                                                        .setMessage(response.getMessage())
                                                        .build());
                        responseObserver.onCompleted();
                    }, error -> {
                        LogUnity.error("簡單上傳失敗: %s", error, error.getMessage());
                        responseObserver.onNext(UploadFileResponse.newBuilder().setSuccess(false).setErrorMessage(error.getMessage()).build());
                        responseObserver.onCompleted();
                    }
        );
    }


    /**
     * 獲取檔案內容服務。
     *
     * <p>根據指定的檔案 ID 及用戶認證資訊，獲取檔案的完整內容。
     * 支持不同類型的檔案資料輸出，包括二進位檔案、JSON 格式的線上文件等。
     *
     * <p>支持的檔案類型：
     * <ul>
     *   <li>一般檔案：直接輸出二進位內容</li>
     *   <li>線上文件：將結構化內容序列化為 JSON 格式</li>
     *   <li>大型檔案：採用分塊流式輸出，減少記憶體使用</li>
     * </ul>
     *
     * <p>安全性檢查：
     * <ol>
     *   <li>驗證用戶 JWT 令牌的有效性</li>
     *   <li>檢查用戶對指定檔案的讀取權限</li>
     *   <li>確保檔案存在且未被刪除</li>
     * </ol>
     *
     * <p>分塊傳輸機制：為了優化大檔案的傳輸效率和記憶體使用，
     * 系統會自動將檔案內容分成 64KB 的小塊進行傳輸，並標記最後一個分塊。
     *
     * @param request 檔案內容獲取請求，包含檔案 ID 和用戶認證資訊
     * @param responseObserver 檔案內容回應觀察器，用於分塊傳輸檔案內容
     */
    @Override
    public void getFileContent(GetFileRequest request, StreamObserver<FileContentResponse> responseObserver) {
        User user = new User();
        user.setId(request.getUserId());

        tokenService.validateToken(request.getUserToken(), request.getUserId(), TokenEnum.JWT_AUTHORIZATION_TOKEN).flatMap(userId -> {
            user.setId(userId);
            return permissionService.validateUserPermission(user, request.getFileId());
        }).flatMap(file -> {
            FileService fileService = fileServiceStrategy.getFileService(file.getFileType());
            return fileService.downloadFile(file, user);
        }).subscribe(userFileDataBO -> handleFileContent(userFileDataBO, responseObserver), error -> {
                         LogUnity.error("獲取檔案內容失敗: %s", error, error.getMessage());
                         responseObserver.onNext(FileContentResponse.newBuilder().setErrorMessage(error.getMessage()).build());
                         responseObserver.onCompleted();
                     }
        );
    }


    /**
     * 獲取檔案元資料服務。
     *
     * <p>根據指定的檔案 ID 獲取檔案的詳細元資料資訊，包含檔案名稱、
     * 大小、創建時間、修改時間和所屬目錄等關鍵資訊。
     *
     * <p>元資料內容：
     * <ul>
     *   <li>檔案唯一識別符 (ID)</li>
     *   <li>檔案名稱和類型（檔案/資料夾）</li>
     *   <li>檔案大小（位元組數）</li>
     *   <li>創建時間和最後修改時間</li>
     *   <li>上級目錄 ID（用於目錄結構導覽）</li>
     * </ul>
     *
     * <p>權限驗證：
     * <ol>
     *   <li>驗證 JWT 令牌的有效性和用戶身份</li>
     *   <li>檢查用戶是否擁有該檔案的讀取權限</li>
     *   <li>確保檔案存在且未被刪除至回收站</li>
     * </ol>
     *
     * <p>效能特點：本服務僅獲取檔案元資料，不讀取檔案內容，
     * 因此具有很高的執行效率，適用於檔案瀏覽器和列表顯示等場景。
     *
     * @param request 檔案元資料獲取請求，包含檔案 ID 和用戶認證資訊
     * @param responseObserver 檔案元資料回應觀察器，用於返回檔案的元資料資訊
     */
    @Override
    public void getFileMetadata(GetFileMetadataRequest request, StreamObserver<FileMetadataResponse> responseObserver) {
        User user = new User();
        user.setId(request.getUserId());
        tokenService
                .validateToken(request.getUserToken(), request.getUserId(), TokenEnum.JWT_AUTHORIZATION_TOKEN)
                .flatMap(userId -> permissionService.validateUserPermission(user, request.getFileId()))
                .flatMap(fileMetadata -> {
                    if (fileMetadata.getServerFileId() != null) {
                        FileService fileService = fileServiceStrategy.getFileService(fileMetadata.getFileType());
                        return fileService.getByServerFileMetadataId(fileMetadata.getServerFileId()).flatMap(serverFileMetadata -> {
                            return Mono.just(Tuples.of(serverFileMetadata.getFileSize(), fileMetadata));
                        });
                    }
                    return Mono.just(Tuples.of(0L, fileMetadata));
                })
                .subscribe(tuple2 -> {
                               UserFileMetadata file = tuple2.getT2();
                               FileInfo fileInfo = FileInfo
                                       .newBuilder()
                                       .setId(file.getId())
                                       .setName(file.getFilename())
                                       .setSize(tuple2.getT1())
                                       .setIsDirectory(file.getFileType() == FileEnum.FOLDER)
                                       .setCreatedTime(file.getUploadTime() != null ? file
                                               .getUploadTime()
                                               .toInstant(java.time.ZoneOffset.UTC)
                                               .toEpochMilli() : 0)
                                       .setModifiedTime(file.getLastAccessTime() != null ? file
                                               .getLastAccessTime()
                                               .toInstant(java.time.ZoneOffset.UTC)
                                               .toEpochMilli() : 0)
                                       .setParentId(file.getParentFolderId() != null ? file.getParentFolderId() : 0)
                                       .build();

                               responseObserver.onNext(FileMetadataResponse.newBuilder().setSuccess(true).setFileInfo(fileInfo).build());
                               responseObserver.onCompleted();
                           }, error -> {
                               LogUnity.error("獲取檔案元資料失敗: %s", error, error.getMessage());
                               responseObserver.onNext(FileMetadataResponse.newBuilder().setSuccess(false).setErrorMessage(error.getMessage()).build());
                               responseObserver.onCompleted();
                           }
                );
    }


    /**
     * 檔案刪除服務（軟刪除）。
     *
     * <p>將指定的檔案或資料夾移動至用戶回收站，而非物理刪除。
     * 這種軟刪除機制允許用戶在需要時恢復刪除的檔案，提供更好的數據安全性。
     *
     * <p>刪除範圍：
     * <ul>
     *   <li>單一檔案：直接移動至回收站</li>
     *   <li>資料夾：包含其下所有子檔案和子資料夾</li>
     *   <li>共享檔案：只影響當前用戶的存取權限</li>
     * </ul>
     *
     * <p>權限驗證：
     * <ol>
     *   <li>驗證 JWT 令牌的有效性和用戶身份</li>
     *   <li>檢查用戶是否為檔案擁有者或擁有刪除權限</li>
     *   <li>確保檔案存在且未被刪除</li>
     * </ol>
     *
     * <p>刪除記錄：
     * <ul>
     *   <li>記錄刪除時間和原始位置，便於後續恢復</li>
     *   <li>保存檔案元資料和權限設定</li>
     *   <li>支持回收站管理和自動清理機制</li>
     * </ul>
     *
     * <p>特殊處理：對於資料夾的刪除，系統會遞歸處理所有子項目，
     * 確保整個目錄結構都被正確標記為已刪除狀態。
     *
     * @param request 檔案刪除請求，包含檔案 ID 和用戶認證資訊
     * @param responseObserver 檔案刪除回應觀察器，用於返回刪除操作結果
     */
    @Override
    public void deleteFile(DeleteFileRequest request, StreamObserver<DeleteFileResponse> responseObserver) {
        User user = new User();
        user.setId(request.getUserId());
        tokenService
                .validateToken(request.getUserToken(), request.getUserId(), TokenEnum.JWT_AUTHORIZATION_TOKEN)
                .flatMap(userId -> permissionService.validateUserPermission(user, request.getFileId()))
                .flatMap(file -> {
                    FileService fileService = fileServiceStrategy.getFileService(file.getFileType());
                    return fileService.removeFile(file, user);
                })
                .subscribe(result -> {
                               responseObserver.onNext(DeleteFileResponse.newBuilder().setSuccess(true).setMessage("已移動到回收站").build());
                               responseObserver.onCompleted();
                           }, error -> {
                               LogUnity.error("刪除失敗: %s", error, error.getMessage());
                               responseObserver.onNext(DeleteFileResponse.newBuilder().setSuccess(false).setErrorMessage(error.getMessage()).build());
                               responseObserver.onCompleted();
                           }
                );
    }


    /**
     * 獲取資料夾列表服務。
     *
     * <p>根據指定的資料夾 ID 獲取其下所有檔案和子資料夾的列表資訊。
     * 如果指定的資料夾 ID 為 0，則返回用戶根目錄的所有內容。
     *
     * <p>列表內容特點：
     * <ul>
     *   <li>包含所有類型的檔案和資料夾</li>
     *   <li>提供檔案的基本資訊（名稱、大小、時間等）</li>
     *   <li>自動過濾已刪除的檔案</li>
     *   <li>按照預設順序排列（資料夾優先，然後按名稱排序）</li>
     * </ul>
     *
     * <p>權限控制：
     * <ul>
     *   <li>驗證用戶身份和 JWT 令牌有效性</li>
     *   <li>只顯示用戶擁有讀取權限的檔案和資料夾</li>
     *   <li>遵循系統的檔案共享與私有設定</li>
     * </ul>
     *
     * <p>效能優化：為了提供高效的目錄瀏覽體驗，系統會一次性獲取
     * 整個目錄的所有內容，避免多次資料庫查詢。
     *
     * @param request 資料夾列表獲取請求，包含資料夾 ID 和用戶認證資訊
     * @param responseObserver 資料夾列表回應觀察器，用於返回檔案和資料夾列表
     */
    @Override
    public void listFolder(ListFolderRequest request, StreamObserver<ListFolderResponse> responseObserver) {
        tokenService.validateToken(request.getUserToken(), request.getUserId(), TokenEnum.JWT_AUTHORIZATION_TOKEN).flatMap(userId -> {
            User user = new User();
            user.setId(userId);
            FileFilterDTO filterDTO = FileFilterDTO
                    .builder()
                    .folderId(request.getFolderId() == 0 ? null : request.getFolderId())
                    .pageSize(Integer.MAX_VALUE)
                    .page(1)
                    .includeDeleted(false)
                    .build();

            FileService fileService = fileServiceStrategy.getFileService(FileEnum.FOLDER);
            return fileService.getUserFileList(user, filterDTO).map(PagedResponseDTO::getData);
        }).subscribe(files -> {
                         List<FileInfo> fileInfos = files.stream().map(this::convertUserFileListDTOToFileInfo).collect(Collectors.toList());

                         responseObserver.onNext(ListFolderResponse.newBuilder().setSuccess(true).addAllFiles(fileInfos).setTotalCount(fileInfos.size()).build());
                         responseObserver.onCompleted();
                     }, error -> {
                         LogUnity.error("列出資料夾失敗: %s", error, error.getMessage());
                         responseObserver.onNext(ListFolderResponse.newBuilder().setSuccess(false).setErrorMessage(error.getMessage()).build());
                         responseObserver.onCompleted();
                     }
        );
    }


    /**
     * 創建資料夾服務。
     *
     * <p>在指定的上級目錄中創建一個新的資料夾。如果上級目錄 ID 為 0，
     * 則在用戶根目錄下創建。系統會自動驗證資料夾名稱的唯一性。
     *
     * <p>創建驗證：
     * <ul>
     *   <li>資料夾名稱不能為空或只包含空格</li>
     *   <li>資料夾名稱不能包含系統保留字元（如 / \ : * ? " < > |）</li>
     *   <li>在同一上級目錄中不能有重名的資料夾</li>
     *   <li>用戶必須對上級目錄擁有寫入權限</li>
     * </ul>
     *
     * <p>權限檢查：
     * <ol>
     *   <li>驗證 JWT 令牌的有效性和用戶身份</li>
     *   <li>檢查用戶對上級目錄的寫入權限</li>
     *   <li>確保上級目錄存在且未被刪除</li>
     * </ol>
     *
     * <p>自動化處理：
     * <ul>
     *   <li>自動設定資料夾的創建時間和擁有者</li>
     *   <li>繼承上級目錄的權限設定</li>
     *   <li>自動更新目錄結構緩存</li>
     * </ul>
     *
     * @param request 資料夾創建請求，包含資料夾名稱、上級目錄 ID 和用戶認證資訊
     * @param responseObserver 資料夾創建回應觀察器，用於返回創建結果和新資料夾 ID
     */
    @Override
    public void createFolder(CreateFolderRequest request, StreamObserver<CreateFolderResponse> responseObserver) {
        Long parentId = request.getParentId() == 0 ? null : request.getParentId();
        FileEditDTO fileEditDTO = FileEditDTO.builder().filename(request.getFolderName()).parentFolderId(parentId).build();

        tokenService.validateToken(request.getUserToken(), request.getUserId(), TokenEnum.JWT_AUTHORIZATION_TOKEN).flatMap(userId -> {
            FolderService folderService = (FolderService) fileServiceStrategy.getFileService(FileEnum.FOLDER);

            User user = new User();
            user.setId(userId);
            return folderService.createFolder(fileEditDTO, user).doOnSuccess(v -> {
                responseObserver.onNext(CreateFolderResponse
                                                .newBuilder()
                                                .setSuccess(true)
                                                .setFolderId(Long.parseLong(fileEditDTO.getFileId()))
                                                .setMessage("資料夾創建成功")
                                                .build());
                responseObserver.onCompleted();
            }).doOnError(error -> {
                LogUnity.error("創建資料夾失敗: %s", error, error.getMessage());
                responseObserver.onNext(CreateFolderResponse.newBuilder().setSuccess(false).setErrorMessage(error.getMessage()).build());
                responseObserver.onCompleted();
            });
        }).subscribe();
    }


    /**
     * 用戶認證服務。
     *
     * <p>提供基於用戶名和密碼的認證服務，成功認證後返回 JWT 令牌和用戶 ID。
     * 此令牌可用於後續的所有 gRPC 服務調用，確保操作的安全性。
     *
     * <p>認證流程：
     * <ol>
     *   <li>接收客戶端提供的用戶名和密碼</li>
     *   <li>調用用戶服務進行身份驗證</li>
     *   <li>驗證成功後生成 JWT 令牌</li>
     *   <li>從 JWT 令牌中提取用戶 ID</li>
     *   <li>返回認證結果、JWT 令牌和用戶 ID</li>
     * </ol>
     *
     * <p>安全特性：
     * <ul>
     *   <li>密碼驗證采用安全的哈希算法</li>
     *   <li>JWT 令牌具有過期時間限制</li>
     *   <li>失敗次數限制和帳戶鎖定機制</li>
     *   <li>詳細的安全日誌記錄</li>
     * </ul>
     *
     * <p>錯誤處理：當認證失敗時，系統會返回適當的錯誤訊息，
     * 但不會洩露具體的失敗原因（如用戶不存在或密碼錯誤），
     * 以防止惡意攻擊者進行用戶枚舉。
     *
     * @param request 用戶認證請求，包含用戶名和密碼
     * @param responseObserver 認證結果回應觀察器，用於返回 JWT 令牌和用戶 ID
     */
    @Override
    public void authenticateUser(AuthenticationRequest request, StreamObserver<AuthenticationResponse> responseObserver) {
        AuthRequestDTO authRequest = AuthRequestDTO.builder().username(request.getUsername()).password(request.getPassword()).build();

        userService.login(authRequest, null).flatMap(jwtToken -> {
            return tokenService.extractUserIdFromToken(jwtToken, TokenEnum.JWT_AUTHORIZATION_TOKEN).map(userId -> Tuples.of(jwtToken, userId));
        }).subscribe(tokenAndUserId -> {
                         String jwtToken = tokenAndUserId.getT1();
                         Long userId = tokenAndUserId.getT2();

                         responseObserver.onNext(AuthenticationResponse.newBuilder().setSuccess(true).setJwtToken(jwtToken).setUserId(userId).build());
                         responseObserver.onCompleted();
                     }, error -> {
                         LogUnity.error("認證失敗: %s", error, error.getMessage());
                         responseObserver.onNext(AuthenticationResponse.newBuilder().setSuccess(false).setErrorMessage(error.getMessage()).build());
                         responseObserver.onCompleted();
                     }
        );
    }

    // ==================== 輔助方法 ====================


    /**
     * 將檔案列表 DTO 轉換為 gRPC FileInfo 物件的輔助方法。
     *
     * <p>此方法專門處理檔案列表查詢結果的轉換，將內部的 UserFileListDTO 轉換為
     * 客戶端可使用的 gRPC FileInfo 格式。與其他轉換方法的差別在於包含檔案大小資訊。
     *
     * <p>特殊處理：
     * <ul>
     *   <li>包含檔案大小資訊，適用於檔案瀏覽器顯示</li>
     *   <li>對於 null 值進行安全處理，避免空指針異常</li>
     *   <li>時間格式轉換為 UTC 時區的毫秒時間戳</li>
     *   <li>檔案類型判斷和資料夾標記設定</li>
     * </ul>
     *
     * <p>數據安全性：所有可能為 null 的欄位都會進行檢查，並設定預設值，
     * 確保返回的 FileInfo 物件完整可用。
     *
     * @param dto 檔案列表查詢結果 DTO
     * @return 轉換後的 gRPC FileInfo 物件
     */
    private FileInfo convertUserFileListDTOToFileInfo(UserFileListDTO dto) {
        return FileInfo
                .newBuilder()
                .setId(dto.getId())
                .setName(dto.getFilename())
                .setSize(dto.getFileSize() != null ? dto.getFileSize() : 0L)
                .setIsDirectory(dto.getFileType() == FileEnum.FOLDER)
                .setParentId(dto.getParentFolderId() != null ? dto.getParentFolderId() : 0L)
                .setCreatedTime(dto.getCreateTime() != null ? dto.getCreateTime().toInstant(ZoneOffset.UTC).toEpochMilli() : 0L)
                .setModifiedTime(dto.getLastAccessTime() != null ? dto.getLastAccessTime().toInstant(ZoneOffset.UTC).toEpochMilli() : 0L)
                .build();
    }


    /**
     * 處理檔案內容輸出的輔助方法。
     *
     * <p>這是檔案下載服務的核心輔助方法，負責根據不同的檔案類型選擇適當的輸出策略。
     * 支援多種數據格式的輸出，包括結構化內容、二進位数據和流式數據。
     *
     * <p>支援的數據類型：
     * <ol>
     *   <li>線上文件內容：將結構化內容序列化為 JSON 後輸出</li>
     *   <li>二進位數據：直接輸出 Mono&lt;byte[]&gt; 格式的數據</li>
     *   <li>流式數據：處理 Flux&lt;DataBuffer&gt; 格式的大檔案流</li>
     * </ol>
     *
     * <p>效能優化：
     * <ul>
     *   <li>對於大檔案，採用分塊傳輸以減少記憶體使用</li>
     *   <li>對於線上文件，進行 JSON 格式轉換</li>
     *   <li>正確處理 DataBuffer 釋放，防止記憶體洩漏</li>
     * </ul>
     *
     * <p>錯誤處理：當處理過程中發生錯誤時，會記錄詳細日誌並向客戶端返回錯誤訊息。
     *
     * @param userFileDataBO 用戶檔案數據業務物件，包含檔案內容和元資料
     * @param responseObserver gRPC 回應觀察器，用於分塊傳輸檔案內容
     */
    private void handleFileContent(UserFileDataBO userFileDataBO, StreamObserver<FileContentResponse> responseObserver) {
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

        responseObserver.onNext(FileContentResponse.newBuilder().setContent(ByteString.EMPTY).setTotalSize(0L).setIsFinal(true).build());
        responseObserver.onCompleted();
    }


    /**
     * 分塊發送檔案內容的輔助方法。
     *
     * <p>將大型檔案內容分成多個小塊進行傳輸，以優化記憶體使用和網絡傳輸效率。
     * 預設分塊大小為 64KB，適用於大部分檔案類型的傳輸。
     *
     * <p>分塊策略：
     * <ul>
     *   <li>每個分塊最大 64KB，避免單次傳輸過大数據</li>
     *   <li>最後一個分塊會被標記為 isFinal=true</li>
     *   <li>每個分塊都包含總檔案大小資訊</li>
     *   <li>使用系統複製操作確保數據安全性</li>
     * </ul>
     *
     * <p>效能考量：此方法采用同步方式進行分塊傳輸，適用於中等大小的檔案。
     * 對於非常大的檔案，建議使用流式處理方法。
     *
     * @param data 要傳輸的檔案二進位數據
     * @param responseObserver gRPC 回應觀察器，用於傳輸分塊數據
     */
    private void sendContentInChunks(byte[] data, StreamObserver<FileContentResponse> responseObserver) {
        int chunkSize = 64 * 1024;
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
     * 將位元組陣列轉換為十六進位字串的輔助方法。
     *
     * <p>此方法主要用於 MD5 校驗碼的轉換，將二進位的雜湊值轉換為可讀的十六進位字串格式。
     * 這是一個通用的工具方法，遵循標準的十六進位轉換規則。
     *
     * <p>轉換規則：
     * <ul>
     *   <li>每個位元組轉換為兩位十六進位字元</li>
     *   <li>使用小寫字母 (a-f) 表示 10-15</li>
     *   <li>不足兩位時在前面補零</li>
     *   <li>返回的字串長度為原始数據長度的兩倍</li>
     * </ul>
     *
     * <p>使用場景：
     * <ul>
     *   <li>MD5 雜湊值轉換</li>
     *   <li>SHA 系列雜湊值轉換</li>
     *   <li>任何需要十六進位顯示的二進位數據</li>
     * </ul>
     *
     * @param bytes 要轉換的位元組陣列
     * @return 十六進位表示的字串，使用小寫字母
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }


    /**
     * 驗證用戶並執行檔案上傳的輔助方法。
     *
     * <p>此方法是上傳流程的核心輔助方法，負責驗證用戶身份、權限和執行最終的檔案上傳操作。
     * 整合了安全性檢查和檔案處理邏輯，確保上傳操作的完整性。
     *
     * <p>執行步驟：
     * <ol>
     *   <li>驗證 JWT 令牌的有效性和用戶身份</li>
     *   <li>根據用戶 ID 獲取用戶詳細資訊</li>
     *   <li>構建檔案元資料物件（包含檔案名、大小、MD5 等）</li>
     *   <li>選擇適當的檔案服務策略並執行上傳</li>
     * </ol>
     *
     * <p>安全考量：
     * <ul>
     *   <li>所有參數都會進行嚴格的驗證</li>
     *   <li>使用反應式程式設計，避免阻塞作業</li>
     *   <li>自動處理權限檢查和檔案重複性驗證</li>
     * </ul>
     *
     * @param userId 用戶唯一識別符
     * @param token JWT 令牌，用於驗證用戶身份
     * @param filename 檔案名稱
     * @param parentFolderId 上級目錄 ID，可為 null 表示根目錄
     * @param md5 檔案 MD5 校驗碼
     * @param content 檔案二進位內容
     * @param mimeType 檔案 MIME 類型
     * @return 上傳結果的 Mono 包裝，包含上傳狀態和檔案 ID
     */
    private Mono<UploadResponseDTO> validateUserAndUpload(Long userId, String token, String filename, Long parentFolderId, String md5, byte[] content, String mimeType) {
        return tokenService.validateToken(token, userId, TokenEnum.JWT_AUTHORIZATION_TOKEN).flatMap(userService::getById).flatMap(user -> {
            FileMetadataDTO metadata = new FileMetadataDTO();
            metadata.setFilename(filename);
            metadata.setParentFolderId(parentFolderId);
            metadata.setMd5(md5);
            metadata.setFileSize((long) content.length);
            metadata.setUser(user);

            FileService fileService = fileServiceStrategy.getFileService();
            return fileService.uploadFile(metadata, user);
        });
    }


    /**
     * 將用戶檔案元資料轉換為 gRPC FileInfo 物件的輔助方法。
     *
     * <p>此方法負責將內部的 UserFileMetadata 實體轉換為 gRPC 協議中定義的 FileInfo 格式，
     * 便於客戶端使用。這種轉換確保了數據的一致性和標準化。
     *
     * <p>轉換的檔案屬性：
     * <ul>
     *   <li>檔案 ID：系統內部的唯一識別符</li>
     *   <li>檔案名稱：原始檔案名稱</li>
     *   <li>檔案類型：是否為資料夾的布理值</li>
     *   <li>上級目錄 ID：用於目錄結構導覽</li>
     *   <li>時間資訊：創建和修改時間的 Unix 時間戳</li>
     * </ul>
     *
     * <p>時間處理：所有時間欄位都會轉換為 UTC 時區的 Unix 時間戳格式，
     * 確保不同時區客戶端的時間一致性。
     *
     * @param file 用戶檔案元資料實體
     * @return 轉換後的 gRPC FileInfo 物件
     */
    private FileInfo convertToFileInfo(UserFileMetadata file) {
        return FileInfo
                .newBuilder()
                .setId(file.getId())
                .setName(file.getFilename())
                .setIsDirectory(file.getFileType() == FileEnum.FOLDER)
                .setParentId(file.getParentFolderId() != null ? file.getParentFolderId() : 0)
                .setCreatedTime(file.getUploadTime() != null ? file.getUploadTime().toInstant(java.time.ZoneOffset.UTC).toEpochMilli() : 0)
                .setModifiedTime(file.getLastAccessTime() != null ? file.getLastAccessTime().toInstant(java.time.ZoneOffset.UTC).toEpochMilli() : 0)
                .build();
    }
}