package xyz.dowob.filemanagement.service.ServiceImpl;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.digest.DigestUtils;
import org.bson.types.ObjectId;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import xyz.dowob.filemanagement.annotation.FileHandlerType;
import xyz.dowob.filemanagement.component.manager.TransfersTasksManager;
import xyz.dowob.filemanagement.component.provider.providerImpl.GridFsProvider;
import xyz.dowob.filemanagement.component.provider.providerImpl.RedisProvider;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.customenum.TransfersStatusEnum;
import xyz.dowob.filemanagement.dto.file.*;
import xyz.dowob.filemanagement.entity.ServerFileMetadata;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.entity.UserFileMetadata;
import xyz.dowob.filemanagement.exception.ProcessException;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.repostiory.ServerFileMetaRepository;
import xyz.dowob.filemanagement.repostiory.UserFileMetaRepository;
import xyz.dowob.filemanagement.service.ServiceInterface.AbstractFileService;

import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 圖片文件業務邏輯實現類，實現接口 @see {@link AbstractFileService}
 * 主要實現圖片文件的上傳、下載、刪除等業務邏輯
 *
 * @author yuan
 * @program FileManagement
 * @ClassName ImageFileServiceImpl
 * @description
 * @create 2024-09-27 00:48
 * @Version 1.0
 **/

@RequiredArgsConstructor
@FileHandlerType(FileEnum.IMAGE)
@Log4j2
@Service
public class ImageFileServiceImpl extends AbstractFileService {
    /**
     * 服務器文件元數據庫操作對象
     * 用於操作ServerFileMetadata實體與數據庫的轉換
     */
    private final ServerFileMetaRepository serverFileMetaRepository;

    /**
     * 用戶文件元數據庫操作對象
     * 用於操作UserFileMetadata實體與數據庫的轉換
     */
    private final UserFileMetaRepository userFileMetaRepository;

    /**
     * Redis數據庫操作對象
     */
    private final RedisProvider redisProvider;

    /**
     * GridFs數據庫操作對象
     */
    private final GridFsProvider gridFsProvider;

    /**
     * 文件傳輸任務管理器
     */
    private final TransfersTasksManager transfersTasksManager;

    /**
     * 文件屬性配置
     */
    private final FileProperties fileProperties;

    private Long CHUNK_SIZE;

    @PostConstruct
    public void init() {
        CHUNK_SIZE = (long) fileProperties.getUpload().getChunkSize() * 1024 * 1024;
    }


    /**
     * 獲取用戶文件列表的接口
     *
     * @param user 用戶信息
     *
     * @return 返回用戶文件列表
     */
    //todo 優化只顯示當前目錄下的文件，未來改到FileService中
    @Override
    public Flux<UserFileListDTO> getUserFileList(User user) {
        return userFileMetaRepository.findAllByUserId(user.getId()).collectList().flatMapMany(userFileMetadataList -> {
            Set<Long> serverFileIds = userFileMetadataList.stream().map(UserFileMetadata::getServerFileId).collect(Collectors.toSet());
            if (serverFileIds.isEmpty()) {
                return Flux.empty();
            }
            return serverFileMetaRepository.findAllByIdIn(serverFileIds)
                    .collectMap(ServerFileMetadata::getId)
                    .flatMapMany(serverFileMetadataMap -> Flux.fromIterable(userFileMetadataList).map(userFileMetadata -> {
                        ServerFileMetadata serverFileMetadata = serverFileMetadataMap.get(userFileMetadata.getServerFileId());
                        return new UserFileListDTO(serverFileMetadata, userFileMetadata, user);
                    }));
        });
    }

