/**
 * 響應式用戶限流器實現包，提供多種策略的流量控制與請求限制機制。
 * <p>
 * 本包實現了完整的用戶級限流控制系統，透過不同的限流策略保護系統資源，
 * 防止惡意攻擊和過度使用導致的系統過載。
 * </p>
 * <p>
 * 核心元件：
 * </p>
 * <ul>
 *   <li>{@link xyz.dowob.filemanagement.component.limiter.UserLimiter} - 用戶限流器介面，定義標準的限流操作契約</li>
 *   <li>{@link xyz.dowob.filemanagement.component.limiter.UserUploadLimiter} - 上傳任務限流器，控制用戶並發上傳數量</li>
 * </ul>
 * <p>
 * 子包：
 * </p>
 * <ul>
 *   <li>loginlimiter: 登入失敗次數限流器實現，防範暴力破解攻擊</li>
 * </ul>
 * <p>
 * 所有限流器均採用響應式設計，支援非阻塞的高併發處理。
 * </p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */

package xyz.dowob.filemanagement.component.limiter;