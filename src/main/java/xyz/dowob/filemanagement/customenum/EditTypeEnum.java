package xyz.dowob.filemanagement.customenum;

/**
 * 文件編輯類型枚舉
 * @author yuan
 * @program FileManagement
 * @ClassName FileEditTypeEnum
 * @create 2025/2/11
 * @Version 1.0
 **/

public enum EditTypeEnum {
    /**
     * 檔案編輯，不包含文件內容的編輯
     */
    EDIT_METADATA,

    /**
     * 包含文件內容的編輯
     */
    EDIT_CONTENT,

    /**
     * 建立歷程記錄
     */
    BUILD_HISTORY_RECORD,

    /**
     * 還原歷程記錄
     */
    REVERT_HISTORY_RECORD
}
