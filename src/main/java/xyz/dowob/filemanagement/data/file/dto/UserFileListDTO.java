package xyz.dowob.filemanagement.data.file.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.customenum.FileShareTypeEnum;
import xyz.dowob.filemanagement.data.file.dao.UserFileMetaWithDataDAO;
import xyz.dowob.filemanagement.entity.ServerFileMetadata;
import xyz.dowob.filemanagement.entity.UserFileMetadata;
import xyz.dowob.filemanagement.entity.UserOnlineFile;

import java.time.LocalDateTime;
import java.util.Collection;
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
     * 是否為星標文件
     */
    private Boolean isStar;

    /**
     * 文件大小
     */
    private Long fileSize;

    /**
     * 文件類型
     */
    private FileEnum fileType;

    /**
     * 文件共享類型
     */
    private FileShareTypeEnum shareType;

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
     * 是否刪除
     */
    private Boolean isDeleted = false;

    /**
     * 文件擁有者名稱
     */
    private String ownerUsername;

    /**
     * 文件的MIME類型
     */
    private String mimeType;


    /**
     * 用戶文件列表數據傳輸對象構造函數
     *
     * @param serverFileMetadata 服務器文件元數據對象
     * @param userFileMetadata   用戶文件元數據對象
     */
    public UserFileListDTO(ServerFileMetadata serverFileMetadata, UserFileMetadata userFileMetadata, Collection<Long> shareUsers) {
        this.id = userFileMetadata.getId();
        this.filename = userFileMetadata.getFilename();
        this.parentFolderId = userFileMetadata.getParentFolderId();
        this.createTime = userFileMetadata.getUploadTime();
        this.lastAccessTime = userFileMetadata.getLastAccessTime();
        this.fileSize = serverFileMetadata.getFileSize();
        this.fileType = userFileMetadata.getFileType();
        this.gridFsId = serverFileMetadata.getGridFsId();
        this.md5 = serverFileMetadata.getMd5();
        this.isStar = userFileMetadata.getIsStar();
        this.isDeleted = userFileMetadata.getIsDeleted();
        this.shareType = userFileMetadata.getShareType();
        this.mimeType = serverFileMetadata.getMimeType();

        if (shareUsers != null) {
            this.shareUsers.addAll(shareUsers);
        }
    }


    /**
     * 用戶文件列表數據傳輸對象構造函數
     *
     * @param userOnlineFile   用戶在線文件對象
     * @param userFileMetadata 用戶文件元數據對象
     */
    public UserFileListDTO(UserOnlineFile userOnlineFile, UserFileMetadata userFileMetadata, Collection<Long> shareUsers) {
        this.id = userFileMetadata.getId();
        this.filename = userFileMetadata.getFilename();
        this.parentFolderId = userFileMetadata.getParentFolderId();
        this.createTime = userFileMetadata.getUploadTime();
        this.lastAccessTime = userFileMetadata.getLastAccessTime();
        this.fileSize = userOnlineFile.getFileSize();
        this.fileType = userFileMetadata.getFileType();
        this.isStar = userFileMetadata.getIsStar();
        this.isDeleted = userFileMetadata.getIsDeleted();
        this.shareType = userFileMetadata.getShareType();

        if (shareUsers != null) {
            this.shareUsers.addAll(shareUsers);
        }
    }


    /**
     * 用戶文件列表數據傳輸對象構造函數
     *
     * @param userFileMetadata 用戶文件元數據對象
     */
    public UserFileListDTO(UserFileMetadata userFileMetadata, Collection<Long> shareUsers) {
        this.id = userFileMetadata.getId();
        this.filename = userFileMetadata.getFilename();
        this.parentFolderId = userFileMetadata.getParentFolderId();
        this.createTime = userFileMetadata.getUploadTime();
        this.fileType = userFileMetadata.getFileType();
        this.lastAccessTime = userFileMetadata.getLastAccessTime();
        this.isStar = userFileMetadata.getIsStar();
        this.isDeleted = userFileMetadata.getIsDeleted();
        this.shareType = userFileMetadata.getShareType();
        if (shareUsers != null) {
            this.shareUsers.addAll(shareUsers);
        }
    }


    /**
     * 用戶文件列表數據傳輸對象構造函數
     *
     * @param userFileMetaWithDataDAO 用戶文件元數據與數據對象
     * @param shareUsers              共享用戶ID集合
     */
    public UserFileListDTO(UserFileMetaWithDataDAO userFileMetaWithDataDAO, Collection<Long> shareUsers) {
        this.id = userFileMetaWithDataDAO.getUfmId();
        this.filename = userFileMetaWithDataDAO.getUfmFilename();
        this.parentFolderId = userFileMetaWithDataDAO.getUfmParentFolderId();
        this.createTime = userFileMetaWithDataDAO.getUfmUploadTime();
        this.lastAccessTime = userFileMetaWithDataDAO.getUfmLastAccessTime();
        this.isStar = userFileMetaWithDataDAO.getUfmIsStar();
        this.fileSize = userFileMetaWithDataDAO.getSfmFileSize();
        this.fileType = userFileMetaWithDataDAO.getUfmFileType();
        this.shareType = userFileMetaWithDataDAO.getUfmShareType();
        this.gridFsId = userFileMetaWithDataDAO.getSfmGridFsId();
        this.md5 = userFileMetaWithDataDAO.getSfmMd5();
        this.isDeleted = userFileMetaWithDataDAO.getUfmIsDeleted();
        this.ownerUsername = userFileMetaWithDataDAO.getOwnerUsername();
        this.mimeType = userFileMetaWithDataDAO.getSfmMimeType();

        if (shareUsers != null) {
            this.shareUsers.addAll(shareUsers);
        }
    }
}
