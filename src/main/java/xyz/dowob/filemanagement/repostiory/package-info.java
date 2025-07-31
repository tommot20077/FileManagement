/**
 * 資料存取層介面套件，提供基於 Spring Data R2DBC 的響應式資料庫操作功能。
 * <p>
 * 此套件包含系統中所有的資料存取層介面，採用響應式程式設計模式，
 * 提供非阻塞的資料庫存取能力，確保高併發環境下的效能表現。
 * 所有介面均繼承自 {@link org.springframework.data.repository.reactive.ReactiveCrudRepository}，
 * 提供標準的 CRUD 操作和自定義查詢方法。
 * </p>
 * <p>
 * 主要介面包括：
 * <ul>
 *   <li>{@link xyz.dowob.filemanagement.repostiory.FileTrashRecordRepository} - 檔案回收站記錄管理</li>
 *   <li>{@link xyz.dowob.filemanagement.repostiory.JwtSecurityContextRepository} - JWT 安全上下文處理</li>
 *   <li>{@link xyz.dowob.filemanagement.repostiory.ServerFileMetaRepository} - 伺服器檔案元資料管理</li>
 *   <li>{@link xyz.dowob.filemanagement.repostiory.TokenRepository} - 使用者憑證管理</li>
 *   <li>{@link xyz.dowob.filemanagement.repostiory.TransfersTasksRepository} - 檔案傳輸任務管理</li>
 *   <li>{@link xyz.dowob.filemanagement.repostiory.UserFileMetaRepository} - 使用者檔案元資料管理</li>
 *   <li>{@link xyz.dowob.filemanagement.repostiory.UserFIleShareRecordRepository} - 檔案分享記錄管理</li>
 *   <li>{@link xyz.dowob.filemanagement.repostiory.UserOnlineFileRepository} - 線上檔案管理</li>
 *   <li>{@link xyz.dowob.filemanagement.repostiory.UserOnlineFileHistoryRepository} - 線上檔案版本歷史管理</li>
 *   <li>{@link xyz.dowob.filemanagement.repostiory.UserRepository} - 使用者資料管理</li>
 * </ul>
 * </p>
 * <p>
 * 設計原則：
 * <ul>
 *   <li>採用響應式程式設計模式，支援非阻塞操作</li>
 *   <li>遵循單一職責原則，每個介面專注於特定實體的資料操作</li>
 *   <li>提供豐富的查詢方法，滿足不同業務場景需求</li>
 *   <li>支援複雜查詢和統計分析功能</li>
 *   <li>確保資料存取的安全性和一致性</li>
 * </ul>
 * </p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
package xyz.dowob.filemanagement.repostiory;