package xyz.dowob.filemanagement.component.manager;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import xyz.dowob.filemanagement.component.provider.providerImplement.JwtTokenProviderImpl;
import xyz.dowob.filemanagement.repostiory.ServerFileMetaRepository;
import xyz.dowob.filemanagement.repostiory.UserFileMetaRepository;
import xyz.dowob.filemanagement.repostiory.UserRepository;

import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 定時任務管理器，統一將伺服器上的定時任務放在這裡
 * @author yuan
 * @program FileManagement
 * @ClassName CronTaskManager
 * @create 2025/1/26
 * @Version 1.0
 **/
@Component
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
     * 每 4 小時執行一次
     */
    @Scheduled(cron = "0 0 */4 * * ?")
    public void checkUserStorageLimit() {
        userRepository
                .findAll()
                .flatMap(user -> userFileMetaRepository.findAllByUserId(user.getId()).collectList().flatMap(userFileMetaList -> {
                    Map<String, Integer> serverFileIdMap = userFileMetaList
                            .stream()
                            .filter(metadata -> !metadata.getIsFolder())
                            .filter(metadata -> metadata.getServerFileId() != null) //todo 線上檔案沒有serverFileId暫不紀錄
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
                                .mapToLong(serverFileMeta -> serverFileMeta.getFileSize() * serverFileIdMap.getOrDefault(serverFileMeta
                                                                                                                                 .getId()
                                                                                                                                 .toString(),
                                                                                                                         0
                                ))
                                .sum();
                        user.setUsedStorage(totalSize);
                        return userRepository.save(user);
                    });
                }))
                .subscribe();
    }
}