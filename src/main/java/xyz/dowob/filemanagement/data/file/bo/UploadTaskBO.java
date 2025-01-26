package xyz.dowob.filemanagement.data.file.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.data.file.dto.FileMetadataDTO;

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
public class UploadTaskBO {
    /**
     * 任務ID
     */
    @NotBlank(message = "任務ID不能為空")
    private String transferTaskId;

    /**
     * 檔案母資料夾ID
     */
    private Long parentFolderId;

    /**
     * 檔案名稱
     */
    @NotBlank(message = "檔案名稱不能為空")
    private String fileName;

    /**
     * 檔案類型
     */
    private FileEnum fileType = FileEnum.OTHER;

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
    public FileMetadataDTO formatToFileMetadata() {
        FileMetadataDTO fileMetadataDTO = new FileMetadataDTO();
        fileMetadataDTO.setFileName(this.fileName);
        fileMetadataDTO.setParentFolderId(this.parentFolderId);
        fileMetadataDTO.setMd5(this.md5);
        fileMetadataDTO.setFileSize(this.fileSize);
        fileMetadataDTO.setUserId(this.userId);
        return fileMetadataDTO;
    }
}
