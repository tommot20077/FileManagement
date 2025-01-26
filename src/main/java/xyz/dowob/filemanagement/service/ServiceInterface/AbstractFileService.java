package xyz.dowob.filemanagement.service.ServiceInterface;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.tika.Tika;
import org.bson.types.ObjectId;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import xyz.dowob.filemanagement.component.manager.TransfersTasksManager;
import xyz.dowob.filemanagement.component.provider.provider.GridFsProvider;
import xyz.dowob.filemanagement.component.provider.provider.RedisProvider;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.customenum.TransfersStatusEnum;
import xyz.dowob.filemanagement.data.file.bo.UploadTaskBO;
import xyz.dowob.filemanagement.data.file.bo.UserFileDataBO;
import xyz.dowob.filemanagement.data.file.dto.*;
import xyz.dowob.filemanagement.entity.ServerFileMetadata;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.entity.UserFileMetadata;
import xyz.dowob.filemanagement.exception.ProcessException;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.repostiory.ServerFileMetaRepository;
import xyz.dowob.filemanagement.repostiory.UserFileMetaRepository;

import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 檔案服務的抽象類，包含共通的上傳、下載邏輯
 */
@RequiredArgsConstructor
public abstract class AbstractFileService implements FileService {
    /**
     * 服務器文件元數據庫操作對象
     */
    protected final ServerFileMetaRepository serverFileMetaRepository;
    /**
     * 用戶文件元數據庫操作對象
     */
    protected final UserFileMetaRepository userFileMetaRepository;
    /**
     * Redis操作對象
     */
    protected final RedisProvider redisProvider;
    /**
     * GridFs操作對象
     */
    protected final GridFsProvider gridFsProvider;
    /**
     * 文件上傳任務管理器
     */
    protected final TransfersTasksManager transfersTasksManager;
    /**
     * 文件配置屬性
     */
    protected final FileProperties fileProperties;
    /**
     * 數據庫操作對象
     */
    protected final DatabaseClient databaseClient;
    /**
     * 斷路器配置
     */
    protected final CircuitBreakerConfig circuitBreakerConfig;

    /**
     * 檔案類型檢驗器
     */
    private final Tika tika = new Tika();
    /**
     * 每個分塊的大小
     */
    protected Long CHUNK_SIZE;

    /**
     * 初始化方法，獲取文件配置中的分塊大小
     */
    @PostConstruct
    public void init() {
        CHUNK_SIZE = (long) fileProperties.getUpload().getChunkSize() * 1024 * 1024;
    }

