package xyz.dowob.filemanagement.data.file.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.data.file.bo.UserFileDataBO;
import xyz.dowob.filemanagement.entity.ServerFileMetadata;
import xyz.dowob.filemanagement.entity.UserFileMetadata;
import xyz.dowob.filemanagement.entity.UserOnlineFile;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 用戶文件列表數據傳輸對象，用於封裝用戶文件列表的數據
 *
 * @author yuan
 * @program FileManagement
 * @ClassName UserFileListDTO
 * @create 2025/1/14
 * @Version 1.0
 **/
@Getter
@Setter
@NoArgsConstructor
public class UserFileListDTO {
    /**
     * 文件ID
     */
    private Long id;

    /**
     * 文件名稱
     */
    private String filename;

    /**
     * 父文件夾ID
     */
    private Long parentFolderId;

    /**
     * 創建時間
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 最後更改時間
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastAccessTime;

    /**
     * 是否為文件夾
     */
    private boolean isFolder;

    /**
     * 文件大小
     */
    private Long fileSize;

    /**
     * 文件類型
     */
    private FileEnum fileType;

    /**
     * 文件GridFsId
     */
    private String gridFsId;

    /**
     * 文件MD5值
     */
    private String md5;

    /**
     * 共享用戶
     */
    private Set<Long> shareUsers = new HashSet<>();

    /**
     * 用戶文件列表數據傳輸對象構造函數
     *
     * @param serverFileMetadata 服務器文件元數據對象
     * @param userFileMetadata   用戶文件元數據對象
     */
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

    /**
     * 用戶文件列表數據傳輸對象構造函數
     *
     * @param userOnlineFile   用戶在線文件對象
     * @param userFileMetadata 用戶文件元數據對象
     */
    public UserFileListDTO(UserOnlineFile userOnlineFile, UserFileMetadata userFileMetadata) {
        this.id = userFileMetadata.getId();
        this.filename = userFileMetadata.getFilename();
        this.parentFolderId = userFileMetadata.getParentFolderId();
        this.createTime = userFileMetadata.getUploadTime();
        this.lastAccessTime = userFileMetadata.getLastAccessTime();
        this.fileSize = userOnlineFile.getFileSize();
        this.fileType = userFileMetadata.getFileType();
        this.isFolder = userFileMetadata.getIsFolder();

        if (userFileMetadata.getSharedWithUsers() != null) {
            shareUsers.addAll(userFileMetadata.getSharedWithUsers());
        }
    }


    /**
     * 用戶文件列表數據傳輸對象構造函數
     *
     * @param userFileMetadata 用戶文件元數據對象
     */
    public UserFileListDTO(UserFileMetadata userFileMetadata) {
        this.id = userFileMetadata.getId();
        this.filename = userFileMetadata.getFilename();
        this.parentFolderId = userFileMetadata.getParentFolderId();
        this.createTime = userFileMetadata.getUploadTime();
        this.fileType = userFileMetadata.getFileType();
        this.lastAccessTime = userFileMetadata.getLastAccessTime();
        this.isFolder = userFileMetadata.getIsFolder();
        if (userFileMetadata.getSharedWithUsers() != null) {
            shareUsers.addAll(userFileMetadata.getSharedWithUsers());
        }
    }

    /**
     * 用戶文件列表數據傳輸對象構造函數
     *
     * @param userFileDataBO 用戶文件數據業務對象
     */
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
