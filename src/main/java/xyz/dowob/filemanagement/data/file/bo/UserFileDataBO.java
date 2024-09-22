package xyz.dowob.filemanagement.data.file.bo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.customenum.FileShareTypeEnum;
import xyz.dowob.filemanagement.data.file.dto.EditorContentDTO;
import xyz.dowob.filemanagement.entity.ServerFileMetadata;
import xyz.dowob.filemanagement.entity.UserFileMetadata;
import xyz.dowob.filemanagement.entity.UserFileShareRecord;
import xyz.dowob.filemanagement.entity.UserOnlineFile;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * 用戶文件數據業務對象，此對象用於封裝用戶文件的數據
 * 包括整個用戶檔案的所有信息
 *
 * @author yuan
 * @program FileManagement
 * @ClassName DownloadTaskBO
 * @create 2025/1/22
 * @Version 1.0
 **/
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserFileDataBO {
    /**
     * 用戶文件ID
     */
    private Long userFileId;

    /**
     * 服務器文件ID
     */
    private Long serverFileId;

    /**
     * 用戶ID
     */
    private Long userId;

    /**
     * 文件名稱
     */
    private String filename;

    /**
     * 父文件夾ID
     */
    private Long parentFolderId;

    /**
     * 文件類型
     */
    private FileEnum fileType;

    /**
     * 文件的MIME類型
     */
    private String mimeType;

    /**
     * 文件大小
     */
    private Long fileSize;

    /**
     * 共享類型
     */
    private FileShareTypeEnum shareType;

    /**
     * 共享用戶
     */
    private Set<Long> shareUsers = new HashSet<>();

    /**
     * 最後更改時間
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastAccessTime;

    /**
     * 上傳時間
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime uploadTime;

    /**
     * GridFS ID
     */
    private String gridFsId;

    /**
     * MD5值
     */
    private String md5;

    /**
     * 數據流 dataBuffer
     */
    @JsonIgnore
    private Flux<DataBuffer> dataBufferFlux;

    /**
     * 數據流 byte[]
     */
    @JsonIgnore
    private Mono<byte[]> dataBufferByte;

    /**
     * 字符串內容
     */
    @JsonIgnore
    private EditorContentDTO content;

    /**
     * 最後修改者
     */
    private Long lastModifiedBy;


    /**
     * 用戶文件數據業務對象構造函數
     *
     * @param serverFileMetadata 服務器文件元數據對象
     * @param userFileMetadata   用戶文件元數據對象
     */
    public UserFileDataBO(ServerFileMetadata serverFileMetadata, UserFileMetadata userFileMetadata) {
        this.userFileId = userFileMetadata.getId();
        this.serverFileId = serverFileMetadata.getId();
        this.userId = userFileMetadata.getUserId();
        this.filename = userFileMetadata.getFilename();
        this.parentFolderId = userFileMetadata.getParentFolderId();
        this.fileType = userFileMetadata.getFileType();
        this.mimeType = serverFileMetadata.getMimeType();
        this.fileSize = serverFileMetadata.getFileSize();
        this.lastAccessTime = userFileMetadata.getLastAccessTime();
        this.uploadTime = userFileMetadata.getUploadTime();
        this.gridFsId = serverFileMetadata.getGridFsId();
        this.md5 = serverFileMetadata.getMd5();
        this.shareType = userFileMetadata.getShareType();
    }


    /**
     * 用戶文件數據業務對象構造函數
     *
     * @param serverFileMetadata   服務器文件元數據對象
     * @param userFileMetadata     用戶文件元數據對象
     * @param userFileShareRecords 用戶文件共享記錄對象集合
     */
    public UserFileDataBO(ServerFileMetadata serverFileMetadata, UserFileMetadata userFileMetadata, Collection<UserFileShareRecord> userFileShareRecords) {
        UserFileDataBO userFileDataBO = new UserFileDataBO(serverFileMetadata, userFileMetadata);
        userFileShareRecords.forEach(record -> userFileDataBO.shareUsers.add(record.getUserId()));
    }


    /**
     * 用戶文件數據業務對象構造函數
     *
     * @param userOnlineFile 用戶線上檔案對象
     */
    public UserFileDataBO(UserOnlineFile userOnlineFile, UserFileMetadata userFileMetadata, EditorContentDTO content) {
        this.userFileId = userOnlineFile.getId();
        this.fileSize = userOnlineFile.getFileSize();
        this.fileType = userFileMetadata.getFileType();
        this.content = content;
        this.lastModifiedBy = userOnlineFile.getLastModifiedBy();
        this.userId = userFileMetadata.getUserId();
        this.filename = userFileMetadata.getFilename();
        this.parentFolderId = userFileMetadata.getParentFolderId();
        this.lastAccessTime = userFileMetadata.getLastAccessTime();
        this.uploadTime = userFileMetadata.getUploadTime();
    }


    /**
     * 用戶文件數據業務對象構造函數
     *
     * @param userOnlineFile       用戶線上檔案對象
     * @param userFileMetadata     用戶文件元數據對象
     * @param content              編輯器內容對象
     * @param userFileShareRecords 用戶文件共享記錄對象集合
     */
    public UserFileDataBO(UserOnlineFile userOnlineFile, UserFileMetadata userFileMetadata, EditorContentDTO content, Collection<UserFileShareRecord> userFileShareRecords) {
        UserFileDataBO userFileDataBO = new UserFileDataBO(userOnlineFile, userFileMetadata, content);
        userFileShareRecords.forEach(record -> userFileDataBO.shareUsers.add(record.getUserId()));
    }
}
