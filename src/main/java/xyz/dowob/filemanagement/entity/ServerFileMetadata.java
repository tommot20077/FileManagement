package xyz.dowob.filemanagement.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import xyz.dowob.filemanagement.customenum.FileEnum;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Set;

/**
 * 用於定義以及映射伺服器文件元數據表
 * @author yuan
 * @program File-Management
 * @ClassName ServerFileMetadata
 * @description
 * @create 2024-09-20 22:30
 * @Version 1.0
 **/
@Getter
@Setter
@Table(name = "server_file_metadata")
public class ServerFileMetadata {
    /**
     * 文件的主鍵ID
     */
    @Id
    private Long id;

    /**
     * 文件名稱
     */
    @Column("file_size")
    private Long fileSize;

    /**
     * 文件類型
     */
    @Column("file_type")
    private FileEnum fileType;

    /**
     * 文件名稱
     */
    @Column("upload_time")
    private LocalDateTime uploadTime;

    /**
     * 文件最後訪問時間
     */
    @Column("last_access_time")
    private LocalDateTime lastAccessTime;

    /**
     * 文件的GridFS ID
     */
    @Column("grid_fs_id")
    private String gridFsId;

    /**
     * 文件的MD5值
     */
    private String md5;

    /**
     * 擁有文件的用戶
     */
    private Set<User> owners;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ServerFileMetadata that = (ServerFileMetadata) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        HashMap<String, Object> fileMap = new HashMap<>();
        fileMap.put("id", id);
        fileMap.put("fileSize", fileSize);
        fileMap.put("fileType", fileType);
        fileMap.put("uploadTime", uploadTime);
        fileMap.put("lastAccessTime", lastAccessTime);
        fileMap.put("gridFsId", gridFsId);
        fileMap.put("md5", md5);
        return fileMap.toString();
    }

}
