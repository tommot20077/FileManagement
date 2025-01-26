package xyz.dowob.filemanagement.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
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
     * 是否為資料夾
     */
    @Column("is_folder")
    private Boolean isFolder = false;

    /**
     * 文件大小
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
     * 文件共享給的用戶
     */
    @Column("shared_with_users")
    private Set<Long> sharedWithUsers = new HashSet<>();


    @Override
    public String toString() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("user", userId);
        map.put("serverFile", serverFileId);
        map.put("parentFolder", parentFolderId);
        map.put("filename", filename);
        map.put("isFolder", isFolder);
        map.put("uploadTime", uploadTime);
        map.put("lastAccessTime", lastAccessTime);
        map.put("sharedWithUsers", sharedWithUsers);
        return map.toString();
    }
}
