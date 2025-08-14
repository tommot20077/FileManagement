package xyz.dowob.filemanagement.component.limiter.loginlimiter;

import jakarta.annotation.PreDestroy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.UserLimiterType;
import xyz.dowob.filemanagement.component.limiter.UserLimiter;
import xyz.dowob.filemanagement.config.properties.SecurityProperties;
import xyz.dowob.filemanagement.customenum.UserLimiterEnum;
import xyz.dowob.filemanagement.unity.CacheConcurrentHashMap;

import java.time.Duration;

/**
 * 基於本地記憶體的用戶登錄嘗試限流器實現。使用 {@link CacheConcurrentHashMap} 儲存失敗計數，
 * 當用戶登錄失敗次數達到設定上限時，暫時禁止該用戶進行登錄嘗試。
 * <p>
 * 此實現具有以下特性：
 * <ul>
 * <li>非阻塞反應式操作，適用於高併發場景</li>
 * <li>基於記憶體存儲，重啟後資料會丟失</li>
 * <li>支援自動過期機制，失敗記錄會在鎖定時間後自動清除</li>
 * <li>僅在 security.login.limiter-provider 設定為 "local" 時啟用</li>
 * </ul>
 * <p>
 * 當用戶登錄失敗達到最大次數限制時，該用戶將被鎖定指定時間。
 * 成功登錄後呼叫 {@link #release(Object)} 方法可立即清除失敗記錄。
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see UserLimiter
 * @see RedisUserLoginLimiter
 * @see xyz.dowob.filemanagement.component.strategy.UserLimiterStrategy
 */
@Component
@UserLimiterType(UserLimiterEnum.USER_LOGIN_LIMITER)
@ConditionalOnProperty(prefix = "security.login", name = "limiter-provider", havingValue = "local")
public class LocalUserLoginLimiter implements UserLimiter {
    /**
     * 用戶登錄失敗次數的本地緩存存儲器。
     * 鍵為用戶名稱，值為累計失敗次數，具備自動過期功能。
     */
    private final CacheConcurrentHashMap<String, Integer> userLoginCountMap;

    /**
     * 允許的最大登錄失敗次數。超過此次數將觸發帳戶鎖定。
     */
    private final int MAX_FAILURE_COUNT;

    /**
     * 帳戶鎖定持續時間。失敗記錄將在此時間後自動清除。
     */
    private final Duration LOCK_TIME;


    /**
     * 建構用戶登錄限流器實例。
     * 從安全配置中讀取最大失敗次數和鎖定時間參數，並初始化本地緩存存儲器。
     *
     * @param securityProperties 包含登錄限制配置的安全屬性物件
     * @throws IllegalArgumentException 當鎖定時間不為正值或最大失敗次數不大於零時
     */
    public LocalUserLoginLimiter(SecurityProperties securityProperties) {
        Assert.isTrue(securityProperties.getLogin().getLockTime().isPositive(), "用戶登錄鎖定時間必須大於0");
        Assert.isTrue(securityProperties.getLogin().getMaxFailure() > 0, "用戶登錄失敗最大次數必須大於0");
        this.MAX_FAILURE_COUNT = securityProperties.getLogin().getMaxFailure();
        this.LOCK_TIME = securityProperties.getLogin().getLockTime();
        this.userLoginCountMap = new CacheConcurrentHashMap<>(64, LOCK_TIME, false);
        this.userLoginCountMap.setTag("用戶登錄限流器緩存表");
    }

    /**
     * 嘗試取得登錄許可。檢查指定用戶的當前失敗次數，若未超過限制則增加計數。
     * 此方法為非阻塞操作，適用於反應式程式設計模式。
     *
     * @param key 用戶識別鍵，預期為字串型態的用戶名稱
     * @return 包含許可結果的 Mono，true 表示允許登錄嘗試，false 表示已達上限被拒絕
     */
    @Override
    public Mono<Boolean> tryAcquire(Object key) {
        String username = (String) key;
        if (userLoginCountMap.checkOrDefault(username, 0) > MAX_FAILURE_COUNT) {
            return Mono.just(false);
        }

        int loginCount = userLoginCountMap.computeIfPresentOrDefault(username, 1, LOCK_TIME, (k, v) -> v + 1);
        return Mono.just(loginCount <= MAX_FAILURE_COUNT);
    }

    /**
     * 釋放用戶的登錄限制，清除該用戶的所有失敗記錄。
     * 通常在用戶成功登錄後呼叫，以重置失敗計數器。
     *
     * @param key 用戶識別鍵，預期為字串型態的用戶名稱
     * @return 表示操作完成的空 Mono
     */
    @Override
    public Mono<Void> release(Object key) {
        String userId = (String) key;
        userLoginCountMap.remove(userId);
        return Mono.empty();
    }


    /**
     * 應用程式關閉時的清理方法。
     * 銷毀用戶登錄計數器緩存，釋放相關資源。
     */
    @PreDestroy
    public void destroy() {
        userLoginCountMap.destroy();
    }
}
