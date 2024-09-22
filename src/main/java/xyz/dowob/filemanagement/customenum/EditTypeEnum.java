package xyz.dowob.filemanagement.customenum;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * 文件編輯類型枚舉
 *
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
    REVERT_HISTORY_RECORD,

    /**
     * 刪除歷程記錄
     */
    DELETE_HISTORY_RECORD;

    /**
     * 轉換字符串為枚舉類型，此方法將不檢測大小寫
     *
     * @param original 原始字符串
     *
     * @return 對應的枚舉類型
     */
    @JsonCreator
    public static EditTypeEnum fromString(String original) {
        String value = original.toUpperCase();
        for (EditTypeEnum type : EditTypeEnum.values()) {
            if (type.name().equals(value)) {
                return type;
            }
        }
        return null;
    }
}
