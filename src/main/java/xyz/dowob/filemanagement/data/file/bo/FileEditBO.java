package xyz.dowob.filemanagement.data.file.bo;

import lombok.Data;
import xyz.dowob.filemanagement.data.file.dto.FileEditDTO;
import xyz.dowob.filemanagement.entity.UserFileMetadata;

/**
 * 檔案編輯業務對象，封裝檔案編輯相關的業務邏輯。
 * 用於包裝檔案編輯請求的資料傳輸對象和相關的元資料驗證資訊。
 *
 * <p>此物件提供檔案編輯操作所需的完整業務上下文，包括編輯請求資料、
 * 用戶權限驗證以及父資料夾權限檢查。支援驗證用戶對指定檔案的編輯權限。
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@Data
public class FileEditBO {
    private FileEditDTO fileEditDTO;

    /**
     * 用戶檔案元資料，用於權限驗證和檔案資訊存取
     */
    private UserFileMetadata userFileMetadata;

    /**
     * 父資料夾檔案元資料，用於資料夾權限驗證
     */
    private UserFileMetadata parentFolderFileMetadata;

    /**
     * 建構檔案編輯業務對象。
     *
     * @param fileEditDTO 檔案編輯資料傳輸對象
     */
    public FileEditBO(FileEditDTO fileEditDTO) {
        this.fileEditDTO = fileEditDTO;
    }
}
