/**
 * 基於介面與實現分離的功能提供者模組，透過 Spring WebFlux 非阻塞機制提供高效能系統服務。
 *
 * <p>此模組實現策略模式與工廠模式的組合設計，提供可插拔且高度可擴展的服務架構。
 * 支援反應式編程模型，確保所有操作均為非阻塞式處理。</p>
 *
 * <p>主要子模組：
 * <ul>
 *   <li>factory：工廠模式實現，動態創建內容轉換服務</li>
 *   <li>provider：具體服務提供者，包含 Redis、GridFS、檔案樹管理等</li>
 *   <li>providerImplement：介面實現類，提供郵件、檔案掃描、令牌管理等具體實現</li>
 *   <li>providerInterface：服務介面定義，確保實現類的一致性與可替換性</li>
 * </ul></p>
 *
 * <p>透過註解驅動的組件註冊機制，系統可根據設定動態選擇適當的服務實現。
 * 當需要新增功能時，僅需實現對應介面並添加適當標記即可。</p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
package xyz.dowob.filemanagement.component.provider;