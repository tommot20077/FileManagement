package xyz.dowob.filemanagement.data.file.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import xyz.dowob.filemanagement.data.file.bo.UploadTaskBO;
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
    private String fileName;

    /**
     * 文件路徑
     */
    @NotBlank(message = "文件路徑不能為空")
    private String filePath;

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
     * 用戶ID
     */
    private Long userId;


    /**
     * 將文件元數據對象轉換為用戶文件元數據對象
     *
     * @param serverFileId 服務器文件ID
     *
     * @return 用戶文件元數據對象
     */
    public UserFileMetadata formatToUserFileMetadata(Long serverFileId) {
        UserFileMetadata userFileMetadata = new UserFileMetadata();
        userFileMetadata.setFilename(this.fileName);
        userFileMetadata.setFilePath(this.filePath);
        userFileMetadata.setServerFileId(serverFileId);
        userFileMetadata.setUserId(this.userId);
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
        task.setFilePath(formatFilePath(this.getFilePath(), this.getFileName()));
        task.setFileName(this.getFileName());
        task.setMd5(this.getMd5());
        task.setUserId(this.userId);
        task.setMessage(message);
        task.setFileSize(this.getFileSize());
        return task;
    }

    /**
     * 格式化文件路徑
     *
     * @param filePath 文件路徑
     * @param filename 文件名稱
     *
     * @return 格式化後的文件路徑
     */
    public String formatFilePath(String filePath, String filename) {
        if (filePath.lastIndexOf(filename) != -1) {
            return filePath.substring(0, filePath.lastIndexOf(filename));
        }
        return filePath;
    }
}
