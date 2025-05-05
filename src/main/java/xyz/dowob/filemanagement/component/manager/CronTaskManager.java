package xyz.dowob.filemanagement.component.manager;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import xyz.dowob.filemanagement.annotation.RecordLevel;
import xyz.dowob.filemanagement.component.provider.providerImplement.JwtTokenProviderImpl;
import xyz.dowob.filemanagement.component.strategy.CsrfTokenRepositoryStrategy;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.customenum.LogLevelEnum;
import xyz.dowob.filemanagement.customenum.TransfersStatusEnum;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.repostiory.*;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 定時任務管理器，將伺服器上的定時任務放在這裡
 *
 * @author yuan
 * @program FileManagement
 * @ClassName CronTaskManager
 * @create 2025/1/26
 * @Version 1.0
 **/
@Component
@RecordLevel(LogLevelEnum.INFO)
@RequiredArgsConstructor
public class CronTaskManager {
    /**
     * JWT 憑證提供者
     */
    private final JwtTokenProviderImpl jwtTokenProvider;

    /**
     * 用戶資料庫操作類
     */
    private final UserRepository userRepository;

    /**
     * 用戶檔案元數據庫操作類
     */
    private final UserFileMetaRepository userFileMetaRepository;

    /**
     * 伺服器檔案元數據庫操作類
     */
    private final ServerFileMetaRepository serverFileMetaRepository;

    /**
     * 檔案回收站紀錄資料庫操作類
     */
    private final FileTrashRecordRepository fileTrashRecordRepository;

    /**
     * 事務操作器
     */
    private final TransactionalOperator transactionalOperator;

    /**
     * CSRF 憑證儲存庫策略
     */
    private final CsrfTokenRepositoryStrategy csrfTokenRepositoryStrategy;

    /**
     * 檔案傳輸任務管理器
     */
    private final TransfersTasksManager transfersTasksManager;

    /**
     * 檔案傳輸任務資料庫操作類
     */
    private final TransfersTasksRepository transfersTasksRepository;

    /**
     * 檔案配置類
     */
    private final FileProperties fileProperties;


    /**
     * 清理過期的 JWT緩存憑證
     * 每 6 小時執行一次
     */
    @Scheduled(cron = "0 0 */6 * * ?")
    public void deleteExpiredToken() {
        jwtTokenProvider.getCacheTokenMap().forEach((k, v) -> {
            if (v.expireTime().before(new Date())) {
                jwtTokenProvider.getCacheTokenMap().remove(k);
            }
        });
    }


    /**
     * 檢查並更新用戶的存儲使用量
     * 每 6 小時執行一次
     */
    @Scheduled(cron = "0 30 */6 * * ?")
    public void checkUserStorageLimit() {
        userRepository.findAll().flatMap(this::calculateUserStorageLimit).subscribeOn(Schedulers.boundedElastic()).subscribe();
    }


    /**
     * 刪除過期的檔案並更新用戶的存儲使用量
     * 每 1 小時執行一次
     */
    @Scheduled(cron = "0 0 */1 * * ?")
    public void deleteExpiredFile() {
        LocalDateTime now = LocalDateTime.now();
        userRepository
                .findAll()
                .flatMap(user -> fileTrashRecordRepository
                        .findAllByUserIdAndDeleteTimeBefore(user.getId(), now)
                        .map(fileTrashRecord -> fileTrashRecord.getFileId().toString())
                        .collectList()
                        .flatMap(fileIdList -> {
                            if (fileIdList.isEmpty()) {
                                return Mono.empty();
                            }
                            return transactionalOperator.transactional(userFileMetaRepository.deleteAllById(fileIdList));
                        }).then(calculateUserStorageLimit(user))).subscribeOn(Schedulers.boundedElastic())
                .subscribe();
    }


    /**
     * 計算用戶的存儲使用量
     *
     * @param user 用戶
     *
     * @return 用戶
     */
    private Mono<User> calculateUserStorageLimit(User user) {
        return userFileMetaRepository.findAllByUserId(user.getId()).collectList().flatMap(userFileMetaList -> {
            Map<String, Integer> serverFileIdMap = userFileMetaList
                    .stream()
                    .filter(metadata -> metadata.getFileType() != FileEnum.FOLDER)
                    .filter(metadata -> metadata.getServerFileId() != null)
                    .collect(Collectors.groupingBy(metadata -> metadata.getServerFileId().toString(),
                                                   Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                    ));
            if (serverFileIdMap.isEmpty()) {
                user.setUsedStorage(0L);
                return userRepository.save(user);
            }

            return serverFileMetaRepository.findAllById(serverFileIdMap.keySet()).collectList().flatMap(serverFileMetaList -> {
                long totalSize = serverFileMetaList
                        .stream()
                        .mapToLong(serverFileMeta -> serverFileMeta.getFileSize() * serverFileIdMap.getOrDefault(serverFileMeta.getId().toString(),
                                                                                                                 0
                        ))
                        .sum();
                user.setUsedStorage(totalSize);
                return userRepository.save(user);
            });
        });
    }


    /**
     * 清理過期的 CSRF 憑證
     * 每 1 小時執行一次
     */
    @Scheduled(cron = "0 10 */1 * * ?")
    public void clearExpiredToken() {
        csrfTokenRepositoryStrategy.getCsrfTokenRepository().deleteToken(null).subscribeOn(Schedulers.boundedElastic()).subscribe();
    }


    /**
     * 清理過期的檔案傳輸任務
     * 每 4 小時執行一次
     */
    @Scheduled(cron = "0 0 */4 * * ?")
    public void cleanExpireTransferTask() {
        List<TransfersStatusEnum> status = List.of(TransfersStatusEnum.UPLOADING, TransfersStatusEnum.DOWNLOADING);
        LocalDateTime expiredTime = LocalDateTime.now().minusMinutes(fileProperties.getUpload().getMaxUploadDuration().toMinutes());
        transfersTasksRepository
                .findAllByStatusInAndStartTimeBefore(status, expiredTime)
                .flatMap(transfersTask -> transfersTasksManager.updateTransfersTask(transfersTask.getMd5(),
                                                                                    transfersTask.getTransferTaskId(),
                                                                                    TransfersStatusEnum.FAILED,
                                                                                    "上傳超時",
                                                                                    null,
                                                                                    true
                ))
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();
    }
}