    /**
     * 獲取用戶文件列表的共通實現
     *
     * @param user 用戶信息
     *
     * @return Flux<UserFileListDTO>
     */
    //todo 加入緩存
    @Override
    public Flux<UserFileListDTO> getUserFileList(User user, Long folderId) {
        Mono<List<UserFileMetadata>> userFileMetadataMono;
        if (folderId == null || folderId == 0) {
            userFileMetadataMono = userFileMetaRepository.findAllByUserIdAndParentFolderIdIsNull(user.getId()).collectList();
        } else if (folderId == -1) {
            userFileMetadataMono = userFileMetaRepository.findAllByUserId(user.getId()).collectList();
        } else {
            userFileMetadataMono = userFileMetaRepository.findAllByUserIdAndParentFolderIdIn(user.getId(), List.of(folderId)).collectList();
        }

        return userFileMetadataMono.flatMapMany(userFileMetadataList -> {
            Set<Long> serverFileIds = userFileMetadataList
                    .stream()
                    .map(UserFileMetadata::getServerFileId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            List<UserFileMetadata> folderMetadata = userFileMetadataList
                    .stream()
                    .filter(userFileMetadata -> userFileMetadata.getServerFileId() == null)
                    .toList();

            if (serverFileIds.isEmpty() && folderMetadata.isEmpty()) {
                return Flux.empty();
            }

            Flux<UserFileListDTO> nullServerFileMetadataFlux = Flux.fromIterable(folderMetadata).map(UserFileListDTO::new);
            Flux<UserFileListDTO> serverFileMetadataFlux = serverFileMetaRepository
                    .findAllByIdIn(serverFileIds)
                    .collectMap(ServerFileMetadata::getId)
                    .flatMapMany(serverFileMetadataMap -> Flux
                            .fromIterable(userFileMetadataList)
                            .filter(userFileMetadata -> userFileMetadata.getServerFileId() != null)
                            .map(userFileMetadata -> {
                                ServerFileMetadata serverFileMetadata = serverFileMetadataMap.get(userFileMetadata.getServerFileId());
                                return new UserFileListDTO(serverFileMetadata, userFileMetadata);
                            }));

            return Flux.concat(nullServerFileMetadataFlux, serverFileMetadataFlux);
        });
    }

    /**
     * 上傳文件的共通實現
     *
     * @param fileMetadataDTO 文件元數據
     * @param user            用戶信息
     *
     * @return Mono<UploadResponseDTO>
     */
    @Override
    public Mono<UploadResponseDTO> uploadFile(FileMetadataDTO fileMetadataDTO, User user) {
        fileMetadataDTO.setUserId(user.getId());
        return Mono.defer(() -> {
            if (fileMetadataDTO.getParentFolderId() != null) {
                return checkParentFolderId(fileMetadataDTO.getParentFolderId(), user);
            }
            return Mono.empty();
        }).then(serverFileMetaRepository.findByMd5(fileMetadataDTO.getMd5()).flatMap(existingFile -> {
            existingFile.getOwners().add(user.getId());
            existingFile.setLastAccessTime(LocalDateTime.now());
            return serverFileMetaRepository
                    .save(existingFile)
                    .then(associateUserFile(existingFile.getId(), fileMetadataDTO)
                                  .flatMap(userFileMetaRepository::save)
                                  .thenReturn(UploadResponseDTO
                                                      .builder()
                                                      .progress(100.0)
                                                      .isSuccess(true)
                                                      .isFinished(true)
                                                      .message("上傳成功")
                                                      .build()));
        }).switchIfEmpty(initialUpload(fileMetadataDTO)));
    }

    /**
     * 上傳文件分塊的共通實現
     *
     * @param uploadChunkDTO 上傳文件數據
     *
     * @return Mono<UploadResponseDTO>
     */
    @Override
    public Mono<UploadResponseDTO> uploadFileChunk(UploadChunkDTO uploadChunkDTO) {
        String transferTaskId = uploadChunkDTO.getTransferTaskId();
        String key = "upload_task:" + transferTaskId;
        String pendingChunkKey = key + ":pending_chunks";

        return Mono.defer(() -> redisProvider
                .getHashMap(key, "DTO")
                .switchIfEmpty(Mono.error(new ProcessException(ProcessException.ErrorCode.NOT_EXISTING_UPLOAD_TASK, transferTaskId)))
                .flatMap(task -> redisProvider.isChunkSetPending(pendingChunkKey, uploadChunkDTO.getChunkIndex()).flatMap(isPending -> {
                    if (isPending) {
                        return processChunk(uploadChunkDTO, transferTaskId, key, pendingChunkKey);
                    }
                    return redisProvider.getHashMap(key, "uploaded_count").flatMap(uploadCount -> {
                        Long uploadCountLong = (Long) uploadCount;
                        double progress = (uploadCountLong.doubleValue() / uploadChunkDTO.getTotalChunks()) * 100.0;
                        String message = String.format("文件分塊: %d 已上傳", uploadChunkDTO.getChunkIndex());
                        UploadResponseDTO responseDTO = UploadResponseDTO
                                .builder()
                                .chunkIndex(uploadChunkDTO.getChunkIndex())
                                .transferTaskId(transferTaskId)
                                .progress(progress)
                                .isSuccess(true)
                                .isFinished(false)
                                .message(message)
                                .totalChunks(uploadChunkDTO.getTotalChunks())
                                .build();

                        if (uploadCountLong.intValue() == uploadChunkDTO.getTotalChunks()) {
                            combineChunks(transferTaskId, uploadChunkDTO.getTotalChunks())
                                    .subscribeOn(Schedulers.boundedElastic())
                                    .subscribe();
                            responseDTO.setIsFinished(true);
                        }
                        return Mono.just(responseDTO);
                    });

                })));
    }

    /**
     * 下載文件的共通實現
     *
     * @param fileId 文件ID
     * @param user   用戶信息
     *
     * @return Mono<UserFileDataBO>
     */
    //todo 後期加入下載資料夾
    @Override
    public Mono<UserFileDataBO> downloadFile(String fileId, User user) {
        return userFileMetaRepository
                .findById(fileId)
                .switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.NOT_EXISTING_USER_FILE, fileId)))
                .flatMap(userFileMetadata -> validateUserPermission(user, userFileMetadata, false).then(isFileOrFolder(userFileMetadata,
                                                                                                                       false
                )))
                .flatMap(userFileMetadata -> serverFileMetaRepository
                        .findById(userFileMetadata.getServerFileId().toString())
                        .switchIfEmpty(Mono.error(new ProcessException(ProcessException.ErrorCode.USER_HAVE_NOT_EXIST_SERVER_FILE,
                                                                       userFileMetadata.getServerFileId(),
                                                                       fileId
                        )))
                        .map(serverFileMetadata -> new UserFileDataBO(serverFileMetadata, userFileMetadata)))
                .flatMap(fileData -> gridFsProvider
                        .findFileById(new ObjectId(fileData.getGridFsId()))
                        .switchIfEmpty(Mono.error(new ProcessException(ProcessException.ErrorCode.GRIDFS_FILE_NOT_FOUND,
                                                                       fileData.getServerFileId()
                        )))
                        .flatMap(gridFsFile -> gridFsProvider.getResource(gridFsFile).map(resource -> {
                            Flux<DataBuffer> dataStream = resource.getDownloadStream().map(dataBuffer -> {
                                byte[] bytes = new byte[dataBuffer.readableByteCount()];
                                dataBuffer.read(bytes);
                                DataBufferUtils.release(dataBuffer);
                                return DefaultDataBufferFactory.sharedInstance.wrap(bytes);
                            });
                            fileData.setDataStream(dataStream);
                            return fileData;
                        })));
    }

    /**
     * 刪除文件的共通實現
     *
     * @param fileId 文件ID
     * @param user   用戶信息
     *
     * @return Mono<Void>
     */
    @Override
    public Mono<Void> deleteFile(String fileId, User user) {
        return userFileMetaRepository
                .findById(fileId)
                .switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.NOT_EXISTING_USER_FILE, fileId)))
                .flatMap(userFileMetadata -> validateUserPermission(user, userFileMetadata).then(isFileOrFolder(userFileMetadata, false)))
                .flatMap(userFileMetadata -> updateOwner(userFileMetadata, user).then(userFileMetaRepository.deleteById(userFileMetadata
                                                                                                                                .getId()
                                                                                                                                .toString())));
    }

    /**
     * 編輯文件的共通實現
     * 處理文件名、父文件夾ID、共享用戶ID的更新
     * @param fileEditDTO 文件ID
     * @param user        用戶信息
     *
     * @return Mono<Void>
     */
    @Override
    public Mono<Void> editFile(FileEditDTO fileEditDTO, User user) {
        return userFileMetaRepository
                .findById(fileEditDTO.getFileId())
                .switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.NOT_EXISTING_USER_FILE,
                                                                  fileEditDTO.getFileId()
                )))
                .flatMap(userFileMetadata -> validateUserPermission(user, userFileMetadata)
                        .then(isFileOrFolder(userFileMetadata, false))
                        .then(Mono.defer(() -> {
                            if (fileEditDTO.getParentFolderId() == null) {
                                return Mono.just(userFileMetadata);
                            }
                            return userFileMetaRepository
                                    .findById(fileEditDTO.getParentFolderId().toString())
                                    .flatMap(parentFolder -> validateUserPermission(user, parentFolder)
                                            .then(Mono.just(userFileMetadata))
                                            .switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.PERMISSION_DENIED,
                                                                                              fileEditDTO.getParentFolderId()
                                            ))))
                                    .switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.NOT_EXISTING_USER_FILE,
                                                                                      fileEditDTO.getParentFolderId()
                                    )));
                        }))
                        .then(Mono.just(userFileMetadata)))
                .flatMap(userFileMetadata -> {
                    userFileMetadata.setFilename(fileEditDTO.getFileName());
                    userFileMetadata.setParentFolderId(fileEditDTO.getParentFolderId());
                    userFileMetadata.setSharedWithUsers(fileEditDTO.getShareUserIds());
                    userFileMetadata.setLastAccessTime(LocalDateTime.now());
                    return userFileMetaRepository.save(userFileMetadata).then();
                });
    }

    /**
     * 創建文件夾的共通實現
     *
     * @param fileEditDTO 文件編輯數據
     * @param user        用戶信息
     *
     * @return Mono<Void>
     */
    @Override
    public Mono<Void> createFolder(FileEditDTO fileEditDTO, User user) {
        return Mono.defer(() -> {
            if (fileEditDTO.getParentFolderId() != null) {
                return checkParentFolderId(fileEditDTO.getParentFolderId(), user);
            }
            return Mono.empty();
        }).then(Mono.defer(() -> {
            UserFileMetadata folder = new UserFileMetadata();
            folder.setUserId(user.getId());
            folder.setFilename(fileEditDTO.getFileName());
            folder.setIsFolder(true);
            folder.setParentFolderId(fileEditDTO.getParentFolderId());
            folder.setSharedWithUsers(fileEditDTO.getShareUserIds());
            folder.setLastAccessTime(LocalDateTime.now());
            folder.setUploadTime(LocalDateTime.now());
            return userFileMetaRepository.save(folder).then();
        }));
    }

    /**
     * 編輯文件夾的共通實現
     *
     * @param fileEditDTO 文件編輯數據
     * @param user        用戶信息
     *
     * @return Mono<Void>
     */
    @Override
    public Mono<Void> editFolder(FileEditDTO fileEditDTO, User user) {
        return userFileMetaRepository
                .findById(fileEditDTO.getFileId())
                .switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.NOT_EXISTING_USER_FILE,
                                                                  fileEditDTO.getFileId()
                )))
                .flatMap(userFileMetadata -> validateUserPermission(user, userFileMetadata).then(isFileOrFolder(userFileMetadata, true)))
                .flatMap(userFileMetadata -> {
                    if (fileEditDTO.getParentFolderId() != null) {
                        return checkParentFolderId(fileEditDTO.getParentFolderId(), user).then(Mono.just(userFileMetadata));
                    }
                    return Mono.just(userFileMetadata);
                })
                .flatMap(userFileMetadata -> {
                    userFileMetadata.setFilename(fileEditDTO.getFileName());
                    userFileMetadata.setParentFolderId(fileEditDTO.getParentFolderId());
                    userFileMetadata.setSharedWithUsers(fileEditDTO.getShareUserIds());
                    userFileMetadata.setLastAccessTime(LocalDateTime.now());
                    return userFileMetaRepository.save(userFileMetadata).then();
                });
    }

    /**
     * 刪除文件夾的共通實現
     *
     * @param fileId 文件ID
     * @param user   用戶信息
     *
     * @return Mono<Void>
     */
    @Override
    public Mono<Void> deleteFolder(String fileId, User user) {
        return userFileMetaRepository
                .findById(fileId)
                .switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.NOT_EXISTING_USER_FILE, fileId)))
                .flatMap(userFileMetadata -> validateUserPermission(user, userFileMetadata).then(isFileOrFolder(userFileMetadata, true)))
                .flatMap(userFileMetadata -> {
                    List<UserFileMetadata> userFileList = new ArrayList<>(List.of(userFileMetadata));
                    List<Long> parentFolderIdList = new ArrayList<>(List.of(userFileMetadata.getId()));
                    Mono<Void> result = deleteFolderRecursive(user, parentFolderIdList, userFileList);
                    return result.then(updateOwner(userFileList, user)).then(userFileMetaRepository.delete(userFileMetadata));
                });
    }

    /**
     * 檢查父文件夾的實體是否存在以及相關用戶的權限
     * 當檔案不存在或是用戶無權限時，拋出ValidationException
     * @param parentFolderId 父文件夾ID
     * @param user          用戶信息
     * @return Mono<Void>
     */
    private Mono<Void> checkParentFolderId(Long parentFolderId, User user) {
        return userFileMetaRepository
                .findById(parentFolderId.toString())
                .switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.NOT_EXISTING_USER_FILE, parentFolderId)))
                .flatMap(parentFolder -> validateUserPermission(user, parentFolder).then(isFileOrFolder(parentFolder, true)))
                .switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.PERMISSION_DENIED, parentFolderId)))
                .then();
    }

    /**
     * 檢驗用戶檔案權限的共通實現，此為重載方法默認限定只有擁有者才有權限
     *
     * @param user             用戶信息
     * @param userFileMetadata 用戶文件元數據
     *
     * @return Mono<Void>
     */
    protected Mono<Void> validateUserPermission(User user, UserFileMetadata userFileMetadata) {
        return validateUserPermission(user, userFileMetadata, true);
    }

    /**
     * 關聯用戶與文件的共通實現
     *
     * @param serverFileMetadataId 服務器文件ID
     * @param fileMetadataDTO      文件元數據
     *
     * @return Mono<UserFileMetadata>
     */
    protected Mono<UserFileMetadata> associateUserFile(Long serverFileMetadataId, FileMetadataDTO fileMetadataDTO) {
        UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setUserId(fileMetadataDTO.getUserId());
        userFileMetadata.setServerFileId(serverFileMetadataId);
        userFileMetadata.setFilename(fileMetadataDTO.getFileName());
        userFileMetadata.setParentFolderId(fileMetadataDTO.getParentFolderId());
        userFileMetadata.setUploadTime(LocalDateTime.now());
        userFileMetadata.setLastAccessTime(LocalDateTime.now());
        return userFileMetaRepository.save(userFileMetadata);
    }



    /**
     * 初始化上傳任務，若需要則返回Mono<String> taskId
     *
     * @param fileMetadataDTO 檔案元數據
     *
     * @return Mono<UploadResponseDTO>
     */
    protected Mono<UploadResponseDTO> initialUpload(FileMetadataDTO fileMetadataDTO) {
        String uploadTaskId = UUID.randomUUID().toString();
        return transfersTasksManager.registerUploadTask(fileMetadataDTO, uploadTaskId).flatMap(isRegisterSuccess -> {
            if (isRegisterSuccess) {
                UploadTaskBO task = fileMetadataDTO.formatToTransferTask(uploadTaskId, "初始化任務成功");
                String key = "upload_task:" + uploadTaskId;
                int totalChunks = getTotalChunks(fileMetadataDTO.getFileSize());

                return redisProvider
                        .setHashMap(key, "DTO", task, 6, ChronoUnit.HOURS)
                        .then(redisProvider.setHashMap(key, "uploaded_count", 0, 6, ChronoUnit.HOURS))
                        .then(redisProvider.setHashMap(key, "total_chunks", totalChunks, 6, ChronoUnit.HOURS))
                        .then(redisProvider.generateChunkSet(key + ":pending_chunks", totalChunks))
                        .then(Mono.just(UploadResponseDTO
                                                .builder()
                                                .transferTaskId(uploadTaskId)
                                                .totalChunks(totalChunks)
                                                .chunkSize(CHUNK_SIZE)
                                                .progress(0.0)
                                                .isSuccess(true)
                                                .isFinished(false)
                                                .message("初始化任務成功")
                                                .build()));
            } else {
                String md5 = fileMetadataDTO.getMd5();
                return Mono.error(new ValidationException(ValidationException.ErrorCode.EXISTING_TRANSFER_TASK,
                                                          md5,
                                                          transfersTasksManager
                                                                  .getTransfersTask(md5, TransfersStatusEnum.UPLOADING)
                                                                  .getFirst()
                                                                  .getTransferTaskId()
                ));
            }
        });
    }

    /**
     * 獲取總分塊數的共通實現
     *
     * @param fileSize 文件大小
     *
     * @return 總分塊數
     */
    protected int getTotalChunks(long fileSize) {
        return (int) Math.ceil((double) fileSize / CHUNK_SIZE);
    }

    private Mono<UserFileMetadata> isFileOrFolder(UserFileMetadata userFileMetadata, boolean isFolder) {
        if (userFileMetadata.getIsFolder() == isFolder) {
            return Mono.just(userFileMetadata);
        }
        ValidationException.ErrorCode errorCode = isFolder ? ValidationException.ErrorCode.THIS_OBJECT_ID_NOT_FOLDER : ValidationException.ErrorCode.THIS_OBJECT_ID_NOT_FILE;
        return Mono.error(new ValidationException(errorCode, userFileMetadata.getId()));
    }

    /**
     * 驗證用戶權限的抽象方法
     *
     * @param user             用戶信息
     * @param userFileMetadata 用戶文件元數據
     *
     * @return Mono<Void>
     */
    protected Mono<Void> validateUserPermission(User user, UserFileMetadata userFileMetadata, boolean ownerOnly) {
        boolean isOwner = userFileMetadata.getUserId().equals(user.getId());
        boolean isShared = !ownerOnly && userFileMetadata.getSharedWithUsers().contains(user.getId());
        if (isOwner || isShared) {
            return Mono.empty();
        }
        return Mono.error(new ValidationException(ValidationException.ErrorCode.PERMISSION_DENIED, userFileMetadata.getId()));
    }

    /**
     * 更新文件擁有者的共通實現
     * 若文件只有一個擁有者，則將文件的擁有者列表中移除用戶ID
     *
     * @param userFileMetadata 文件元數據
     * @param user             用戶信息
     *
     * @return Mono<Void>
     */
    private Mono<Void> updateOwner(UserFileMetadata userFileMetadata, User user) {
        return Mono.defer(() -> {
            if (userFileMetadata.getIsFolder()) {
                return Mono.empty();
            }
            return userFileMetaRepository
                    .countByServerFileIdAndUserId(userFileMetadata.getServerFileId(), user.getId(), databaseClient)
                    .flatMap(c -> {
                        if (c == 1) {
                            return serverFileMetaRepository
                                    .findById(userFileMetadata.getServerFileId().toString())
                                    .flatMap(serverFileMetadata -> {
                                        serverFileMetadata.getOwners().remove(user.getId());
                                        serverFileMetadata.setLastAccessTime(LocalDateTime.now());
                                        return serverFileMetaRepository.save(serverFileMetadata).then();
                                    });
                        }
                        return Mono.empty();
                    });
        });
    }
    public FileEnum detectFileType(byte[] fileBytes) {
        String mimeType = tika.detect(fileBytes);
        return FileEnum.fromMimeType(mimeType);
    }

    /**
     * 合併分塊數據的共通實現
     *
     * @param byteArrays 分塊數據
     *
     * @return Mono<byte [ ]>
     */
    protected Mono<byte[]> combineBytes(List<byte[]> byteArrays) {
        return Mono.fromCallable(() -> {
            int totalLength = byteArrays.stream().mapToInt(bytes -> bytes.length).sum();
            ByteBuffer buffer = ByteBuffer.allocate(totalLength);
            byteArrays.forEach(buffer::put);
            return buffer.array();
        });
    }

    /**
     * 刪除暫存分塊數據的共通實現
     *
     * @param uploadTaskBO 傳輸任務
     *
     * @return Mono<Void>
     */
    protected Mono<Void> removeTempData(UploadTaskBO uploadTaskBO) {
        String key = "upload_task:" + uploadTaskBO.getTransferTaskId();
        return redisProvider
                .getHashMap(key, "total_chunks")
                .map(Integer.class::cast)
                .flatMapMany(totalChunks -> Flux
                        .range(1, totalChunks)
                        .flatMap(i -> gridFsProvider.deleteFileByFilename(uploadTaskBO.getTransferTaskId() + "_chunk_" + i))
                        .then(redisProvider.deleteHash(key).then(redisProvider.deleteSet(key + ":pending_chunks"))))
                .then();
    }

    /**
     * 處理MD5校驗成功後的文件存儲邏輯的共通實現
     *
     * @param uploadTaskBO  任務
     * @param combinedBytes 合併後的數據
     *
     * @return Mono<Void>
     */
    protected Mono<Void> processFileAfterMd5Check(UploadTaskBO uploadTaskBO, byte[] combinedBytes) {
        return gridFsProvider
                .storeFile(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(combinedBytes)),
                           String.format("%s_output", uploadTaskBO.getTransferTaskId())
                )
                .flatMap(fileGridFsId -> {
                    ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
                    serverFileMetadata.setFileSize(uploadTaskBO.getFileSize());
                    serverFileMetadata.setFileType(uploadTaskBO.getFileType());
                    serverFileMetadata.setMd5(uploadTaskBO.getMd5());
                    serverFileMetadata.setGridFsId(fileGridFsId.toHexString());
                    serverFileMetadata.setUploadTime(LocalDateTime.now());
                    serverFileMetadata.setLastAccessTime(LocalDateTime.now());
                    serverFileMetadata.getOwners().add(uploadTaskBO.getUserId());
                    transfersTasksManager
                            .finishTransfersTask(uploadTaskBO.getMd5(), uploadTaskBO.getTransferTaskId(), fileGridFsId.toHexString())
                            .subscribeOn(Schedulers.boundedElastic())
                            .subscribe();

                    return serverFileMetaRepository
                            .save(serverFileMetadata)
                            .flatMap(serverFile -> associateUserFile(serverFile.getId(), uploadTaskBO.formatToFileMetadata()).flatMap(
                                    userFileMetadata -> userFileMetaRepository.save(userFileMetadata).then(removeTempData(uploadTaskBO))));
                });
    }

    /**
     * 合併已上傳的文件分塊的共通實現
     *
     * @param transferTaskId 任務ID
     * @param totalChunks    總分塊數
     *
     * @return Mono<ObjectId>
     */
    protected Mono<ObjectId> combineChunks(String transferTaskId, int totalChunks) {
        record ChunkData(int index, byte[] data) {
            static Comparator<Object> comparator() {
                return Comparator.comparingInt(a -> ((ChunkData) a).index);
            }
        }

        String key = "upload_task:" + transferTaskId;
        return redisProvider.getHashMap(key, "DTO").cast(UploadTaskBO.class).flatMap(uploadTaskBO -> Mono.defer(() -> {
            Flux<byte[]> chunkFiles = Flux
                    .range(1, totalChunks)
                    .parallel(transfersTasksManager.getAvailableThreadCount())
                    .runOn(Schedulers.boundedElastic())
                    .flatMap(index -> CircuitBreakerOperator
                            .of(CircuitBreaker.of("chunkProcessor", this.circuitBreakerConfig))
                            .apply(this.gridFsProvider
                                           .findFileByFileName(transferTaskId + "_chunk_" + index)
                                           .flatMap(this.gridFsProvider::getResource)
                                           .flatMap(resource -> DataBufferUtils
                                                   .join(resource.getDownloadStream())
                                                   .onErrorResume(e -> Mono.error(new ProcessException(ProcessException.ErrorCode.CANNOT_GET_FILE_STREAM,
                                                                                                       transferTaskId + "_chunk_" + index
                                                   )))
                                                   .map(dataBuffer -> {
                                                       try {
                                                           byte[] bytes = new byte[dataBuffer.readableByteCount()];
                                                           dataBuffer.read(bytes);
                                                           return new ChunkData(index, bytes);
                                                       } finally {
                                                           DataBufferUtils.release(dataBuffer);
                                                       }
                                                   }))))
                    .sequential()
                    .sort(ChunkData.comparator())
                    .cast(ChunkData.class)
                    .map((ChunkData -> ChunkData.data));
            return chunkFiles.collectList().flatMap(this::combineBytes).flatMap(combinedBytes -> {
                String computedChunkMd5 = DigestUtils.md5Hex(combinedBytes);
                if (!uploadTaskBO.getMd5().equals(computedChunkMd5)) {
                    transfersTasksManager
                            .updateTransfersTask(uploadTaskBO.getMd5(),
                                                 uploadTaskBO.getTransferTaskId(),
                                                 TransfersStatusEnum.FAILED,
                                                 "MD5校驗失敗",
                                                 null,
                                                 false
                            )
                            .subscribeOn(Schedulers.boundedElastic())
                            .subscribe();
                    return Mono.defer(() -> removeTempData(uploadTaskBO).then(Mono.error(new ProcessException(ProcessException.ErrorCode.MD5_NOT_MATCH))));
                }

                uploadTaskBO.setFileType(detectFileType(combinedBytes));
                processFileAfterMd5Check(uploadTaskBO, combinedBytes).subscribeOn(Schedulers.boundedElastic()).subscribe();
                return Mono.just(new ObjectId());

            }).flatMap(id -> removeTempData(uploadTaskBO).thenReturn(id));
        }));
    }

    /**
     * 處理文件分塊的共通實現
     *
     * @param uploadChunkDTO  上傳文件數據
     * @param transferTaskId  任務ID
     * @param key             任務Key
     * @param pendingChunkKey 待處理文件分塊Key
     *
     * @return Mono<UploadResponseDTO>
     */
    protected Mono<UploadResponseDTO> processChunk(UploadChunkDTO uploadChunkDTO, String transferTaskId, String key, String pendingChunkKey) {
        int chunkIndex = uploadChunkDTO.getChunkIndex();
        int totalChunks = uploadChunkDTO.getTotalChunks();

        DataBufferFactory dataBufferFactory = new DefaultDataBufferFactory();
        Flux<DataBuffer> chunkData = Flux.just(dataBufferFactory.wrap(uploadChunkDTO.getChunkData()));

        return redisProvider
                .deleteSet(pendingChunkKey, chunkIndex)
                .then(gridFsProvider.storeFile(chunkData, transferTaskId + "_chunk_" + chunkIndex))
                .then(redisProvider.incrementHashMap(key, "uploaded_count", 1, 1, ChronoUnit.HOURS))
                .flatMap(uploadCount -> {
                    Long uploadCountLong = (Long) uploadCount;
                    double progress = (uploadCountLong.doubleValue() / totalChunks) * 100.0;
                    String message = String.format("文件分塊: %d 上傳成功", chunkIndex);

                    UploadResponseDTO responseDTO = UploadResponseDTO
                            .builder()
                            .chunkIndex(chunkIndex)
                            .transferTaskId(transferTaskId)
                            .progress(progress)
                            .isSuccess(true)
                            .isFinished(false)
                            .message(message).totalChunks(totalChunks)
                            .build();

                    if (uploadCountLong.intValue() == totalChunks) {
                        combineChunks(transferTaskId, totalChunks).subscribeOn(Schedulers.boundedElastic()).subscribe();
                        responseDTO.setIsFinished(true);
                    }
                    return Mono.just(responseDTO);
                })
                .onErrorResume(e -> redisProvider
                        .setSet(pendingChunkKey, chunkIndex)
                        .then(redisProvider.getHashMap(key, "uploaded_count").map(count -> {
                            long uploadCountLong = Long.parseLong(count.toString());
                            double progress = ((double) uploadCountLong / totalChunks) * 100.0;
                            return UploadResponseDTO
                                    .builder()
                                    .chunkIndex(chunkIndex)
                                    .transferTaskId(transferTaskId)
                                    .progress(progress)
                                    .isSuccess(false)
                                    .isFinished(false)
                                    .message("文件分塊上傳失敗")
                                    .totalChunks(totalChunks)
                                    .build();
                        })));
    }

    /**
     * 遞歸刪除文件夾的共通實現
     *
     * @param user             用戶信息
     * @param parentFolderIdList 父文件夾ID列表
     * @param serverFileList   服務器文件列表
     *
     * @return Mono<Void>
     */
    private Mono<Void> deleteFolderRecursive(User user, List<Long> parentFolderIdList, List<UserFileMetadata> serverFileList) {
        return findFileWithSameParentFolderId(user.getId(), parentFolderIdList, serverFileList).flatMap(nextParentFolderIds -> {
            if (nextParentFolderIds.isEmpty()) {
                return Mono.empty();
            }
            return deleteFolderRecursive(user, nextParentFolderIds, serverFileList);
        });
    }

    /**
     * 查找與父文件夾ID相同的文件的共通實現
     *
     * @param userId          用戶ID
     * @param parentFolderId 父文件夾ID
     * @param serverFileList  服務器文件列表
     *
     * @return Mono<List<Long>>
     */
    private Mono<List<Long>> findFileWithSameParentFolderId(Long userId, List<Long> parentFolderId, List<UserFileMetadata> serverFileList) {
        return userFileMetaRepository.findAllByUserIdAndParentFolderIdIn(userId, parentFolderId).collectList().map(userFileMetadataList -> {
            serverFileList.addAll(userFileMetadataList);
            return userFileMetadataList
                    .stream()
                    .filter(UserFileMetadata::getIsFolder)
                    .map(UserFileMetadata::getId)
                    .collect(Collectors.toList());
        }).switchIfEmpty(Mono.just(Collections.emptyList()));
    }

    /**
     * 更新文件擁有者的共通實現
     *
     * @param userFileMetadataList 用戶文件元數據列表
     * @param user                用戶信息
     *
     * @return Mono<Void>
     */
    private Mono<Void> updateOwner(List<UserFileMetadata> userFileMetadataList, User user) {
        return Flux.fromIterable(userFileMetadataList).flatMap(userFileMetadata -> updateOwner(userFileMetadata, user)).then();
    }

    /**
     * 創建一個新的用戶文件元數據實體
     *
     * @return 返回一個新的實體對象
     */
    @Override
    public Mono<UserFileMetadata> createUserFileMetadata() {
        return null;
    }

    /**
     * 根據ID獲取一個用戶文件元數據實體
     *
     * @param id 服務器文件元數據ID
     *
     * @return 用戶文件元數據實體對象
     */
    @Override
    public Mono<UserFileMetadata> getUserFileMetadataById(Long id) {
        if (id == null) {
            return Mono.empty();
        }
        return userFileMetaRepository.findById(id.toString());
    }

    /**
     * 獲取所有用戶文件元數據實體
     */
    @Override
    public Flux<UserFileMetadata> getAllUserFileMetadata() {
        return null;
    }

    /**
     * 更新一個用戶文件元數據實體
     *
     * @param entity 用戶文件元數據實體對象
     */
    @Override
    public Mono<Void> updateUserFileMetadata(UserFileMetadata entity) {
        return null;
    }

    /**
     * 刪除一個用戶文件元數據實體
     *
     * @param entity 用戶文件元數據實體對象
     */
    @Override
    public Mono<Void> deleteUserFileMetadata(UserFileMetadata entity) {
        return null;
    }

    /**
     * 創建一個新的服務器文件元數據實體
     *
     * @return 返回一個新的服務器文件元數據實體對象
     */
    @Override
    public Mono<ServerFileMetadata> createServerFileMetadata() {
        return null;
    }

    /**
     * 根據ID獲取一個服務器文件元數據實體
     *
     * @param id 服務器文件元數據ID
     *
     * @return 服務器文件元數據實體對象
     */
    @Override
    public Mono<ServerFileMetadata> getByServerFileMetadataId(Long id) {
        return null;
    }

    /**
     * 獲取所有服務器文件元數據實體
     */
    @Override
    public Flux<ServerFileMetadata> getAllServerFileMetadata() {
        return null;
    }

    /**
     * 更新一個服務器文件元數據實體
     *
     * @param entity 服務器文件元數據實體對象
     */
    @Override
    public Mono<ServerFileMetadata> updateServerFileMetadata(ServerFileMetadata entity) {
        return null;
    }

    /**
     * 刪除一個服務器文件元數據實體
     *
     * @param entity 服務器文件元數據實體對象
     */
    @Override
    public Mono<ServerFileMetadata> deleteServerFileMetadata(ServerFileMetadata entity) {
        return null;
    }
}
