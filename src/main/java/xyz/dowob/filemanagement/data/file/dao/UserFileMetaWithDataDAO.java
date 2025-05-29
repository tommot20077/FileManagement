package xyz.dowob.filemanagement.data.file.dao;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.customenum.FileShareTypeEnum;

import java.time.LocalDateTime;

/**
 * 用戶文件元數據與數據的數據訪問對象，用於封裝用戶文件元數據和主資料數據的實體類
 * @author yuan
 * @program FileManagement
 * @ClassName UserFileMetaWithData
 * @create 2025/5/26
 * @Version 1.0
 **/
@Data
public class UserFileMetaWithDataDAO {
    /**
     * 用戶文件元數據ID
     */
    private Long ufmId;

    /**
     * 用戶文件元數據的檔案名稱
     */
    private String ufmFilename;

    /**
     * 用戶文件元數據的父文件夾ID
     */
    private Long ufmParentFolderId;

    /**
     * 用戶文件元數據是否為星標文件
     */
    private Boolean ufmIsStar;

    /**
     * 用戶文件元數據的檔案類型
     */
    private FileEnum ufmFileType;

    /**
     * 用戶文件元數據的分享類型
     */
    private FileShareTypeEnum ufmShareType;

    /**
     * 用戶文件元數據的上傳時間
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime ufmUploadTime;

    /**
     * 用戶文件元數據的最後訪問時間
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime ufmLastAccessTime;

    /**
     * 用戶文件元數據是否已刪除
     */
    private Boolean ufmIsDeleted;

    /**
     * 伺服器文件元數據ID
     */
    private Long sfmId;

    /**
     * 伺服器元數據檔案大小
     */
    private Long sfmFileSize;

    /**
     * 伺服器元數據的MIME類型
     */
    private String sfmMimeType;

    /**
     * 伺服器元數據的GridFsId
     */
    private String sfmGridFsId;

    /**
     * 伺服器元數據的MD5值
     */
    private String sfmMd5;

    /**
     * 線上檔案的識別ID
     */
    private Long uofId;

    /**
     * 用戶文件元數據的用戶ID
     */
    private Long ufmUserId;

    /**
     * 用戶文件元數據的所有者用戶名
     */
    private String ownerUsername;
}
