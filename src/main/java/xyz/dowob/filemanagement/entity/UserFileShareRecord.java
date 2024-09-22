package xyz.dowob.filemanagement.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.HashMap;

/**
 * 用戶檔案分享記錄實體類，用於映射數據庫中的user_file_share_record表
 * 用於儲存用戶分享檔案的記錄，包含用戶ID、檔案ID，當用戶分享檔案時，會在此表中新增一條記錄
 * 後續用戶可以根據此表中的記錄來查詢用戶分享的檔案
 *
 * @author yuan
 * @program FileManagement
 * @ClassName UserFIleShareRecord
 * @create 2025/3/5
 * @Version 1.0
 **/
@Table("user_file_share_record")
@Getter
@Setter
@NoArgsConstructor
public class UserFileShareRecord {
    /**
     * 主鍵ID
     */
    @Id
    private Long id;

    /**
     * 用戶ID
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
     * 重寫hashCode方法，用於判斷用戶是否相同
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
