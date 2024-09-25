package xyz.dowob.filemanagement.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.HashMap;
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
    private User user;

    /**
     * 文件類型
     */
    private ServerFileMetadata serverFile;

    /**
     * 文件名稱
     */
    private String filename;

    /**
     * 文件路徑
     */
    @Column("file_path")
    private String filePath;

    /**
     * 文件大小
     */
    @Column("upload_time")
    private LocalDateTime uploadTime;

    /**
     * 最後訪問時間
     */
    @Column("last_access_time")
    private LocalDateTime lastAccessTime;

    /**
     * 文件是否被刪除
     */
    private Set<User> sharedWithUsers;

    @Override
    public String toString() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("user", user.getId());
        map.put("serverFile", serverFile.getId());
        map.put("filename", filename);
        map.put("filePath", filePath);
        map.put("uploadTime", uploadTime);
        map.put("lastAccessTime", lastAccessTime);
        return map.toString();
    }
}
