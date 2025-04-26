/**
 * 此包用於 IP 限流的請求過濾器，當某一個 IP 的請求次數超過限制時，會返回 429 錯誤
 * 降低請求的頻率，避免過多的請求影響伺服器的性能
 * 1. localRequestLimiterFilter: 使用本地的請求過濾器 {@link xyz.dowob.filemanagement.component.filter.requestlimiter.localRequestLimiterFilter}
 * 2. redisRequestLimiterFilter: 使用 Redis 的請求過濾器 {@link xyz.dowob.filemanagement.component.filter.requestlimiter.RedisRequestLimiterFilter}
 */

package xyz.dowob.filemanagement.component.filter.requestlimiter;