package xyz.dowob.filemanagement.data.file.bo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.entity.ServerFileMetadata;
import xyz.dowob.filemanagement.entity.UserFileMetadata;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * @author yuan
 * @program FileManagement
 * @ClassName DownloadTaskBO
 * @create 2025/1/22
 * @Version 1.0
 **/
@Getter
@Setter
@NoArgsConstructor
public class UserFileDataBO {
    private Long userFileId;
    private Long serverFileId;
    private Long userId;
    private String fileName;
    private Long parentFolderId;
    private FileEnum fileType;
    private Long fileSize;
    private Set<Long> shareUsers = new HashSet<>();
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastAccessTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime uploadTime;
    private String gridFsId;
    private String md5;
    @JsonIgnore
    private Flux<DataBuffer> dataStream;

    public UserFileDataBO(ServerFileMetadata serverFileMetadata, UserFileMetadata userFileMetadata) {
        this.userFileId = userFileMetadata.getId();
        this.serverFileId = serverFileMetadata.getId();
        this.userId = userFileMetadata.getUserId();
        this.fileName = userFileMetadata.getFilename();
        this.parentFolderId = userFileMetadata.getParentFolderId();
        this.fileType = serverFileMetadata.getFileType();
        this.fileSize = serverFileMetadata.getFileSize();
        this.lastAccessTime = userFileMetadata.getLastAccessTime();
        this.uploadTime = userFileMetadata.getUploadTime();
        this.gridFsId = serverFileMetadata.getGridFsId();
        this.md5 = serverFileMetadata.getMd5();
        this.shareUsers = userFileMetadata.getSharedWithUsers();
    }

    public UserFileDataBO(UserFileMetadata userFileMetadata) {
        this.userFileId = userFileMetadata.getId();
        this.userId = userFileMetadata.getUserId();
        this.fileName = userFileMetadata.getFilename();
        this.parentFolderId = userFileMetadata.getParentFolderId();
        this.lastAccessTime = userFileMetadata.getLastAccessTime();
        this.uploadTime = userFileMetadata.getUploadTime();
        this.shareUsers = userFileMetadata.getSharedWithUsers();
    }
}
