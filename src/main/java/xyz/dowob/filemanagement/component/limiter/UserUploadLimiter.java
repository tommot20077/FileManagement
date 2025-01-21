package xyz.dowob.filemanagement.component.limiter;

import org.springframework.stereotype.Component;
import xyz.dowob.filemanagement.annotation.UserLimiterType;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.customenum.UserLimiterEnum;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

/**
 * 用戶上傳限流器，用於限制用戶的上傳任務數量
 * 實現 UserLimiter 接口，實現用戶限流器的限流和釋放
 *
 * @author yuan
 * @program FileManagement
 * @ClassName UploadLimiter
 * @create 2025/1/20
 * @Version 1.0
 **/
@Component
@UserLimiterType(UserLimiterEnum.USER_UPLOAD_LIMITER)
public class UserUploadLimiter implements UserLimiter {
    /**
     * 每個用戶最大的並發上傳任務數量，此值從配置文件中獲取
     */
    private final int MAX_CONCURRENT_UPLOADS_PER_USER;
    /**
     * 用戶憑證映射，用於存儲用戶的可用的憑證
     */
    private final ConcurrentHashMap<Long, Semaphore> userSemaphoreMap = new ConcurrentHashMap<>();

    public UserUploadLimiter(FileProperties fileProperties) {
        this.MAX_CONCURRENT_UPLOADS_PER_USER = fileProperties.getUpload().getMaxUploadTaskLimit();
    }

    /**
     * 嘗試獲取用戶的限流器，根據設定的限制數量，判斷是否可以獲取
     * 當用戶的憑證不存在時，創建一個新的憑證
     *
     * @param userId 用戶ID
     *
     * @return 是否獲取成功
     */
    @Override
    public boolean tryAcquire(Long userId) {
        Semaphore semaphore = userSemaphoreMap.computeIfAbsent(userId, k -> new Semaphore(MAX_CONCURRENT_UPLOADS_PER_USER));
        return semaphore.tryAcquire();
    }

    /**
     * 釋放用戶的限流器
     * 當用戶的憑證可用憑證數量等於最大憑證數量時，刪除用戶的憑證
     *
     * @param userId 用戶ID
     */
    @Override
    public void release(Long userId) {
        Semaphore semaphore = userSemaphoreMap.get(userId);
        if (semaphore != null) {
            semaphore.release();
            if (semaphore.availablePermits() == MAX_CONCURRENT_UPLOADS_PER_USER) {
                userSemaphoreMap.remove(userId);
            }
        }
    }
}
