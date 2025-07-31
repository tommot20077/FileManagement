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
import xyz.dowob.filemanagement.entity.UserFileMetadata;
import xyz.dowob.filemanagement.repostiory.*;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 基於 Spring Scheduled 的定時任務管理器，負責系統的週期性維護作業。
 *
 * <p>此管理器執行多種定期清理和維護任務，包括過期 JWT 憑證清理、用戶存儲用量計算、
 * 過期檔案刪除、CSRF 憑證清理及檔案傳輸任務狀態管理。所有任務均採用非阻塞響應式模式執行，
 * 確保在高併發環境下的穩定性能。任務調度遵循 Spring 的 {@code @Scheduled} 機制，
 * 具備事務支援和錯誤處理能力。
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see org.springframework.scheduling.annotation.Scheduled
 * @see reactor.core.publisher.Mono
 */
@Component
@RecordLevel(LogLevelEnum.INFO)
@RequiredArgsConstructor
public class CronTaskManager {
    /**
     * JWT 憑證提供者，用於管理 JWT 憑證的生成、驗證和緩存清理。
     */
    private final JwtTokenProviderImpl jwtTokenProvider;

    /**
     * 用戶資料庫操作倉庫，提供用戶實體的 CRUD 操作。
     */
    private final UserRepository userRepository;

    /**
     * 用戶檔案元資料庫操作倉庫，管理用戶檔案的元資料資訊。
     */
    private final UserFileMetaRepository userFileMetaRepository;

    /**
     * 伺服器檔案元資料庫操作倉庫，儲存實際檔案的物理資訊。
     */
    private final ServerFileMetaRepository serverFileMetaRepository;

    /**
     * 檔案回收站記錄資料庫操作倉庫，管理已刪除檔案的回收站記錄。
     */
    private final FileTrashRecordRepository fileTrashRecordRepository;

    /**
     * 反應式事務操作器，提供響應式環境下的事務管理能力。
     */
    private final TransactionalOperator transactionalOperator;

    /**
     * CSRF 憑證儲存庫策略，支援多種 CSRF 憑證儲存實作方式。
     */
    private final CsrfTokenRepositoryStrategy csrfTokenRepositoryStrategy;

    /**
     * 檔案傳輸任務管理器，負責檔案上傳和下載任務的狀態管理。
     */
    private final TransfersTasksManager transfersTasksManager;

    /**
     * 檔案傳輸任務資料庫操作倉庫，儲存檔案傳輸任務的相關資訊。
     */
    private final TransfersTasksRepository transfersTasksRepository;

    /**
     * 檔案相關配置屬性，包含檔案上傳、下載及儲存的各項設定。
     */
    private final FileProperties fileProperties;


    /**
     * 清理過期的 JWT 緩存憑證，定期維護憑證緩存的有效性。
     *
     * <p>此方法會檢查所有已緩存的 JWT 憑證，移除已過期的憑證項目，
     * 防止記憶體洩漏並確保安全性。執行頻率為每 6 小時一次。
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
     * 檢查並更新所有用戶的存儲使用量，確保用量統計的準確性。
     *
     * <p>此方法會遍歷所有用戶，重新計算其檔案佔用的實際存儲空間，
     * 並更新至用戶記錄中。計算過程會考慮檔案共享和重複檔案的影響。
     * 執行頻率為每 6 小時一次。
     */
    @Scheduled(cron = "0 30 */6 * * ?")
    public void checkUserStorageLimit() {
        userRepository.findAll().flatMap(this::calculateUserStorageLimit).subscribeOn(Schedulers.boundedElastic()).subscribe();
    }

    /**
     * 計算指定用戶的存儲使用量並更新至資料庫。
     *
     * <p>此方法會查詢用戶的所有檔案元資料，計算實際佔用的存儲空間。
     * 對於共享檔案，會根據引用次數進行適當的空間分攤計算。
     * 資料夾類型的檔案不計入存儲用量統計。
     *
     * @param user 要計算存儲用量的用戶實體
     * @return 包含更新後存儲用量的用戶實體 Mono
     */
    private Mono<User> calculateUserStorageLimit(User user) {
        return userFileMetaRepository.findAllByUserId(user.getId()).collectList().flatMap(userFileMetaList -> {
            Map<Long, Integer> serverFileIdMap = userFileMetaList
                    .stream()
                    .filter(metadata -> metadata.getFileType() != FileEnum.FOLDER)
                    .filter(metadata -> metadata.getServerFileId() != null)
                    .collect(Collectors.groupingBy(UserFileMetadata::getServerFileId,
                                                   Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                    ));
            if (serverFileIdMap.isEmpty()) {
                user.setUsedStorage(0L);
                return userRepository.save(user);
            }

            return serverFileMetaRepository.findAllById(serverFileIdMap.keySet()).collectList().flatMap(serverFileMetaList -> {
                long totalSize = serverFileMetaList
                        .stream()
                        .mapToLong(serverFileMeta -> serverFileMeta.getFileSize() * serverFileIdMap.getOrDefault(serverFileMeta.getId(),
                                                                                                                 0
                        ))
                        .sum();
                user.setUsedStorage(totalSize);
                return userRepository.save(user);
            });
        });
    }

    /**
     * 刪除回收站中過期的檔案並重新計算用戶存儲用量。
     *
     * <p>此方法會查詢所有用戶回收站中超過保留期限的檔案記錄，
     * 從資料庫中永久刪除這些檔案的元資料，並重新計算相關用戶的存儲用量。
     * 整個過程在事務環境中執行，確保資料一致性。執行頻率為每小時一次。
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
     * 清理系統中過期的 CSRF 憑證，維護安全憑證的有效性。
     *
     * <p>此方法會透過 CSRF 憑證儲存策略清理所有過期的 CSRF 憑證，
     * 防止過期憑證累積造成的安全風險和記憶體浪費。
     * 執行頻率為每小時一次。
     */
    @Scheduled(cron = "0 10 */1 * * ?")
    public void clearExpiredToken() {
        csrfTokenRepositoryStrategy.getCsrfTokenRepository().deleteToken(null).subscribeOn(Schedulers.boundedElastic()).subscribe();
    }


    /**
     * 清理超時的檔案傳輸任務，將長時間處於傳輸狀態的任務標記為失敗。
     *
     * <p>此方法會查詢所有處於上傳或下載狀態且超過最大傳輸時間限制的任務，
     * 將其狀態更新為失敗，並設定超時訊息。這有助於釋放系統資源並維護任務狀態的準確性。
     * 執行頻率為每 4 小時一次。
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