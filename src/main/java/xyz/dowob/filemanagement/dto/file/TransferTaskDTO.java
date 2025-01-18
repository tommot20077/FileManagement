package xyz.dowob.filemanagement.dto.file;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 文件傳輸任務的數據傳輸對象，用於規範文件傳輸任務的數據傳輸對象，紀錄文件傳輸任務的數據
 *
 * @author yuan
 * @program FileManagement
 * @ClassName TransferTask
 * @description
 * @create 2024-09-27 01:32
 * @Version 1.0
 **/
@Data
public class TransferTaskDTO {
    /**
     * 任務ID
     */
    @NotBlank(message = "任務ID不能為空")
    private String transferTaskId;

    /**
     * 檔案路徑
     */
    @NotBlank(message = "檔案路徑不能為空")
    private String filePath;

    /**
     * 檔案名稱
     */
    @NotBlank(message = "檔案名稱不能為空")
    private String fileName;

    /**
     * 檔案大小
     */
    @NotBlank(message = "檔案大小不能為空")
    private long fileSize;

    /**
     * MD5值
     */
    @NotBlank(message = "MD5不能為空")
    private String md5;

    /**
     * 用戶ID
     */
    @NotBlank(message = "用戶不能為空")
    private Long userId;

    /**
     * 訊息
     */
    private String message;


    /**
     * 將文件傳輸任務對象轉換為文件元數據對象
     *
     * @return 文件元數據對象
     */
    public FileMetadata formatToFileMetadata() {
        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setFileName(this.fileName);
        fileMetadata.setFilePath(this.filePath);
        fileMetadata.setMd5(this.md5);
        fileMetadata.setFileSize(this.fileSize);
        fileMetadata.setUserId(this.userId);
        return fileMetadata;
    }
}
