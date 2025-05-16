package xyz.dowob.filemanagement.data.file.bo;

import lombok.Data;
import xyz.dowob.filemanagement.data.file.dto.FileEditDTO;
import xyz.dowob.filemanagement.entity.UserFileMetadata;

/**
 * 文件編輯的數據傳輸對象，用於規範文件編輯的數據傳輸對象，紀錄文件編輯的數據
 * @author yuan
 * @program FileManagement
 * @ClassName FileEditDTO
 * @create 2025/1/23
 * @Version 1.0
 **/
@Data
public class FileEditBO {
    private FileEditDTO fileEditDTO;

    /**
     * 文件元數據，用於驗證用戶權限
     */
    private UserFileMetadata userFileMetadata;

    /**
     * 父資料夾元數據
     */
    private UserFileMetadata parentFolderFileMetadata;

    public FileEditBO(FileEditDTO fileEditDTO) {
        this.fileEditDTO = fileEditDTO;
    }
}
