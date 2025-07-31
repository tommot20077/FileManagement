/**
 * 資料存取物件（Data Access Object）包，包含資料庫查詢結果的映射物件。
 * 
 * <p>此包中的類別主要用於封裝複雜查詢的結果，特別是聯結查詢和統計查詢。
 * 與實體類不同，DAO 物件專注於特定查詢場景的資料傳輸，提供高效的資料存取介面。
 * 
 * <p>包含的主要類別：
 * <ul>
 * <li>{@link xyz.dowob.filemanagement.data.file.dao.OnlineHistoryCountAndOldestDAO} - 在線檔案歷史記錄統計</li>
 * <li>{@link xyz.dowob.filemanagement.data.file.dao.ServerFileMetaCountDAO} - 伺服器檔案引用計數</li>
 * <li>{@link xyz.dowob.filemanagement.data.file.dao.UserFileMetaWithDataDAO} - 用戶檔案完整資料映射</li>
 * </ul>
 * 
 * @author yuan
 * @version 1.0
 * @since 1.0
 */

package xyz.dowob.filemanagement.data.file.dao;