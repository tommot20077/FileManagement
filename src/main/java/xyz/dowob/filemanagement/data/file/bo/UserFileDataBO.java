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
 * 用戶檔案資料業務對象，封裝用戶檔案的完整資料和業務邏輯。
 * 用於統一管理用戶檔案的元資料、內容資料和共享資訊。
 *
 * <p>此對象整合來自多個實體的資料，包括用戶檔案元資料、伺服器檔案元資料、
 * 在線檔案資料和共享記錄。提供多種建構方式以適應不同的使用場景。
 * 支援非阻塞式資料流讀取和內容編輯功能。
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserFileDataBO {
    /**
     * 用戶檔案元資料識別符
     */
    private Long userFileId;

    /**
     * 伺服器檔案元資料識別符
     */
    private Long serverFileId;

    /**
     * 檔案擁有者的用戶識別符
     */
    private Long userId;

    /**
     * 檔案名稱，包括副檔名
     */
    private String filename;

    /**
     * 父資料夾識別符，為 null 則表示根目錄
     */
    private Long parentFolderId;

    /**
     * 檔案類型枚舉，定義檔案的基本類別
     */
    private FileEnum fileType;

    /**
     * 檔案 MIME 類型，用於內容識別和處理
     */
    private String mimeType;

    /**
     * 檔案大小（以位元組為單位）
     */
    private Long fileSize;

    /**
     * 檔案共享類型，定義檔案的存取權限
     */
    private FileShareTypeEnum shareType;

    /**
     * 共享用戶識別符集合，包含所有可存取此檔案的用戶
     */
    private Set<Long> shareUsers = new HashSet<>();

    /**
     * 最後存取時間，記錄檔案最後一次被存取的時間
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastAccessTime;

    /**
     * 檔案初始上傳時間
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime uploadTime;

    /**
     * MongoDB GridFS 檔案識別符，用於二進位資料存取
     */
    private String gridFsId;

    /**
     * 檔案 MD5 校驗值，用於檔案完整性驗證
     */
    private String md5;

    /**
     * 非阻塞式資料流，用於檔案內容的實時讀取
     */
    @JsonIgnore
    private transient Flux<DataBuffer> dataBufferFlux;

    /**
     * 位元組資料流，用於一次性讀取完整檔案內容
     */
    @JsonIgnore
    private transient Mono<byte[]> dataBufferByte;

    /**
     * 編輯器內容，用於文本檔案的結構化內容表示
     */
    @JsonIgnore
    private transient EditorContentDTO content;

    /**
     * 最後修改者的用戶識別符
     */
    private Long lastModifiedBy;


    /**
     * 建構用戶檔案業務對象，整合伺服器和用戶檔案元資料。
     *
     * @param serverFileMetadata 伺服器檔案元資料，包含檔案的物理資訊
     * @param userFileMetadata   用戶檔案元資料，包含用戶相關的檔案資訊
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
     * 建構用戶檔案業務對象，包含共享記錄資訊。
     *
     * @param serverFileMetadata   伺服器檔案元資料，包含檔案的物理資訊
     * @param userFileMetadata     用戶檔案元資料，包含用戶相關的檔案資訊
     * @param userFileShareRecords 用戶檔案共享記錄集合，定義檔案的共享權限
     */
    public UserFileDataBO(ServerFileMetadata serverFileMetadata, UserFileMetadata userFileMetadata, Collection<UserFileShareRecord> userFileShareRecords) {
        UserFileDataBO userFileDataBO = new UserFileDataBO(serverFileMetadata, userFileMetadata);
        userFileShareRecords.forEach(record -> userFileDataBO.shareUsers.add(record.getUserId()));
    }


    /**
     * 建構在線檔案業務對象，用於在線編輯場景。
     *
     * @param userOnlineFile   在線檔案實體，包含在線編輯相關資訊
     * @param userFileMetadata 用戶檔案元資料，包含基本檔案資訊
     * @param content          編輯器內容，包含結構化的文本內容
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
     * 建構完整的在線檔案業務對象，包含共享記錄資訊。
     *
     * @param userOnlineFile       在線檔案實體，包含在線編輯相關資訊
     * @param userFileMetadata     用戶檔案元資料，包含基本檔案資訊
     * @param content              編輯器內容，包含結構化的文本內容
     * @param userFileShareRecords 用戶檔案共享記錄集合，定義檔案的共享權限
     */
    public UserFileDataBO(UserOnlineFile userOnlineFile, UserFileMetadata userFileMetadata, EditorContentDTO content, Collection<UserFileShareRecord> userFileShareRecords) {
        UserFileDataBO userFileDataBO = new UserFileDataBO(userOnlineFile, userFileMetadata, content);
        userFileShareRecords.forEach(record -> userFileDataBO.shareUsers.add(record.getUserId()));
    }
}