    /**
     * 上傳文件的接口
     *
     * @param fileMetadata 文件元數據
     *                     包含文件名、文件大小、文件類型等信息
     * @param user         用戶信息
     *
     * @return Mono<ResponseEntity < ?>> 返回上傳結果
     */
    @Override
    public Mono<UploadResponseDTO> uploadFile(FileMetadata fileMetadata, User user) {
        fileMetadata.setUserId(user.getId());
        return serverFileMetaRepository.findByMd5(fileMetadata.getMd5()).flatMap(existingFile -> {
            existingFile.getOwners().add(user.getId());
            return serverFileMetaRepository
                    .save(existingFile)
                    .then(associateUserFile(existingFile.getId(), fileMetadata).flatMap(userFileMetadata -> userFileMetaRepository
                            .save(userFileMetadata)
                            .then(Mono.just(new UploadResponseDTO(null, null, null, null, 100.0, true, true, "上傳成功")))));
        }).switchIfEmpty(initialUpload(fileMetadata));
    }


    /**
     * 初始化上傳任務，若需要則返回Mono<String> taskId
     *
     * @param fileMetadata 檔案元數據
     *
     * @return Mono<String> taskId
     */
    @Override
    protected Mono<UploadResponseDTO> initialUpload(FileMetadata fileMetadata) {
        String uploadTaskId = UUID.randomUUID().toString();
        return transfersTasksManager.registerUploadTask(fileMetadata, uploadTaskId).flatMap(isRegisterSuccess -> {
            if (isRegisterSuccess) {
                TransferTaskDTO task = fileMetadata.formatToTransferTask(uploadTaskId, "初始化任務成功");
                String key = "upload_task:" + uploadTaskId;
                int totalChunks = getTotalChunks(fileMetadata.getFileSize());

                return redisProvider
                        .setHashMap(key, "DTO", task, 6, ChronoUnit.HOURS)
                        .then(redisProvider.setHashMap(key, "uploaded_count", 0, 6, ChronoUnit.HOURS))
                        .then(redisProvider.setHashMap(key, "total_chunks", totalChunks, 6, ChronoUnit.HOURS))
                        .then(redisProvider.generateChunkSet(key + ":pending_chunks", totalChunks))
                        .then(Mono.just(new UploadResponseDTO(uploadTaskId, totalChunks, CHUNK_SIZE, null, 0.0, true, false, "初始化任務成功"
                        )));
            } else {
                String md5 = fileMetadata.getMd5();
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
     * 上傳文件分塊
     *
     * @param uploadChunkDTO 上傳文件數據
     *
     * @return Mono<TransferResponseDTO> 上傳結果
     */
    // todo 對失敗的分塊進行重試
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
                            UploadResponseDTO responseDTO = generateResponseDTO(uploadChunkDTO.getChunkIndex(),
                                                                                transferTaskId,
                                                                                progress,
                                                                                true,
                                                                                false,
                                                                                String.format("文件分塊: %d 已上傳",
                                                                                              uploadChunkDTO.getChunkIndex()
                                                                                ),
                                                                                uploadChunkDTO.getTotalChunks()
                            );
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
     * 合併已上傳的文件分塊
     *
     * @param transferTaskId 任務ID
     *
     * @return Mono<Void>
     */
    @Override
    protected Mono<ObjectId> combineChunks(String transferTaskId, int totalChunks) {
        return redisProvider.getHashMap("upload_task:" + transferTaskId, "DTO").flatMap(task -> {
            TransferTaskDTO transferTaskDTO = (TransferTaskDTO) task;
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
                    if (!transferTaskDTO.getMd5().equals(computedChunkMd5)) {
                        log.error("任務:{} MD5校驗失敗", transferTaskId);
                        transfersTasksManager
                                .updateTransfersTask(transferTaskDTO.getMd5(),
                                                     transferTaskDTO.getTransferTaskId(),
                                                     TransfersStatusEnum.FAILED,
                                                     "MD5校驗失敗",
                                                     null,
                                                     false
                                )
                                .subscribeOn(Schedulers.boundedElastic())
                                .subscribe();
                        return Mono.defer(() -> removeTempData(transferTaskDTO).then(Mono.error(new ProcessException(ProcessException.ErrorCode.MD5_NOT_MATCH))));
                    } else {
                        log.info("任務:{} MD5校驗成功 {}", transferTaskId, computedChunkMd5);
                        processFileAfterMd5Check(transferTaskDTO, combinedBytes).subscribeOn(Schedulers.boundedElastic()).subscribe();
                        return Mono.just(new ObjectId());
                    }
                }).flatMap(id -> removeTempData(transferTaskDTO).thenReturn(id));
            });
        });
    }

