package xyz.dowob.filemanagement.data.file.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import xyz.dowob.filemanagement.data.file.bo.UploadTaskBO;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.entity.UserFileMetadata;

import java.time.LocalDateTime;

/**
 * 文件元數據傳輸對象，用於規範文件元數據的傳輸對象
 * 用於初始化文件元數據
 *
 * @author yuan
 * @program FileManagement
 * @ClassName FileMetadata
 * @description
 * @create 2024-09-26 23:54
 * @Version 1.0
 **/
@Data
public class FileMetadataDTO {
    /**
     * 文件名稱
     */
    @NotBlank(message = "文件名稱不能為空")
    private String filename;

    /**
     * 文件路徑
     */
    private Long parentFolderId;

    /**
     * 文件MD5值
     */
    @NotBlank(message = "文件MD5值不能為空")
    private String md5;

    /**
     * 文件大小
     */
    @NotBlank(message = "文件大小不能為空")
    private Long fileSize;

    /**
     * 用戶
     */
    private User user;


    /**
     * 將文件元數據對象轉換為用戶文件元數據對象
     *
     * @param serverFileId 服務器文件ID
     *
     * @return 用戶文件元數據對象
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
     * 將文件元數據對象轉換為文件傳輸任務對象
     *
     * @param uploadTaskId 上傳任務ID
     * @param message      任務消息
     *
     * @return 文件傳輸任務對象
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
