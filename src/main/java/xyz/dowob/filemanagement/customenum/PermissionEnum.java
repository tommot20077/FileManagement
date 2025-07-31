package xyz.dowob.filemanagement.customenum;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 權限枚舉，用於定義系統中的各種權限操作。
 *
 * <p>此枚舉提供了檔案管理系統中的基本權限控制，包括讀取、寫入、
 * 刪除、上傳、下載、分享、更新和管理等操作。這些權限用於控制使用者
 * 對檔案和資料夾的存取和操作能力。</p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */

@Getter
@RequiredArgsConstructor
public enum PermissionEnum {
    /**
     * 寫入權限，允許使用者建立新檔案和資料夾。
     */
    WRITE("寫入"),

    /**
     * 讀取權限，允許使用者檢視檔案內容和檔案資訊。
     */
    READ("讀取"),

    /**
     * 刪除權限，允許使用者刪除檔案和資料夾。
     */
    DELETE("刪除"),

    /**
     * 上傳權限，允許使用者上傳檔案到系統中。
     */
    UPLOAD("上傳"),

    /**
     * 下載權限，允許使用者下載檔案到本地設備。
     */
    DOWNLOAD("下載"),

    /**
     * 分享權限，允許使用者將檔案分享給其他使用者。
     */
    SHARE("分享"),

    /**
     * 更新權限，允許使用者修改檔案內容和屬性。
     */
    UPDATE("更新"),

    /**
     * 管理權限，允許使用者執行系統管理和高級操作。
     */
    MANAGE("管理");

    /**
     * 權限名稱，權限的中文描述。
     */
    private final String permission;

}
