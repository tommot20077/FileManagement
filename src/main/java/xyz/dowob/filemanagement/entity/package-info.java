/**
 * 實體類別套件，包含檔案管理系統所有的資料庫實體映射類別。
 * <p>
 * 此套件中的實體類別採用 Spring Data R2DBC 進行 ORM 映射，
 * 每個實體類別對應資料庫中的一個表，負責管理系統的核心資料結構。
 * </p>
 * <p>
 * 核心實體類別包括：
 * <ul>
 *   <li>{@link xyz.dowob.filemanagement.entity.ServerFileMetadata} - 伺服器檔案元資料表，管理檔案物理實體</li>
 *   <li>{@link xyz.dowob.filemanagement.entity.UserFileMetadata} - 用戶檔案元資料表，管理用戶視角的檔案資訊</li>
 *   <li>{@link xyz.dowob.filemanagement.entity.User} - 用戶實體表，管理系統用戶基本資訊與權限</li>
 *   <li>{@link xyz.dowob.filemanagement.entity.Token} - 憑證實體表，管理 JWT 憑證與重設密碼驗證碼</li>
 *   <li>{@link xyz.dowob.filemanagement.entity.UserOnlineFile} - 用戶線上檔案表，管理協作編輯檔案內容</li>
 *   <li>{@link xyz.dowob.filemanagement.entity.UserOnlineFileHistory} - 用戶線上檔案歷史表，管理檔案版本控制</li>
 *   <li>{@link xyz.dowob.filemanagement.entity.UserFileShareRecord} - 用戶檔案分享記錄表，管理檔案分享權限</li>
 *   <li>{@link xyz.dowob.filemanagement.entity.TransfersTask} - 檔案傳輸任務表，管理上傳下載任務狀態</li>
 *   <li>{@link xyz.dowob.filemanagement.entity.FileTrashRecord} - 檔案回收站記錄表，管理已刪除檔案資訊</li>
 * </ul>
 * </p>
 * <p>
 * 設計特點：
 * <ul>
 *   <li>採用 Lombok 註解簡化程式碼結構</li>
 *   <li>使用 Spring Data 註解進行 ORM 映射</li>
 *   <li>實作標準的 equals、hashCode、toString 方法</li>
 *   <li>支援 JSON 序列化與反序列化</li>
 *   <li>遵循響應式程式設計模式</li>
 * </ul>
 * </p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see org.springframework.data.annotation
 * @see org.springframework.data.relational.core.mapping
 */
package xyz.dowob.filemanagement.entity;