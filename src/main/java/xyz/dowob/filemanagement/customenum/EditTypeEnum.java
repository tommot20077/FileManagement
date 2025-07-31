package xyz.dowob.filemanagement.customenum;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * 檔案編輯類型枚舉，定義檔案編輯操作的不同類型。
 *
 * <p>此枚舉用於追蹤和管理檔案的修改狀態，區分元數據編輯和內容編輯，
 * 以及歷程記錄的管理操作。提供了完整的檔案編輯追蹤機制。</p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */

public enum EditTypeEnum {
    /**
     * 元數據編輯，只編輯檔案的元數據資訊，不包含檔案內容的修改。
     */
    EDIT_METADATA,

    /**
     * 內容編輯，包含檔案內容的修改和編輯操作。
     */
    EDIT_CONTENT,

    /**
     * 建立歷程記錄，為檔案編輯操作建立歷史記錄。
     */
    BUILD_HISTORY_RECORD,

    /**
     * 還原歷程記錄，將檔案還原到特定的歷史版本。
     */
    REVERT_HISTORY_RECORD,

    /**
     * 刪除歷程記錄，移除指定的檔案歷史記錄。
     */
    DELETE_HISTORY_RECORD;

    /**
     * 轉換字符串為枚舉類型，此方法將不檢測大小寫。
     *
     * @param original 原始字符串
     * @return 對應的枚舉類型，若找不到匹配的枚舉值則回傳 null
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
