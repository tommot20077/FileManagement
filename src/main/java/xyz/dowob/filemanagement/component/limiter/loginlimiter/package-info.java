/**
 * 本包主要為用戶登入限制器的具體實現，負責對用戶登入請求進行限制和控制。
 * 1. LocalUserLoginLimiter：本地用戶登入限制器，使用自定義的緩存類 {@link xyz.dowob.filemanagement.unity.CacheConcurrentHashMap}中的計數器來限制用戶的登入請求
 * {@link xyz.dowob.filemanagement.component.limiter.loginlimiter.LocalUserLoginLimiter}。
 * 2. RedisUserLoginLimiter：Redis 用戶登入限制器，使用 Redis 中的計數器來限制用戶的登入請求
 * {@link xyz.dowob.filemanagement.component.limiter.loginlimiter.RedisUserLoginLimiter}。
 *
 * @author yuan
 * @program FileManagement
 * @ClassName package-info
 * @create 2025/5/3
 * @Version 1.0
 **/

package xyz.dowob.filemanagement.component.limiter.loginlimiter;