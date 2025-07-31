package xyz.dowob.filemanagement.data.file.dao;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.customenum.FileShareTypeEnum;

import java.time.LocalDateTime;

/**
 * 用戶檔案元資料與伺服器資料的資料存取對象，封裝聯結查詢結果。
 * 用於一次性獲取用戶檔案元資料和對應的伺服器檔案資訊。
 *
 * <p>此類別整合來自多個表的資料，包括用戶檔案元資料、伺服器檔案元資料和用戶資訊。
 * 通過此對象可以減少多次數據庫查詢，提高效能。字段命名使用縮寫形式以区分來源表。
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@Data
public class UserFileMetaWithDataDAO {
    /**
     * 用戶檔案元資料識別符（ufm = UserFileMetadata）
     */
    private Long ufmId;

    /**
     * 用戶檔案名稱，包含副檔名
     */
    private String ufmFilename;

    /**
     * 父資料夾識別符，為 null 則表示根目錄
     */
    private Long ufmParentFolderId;

    /**
     * 是否為星標檔案，用於快速訪問
     */
    private Boolean ufmIsStar;

    /**
     * 檔案類型枚舉，定義檔案的基本類別
     */
    private FileEnum ufmFileType;

    /**
     * 檔案共享類型，定義檔案的存取權限
     */
    private FileShareTypeEnum ufmShareType;

    /**
     * 檔案初始上傳時間
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime ufmUploadTime;

    /**
     * 最後存取時間，記錄檔案最後一次被存取的時間
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime ufmLastAccessTime;

    /**
     * 是否已被標記為已刪除，支援軟刪除功能
     */
    private Boolean ufmIsDeleted;

    /**
     * 伺服器檔案元資料識別符（sfm = ServerFileMetadata）
     */
    private Long sfmId;

    /**
     * 檔案大小（以位元組為單位）
     */
    private Long sfmFileSize;

    /**
     * 檔案 MIME 類型，用於內容識別和處理
     */
    private String sfmMimeType;

    /**
     * MongoDB GridFS 檔案識別符，用於二進位資料存取
     */
    private String sfmGridFsId;

    /**
     * 檔案 MD5 校驗值，用於檔案完整性驗證
     */
    private String sfmMd5;

    /**
     * 在線檔案識別符（uof = UserOnlineFile），為 null 則表示非在線檔案
     */
    private Long uofId;

    /**
     * 檔案擁有者的用戶識別符
     */
    private Long ufmUserId;

    /**
     * 檔案擁有者的用戶名，用於顯示和權限管理
     */
    private String ownerUsername;
}
