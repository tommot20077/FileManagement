package xyz.dowob.filemanagement.data.file.dto;

import lombok.Data;

import java.util.Set;

/**
 * @author yuan
 * @program FileManagement
 * @ClassName FileEditDTO
 * @create 2025/1/23
 * @Version 1.0
 **/
@Data
public class FileEditDTO {
    private String fileId;
    /**
     * 文件名稱
     */
    private String fileName;

    /**
     * 文件路徑
     */
    private String filePath;

    /**
     * 分享用戶ID
     */
    private Set<Long> shareUserIds;
}
