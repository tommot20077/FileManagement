package xyz.dowob.filemanagement.data.file.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.entity.ServerFileMetadata;
import xyz.dowob.filemanagement.entity.UserFileMetadata;

import java.time.LocalDateTime;

/**
 * 檔案上傳結果業務物件，封裝檔案上傳操作完成後的相關資訊。
 * <p>
 * 此類別整合了使用者檔案元資料（UserFileMetadata）和伺服器檔案元資料（ServerFileMetadata）
 * 的關鍵資訊，提供統一的資料結構來表示檔案上傳的結果狀態。主要用於檔案上傳完成後
 * 向前端或其他服務層回傳上傳成功的檔案詳細資訊。
 * <p>
 * 該物件包含檔案的基本屬性（檔名、大小、類型等）、儲存位置資訊（使用者檔案ID、
 * 伺服器檔案ID）以及檔案完整性驗證資訊（MD5雜湊值）。支援透過建構方法直接從
 * 實體物件建立，確保資料的一致性和完整性。
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@Data
@Builder
@AllArgsConstructor
public class FileUploadResultBO {
    /**
     * 使用者檔案識別碼，對應 UserFileMetadata 的主鍵。
     * 用於關聯使用者與其上傳的檔案記錄。
     */
    private Long userFileId;
    
    /**
     * 伺服器檔案識別碼，對應 ServerFileMetadata 的主鍵。
     * 用於識別檔案在伺服器端的實際儲存位置和元資料。
     */
    private Long serverFileId;
    
    /**
     * 檔案名稱，包含副檔名。
     * 表示使用者上傳時的原始檔案名稱。
     */
    private String filename;
    
    /**
     * 檔案大小，以位元組（bytes）為單位。
     * 表示檔案的實際儲存大小。
     */
    private Long fileSize;
    
    /**
     * 檔案類型枚舉，定義檔案的分類。
     * 用於系統內部對檔案進行分類處理和權限控制。
     */
    private FileEnum fileType;
    
    /**
     * MIME 類型，標準的網際網路媒體類型。
     * 用於識別檔案內容的格式，例如 "image/jpeg"、"text/plain" 等。
     */
    private String mimeType;
    
    /**
     * 父資料夾識別碼，表示檔案所屬的資料夾。
     * 若為 null 則表示檔案位於根目錄。
     */
    private Long parentFolderId;
    
    /**
     * 檔案上傳時間，記錄檔案完成上傳的時間點。
     * 使用本地日期時間格式。
     */
    private LocalDateTime uploadTime;
    
    /**
     * 檔案的 MD5 雜湊值，用於檔案完整性驗證。
     * 確保檔案在傳輸和儲存過程中沒有被損壞或篡改。
     */
    private String md5Hash;


    /**
     * 透過使用者檔案元資料和伺服器檔案元資料建構檔案上傳結果物件。
     * <p>
     * 此建構方法整合兩個實體物件的相關資訊，建立完整的檔案上傳結果表示。
     * 確保從資料庫實體物件中提取正確的欄位對應關係，維持資料的一致性。
     * <p>
     * 建構過程中會自動對應以下欄位：
     * <ul>
     * <li>使用者檔案ID - 來自 UserFileMetadata.id</li>
     * <li>伺服器檔案ID - 來自 ServerFileMetadata.id</li>
     * <li>檔案名稱 - 來自 UserFileMetadata.filename</li>
     * <li>檔案大小 - 來自 ServerFileMetadata.fileSize</li>
     * <li>檔案類型 - 來自 UserFileMetadata.fileType</li>
     * <li>MIME類型 - 來自 ServerFileMetadata.mimeType</li>
     * <li>父資料夾ID - 來自 UserFileMetadata.parentFolderId</li>
     * <li>上傳時間 - 來自 UserFileMetadata.uploadTime</li>
     * <li>MD5雜湊值 - 來自 ServerFileMetadata.md5</li>
     * </ul>
     *
     * @param userFileMetadata 使用者檔案元資料，包含檔案的邏輯資訊和使用者相關屬性
     * @param serverFileMetadata 伺服器檔案元資料，包含檔案的物理儲存資訊和完整性驗證資料
     * @throws NullPointerException 當任一參數為 null 時拋出
     */
    public FileUploadResultBO(UserFileMetadata userFileMetadata, ServerFileMetadata serverFileMetadata) {
        this.userFileId = userFileMetadata.getId();
        this.serverFileId = serverFileMetadata.getId();
        this.filename = userFileMetadata.getFilename();
        this.fileSize = serverFileMetadata.getFileSize();
        this.fileType = userFileMetadata.getFileType();
        this.mimeType = serverFileMetadata.getMimeType();
        this.parentFolderId = userFileMetadata.getParentFolderId();
        this.uploadTime = userFileMetadata.getUploadTime();
        this.md5Hash = serverFileMetadata.getMd5();
    }
}
