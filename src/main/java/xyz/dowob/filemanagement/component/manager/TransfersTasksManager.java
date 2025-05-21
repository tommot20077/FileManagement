package xyz.dowob.filemanagement.component.manager;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.RecordLevel;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.customenum.LogLevelEnum;
import xyz.dowob.filemanagement.customenum.TransfersStatusEnum;
import xyz.dowob.filemanagement.data.file.dto.FileMetadataDTO;
import xyz.dowob.filemanagement.entity.TransfersTask;
import xyz.dowob.filemanagement.exception.ProcessException;
import xyz.dowob.filemanagement.exception.ValidationException;
import xyz.dowob.filemanagement.repostiory.TransfersTasksRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 此類用於管理檔案上傳任務，管理全局任務的進度以及狀態
 *
 * @author yuan
 * @program FileManagement
 * @ClassName TransfersTasksManager
 * @description
 * @create 2024-12-10 22:47
 * @Version 1.0
 **/
@Component
public class TransfersTasksManager {
    /**
     * 用於存儲正在進行的傳輸任務，key 為檔案的 MD5 值，value 為檔案的傳輸任務Map
     * 傳輸任務Map的key 為任務ID，value 為傳輸任務
     */
    private final ConcurrentHashMap<String, Map<String, TransfersTask>> activeTransfersTask = new ConcurrentHashMap<>();

    /**
     * TransfersTasksRepository 用於操作傳輸任務的數據庫操作接口
     */
    private final TransfersTasksRepository transfersTasksRepository;

    /**
     * FileProperties 用於操作文件上傳相關配置的類
     */
    private final FileProperties fileProperties;

    /**
     * 用於構造 TransfersTasksManager 對象
     *
     * @param transfersTasksRepository TransfersTasksRepository 用於操作傳輸任務的數據庫操作接口
     * @param fileProperties           FileProperties 用於操作文件上傳相關配置的類
     */
    public TransfersTasksManager(TransfersTasksRepository transfersTasksRepository, FileProperties fileProperties) {
        this.transfersTasksRepository = transfersTasksRepository;
        this.fileProperties = fileProperties;
    }


    /**
     * 用於註冊一個新的上傳任務
     * 將檢查任務是否已經存在，如果存在，則拋出異常
     * 並且檢查檔案大小是否超過限制，如果超過限制，則拋出異常
     * 不然的話，則創建一個新的上傳任務
     *
     * @param fileMetadataDTO 檔案的元數據
     * @param transferTaskId  任務ID
     *
     * @return Mono<Void>
     */
    @RecordLevel(LogLevelEnum.DEBUG)
    public Mono<Void> registerUploadTask(FileMetadataDTO fileMetadataDTO, String transferTaskId) {
        return Mono.defer(() -> {
            String md5 = fileMetadataDTO.getMd5();
            if (activeTransfersTask.containsKey(fileMetadataDTO.getMd5())) {
                String alreadyTransferTaskId = this.getTransfersTask(md5, TransfersStatusEnum.UPLOADING).getFirst().getTransferTaskId();
                ValidationException error = new ValidationException(ValidationException.ErrorCode.EXISTING_TRANSFER_TASK, md5, alreadyTransferTaskId);
                return Mono.error(error);
            }
            boolean isLimitSize = fileProperties.getUpload().getMaxUploadFileSize().toBytes() > 0;
            if (isLimitSize && fileMetadataDTO.getFileSize() > fileProperties.getUpload().getMaxUploadFileSize().toBytes()) {
                ValidationException error = new ValidationException(ValidationException.ErrorCode.FILE_SIZE_LIMIT,
                                                                    fileMetadataDTO.getFileSize(),
                                                                    fileProperties.getUpload().getMaxUploadFileSize()
                );
                return Mono.error(error);
            }
            return createTransfersTask(fileMetadataDTO, transferTaskId, TransfersStatusEnum.UPLOADING);
        });
    }

    /**
     * 用於獲取一個檔案的所有傳輸任務，可以根據任務狀態進行過濾
     * 當檔案不存在時，返回空列表
     *
     * @param md5    檔案的 MD5 值
     * @param status 任務狀態，當為 null 時，不進行過濾
     *
     * @return List<TransfersTask> 返回一個包含所有符合條件的傳輸任務的列表
     */
    public List<TransfersTask> getTransfersTask(String md5, TransfersStatusEnum status) {
        if (activeTransfersTask.containsKey(md5)) {
            Map<String, TransfersTask> transfersTaskMap = activeTransfersTask.get(md5);
            List<TransfersTask> result = new ArrayList<>();
            transfersTaskMap.forEach((key, value) -> {
                if (status == null || value.getStatus() == status) {
                    result.add(value);
                }
            });
            return result;
        }
        return Collections.emptyList();
    }

