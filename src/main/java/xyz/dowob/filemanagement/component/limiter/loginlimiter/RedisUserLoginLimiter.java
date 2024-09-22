package xyz.dowob.filemanagement.component.limiter.loginlimiter;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.annotation.UserLimiterType;
import xyz.dowob.filemanagement.component.limiter.UserLimiter;
import xyz.dowob.filemanagement.component.provider.provider.RedisProvider;
import xyz.dowob.filemanagement.config.properties.SecurityProperties;
import xyz.dowob.filemanagement.customenum.UserLimiterEnum;

import java.time.Duration;

/**
 * Redis 用戶登錄限流器，使用 Redis 作為限流器的存儲
 * 實現 {@link UserLimiter} 接口，實現用戶限流器的限流和釋放
 * 當用戶的登錄失敗次數達到最大限制時，禁止用戶登錄，避免暴力破解
 * 根據配置文件中的最大失敗次數和鎖定時間，設置用戶的限流器
 * 此類僅在 limiter-provider 為 redis 時生效
 * * 若須取得此類的限流器，請使用 {@link xyz.dowob.filemanagement.component.strategy.UserLimiterStrategy} 類，該類會自動注入所有設定的限流器
 *
 * @author yuan
 * @program FileManagement
 * @ClassName UserLoginLimiter
 * @create 2025/5/2
 * @Version 1.0
 **/
@Component
@UserLimiterType(UserLimiterEnum.USER_LOGIN_LIMITER)
@ConditionalOnProperty(prefix = "security.login", name = "limiter-provider", havingValue = "redis", matchIfMissing = true)
public class RedisUserLoginLimiter implements UserLimiter {
    /**
     * Redis 提供者，用於操作 Redis 數據庫
     */
    private final RedisProvider redisProvider;

    /**
     * 用戶登錄失敗最大次數
     */
    private final int MAX_FAILURE_COUNT;

    /**
     * 用戶登錄鎖定時間
     */
    private final Duration LOCK_TIME;

    /**
     * 用戶登錄限流器的鍵前綴
     */
    private final String KEY_PREFIX = "user-login-limiter:";

    /**
     * 用戶登錄限流器構造方法
     * 獲取配置文件中的登入限制器的最大失敗次數和鎖定時間
     *
     * @param securityProperties 安全配置文件
     * @param redisProvider      Redis 提供者
     */
    public RedisUserLoginLimiter(SecurityProperties securityProperties, RedisProvider redisProvider) {
        Assert.isTrue(securityProperties.getLogin().getLockTime().isPositive(), "用戶登錄鎖定時間必須大於0");
        Assert.isTrue(securityProperties.getLogin().getMaxFailure() > 0, "用戶登錄失敗最大次數必須大於0");
        this.MAX_FAILURE_COUNT = securityProperties.getLogin().getMaxFailure();
        this.LOCK_TIME = securityProperties.getLogin().getLockTime();
        this.redisProvider = redisProvider;
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
        String cacheKey = KEY_PREFIX + key.toString();
        return redisProvider.getValue(cacheKey, Integer.class).switchIfEmpty(Mono.just(0)).flatMap(count -> {
            if (count > MAX_FAILURE_COUNT) {
                return Mono.just(false);
            }
            return redisProvider.incrementDelta(cacheKey, 1, LOCK_TIME).flatMap(newCount -> {
                if (newCount > MAX_FAILURE_COUNT) {
                    return Mono.just(false);
                }
                return Mono.just(true);
            });
        });
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
        String cacheKey = KEY_PREFIX + key.toString();
        return redisProvider.delete(cacheKey);
    }
}
