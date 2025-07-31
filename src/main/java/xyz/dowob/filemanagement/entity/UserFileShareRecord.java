package xyz.dowob.filemanagement.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.HashMap;

/**
 * 使用者檔案分享記錄實體類，對應資料庫中的 user_file_share_record 表。
 * <p>
 * 此實體類管理使用者之間的檔案分享關係，記錄哪些使用者擁有對特定檔案的存取權限。
 * 當使用者將檔案分享給其他使用者時，系統會在此表中建立相對應的記錄。
 * </p>
 * <p>
 * 每筆記錄代表一個使用者對一個檔案的分享權限，支援：
 * <ul>
 *   <li>多使用者共同存取同一檔案</li>
 *   <li>分享權限的快速查詢和驗證</li>
 *   <li>使用者分享檔案清單的管理</li>
 *   <li>檔案存取權限的審計追蹤</li>
 * </ul>
 * </p>
 * <p>
 * 使用範例：
 * <pre>{@code
 * UserFileShareRecord shareRecord = new UserFileShareRecord(userId, fileId);
 * // 建立使用者與檔案之間的分享關係
 * }</pre>
 * </p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see UserFileMetadata
 * @see User
 **/
@Table("user_file_share_record")
@Getter
@Setter
@NoArgsConstructor
public class UserFileShareRecord {
    /**
     * 分享記錄的唯一識別碼。
     * <p>
     * 作為資料庫主鍵，唯一標識每筆使用者檔案分享記錄。
     * 系統自動產生，用於內部識別和關聯查詢。
     * </p>
     */
    @Id
    private Long id;

    /**
     * 獲得檔案存取權限的使用者 ID。
     * <p>
     * 外鍵參照 user 表的主鍵，指向擁有檔案存取權限的使用者。
     * 此使用者可以是檔案的原始擁有者或透過分享獲得存取權限的使用者。
     * 用於權限驗證和分享關係管理。
     * </p>
     */
    private Long userId;

    /**
     * 檔案ID
     */
    private Long fileId;


    public UserFileShareRecord(Long userId, Long fileId) {
        this.userId = userId;
        this.fileId = fileId;
    }

    /**
     * 重寫hashCode方法，用於判斷使用者是否相同
     *
     * @return hashCode
     */
    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /**
     * 重寫equals方法，用於判斷用戶是否相同
     *
     * @param o Object
     *
     * @return boolean
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UserFileShareRecord record = (UserFileShareRecord) o;
        return id.equals(record.id);
    }

    /**
     * 重寫toString方法，用於打印對象
     *
     * @return String
     */
    @Override
    public String toString() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("userId", userId);
        map.put("fileId", fileId);
        return map.toString();
    }
}
