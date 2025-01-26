package xyz.dowob.filemanagement.data.file.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.data.file.bo.UserFileDataBO;
import xyz.dowob.filemanagement.entity.ServerFileMetadata;
import xyz.dowob.filemanagement.entity.UserFileMetadata;

import java.time.LocalDateTime;
import java.util.HashSet;
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

    private String filename;

    private Long parentFolderId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastAccessTime;

    private boolean isFolder;

    private Long fileSize;

    private FileEnum fileType;

    private String gridFsId;

    private String md5;

    private Set<Long> shareUsers = new HashSet<>();

    public UserFileListDTO(ServerFileMetadata serverFileMetadata, UserFileMetadata userFileMetadata) {
        this.id = userFileMetadata.getId();
        this.filename = userFileMetadata.getFilename();
        this.parentFolderId = userFileMetadata.getParentFolderId();
        this.createTime = userFileMetadata.getUploadTime();
        this.lastAccessTime = userFileMetadata.getLastAccessTime();
        this.fileSize = serverFileMetadata.getFileSize();
        this.fileType = serverFileMetadata.getFileType();
        this.gridFsId = serverFileMetadata.getGridFsId();
        this.md5 = serverFileMetadata.getMd5();
        this.isFolder = userFileMetadata.getIsFolder();

        if (userFileMetadata.getSharedWithUsers() != null) {
            shareUsers.addAll(userFileMetadata.getSharedWithUsers());
        }
    }

    public UserFileListDTO(UserFileMetadata userFileMetadata) {
        this.id = userFileMetadata.getId();
        this.filename = userFileMetadata.getFilename();
        this.parentFolderId = userFileMetadata.getParentFolderId();
        this.createTime = userFileMetadata.getUploadTime();
        this.lastAccessTime = userFileMetadata.getLastAccessTime();
        this.isFolder = userFileMetadata.getIsFolder();
        if (userFileMetadata.getSharedWithUsers() != null) {
            shareUsers.addAll(userFileMetadata.getSharedWithUsers());
        }
    }

    public UserFileListDTO(UserFileDataBO userFileDataBO) {
        this.id = userFileDataBO.getUserFileId();
        this.filename = userFileDataBO.getFileName();
        this.parentFolderId = userFileDataBO.getParentFolderId();
        this.createTime = userFileDataBO.getUploadTime();
        this.lastAccessTime = userFileDataBO.getLastAccessTime();
        this.fileSize = userFileDataBO.getFileSize();
        this.fileType = userFileDataBO.getFileType();
        this.gridFsId = userFileDataBO.getGridFsId();
        this.md5 = userFileDataBO.getMd5();
        if (userFileDataBO.getShareUsers() != null) {
            shareUsers.addAll(userFileDataBO.getShareUsers());
        }

    }
}
