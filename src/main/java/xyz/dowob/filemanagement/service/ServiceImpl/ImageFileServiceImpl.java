package xyz.dowob.filemanagement.service.ServiceImpl;

import com.mongodb.client.gridfs.model.GridFSFile;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.digest.DigestUtils;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.ReactiveGridFsTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.FileHandlerType;
import xyz.dowob.filemanagement.component.provider.providerImpl.RedisProvider;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.dto.file.FileMetadata;
import xyz.dowob.filemanagement.dto.file.TransferResponseDTO;
import xyz.dowob.filemanagement.dto.file.TransferTask;
import xyz.dowob.filemanagement.dto.file.UploadChunkDTO;
import xyz.dowob.filemanagement.entity.ServerFileMetadata;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.entity.UserFileMetadata;
import xyz.dowob.filemanagement.repostiory.ServerFileMetaRepository;
import xyz.dowob.filemanagement.repostiory.UserFileMetaRepository;
import xyz.dowob.filemanagement.service.ServiceInterface.AbstractFileService;
import xyz.dowob.filemanagement.service.ServiceInterface.FileService;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * @author yuan
 * @program FileManagement
 * @ClassName ImageFileServiceImpl
 * @description
 * @create 2024-09-27 00:48
 * @Version 1.0
 **/
@Service
@RequiredArgsConstructor
@FileHandlerType(FileEnum.IMAGE)
@Log4j2
public class ImageFileServiceImpl extends AbstractFileService {
    private final ServerFileMetaRepository serverFileMetaRepository;

    private final UserFileMetaRepository userFileMetaRepository;

    private final RedisProvider redisProvider;

    private final ReactiveGridFsTemplate gridFsTemplate;


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
    public Mono<TransferResponseDTO> uploadFile(FileMetadata fileMetadata, User user) {
        fileMetadata.setUserId(user.getId());
        return serverFileMetaRepository
                .findByMd5(fileMetadata.getMd5())
                .flatMap(existingFile -> associateUserFile(existingFile.getId(),
                                                           fileMetadata).flatMap(userFileMetadata -> userFileMetaRepository
                        .save(userFileMetadata)
                        .flatMap(userFile -> Mono.just(new TransferResponseDTO(null, null, 100.0, true, true, "上傳成功")))))
                .switchIfEmpty(initialUpload(fileMetadata));
    }


    /**
     * 初始化上傳任務，若需要則返回Mono<String> taskId
     *
     * @param fileMetadata 檔案元數據
     *
     * @return Mono<String> taskId
     */

    // todo 補上FileMetadata驗證
    // todo 全局列隊避免重複
    @Override
    protected Mono<TransferResponseDTO> initialUpload(FileMetadata fileMetadata) {
        String uploadTaskId = UUID.randomUUID().toString();
        TransferTask task = fileMetadata.formatToTransferTask(uploadTaskId, "初始化任務成功");

        String key = "upload_task:" + uploadTaskId;
        return redisProvider
                .setHashMap(key, "DTO", task, 1, ChronoUnit.HOURS)
                .then(redisProvider.setHashMap(key, "uploaded_count", 0, 1, ChronoUnit.HOURS))
                .then(redisProvider.generateChunkSet(key + ":pending_chunks", fileMetadata.getTotalChunks()))
                .then(Mono.just(new TransferResponseDTO(uploadTaskId, null, 0.0, true, false, "初始化任務成功")));
    }

