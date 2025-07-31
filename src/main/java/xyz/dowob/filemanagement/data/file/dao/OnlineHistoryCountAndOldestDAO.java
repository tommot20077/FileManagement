package xyz.dowob.filemanagement.data.file.dao;

/**
 * 在線檔案歷史記錄統計資料存取對象，封裝歷史版本數量和最早版本資訊。
 * 用於查詢和管理在線檔案的歷史版本資料。
 *
 * <p>此記錄類型提供不可變的資料封裝，適合用於數據庫查詢結果的傳輸。
 * 版本號通常代表最早的歷史版本，數量表示總歷史記錄數。
 *
 * @param version 最早的歷史版本號
 * @param count   歷史記錄總數
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
public record OnlineHistoryCountAndOldestDAO(Long version, Long count) {
}