    /**
     * 關聯用戶與文件
     *
     * @param serverFileMetadataId 任務ID
     * @param fileMetadata         用戶信息
     *
     * @return Mono<String> 文件ID
     */
    @Override
    protected Mono<UserFileMetadata> associateUserFile(Long serverFileMetadataId, FileMetadata fileMetadata) {
        UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setUserId(fileMetadata.getUserId());
        userFileMetadata.setServerFileId(serverFileMetadataId);
        userFileMetadata.setFilename(fileMetadata.getFileName());
        userFileMetadata.setFilePath(fileMetadata.formatFilePath(fileMetadata.getFilePath(), fileMetadata.getFileName()));
        userFileMetadata.setUploadTime(LocalDateTime.now());
        userFileMetadata.setLastAccessTime(LocalDateTime.now());
        return userFileMetaRepository.save(userFileMetadata);
    }

    /**
     * 驗證用戶權限，對用戶當前的操作進行權限校驗
     * 若用戶擁有權限則返回Mono.empty()，否則返回Mono.error()
     * 此處用戶擁有權限的條件為用戶擁有文件或文件被分享給用戶
     *
     * @param user             用戶信息
     * @param userFileMetadata 用戶文件元數據
     */
    @Override
    protected Mono<Void> validateUserPermission(User user, UserFileMetadata userFileMetadata) {
        if (userFileMetadata.getUserId().equals(user.getId()) || userFileMetadata.getSharedWithUsers().contains(user.getId())) {
            return Mono.empty();
        } else {
            return Mono.error(new ValidationException(ValidationException.ErrorCode.PERMISSION_DENIED,
                                                      user.getId(),
                                                      userFileMetadata.getId()
            ));
        }
    }

    private int getTotalChunks(long fileSize) {
        long chunkSize = (long) fileProperties.getUpload().getChunkSize() * 1024 * 1024;
        return (int) Math.ceil((double) fileSize / chunkSize);
    }

