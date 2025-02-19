package xyz.dowob.filemanagement.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.HashMap;

/**
 * 用戶在線文件實體類，用於保存用戶在線文件的元數據
 * 保存了文件的大小、內容、最後修改者、快照數量等信息
 * @author yuan
 * @program FileManagement
 * @ClassName OnlineFile
 * @create 2025/2/8
 * @Version 1.0
 **/
@Getter
@Setter
@Table(name = "user_online_file")
public class UserOnlineFile {
    /**
     * 文件 ID
     */
    @Id
    private Long id;

    /**
     * 文件大小
     */
    @Column("file_size")
    private Long fileSize = 0L;

    /**
     * 檔案內容，用於存儲文件的內容為JSON格式
     */
    private String content;

    /**
     * 最後修改者
     */
    @Column("last_modified_by")
    private Long lastModifiedBy;

    /**
     * 快照數量
     */
    @Column("current_snapshot_count")
    private Integer currentSnapshotCount = 0;

    /**
     * 最後一次歷史版本
     */
    @Column("last_history_version")
    private Long lastHistoryVersion = 0L;
    /**
     * 是否匹配最後的歷史紀錄
     */
    @Column("is_match_history")
    private Boolean isMatchHistory = true;

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
        UserOnlineFile that = (UserOnlineFile) o;
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
        map.put("fileSize", fileSize);
        map.put("content", formatString(content));
        map.put("lastModifiedBy", lastModifiedBy);
        map.put("currentSnapshotCount", currentSnapshotCount);
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
