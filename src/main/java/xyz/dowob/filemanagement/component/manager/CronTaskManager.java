package xyz.dowob.filemanagement.component.manager;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import xyz.dowob.filemanagement.component.provider.providerImplement.JwtTokenProviderImpl;

import java.util.Date;

/**
 * @author yuan
 * @program FileManagement
 * @ClassName CronTaskManager
 * @create 2025/1/26
 * @Version 1.0
 **/
@Component
@RequiredArgsConstructor
public class CronTaskManager {
    private final JwtTokenProviderImpl jwtTokenProvider;

    @Scheduled(cron = "0 0 */6 * * ?")
    public void deleteExpiredToken() {
        jwtTokenProvider.getCacheTokenMap().forEach((k, v) -> {
            if (v.expireTime().before(new Date())) {
                jwtTokenProvider.getCacheTokenMap().remove(k);
            }
        });
    }
}
