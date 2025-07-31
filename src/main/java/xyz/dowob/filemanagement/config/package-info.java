/**
 * 系統設定包，包含檔案管理系統的所有核心設定類別。
 * <p>
 * 提供完整的反應式檔案管理系統設定生態系統，涵蓋資料存取（R2DBC、Redis、MongoDB）、
 * 網路通訊（WebFlux、WebSocket、Netty）、安全控制（Spring Security）、系統穩定性（斷路器、事件）
 * 和 API 文檔（OpenAPI）等所有技術棧設定。每個設定類別專門負責特定技術領域，
 * 確保系統的模組化架構和可維護性。
 * </p>
 * <p>
 * 核心設定類別：
 * </p>
 * <ul>
 *   <li>{@link xyz.dowob.filemanagement.config.R2dbcConfig} - 反應式關聯式資料庫設定</li>
 *   <li>{@link xyz.dowob.filemanagement.config.RedisConfig} - Redis 緩存和分散式鎖設定</li>
 *   <li>{@link xyz.dowob.filemanagement.config.MongoConfig} - MongoDB GridFS 檔案儲存設定</li>
 *   <li>{@link xyz.dowob.filemanagement.config.WebFluxConfig} - Spring WebFlux 反應式網路設定</li>
 *   <li>{@link xyz.dowob.filemanagement.config.WebSocketConfig} - WebSocket 即時通訊設定</li>
 *   <li>{@link xyz.dowob.filemanagement.config.NettyConfig} - Netty 網路伺服器設定</li>
 *   <li>{@link xyz.dowob.filemanagement.config.SecurityConfig} - Spring Security 安全設定</li>
 *   <li>{@link xyz.dowob.filemanagement.config.CircuitBreakersConfig} - 斷路器穩定性設定</li>
 *   <li>{@link xyz.dowob.filemanagement.config.EventSinkConfig} - 反應式事件處理設定</li>
 *   <li>{@link xyz.dowob.filemanagement.config.OpenApiConfig} - API 文檔生成設定</li>
 * </ul>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
package xyz.dowob.filemanagement.config;