    /**
     * 用於註冊一個新的轉換任務
     *
     * @param fileMetadataDTO 檔案的元數據
     * @param transferTaskId  任務ID
     * @param status          任務狀態
     *
     * @return Mono<Boolean> 返回一個 Mono 對象，當註冊成功時返回 true，否則返回 false
     */
    public Mono<Void> createTransfersTask(FileMetadataDTO fileMetadataDTO, String transferTaskId, TransfersStatusEnum status) {
        return createTransfersTask(fileMetadataDTO, transferTaskId, null, null, status);
    }

    /**
     * 用於創建一個新的傳輸任務，當任務創建成功時，將任務存入 activeTransfersTask 中以及數據庫中
     *
     * @param fileMetadataDTO 檔案的元數據
     * @param transferTaskId  任務ID
     * @param gridFsId        GridFS 檔案ID
     * @param message         任務消息
     * @param status          任務狀態
     *
     * @return Mono<Void>
     */
    public Mono<Void> createTransfersTask(FileMetadataDTO fileMetadataDTO, String transferTaskId, String gridFsId, String message, TransfersStatusEnum status) {
        TransfersTask transfersTask = new TransfersTask();
        transfersTask.setTransferTaskId(transferTaskId);
        transfersTask.setMd5(fileMetadataDTO.getMd5());
        transfersTask.setGridFsId(gridFsId);
        transfersTask.setFileSize(fileMetadataDTO.getFileSize());
        transfersTask.setStartTime(LocalDateTime.now());
        transfersTask.setMessage(message);
        transfersTask.setStatus(status);
        activeTransfersTask.put(fileMetadataDTO.getMd5(), Map.of(transferTaskId, transfersTask));
        return transfersTasksRepository.save(transfersTask).then();
    }

    /**
     * 用於更新一個傳輸任務的狀態
     *
     * @param md5             檔案的 MD5 值
     * @param transfersTaskId 任務ID
     * @param status          任務狀態
     * @param message         任務消息
     * @param gridFsId        GridFS 檔案ID
     * @param isFinished      是否完成
     *
     * @return Mono<Void>
     */
    @RecordLevel(LogLevelEnum.DEBUG)
    public Mono<Void> updateTransfersTask(String md5, String transfersTaskId, TransfersStatusEnum status, String message, String gridFsId, Boolean isFinished) {
        TransfersTask transfersTask = Optional.ofNullable(activeTransfersTask.get(md5)).map(map -> map.get(transfersTaskId)).orElse(null);
        if (transfersTask == null) {
            return Mono.error(new ProcessException(ProcessException.ErrorCode.NOT_EXISTING_MD5_TRANSFERS_TASK, md5));
        }
        transfersTask.setStatus(status);
        if (message != null) {
            transfersTask.setMessage(message);
        }
        if (isFinished) {
            transfersTask.setFinishTime(LocalDateTime.now());
        }
        if (gridFsId != null) {
            transfersTask.setGridFsId(gridFsId);
        }
        return transfersTasksRepository.save(transfersTask).flatMap(task -> {
            if (isFinished) {
                activeTransfersTask.remove(md5);
            }
            return Mono.empty();
        });
    }

    /**
     * 獲取當前可用的線程數量
     * 此方法獲取可用線程數量的計算方式為：最大線程數 - 正在進行的任務數量
     * 並且最小為 1，最大為配置文件中的 combineProcessCountLimit
     *
     * @return int 返回一個整數，表示可用線程數量
     */
    @Deprecated
    public int getAvailableThreadCount() {
        return Math.min(Math.max((Runtime.getRuntime().availableProcessors() - activeTransfersTask.size()), 1),
                        fileProperties.getUpload().getCombineProcessCountLimit()
        );
    }


    /**
     * 用於在應用關閉時，將所有未完成的任務設置為失敗
     *
     * @return Mono<Void> 返回一個 Mono 對象
     */
    @PreDestroy
    @RecordLevel(LogLevelEnum.DEBUG)
    public Mono<Void> destroy() {
        List<TransfersStatusEnum> status = new ArrayList<>();
        status.add(TransfersStatusEnum.UPLOADING);
        status.add(TransfersStatusEnum.DOWNLOADING);
        return transfersTasksRepository.findAllByStatusIn(status).collectList().flatMap(unfinishedTasks -> {
            if (unfinishedTasks.isEmpty()) {
                return Mono.empty();
            }
            for (TransfersTask unfinishedTask : unfinishedTasks) {
                unfinishedTask.setStatus(TransfersStatusEnum.FAILED);
                unfinishedTask.setFinishTime(LocalDateTime.now());
                unfinishedTask.setMessage("伺服器關閉，任務被取消");
            }
            return transfersTasksRepository.saveAll(unfinishedTasks).then();
        });
    }
}
