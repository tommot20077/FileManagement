/**
 * CSRF Token 安全防護元件套件，提供多種儲存策略的 CSRF Token 管理實作。
 * <p>
 * 此套件包含完整的 CSRF（跨站請求偽造）防護解決方案，支援不同的儲存後端和部署場景。
 * 透過策略模式設計，可根據應用需求選擇適當的 Token 儲存方式。
 * </p>
 * <h3>主要元件：</h3>
 * <ul>
 *   <li>{@link xyz.dowob.filemanagement.repostiory.ServerCsrfToken.CustomServerCsrfTokenRepository} - 
 *       自訂 CSRF Token 存放庫介面，擴展標準功能</li>
 *   <li>{@link xyz.dowob.filemanagement.repostiory.ServerCsrfToken.AbstractServerCsrfTokenRepository} - 
 *       抽象基礎類別，提供通用的設定和基礎實作</li>
 * </ul>
 * <h3>具體實作：</h3>
 * <ul>
 *   <li>{@link xyz.dowob.filemanagement.repostiory.ServerCsrfToken.LocalServerCsrfTokenRepository} - 
 *       本地記憶體存放庫，適用於單機部署</li>
 *   <li>{@link xyz.dowob.filemanagement.repostiory.ServerCsrfToken.RedisServerCsrfTokenRepository} - 
 *       Redis 分散式存放庫，適用於集群部署</li>
 * </ul>
 * <p>
 * 所有實作均支援 Spring Security WebFlux 的響應式程式設計模式，
 * 確保非阻塞的安全性操作和高效能的併發處理能力。
 * </p>
 *
 * @since 1.0
 * @version 1.0
 */

package xyz.dowob.filemanagement.repostiory.ServerCsrfToken;