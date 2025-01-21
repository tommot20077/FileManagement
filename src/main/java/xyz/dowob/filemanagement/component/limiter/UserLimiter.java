package xyz.dowob.filemanagement.component.limiter;

/**
 * 用戶限流器接口，用於實現不同的用戶限流器
 * 當用戶請求過多時，限制用戶的請求
 *
 * @author yuan
 * @program FileManagement
 * @ClassName UserLimiter
 * @create 2025/1/20
 * @Version 1.0
 **/

public interface UserLimiter {
    /**
     * 嘗試獲取用戶的限流器，根據設定的限制數量，判斷是否可以獲取
     *
     * @param userId 用戶ID
     *
     * @return 是否獲取成功
     */
    boolean tryAcquire(Long userId);

    /**
     * 釋放用戶的限流器
     *
     * @param userId 用戶ID
     */
    void release(Long userId);
}
