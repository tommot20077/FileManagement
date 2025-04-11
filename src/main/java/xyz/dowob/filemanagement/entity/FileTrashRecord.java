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
 * 文件回收站記錄實體類，用於記錄文件回收站的文件
 *
 * @author yuan
 * @program FileManagement
 * @ClassName FileTrashCan
 * @create 2025/2/22
 * @Version 1.0
 **/
@Getter
@Setter
@Table(name = "file_trash_record")
@NoArgsConstructor
public class FileTrashRecord {

    /**
     * 文件 ID，此與{@link UserFileMetadata}的 ID 一致
     */
    @Id
    @Column("file_id")
    private Long fileId;

    /**
     * 用戶 ID
     */
    @Column("user_id")
    private Long userId;

    /**
     * 父文件夾 ID
     */
    @Column("parent_folder_id")
    private Long parentFolderId;

    /**
     * 刪除時間
     */
    @Column("delete_time")
    private LocalDateTime deleteTime;

    /**
     * 有參數的構造方法
     *
     * @param userFileMetadata 用戶文件元數據
     * @param deleteTime       刪除時間
     */
    public FileTrashRecord(UserFileMetadata userFileMetadata, LocalDateTime deleteTime) {
        this.fileId = userFileMetadata.getId();
        this.userId = userFileMetadata.getUserId();
        this.parentFolderId = userFileMetadata.getParentFolderId();
        this.deleteTime = deleteTime;
    }


    /**
     * 重寫 toString 方法
     *
     * @return 返回對象的字符串表示
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


    /**
     * 重寫 equals 方法
     *
     * @param obj 比較對象
     *
     * @return 返回比較結果
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
}
