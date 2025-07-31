package xyz.dowob.filemanagement.data.file.dao;

/**
 * 伺服器檔案元資料引用計數資料存取對象，封裝檔案引用統計資訊。
 * 用於統計和查詢特定伺服器檔案被用戶檔案引用的次數。
 *
 * <p>此記錄類型提供不可變的資料封裝，適合用於數據庫查詢結果的傳輸。
 * 引用計數用於決定伺服器檔案是否可以安全刪除，當計數為 0 時表示沒有用戶引用。
 *
 * @param serverFileId 伺服器檔案元資料識別符
 * @param count        引用次數，表示有多少用戶檔案引用此伺服器檔案
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
public record ServerFileMetaCountDAO(Long serverFileId, Long count) {
}
