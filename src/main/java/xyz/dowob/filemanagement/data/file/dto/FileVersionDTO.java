package xyz.dowob.filemanagement.data.file.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件版本數據傳輸對象，用於封裝文件版本的數據
 * @author yuan
 * @program FileManagement
 * @ClassName FileVersionDTO
 * @create 2025/2/11
 * @Version 1.0
 **/
@Data
public class FileVersionDTO {
    /**
     * 當前版本號
     */
    private Long version;
    /**
     * 上一個版本號
     */
    private String modifiedBy;
    /**
     * 修改時間
     */
    private LocalDateTime modifiedTime;
    /**
     * 修改註釋
     */
    private String note;
    /**
     * 文件大小
     */
    private Long fileSize;
}