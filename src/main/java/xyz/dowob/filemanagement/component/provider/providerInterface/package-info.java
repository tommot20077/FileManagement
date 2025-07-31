/**
 * 反應式服務提供者介面定義模組，建立系統服務的可插拔抽象層。
 *
 * <p>此模組定義核心業務服務的標準介面規範，遵循策略模式與依賴倒置原則。
 * 所有介面均基於 Spring WebFlux 反應式編程模型設計，確保非阻塞操作特性。
 * 透過介面抽象實現高度解耦與可測試性。</p>
 *
 * <p>核心服務介面：
 * <ul>
 *   <li>{@link xyz.dowob.filemanagement.component.provider.providerInterface.EmailProvider}：
 *       非阻塞郵件發送服務介面</li>
 *   <li>{@link xyz.dowob.filemanagement.component.provider.providerInterface.TokenProvider}：
 *       令牌管理服務介面，支援生成、驗證與撤銷</li>
 *   <li>{@link xyz.dowob.filemanagement.component.provider.providerInterface.CacheProvider}：
 *       快取操作服務介面，提供多種資料結構支援</li>
 *   <li>{@link xyz.dowob.filemanagement.component.provider.providerInterface.ContentConvertProvider}：
 *       內容轉換服務介面，支援多種格式轉換</li>
 *   <li>{@link xyz.dowob.filemanagement.component.provider.providerInterface.FileScanProvider}：
 *       檔案安全掃描服務介面，提供病毒檢測功能</li>
 * </ul>
 * </p>
 *
 * <p>實作新服務提供者時，只需實現對應介面並遵循反應式編程模型即可無縫整合。</p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
package xyz.dowob.filemanagement.component.provider.providerInterface;