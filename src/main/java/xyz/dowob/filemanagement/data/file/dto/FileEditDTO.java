package xyz.dowob.filemanagement.data.file.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.dowob.filemanagement.customenum.EditTypeEnum;
import xyz.dowob.filemanagement.customenum.FileShareTypeEnum;
import xyz.dowob.filemanagement.data.file.po.ShareUserEditPO;

import java.util.HashSet;
import java.util.Set;

/**
 * 檔案編輯資料傳輸對象，封裝檔案編輯操作的請求資料。
 * 用於傳輸檔案元資料修改、權限設定和內容編輯的資訊。
 *
 * <p>此對象支援多種編輯操作，包括基本元資料修改、共享權限設定、內容編輯和版本恢復。
 * 通過 editType 欄位可以指定具體的編輯操作類型。
 * 支援遞迴設定，可以將權限變更應用到子檔案夾。
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileEditDTO {
    /**
     * 編輯目標檔案的識別符
     */
    @NotBlank(message = "檔案ID不能為空")
    private String fileId;

    /**
     * 新的檔案名稱，包含副檔名
     */
    @NotBlank(message = "檔案名稱不能為空")
    private String filename;

    /**
     * 新的父資料夾識別符，用於移動檔案
     */
    private Long parentFolderId;

    /**
     * 共享用戶編輯記錄集合，定義要修改的共享權限
     */
    @Valid
    private Set<ShareUserEditPO> shareUsers = new HashSet<>();

    /**
     * 新的檔案共享類型
     */
    private FileShareTypeEnum shareType;

    /**
     * 是否遞迴應用權限設定到子檔案夾
     */
    private Boolean recursiveSetting = false;

    /**
     * 是否設定為星標檔案
     */
    private Boolean isStar;

    /**
     * 編輯操作類型，定義具體的編輯動作
     */
    private EditTypeEnum editType = EditTypeEnum.EDIT_METADATA;

    /**
     * 新的檔案內容，用於在線編輯場景
     */
    private EditorContentDTO content;

    /**
     * 編輯備註或版本說明
     */
    private String note;

    /**
     * 要恢復的歷史版本號，用於版本回滾操作
     */
    private Long version;
}
