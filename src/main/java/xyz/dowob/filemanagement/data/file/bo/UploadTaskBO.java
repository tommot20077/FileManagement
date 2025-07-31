package xyz.dowob.filemanagement.data.file.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import xyz.dowob.filemanagement.data.file.dto.FileMetadataDTO;
import xyz.dowob.filemanagement.entity.User;

import java.util.HashMap;

/**
 * 檔案上傳任務業務對象，封裝檔案上傳任務的完整業務邏輯。
 * 用於管理和追蹤檔案上傳任務的執行狀態和相關信息。
 *
 * <p>提供上傳任務的完整生命週期管理，包括任務建立、進度追蹤和狀態更新。
 * 支援檔案元資料轉換和任務狀態序列化。
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@Data
public class UploadTaskBO {
    /**
     * 唯一任務識別符，用於追蹤和管理上傳任務
     */
    @NotBlank(message = "任務ID不能為空")
    private String transferTaskId;

    /**
     * 父資料夾識別符，指定檔案上傳的目標資料夾
     */
    private Long parentFolderId;

    /**
     * 檔案名稱，包含副檔名
     */
    @NotBlank(message = "檔案名稱不能為空")
    private String filename;

    /**
     * 檔案大小（以位元組為單位）
     */
    @NotBlank(message = "檔案大小不能為空")
    private long fileSize;

    /**
     * 檔案 MD5 校驗值，用於檔案完整性驗證
     */
    @NotBlank(message = "MD5不能為空")
    private String md5;

    /**
     * 上傳任務的用戶實體
     */
    @NotBlank(message = "用戶不能為空")
    private User user;

    /**
     * 任務執行狀態或錯誤訊息
     */
    private String message;
    


    /**
     * 將上傳任務轉換為檔案元資料對象。
     *
     * @return 檔案元資料傳輸對象，包含檔案基本資訊
     */
    public FileMetadataDTO formatToFileMetadata() {
        FileMetadataDTO fileMetadataDTO = new FileMetadataDTO();
        fileMetadataDTO.setFilename(this.filename);
        fileMetadataDTO.setParentFolderId(this.parentFolderId);
        fileMetadataDTO.setMd5(this.md5);
        fileMetadataDTO.setFileSize(this.fileSize);
        fileMetadataDTO.setUser(this.user);
        return fileMetadataDTO;
    }


    /**
     * 將上傳任務轉換為字符串表示。
     *
     * @return 包含任務所有屬性的字符串表示
     */
    @Override
    public String toString() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("transferTaskId", transferTaskId);
        map.put("parentFolderId", parentFolderId);
        map.put("filename", filename);
        map.put("fileSize", fileSize);
        map.put("md5", md5);
        map.put("user", user);
        map.put("message", message);
        return map.toString();
    }
}
