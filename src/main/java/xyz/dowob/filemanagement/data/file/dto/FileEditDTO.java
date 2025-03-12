package xyz.dowob.filemanagement.data.file.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import xyz.dowob.filemanagement.customenum.EditTypeEnum;
import xyz.dowob.filemanagement.customenum.FileShareTypeEnum;
import xyz.dowob.filemanagement.data.file.po.ShareUserEditPO;
import xyz.dowob.filemanagement.entity.UserFileMetadata;

import java.util.HashSet;
import java.util.Set;

/**
 * 文件編輯的數據傳輸對象，用於規範文件編輯的數據傳輸對象，紀錄文件編輯的數據
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
    @NotBlank(message = "文件ID不能為空")
    private String fileId;

    /**
     * 文件名稱
     */
    @NotBlank(message = "文件名稱不能為空")
    private String filename;

    /**
     * 文件父資料夾ID
     */
    private Long parentFolderId;

    /**
     * 分享用戶ID
     */
    @Valid
    private Set<ShareUserEditPO> shareUsers = new HashSet<>();

    /**
     * 文件分享類型
     */
    private FileShareTypeEnum shareType;

    /**
     * 是否為星標文件
     */
    private Boolean isStar;

    /**
     * 編輯類型
     */
    private EditTypeEnum editType = EditTypeEnum.EDIT_METADATA;

    /**
     * 文件內容
     */
    private EditorContentDTO content;

    /**
     * 備註
     */
    private String note;

    /**
     * 恢復的版本號
     */
    private Long version;

    /**
     * 文件元數據，用於驗證用戶權限
     */
    private UserFileMetadata userFileMetadata;

    /**
     * 父資料夾元數據
     */
    private UserFileMetadata parentFolderFileMetadata;
}
