package xyz.dowob.filemanagement.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.customenum.FileShareTypeEnum;

import java.time.LocalDateTime;
import java.util.HashMap;

/**
 * 基於 Spring Data R2DBC 的用戶檔案元資料實體，對應 user_file_metadata 表。
 * <p>
 * 此實體管理每個用戶視角下的檔案元資料，支援個人化檔案組織和權限控制。
 * 與 ServerFileMetadata 形成一對多關係，允許多個用戶擁有同一實體檔案的獨立元資料。
 * 提供樹狀目錄結構、軟刪除機制、星標標記及分享類型管理功能。
 * <p>
 * 此實體具備完整的 equals、hashCode 和 toString 實現，基於唯一識別碼進行物件比較，
 * 確保在集合操作中的正確性。時間欄位使用 JSON 格式化註解，便於 API 序列化。
 * 所有欄位預設值已適當設定，支援安全的物件初始化。
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see ServerFileMetadata
 * @see FileEnum
 * @see FileShareTypeEnum
 */
@Getter
@Setter
@Table(name = "user_file_metadata")
public class UserFileMetadata {
    /**
     * 檔案元資料的唯一識別碼，作為主鍵。
     * 用於檔案元資料的唯一標識和資料庫關聯操作。
     */
    @Id
    private Long id;

    /**
     * 檔案擁有者的用戶識別碼。
     * 建立檔案與用戶之間的所有權關係，支援多用戶檔案管理。
     */
    @Column("user_id")
    private Long userId;

    /**
     * 對應的伺服器檔案元資料識別碼。
     * 連結至 ServerFileMetadata 實體，建立用戶視角與實體檔案的關聯。
     * 允許多個用戶元資料指向同一實體檔案。
     */
    @Column("server_file_id")
    private Long serverFileId;

    /**
     * 用戶定義的檔案名稱。
     * 支援用戶個人化的檔案命名，可與實體檔案名稱不同。
     */
    private String filename;

    /**
     * 父資料夾的檔案元資料識別碼，用於建構樹狀目錄結構。
     * 當值為 null 時表示檔案位於根目錄。支援遞迴目錄遍歷和路徑解析。
     */
    @Column("parent_folder_id")
    private Long parentFolderId;

    /**
     * 星標標記狀態，用於檔案的個人化標記。
     * 預設值為 false，支援用戶快速篩選重要檔案。
     */
    @Column("is_star")
    private Boolean isStar = false;

    /**
     * 檔案類型枚舉，定義檔案的分類和處理方式。
     * 通常與 ServerFileMetadata 的 fileType 保持一致，但對於無實體檔案的
     * 虛擬項目（如資料夾）提供獨立的類型定義。預設值為 OTHER。
     */
    @Column("file_type")
    private FileEnum fileType = FileEnum.OTHER;

    /**
     * 檔案分享權限類型，控制檔案的存取範圍。
     * 預設值為 DEFAULT，支援私有、公開、連結分享等多種分享模式。
     */
    @Column("share_type")
    private FileShareTypeEnum shareType = FileShareTypeEnum.DEFAULT;

    /**
     * 檔案上傳或建立的時間戳記。
     * 使用 JsonFormat 註解格式化為 "yyyy-MM-dd HH:mm:ss" 格式，便於 API 回應。
     */
    @Column("upload_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime uploadTime;

    /**
     * 檔案最後一次被存取的時間戳記。
     * 用於追蹤檔案使用情況和實施存取策略。格式化為 "yyyy-MM-dd HH:mm:ss"。
     */
    @Column("last_access_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastAccessTime;


    /**
     * 軟刪除標記，指示檔案是否已被邏輯刪除。
     * 預設值為 false，當值為 true 時檔案在一般查詢中被隱藏，支援回收站功能。
     */
    @Column("is_deleted")
    private Boolean isDeleted = false;

    /**
     * 計算用戶檔案元資料物件的雜湊碼，基於檔案的唯一識別碼（ID）。
     * <p>
     * 此實現確保具有相同 ID 的檔案元資料物件具有相同的雜湊碼，
     * 這對於在集合類別（如 HashSet、HashMap）中正確運作是必要的。
     * 遵循 equals-hashCode 合約的要求。
     * </p>
     *
     * @return 基於檔案 ID 的雜湊碼
     * @see #equals(Object)
     */
    @Override
    public int hashCode() {
        return id.hashCode();
    }


    /**
     * 判斷兩個用戶檔案元資料物件是否相等，基於檔案的唯一識別碼（ID）。
     * <p>
     * 此方法遵循 equals 方法的標準實現模式，確保具有相同 ID 的檔案元資料
     * 物件被視為相等。這對於檔案的唯一性識別和集合操作至關重要。
     * </p>
     *
     * @param o 用於比較的物件
     * @return 如果兩個檔案元資料物件的 ID 相同則回傳 true，否則回傳 false
     * @see #hashCode()
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UserFileMetadata that = (UserFileMetadata) o;
        return id.equals(that.id);
    }


    /**
     * 將用戶檔案元資料轉換為可讀的字串表示形式。
     * <p>
     * 此方法將檔案元資料的關鍵資訊組織成 HashMap 結構並轉換為字串，
     * 方便進行日誌記錄、除錯和資料展示。包含檔案的基本資訊、
     * 時間戳記和狀態標記等重要屬性。
     * </p>
     *
     * @return 包含檔案元資料資訊的字串表示，格式為 HashMap 的字串形式
     */
    @Override
    public String toString() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("user", userId);
        map.put("serverFile", serverFileId);
        map.put("parentFolder", parentFolderId);
        map.put("filename", filename);
        map.put("uploadTime", uploadTime);
        map.put("lastAccessTime", lastAccessTime);
        map.put("isDeleted", isDeleted);
        map.put("isStar", isStar);
        map.put("fileType", fileType);
        return map.toString();
    }
}
