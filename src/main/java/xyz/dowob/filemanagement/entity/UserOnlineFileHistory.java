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
 * 用戶線上檔案歷史記錄實體類，對應資料庫中的 user_online_file_history 表。
 * <p>
 * 此實體類實現完整的版本控制系統，追蹤線上檔案的所有變更歷史和快照記錄。
 * 支援增量差異儲存、完整快照備份和版本分支管理，確保協作編輯過程中的
 * 資料完整性和可恢復性。
 * </p>
 * <p>
 * 版本控制機制包括：
 * <ul>
 *   <li>增量差異追蹤（diff 儲存）和完整快照備份</li>
 *   <li>線性版本鏈和分支版本管理</li>
 *   <li>修改者身份追蹤和時間戳記錄</li>
 *   <li>可選的版本註釋和標記功能</li>
 * </ul>
 * </p>
 * <p>
 * 快照策略：系統會定期建立完整內容快照，減少版本恢復時的計算開銷。
 * 在非快照版本中，僅儲存與前一版本的差異資料，實現空間效率最佳化。
 * </p>
 * <p>
 * 使用範例：
 * <pre>{@code
 * UserOnlineFileHistory history = new UserOnlineFileHistory();
 * history.setFileId(fileId);
 * history.setVersion(newVersion);
 * history.setDiff(deltaChanges);
 * history.setModifiedBy(userId);
 * history.setModifiedTime(LocalDateTime.now());
 * }</pre>
 * </p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see UserOnlineFile
 * @see UserFileMetadata
 **/
@Getter
@Setter
@Table(name = "user_online_file_history")
@NoArgsConstructor
public class UserOnlineFileHistory {
    /**
     * 歷史記錄的唯一識別碼。
     * <p>
     * 作為資料庫主鍵，唯一標識每筆檔案歷史版本記錄。
     * 用於版本追蹤、歷史查詢和版本恢復操作的核心識別標準。
     * </p>
     */
    @Id
    private Long id;

    /**
     * 關聯的線上檔案 ID。
     * <p>
     * 外鍵參照 user_online_file 表的主鍵，建立歷史記錄與當前檔案之間的關聯。
     * 一個線上檔案可以有多個歷史版本記錄，形成完整的版本歷史鏈。
     * 用於版本查詢、歷史追蹤和檔案恢復功能。
     * </p>
     */
    @Column("file_id")
    private Long fileId;

    /**
     * 版本號
     */
    private Long version;

    /**
     * 上一個版本號
     */
    @Column("previous_version")
    private Long previousVersion;

    /**
     * 修改內容
     */
    private String diff;

    /**
     * 備註
     */
    private String note;

    /**
     * 是否為快照
     */
    @Column("is_snapshot")
    private Boolean isSnapshot = false;

    /**
     * 快照內容（如果是快照）
     */
    @Column("snapshot_content")
    private String snapshotContent;

    /**
     * 修改時間
     */
    @Column("modified_time")
    private LocalDateTime modifiedTime;

    /**
     * 修改者
     */
    @Column("modified_by")
    private Long modifiedBy;

    /**
     * 重寫hashCode方法，獲取對象的hashCode
     *
     * @return hashCode
     */
    @Override
    public int hashCode() {
        return id.hashCode();
    }


    /**
     * 重寫equals方法，使用id作為判斷是否相等的依據
     *
     * @param o 用於比較的對象
     *
     * @return 是否相等
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UserOnlineFileHistory that = (UserOnlineFileHistory) o;
        return id.equals(that.id);
    }


    /**
     * 重寫toString方法，將檔案元資料轉換為HashMap
     *
     * @return 檔案元資料HashMap
     */
    @Override
    public String toString() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("fileId", fileId);
        map.put("version", version);
        map.put("diff", formatString(diff));
        map.put("note", note);
        map.put("isSnapshot", isSnapshot);
        map.put("snapshotContent", formatString(snapshotContent));
        map.put("modifiedTime", modifiedTime);
        map.put("modifiedBy", modifiedBy);
        return map.toString();
    }


    /**
     * 格式化字符串，如果字符串長度大於30，則截取前30個字符並添加省略號
     *
     * @param str 需要格式化的字符串
     *
     * @return 格式化後的字符串
     */
    private String formatString(String str) {
        if (str == null) {
            return null;
        }
        if (str.length() > 30) {
            return str.substring(0, 30).concat("...");
        }
        return str;
    }
}
