package xyz.dowob.filemanagement.service.ServiceInterface;

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
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 檔案服務的抽象類，包含共通的上傳、下載邏輯
 */
@RequiredArgsConstructor
public abstract class AbstractFileService implements FileService {
    protected final ServerFileMetaRepository serverFileMetaRepository;
    protected final UserFileMetaRepository userFileMetaRepository;
    protected final RedisProvider redisProvider;
    protected final GridFsProvider gridFsProvider;
    protected final TransfersTasksManager transfersTasksManager;
    protected final FileProperties fileProperties;
    protected final DatabaseClient databaseClient;

    private final Tika tika = new Tika();
    protected Long CHUNK_SIZE;

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
    @Override
    public Flux<UserFileListDTO> getUserFileList(User user) {
        return userFileMetaRepository.findAllByUserId(user.getId()).collectList().flatMapMany(userFileMetadataList -> {
            Set<Long> serverFileIds = userFileMetadataList.stream().map(UserFileMetadata::getServerFileId).collect(Collectors.toSet());
            if (serverFileIds.isEmpty()) {
                return Flux.empty();
            }
            return serverFileMetaRepository
                    .findAllByIdIn(serverFileIds)
                    .collectMap(ServerFileMetadata::getId)
                    .flatMapMany(serverFileMetadataMap -> Flux.fromIterable(userFileMetadataList).map(userFileMetadata -> {
                        ServerFileMetadata serverFileMetadata = serverFileMetadataMap.get(userFileMetadata.getServerFileId());
                        return new UserFileListDTO(serverFileMetadata, userFileMetadata);
                    }));
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
        return serverFileMetaRepository.findByMd5(fileMetadataDTO.getMd5()).flatMap(existingFile -> {
            existingFile.getOwners().add(user.getId());
            existingFile.setLastAccessTime(LocalDateTime.now());
            return serverFileMetaRepository
                    .save(existingFile)
                    .then(associateUserFile(existingFile.getId(), fileMetadataDTO).flatMap(userFileMetadata -> userFileMetaRepository
                            .save(userFileMetadata)
                            .then(Mono.just(UploadResponseDTO
                                                    .builder()
                                                    .progress(100.0)
                                                    .isSuccess(true)
                                                    .isFinished(true)
                                                    .message("上傳成功")
                                                    .build()))));
        }).switchIfEmpty(initialUpload(fileMetadataDTO));
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
                    } else {
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
                    }
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
    @Override
    public Mono<UserFileDataBO> downloadFile(String fileId, User user) {
        return userFileMetaRepository
                .findById(fileId)
                .switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.NOT_EXISTING_USER_FILE, fileId)))
                .flatMap(userFileMetadata -> validateUserPermission(user, userFileMetadata, false).then(Mono.just(userFileMetadata)))
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

    public FileEnum detectFileType(byte[] fileBytes) {
        String mimeType = tika.detect(fileBytes);
        return FileEnum.fromMimeType(mimeType);
    }

    /**
     * 驗證用戶權限的抽象方法
     *
     * @param user             用戶信息
     * @param userFileMetadata 用戶文件元數據
     *
     * @return Mono<Void>
     */
    protected Mono<Void> validateUserPermission(User user, UserFileMetadata userFileMetadata, boolean OwnerOnly) {
        if (userFileMetadata.getUserId().equals(user.getId()) || (!OwnerOnly && userFileMetadata
                .getSharedWithUsers()
                .contains(user.getId()))) {
            return Mono.empty();
        }
        return Mono.error(new ValidationException(ValidationException.ErrorCode.PERMISSION_DENIED, userFileMetadata.getId()));

    }

