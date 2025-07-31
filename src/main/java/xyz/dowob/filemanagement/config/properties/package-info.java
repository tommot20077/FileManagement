/**
 * 設定屬性包，基於 Spring Boot ConfigurationProperties 的類型安全設定管理。
 *
 * <p>提供快取（{@link xyz.dowob.filemanagement.config.properties.CacheProperties}）、檔案（{@link xyz.dowob.filemanagement.config.properties.FileProperties}）、
 * 全域（{@link xyz.dowob.filemanagement.config.properties.GlobalProperties}）和安全（{@link
 * xyz.dowob.filemanagement.config.properties.SecurityProperties}）四大類設定屬性。
 * 所有設定都可透過 application.yml 進行外部化設定，具備預設值、類型安全和編譯期驗證能力。
 * 支援不同環境的設定覆蓋和部分設定的動態刷新。</p>
 *
 * <p>設定分層結構採用前綴模式：cache.*（快取策略）、file.*（檔案管理）、
 * global.*（系統參數）、security.*（安全控制）。提供完整的文檔說明和使用範例，
 * 確保設定的正確性和可維護性。</p>
 *
 * @author yuan
 * @since 1.0
 */
package xyz.dowob.filemanagement.config.properties;