package xyz.dowob.filemanagement.data.file.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.data.file.bo.UploadTaskBO;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.entity.UserFileMetadata;

import java.time.LocalDateTime;

/**
 * 檔案元資料傳輸對象，用於規範檔案元資料的傳輸對象
 * 用於初始化檔案元資料
 *
 * @author yuan
 * @since 1.0
 * @version 1.0
 **/
@Data
public class FileMetadataDTO {
    /**
     * 檔案名稱
     */
    @NotBlank(message = "檔案名稱不能為空")
    private String filename;

    /**
     * 檔案路徑
     */
    private Long parentFolderId;

    /**
     * 檔案MD5值
     */
    @NotBlank(message = "檔案MD5值不能為空")
    private String md5;

    /**
     * 檔案大小
     */
    @NotBlank(message = "檔案大小不能為空")
    private Long fileSize;

    /**
     * 用戶
     */
    private User user;

    /**
     * 檔案類型
     */
    private FileEnum fileType;


    /**
     * 將檔案元資料對象轉換為用戶檔案元資料對象
     *
     * @param serverFileId 服務器檔案ID
     *
     * @return 用戶檔案元資料對象
     */
    public UserFileMetadata formatToUserFileMetadata(Long serverFileId) {
        UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setFilename(this.filename);
        userFileMetadata.setParentFolderId(this.parentFolderId);
        userFileMetadata.setServerFileId(serverFileId);
        userFileMetadata.setUserId(this.user.getId());
        userFileMetadata.setLastAccessTime(LocalDateTime.now());
        userFileMetadata.setUploadTime(LocalDateTime.now());
        return userFileMetadata;
    }


    /**
     * 將檔案元資料對象轉換為檔案傳輸任務對象
     *
     * @param uploadTaskId 上傳任務ID
     * @param message      任務訊息
     *
     * @return 檔案傳輸任務對象
     */
    public UploadTaskBO formatToTransferTask(String uploadTaskId, String message) {
        UploadTaskBO task = new UploadTaskBO();
        task.setTransferTaskId(uploadTaskId);
        task.setParentFolderId(parentFolderId);
        task.setFilename(this.getFilename());
        task.setMd5(this.getMd5());
        task.setUser(this.user);
        task.setMessage(message);
        task.setFileSize(this.getFileSize());
        return task;
    }
}
