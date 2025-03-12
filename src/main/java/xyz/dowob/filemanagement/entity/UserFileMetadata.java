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
 * 用於定義用戶文件元數據表
 *
 * @author yuan
 * @program File-Management
 * @ClassName UserFileMetadata
 * @description
 * @create 2024-09-20 23:29
 * @Version 1.0
 **/
@Getter
@Setter
@Table(name = "user_file_metadata")
public class UserFileMetadata {
    /**
     * 文件 ID
     */
    @Id
    private Long id;

    /**
     * 文件擁有者
     */
    @Column("user_id")
    private Long userId;

    /**
     * 文件所在伺服器的文件ID
     */
    @Column("server_file_id")
    private Long serverFileId;

    /**
     * 文件名稱
     */
    private String filename;

    /**
     * 文件的父資料夾ID（用於樹狀結構）
     * 若為根目錄，則為 null
     */
    @Column("parent_folder_id")
    private Long parentFolderId;

    /**
     * 是否為星標文件
     */
    @Column("is_star")
    private Boolean isStar = false;

    /**
     * 文件類型，此與ServerFileMetadata的fileType相同
     * 部分自定義檔案無伺服器文件元數據，因此需要在此處定義
     */
    @Column("file_type")
    private FileEnum fileType = FileEnum.OTHER;

    /**
     * 分享類型，默認為不分享
     */
    @Column("share_type")
    private FileShareTypeEnum shareType = FileShareTypeEnum.NONE;

    /**
     * 上傳時間
     */
    @Column("upload_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime uploadTime;

    /**
     * 最後訪問時間
     */
    @Column("last_access_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastAccessTime;


    /**
     * 是否標記為刪除
     */
    @Column("is_deleted")
    private Boolean isDeleted = false;

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
        UserFileMetadata that = (UserFileMetadata) o;
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
