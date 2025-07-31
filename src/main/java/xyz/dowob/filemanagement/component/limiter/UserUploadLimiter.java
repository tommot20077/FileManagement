package xyz.dowob.filemanagement.component.limiter;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.RecordLevel;
import xyz.dowob.filemanagement.annotation.UserLimiterType;
import xyz.dowob.filemanagement.config.properties.FileProperties;
import xyz.dowob.filemanagement.customenum.LogLevelEnum;
import xyz.dowob.filemanagement.customenum.UserLimiterEnum;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

/**
 * 基於信號量的用戶上傳任務限流器，控制並發上傳數量防止系統過載。
 * 
 * <p>本類使用 {@link java.util.concurrent.Semaphore} 實現用戶級別的上傳任務並發控制，
 * 每個用戶獨立維護其上傳許可池，確保單一用戶無法佔用過多系統資源。</p>
 * 
 * <p>限流器的運作機制：每個用戶分配固定數量的上傳許可，當許可用盡時新的上傳請求將被拒絕。
 * 任務完成後自動釋放許可，若用戶無活躍任務則清理其許可池以節約記憶體。</p>
 * 
 * <p>設定來源：最大並發數從 {@link xyz.dowob.filemanagement.config.properties.FileProperties} 
 * 的 upload.maxUploadTaskLimit 屬性獲取。</p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@Component
@UserLimiterType(UserLimiterEnum.USER_UPLOAD_LIMITER)
public class UserUploadLimiter implements UserLimiter {
    /**
     * 每用戶最大並發上傳任務數量，從設定檔案動態載入
     */
    private final int MAX_CONCURRENT_UPLOADS_PER_USER;

    /**
     * 用戶信號量映射表，儲存各用戶的上傳許可信號量
     */
    private final ConcurrentHashMap<Long, Semaphore> userSemaphoreMap = new ConcurrentHashMap<>();

    /**
     * 構造上傳限流器並初始化最大並發限制。
     * 
     * <p>從檔案設定中讀取每用戶的最大並發上傳數量設定。</p>
     *
     * @param fileProperties 檔案屬性設定，包含上傳限制參數
     */
    public UserUploadLimiter(FileProperties fileProperties) {
        this.MAX_CONCURRENT_UPLOADS_PER_USER = fileProperties.getUpload().getMaxUploadTaskLimit();
    }


    /**
     * 嘗試為用戶取得上傳許可，支援動態信號量建立。
     * 
     * <p>檢查用戶是否有可用的上傳許可。若用戶首次上傳，
     * 自動建立對應的信號量池。採用非阻塞方式嘗試取得許可。</p>
     *
     * @param key 用戶ID，必須為 Long 類型
     * @return 布林值響應流，true表示許可取得成功，false表示已達上限
     */
    @Override
    @RecordLevel(LogLevelEnum.DEBUG)
    public Mono<Boolean> tryAcquire(Object key) {
        Long userId = (Long) key;
        Semaphore semaphore = userSemaphoreMap.computeIfAbsent(userId, k -> new Semaphore(MAX_CONCURRENT_UPLOADS_PER_USER));
        return Mono.just(semaphore.tryAcquire());
    }
    //todo userSemaphoreMap改為cacheConcurrentHashMap


    /**
     * 釋放用戶上傳許可並執行記憶體最佳化。
     * 
     * <p>歸還已使用的上傳許可。當用戶所有許可都歸還時，
     * 自動清理該用戶的信號量以節約記憶體。</p>
     *
     * @param key 用戶ID，必須為 Long 類型
     * @return 完成信號，表示釋放操作已執行
     */
    @Override
    public Mono<Void> release(Object key) {
        Long userId = (Long) key;
        Semaphore semaphore = userSemaphoreMap.get(userId);
        if (semaphore != null) {
            semaphore.release();
            if (semaphore.availablePermits() == MAX_CONCURRENT_UPLOADS_PER_USER) {
                userSemaphoreMap.remove(userId);
            }
        }
        return Mono.empty();
    }


    /**
     * 應用關閉時的資源清理，釋放所有用戶信號量資源。
     * 
     * <p>確保應用程式正常關閉時清理所有信號量映射，防止記憶體洩漏。</p>
     */
    @PreDestroy
    public void destroy() {
        userSemaphoreMap.clear();
    }
}
