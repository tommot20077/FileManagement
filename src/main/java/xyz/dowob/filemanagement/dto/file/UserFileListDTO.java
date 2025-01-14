package xyz.dowob.filemanagement.dto.file;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.entity.ServerFileMetadata;
import xyz.dowob.filemanagement.entity.UserFileMetadata;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * @author yuan
 * @program FileManagement
 * @ClassName UserFileListDTO
 * @create 2025/1/14
 * @Version 1.0
 **/
@Getter
@Setter
public class UserFileListDTO {
    private Long id;

    private Long userId;

    private String filename;

    private String filePath;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastAccessTime;

    private Long fileSize;

    private FileEnum fileType;

    private String gridFsId;

    private String md5;

    private Set<Long> shareUsers;

    public UserFileListDTO (ServerFileMetadata serverFileMetadata, UserFileMetadata userFileMetadata) {
        this.id = userFileMetadata.getId();
        this.userId = userFileMetadata.getUserId();
        this.filename = userFileMetadata.getFilename();
        this.filePath = userFileMetadata.getFilePath();
        this.createTime = userFileMetadata.getUploadTime();
        this.lastAccessTime = userFileMetadata.getLastAccessTime();
        this.shareUsers = userFileMetadata.getSharedWithUsers();
        this.fileSize = serverFileMetadata.getFileSize();
        this.fileType = serverFileMetadata.getFileType();
        this.gridFsId = serverFileMetadata.getGridFsId();
        this.md5 = serverFileMetadata.getMd5();
    }
}
