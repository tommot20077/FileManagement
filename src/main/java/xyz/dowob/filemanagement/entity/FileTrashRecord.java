package xyz.dowob.filemanagement.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.HashMap;

/**
 * 檔案回收站記錄實體類別，對應資料庫中的 file_trash_record 表。
 * <p>
 * 此實體類別管理已刪除檔案的記錄資訊，作為檔案復原機制的基礎。
 * 當使用者刪除檔案時，檔案的元資料資訊會被暫時保存在此表中，
 * 提供恢復功能並追蹤刪除操作的歷史記錄。
 * </p>
 * <p>
 * 此類別與 {@link UserFileMetadata} 存在一對一的關聯關係，
 * 透過 fileId 欄位建立連結。當檔案被移至回收站時，
 * 其原有的父資料夾資訊和刪除時間會被記錄，
 * 以便在復原時能夠準確還原檔案的原始位置。
 * </p>
 * <p>
 * 主要功能包括：
 * <ul>
 *   <li>記錄已刪除檔案的基本識別資訊</li>
 *   <li>保存檔案原始的父資料夾位置</li>
 *   <li>追蹤檔案刪除的確切時間</li>
 *   <li>支援檔案復原操作的資料基礎</li>
 * </ul>
 * </p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see UserFileMetadata
 */
@Getter
@Setter
@Table(name = "file_trash_record")
@NoArgsConstructor
public class FileTrashRecord {

    /**
     * 檔案唯一識別碼，與 {@link UserFileMetadata} 的 ID 保持一致。
     * <p>
     * 此欄位作為主鍵，同時也是與使用者檔案元資料表的外鍵關聯，
     * 確保回收站記錄與原始檔案資訊的一對一對應關係。
     * </p>
     */
    @Id
    @Column("file_id")
    private Long fileId;

    /**
     * 檔案擁有者的使用者識別碼。
     * <p>
     * 記錄執行刪除操作的使用者 ID，用於權限驗證和檔案復原時的擁有者確認。
     * 確保只有檔案的合法擁有者才能對回收站中的檔案進行操作。
     * </p>
     */
    @Column("user_id")
    private Long userId;

    /**
     * 檔案被刪除前所在的父資料夾識別碼。
     * <p>
     * 保存檔案原始的目錄位置資訊，當使用者選擇復原檔案時，
     * 系統會根據此 ID 將檔案還原到原始的資料夾位置。
     * 如果原始資料夾已被刪除，則需要額外的處理邏輯。
     * </p>
     */
    @Column("parent_folder_id")
    private Long parentFolderId;

    /**
     * 檔案被移至回收站的確切時間戳。
     * <p>
     * 記錄刪除操作的精確時間，用於：
     * <ul>
     *   <li>追蹤檔案刪除歷史</li>
     *   <li>實施自動清理策略</li>
     *   <li>提供時間排序功能</li>
     *   <li>審計和日誌記錄</li>
     * </ul>
     * </p>
     */
    @Column("delete_time")
    private LocalDateTime deleteTime;

    /**
     * 建構檔案回收站記錄實例，基於使用者檔案元資料和刪除時間。
     * <p>
     * 此建構方法會自動提取使用者檔案元資料中的關鍵資訊，
     * 包括檔案 ID、使用者 ID 和父資料夾 ID，並結合指定的刪除時間
     * 建立完整的回收站記錄。
     * </p>
     *
     * @param userFileMetadata 使用者檔案元資料物件，包含檔案的基本資訊
     * @param deleteTime 檔案被刪除的時間戳
     * @throws NullPointerException 如果 userFileMetadata 或 deleteTime 為 null
     */
    public FileTrashRecord(UserFileMetadata userFileMetadata, LocalDateTime deleteTime) {
        this.fileId = userFileMetadata.getId();
        this.userId = userFileMetadata.getUserId();
        this.parentFolderId = userFileMetadata.getParentFolderId();
        this.deleteTime = deleteTime;
    }

    /**
     * 計算檔案回收站記錄物件的雜湊碼，基於檔案的唯一識別碼（fileId）。
     * <p>
     * 此實現確保具有相同檔案 ID 的回收站記錄物件具有相同的雜湊碼，
     * 這對於在集合類別（如 HashSet、HashMap）中正確運作是必要的。
     * 遵循 equals-hashCode 合約的要求。
     * </p>
     *
     * @return 基於檔案 ID 的雜湊碼
     * @see #equals(Object)
     */
    @Override
    public int hashCode() {
        return fileId.hashCode();
    }

    /**
     * 判斷兩個檔案回收站記錄物件是否相等，基於檔案的唯一識別碼（fileId）。
     * <p>
     * 此實現遵循 equals 方法的標準契約，確保具有相同檔案 ID 的
     * 回收站記錄被視為相等，這對於集合操作和重複檢測至關重要。
     * </p>
     *
     * @param obj 用於比較的物件
     * @return 如果兩個回收站記錄的檔案 ID 相同則回傳 true，否則回傳 false
     * @see #hashCode()
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        FileTrashRecord that = (FileTrashRecord) obj;
        return that.fileId.equals(fileId);
    }

    /**
     * 將檔案回收站記錄轉換為可讀的字串表示形式。
     * <p>
     * 此方法將回收站記錄的關鍵資訊組織成 HashMap 結構並轉換為字串，
     * 方便進行日誌記錄、除錯和資料展示。包含檔案 ID、使用者 ID、
     * 父資料夾 ID 和刪除時間等重要資訊。
     * </p>
     *
     * @return 包含回收站記錄資訊的字串表示，格式為 HashMap 的字串形式
     */
    @Override
    public String toString() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("fileId", fileId);
        map.put("userId", userId);
        map.put("parentFolderId", parentFolderId);
        map.put("deleteTime", deleteTime);
        return map.toString();
    }
}
