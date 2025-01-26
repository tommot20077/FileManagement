package xyz.dowob.filemanagement.data.file.dto;

import jakarta.validation.constraints.NotBlank;
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
    /**
     * 文件ID
     */
    private String fileId;

    /**
     * 文件名稱
     */
    @NotBlank(message = "文件名稱不能為空")
    private String fileName;

    /**
     * 文件父資料夾ID
     */
    private Long parentFolderId;

    /**
     * 分享用戶ID
     */
    private Set<Long> shareUserIds;
}
