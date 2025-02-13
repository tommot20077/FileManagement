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
 * 用戶在線文件歷史數據實體類，用於保存用戶在線文件的歷史數據
 * 保存了文件的修改時間、修改者、版本號、修改內容、備註等信息
 * @author yuan
 * @program FileManagement
 * @ClassName UserOnlineFileHistory
 * @create 2025/2/8
 * @Version 1.0
 **/
@Getter
@Setter
@Table(name = "user_online_file_history")
@NoArgsConstructor
public class UserOnlineFileHistory {
    /**
     * 文件 ID
     */
    @Id
    private Long id;

    /**
     * UserOnlineFile的ID
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
     * 重寫toString方法，將文件元數據轉換為HashMap
     *
     * @return 文件元數據HashMap
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
