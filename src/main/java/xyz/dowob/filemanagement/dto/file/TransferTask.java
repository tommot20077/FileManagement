package xyz.dowob.filemanagement.dto.file;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import xyz.dowob.filemanagement.entity.User;

/**
 * @author yuan
 * @program FileManagement
 * @ClassName TransferTask
 * @description
 * @create 2024-09-27 01:32
 * @Version 1.0
 **/
@Data
public class TransferTask {
    @NotBlank(message = "任務ID不能為空")
    private String transferTaskId;

    @NotBlank(message = "總分塊數不能為空")
    private Integer totalChunks;

    @NotBlank(message = "檔案路徑不能為空")
    private String filePath;

    @NotBlank(message = "檔案名稱不能為空")
    private String fileName;

    @NotBlank(message = "檔案大小不能為空")
    private long fileSize;

    @NotBlank(message = "MD5不能為空")
    private String md5;

    @NotBlank(message = "用戶不能為空")
    private Long userId;

    private String message;


    public FileMetadata formatToFileMetadata() {
        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setFileName(this.fileName);
        fileMetadata.setFilePath(this.filePath);
        fileMetadata.setMd5(this.md5);
        fileMetadata.setFileSize(this.fileSize);
        fileMetadata.setTotalChunks(this.totalChunks);
        return fileMetadata;
    }
}