    @Override
    public Mono<Void> deleteFile(String fileId, User user) {
        return userFileMetaRepository
                .findById(fileId)
                .switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.NOT_EXISTING_USER_FILE, fileId)))
                .flatMap(userFileMetadata -> validateUserPermission(user, userFileMetadata).then(Mono.just(userFileMetadata)))
                .flatMap(userFileMetadata -> userFileMetaRepository
                        .countByServerFileIdAndUserId(userFileMetadata.getServerFileId(), user.getId(), databaseClient)
                        .flatMap(c -> {
                            if (c == 1) {
                                return serverFileMetaRepository
                                        .findById(userFileMetadata.getServerFileId().toString())
                                        .flatMap(serverFileMetadata -> {
                                            serverFileMetadata.getOwners().remove(user.getId());
                                            serverFileMetadata.setLastAccessTime(LocalDateTime.now());
                                            return serverFileMetaRepository
                                                    .save(serverFileMetadata)
                                                    .then(userFileMetaRepository.deleteById(fileId));
                                        });
                            } else {
                                return userFileMetaRepository.deleteById(fileId);
                            }
                        }));
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
        userFileMetadata.setFilePath(fileMetadataDTO.formatFilePath(fileMetadataDTO.getFilePath(), fileMetadataDTO.getFileName()));
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

    /**
     * 合併已上傳的文件分塊的共通實現
     *
     * @param transferTaskId 任務ID
     * @param totalChunks    總分塊數
     *
     * @return Mono<ObjectId>
     */
    protected Mono<ObjectId> combineChunks(String transferTaskId, int totalChunks) {
        return redisProvider.getHashMap("upload_task:" + transferTaskId, "DTO").flatMap(task -> {
            UploadTaskBO uploadTaskBO = (UploadTaskBO) task;
            return Mono.defer(() -> {
                record ChunkData(int index, byte[] data) {
                }

                Flux<byte[]> chunkFiles = Flux
                        .range(1, totalChunks)
                        .parallel(Runtime.getRuntime().availableProcessors())
                        .runOn(Schedulers.boundedElastic())
                        .flatMap(index -> gridFsProvider
                                .findFileByFileName(transferTaskId + "_chunk_" + index)
                                .flatMap(gridFsProvider::getResource)
                                .flatMap(resource -> DataBufferUtils.join(resource.getDownloadStream()).map(dataBuffer -> {
                                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                                    dataBuffer.read(bytes);
                                    DataBufferUtils.release(dataBuffer);
                                    return new ChunkData(index, bytes);
                                })))
                        .sequential()
                        .sort(Comparator.comparingInt(ChunkData::index))
                        .map(ChunkData::data);
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
            });
        });
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

        return Mono.defer(() -> redisProvider
                .deleteSet(pendingChunkKey, chunkIndex)
                .then(gridFsProvider.storeFile(chunkData, transferTaskId + "_chunk_" + chunkIndex))
                .flatMap(id -> redisProvider.incrementHashMap(key, "uploaded_count", 1, 1, ChronoUnit.HOURS))
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
                            .message(message)
                            .totalChunks(uploadChunkDTO.getTotalChunks())
                            .build();
                    if (uploadCountLong.intValue() == totalChunks) {
                        combineChunks(transferTaskId, uploadChunkDTO.getTotalChunks()).subscribeOn(Schedulers.boundedElastic()).subscribe();
                        responseDTO.setIsFinished(true);
                    }
                    return Mono.just(responseDTO);
                })
                .onErrorResume(Exception.class,
                               e -> redisProvider
                                       .setSet(pendingChunkKey, chunkIndex)
                                       .then(redisProvider.getHashMap(key, "uploaded_count"))
                                       .flatMap(currentCount -> {
                                           long uploadCountLong = Long.parseLong(currentCount.toString());
                                           double progress = ((double) uploadCountLong / totalChunks) * 100.0;
                                           UploadResponseDTO responseDTO = UploadResponseDTO
                                                   .builder()
                                                   .chunkIndex(chunkIndex)
                                                   .transferTaskId(transferTaskId)
                                                   .progress(progress)
                                                   .isSuccess(false)
                                                   .isFinished(false)
                                                   .message("文件分塊上傳失敗")
                                                   .totalChunks(uploadChunkDTO.getTotalChunks())
                                                   .build();
                                           return Mono.just(responseDTO);
                                       })
                ));
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
     * 驗證用戶權限的重寫方法，默認開啟擁有者限定
     *
     * @param user             用戶信息
     * @param userFileMetadata 用戶文件元數據
     *
     * @return Mono<Void>
     */
    protected Mono<Void> validateUserPermission(User user, UserFileMetadata userFileMetadata) {
        return validateUserPermission(user, userFileMetadata, true);
    }

    @Override
    public Mono<Void> editFile(FileEditDTO fileEditDTO, User user) {
        return userFileMetaRepository
                .findById(fileEditDTO.getFileId())
                .switchIfEmpty(Mono.error(new ValidationException(ValidationException.ErrorCode.NOT_EXISTING_USER_FILE,
                                                                  fileEditDTO.getFileId()
                )))
                .flatMap(userFileMetadata -> validateUserPermission(user, userFileMetadata).then(Mono.just(userFileMetadata)))
                .flatMap(userFileMetadata -> {
                    userFileMetadata.setFilename(fileEditDTO.getFileName());
                    userFileMetadata.setFilePath(fileEditDTO.getFilePath());
                    userFileMetadata.setSharedWithUsers(fileEditDTO.getShareUserIds());
                    userFileMetadata.setLastAccessTime(LocalDateTime.now());
                    return userFileMetaRepository.save(userFileMetadata).then();
                });
    }
}