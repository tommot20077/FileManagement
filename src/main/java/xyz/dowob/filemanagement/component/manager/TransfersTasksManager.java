package xyz.dowob.filemanagement.component.manager;

import jakarta.annotation.PreDestroy;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.customenum.TransfersStatusEnum;
import xyz.dowob.filemanagement.data.file.dto.FileMetadataDTO;
import xyz.dowob.filemanagement.entity.TransfersTask;
import xyz.dowob.filemanagement.exception.ProcessException;
import xyz.dowob.filemanagement.repostiory.TransfersTasksRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
@Log4j2
public class TransfersTasksManager {
    /**
     * 用於存儲正在進行的傳輸任務，key 為檔案的 MD5 值value 為檔案的傳輸任務Map
     * 傳輸任務Map的key 為任務ID，value 為傳輸任務
     */
    private final ConcurrentHashMap<String, Map<String, TransfersTask>> activeTransfersTask = new ConcurrentHashMap<>();

    /**
     * TransfersTasksRepository 用於操作傳輸任務的數據庫操作接口
     */
    private final TransfersTasksRepository transfersTasksRepository;

    /**
     * 用於構造 TransfersTasksManager 對象
     *
     * @param transfersTasksRepository TransfersTasksRepository 用於操作傳輸任務的數據庫操作接口
     */
    public TransfersTasksManager (TransfersTasksRepository transfersTasksRepository) {
        this.transfersTasksRepository = transfersTasksRepository;
    }

    /**
     * 用於註冊一個新的上傳任務
     *
     * @param fileMetadataDTO   檔案的元數據
     * @param transferTaskId 任務ID
     *
     * @return Mono<Boolean> 返回一個 Mono 對象，當註冊成功時返回 true，否則返回 false
     */
    public Mono<Boolean> registerUploadTask(FileMetadataDTO fileMetadataDTO, String transferTaskId) {
        return Mono.defer(() -> {
            if (activeTransfersTask.containsKey(fileMetadataDTO.getMd5())) {
                Map<String, TransfersTask> transfersTaskMap = activeTransfersTask.get(fileMetadataDTO.getMd5());
                transfersTaskMap.forEach((key, value) -> {
                    if (value.getStatus() == TransfersStatusEnum.UPLOADING) {
                        log.debug("發現相同檔案正在上傳，MD5: {}, 現有任務ID: {}, 重複任務ID: {}", fileMetadataDTO.getMd5(),
                                  value.getTransferTaskId(),
                                  transferTaskId
                        );
                    }
                });
                return Mono.just(false);
            }
            return createTransfersTask(fileMetadataDTO, transferTaskId, TransfersStatusEnum.UPLOADING).thenReturn(true);
        });
    }

    /**
     * 用於註冊一個新的轉換任務
     *
     * @param fileMetadataDTO   檔案的元數據
     * @param transferTaskId 任務ID
     *
     * @return Mono<Boolean> 返回一個 Mono 對象，當註冊成功時返回 true，否則返回 false
     */
    public Mono<Void> createTransfersTask(FileMetadataDTO fileMetadataDTO, String transferTaskId, TransfersStatusEnum status) {
        return createTransfersTask(fileMetadataDTO, transferTaskId, null, FileEnum.IMAGE, null, status);
        // todo 硬編碼
    }

    /**
     * 用於創建一個新的傳輸任務，當任務創建成功時，將任務存入 activeTransfersTask 中以及數據庫中
     *
     * @param fileMetadataDTO   檔案的元數據
     * @param transferTaskId 任務ID
     * @param status         任務狀態
     *
     * @return Mono<Void> 返回一個 Mono 對象
     */
    public Mono<Void> createTransfersTask(FileMetadataDTO fileMetadataDTO, String transferTaskId, String gridFsId, FileEnum fileType, String message, TransfersStatusEnum status) {
        TransfersTask transfersTask = new TransfersTask();
        transfersTask.setTransferTaskId(transferTaskId);
        transfersTask.setMd5(fileMetadataDTO.getMd5());
        transfersTask.setGridFsId(gridFsId);
        transfersTask.setFileSize(fileMetadataDTO.getFileSize());
        transfersTask.setFileType(fileType);
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
     * @return Mono<Void> 返回一個 Mono 對象
     */
    public Mono<Void> updateTransfersTask (String md5, String transfersTaskId, TransfersStatusEnum status, String message, String gridFsId, Boolean isFinished) {
        TransfersTask transfersTask = activeTransfersTask.get(md5).get(transfersTaskId);
        if (transfersTask == null) {
            log.error("無法找到MD5為{}的任務", md5);
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
        return transfersTasksRepository.save(transfersTask).then();
    }

    /**
     * 用於完成一個傳輸任務時，更新任務的狀態
     *
     * @param md5             檔案的 MD5 值
     * @param transfersTaskId 任務ID
     * @param gridFsId        GridFS 檔案ID
     *
     * @return Mono<Void> 返回一個 Mono 對象
     */
    public Mono<Void> finishTransfersTask (String md5, String transfersTaskId, String gridFsId) {
        return updateTransfersTask(md5,
                                   transfersTaskId,
                                   TransfersStatusEnum.COMPLETED,
                                   "檔案處理成功",
                                   gridFsId,
                                   true
        ).doOnSuccess(aVoid -> activeTransfersTask.remove(md5));
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
    public List<TransfersTask> getTransfersTask (String md5, TransfersStatusEnum status) {
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
     * 用於移除一個檔案的傳輸任務，當該檔案的所有任務都被移除時，將從 activeTransfersTask 中移除
     *
     * @param md5             檔案的 MD5 值
     * @param transfersTaskId 任務ID
     *
     * @return Mono<Void> 返回一個 Mono 對象
     */
    public Mono<Void> removeTransfersTask (String md5, String transfersTaskId) {
        if (activeTransfersTask.containsKey(md5)) {
            activeTransfersTask.get(md5).remove(transfersTaskId);
            if (activeTransfersTask.get(md5).isEmpty()) {
                activeTransfersTask.remove(md5);
            }
        }
        return Mono.empty();
    }

    /**
     * 用於在應用關閉時，將所有未完成的任務設置為失敗
     *
     * @return Mono<Void> 返回一個 Mono 對象
     */
    @PreDestroy
    public Mono<Void> destroy () {
        List<TransfersStatusEnum> status = new ArrayList<>();
        status.add(TransfersStatusEnum.COMPLETED);
        status.add(TransfersStatusEnum.FAILED);
        return transfersTasksRepository.findAllByStatusNotIn(status).collectList().map(unfinishedTasks -> {
            if (unfinishedTasks.isEmpty()) {
                return Mono.empty();
            }
            unfinishedTasks.stream().peek(transfersTask -> {
                transfersTask.setStatus(TransfersStatusEnum.FAILED);
                transfersTask.setFinishTime(LocalDateTime.now());
                transfersTask.setMessage("伺服器關閉，任務被取消");
            }).forEach(transfersTasksRepository::save);
            return Mono.empty();
        }).then();
    }
}