    /**
     * 處理文件分塊，將文件分塊存儲到GridFs數據庫中，當所有文件分塊上傳完成後，合併文件分塊
     *
     * @param uploadChunkDTO  上傳文件數據
     * @param transferTaskId  任務ID
     * @param key             任務Key
     * @param pendingChunkKey 待處理文件分塊Key
     *
     * @return Mono<TransferResponseDTO> 上傳結果
     */
    private Mono<UploadResponseDTO> processChunk(UploadChunkDTO uploadChunkDTO, String transferTaskId, String key, String pendingChunkKey) {
        int chunkIndex = uploadChunkDTO.getChunkIndex();
        int totalChunks = uploadChunkDTO.getTotalChunks();

        DataBufferFactory dataBufferFactory = new DefaultDataBufferFactory();
        Flux<DataBuffer> chunkData = Flux.just(dataBufferFactory.wrap(uploadChunkDTO.getChunkData()));

        log.debug("任務:{} 上傳文件分塊: {} / {}", transferTaskId, chunkIndex, totalChunks);


        return Mono.defer(() -> redisProvider
                .deleteSet(pendingChunkKey, chunkIndex)
                .then(gridFsProvider.storeFile(chunkData, transferTaskId + "_chunk_" + chunkIndex))
                .flatMap(id -> redisProvider.incrementHashMap(key, "uploaded_count", 1, 1, ChronoUnit.HOURS))
                .flatMap(uploadCount -> {
                    Long uploadCountLong = (Long) uploadCount;
                    double progress = (uploadCountLong.doubleValue() / totalChunks) * 100.0;
                    String message = String.format("文件分塊: %d 上傳成功", chunkIndex);
                    log.debug("任務ID: {}, 目前進度: {}", transferTaskId, progress);
                    UploadResponseDTO responseDTO = generateResponseDTO(chunkIndex,
                                                                        transferTaskId,
                                                                        progress,
                                                                        true,
                                                                        false,
                                                                        message,
                                                                        uploadChunkDTO.getTotalChunks()
                    );
                    if (uploadCountLong.intValue() == totalChunks) {
                        combineChunks(transferTaskId, uploadChunkDTO.getTotalChunks()).subscribeOn(Schedulers.boundedElastic()).subscribe();
                        responseDTO.setIsFinished(true);
                    }
                    return Mono.just(responseDTO);
                })
                .onErrorResume(Exception.class, e -> {
                    log.error("任務:{} 上傳文件分塊: {} / {} 失敗", transferTaskId, chunkIndex, totalChunks, e);
                    return redisProvider
                            .setSet(pendingChunkKey, chunkIndex)
                            .then(redisProvider.getHashMap(key, "uploaded_count"))
                            .flatMap(currentCount -> {
                                long uploadCountLong = Long.parseLong(currentCount.toString());
                                double progress = ((double) uploadCountLong / totalChunks) * 100.0;
                                return Mono.just(generateResponseDTO(chunkIndex,
                                                                     transferTaskId,
                                                                     progress,
                                                                     false, false, "文件分塊上傳失敗", uploadChunkDTO.getTotalChunks()
                                ));
                            });
                }));
    }

    /**
     * 生成回應的傳輸對象
     *
     * @param chunkIndex     文件分塊索引
     * @param transferTaskId 任務ID
     * @param progress       進度
     * @param isSuccess      是否成功
     * @param isFinished     是否完成
     * @param message        消息
     *
     * @return TransferResponseDTO
     */
    private UploadResponseDTO generateResponseDTO(int chunkIndex, String transferTaskId, double progress, boolean isSuccess, boolean isFinished, String message, int totalChunk) {
        UploadResponseDTO responseDTO = new UploadResponseDTO();
        responseDTO.setChunkIndex(chunkIndex);
        responseDTO.setTransferTaskId(transferTaskId);
        responseDTO.setProgress(progress);
        responseDTO.setIsSuccess(isSuccess);
        responseDTO.setIsFinished(isFinished);
        responseDTO.setMessage(message);
        responseDTO.setTotalChunks(totalChunk);
        responseDTO.setChunkSize(CHUNK_SIZE);
        return responseDTO;
    }

    /**
     * 合併分塊數據
     *
     * @param byteArrays 分塊數據
     *
     * @return Mono<byte [ ]> 合併後的數據
     */
    private Mono<byte[]> combineBytes(List<byte[]> byteArrays) {
        return Mono.fromCallable(() -> {
            int totalLength = byteArrays.stream().mapToInt(bytes -> bytes.length).sum();
            ByteBuffer buffer = ByteBuffer.allocate(totalLength);
            byteArrays.forEach(buffer::put);
            return buffer.array();
        });
    }

