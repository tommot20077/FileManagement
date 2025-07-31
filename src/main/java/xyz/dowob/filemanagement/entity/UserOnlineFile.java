package xyz.dowob.filemanagement.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.HashMap;

/**
 * 用戶線上檔案實體類，對應資料庫中的 user_online_file 表。
 * <p>
 * 此實體類管理協作編輯功能中的線上檔案內容和版本控制資訊。支援多用戶即時協作編輯，
 * 包含檔案的當前內容、版本歷史追蹤、快照管理和同步狀態等核心功能。
 * </p>
 * <p>
 * 與版本控制系統的整合：
 * <ul>
 *   <li>自動版本快照和增量更新追蹤</li>
 *   <li>多用戶協作衝突檢測和解決</li>
 *   <li>檔案內容的 JSON 格式結構化存儲</li>
 *   <li>檔案大小監控和存儲最佳化</li>
 * </ul>
 * </p>
 * <p>
 * 使用範例：
 * <pre>{@code
 * UserOnlineFile onlineFile = new UserOnlineFile();
 * onlineFile.setContent("{\"ops\":[{\"insert\":\"Hello World\"}]}");
 * onlineFile.setLastModifiedBy(userId);
 * onlineFile.setFileSize(content.getBytes().length);
 * }</pre>
 * </p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see UserOnlineFileHistory
 * @see UserFileMetadata
 **/
@Getter
@Setter
@Table(name = "user_online_file")
public class UserOnlineFile {
    /**
     * 線上檔案的唯一識別碼。
     * <p>
     * 此 ID 與 user_file_metadata 表的主鍵一對一對應，用於關聯用戶檔案元資料
     * 和線上編輯內容。作為協作編輯功能的核心識別標準，確保檔案內容的一致性
     * 和版本控制的正確性。
     * </p>
     */
    @Id
    private Long id;

    /**
     * 檔案內容的位元組大小。
     * <p>
     * 記錄檔案當前內容的實際大小（以位元組為單位），用於存儲空間管理、
     * 傳輸最佳化和檔案大小限制檢查。會隨著檔案內容的編輯而即時更新，
     * 協助系統監控存儲使用量和效能最佳化。預設值為 0L。
     * </p>
     */
    @Column("file_size")
    private Long fileSize = 0L;

    /**
     * 檔案內容，用於存儲檔案的內容為JSON格式
     */
    private String content;

    /**
     * 最後修改者
     */
    @Column("last_modified_by")
    private Long lastModifiedBy;

    /**
     * 距離上次快照的間隔版本數量
     */
    @Column("current_snapshot_count")
    private Integer currentSnapshotCount;

    /**
     * 最後一次歷史版本
     */
    @Column("last_history_version")
    private Long lastHistoryVersion;

    /**
     * 是否匹配最後的歷史記錄
     */
    @Column("is_match_history")
    private Boolean isMatchHistory;

    /**
     * 計算用戶線上檔案物件的雜湊碼，基於檔案的唯一識別碼（ID）。
     * <p>
     * 此實現確保具有相同 ID 的線上檔案物件具有相同的雜湊碼，
     * 這對於在集合類別（如 HashSet、HashMap）中正確運作是必要的。
     * 遵循 equals-hashCode 合約的要求。
     * </p>
     *
     * @return 基於線上檔案 ID 的雜湊碼
     * @see #equals(Object)
     */
    @Override
    public int hashCode() {
        return id.hashCode();
    }


    /**
     * 判斷兩個用戶線上檔案物件是否相等，基於檔案的唯一識別碼（ID）。
     * <p>
     * 此方法遵循 equals 方法的標準實現模式，確保具有相同 ID 的線上檔案
     * 物件被視為相等。這對於協作編輯功能中的檔案管理至關重要。
     * </p>
     *
     * @param o 用於比較的物件
     * @return 如果兩個線上檔案物件的 ID 相同則回傳 true，否則回傳 false
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
        UserOnlineFile that = (UserOnlineFile) o;
        return id.equals(that.id);
    }


    /**
     * 將用戶線上檔案資訊轉換為可讀的字串表示形式。
     * <p>
     * 此方法將線上檔案的關鍵資訊組織成 HashMap 結構並轉換為字串，
     * 方便進行日誌記錄和除錯。檔案內容會被安全截斷，避免過長的輸出。
     * </p>
     *
     * @return 包含線上檔案資訊的字串表示，格式為 HashMap 的字串形式
     * @see #formatString(String)
     */
    @Override
    public String toString() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("fileSize", fileSize);
        map.put("content", formatString(content));
        map.put("lastModifiedBy", lastModifiedBy);
        map.put("currentSnapshotCount", currentSnapshotCount);
        return map.toString();
    }


    /**
     * 安全地格式化長字串，防止過長內容佔用過多輸出空間。
     * <p>
     * 如果輸入字串長度超過 30 個字元，則截取前 30 個字元並附加省略號。
     * 用於 {@link #toString()} 方法中安全顯示檔案內容或其他長文本資料。
     * </p>
     *
     * @param str 需要格式化的原始字串，可為 null
     * @return 格式化後的字串，長度不超過 33 個字元（包含省略號）
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
