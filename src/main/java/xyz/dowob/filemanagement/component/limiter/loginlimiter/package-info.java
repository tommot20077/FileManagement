/**
 * 用戶登入失敗次數限流器實現包，提供多種儲存後端的登入保護機制。
 * <p>
 * 本包專門處理用戶登入安全保護，透過追蹤登入失敗次數來防範暴力破解攻擊。
 * 支援本地快取和分散式 Redis 兩種儲存策略，適應不同的部署環境需求。
 * </p>
 * <p>
 * 實現類別：
 * </p>
 * <ul>
 *   <li>{@link xyz.dowob.filemanagement.component.limiter.loginlimiter.LocalUserLoginLimiter} - 
 *       本地快取限流器，使用 {@link xyz.dowob.filemanagement.unity.CacheConcurrentHashMap} 實現單機限流</li>
 *   <li>{@link xyz.dowob.filemanagement.component.limiter.loginlimiter.RedisUserLoginLimiter} - 
 *       Redis 分散式限流器，支援多節點部署的統一限流控制</li>
 * </ul>
 * <p>
 * 設定控制：透過 {@code security.login.limiter-provider} 屬性選擇使用的限流器實現。
 * 限流參數如最大失敗次數和鎖定時間可透過安全設定動態調整。
 * </p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */

package xyz.dowob.filemanagement.component.limiter.loginlimiter;