    /**
     * 上傳文件分塊
     *
     * @param uploadChunkDTO 上傳文件數據
     *
     * @return Mono<TransferResponseDTO> 上傳結果
     */
    @SneakyThrows
    @Override
    public Mono<TransferResponseDTO> uploadFileChunk(UploadChunkDTO uploadChunkDTO) {
        String transferTaskId = uploadChunkDTO.getTransferTaskId();
        int chunkIndex = uploadChunkDTO.getChunkIndex();
        int totalChunks = uploadChunkDTO.getTotalChunks();

        String receivedChunkMd5 = uploadChunkDTO.getMd5();
        DataBufferFactory dataBufferFactory = new DefaultDataBufferFactory();
        Flux<DataBuffer> chunkData = Flux.just(dataBufferFactory.wrap(uploadChunkDTO.getChunkData()));


        String computedChunkMd5 = DigestUtils.md5Hex(uploadChunkDTO.getChunkData());
        if (!receivedChunkMd5.equals(computedChunkMd5)) {
            log.error("任務:{} 上傳文件分塊: {} / {} 失敗，MD5校驗失敗", transferTaskId, chunkIndex, totalChunks);
            return Mono.just(new TransferResponseDTO(transferTaskId, chunkIndex, 0.0, false, false, "MD5校驗失敗"));
        } else {
            log.info("任務:{} 上傳文件分塊: {} / {} MD5校驗成功 {}", transferTaskId, chunkIndex, totalChunks, receivedChunkMd5);
        }

        log.info("任務:{} 上傳文件分塊: {} / {}", transferTaskId, chunkIndex, totalChunks);

        String key = "upload_task:" + transferTaskId;
        return Mono.defer(() -> redisProvider
                .incrementHashMap(key, "uploaded_count", 1, 1, ChronoUnit.HOURS)
                .flatMap(uploadCount -> redisProvider
                        .deleteSet(key + ":pending_chunks", chunkIndex)
                        .then(gridFsTemplate.store(chunkData, transferTaskId + "_chunk_" + chunkIndex).flatMap(id -> {
                            Long uploadCountLong = (Long) uploadCount;
                            TransferResponseDTO responseDTO = new TransferResponseDTO();
                            responseDTO.setChunkIndex(chunkIndex);
                            responseDTO.setTransferTaskId(transferTaskId);
                            responseDTO.setProgress((uploadCountLong / totalChunks) * 100.0);
                            responseDTO.setIsSuccess(true);

                            if (uploadCountLong.intValue() == totalChunks) {
                                return combineChunks(transferTaskId, totalChunks).flatMap(outputId -> {
                                    responseDTO.setIsFinished(true);
                                    responseDTO.setMessage(String.format("文件分塊: %d 上傳成功", chunkIndex));
                                    return Mono.just(responseDTO);
                                });
                            }
                            responseDTO.setIsFinished(false);
                            responseDTO.setMessage(String.format("文件分塊: %d 上傳成功", chunkIndex));
                            return Mono.just(responseDTO);
                        }))
                        .onErrorResume(Exception.class, e -> {
                            log.error("任務:{} 上傳文件分塊: {} / {} 失敗", transferTaskId, chunkIndex, totalChunks, e);
                            Long uploadCountLong = (Long) uploadCount;
                            TransferResponseDTO responseDTO = new TransferResponseDTO();
                            responseDTO.setChunkIndex(chunkIndex);
                            responseDTO.setTransferTaskId(transferTaskId);
                            responseDTO.setProgress((uploadCountLong / totalChunks) * 100.0);
                            responseDTO.setIsSuccess(false);
                            responseDTO.setIsFinished(false);
                            responseDTO.setMessage(String.format("文件分塊: %d 上傳失敗", chunkIndex));
                            return redisProvider.setSet(key + ":pending_chunks", chunkIndex).then(Mono.just(responseDTO));
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
            TransferTask transferTask = (TransferTask) task;
            return Mono.defer(() -> {
                String outputFileName = String.format("%s_output%s", transferTaskId, getExtension(transferTask.getFileName()));
                Flux<GridFSFile> chunkFiles = Flux
                        .range(1, totalChunks)
                        .concatMap(index -> gridFsTemplate.findOne(Query.query(Criteria
                                                                                       .where("filename")
                                                                                       .is(transferTaskId + "_chunk_" + index))));
                return chunkFiles
                        .concatMap(gridFsTemplate::getResource)
                        .concatMap(resource -> DataBufferUtils.join(resource.getDownloadStream()).map(dataBuffer -> {
                            byte[] bytes = new byte[dataBuffer.readableByteCount()];
                            dataBuffer.read(bytes);
                            DataBufferUtils.release(dataBuffer);
                            return bytes;
                        }))
                        .reduce(new byte[0], (acc, bytes) -> {
                            byte[] newBytes = new byte[acc.length + bytes.length];
                            System.arraycopy(acc, 0, newBytes, 0, acc.length);
                            System.arraycopy(bytes, 0, newBytes, acc.length, bytes.length);
                            return newBytes;
                        })
                        .flatMap(combinedBytes -> gridFsTemplate
                                .store(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(combinedBytes)), outputFileName)
                                .flatMap(fileGridFsId -> {
                                    ServerFileMetadata serverFileMetadata = new ServerFileMetadata();
                                    serverFileMetadata.setFileSize(transferTask.getFileSize());
                                    serverFileMetadata.setFileType(FileEnum.IMAGE);
                                    serverFileMetadata.setMd5(transferTask.getMd5());
                                    serverFileMetadata.setGridFsId(fileGridFsId.toHexString());
                                    serverFileMetadata.setUploadTime(LocalDateTime.now());
                                    serverFileMetadata.setLastAccessTime(LocalDateTime.now());
                                    return serverFileMetaRepository
                                            .save(serverFileMetadata)
                                            .flatMap(serverFile -> associateUserFile(serverFile.getId(),
                                                                                     transferTask.formatToFileMetadata()).flatMap(
                                                    userFileMetadata -> userFileMetaRepository
                                                            .save(userFileMetadata)
                                                            .then(removeTempData(transferTask).thenReturn(fileGridFsId))));


                                }))
                        .flatMap(id -> removeTempData(transferTask).thenReturn(id));
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
        userFileMetadata.setFilePath(fileMetadata.getFilePath());
        userFileMetadata.setUploadTime(LocalDateTime.now());
        userFileMetadata.setLastAccessTime(LocalDateTime.now());
        return userFileMetaRepository.save(userFileMetadata);
    }

    private Mono<Void> removeTempData(TransferTask transferTask) {
        String key = "upload_task:" + transferTask.getTransferTaskId();
        return Flux
                .range(1, transferTask.getTotalChunks())
                .flatMap(i -> gridFsTemplate.delete(Query.query(Criteria
                                                                        .where("filename")
                                                                        .is(transferTask.getTransferTaskId() + "_chunk_" + i))))
                .then(redisProvider.deleteHash(key).then(redisProvider.deleteSet(key + ":pending_chunks")));
    }

    private String getExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(lastDotIndex);
    }
}