    /**
     * 下載文件，根據文件ID獲取文件數據流並返回
     *
     * @param fileId 文件ID
     * @param user   用戶信息
     *
     * @return Flux<DataBuffer> 文件數據流
     */
    public Flux<DataBuffer> downloadFile(String fileId, User user) {
        return userFileMetaRepository
                .findById(fileId)
                .<DataBuffer>flatMapMany(userFileMetadata -> validateUserPermission(user, userFileMetadata).thenMany(
                        serverFileMetaRepository
                                .findById(userFileMetadata.getServerFileId().toString())
                                .flatMapMany(serverFileMetadata -> gridFsProvider
                                        .findFileById(new ObjectId(serverFileMetadata.getGridFsId()))
                                        .flatMapMany(gridFsFile -> gridFsProvider
                                                .getResource(gridFsFile)
                                                .flatMapMany(resource -> resource.getDownloadStream().map(dataBuffer -> {
                                                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                                                    dataBuffer.read(bytes);
                                                    DataBufferUtils.release(dataBuffer);
                                                    return DefaultDataBufferFactory.sharedInstance.wrap(bytes);
                                                })))
                                        .switchIfEmpty(Flux.error(new ProcessException(ProcessException.ErrorCode.GRIDFS_FILE_NOT_FOUND,
                                                                                       serverFileMetadata.getId()
                                        ))))
                                .switchIfEmpty(Flux.error(new ProcessException(ProcessException.ErrorCode.USER_HAVE_NOT_EXIST_SERVER_FILE,
                                                                               userFileMetadata.getServerFileId(),
                                                                               fileId
                                )))))
                .switchIfEmpty(Flux.error(new ValidationException(ValidationException.ErrorCode.NOT_EXISTING_USER_FILE, fileId)));
    }

    /**
     * 刪除暫存分塊數據，包括GridFs數據庫中的分塊數據和Redis數據庫中的任務數據
     *
     * @param transferTaskDTO 傳輸任務
     *
     * @return Mono<Void>
     */
    //todo 合併完成後清除記憶體中的數據
    private Mono<Void> removeTempData(TransferTaskDTO transferTaskDTO) {
        String key = "upload_task:" + transferTaskDTO.getTransferTaskId();
        return redisProvider
                .getHashMap(key, "total_chunks")
                .map(Integer.class::cast)
                .flatMapMany(totalChunks -> Flux
                        .range(1, totalChunks)
                        .flatMap(i -> gridFsProvider.deleteFileByFilename(transferTaskDTO.getTransferTaskId() + "_chunk_" + i))
                        .then(redisProvider.deleteHash(key).then(redisProvider.deleteSet(key + ":pending_chunks"))))
                .then();
    }

    /**
     * 根據文件名獲取文件類型
     *
     * @param fileName 文件名
     *
     * @return String 文件類型
     */
    private String getExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(lastDotIndex);
    }

    /**
     * MD5校驗成功後處理文件，將文件存儲到GridFs數據庫中，並將文件元數據存儲到數據庫中
     *
     * @param transferTaskDTO 任務
     * @param combinedBytes   合併後的數據
     *
     * @return Mono<Void>
     */
    private Mono<Void> processFileAfterMd5Check(TransferTaskDTO transferTaskDTO, byte[] combinedBytes) {
        return gridFsProvider
                .storeFile(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(combinedBytes)),
                           String.format("%s_output", transferTaskDTO.getTransferTaskId())
                )
                .flatMap(fileGridFsId -> {
                    ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
                    serverFileMetadata.setFileSize(transferTaskDTO.getFileSize());
                    serverFileMetadata.setFileType(FileEnum.IMAGE);
                    serverFileMetadata.setMd5(transferTaskDTO.getMd5());
                    serverFileMetadata.setGridFsId(fileGridFsId.toHexString());
                    serverFileMetadata.setUploadTime(LocalDateTime.now());
                    serverFileMetadata.setLastAccessTime(LocalDateTime.now());
                    serverFileMetadata.getOwners().add(transferTaskDTO.getUserId());
                    transfersTasksManager
                            .finishTransfersTask(transferTaskDTO.getMd5(), transferTaskDTO.getTransferTaskId(), fileGridFsId.toHexString())
                            .subscribeOn(Schedulers.boundedElastic())
                            .subscribe();

                    return serverFileMetaRepository
                            .save(serverFileMetadata)
                            .flatMap(serverFile -> associateUserFile(serverFile.getId(), transferTaskDTO.formatToFileMetadata()).flatMap(
                                    userFileMetadata -> userFileMetaRepository
                                            .save(userFileMetadata)
                                            .then(removeTempData(transferTaskDTO))));
                });
    }
}
