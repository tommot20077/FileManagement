package xyz.dowob.filemanagement.customenum;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * 使用者資訊查詢類型列舉。
 * <p>定義系統中使用者資訊查詢的不同模式，支援以使用者名稱或使用者 ID 進行查詢。
 * 系統會根據指定的查詢類型回傳相對應的資訊：以名稱查詢時回傳 ID，以 ID 查詢時回傳名稱。
 * 若查詢類型無法識別或未指定，系統預設使用名稱查詢模式。
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */

public enum UserInfoTypeEnum {
    /**
     * 使用者名稱查詢模式。
     * 使用使用者名稱作為查詢條件，系統將回傳對應的使用者唯一識別碼。
     */
    NAME,

    /**
     * 使用者識別碼查詢模式。
     * 使用使用者唯一識別碼作為查詢條件，系統將回傳對應的使用者名稱。
     */
    ID;

    /**
     * 根據字串值解析使用者資訊查詢類型。
     * 將字串格式的查詢類型轉換為對應的列舉值，支援不分大小寫的比對。
     * 若無法匹配或參數為 null，則預設回傳名稱查詢模式。
     *
     * @param value 查詢類型字串，如 "name" 或 "id"
     * @return 匹配的查詢類型列舉，預設為 NAME
     */
    @JsonCreator
    public static UserInfoTypeEnum fromString(String value) {
        for (UserInfoTypeEnum type : UserInfoTypeEnum.values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        return NAME;
    }
}