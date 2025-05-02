package xyz.dowob.filemanagement.component.limiter.loginlimiter;

import jakarta.annotation.PreDestroy;
import lombok.extern.log4j.Log4j2;
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
 * 本地用戶登錄限流器，使用自定義的 CacheConcurrentHashMap 作為限流器的存儲
 * 實現 {@link UserLimiter} 接口，實現用戶限流器的限流和釋放
 * 當用戶的登錄失敗次數達到最大限制時，禁止用戶登錄，避免暴力破解
 * 根據配置文件中的最大失敗次數和鎖定時間，設置用戶的限流器
 * 此類僅在 limiter-provider 為 local 時生效
 * 若須取得此類的限流器，請使用 {@link xyz.dowob.filemanagement.component.strategy.UserLimiterStrategy} 類，該類會自動注入所有設定的限流器
 * 注意：此類使用 {@link CacheConcurrentHashMap} 作為限流器的存儲，並不會將數據持久化
 * 因此，當應用重啟時，限流器的數據會丟失
 * 如果需要持久化數據，請使用 {@link RedisUserLoginLimiter} 類
 *
 * @author yuan
 * @program FileManagement
 * @ClassName UserLoginLimiter
 * @create 2025/5/2
 * @Version 1.0
 **/
@Log4j2
@Component
@UserLimiterType(UserLimiterEnum.USER_LOGIN_LIMITER)
@ConditionalOnProperty(prefix = "security.login", name = "limiter-provider", havingValue = "local")
public class LocalUserLoginLimiter implements UserLimiter {
    /**
     * 用戶登錄儲存器，使用 CacheConcurrentHashMap 作為限流器的存儲
     */
    private final CacheConcurrentHashMap<String, Integer> userLoginCountMap;

    /**
     * 用戶登錄失敗最大次數
     */
    private final int MAX_FAILURE_COUNT;

    /**
     * 用戶登錄鎖定時間
     */
    private final Duration LOCK_TIME;


    /**
     * 用戶登錄限流器構造方法
     * 獲取配置文件中的登入限制器的最大失敗次數和鎖定時間
     *
     * @param securityProperties 安全配置文件
     */
    public LocalUserLoginLimiter(SecurityProperties securityProperties) {
        Assert.isTrue(securityProperties.getLogin().getLockTime().isPositive(), "用戶登錄鎖定時間必須大於0");
        Assert.isTrue(securityProperties.getLogin().getMaxFailure() > 0, "用戶登錄失敗最大次數必須大於0");
        this.MAX_FAILURE_COUNT = securityProperties.getLogin().getMaxFailure();
        this.LOCK_TIME = securityProperties.getLogin().getLockTime();
        this.userLoginCountMap = new CacheConcurrentHashMap<>(64, LOCK_TIME, false);
    }

    /**
     * 嘗試獲取用戶的限流器，根據設定的限制數量，判斷是否可以獲取
     *
     * @param key 用戶辨識值
     *
     * @return Mono<Boolean> 是否獲取成功
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
     * 釋放用戶的限流器
     *
     * @param key 用戶辨識值
     *
     * @return Mono<Void>
     */
    @Override
    public Mono<Void> release(Object key) {
        String userId = (String) key;
        userLoginCountMap.remove(userId);
        return Mono.empty();
    }


    /**
     * 在應用關閉時，銷毀用戶登入計數器
     */
    @PreDestroy
    public void destroy() {
        userLoginCountMap.destroy();
    }
}
