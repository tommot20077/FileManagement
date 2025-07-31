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
 * 基於 Redis 分散式儲存的 UserLimiter 介面實現，提供跨節點共享的用戶登入失敗次數限制功能。
 * 
 * <p>使用 Redis 作為分散式快取後端，實現登入失敗次數的原子性計數與過期控制。
 * 當用戶登入失敗次數超過設定值時，在指定時間內拒絕該用戶的登入請求，防範暴力破解攻擊。
 * 支援多個應用實例間共享限流狀態，確保集群環境下的一致性保護。</p>
 * 
 * <p>本實現透過 Redis 的 INCR 命令進行原子性計數，並設定 TTL 實現自動過期重置。
 * 失敗次數上限和鎖定時間由 SecurityProperties 設定載入，支援運行時動態調整。
 * 僅在 security.login.limiter-provider 設定為 "redis" 時啟用。</p>
 * 
 * <p>執行邏輯：每次 tryAcquire 呼叫檢查並遞增失敗計數，超過上限則返回 false；
 * release 呼叫清除 Redis 中的計數記錄，重置限流狀態。適用於需要分散式登入保護的場景。</p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see xyz.dowob.filemanagement.component.limiter.UserLimiter
 * @see xyz.dowob.filemanagement.component.strategy.UserLimiterStrategy
 */
@Component
@UserLimiterType(UserLimiterEnum.USER_LOGIN_LIMITER)
@ConditionalOnProperty(prefix = "security.login", name = "limiter-provider", havingValue = "redis", matchIfMissing = true)
public class RedisUserLoginLimiter implements UserLimiter {
    /**
     * Redis 操作提供者，用於執行分散式快取操作
     */
    private final RedisProvider redisProvider;

    /**
     * 用戶登入失敗次數上限，超過則觸發鎖定
     */
    private final int MAX_FAILURE_COUNT;

    /**
     * 用戶帳號鎖定持續時間
     */
    private final Duration LOCK_TIME;

    /**
     * Redis 鍵值前綴，用於識別登入限流器資料
     */
    private final String KEY_PREFIX = "user-login-limiter:";

    /**
     * 構造 Redis 登入限流器實例，初始化限流參數並驗證設定有效性。
     * 
     * <p>從 SecurityProperties 設定中載入最大失敗次數和鎖定時間，並執行參數驗證。
     * 鎖定時間必須為正值，最大失敗次數必須大於 0，否則拋出 IllegalArgumentException。</p>
     *
     * @param securityProperties 安全設定屬性，提供登入限制的最大失敗次數和鎖定時間設定
     * @param redisProvider Redis 操作提供者，用於執行分散式快取的讀寫操作
     * @throws IllegalArgumentException 當鎖定時間非正值或最大失敗次數小於等於 0 時
     */
    public RedisUserLoginLimiter(SecurityProperties securityProperties, RedisProvider redisProvider) {
        Assert.isTrue(securityProperties.getLogin().getLockTime().isPositive(), "用戶登錄鎖定時間必須大於0");
        Assert.isTrue(securityProperties.getLogin().getMaxFailure() > 0, "用戶登錄失敗最大次數必須大於0");
        this.MAX_FAILURE_COUNT = securityProperties.getLogin().getMaxFailure();
        this.LOCK_TIME = securityProperties.getLogin().getLockTime();
        this.redisProvider = redisProvider;
    }


    /**
     * 檢查並記錄用戶登入嘗試，執行分散式登入失敗次數限制邏輯。
     * 
     * <p>執行流程：首先從 Redis 查詢該用戶的失敗計數，若記錄不存在則視為 0；
     * 檢查當前計數是否已超過上限，超過則直接返回 false；
     * 未超過則執行原子性遞增操作，並設定 TTL 為鎖定時間；
     * 遞增後再次檢查是否超過上限，決定最終結果。</p>
     * 
     * <p>Redis 鍵值格式為 "user-login-limiter:{key}"，使用 INCR 命令確保原子性操作。
     * 計數器會在鎖定時間到期後自動清除，實現自動重置功能。</p>
     *
     * @param key 用戶識別值，通常為使用者名稱或使用者 ID，不可為 null
     * @return Mono&lt;Boolean&gt; 響應流，true 表示允許登入嘗試，false 表示已被鎖定拒絕
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
     * 清除用戶登入失敗計數記錄，重置該用戶的限流狀態。
     * 
     * <p>透過 Redis DELETE 命令移除該用戶的失敗計數快取，立即解除鎖定狀態。
     * 通常在用戶成功登入後調用，或需要手動重置用戶限流狀態時使用。
     * 操作具有冪等性，即使記錄不存在也不會產生錯誤。</p>
     *
     * @param key 用戶識別值，必須與 tryAcquire 方法使用相同的標識符，不可為 null
     * @return Mono&lt;Void&gt; 完成信號響應流，表示清除操作已執行完畢
     */
    @Override
    public Mono<Void> release(Object key) {
        String cacheKey = KEY_PREFIX + key.toString();
        return redisProvider.delete(cacheKey).then();
    }
}
