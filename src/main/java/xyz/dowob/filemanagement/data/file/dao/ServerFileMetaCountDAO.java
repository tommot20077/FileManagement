package xyz.dowob.filemanagement.data.file.dao;

/**
 * 伺服器檔案元數據計數數據傳輸對象，用於封裝伺服器檔案元數據計數
 * @author yuan
 * @program FileManagement
 * @ClassName ServerFileMetaCountDAO
 * @create 2025/2/7
 * @Version 1.0
 **/
public record ServerFileMetaCountDAO(Long serverFileId, Long count) {
}
